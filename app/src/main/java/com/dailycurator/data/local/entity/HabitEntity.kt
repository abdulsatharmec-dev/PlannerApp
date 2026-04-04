package com.dailycurator.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    val habitType: String = "BUILDING",
    val iconEmoji: String,
    val currentValue: Float,
    val targetValue: Float,
    val unit: String,
    val streakDays: Int = 0,
    val date: String,
    val isDone: Boolean = false,
    val doneNote: String? = null
)
