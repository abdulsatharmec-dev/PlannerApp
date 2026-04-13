package com.dailycurator.data.local.entity

import androidx.room.ColumnInfo
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
    @ColumnInfo(index = true) val goalId: Long? = null,
    /** Shared id for tasks created from one repeating add/edit; null = standalone. */
    @ColumnInfo(index = true) val repeatSeriesId: String? = null,
    val repeatOption: String = "NONE",
    val customRepeatIntervalDays: Int = 3,
    /** Last calendar day to include for this repeat (ISO date); null = use built-in max span only. */
    val repeatUntilDate: String? = null,
    val date: String,
    val tags: String = "[]",
    val location: String? = null,
    val isProtected: Boolean = false
)
