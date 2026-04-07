package com.dailycurator.data.repository

import com.dailycurator.data.local.dao.TaskDao
import com.dailycurator.data.local.entity.TaskEntity
import com.dailycurator.data.model.PriorityTask
import com.dailycurator.data.model.Urgency
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

private val DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE
private val TIME_FMT = DateTimeFormatter.ofPattern("HH:mm")

private fun String.toLocalTime() = LocalTime.parse(this, TIME_FMT)
private fun String.toLocalDate() = LocalDate.parse(this, DATE_FMT)

private fun TaskEntity.toPriorityTask() = PriorityTask(
    id = id, rank = rank, title = title,
    startTime = startTime.toLocalTime(),
    endTime = endTime.toLocalTime(),
    dueInfo = dueInfo, statusNote = statusNote,
    urgency = Urgency.valueOf(urgency),
    isDone = isDone, isTopFive = isTopFive, isMustDo = isMustDo, displayNumber = displayNumber,
    goalId = goalId, date = date.toLocalDate(),
)

@Singleton
class TaskRepository @Inject constructor(private val dao: TaskDao) {
    fun getTasksForDate(date: LocalDate): Flow<List<PriorityTask>> =
        dao.getTasksForDate(date.format(DATE_FMT)).map { list ->
            list.map { it.toPriorityTask() }
        }

    fun getTasksBetween(start: LocalDate, end: LocalDate): Flow<List<PriorityTask>> =
        dao.getTasksBetween(start.format(DATE_FMT), end.format(DATE_FMT)).map { list ->
            list.map { it.toPriorityTask() }
        }

    suspend fun getById(id: Long): PriorityTask? = dao.getById(id)?.toPriorityTask()

    fun getTasksForGoal(goalId: Long): Flow<List<PriorityTask>> =
        dao.getTasksForGoal(goalId).map { list -> list.map { it.toPriorityTask() } }

    suspend fun nextRankForDate(date: LocalDate): Int {
        val tasks = getTasksForDate(date).first()
        return (tasks.maxOfOrNull { it.rank } ?: 0) + 1
    }

    suspend fun toggleDone(task: PriorityTask) = dao.setDone(task.id, !task.isDone)

    suspend fun insert(task: PriorityTask): Long = dao.insert(
        TaskEntity(id = task.id, rank = task.rank, title = task.title,
            startTime = task.startTime.format(TIME_FMT),
            endTime = task.endTime.format(TIME_FMT),
            dueInfo = task.dueInfo, statusNote = task.statusNote,
            urgency = task.urgency.name, isDone = task.isDone, isTopFive = task.isTopFive, isMustDo = task.isMustDo,
            displayNumber = task.displayNumber, goalId = task.goalId,
            date = task.date.format(DATE_FMT))
    )

    suspend fun update(task: PriorityTask) = dao.update(
        TaskEntity(id = task.id, rank = task.rank, title = task.title,
            startTime = task.startTime.format(TIME_FMT),
            endTime = task.endTime.format(TIME_FMT),
            dueInfo = task.dueInfo, statusNote = task.statusNote,
            urgency = task.urgency.name, isDone = task.isDone, isTopFive = task.isTopFive, isMustDo = task.isMustDo,
            displayNumber = task.displayNumber, goalId = task.goalId,
            date = task.date.format(DATE_FMT))
    )

    suspend fun delete(task: PriorityTask) = dao.delete(
        TaskEntity(id = task.id, rank = task.rank, title = task.title,
            startTime = task.startTime.format(TIME_FMT),
            endTime = task.endTime.format(TIME_FMT),
            dueInfo = task.dueInfo, statusNote = task.statusNote,
            urgency = task.urgency.name, isDone = task.isDone, isTopFive = task.isTopFive, isMustDo = task.isMustDo,
            displayNumber = task.displayNumber, goalId = task.goalId,
            date = task.date.format(DATE_FMT))
    )

    suspend fun getUndoneTasks(): List<PriorityTask> =
        dao.getUndoneTasks().map { it.toPriorityTask() }

    suspend fun setDoneById(id: Long, done: Boolean) = dao.setDone(id, done)

    suspend fun clearGoalLinks(goalId: Long) = dao.clearGoalLinks(goalId)
}
