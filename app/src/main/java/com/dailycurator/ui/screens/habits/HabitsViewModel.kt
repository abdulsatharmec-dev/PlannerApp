package com.dailycurator.ui.screens.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailycurator.data.model.Habit
import com.dailycurator.data.model.HabitType
import com.dailycurator.data.repository.HabitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import javax.inject.Inject

data class HabitsUiState(
    val buildingHabits: List<Habit> = emptyList(),
    val eliminatingHabits: List<Habit> = emptyList()
)

@HiltViewModel
class HabitsViewModel @Inject constructor(
    private val repo: HabitRepository
) : ViewModel() {

    val uiState: StateFlow<HabitsUiState> =
        repo.getHabitsForDate(LocalDate.now()).map { habits ->
            HabitsUiState(
                buildingHabits = habits.filter { it.habitType == HabitType.BUILDING },
                eliminatingHabits = habits.filter { it.habitType == HabitType.ELIMINATING }
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HabitsUiState())
}
