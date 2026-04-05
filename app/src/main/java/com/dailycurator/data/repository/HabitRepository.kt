package com.dailycurator.data.repository

import com.dailycurator.data.local.dao.HabitDao
import com.dailycurator.data.local.entity.HabitEntity
import com.dailycurator.data.model.Habit

import com.dailycurator.data.model.HabitType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

private val DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE

private fun HabitEntity.toHabit() = Habit(
    id = id, name = name,
    category = category,
    habitType = HabitType.valueOf(habitType),
    iconEmoji = iconEmoji,
    currentValue = currentValue, targetValue = targetValue,
    unit = unit, trigger = trigger, frequency = frequency, streakDays = streakDays,
    date = LocalDate.parse(date, DATE_FMT),
    isDone = isDone, doneNote = doneNote,
)

@Singleton
class HabitRepository @Inject constructor(private val dao: HabitDao) {
    fun getHabitsForDate(date: LocalDate): Flow<List<Habit>> =
        dao.getHabitsForDate(date.format(DATE_FMT)).map { list ->
            list.map { it.toHabit() }
        }

    suspend fun getById(id: Long): Habit? = dao.getById(id)?.toHabit()

    suspend fun insert(habit: Habit): Long = dao.insert(
        HabitEntity(id = habit.id, name = habit.name, category = habit.category,
            habitType = habit.habitType.name, iconEmoji = habit.iconEmoji,
            currentValue = habit.currentValue, targetValue = habit.targetValue,
            unit = habit.unit, trigger = habit.trigger, frequency = habit.frequency, streakDays = habit.streakDays,
            date = habit.date.format(DATE_FMT), isDone = habit.isDone, doneNote = habit.doneNote)
    )

    suspend fun update(entity: HabitEntity) = dao.update(entity)
    suspend fun delete(entity: HabitEntity) = dao.delete(entity)

    suspend fun update(habit: Habit) = dao.update(
        HabitEntity(id = habit.id, name = habit.name, category = habit.category,
            habitType = habit.habitType.name, iconEmoji = habit.iconEmoji,
            currentValue = habit.currentValue, targetValue = habit.targetValue,
            unit = habit.unit, trigger = habit.trigger, frequency = habit.frequency, streakDays = habit.streakDays,
            date = habit.date.format(DATE_FMT), isDone = habit.isDone, doneNote = habit.doneNote)
    )

    suspend fun delete(habit: Habit) = dao.delete(
        HabitEntity(id = habit.id, name = habit.name, category = habit.category,
            habitType = habit.habitType.name, iconEmoji = habit.iconEmoji,
            currentValue = habit.currentValue, targetValue = habit.targetValue,
            unit = habit.unit, trigger = habit.trigger, frequency = habit.frequency, streakDays = habit.streakDays,
            date = habit.date.format(DATE_FMT), isDone = habit.isDone, doneNote = habit.doneNote)
    )

    suspend fun markHabitDone(habit: Habit, note: String?) {
        update(habit.copy(isDone = true, doneNote = note))
    }
}
