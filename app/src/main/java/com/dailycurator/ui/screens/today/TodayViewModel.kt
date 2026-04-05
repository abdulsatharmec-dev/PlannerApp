package com.dailycurator.ui.screens.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailycurator.data.local.AppPreferences
import com.dailycurator.data.model.*
import com.dailycurator.data.repository.GoalRepository
import com.dailycurator.data.repository.HabitRepository
import com.dailycurator.data.repository.GmailMailboxSummaryRepository
import com.dailycurator.data.repository.InsightCacheRepository
import com.dailycurator.data.pomodoro.PomodoroLaunchRequest
import com.dailycurator.data.pomodoro.PomodoroNavBridge
import com.dailycurator.data.repository.TaskRepository
import com.dailycurator.reminders.TaskReminderScheduler
import com.dailycurator.data.repository.toAiInsight
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

data class TodayUiState(
    val tasks: List<PriorityTask> = emptyList(),
    val goals: List<WeeklyGoal> = emptyList(),
    val scheduleEvents: List<ScheduleEvent> = emptyList(),
    val assistantInsight: AiInsight = AiInsight(
        insightText = "Insights will appear here after the first daily generation.",
        boldPart = "Assistant insight",
    ),
    val weeklyGoalsInsight: AiInsight = AiInsight(
        insightText = "Weekly goal coaching will appear after generation.",
        boldPart = "Weekly focus",
    ),
    val goalsCollapsed: Boolean = false,
    val scheduleTab: ScheduleTab = ScheduleTab.TIMELINE,
    val assistantInsightEnabled: Boolean = true,
    val weeklyGoalsInsightEnabled: Boolean = true,
    val cerebrasConfigured: Boolean = false,
    val assistantInsightLoading: Boolean = false,
    val weeklyGoalsInsightLoading: Boolean = false,
    val dayWindowStart: LocalTime = LocalTime.of(4, 0),
    val dayWindowEnd: LocalTime = LocalTime.of(22, 0),
    val homeGmailSummaryEnabled: Boolean = false,
    /** Condensed markdown/text for the Home card (from cached mailbox summary). */
    val gmailHomeDigestMarkdown: String = "",
)

