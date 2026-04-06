package com.dailycurator.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailycurator.data.chat.ChatToolExecutor
import com.dailycurator.data.chat.PendingChatDeletion
import com.dailycurator.data.chat.ToolCallEnvelope
import com.dailycurator.data.ai.JournalContextFormatter
import com.dailycurator.data.chat.cerebrasChatToolDefinitions
import com.dailycurator.data.chat.gmailAgentChatTools
import com.dailycurator.data.local.AppPreferences
import com.dailycurator.data.local.ChatFontSizeCategory
import com.dailycurator.data.local.entity.ChatMessageEntity
import com.dailycurator.data.remote.CerebrasApiException
import com.dailycurator.data.remote.CerebrasChatMessage
import com.dailycurator.data.remote.CerebrasRestClient
import com.dailycurator.data.repository.AgentMemoryRepository
import com.dailycurator.data.repository.ChatRepository
import com.dailycurator.data.repository.GoalRepository
import com.dailycurator.data.repository.HabitRepository
import com.dailycurator.data.repository.JournalRepository
import com.dailycurator.data.repository.PhoneUsageRepository
import com.dailycurator.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class ChatMessage(
    val id: Long,
    val content: String,
    val isUser: Boolean,
    val timestamp: LocalDateTime,
    val totalTokens: Int? = null,
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val habitRepository: HabitRepository,
    private val goalRepository: GoalRepository,
    private val journalRepository: JournalRepository,
    private val chatRepository: ChatRepository,
    private val prefs: AppPreferences,
    private val cerebras: CerebrasRestClient,
    private val toolExecutor: ChatToolExecutor,
    private val agentMemoryRepository: AgentMemoryRepository,
    private val phoneUsageRepository: PhoneUsageRepository,
) : ViewModel() {

    val messages = chatRepository.observeMessages()
        .map { list -> list.map { it.toChatMessage() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val chatFontSizeCategory: StateFlow<ChatFontSizeCategory> =
        prefs.chatFontSizeCategoryFlow
            .map { ChatFontSizeCategory.fromStorageId(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChatFontSizeCategory.DEFAULT)

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _pendingDeletion = MutableStateFlow<PendingChatDeletion?>(null)
    val pendingDeletion = _pendingDeletion.asStateFlow()

    /** Suggested memory lines from the user's messages; shown in chat for confirm/skip. */
    private val _pendingMemoryProposal = MutableStateFlow<List<String>?>(null)
    val pendingMemoryProposal = _pendingMemoryProposal.asStateFlow()

    /** Bumped on [clearChat] so in-flight replies are not written after history is cleared. */
    private var chatSession: Int = 0

    fun clearChat() {
        chatSession++
        _isLoading.value = false
        _pendingDeletion.value = null
        _pendingMemoryProposal.value = null
        viewModelScope.launch {
            chatRepository.clearAll()
        }
    }

    fun dismissMemoryProposal() {
        _pendingMemoryProposal.value = null
    }

    fun confirmMemoryProposal(lines: List<String>) = viewModelScope.launch {
        if (lines.isEmpty()) {
            _pendingMemoryProposal.value = null
            return@launch
        }
        withContext(Dispatchers.IO) {
            agentMemoryRepository.insertUserConfirmedMemoryLines(lines)
        }
        _pendingMemoryProposal.value = null
    }

    fun dismissPendingDeletion() {
        _pendingDeletion.value = null
    }

    fun confirmPendingDeletion() = viewModelScope.launch {
        val sessionAt = chatSession
        when (val p = _pendingDeletion.value) {
            null -> return@launch
            is PendingChatDeletion.Task -> {
                val t = taskRepository.getById(p.id)
                if (t != null) taskRepository.delete(t)
                if (sessionAt == chatSession) {
                    chatRepository.appendMessage("Deleted task \"${p.title}\".", false)
                }
            }
            is PendingChatDeletion.Goal -> {
                val g = goalRepository.getById(p.id)
                if (g != null) goalRepository.delete(g)
                if (sessionAt == chatSession) {
                    chatRepository.appendMessage("Deleted goal \"${p.title}\".", false)
                }
            }
            is PendingChatDeletion.Habit -> {
                val h = habitRepository.getById(p.id)
                if (h != null) {
                    val sid = h.seriesId.ifBlank { null }
                    if (!sid.isNullOrBlank()) habitRepository.deleteSeries(sid)
                    else habitRepository.delete(h)
                }
                if (sessionAt == chatSession) {
                    chatRepository.appendMessage("Deleted habit \"${p.title}\".", false)
                }
            }
        }
        _pendingDeletion.value = null
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || _isLoading.value) return

        val sessionAtSend = chatSession
        viewModelScope.launch {
            chatRepository.appendMessage(text, true)
            if (sessionAtSend != chatSession) return@launch

            if (!prefs.isLlmConfigured()) {
                chatRepository.appendMessage(
                    "Add at least one LLM API key in Settings (Manage API keys) to chat.",
                    false,
                )
                return@launch
            }

            _isLoading.value = true
            try {
                runChatTurn(sessionAtSend)
                if (sessionAtSend == chatSession && prefs.isAgentMemoryEnabled()) {
                    val recent = chatRepository.getRecentAscending(16)
                    val proposed = withContext(Dispatchers.IO) {
                        agentMemoryRepository.proposeMemoryFromUserChatOnly(recent)
                    }
                    if (sessionAtSend == chatSession && proposed.isNotEmpty()) {
                        _pendingMemoryProposal.value = proposed
                    }
                }
            } catch (e: Exception) {
                if (sessionAtSend != chatSession) return@launch
                chatRepository.appendMessage(
                    "I had trouble communicating with the server: ${e.localizedMessage}\n\nPlease check your key in Settings and try again.",
                    false,
                )
            } finally {
                if (sessionAtSend == chatSession) {
                    _isLoading.value = false
                }
            }
        }
    }

    private suspend fun runChatTurn(sessionAtSend: Int) {
        val tools = cerebrasChatToolDefinitions() + gmailAgentChatTools(prefs)
        val memoryBlock = agentMemoryRepository.formatForChatContext()
        val apiMessages = mutableListOf<CerebrasChatMessage>()
        apiMessages.add(
            CerebrasChatMessage(
                role = "system",
                content = buildString {
                    appendLine("You are Daily Curator, a planner assistant with tools to create/update tasks, weekly goals, and habits.")
                    appendLine("If Gmail tools are listed, the user linked Google accounts in Settings. Use them only for real mail; never invent message bodies. Respect read vs send permissions.")
                    appendLine("If a JOURNAL section appears below, the user chose to share excerpts with you — be respectful, avoid quoting long passages, and do not invent journal content.")
                    appendLine("If a PHONE USAGE section appears, the user opted in — foreground time from Android; session counts are approximate; use for planning/wellbeing context without shaming.")
                    appendLine("Use tools when the user wants changes. Reference ids from the data snapshot below.")
                    appendLine("For delete_task, delete_goal, or delete_habit: the app will ask the user to confirm — tell them to use the confirmation bar.")
                    appendLine("After tools succeed, reply briefly in natural language confirming what changed.")
                    appendLine()
                    append(buildContext())
                    if (memoryBlock.isNotBlank()) {
                        appendLine()
                        append(memoryBlock)
                    }
                },
            ),
        )

        val recent = chatRepository.getRecentAscending(40)
        recent.forEach { e ->
            apiMessages.add(
                CerebrasChatMessage(
                    role = if (e.isUser) "user" else "assistant",
                    content = e.content,
                ),
            )
        }

        var iterations = 0
        var turnTokenTotal = 0
        while (iterations++ < 10) {
            if (sessionAtSend != chatSession) return

            val completion = withContext(Dispatchers.IO) {
                cerebras.chatCompletion(
                    messages = apiMessages,
                    tools = tools,
                    toolChoice = "auto",
                    temperature = 0.3,
                    maxTokens = 2048,
                )
            }
            completion.totalTokens?.let { turnTokenTotal += it }
            if (sessionAtSend != chatSession) return

            val choiceMsg = completion.message
                ?: throw CerebrasApiException("Empty completion")

            val toolCalls = choiceMsg.toolCalls
            if (!toolCalls.isNullOrEmpty()) {
                apiMessages.add(
                    CerebrasChatMessage(
                        role = "assistant",
                        content = choiceMsg.content,
                        toolCalls = toolCalls,
                    ),
                )
                for (tc in toolCalls) {
                    if (sessionAtSend != chatSession) return
                    when (
                        val env = toolExecutor.execute(
                            tc.functionName,
                            tc.argumentsJson,
                            tc.id,
                        )
                    ) {
                        is ToolCallEnvelope.Done -> {
                            apiMessages.add(
                                CerebrasChatMessage(
                                    role = "tool",
                                    content = env.content,
                                    toolCallId = env.toolCallId,
                                ),
                            )
                        }
                        is ToolCallEnvelope.AwaitDeleteConfirmation -> {
                            _pendingDeletion.value = env.deletion
                            val label = when (val d = env.deletion) {
                                is PendingChatDeletion.Task -> "task \"${d.title}\""
                                is PendingChatDeletion.Goal -> "goal \"${d.title}\""
                                is PendingChatDeletion.Habit -> "habit \"${d.title}\""
                            }
                            chatRepository.appendMessage(
                                "I'm ready to delete $label. Confirm or cancel using the bar below.",
                                isUser = false,
                                totalTokens = null,
                            )
                            return
                        }
                    }
                }
                continue
            }

            val finalText = choiceMsg.content?.trim().orEmpty()
            val tokensForBubble = turnTokenTotal.takeIf { it > 0 }
            if (finalText.isNotEmpty()) {
                chatRepository.appendMessage(finalText, isUser = false, totalTokens = tokensForBubble)
            } else {
                chatRepository.appendMessage("(No text response.)", isUser = false, totalTokens = tokensForBubble)
            }
            return
        }
        chatRepository.appendMessage(
            "Stopped after too many tool steps. Try a simpler request.",
            isUser = false,
            totalTokens = null,
        )
    }

    private suspend fun buildContext(): String {
        val now = LocalDateTime.now()
        val today = LocalDate.now()
        val weekStart = today.minusDays(today.dayOfWeek.value.toLong() - 1)

        val tasks = taskRepository.getTasksForDate(today).firstOrNull() ?: emptyList()
        val habits = habitRepository.getHabitsForDate(today).firstOrNull() ?: emptyList()
        val goals = goalRepository.getGoalsForWeek(weekStart).firstOrNull() ?: emptyList()

        val sb = StringBuilder()
        sb.append("Current Time: ${now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))}\n\n")

        sb.append("--- TASKS FOR TODAY ---\n")
        if (tasks.isEmpty()) sb.append("None\n")
        tasks.forEach {
            sb.append("- id=${it.id} [${if (it.isDone) "X" else " "}] ${it.title} (${it.startTime}-${it.endTime}) urgency=${it.urgency} rank=${it.rank}\n")
        }

        sb.append("\n--- HABITS FOR TODAY ---\n")
        if (habits.isEmpty()) sb.append("None\n")
        habits.forEach {
            sb.append("- id=${it.id} [${if (it.isDone) "X" else " "}] ${it.name} (${it.currentValue}/${it.targetValue} ${it.unit})\n")
        }

        sb.append("\n--- GOALS FOR THIS WEEK ---\n")
        if (goals.isEmpty()) sb.append("None\n")
        goals.forEach {
            sb.append("- id=${it.id} [${if (it.isCompleted) "X" else " "}] ${it.title}: ${it.description} deadline=${it.deadline}\n")
        }

        if (prefs.isJournalSharedWithChat()) {
            val journal = journalRepository.getRecentForAiContext(30)
            sb.append("\n--- JOURNAL (recent, user-enabled for chat) ---\n")
            sb.append(JournalContextFormatter.format(journal))
            sb.append("\n")
        }

        if (prefs.isPhoneUsageInChatAgent()) {
            val usage = phoneUsageRepository.buildCompactUsageContextBlock(prefs.getPhoneUsageAiContextDays())
            if (!usage.isNullOrBlank()) {
                sb.append("\n")
                sb.append(usage)
                sb.append("\n")
            }
        }

        return sb.toString()
    }
}

private fun ChatMessageEntity.toChatMessage(): ChatMessage {
    val zone = ZoneId.systemDefault()
    return ChatMessage(
        id = id,
        content = content,
        isUser = isUser,
        timestamp = Instant.ofEpochMilli(createdAtEpochMillis).atZone(zone).toLocalDateTime(),
        totalTokens = totalTokens,
    )
}
