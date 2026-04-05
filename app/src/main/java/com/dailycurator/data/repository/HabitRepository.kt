package com.dailycurator.data.repository

import com.dailycurator.data.habit.HabitStreakCalculator
import com.dailycurator.data.local.dao.HabitDao
import com.dailycurator.data.local.dao.HabitLogDao
import com.dailycurator.data.local.entity.HabitEntity
import com.dailycurator.data.local.entity.HabitLogEntity
import com.dailycurator.data.model.Habit
import com.dailycurator.data.model.HabitType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE

private fun HabitEntity.toHabit() = Habit(
    id = id,
    seriesId = seriesId,
    name = name,
    category = category,
    habitType = HabitType.valueOf(habitType),
    iconEmoji = iconEmoji,
    currentValue = currentValue,
    targetValue = targetValue,
    unit = unit,
    trigger = trigger,
    frequency = frequency,
    streakDays = streakDays,
    longestStreak = longestStreak,
    date = LocalDate.parse(date, DATE_FMT),
    isDone = isDone,
    doneNote = doneNote,
)

private fun Habit.toEntity() = HabitEntity(
    id = id,
    seriesId = seriesId,
    name = name,
    category = category,
    habitType = habitType.name,
    iconEmoji = iconEmoji,
    currentValue = currentValue,
    targetValue = targetValue,
    unit = unit,
    trigger = trigger,
    frequency = frequency,
    streakDays = streakDays,
    longestStreak = longestStreak,
    date = date.format(DATE_FMT),
    isDone = isDone,
    doneNote = doneNote,
)

@Singleton
class HabitRepository @Inject constructor(
    private val dao: HabitDao,
    private val logDao: HabitLogDao,
) {
    fun getHabitsForDate(date: LocalDate): Flow<List<Habit>> =
        dao.getHabitsForDate(date.format(DATE_FMT)).map { list ->
            list.map { it.toHabit() }
        }

    suspend fun getById(id: Long): Habit? = dao.getById(id)?.toHabit()

    fun resolveSeriesId(habit: Habit): String =
        habit.seriesId.ifBlank { habit.id.toString() }

    suspend fun insert(habit: Habit): Long {
        val sid = habit.seriesId.ifBlank { UUID.randomUUID().toString() }
        return dao.insert(habit.copy(seriesId = sid).toEntity())
    }

    suspend fun update(habit: Habit) = dao.update(habit.toEntity())

    suspend fun delete(habit: Habit) = dao.delete(habit.toEntity())

    suspend fun deleteSeries(seriesId: String) {
        val sid = seriesId.ifBlank { return }
        logDao.deleteAllForSeries(sid)
        dao.deleteAllForSeries(sid)
    }

    suspend fun markHabitDone(habit: Habit, note: String?) {
        val sid = resolveSeriesId(habit)
        val dayKey = habit.date.format(DATE_FMT)
        val valueCompleted = if (habit.habitType == HabitType.BUILDING) {
            habit.targetValue
        } else {
            habit.currentValue.coerceAtMost(habit.targetValue)
        }
        logDao.insert(
            HabitLogEntity(
                habitSeriesId = sid,
                dayKey = dayKey,
                note = note,
                loggedAtMillis = System.currentTimeMillis(),
                valueCompleted = valueCompleted,
            ),
        )
        val updated = habit.copy(
            isDone = true,
            doneNote = note,
            currentValue = if (habit.habitType == HabitType.BUILDING) habit.targetValue else habit.currentValue,
            seriesId = sid,
        )
        val withStreaks = recalculateStreaks(updated, sid)
        dao.update(withStreaks.toEntity())
    }

    suspend fun updateProgress(habit: Habit, newCurrent: Float) {
        val sid = resolveSeriesId(habit)
        dao.update(
            habit.copy(currentValue = newCurrent.coerceIn(0f, habit.targetValue), seriesId = sid).toEntity(),
        )
    }

    private suspend fun recalculateStreaks(habit: Habit, seriesId: String): Habit {
        val keys = logDao.getAllCompletedDayKeys(seriesId)
        val days = HabitStreakCalculator.parseDayKeys(keys)
        val today = LocalDate.now()
        val cur = HabitStreakCalculator.currentStreak(days, today)
        val longest = maxOf(habit.longestStreak, HabitStreakCalculator.longestStreak(days), cur)
        return habit.copy(streakDays = cur, longestStreak = longest)
    }

    fun observeLogsForSeries(seriesId: String): Flow<List<HabitLogEntity>> =
        logDao.observeRecentForSeries(seriesId, 80)

    suspend fun getCompletedDayKeysForMonth(seriesId: String, month: LocalDate): Set<LocalDate> {
        val start = month.withDayOfMonth(1)
        val end = month.withDayOfMonth(month.lengthOfMonth())
        val keys = logDao.getCompletedDayKeysInRange(
            seriesId,
            start.format(DATE_FMT),
            end.format(DATE_FMT),
        )
        return HabitStreakCalculator.parseDayKeys(keys)
    }

    suspend fun missedInWindow(habit: Habit): Int {
        val sid = resolveSeriesId(habit)
        val keys = logDao.getAllCompletedDayKeys(sid)
        val days = HabitStreakCalculator.parseDayKeys(keys)
        val (from, to) = HabitStreakCalculator.daysInFrequencyWindow(habit.frequency, habit.date)
        return when (habit.frequency.lowercase()) {
            "weekly", "monthly" -> {
                val inWin = days.count { !it.isBefore(from) && !it.isAfter(to) }
                val expected = 1
                (expected - inWin).coerceAtLeast(0)
            }
            else -> HabitStreakCalculator.missedDaysDaily(days, from, to)
        }
    }
}
