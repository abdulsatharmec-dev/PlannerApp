package com.dailycurator.data.model

import java.time.LocalDate
import java.time.LocalTime

enum class Urgency { GREEN, RED, NEUTRAL }
// HabitCategory has been converted to dynamic Strings. Standard ones: Physical, Mental, Spiritual
enum class HabitType { BUILDING, ELIMINATING }
enum class EventPriority { HIGH, MEDIUM, LOW }

data class PriorityTask(
    val id: Long = 0,
    val rank: Int,
    val title: String,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val dueInfo: String? = null,
    val statusNote: String? = null,
    val urgency: Urgency = Urgency.GREEN,
    val isDone: Boolean = false,
    /** Shown under Top 5 on Home (independent of list sort rank). */
    val isTopFive: Boolean = false,
    /** Daily essentials (meals, sleep, etc.) excluded from productive time estimates. */
    val isMustDo: Boolean = false,
    /**
     * Optional label index shown on the left of the task card. **0** = auto (1, 2, 3… by day order: rank, then start time, then id).
     */
    val displayNumber: Int = 0,
    val date: LocalDate = LocalDate.now()
)

data class ScheduleEvent(
    val id: Long = 0,
    val title: String,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val location: String? = null,
    val tags: List<String> = emptyList(),
    val priority: EventPriority = EventPriority.MEDIUM,
    val isProtected: Boolean = false,
    val date: LocalDate = LocalDate.now()
)

data class JournalEntry(
    val id: Long = 0,
    val title: String,
    val body: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

data class WeeklyGoal(
    val id: Long = 0,
    val title: String,
    val description: String? = null,
    val deadline: String? = null,
    val timeEstimate: String? = null,
    val category: String = "Spiritual",
    val isCompleted: Boolean = false,
    val weekStart: LocalDate = LocalDate.now()
)

data class Habit(
    val id: Long = 0,
    /** Stable key for logs, calendar, and analytics (one per logical habit). */
    val seriesId: String = "",
    val name: String,
    val category: String,
    val habitType: HabitType = HabitType.BUILDING,
    val iconEmoji: String,
    val currentValue: Float,
    val targetValue: Float,
    val unit: String,
    val trigger: String? = null,
    val frequency: String = "daily",
    val streakDays: Int = 0,
    val longestStreak: Int = 0,
    val date: LocalDate = LocalDate.now(),
    val isDone: Boolean = false,
    val doneNote: String? = null,
) {
    val progress: Float get() = (currentValue / targetValue).coerceIn(0f, 1f)
    val isGoalMet: Boolean get() = currentValue >= targetValue
    val completionPercent: Int get() = (progress * 100f).toInt().coerceIn(0, 100)
}

data class AiInsight(
    val insightText: String,
    val boldPart: String = "",
    val recoveryPlan: String? = null,
    /** Epoch millis when this insight was generated (null = placeholder / not yet generated). */
    val generatedAtEpochMillis: Long? = null,
    /** Calendar day key (ISO date) this insight was generated for. */
    val insightDayKey: String? = null,
)
