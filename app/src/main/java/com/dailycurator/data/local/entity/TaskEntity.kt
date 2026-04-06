package com.dailycurator.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rank: Int,
    val title: String,
    val startTime: String,
    val endTime: String,
    val dueInfo: String? = null,
    val statusNote: String? = null,
    val urgency: String = "GREEN",
    val isDone: Boolean = false,
    val isTopFive: Boolean = false,
    val isMustDo: Boolean = false,
    val displayNumber: Int = 0,
    val date: String,
    val tags: String = "[]",
    val location: String? = null,
    val isProtected: Boolean = false
)
