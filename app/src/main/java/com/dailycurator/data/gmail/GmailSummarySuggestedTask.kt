package com.dailycurator.data.gmail

import com.dailycurator.data.model.Urgency

/**
 * Action item proposed from the mailbox summary markdown (LLM extraction).
 */
data class GmailSummarySuggestedTask(
    val title: String,
    val detail: String,
    val urgency: Urgency,
)
