package com.dailycurator.ui.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailycurator.data.model.PriorityTask
import com.dailycurator.data.model.TaskRepeatOption
import com.dailycurator.data.model.Urgency
import com.dailycurator.data.model.WeeklyGoal
import com.dailycurator.data.pomodoro.PomodoroLaunchRequest
import com.dailycurator.data.pomodoro.PomodoroNavBridge
import com.dailycurator.data.repository.GoalRepository
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject

enum class TasksDisplayMode {
    /** Tasks for the selected day (list order by rank / time). */
    WEEK_CALENDAR,
    /** Same selected day, tasks grouped by urgency. */
    PRIORITY_GROUPS,
}

enum class TaskDeleteScope {
    /** Remove only the row for the task you tapped. */
    THIS_DAY_ONLY,
    /** Remove this calendar day and every later occurrence in the same repeat (series or legacy match). */
    THIS_AND_FUTURE_DAYS,
    /** Remove every occurrence in the series or legacy match, including past days. */
    ALL_SERIES_DAYS,
}

data class TasksUiState(
    val displayMode: TasksDisplayMode = TasksDisplayMode.WEEK_CALENDAR,
    val listDate: LocalDate = LocalDate.now(),
    val tasks: List<PriorityTask> = emptyList(),
    val prioritySections: List<Pair<String, List<PriorityTask>>> = emptyList(),
    val activeGoals: List<WeeklyGoal> = emptyList(),
    /** When false, completed tasks are hidden from the list. */
    val showCompletedTasks: Boolean = false,
    /** When false, must-do tasks are hidden from the list. */
    val showMustDoTasks: Boolean = true,
    /** True when the day has tasks but all are completed and hidden by the filter. */
    val allTasksDoneHidden: Boolean = false,
    /** True when tasks remain after the completed filter but every one is must-do and must-do is hidden. */
    val allMustDoHidden: Boolean = false,
)

private fun compareTasks(): Comparator<PriorityTask> =
    compareBy<PriorityTask> { it.rank }.thenBy { it.startTime }

