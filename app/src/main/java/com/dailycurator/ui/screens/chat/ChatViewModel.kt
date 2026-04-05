package com.dailycurator.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.llm.LLMProvider
import com.dailycurator.data.local.AppPreferences
import com.dailycurator.data.local.entity.ChatMessageEntity
import com.dailycurator.data.repository.ChatRepository
import com.dailycurator.data.repository.GoalRepository
import com.dailycurator.data.repository.HabitRepository
import com.dailycurator.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.time.ExperimentalTime

data class ChatMessage(
    val id: Long,
    val content: String,
    val isUser: Boolean,
    val timestamp: LocalDateTime,
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val habitRepository: HabitRepository,
    private val goalRepository: GoalRepository,
    private val chatRepository: ChatRepository,
    private val prefs: AppPreferences,
) : ViewModel() {

    val messages = chatRepository.observeMessages()
        .map { list -> list.map { it.toChatMessage() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private var agent: AIAgent<String, String>? = null

    init {
        setupAgent()
    }

    private var lastUsedKey: String? = null
    private var lastUsedModelId: String? = null

    /** Bumped on [clearChat] so in-flight replies are not written after history is cleared. */
    private var chatSession: Int = 0

    fun clearChat() {
        chatSession++
        _isLoading.value = false
        viewModelScope.launch {
            chatRepository.clearAll()
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun setupAgent() {
        val apiKey = prefs.getCerebrasKey()
        val modelId = prefs.getCerebrasModelId()
        if (apiKey.isBlank()) {
            agent = null
            lastUsedKey = null
            lastUsedModelId = null
            return
        }

        if (apiKey == lastUsedKey && modelId == lastUsedModelId && agent != null) return

        val client = OpenAILLMClient(
            apiKey = apiKey,
            settings = OpenAIClientSettings(baseUrl = "https://api.cerebras.ai"),
        )
        val executor = SingleLLMPromptExecutor(client)
        val model = LLModel(
            provider = LLMProvider.OpenAI,
            id = modelId,
            capabilities = listOf(
                LLMCapability.Completion,
                LLMCapability.OpenAIEndpoint.Completions,
            ),
            contextLength = 8096L,
            maxOutputTokens = null,
        )

        agent = AIAgent(promptExecutor = executor, llmModel = model)
        lastUsedKey = apiKey
        lastUsedModelId = modelId
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || _isLoading.value) return

        setupAgent()

        val sessionAtSend = chatSession
        viewModelScope.launch {
            chatRepository.appendMessage(text, true)
            if (sessionAtSend != chatSession) return@launch

            if (agent == null) {
                chatRepository.appendMessage(
                    "Please set your Cerebras API Key in Settings to chat.",
                    false,
                )
                return@launch
            }

            _isLoading.value = true
            try {
                val context = buildContext()
                if (sessionAtSend != chatSession) return@launch
                val prompt = """
                    Current Context:
                    $context

                    User: $text
                """.trimIndent()

                val response = withContext(Dispatchers.IO) {
                    agent?.run(prompt) ?: "Error: Agent not initialized properly."
                }
                if (sessionAtSend != chatSession) return@launch
                chatRepository.appendMessage(response, false)
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

    private suspend fun buildContext(): String {
        val now = LocalDateTime.now()
        val today = LocalDate.now()
        val weekStart = today.minusDays(today.dayOfWeek.value.toLong() % 7)

        val tasks = taskRepository.getTasksForDate(today).firstOrNull() ?: emptyList()
        val habits = habitRepository.getHabitsForDate(today).firstOrNull() ?: emptyList()
        val goals = goalRepository.getGoalsForWeek(weekStart).firstOrNull() ?: emptyList()

        val sb = StringBuilder()
        sb.append("Current Time: ${now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))}\n\n")

        sb.append("--- TASKS FOR TODAY ---\n")
        if (tasks.isEmpty()) sb.append("None\n")
        tasks.forEach { sb.append("- [${if (it.isDone) "X" else " "}] ${it.title} (${it.startTime}-${it.endTime}) [Urgency: ${it.urgency}]\n") }

        sb.append("\n--- HABITS FOR TODAY ---\n")
        if (habits.isEmpty()) sb.append("None\n")
        habits.forEach { sb.append("- [${if (it.isDone) "X" else " "}] ${it.name} (${it.currentValue}/${it.targetValue} ${it.unit})\n") }

        sb.append("\n--- GOALS FOR THIS WEEK ---\n")
        if (goals.isEmpty()) sb.append("None\n")
        goals.forEach { sb.append("- [${if (it.isCompleted) "X" else " "}] ${it.title}: ${it.description} (Deadline: ${it.deadline})\n") }

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
    )
}
