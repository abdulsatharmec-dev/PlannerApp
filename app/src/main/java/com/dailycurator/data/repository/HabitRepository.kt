package com.dailycurator.data.repository

import com.dailycurator.data.habit.HabitStreakCalculator
import com.dailycurator.data.local.dao.HabitDao
import com.dailycurator.data.local.dao.HabitLogDao
import com.dailycurator.data.local.entity.HabitEntity
import com.dailycurator.data.local.entity.HabitLogEntity
import com.dailycurator.data.model.Habit
import com.dailycurator.data.model.HabitType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE

private fun seriesKey(entity: HabitEntity): String =
    entity.seriesId.trim().ifBlank { entity.id.toString() }

private fun latestPerSeries(entities: List<HabitEntity>): List<HabitEntity> {
    if (entities.isEmpty()) return emptyList()
    return entities
        .groupBy { seriesKey(it) }
        .values
        .map { group ->
            group.maxWith(
                compareBy<HabitEntity> { LocalDate.parse(it.date, DATE_FMT) }
                    .thenBy { it.id },
            )
        }
        .sortedBy { it.name.lowercase() }
}

private fun mergeHabitsForDate(
    date: LocalDate,
    all: List<HabitEntity>,
    dayLogs: List<HabitLogEntity>,
): List<Habit> {
    val latest = latestPerSeries(all)
    val logBySeries = dayLogs.associateBy { it.habitSeriesId }
    return latest.map { e ->
        val template = e.toHabit()
        val sid = seriesKey(e)
        val log = logBySeries[sid]
        val cv = log?.valueCompleted ?: 0f
        val isDone = when (template.habitType) {
            HabitType.BUILDING -> log != null && cv >= template.targetValue - 1e-3f
            HabitType.ELIMINATING -> log != null
        }
        template.copy(
            date = date,
            currentValue = cv,
            isDone = isDone,
            doneNote = log?.note,
        )
    }
}

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
    fun getHabitsForDate(date: LocalDate): Flow<List<Habit>> = combine(
        dao.observeAll(),
        logDao.observeLogsForDay(date.format(DATE_FMT)),
    ) { all, logs ->
        mergeHabitsForDate(date, all, logs)
    }

    suspend fun getById(id: Long): Habit? = dao.getById(id)?.toHabit()

    fun resolveSeriesId(habit: Habit): String =
        habit.seriesId.ifBlank { habit.id.toString() }

    suspend fun insert(habit: Habit): Long {
        val sid = habit.seriesId.ifBlank { UUID.randomUUID().toString() }
        return dao.insert(habit.copy(seriesId = sid).toEntity())
    }

    suspend fun update(habit: Habit) {
        val sid = resolveSeriesId(habit)
        val rows = dao.getAllForSeries(sid)
        val targets = rows.ifEmpty { listOfNotNull(dao.getById(habit.id)) }
        for (entity in targets) {
            dao.update(
                entity.copy(
                    name = habit.name,
                    category = habit.category,
                    habitType = habit.habitType.name,
                    iconEmoji = habit.iconEmoji,
                    targetValue = habit.targetValue,
                    unit = habit.unit,
                    trigger = habit.trigger,
                    frequency = habit.frequency,
                ),
            )
        }
    }

    suspend fun delete(habit: Habit) = dao.delete(habit.toEntity())

    suspend fun deleteSeries(seriesId: String) {
        val sid = seriesId.ifBlank { return }
        logDao.deleteAllForSeries(sid)
        dao.deleteAllForSeries(sid)
    }

    suspend fun markHabitDone(habit: Habit, note: String?) {
        val sid = resolveSeriesId(habit)
        val dayKey = habit.date.format(DATE_FMT)
        val templateEntity = resolveTemplateEntity(habit) ?: return
        val template = templateEntity.toHabit()
        val valueCompleted = if (template.habitType == HabitType.BUILDING) {
            template.targetValue
        } else {
            habit.currentValue.coerceAtMost(template.targetValue)
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
        val withStreaks = recalculateStreaks(template, sid)
        dao.update(
            templateEntity.copy(
                streakDays = withStreaks.streakDays,
                longestStreak = withStreaks.longestStreak,
            ),
        )
    }

    private suspend fun resolveTemplateEntity(habit: Habit): HabitEntity? {
        val sid = resolveSeriesId(habit)
        return dao.getLatestForSeries(sid) ?: dao.getById(habit.id)
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
