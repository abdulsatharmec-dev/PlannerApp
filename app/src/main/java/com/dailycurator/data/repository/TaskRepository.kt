package com.dailycurator.data.repository

import com.dailycurator.data.local.dao.TaskDao
import com.dailycurator.data.local.entity.TaskEntity
import com.dailycurator.data.model.PriorityTask
import com.dailycurator.data.model.TaskRepeatOption
import com.dailycurator.data.model.Urgency
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

private val DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE
private val TIME_FMT = DateTimeFormatter.ofPattern("HH:mm")

private fun String.toLocalTime() = LocalTime.parse(this, TIME_FMT)
private fun String.toLocalDate() = LocalDate.parse(this, DATE_FMT)

private fun String?.toLocalDateOrNull(): LocalDate? =
    this?.takeIf { it.isNotBlank() }?.runCatching { toLocalDate() }?.getOrNull()

private fun String.toTaskRepeatOption(): TaskRepeatOption = try {
    TaskRepeatOption.valueOf(this)
} catch (_: IllegalArgumentException) {
    TaskRepeatOption.NONE
}

private fun parseTagsJson(raw: String?): List<String> {
    val s = raw?.trim().orEmpty()
    if (s.isEmpty() || s == "[]") return emptyList()
    return runCatching {
        val a = JSONArray(s)
        (0 until a.length()).map { a.getString(it).trim() }.filter { it.isNotEmpty() }.distinct()
    }.getOrElse { emptyList() }
}

private fun tagsToJson(tags: List<String>): String {
    val a = JSONArray()
    tags.map { it.trim() }.filter { it.isNotEmpty() }.distinct().forEach { a.put(it) }
    return a.toString()
}

private fun TaskEntity.toPriorityTask() = PriorityTask(
    id = id, rank = rank, title = title,
    startTime = startTime.toLocalTime(),
    endTime = endTime.toLocalTime(),
    dueInfo = dueInfo, statusNote = statusNote,
    urgency = Urgency.valueOf(urgency),
    isDone = isDone, isTopFive = isTopFive, isMustDo = isMustDo,
    isCantComplete = isCantComplete,
    displayNumber = displayNumber,
    goalId = goalId,
    repeatSeriesId = repeatSeriesId,
    repeatOption = repeatOption.toTaskRepeatOption(),
    customRepeatIntervalDays = customRepeatIntervalDays,
    repeatUntilDate = repeatUntilDate.toLocalDateOrNull(),
    date = date.toLocalDate(),
    tags = parseTagsJson(tags),
)

private fun PriorityTask.toEntity() = TaskEntity(
    id = id,
    rank = rank,
    title = title,
    startTime = startTime.format(TIME_FMT),
    endTime = endTime.format(TIME_FMT),
    dueInfo = dueInfo,
    statusNote = statusNote,
    urgency = urgency.name,
    isDone = isDone,
    isTopFive = isTopFive,
    isMustDo = isMustDo,
    isCantComplete = isCantComplete,
    displayNumber = displayNumber,
    goalId = goalId,
    repeatSeriesId = repeatSeriesId,
    repeatOption = repeatOption.name,
    customRepeatIntervalDays = customRepeatIntervalDays,
    repeatUntilDate = repeatUntilDate?.format(DATE_FMT),
    date = date.format(DATE_FMT),
    tags = tagsToJson(tags),
    location = null,
    isProtected = false,
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

    suspend fun getTasksByRepeatSeriesId(seriesId: String): List<PriorityTask> =
        dao.getTasksByRepeatSeriesId(seriesId).map { it.toPriorityTask() }

    /**
     * Tasks created before repeatSeriesId, with the same title and times (and goal) on other days.
     */
    suspend fun findLegacyRepeatSiblings(task: PriorityTask): List<PriorityTask> =
        dao.getTasksByLegacyRepeatFingerprint(
            title = task.title,
            startTime = task.startTime.format(TIME_FMT),
            endTime = task.endTime.format(TIME_FMT),
        ).map { it.toPriorityTask() }
            .filter { it.goalId == task.goalId }

    fun getTasksForGoal(goalId: Long): Flow<List<PriorityTask>> =
        dao.getTasksForGoal(goalId).map { list -> list.map { it.toPriorityTask() } }

    suspend fun nextRankForDate(date: LocalDate): Int {
        val tasks = getTasksForDate(date).first()
        return (tasks.maxOfOrNull { it.rank } ?: 0) + 1
    }

    suspend fun toggleDone(task: PriorityTask) = dao.setDone(task.id, !task.isDone)

    suspend fun insert(task: PriorityTask): Long = dao.insert(task.toEntity())

    suspend fun update(task: PriorityTask) = dao.update(task.toEntity())

    suspend fun delete(task: PriorityTask) = dao.delete(task.toEntity())

    suspend fun getUndoneTasks(): List<PriorityTask> =
        dao.getUndoneTasks().map { it.toPriorityTask() }

    suspend fun setDoneById(id: Long, done: Boolean) = dao.setDone(id, done)

    suspend fun clearGoalLinks(goalId: Long) = dao.clearGoalLinks(goalId)
}
