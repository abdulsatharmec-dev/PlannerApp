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
    weekStart = LocalDate.parse(weekStart, DATE_FMT),
)

@Singleton
class GoalRepository @Inject constructor(private val dao: GoalDao) {
    fun getGoalsForWeek(weekStart: LocalDate): Flow<List<WeeklyGoal>> =
        dao.getGoalsForWeek(weekStart.format(DATE_FMT)).map { list ->
            list.map { it.toWeeklyGoal() }
        }

    suspend fun getById(id: Long): WeeklyGoal? = dao.getById(id)?.toWeeklyGoal()

    suspend fun toggleCompleted(goal: WeeklyGoal) = dao.setCompleted(goal.id, !goal.isCompleted)

    suspend fun insert(goal: WeeklyGoal): Long = dao.insert(
        GoalEntity(id = goal.id, title = goal.title, description = goal.description,
            deadline = goal.deadline, timeEstimate = goal.timeEstimate,
            category = goal.category, isCompleted = goal.isCompleted,
            weekStart = goal.weekStart.format(DATE_FMT))
    )

    suspend fun update(goal: WeeklyGoal) = dao.update(
        GoalEntity(id = goal.id, title = goal.title, description = goal.description,
            deadline = goal.deadline, timeEstimate = goal.timeEstimate,
            category = goal.category, isCompleted = goal.isCompleted,
            weekStart = goal.weekStart.format(DATE_FMT))
    )

    suspend fun delete(goal: WeeklyGoal) = dao.delete(
        GoalEntity(id = goal.id, title = goal.title, description = goal.description,
            deadline = goal.deadline, timeEstimate = goal.timeEstimate,
            category = goal.category, isCompleted = goal.isCompleted,
            weekStart = goal.weekStart.format(DATE_FMT))
    )
}
