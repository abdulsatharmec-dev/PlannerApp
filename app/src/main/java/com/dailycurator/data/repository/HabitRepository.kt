package com.dailycurator.data.repository

import com.dailycurator.data.local.dao.HabitDao
import com.dailycurator.data.local.entity.HabitEntity
import com.dailycurator.data.model.Habit
import com.dailycurator.data.model.HabitCategory
import com.dailycurator.data.model.HabitType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

private val DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE

@Singleton
class HabitRepository @Inject constructor(private val dao: HabitDao) {
    fun getHabitsForDate(date: LocalDate): Flow<List<Habit>> =
        dao.getHabitsForDate(date.format(DATE_FMT)).map { list ->
            list.map { e ->
                Habit(id = e.id, name = e.name,
                    category = HabitCategory.valueOf(e.category),
                    habitType = HabitType.valueOf(e.habitType),
                    iconEmoji = e.iconEmoji,
                    currentValue = e.currentValue, targetValue = e.targetValue,
                    unit = e.unit, streakDays = e.streakDays,
                    date = LocalDate.parse(e.date, DATE_FMT))
            }
        }

    suspend fun insert(habit: Habit) = dao.insert(
        HabitEntity(name = habit.name, category = habit.category.name,
            habitType = habit.habitType.name, iconEmoji = habit.iconEmoji,
            currentValue = habit.currentValue, targetValue = habit.targetValue,
            unit = habit.unit, streakDays = habit.streakDays,
            date = habit.date.format(DATE_FMT))
    )

    suspend fun update(entity: HabitEntity) = dao.update(entity)
    suspend fun delete(entity: HabitEntity) = dao.delete(entity)
}
