package com.dailycurator.ui.screens.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailycurator.data.model.WeeklyGoal
import com.dailycurator.data.repository.GoalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class GoalsUiState(
    val completedGoals: List<WeeklyGoal> = emptyList(),
    val pendingGoals: List<WeeklyGoal> = emptyList()
) {
    val total get() = completedGoals.size + pendingGoals.size
    val completedCount get() = completedGoals.size
}

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val repo: GoalRepository
) : ViewModel() {

    private val today = LocalDate.now()
    private val weekStart = today.minusDays(today.dayOfWeek.value.toLong() - 1)

    val uiState: StateFlow<GoalsUiState> =
        repo.getGoalsForWeek(weekStart).map { goals ->
            GoalsUiState(
                completedGoals = goals.filter { it.isCompleted },
                pendingGoals = goals.filter { !it.isCompleted }
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GoalsUiState())

    fun toggleGoal(goal: WeeklyGoal) = viewModelScope.launch { repo.toggleCompleted(goal) }

    fun addGoal(title: String, description: String?, deadline: String?, timeEstimate: String?, category: String) = viewModelScope.launch {
        repo.insert(WeeklyGoal(title = title, description = description, deadline = deadline, timeEstimate = timeEstimate, category = category, weekStart = weekStart))
    }
}

