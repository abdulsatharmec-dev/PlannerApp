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
    /** Marked in the task editor or reminder sheet when the task cannot be completed as planned. */
    val isCantComplete: Boolean = false,
    /**
     * Optional label index shown on the left of the task card. **0** = auto (1, 2, 3… by day order: rank, then start time, then id).
     */
    val displayNumber: Int = 0,
    /** Optional link to a weekly goal (display-only from Goals). */
    val goalId: Long? = null,
    /** Tasks created together from repeat share this id; null if not part of a series. */
    val repeatSeriesId: String? = null,
    val repeatOption: TaskRepeatOption = TaskRepeatOption.NONE,
    val customRepeatIntervalDays: Int = 3,
    /** Inclusive end date for repeating tasks; null means repeat for the default generated window only. */
    val repeatUntilDate: LocalDate? = null,
    val date: LocalDate = LocalDate.now(),
    /** Stored as JSON array in the database; e.g. Work, Personal, Prayer. */
    val tags: List<String> = emptyList(),
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
    val includeInAgentChat: Boolean = true,
    val includeInAssistantInsight: Boolean = true,
    val includeInWeeklyGoalsInsight: Boolean = true,
    /** Shown in the “every day” (pinned) section regardless of entry date. */
    val isEvergreen: Boolean = false,
    /** Path under app files dir for an attached voice note; null if none. */
    val voiceRelativePath: String? = null,
)

data class WeeklyGoal(
    val id: Long = 0,
    val title: String,
    val description: String? = null,
    val deadline: String? = null,
    val timeEstimate: String? = null,
    val category: String = "Spiritual",
    val isCompleted: Boolean = false,
    /** Manual progress 0–100; UI bar uses this until completed (then treated as 100%). */
    val progressPercent: Int = 0,
    /** Optional emoji shown on the goal card. */
    val iconEmoji: String? = null,
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

data class InsightSummarySegment(
    val text: String,
    /** Model hint: default, emphasis, warning, positive, time, muted */
    val tone: String = "default",
)

data class SpiritualNote(
    val source: String,
    val arabic: String,
    val english: String,
)

data class AiInsight(
    val insightText: String,
    val boldPart: String = "",
    val recoveryPlan: String? = null,
    /** Epoch millis when this insight was generated (null = placeholder / not yet generated). */
    val generatedAtEpochMillis: Long? = null,
    /** Calendar day key (ISO date) this insight was generated for. */
    val insightDayKey: String? = null,
    /** Colored inline segments from the model; when null, [insightText] is shown as markdown. */
    val summarySegments: List<InsightSummarySegment>? = null,
    val spiritualNote: SpiritualNote? = null,
)
