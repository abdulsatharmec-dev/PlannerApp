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

@Singleton
class GoalRepository @Inject constructor(private val dao: GoalDao) {
    fun getGoalsForWeek(weekStart: LocalDate): Flow<List<WeeklyGoal>> =
        dao.getGoalsForWeek(weekStart.format(DATE_FMT)).map { list ->
            list.map { e ->
                WeeklyGoal(id = e.id, title = e.title,
                    description = e.description,
                    deadline = e.deadline,
                    timeEstimate = e.timeEstimate,
                    category = e.category,
                    isCompleted = e.isCompleted,
                    weekStart = LocalDate.parse(e.weekStart, DATE_FMT))
            }
        }

    suspend fun toggleCompleted(goal: WeeklyGoal) = dao.setCompleted(goal.id, !goal.isCompleted)

    suspend fun insert(goal: WeeklyGoal) = dao.insert(
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
