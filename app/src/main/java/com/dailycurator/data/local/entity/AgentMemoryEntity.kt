package com.dailycurator.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "agent_memory")
data class AgentMemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val updatedAtEpochMillis: Long,
    /** True if created or last edited by the user; false if produced by automatic extraction. */
    val isManual: Boolean,
)
