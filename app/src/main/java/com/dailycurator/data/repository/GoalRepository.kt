package com.dailycurator.data.repository

import com.dailycurator.data.local.dao.GoalDao
import com.dailycurator.data.local.entity.GoalEntity
import com.dailycurator.data.model.WeeklyGoal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

private val DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE

private fun GoalEntity.toWeeklyGoal() = WeeklyGoal(
    id = id, title = title,
    description = description,
    deadline = deadline,
    timeEstimate = timeEstimate,
    category = category,
    isCompleted = isCompleted,
    progressPercent = progressPercent,
    iconEmoji = iconEmoji,
    weekStart = LocalDate.parse(weekStart, DATE_FMT),
)

private fun WeeklyGoal.toEntity() = GoalEntity(
    id = id, title = title, description = description,
    deadline = deadline, timeEstimate = timeEstimate,
    category = category, isCompleted = isCompleted,
    progressPercent = progressPercent.coerceIn(0, 100),
    iconEmoji = iconEmoji?.trim()?.takeIf { it.isNotEmpty() },
    weekStart = weekStart.format(DATE_FMT),
)

@Singleton
class GoalRepository @Inject constructor(private val dao: GoalDao) {
    fun getGoalsForWeek(weekStart: LocalDate): Flow<List<WeeklyGoal>> =
        dao.getGoalsForWeek(weekStart.format(DATE_FMT)).map { list ->
            list.map { it.toWeeklyGoal() }
        }

    fun getActiveGoals(): Flow<List<WeeklyGoal>> =
        dao.getActiveGoals().map { list -> list.map { it.toWeeklyGoal() } }

    suspend fun getById(id: Long): WeeklyGoal? = dao.getById(id)?.toWeeklyGoal()

    suspend fun toggleCompleted(goal: WeeklyGoal) = dao.setCompleted(goal.id, !goal.isCompleted)

    suspend fun insert(goal: WeeklyGoal): Long = dao.insert(goal.toEntity())

    suspend fun update(goal: WeeklyGoal) = dao.update(goal.toEntity())

    suspend fun delete(goal: WeeklyGoal) = dao.delete(goal.toEntity())
}
