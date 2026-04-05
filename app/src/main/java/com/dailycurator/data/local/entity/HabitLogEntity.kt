package com.dailycurator.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "habit_logs",
    indices = [Index(value = ["habitSeriesId", "dayKey"], unique = true)],
)
data class HabitLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitSeriesId: String,
    /** ISO local date (yyyy-MM-dd). */
    val dayKey: String,
    val note: String? = null,
    val loggedAtMillis: Long,
    /** Progress value recorded for this completion (e.g. liters, minutes). */
    val valueCompleted: Float,
)