enum class ScheduleTab { TIMELINE, CLOCK }

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val taskRepo: TaskRepository,
    private val goalRepo: GoalRepository,
    private val habitRepo: HabitRepository,
    private val insightRepo: InsightCacheRepository,
    private val gmailMailboxSummaryRepo: GmailMailboxSummaryRepository,
    private val prefs: AppPreferences,
    private val pomodoroNavBridge: PomodoroNavBridge,
    private val taskReminderScheduler: TaskReminderScheduler,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    private val today = LocalDate.now()
    private val weekStart = today.minusDays(today.dayOfWeek.value.toLong() - 1)

    init {
        viewModelScope.launch {
            taskRepo.getTasksForDate(today).collect { tasks ->
                _uiState.update { it.copy(tasks = tasks, scheduleEvents = tasks.toScheduleEvents()) }
            }
        }
        viewModelScope.launch {
            goalRepo.getGoalsForWeek(weekStart).collect { goals ->
                _uiState.update { it.copy(goals = goals) }
            }
        }
        viewModelScope.launch {
            prefs.assistantInsightEnabledFlow.collect { enabled ->
                _uiState.update { it.copy(assistantInsightEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            prefs.weeklyGoalsInsightEnabledFlow.collect { enabled ->
                _uiState.update { it.copy(weeklyGoalsInsightEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            prefs.cerebrasKeyPresentFlow.collect { ok ->
                _uiState.update { it.copy(cerebrasConfigured = ok) }
            }
        }
        viewModelScope.launch {
            insightRepo.observeAssistant().collect { entity ->
                _uiState.update {
                    it.copy(assistantInsight = entity?.toAiInsight() ?: it.assistantInsight)
                }
            }
        }
        viewModelScope.launch {
            insightRepo.observeWeeklyGoals().collect { entity ->
                _uiState.update {
                    it.copy(weeklyGoalsInsight = entity?.toAiInsight() ?: it.weeklyGoalsInsight)
                }
            }
        }
        viewModelScope.launch {
            prefs.dayWindowFlow.collect { w ->
                _uiState.update {
                    it.copy(
                        dayWindowStart = dayWindowMinutesToLocalTime(w.startMinute),
                        dayWindowEnd = dayWindowMinutesToLocalTime(w.endMinute),
                    )
                }
            }
        }
        viewModelScope.launch {
            insightRepo.ensureAssistantForTodayIfNeeded()
        }
        viewModelScope.launch {
            insightRepo.ensureWeeklyGoalsForTodayIfNeeded()
        }
        viewModelScope.launch {
            prefs.homeGmailSummaryEnabledFlow.collect { enabled ->
                _uiState.update { it.copy(homeGmailSummaryEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            gmailMailboxSummaryRepo.observe().collect { entity ->
                val digest = when {
                    entity == null -> ""
                    else -> {
                        val rp = entity.recoveryPlan
                        if (!rp.isNullOrBlank()) rp
                        else {
                            val t = entity.insightText.take(600)
                            if (entity.insightText.length > 600) "$t…" else t
                        }
                    }
                }
                _uiState.update { it.copy(gmailHomeDigestMarkdown = digest) }
            }
        }
    }

    fun regenerateAssistantInsight() = viewModelScope.launch {
        _uiState.update { it.copy(assistantInsightLoading = true) }
        val result = insightRepo.regenerateAssistant()
        _uiState.update { it.copy(assistantInsightLoading = false) }
        result.onFailure { /* UI can observe stale state; optional snackbar via effect */ }
    }

    fun regenerateWeeklyGoalsInsight() = viewModelScope.launch {
        _uiState.update { it.copy(weeklyGoalsInsightLoading = true) }
        insightRepo.regenerateWeeklyGoals()
        _uiState.update { it.copy(weeklyGoalsInsightLoading = false) }
    }

    fun toggleTaskDone(task: PriorityTask) = viewModelScope.launch {
        taskRepo.toggleDone(task)
        val t = taskRepo.getById(task.id) ?: return@launch
        if (t.isDone) {
            taskReminderScheduler.cancel(t.id)
        } else {
            taskReminderScheduler.schedule(t)
        }
    }
    fun toggleGoalsCollapsed() = _uiState.update { it.copy(goalsCollapsed = !it.goalsCollapsed) }
    fun setScheduleTab(tab: ScheduleTab) = _uiState.update { it.copy(scheduleTab = tab) }
    fun toggleGoal(goal: WeeklyGoal) = viewModelScope.launch { goalRepo.toggleCompleted(goal) }

    fun addTask(
        title: String,
        startTime: LocalTime,
        endTime: LocalTime,
        urgency: Urgency = Urgency.GREEN
    ) = viewModelScope.launch {
        val nextRank = (_uiState.value.tasks.maxOfOrNull { it.rank } ?: 0) + 1
        val newId = taskRepo.insert(
            PriorityTask(
                rank = nextRank,
                title = title,
                startTime = startTime,
                endTime = endTime,
                urgency = urgency,
                date = today
            )
        )
        taskRepo.getById(newId)?.let { taskReminderScheduler.schedule(it) }
    }

    fun startPomodoroForTask(task: PriorityTask) {
        pomodoroNavBridge.push(
            PomodoroLaunchRequest(
                entityType = PomodoroLaunchRequest.TYPE_TASK,
                entityId = task.id,
                title = task.title,
            ),
        )
    }

    fun addGoal(title: String) = viewModelScope.launch {
        goalRepo.insert(WeeklyGoal(title = title, weekStart = weekStart))
    }

    private fun dayWindowMinutesToLocalTime(minuteOfDay: Int): LocalTime {
        val c = minuteOfDay.coerceIn(0, 24 * 60 - 1)
        return LocalTime.of(c / 60, c % 60)
    }

    private fun List<PriorityTask>.toScheduleEvents(): List<ScheduleEvent> = map { task ->
        ScheduleEvent(
            id = task.id, title = task.title,
            startTime = task.startTime, endTime = task.endTime,
            location = task.statusNote,
            tags = if (task.urgency == Urgency.RED) listOf("HIGH") else if (task.rank == 1) listOf("FOCUS", "HIGH") else emptyList(),
            priority = if (task.urgency == Urgency.RED) EventPriority.HIGH else EventPriority.MEDIUM,
            isProtected = task.rank == 1,
            date = task.date
        )
    }
}
