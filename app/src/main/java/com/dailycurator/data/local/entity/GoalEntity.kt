package com.dailycurator.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String? = null,
    val deadline: String? = null,
    val timeEstimate: String? = null,
    val category: String = "Spiritual",
    val isCompleted: Boolean = false,
    val progressPercent: Int = 0,
    val iconEmoji: String? = null,
    val weekStart: String
)
