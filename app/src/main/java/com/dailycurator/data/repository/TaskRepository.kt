package com.dailycurator.data.repository

import com.dailycurator.data.local.dao.TaskDao
import com.dailycurator.data.local.entity.TaskEntity
import com.dailycurator.data.model.PriorityTask
import com.dailycurator.data.model.Urgency
import kotlinx.coroutines.flow.Flow
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

@Singleton
class TaskRepository @Inject constructor(private val dao: TaskDao) {
    fun getTasksForDate(date: LocalDate): Flow<List<PriorityTask>> =
        dao.getTasksForDate(date.format(DATE_FMT)).map { list ->
            list.map { e ->
                PriorityTask(
                    id = e.id, rank = e.rank, title = e.title,
                    startTime = e.startTime.toLocalTime(),
                    endTime = e.endTime.toLocalTime(),
                    dueInfo = e.dueInfo, statusNote = e.statusNote,
                    urgency = Urgency.valueOf(e.urgency),
                    isDone = e.isDone, date = e.date.toLocalDate()
                )
            }
        }

    suspend fun toggleDone(task: PriorityTask) = dao.setDone(task.id, !task.isDone)

    suspend fun insert(task: PriorityTask) = dao.insert(
        TaskEntity(id = task.id, rank = task.rank, title = task.title,
            startTime = task.startTime.format(TIME_FMT),
            endTime = task.endTime.format(TIME_FMT),
            dueInfo = task.dueInfo, statusNote = task.statusNote,
            urgency = task.urgency.name, isDone = task.isDone,
            date = task.date.format(DATE_FMT))
    )

    suspend fun update(task: PriorityTask) = dao.update(
        TaskEntity(id = task.id, rank = task.rank, title = task.title,
            startTime = task.startTime.format(TIME_FMT),
            endTime = task.endTime.format(TIME_FMT),
            dueInfo = task.dueInfo, statusNote = task.statusNote,
            urgency = task.urgency.name, isDone = task.isDone,
            date = task.date.format(DATE_FMT))
    )

    suspend fun delete(task: PriorityTask) = dao.delete(
        TaskEntity(id = task.id, rank = task.rank, title = task.title,
            startTime = task.startTime.format(TIME_FMT),
            endTime = task.endTime.format(TIME_FMT),
            dueInfo = task.dueInfo, statusNote = task.statusNote,
            urgency = task.urgency.name, isDone = task.isDone,
            date = task.date.format(DATE_FMT))
    )
}
