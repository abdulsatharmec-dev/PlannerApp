package com.dailycurator.ui.screens.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailycurator.data.model.Habit
import com.dailycurator.data.model.HabitCategory
import com.dailycurator.data.model.HabitType
import com.dailycurator.data.repository.HabitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HabitsUiState(
    val buildingHabits: List<Habit> = emptyList(),
    val eliminatingHabits: List<Habit> = emptyList(),
    val showCompleted: Boolean = false
)

@HiltViewModel
class HabitsViewModel @Inject constructor(
    private val repo: HabitRepository
) : ViewModel() {

    private val _showCompleted = MutableStateFlow(false)

    val uiState: StateFlow<HabitsUiState> = combine(
        repo.getHabitsForDate(LocalDate.now()),
        _showCompleted
    ) { habits, showCompleted ->
        val filtered = if (showCompleted) habits else habits.filter { !it.isDone }
        HabitsUiState(
            buildingHabits = filtered.filter { it.habitType == HabitType.BUILDING },
            eliminatingHabits = filtered.filter { it.habitType == HabitType.ELIMINATING },
            showCompleted = showCompleted
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HabitsUiState())

    fun toggleShowCompleted() {
        _showCompleted.value = !_showCompleted.value
    }

    fun markHabitDone(habit: Habit, note: String?) = viewModelScope.launch {
        repo.markHabitDone(habit, note)
    }

    fun addHabit(
        name: String,
        category: HabitCategory,
        habitType: HabitType,
        iconEmoji: String,
        targetValue: Float,
        unit: String
    ) = viewModelScope.launch {
        repo.insert(
            Habit(
                name = name,
                category = category,
                habitType = habitType,
                iconEmoji = iconEmoji,
                currentValue = 0f,
                targetValue = targetValue,
                unit = unit,
                date = LocalDate.now()
            )
        )
    }
}
