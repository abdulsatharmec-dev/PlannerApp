package com.dailycurator.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pomodoro_sessions",
    indices = [Index(value = ["entityType", "entityId"]), Index(value = ["endedAtMillis"])],
)
data class PomodoroSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** TASK, GOAL, HABIT, or FREE */
    val entityType: String,
    /** Row id for task/goal/today's habit; 0 for FREE */
    val entityId: Long = 0,
    /** Habit series id when entityType is HABIT (for stable analytics). */
    val habitSeriesId: String? = null,
    val title: String,
    val startedAtMillis: Long,
    val endedAtMillis: Long? = null,
    val plannedDurationSeconds: Int,
    val actualFocusedSeconds: Int = 0,
    val completed: Boolean = false,
)
