package com.dailycurator.ui.screens.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailycurator.data.local.AppPreferences
import com.dailycurator.data.model.EventPriority
import com.dailycurator.data.model.ScheduleEvent
import com.dailycurator.data.model.Urgency
import com.dailycurator.data.repository.TaskRepository
import com.dailycurator.data.model.PriorityTask
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

data class ScheduleUiState(
    val scheduleEvents: List<ScheduleEvent> = emptyList(),
    val scheduleTab: ScheduleTab = ScheduleTab.TIMELINE,
    val dayWindowStart: LocalTime = LocalTime.of(4, 0),
    val dayWindowEnd: LocalTime = LocalTime.of(22, 0),
    val scheduleTimelineDate: LocalDate = LocalDate.now(),
)

enum class ScheduleTab { TIMELINE, MAP }

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val taskRepo: TaskRepository,
    private val prefs: AppPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    private val scheduleTimelineDate = MutableStateFlow(LocalDate.now())

    init {
        viewModelScope.launch {
            scheduleTimelineDate.flatMapLatest { d ->
                taskRepo.getTasksForDate(d)
                    .debounce(45)
                    .distinctUntilChanged()
                    .map { tasks -> d to tasks }
            }.collect { (sd, scheduleTasks) ->
                _uiState.update {
                    it.copy(
                        scheduleTimelineDate = sd,
                        scheduleEvents = scheduleTasks.toScheduleEvents(),
                    )
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
    }

    fun setScheduleTab(tab: ScheduleTab) = _uiState.update { it.copy(scheduleTab = tab) }

    fun setScheduleTimelineDate(date: LocalDate) {
        scheduleTimelineDate.value = date
    }

    private fun dayWindowMinutesToLocalTime(minuteOfDay: Int): LocalTime {
        val c = minuteOfDay.coerceIn(0, 24 * 60 - 1)
        return LocalTime.of(c / 60, c % 60)
    }

    private fun List<PriorityTask>.toScheduleEvents(): List<ScheduleEvent> = map { task ->
        ScheduleEvent(
            id = task.id,
            title = task.title,
            startTime = task.startTime,
            endTime = task.endTime,
            location = task.statusNote,
            tags = if (task.urgency == Urgency.RED) {
                listOf("HIGH")
            } else if (task.isTopFive) {
                listOf("FOCUS", "HIGH")
            } else {
                emptyList()
            },
            priority = if (task.urgency == Urgency.RED) EventPriority.HIGH else EventPriority.MEDIUM,
            isProtected = task.isTopFive,
            date = task.date,
        )
    }
}
