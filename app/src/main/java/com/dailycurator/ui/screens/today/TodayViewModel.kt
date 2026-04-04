package com.dailycurator.ui.screens.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailycurator.data.model.*
import com.dailycurator.data.repository.GoalRepository
import com.dailycurator.data.repository.TaskRepository
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
    val insight: AiInsight = AiInsight(
        insightText = "Today's focus on Strategy Audit will put you ahead of schedule for the Q3 release. Keep this momentum.",
        boldPart = "You've crushed 85% of your deep work this week.",
        recoveryPlan = "Focus on the 90m Deep Work block tomorrow morning to catch up on documentation tasks."
    ),
    val goalsCollapsed: Boolean = false,
    val scheduleTab: ScheduleTab = ScheduleTab.TIMELINE
)

enum class ScheduleTab { TIMELINE, CLOCK }

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val taskRepo: TaskRepository,
    private val goalRepo: GoalRepository
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
    }

    fun toggleTaskDone(task: PriorityTask) = viewModelScope.launch { taskRepo.toggleDone(task) }
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
        taskRepo.insert(
            PriorityTask(
                rank = nextRank,
                title = title,
                startTime = startTime,
                endTime = endTime,
                urgency = urgency,
                date = today
            )
        )
    }

    fun addGoal(title: String) = viewModelScope.launch {
        goalRepo.insert(WeeklyGoal(title = title, weekStart = weekStart))
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

