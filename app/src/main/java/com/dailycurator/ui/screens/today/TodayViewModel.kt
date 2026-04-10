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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

data class TodayUiState(
    val tasks: List<PriorityTask> = emptyList(),
    val goals: List<WeeklyGoal> = emptyList(),
    val assistantInsight: AiInsight = AiInsight(
        insightText = "Insights will appear here after the first daily generation.",
        boldPart = "Assistant insight",
    ),
    val weeklyGoalsInsight: AiInsight = AiInsight(
        insightText = "Weekly goal coaching will appear after generation.",
        boldPart = "Weekly focus",
    ),
    val goalsCollapsed: Boolean = false,
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

@OptIn(ExperimentalCoroutinesApi::class)
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

    private val _uiState = MutableStateFlow(
        TodayUiState(cerebrasConfigured = prefs.isLlmConfigured()),
    )
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    private val _detailGoalId = MutableStateFlow<Long?>(null)
    val openDetailGoalId: StateFlow<Long?> = _detailGoalId.asStateFlow()

    val goalDetailLinkedTasks: StateFlow<List<PriorityTask>> = _detailGoalId.flatMapLatest { id ->
        if (id == null || id <= 0L) flowOf(emptyList())
        else taskRepo.getTasksForGoal(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val today = LocalDate.now()
    private val weekStart = today.minusDays(today.dayOfWeek.value.toLong() - 1)

    init {
        viewModelScope.launch {
            taskRepo.getTasksForDate(LocalDate.now()).collect { todayTasks ->
                _uiState.update { it.copy(tasks = todayTasks) }
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
            prefs.llmConfiguredFlow.collect { ok ->
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
    fun toggleGoal(goal: WeeklyGoal) = viewModelScope.launch { goalRepo.toggleCompleted(goal) }

    fun openGoalDetail(goalId: Long) {
        _detailGoalId.value = goalId
    }

    fun dismissGoalDetail() {
        _detailGoalId.value = null
    }

    fun updateWeeklyGoal(goal: WeeklyGoal) = viewModelScope.launch {
        goalRepo.update(goal)
    }

    fun deleteWeeklyGoal(goal: WeeklyGoal) = viewModelScope.launch {
        taskRepo.clearGoalLinks(goal.id)
        goalRepo.delete(goal)
        if (_detailGoalId.value == goal.id) _detailGoalId.value = null
    }

    fun setWeeklyGoalProgress(goalId: Long, percent: Int) = viewModelScope.launch {
        val g = goalRepo.getById(goalId) ?: return@launch
        goalRepo.update(g.copy(progressPercent = percent.coerceIn(0, 100)))
    }

    fun startPomodoroForGoal(goal: WeeklyGoal) {
        pomodoroNavBridge.push(
            PomodoroLaunchRequest(
                entityType = PomodoroLaunchRequest.TYPE_GOAL,
                entityId = goal.id,
                title = goal.title,
            ),
        )
    }

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
                isTopFive = false,
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

    fun addGoalFull(
        title: String,
        description: String?,
        deadline: String?,
        timeEstimate: String?,
        category: String,
        iconEmoji: String?,
        progressPercent: Int,
    ) = viewModelScope.launch {
        goalRepo.insert(
            WeeklyGoal(
                title = title,
                description = description,
                deadline = deadline,
                timeEstimate = timeEstimate,
                category = category,
                iconEmoji = iconEmoji?.trim()?.takeIf { it.isNotEmpty() },
                progressPercent = progressPercent.coerceIn(0, 100),
                weekStart = weekStart,
            ),
        )
    }

    private fun dayWindowMinutesToLocalTime(minuteOfDay: Int): LocalTime {
        val c = minuteOfDay.coerceIn(0, 24 * 60 - 1)
        return LocalTime.of(c / 60, c % 60)
    }
}
