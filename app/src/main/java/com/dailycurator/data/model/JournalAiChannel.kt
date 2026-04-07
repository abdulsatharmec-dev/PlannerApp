package com.dailycurator.data.model

/** Which AI surface may consume a journal entry (after global + date-window filters). */
enum class JournalAiChannel {
    AgentChat,
    AssistantInsight,
    WeeklyGoalsInsight,
}
