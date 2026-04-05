package com.dailycurator.data.chat

import com.dailycurator.data.local.AppPreferences
import com.dailycurator.data.remote.CerebrasToolDefinition
import com.google.gson.JsonObject

private fun obj(vararg props: Pair<String, Any?>): JsonObject {
    val o = JsonObject()
    props.forEach { (k, v) ->
        when (v) {
            null -> o.add(k, null)
            is String -> o.addProperty(k, v)
            is Number -> o.addProperty(k, v.toDouble())
            is Boolean -> o.addProperty(k, v)
            is JsonObject -> o.add(k, v)
            else -> o.addProperty(k, v.toString())
        }
    }
    return o
}

private fun stringProp(desc: String) = obj("type" to "string", "description" to desc)
private fun intProp(desc: String) = obj("type" to "integer", "description" to desc)
private fun numberProp(desc: String) = obj("type" to "number", "description" to desc)
private fun boolProp(desc: String) = obj("type" to "boolean", "description" to desc)

private fun schema(properties: Map<String, JsonObject>, required: List<String> = emptyList()) = JsonObject().apply {
    addProperty("type", "object")
    val p = JsonObject()
    properties.forEach { (k, v) -> p.add(k, v) }
    add("properties", p)
    if (required.isNotEmpty()) {
        val arr = com.google.gson.JsonArray()
        required.forEach { arr.add(it) }
        add("required", arr)
    }
}

fun cerebrasChatToolDefinitions(): List<CerebrasToolDefinition> {
    val tools = mutableListOf<CerebrasToolDefinition>()
    fun add(name: String, description: String, parameters: JsonObject) {
        tools.add(CerebrasToolDefinition(name = name, description = description, parametersSchema = parameters))
    }
    add(
        "create_task",
        "Create a task on a given day with start time and duration.",
        schema(
            mapOf(
                "title" to stringProp("Task title (required)"),
                "date" to stringProp("ISO date yyyy-MM-dd; default today"),
                "start_time" to stringProp("24h time HH:mm; default 09:00"),
                "duration_minutes" to intProp("Length in minutes; default 60"),
                "urgency" to stringProp("GREEN, RED, or NEUTRAL; default GREEN"),
                "top_priority" to boolProp("If true, rank as top priority (rank 1)"),
                "note" to stringProp("Optional status note"),
            ),
            listOf("title"),
        ),
    )
    add(
        "update_task",
        "Update an existing task by id. Only include fields to change.",
        schema(
            mapOf(
                "task_id" to intProp("Task id"),
                "title" to stringProp("New title"),
                "date" to stringProp("ISO date yyyy-MM-dd"),
                "start_time" to stringProp("HH:mm"),
                "duration_minutes" to intProp("New duration from start"),
                "urgency" to stringProp("GREEN, RED, NEUTRAL"),
                "top_priority" to boolProp("Set rank 1 if true"),
                "note" to stringProp("Status note"),
                "done" to boolProp("Mark done or open"),
            ),
            listOf("task_id"),
        ),
    )
    add(
        "delete_task",
        "Request deleting a task. User must confirm in the app before deletion runs.",
        schema(mapOf("task_id" to intProp("Task id")), listOf("task_id")),
    )
    add(
        "toggle_task_done",
        "Toggle task completion for today.",
        schema(mapOf("task_id" to intProp("Task id")), listOf("task_id")),
    )
    add(
        "create_goal",
        "Create a weekly goal for the current week.",
        schema(
            mapOf(
                "title" to stringProp("Goal title"),
                "description" to stringProp("Optional description"),
                "deadline" to stringProp("Optional ISO yyyy-MM-dd deadline"),
                "category" to stringProp("Category; default Spiritual"),
                "time_estimate" to stringProp("e.g. 2h"),
            ),
            listOf("title"),
        ),
    )
    add(
        "update_goal",
        "Update a weekly goal by id.",
        schema(
            mapOf(
                "goal_id" to intProp("Goal id"),
                "title" to stringProp("New title"),
                "description" to stringProp("Description"),
                "deadline" to stringProp("ISO date or empty to clear"),
                "category" to stringProp("Category"),
                "time_estimate" to stringProp("Time estimate text"),
                "completed" to boolProp("Mark completed"),
            ),
            listOf("goal_id"),
        ),
    )
    add(
        "delete_goal",
        "Request deleting a goal. Requires user confirmation.",
        schema(mapOf("goal_id" to intProp("Goal id")), listOf("goal_id")),
    )
    add(
        "toggle_goal_completed",
        "Toggle weekly goal completed state.",
        schema(mapOf("goal_id" to intProp("Goal id")), listOf("goal_id")),
    )
    add(
        "create_habit",
        "Create a habit for today.",
        schema(
            mapOf(
                "name" to stringProp("Habit name"),
                "category" to stringProp("e.g. Physical"),
                "habit_type" to stringProp("BUILDING or ELIMINATING"),
                "emoji" to stringProp("Icon emoji"),
                "target" to numberProp("Target (number)"),
                "unit" to stringProp("times, mins, hours, etc."),
                "trigger" to stringProp("Optional trigger text"),
                "frequency" to stringProp("daily, weekly, weekdays; default daily"),
            ),
            listOf("name"),
        ),
    )
    add(
        "update_habit",
        "Update habit by id.",
        schema(
            mapOf(
                "habit_id" to intProp("Habit id"),
                "name" to stringProp("New name"),
                "target" to numberProp("New target"),
                "unit" to stringProp("Unit"),
                "current_value" to numberProp("Current progress value"),
                "frequency" to stringProp("Frequency"),
            ),
            listOf("habit_id"),
        ),
    )
    add(
        "delete_habit",
        "Request deleting a habit. Requires user confirmation.",
        schema(mapOf("habit_id" to intProp("Habit id")), listOf("habit_id")),
    )
    return tools
}

