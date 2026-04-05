package com.dailycurator.ui.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailycurator.data.model.PriorityTask
import com.dailycurator.data.model.Urgency
import com.dailycurator.data.pomodoro.PomodoroLaunchRequest
import com.dailycurator.data.pomodoro.PomodoroNavBridge
import com.dailycurator.data.repository.TaskRepository
import com.dailycurator.reminders.TaskReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject

enum class TasksDisplayMode {
    /** Week strip + tasks for the selected day (list order by rank / time). */
    WEEK_CALENDAR,
    /** Same selected day, tasks grouped by urgency. */
    PRIORITY_GROUPS,
}

data class TasksUiState(
    val displayMode: TasksDisplayMode = TasksDisplayMode.WEEK_CALENDAR,
    val listDate: LocalDate = LocalDate.now(),
    val weekDays: List<LocalDate> = emptyList(),
    val tasks: List<PriorityTask> = emptyList(),
    val tasksByDay: Map<LocalDate, List<PriorityTask>> = emptyMap(),
    val prioritySections: List<Pair<String, List<PriorityTask>>> = emptyList(),
)

private fun LocalDate.startOfWeek(locale: Locale = Locale.getDefault()): LocalDate {
    val first = WeekFields.of(locale).firstDayOfWeek
    var cursor = this
    while (cursor.dayOfWeek != first) {
        cursor = cursor.minusDays(1)
    }
    return cursor
}

private fun compareTasks(): Comparator<PriorityTask> =
    compareBy<PriorityTask> { it.rank }.thenBy { it.startTime }

private fun buildPrioritySections(tasks: List<PriorityTask>): List<Pair<String, List<PriorityTask>>> {
    val sorted = tasks.sortedWith(compareTasks())
    val order = listOf(
        Urgency.RED to "High priority",
        Urgency.GREEN to "Normal",
        Urgency.NEUTRAL to "Low priority",
    )
    return order.mapNotNull { (u, label) ->
        val part = sorted.filter { it.urgency == u }
        if (part.isEmpty()) null else label to part
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TasksViewModel @Inject constructor(
    private val repo: TaskRepository,
    private val pomodoroNavBridge: PomodoroNavBridge,
    private val taskReminderScheduler: TaskReminderScheduler,
) : ViewModel() {

    private val listDate = MutableStateFlow(LocalDate.now())
    private val displayMode = MutableStateFlow(TasksDisplayMode.WEEK_CALENDAR)

    val uiState: StateFlow<TasksUiState> = combine(
        listDate,
        displayMode,
        listDate.flatMapLatest { d -> repo.getTasksForDate(d) },
        listDate.flatMapLatest { d ->
            val start = d.startOfWeek()
            val end = start.plusDays(6)
            repo.getTasksBetween(start, end).map { tasks ->
                tasks.groupBy { it.date }.mapValues { (_, v) -> v.sortedWith(compareTasks()) }
            }
        },
    ) { date, mode, tasksForDay, byDay ->
        val weekStart = date.startOfWeek()
        val weekDays = (0L..6L).map { weekStart.plusDays(it) }
        val sortedDay = tasksForDay.sortedWith(compareTasks())
        TasksUiState(
            displayMode = mode,
            listDate = date,
            weekDays = weekDays,
            tasks = sortedDay,
            tasksByDay = byDay,
            prioritySections = buildPrioritySections(sortedDay),
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        TasksUiState(),
    )

    fun setListDate(date: LocalDate) {
        listDate.value = date
    }

    fun setDisplayMode(mode: TasksDisplayMode) {
        displayMode.value = mode
    }

    fun shiftSelectedWeek(weeks: Long) {
        listDate.value = listDate.value.plusWeeks(weeks)
    }

    fun addTask(
        title: String,
        date: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        urgency: Urgency,
        isTop5: Boolean,
        note: String?,
    ) = viewModelScope.launch {
        val dayTasks = repo.getTasksForDate(date).first()
        val nextRank = if (isTop5) 1 else ((dayTasks.maxOfOrNull { it.rank } ?: 0) + 1)
        val newId = repo.insert(
            PriorityTask(
                rank = nextRank,
                title = title,
                startTime = startTime,
                endTime = endTime,
                statusNote = note,
                urgency = urgency,
                date = date,
            ),
        )
        repo.getById(newId)?.let { taskReminderScheduler.schedule(it) }
    }

    fun updateTask(
        task: PriorityTask,
        title: String,
        date: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        urgency: Urgency,
        isTop5: Boolean,
        note: String?,
    ) = viewModelScope.launch {
        val updatedRank = if (isTop5) 1 else task.rank.coerceAtLeast(2)
        val updated = task.copy(
            title = title,
            date = date,
            startTime = startTime,
            endTime = endTime,
            urgency = urgency,
            rank = updatedRank,
            statusNote = note,
        )
        repo.update(updated)
        taskReminderScheduler.cancel(task.id)
        repo.getById(task.id)?.let { taskReminderScheduler.schedule(it) }
    }

    fun deleteTask(task: PriorityTask) = viewModelScope.launch {
        taskReminderScheduler.cancel(task.id)
        repo.delete(task)
    }

    fun toggleTaskDone(task: PriorityTask) = viewModelScope.launch {
        repo.toggleDone(task)
        val t = repo.getById(task.id) ?: return@launch
        if (t.isDone) {
            taskReminderScheduler.cancel(t.id)
        } else {
            taskReminderScheduler.schedule(t)
        }
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
}
