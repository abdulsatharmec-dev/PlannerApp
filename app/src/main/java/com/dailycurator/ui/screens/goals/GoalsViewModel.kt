package com.dailycurator.ui.screens.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailycurator.data.model.PriorityTask
import com.dailycurator.data.model.WeeklyGoal
import com.dailycurator.data.pomodoro.PomodoroLaunchRequest
import com.dailycurator.data.pomodoro.PomodoroNavBridge
import com.dailycurator.data.repository.GoalRepository
import com.dailycurator.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class GoalsUiState(
    val weekStart: LocalDate = LocalDate.now(),
    val completedGoals: List<WeeklyGoal> = emptyList(),
    val pendingGoals: List<WeeklyGoal> = emptyList(),
) {
    val total get() = completedGoals.size + pendingGoals.size
    val completedCount get() = completedGoals.size
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val repo: GoalRepository,
    private val taskRepo: TaskRepository,
    private val pomodoroNavBridge: PomodoroNavBridge,
) : ViewModel() {

    private val today = LocalDate.now()
    private val weekStart = today.minusDays(today.dayOfWeek.value.toLong() - 1)

    private val _detailGoalId = MutableStateFlow<Long?>(null)
    val openDetailGoalId: StateFlow<Long?> = _detailGoalId.asStateFlow()

    val uiState: StateFlow<GoalsUiState> =
        repo.getGoalsForWeek(weekStart).map { goals ->
            GoalsUiState(
                weekStart = weekStart,
                completedGoals = goals.filter { it.isCompleted },
                pendingGoals = goals.filter { !it.isCompleted },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GoalsUiState())

    val goalDetailLinkedTasks: StateFlow<List<PriorityTask>> = _detailGoalId.flatMapLatest { id ->
        if (id == null || id <= 0L) flowOf(emptyList())
        else taskRepo.getTasksForGoal(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun openGoalDetail(goalId: Long) {
        _detailGoalId.value = goalId
    }

    fun dismissGoalDetail() {
        _detailGoalId.value = null
    }

    fun toggleGoal(goal: WeeklyGoal) = viewModelScope.launch { repo.toggleCompleted(goal) }

    fun addGoal(title: String, description: String?, deadline: String?, timeEstimate: String?, category: String, iconEmoji: String?, progressPercent: Int) =
        viewModelScope.launch {
            repo.insert(
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

    fun updateGoal(goal: WeeklyGoal) = viewModelScope.launch {
        repo.update(goal)
    }

    fun deleteGoal(goal: WeeklyGoal) = viewModelScope.launch {
        taskRepo.clearGoalLinks(goal.id)
        repo.delete(goal)
        if (_detailGoalId.value == goal.id) _detailGoalId.value = null
    }

    fun setGoalProgress(goalId: Long, percent: Int) = viewModelScope.launch {
        val g = repo.getById(goalId) ?: return@launch
        repo.update(g.copy(progressPercent = percent.coerceIn(0, 100)))
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
}
