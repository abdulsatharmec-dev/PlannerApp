package com.dailycurator.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.llm.LLMProvider
import com.dailycurator.data.local.AppPreferences
import com.dailycurator.data.repository.GoalRepository
import com.dailycurator.data.repository.HabitRepository
import com.dailycurator.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.time.ExperimentalTime

data class ChatMessage(
    val content: String,
    val isUser: Boolean,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val habitRepository: HabitRepository,
    private val goalRepository: GoalRepository,
    private val prefs: AppPreferences
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private var agent: AIAgent<String, String>? = null

    init {
        viewModelScope.launch {
            // Note: Since AppPreferences is a callbackFlow, we don't have a direct key flow.
            // But we can check if the key changed during sendMessage or just use a simple check.
            // Let's just track the last used key in the ViewModel.
            setupAgent()
        }
    }

    private var lastUsedKey: String? = null

    @OptIn(ExperimentalTime::class)
    private fun setupAgent() {
        val apiKey = prefs.getCerebrasKey()
        if (apiKey.isBlank()) {
            agent = null
            lastUsedKey = null
            return
        }
        
        if (apiKey == lastUsedKey && agent != null) return

        val client = OpenAILLMClient(
            apiKey = apiKey,
            settings = OpenAIClientSettings(baseUrl = "https://api.cerebras.ai")
        )
        val executor = SingleLLMPromptExecutor(client)
        val customProvider = object : LLMProvider("other", "Other") {}
        val model = LLModel(
            id = "qwen-3-235b-a22b-instruct-2507",
            provider = customProvider,
            contextLength = 8096
        )

        agent = AIAgent(promptExecutor = executor, llmModel = model)
        lastUsedKey = apiKey
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || _isLoading.value) return
        
        setupAgent()

        val userMsg = ChatMessage(text, true)
        _messages.value = _messages.value + userMsg

        if (agent == null) {
            _messages.value = _messages.value + ChatMessage("Please set your Cerebras API Key in Settings to chat.", false)
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val context = buildContext()
                val prompt = """
                    Current Context:
                    $context
                    
                    User: $text
                """.trimIndent()
                
                val response = agent?.run(prompt) ?: "Error: Agent not initialized properly."
                _messages.value = _messages.value + ChatMessage(response, false)
            } catch (e: Exception) {
                _messages.value = _messages.value + ChatMessage("I had trouble communicating with the server: ${e.localizedMessage}\n\nPlease check your key in Settings and try again.", false)
            } finally {
                _isLoading.value = false
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
