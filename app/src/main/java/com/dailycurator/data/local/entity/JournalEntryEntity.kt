package com.dailycurator.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "journal_entries")
data class JournalEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val body: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    /** When true, this entry may appear in agent chat (if global setting + date window allow). */
    val includeInAgentChat: Boolean = true,
    val includeInAssistantInsight: Boolean = true,
    val includeInWeeklyGoalsInsight: Boolean = true,
    /** Affirmations / reminders: listed on every day in the journal browser. */
    val isEvergreen: Boolean = false,
)
