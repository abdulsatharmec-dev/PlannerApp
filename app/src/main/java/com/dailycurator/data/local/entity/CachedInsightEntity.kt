package com.dailycurator.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_insights")
data class CachedInsightEntity(
    @PrimaryKey val type: String,
    val dayKey: String,
    val generatedAtEpochMillis: Long,
    val insightText: String,
    val boldPart: String,
    val recoveryPlan: String?,
    val spiritualSource: String? = null,
    val spiritualArabic: String? = null,
    val spiritualEnglish: String? = null,
    val summarySegmentsJson: String? = null,
)