/** Gmail tools for the AI agent (mirrors Koog ToolRegistry-style registration; gated in [AppPreferences]). */
fun gmailAgentChatTools(prefs: AppPreferences): List<CerebrasToolDefinition> {
    if (prefs.getGmailLinkedAccounts().isEmpty()) return emptyList()
    val tools = mutableListOf<CerebrasToolDefinition>()
    fun add(name: String, description: String, parameters: JsonObject) {
        tools.add(CerebrasToolDefinition(name = name, description = description, parametersSchema = parameters))
    }
    if (prefs.isAgentGmailReadEnabled()) {
        add(
            "gmail_list_messages",
            "List Gmail message ids and short metadata for the user's linked Google account. Search uses Gmail's query syntax (e.g. newer_than:7d from:recruiter).",
            schema(
                mapOf(
                    "account_email" to stringProp("Linked Gmail address; omit to use the first linked account"),
                    "search_query" to stringProp("Gmail search query; default newer_than:7d"),
                    "max_results" to intProp("Max messages to return; default 15, max 25"),
                ),
            ),
        )
        add(
            "gmail_get_message",
            "Fetch one Gmail message: headers, snippet, and optionally full plain-text body for a linked account.",
            schema(
                mapOf(
                    "account_email" to stringProp("Linked Gmail address; omit for default"),
                    "message_id" to stringProp("Gmail API message id"),
                    "include_full_text" to boolProp("If true, include decoded text/plain body (may be truncated)"),
                ),
                listOf("message_id"),
            ),
        )
    }
    if (prefs.isAgentGmailSendEnabled()) {
        add(
            "gmail_send_email",
            "Send a plain-text email from a linked Gmail account (requires send permission in Settings).",
            schema(
                mapOf(
                    "account_email" to stringProp("Linked Gmail address; omit for default"),
                    "to" to stringProp("Recipient email"),
                    "subject" to stringProp("Subject line"),
                    "body" to stringProp("Plain text body"),
                ),
                listOf("to", "subject", "body"),
            ),
        )
    }
    return tools
}
