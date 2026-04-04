package com.dailycurator.data.model

import java.time.LocalDate
import java.time.LocalTime

enum class Urgency { GREEN, RED, NEUTRAL }
enum class HabitCategory { PHYSICAL, MENTAL, SPIRITUAL }
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
    val name: String,
    val category: HabitCategory,
    val habitType: HabitType = HabitType.BUILDING,
    val iconEmoji: String,
    val currentValue: Float,
    val targetValue: Float,
    val unit: String,
    val streakDays: Int = 0,
    val date: LocalDate = LocalDate.now(),
    val isDone: Boolean = false,
    val doneNote: String? = null
) {
    val progress: Float get() = (currentValue / targetValue).coerceIn(0f, 1f)
    val isGoalMet: Boolean get() = currentValue >= targetValue
}

data class AiInsight(
    val id: Long = 0,
    val insightText: String,
    val boldPart: String = "",
    val recoveryPlan: String? = null,
    val date: LocalDate = LocalDate.now()
)