private fun repeatExpansionDates(
    anchor: LocalDate,
    repeat: TaskRepeatOption,
    customIntervalDays: Int,
    untilDate: LocalDate?,
): List<LocalDate> {
    val step = customIntervalDays.coerceIn(2, 30)
    val unlimited = when (repeat) {
        TaskRepeatOption.NONE -> listOf(anchor)
        TaskRepeatOption.DAILY -> (0..13).map { anchor.plusDays(it.toLong()) }
        TaskRepeatOption.WEEKLY -> (0..3).map { anchor.plusWeeks(it.toLong()) }
        TaskRepeatOption.CUSTOM -> (0..7).map { anchor.plusDays(it * step.toLong()) }
    }
    if (untilDate == null) return unlimited
    val capped = unlimited.filter { !it.isAfter(untilDate) }
    return capped.ifEmpty {
        if (!anchor.isAfter(untilDate)) listOf(anchor) else emptyList()
    }
}

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
    private val goalRepo: GoalRepository,
    private val pomodoroNavBridge: PomodoroNavBridge,
    private val taskReminderScheduler: TaskReminderScheduler,
) : ViewModel() {

    private val listDate = MutableStateFlow(LocalDate.now())
    private val displayMode = MutableStateFlow(TasksDisplayMode.WEEK_CALENDAR)
    private val showCompletedTasks = MutableStateFlow(false)
    private val showMustDoTasks = MutableStateFlow(true)
    private val activeGoals = goalRepo.getActiveGoals().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    val uiState: StateFlow<TasksUiState> = listDate
        .flatMapLatest { date ->
            combine(
                displayMode,
                showCompletedTasks,
                showMustDoTasks,
                repo.getTasksForDate(date),
                activeGoals,
            ) { mode, showDone, showMust, tasksForDay, goals ->
                val afterDone = if (!showDone) tasksForDay.filter { !it.isDone } else tasksForDay
                val sortedDay = (if (!showMust) afterDone.filter { !it.isMustDo } else afterDone)
                    .sortedWith(compareTasks())
                val allDoneHidden =
                    !showDone && tasksForDay.isNotEmpty() && afterDone.isEmpty()
                val allMustHidden =
                    !showMust && afterDone.isNotEmpty() && sortedDay.isEmpty()
                TasksUiState(
                    displayMode = mode,
                    listDate = date,
                    tasks = sortedDay,
                    prioritySections = buildPrioritySections(sortedDay),
                    activeGoals = goals,
                    showCompletedTasks = showDone,
                    showMustDoTasks = showMust,
                    allTasksDoneHidden = allDoneHidden,
                    allMustDoHidden = allMustHidden,
                )
            }
        }
        .stateIn(
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

    fun setShowCompletedTasks(show: Boolean) {
        showCompletedTasks.value = show
    }

    fun setShowMustDoTasks(show: Boolean) {
        showMustDoTasks.value = show
    }

    /** Default “repeat until” end date for the task dialog (last day of the uncapped pattern). */
    fun suggestedRepeatUntil(anchor: LocalDate, repeat: TaskRepeatOption, customRepeatIntervalDays: Int): LocalDate {
        val customDays = customRepeatIntervalDays.coerceIn(2, 30)
        return repeatExpansionDates(anchor, repeat, customDays, untilDate = null).last()
    }

    suspend fun shouldOfferRepeatDeleteOptions(task: PriorityTask): Boolean {
        if (task.repeatSeriesId != null) return true
        return repo.findLegacyRepeatSiblings(task).size > 1
    }

    fun addTask(
        title: String,
        date: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        urgency: Urgency,
        isTop5: Boolean,
        isMustDo: Boolean,
        displayNumber: Int,
        note: String?,
        goalId: Long?,
        repeat: TaskRepeatOption,
        customRepeatIntervalDays: Int,
        repeatUntilDate: LocalDate?,
    ) = viewModelScope.launch {
        val customDays = customRepeatIntervalDays.coerceIn(2, 30)
        val seriesId = if (repeat == TaskRepeatOption.NONE) null else UUID.randomUUID().toString()
        val until = repeatUntilDate?.takeIf { repeat != TaskRepeatOption.NONE }
        val dates = repeatExpansionDates(date, repeat, customDays, until)
        for (d in dates) {
            val dayTasks = repo.getTasksForDate(d).first()
            val nextRank = (dayTasks.maxOfOrNull { it.rank } ?: 0) + 1
            val newId = repo.insert(
                PriorityTask(
                    rank = nextRank,
                    title = title,
                    startTime = startTime,
                    endTime = endTime,
                    statusNote = note,
                    urgency = urgency,
                    isTopFive = isTop5,
                    isMustDo = isMustDo,
                    displayNumber = displayNumber.coerceIn(0, 999),
                    goalId = goalId,
                    date = d,
                    repeatSeriesId = seriesId,
                    repeatOption = repeat,
                    customRepeatIntervalDays = customDays,
                    repeatUntilDate = until,
                ),
            )
            repo.getById(newId)?.let { taskReminderScheduler.schedule(it) }
        }
    }

    fun updateTask(
        task: PriorityTask,
        title: String,
        date: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        urgency: Urgency,
        isTop5: Boolean,
        isMustDo: Boolean,
        displayNumber: Int,
        note: String?,
        goalId: Long?,
        repeat: TaskRepeatOption,
        customRepeatIntervalDays: Int,
        repeatUntilDate: LocalDate?,
    ) = viewModelScope.launch {
        val customDays = customRepeatIntervalDays.coerceIn(2, 30)
        val until = repeatUntilDate?.takeIf { repeat != TaskRepeatOption.NONE }
        val seriesId = task.repeatSeriesId
        val dateChanged = date != task.date

        if (seriesId != null && !dateChanged) {
            val siblings = repo.getTasksByRepeatSeriesId(seriesId)
            val newSeriesId = if (repeat == TaskRepeatOption.NONE) null else seriesId
            for (t in siblings) {
                val merged = t.copy(
                    title = title,
                    startTime = startTime,
                    endTime = endTime,
                    urgency = urgency,
                    isTopFive = isTop5,
                    isMustDo = isMustDo,
                    displayNumber = displayNumber.coerceIn(0, 999),
                    statusNote = note,
                    goalId = goalId,
                    repeatSeriesId = newSeriesId,
                    repeatOption = repeat,
                    customRepeatIntervalDays = customDays,
                    repeatUntilDate = until,
                )
                repo.update(merged)
                rescheduleTaskReminders(merged.id)
            }
            return@launch
        }

        if (seriesId != null && dateChanged) {
            taskReminderScheduler.cancel(task.id)
            val detached = task.copy(
                title = title,
                date = date,
                startTime = startTime,
                endTime = endTime,
                urgency = urgency,
                isTopFive = isTop5,
                isMustDo = isMustDo,
                displayNumber = displayNumber.coerceIn(0, 999),
                statusNote = note,
                goalId = goalId,
                repeatSeriesId = null,
                repeatOption = repeat,
                customRepeatIntervalDays = customDays,
                repeatUntilDate = until,
            )
            repo.update(detached)
            rescheduleTaskReminders(task.id)
            if (repeat != TaskRepeatOption.NONE) {
                expandRepeatSeriesFromAnchor(detached, repeat, customDays, until)
            }
            return@launch
        }

        if (repeat == TaskRepeatOption.NONE) {
            taskReminderScheduler.cancel(task.id)
            val updated = task.copy(
                title = title,
                date = date,
                startTime = startTime,
                endTime = endTime,
                urgency = urgency,
                isTopFive = isTop5,
                isMustDo = isMustDo,
                displayNumber = displayNumber.coerceIn(0, 999),
                statusNote = note,
                goalId = goalId,
                repeatSeriesId = null,
                repeatOption = TaskRepeatOption.NONE,
                customRepeatIntervalDays = customDays,
                repeatUntilDate = null,
            )
            repo.update(updated)
            rescheduleTaskReminders(task.id)
            return@launch
        }

        val newSeriesId = UUID.randomUUID().toString()
        taskReminderScheduler.cancel(task.id)
        val dates = repeatExpansionDates(date, repeat, customDays, until)
        val anchorUpdated = task.copy(
            title = title,
            date = date,
            startTime = startTime,
            endTime = endTime,
            urgency = urgency,
            isTopFive = isTop5,
            isMustDo = isMustDo,
            displayNumber = displayNumber.coerceIn(0, 999),
            statusNote = note,
            goalId = goalId,
            repeatSeriesId = newSeriesId,
            repeatOption = repeat,
            customRepeatIntervalDays = customDays,
            repeatUntilDate = until,
        )
        repo.update(anchorUpdated)
        rescheduleTaskReminders(task.id)
        for (d in dates) {
            if (d == date) continue
            val dayTasks = repo.getTasksForDate(d).first()
            val nextRank = (dayTasks.maxOfOrNull { it.rank } ?: 0) + 1
            val newId = repo.insert(
                PriorityTask(
                    rank = nextRank,
                    title = title,
                    startTime = startTime,
                    endTime = endTime,
                    statusNote = note,
                    urgency = urgency,
                    isTopFive = isTop5,
                    isMustDo = isMustDo,
                    displayNumber = displayNumber.coerceIn(0, 999),
                    goalId = goalId,
                    date = d,
                    repeatSeriesId = newSeriesId,
                    repeatOption = repeat,
                    customRepeatIntervalDays = customDays,
                    repeatUntilDate = until,
                ),
            )
            repo.getById(newId)?.let { taskReminderScheduler.schedule(it) }
        }
    }

    private suspend fun expandRepeatSeriesFromAnchor(
        anchor: PriorityTask,
        repeat: TaskRepeatOption,
        customDays: Int,
        repeatUntilDate: LocalDate?,
    ) {
        val newSeriesId = UUID.randomUUID().toString()
        val dates = repeatExpansionDates(anchor.date, repeat, customDays, repeatUntilDate)
        val withSeries = anchor.copy(
            repeatSeriesId = newSeriesId,
            repeatOption = repeat,
            customRepeatIntervalDays = customDays,
            repeatUntilDate = repeatUntilDate,
        )
        repo.update(withSeries)
        rescheduleTaskReminders(withSeries.id)
        for (d in dates) {
            if (d == anchor.date) continue
            val dayTasks = repo.getTasksForDate(d).first()
            val nextRank = (dayTasks.maxOfOrNull { it.rank } ?: 0) + 1
            val newId = repo.insert(
                PriorityTask(
                    rank = nextRank,
                    title = withSeries.title,
                    startTime = withSeries.startTime,
                    endTime = withSeries.endTime,
                    statusNote = withSeries.statusNote,
                    urgency = withSeries.urgency,
                    isTopFive = withSeries.isTopFive,
                    isMustDo = withSeries.isMustDo,
                    displayNumber = withSeries.displayNumber,
                    goalId = withSeries.goalId,
                    date = d,
                    repeatSeriesId = newSeriesId,
                    repeatOption = repeat,
                    customRepeatIntervalDays = customDays,
                    repeatUntilDate = repeatUntilDate,
                ),
            )
            repo.getById(newId)?.let { taskReminderScheduler.schedule(it) }
        }
    }

    private suspend fun rescheduleTaskReminders(taskId: Long) {
        taskReminderScheduler.cancel(taskId)
        repo.getById(taskId)?.let { taskReminderScheduler.schedule(it) }
    }

    fun deleteTask(task: PriorityTask, scope: TaskDeleteScope = TaskDeleteScope.THIS_DAY_ONLY) =
        viewModelScope.launch {
            val toRemove = tasksToDeleteForScope(task, scope)
            for (t in toRemove) {
                taskReminderScheduler.cancel(t.id)
                repo.delete(t)
            }
        }

    private suspend fun tasksToDeleteForScope(
        task: PriorityTask,
        scope: TaskDeleteScope,
    ): List<PriorityTask> {
        if (scope == TaskDeleteScope.THIS_DAY_ONLY) return listOf(task)
        val seriesId = task.repeatSeriesId
        val siblings = if (seriesId != null) {
            repo.getTasksByRepeatSeriesId(seriesId)
        } else {
            repo.findLegacyRepeatSiblings(task)
        }
        if (siblings.size <= 1) return listOf(task)
        return when (scope) {
            TaskDeleteScope.THIS_DAY_ONLY -> listOf(task)
            TaskDeleteScope.THIS_AND_FUTURE_DAYS ->
                siblings.filter { !it.date.isBefore(task.date) }
            TaskDeleteScope.ALL_SERIES_DAYS -> siblings
        }
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
