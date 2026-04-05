package com.dailycurator.ui.screens.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailycurator.data.local.entity.HabitLogEntity
import com.dailycurator.data.model.Habit
import com.dailycurator.data.model.HabitType
import com.dailycurator.data.pomodoro.PomodoroLaunchRequest
import com.dailycurator.data.pomodoro.PomodoroNavBridge
import com.dailycurator.data.repository.HabitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HabitsUiState(
    val buildingHabits: List<Habit> = emptyList(),
    val eliminatingHabits: List<Habit> = emptyList(),
    val showCompleted: Boolean = false,
)

@HiltViewModel
class HabitsViewModel @Inject constructor(
    private val repo: HabitRepository,
    private val pomodoroNavBridge: PomodoroNavBridge,
) : ViewModel() {

    private val _showCompleted = MutableStateFlow(false)

    val uiState: StateFlow<HabitsUiState> = combine(
        repo.getHabitsForDate(LocalDate.now()),
        _showCompleted,
    ) { habits, showCompleted ->
        val filtered = if (showCompleted) habits else habits.filter { !it.isDone }
        HabitsUiState(
            buildingHabits = filtered.filter { it.habitType == HabitType.BUILDING },
            eliminatingHabits = filtered.filter { it.habitType == HabitType.ELIMINATING },
            showCompleted = showCompleted,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HabitsUiState())

    fun habitLogsFlow(seriesId: String): Flow<List<HabitLogEntity>> =
        repo.observeLogsForSeries(seriesId)

    fun toggleShowCompleted() {
        _showCompleted.value = !_showCompleted.value
    }

    fun markHabitDone(habit: Habit, note: String?) = viewModelScope.launch {
        repo.markHabitDone(habit, note)
    }

    fun deleteHabitSeries(habit: Habit) = viewModelScope.launch {
        val sid = repo.resolveSeriesId(habit)
        repo.deleteSeries(sid)
    }

    fun updateHabit(
        habit: Habit,
        name: String,
        category: String,
        type: HabitType,
        emoji: String,
        target: Float,
        unit: String,
        trigger: String?,
        frequency: String,
    ) = viewModelScope.launch {
        repo.update(
            habit.copy(
                name = name,
                category = category,
                habitType = type,
                iconEmoji = emoji,
                targetValue = target,
                unit = unit,
                trigger = trigger,
                frequency = frequency,
            ),
        )
    }

    fun addHabit(
        name: String,
        category: String,
        habitType: HabitType,
        iconEmoji: String,
        targetValue: Float,
        unit: String,
        trigger: String? = null,
        frequency: String = "daily",
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
                trigger = trigger,
                frequency = frequency,
                date = LocalDate.now(),
            ),
        )
    }

    fun startPomodoroForHabit(habit: Habit) {
        pomodoroNavBridge.push(
            PomodoroLaunchRequest(
                entityType = PomodoroLaunchRequest.TYPE_HABIT,
                entityId = habit.id,
                title = habit.name,
                habitSeriesId = repo.resolveSeriesId(habit),
            ),
        )
    }

    suspend fun completedDaysForMonth(habit: Habit, month: LocalDate): Set<LocalDate> =
        repo.getCompletedDayKeysForMonth(repo.resolveSeriesId(habit), month)

    suspend fun missedCountFor(habit: Habit): Int = repo.missedInWindow(habit)
}
