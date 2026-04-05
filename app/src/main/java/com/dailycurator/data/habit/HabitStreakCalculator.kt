package com.dailycurator.data.habit

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

private val ISO = DateTimeFormatter.ISO_LOCAL_DATE

object HabitStreakCalculator {

    fun parseDayKeys(keys: List<String>): Set<LocalDate> =
        keys.mapNotNull { runCatching { LocalDate.parse(it, ISO) }.getOrNull() }.toSet()

    /** Streak ending at the most recent completed day (today or yesterday). */
    fun currentStreak(completedDays: Set<LocalDate>, today: LocalDate): Int {
        if (completedDays.isEmpty()) return 0
        var anchor = today
        if (!completedDays.contains(today)) {
            anchor = today.minusDays(1)
            if (!completedDays.contains(anchor)) return 0
        }
        var n = 0
        var d = anchor
        while (completedDays.contains(d)) {
            n++
            d = d.minusDays(1)
        }
        return n
    }

    fun longestStreak(completedDays: Set<LocalDate>): Int {
        if (completedDays.isEmpty()) return 0
        val sorted = completedDays.sorted()
        var best = 1
        var cur = 1
        for (i in 1 until sorted.size) {
            if (sorted[i] == sorted[i - 1].plusDays(1)) {
                cur++
                best = maxOf(best, cur)
            } else {
                cur = 1
            }
        }
        return best
    }

    /** Days in [from, to] inclusive with no completion (daily habits). */
    fun missedDaysDaily(completedDays: Set<LocalDate>, from: LocalDate, to: LocalDate): Int {
        var c = 0
        var d = from
        while (!d.isAfter(to)) {
            if (!completedDays.contains(d)) c++
            d = d.plusDays(1)
        }
        return c
    }

    fun daysInFrequencyWindow(frequency: String, today: LocalDate): Pair<LocalDate, LocalDate> {
        return when (frequency.lowercase()) {
            "weekly" -> today.minusDays(6) to today
            "monthly" -> today.with(TemporalAdjusters.firstDayOfMonth()) to today
            else -> today.minusDays(29) to today
        }
    }
}
