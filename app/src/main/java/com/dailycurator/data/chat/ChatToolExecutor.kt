package com.dailycurator.data.chat

import com.dailycurator.data.model.Habit
import com.dailycurator.data.model.HabitType
import com.dailycurator.data.model.PriorityTask
import com.dailycurator.data.model.Urgency
import com.dailycurator.data.model.WeeklyGoal
import com.dailycurator.data.repository.GoalRepository
import com.dailycurator.data.repository.HabitRepository
import com.dailycurator.data.repository.TaskRepository
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

sealed class ToolCallEnvelope {
    data class Done(val toolCallId: String, val content: String) : ToolCallEnvelope()
    data class AwaitDeleteConfirmation(
        val toolCallId: String,
        val deletion: PendingChatDeletion,
    ) : ToolCallEnvelope()
}

@Singleton
class ChatToolExecutor @Inject constructor(
    private val taskRepository: TaskRepository,
    private val goalRepository: GoalRepository,
    private val habitRepository: HabitRepository,
) {

    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
    private val dateFmt = DateTimeFormatter.ISO_LOCAL_DATE

    suspend fun execute(functionName: String, argumentsJson: String, toolCallId: String): ToolCallEnvelope {
        val args = runCatching { JsonParser.parseString(argumentsJson).asJsonObject }.getOrElse {
            return ToolCallEnvelope.Done(toolCallId, """{"ok":false,"error":"invalid_json_arguments"}""")
        }
        return when (functionName) {
            "create_task" -> createTask(args, toolCallId)
            "update_task" -> updateTask(args, toolCallId)
            "delete_task" -> deleteTask(args, toolCallId)
            "toggle_task_done" -> toggleTaskDone(args, toolCallId)
            "create_goal" -> createGoal(args, toolCallId)
            "update_goal" -> updateGoal(args, toolCallId)
            "delete_goal" -> deleteGoal(args, toolCallId)
            "toggle_goal_completed" -> toggleGoal(args, toolCallId)
            "create_habit" -> createHabit(args, toolCallId)
            "update_habit" -> updateHabit(args, toolCallId)
            "delete_habit" -> deleteHabit(args, toolCallId)
            else -> ToolCallEnvelope.Done(toolCallId, """{"ok":false,"error":"unknown_tool"}""")
        }
    }

    private fun today(): LocalDate = LocalDate.now()

    private fun weekStart(d: LocalDate): LocalDate = d.minusDays(d.dayOfWeek.value.toLong() - 1)

    private suspend fun createTask(args: JsonObject, toolCallId: String): ToolCallEnvelope {
        val title = args.optString("title") ?: return err(toolCallId, "title_required")
        val date = args.optString("date")?.let { LocalDate.parse(it, dateFmt) } ?: today()
        val start = args.optString("start_time")?.let { LocalTime.parse(it, timeFmt) } ?: LocalTime.of(9, 0)
        val duration = args.optInt("duration_minutes") ?: 60
        val urgency = parseUrgency(args.optString("urgency"))
        val top = args.optBoolean("top_priority") == true
        val note = args.optString("note")
        val rank = if (top) 1 else taskRepository.nextRankForDate(date)
        val end = start.plusMinutes(duration.toLong())
        val task = PriorityTask(
            rank = rank,
            title = title,
            startTime = start,
            endTime = end,
            statusNote = note,
            urgency = urgency,
            date = date,
        )
        val id = taskRepository.insert(task)
        return ok(toolCallId, "created_task", mapOf("id" to id.toString(), "title" to title))
    }

    private suspend fun updateTask(args: JsonObject, toolCallId: String): ToolCallEnvelope {
        val id = args.optLong("task_id") ?: return err(toolCallId, "task_id_required")
        val existing = taskRepository.getById(id) ?: return err(toolCallId, "task_not_found")
        var t = existing
        args.optString("title")?.let { t = t.copy(title = it) }
        args.optString("date")?.let { t = t.copy(date = LocalDate.parse(it, dateFmt)) }
        var start = t.startTime
        args.optString("start_time")?.let { start = LocalTime.parse(it, timeFmt); t = t.copy(startTime = start) }
        args.optInt("duration_minutes")?.let { dm ->
            t = t.copy(endTime = t.startTime.plusMinutes(dm.toLong()))
        }
        args.optString("urgency")?.let { t = t.copy(urgency = parseUrgency(it)) }
        if (args.optBoolean("top_priority") == true) t = t.copy(rank = 1)
        args.optString("note")?.let { t = t.copy(statusNote = it) }
        args.optBoolean("done")?.let { t = t.copy(isDone = it) }
        taskRepository.update(t)
        return ok(toolCallId, "updated_task", mapOf("id" to id.toString()))
    }

    private suspend fun deleteTask(args: JsonObject, toolCallId: String): ToolCallEnvelope {
        val id = args.optLong("task_id") ?: return err(toolCallId, "task_id_required")
        val task = taskRepository.getById(id) ?: return err(toolCallId, "task_not_found")
        return ToolCallEnvelope.AwaitDeleteConfirmation(
            toolCallId = toolCallId,
            deletion = PendingChatDeletion.Task(id, task.title),
        )
    }

    private suspend fun toggleTaskDone(args: JsonObject, toolCallId: String): ToolCallEnvelope {
        val id = args.optLong("task_id") ?: return err(toolCallId, "task_id_required")
        val task = taskRepository.getById(id) ?: return err(toolCallId, "task_not_found")
        taskRepository.toggleDone(task)
        return ok(toolCallId, "toggled_task_done", mapOf("id" to id.toString(), "done" to (!task.isDone).toString()))
    }

    private suspend fun createGoal(args: JsonObject, toolCallId: String): ToolCallEnvelope {
        val title = args.optString("title") ?: return err(toolCallId, "title_required")
        val ws = weekStart(today())
        val g = WeeklyGoal(
            title = title,
            description = args.optString("description"),
            deadline = args.optString("deadline"),
            category = args.optString("category") ?: "Spiritual",
            timeEstimate = args.optString("time_estimate"),
            weekStart = ws,
        )
        val id = goalRepository.insert(g)
        return ok(toolCallId, "created_goal", mapOf("id" to id.toString()))
    }

    private suspend fun updateGoal(args: JsonObject, toolCallId: String): ToolCallEnvelope {
        val id = args.optLong("goal_id") ?: return err(toolCallId, "goal_id_required")
        val existing = goalRepository.getById(id) ?: return err(toolCallId, "goal_not_found")
        var g = existing
        args.optString("title")?.let { g = g.copy(title = it) }
        args.optString("description")?.let { g = g.copy(description = it) }
        args.optString("deadline")?.let { g = g.copy(deadline = it.ifBlank { null }) }
        args.optString("category")?.let { g = g.copy(category = it) }
        args.optString("time_estimate")?.let { g = g.copy(timeEstimate = it.ifBlank { null }) }
        args.optBoolean("completed")?.let { g = g.copy(isCompleted = it) }
        goalRepository.update(g)
        return ok(toolCallId, "updated_goal", mapOf("id" to id.toString()))
    }

    private suspend fun deleteGoal(args: JsonObject, toolCallId: String): ToolCallEnvelope {
        val id = args.optLong("goal_id") ?: return err(toolCallId, "goal_id_required")
        val g = goalRepository.getById(id) ?: return err(toolCallId, "goal_not_found")
        return ToolCallEnvelope.AwaitDeleteConfirmation(
            toolCallId = toolCallId,
            deletion = PendingChatDeletion.Goal(id, g.title),
        )
    }

    private suspend fun toggleGoal(args: JsonObject, toolCallId: String): ToolCallEnvelope {
        val id = args.optLong("goal_id") ?: return err(toolCallId, "goal_id_required")
        val g = goalRepository.getById(id) ?: return err(toolCallId, "goal_not_found")
        goalRepository.toggleCompleted(g)
        return ok(toolCallId, "toggled_goal", mapOf("id" to id.toString()))
    }

    private suspend fun createHabit(args: JsonObject, toolCallId: String): ToolCallEnvelope {
        val name = args.optString("name") ?: return err(toolCallId, "name_required")
        val type = args.optString("habit_type")?.uppercase()?.let {
            runCatching { HabitType.valueOf(it) }.getOrNull()
        } ?: HabitType.BUILDING
        val target = args.optFloat("target") ?: 1f
        val h = Habit(
            name = name,
            category = args.optString("category") ?: "Physical",
            habitType = type,
            iconEmoji = args.optString("emoji") ?: "⭐",
            currentValue = 0f,
            targetValue = target,
            unit = args.optString("unit") ?: "times",
            trigger = args.optString("trigger"),
            frequency = args.optString("frequency") ?: "daily",
            streakDays = 0,
            date = today(),
        )
        val id = habitRepository.insert(h)
        return ok(toolCallId, "created_habit", mapOf("id" to id.toString()))
    }

    private suspend fun updateHabit(args: JsonObject, toolCallId: String): ToolCallEnvelope {
        val id = args.optLong("habit_id") ?: return err(toolCallId, "habit_id_required")
        val existing = habitRepository.getById(id) ?: return err(toolCallId, "habit_not_found")
        var h = existing
        args.optString("name")?.let { h = h.copy(name = it) }
        args.optFloat("target")?.let { h = h.copy(targetValue = it) }
        args.optString("unit")?.let { h = h.copy(unit = it) }
        args.optFloat("current_value")?.let { h = h.copy(currentValue = it) }
        args.optString("frequency")?.let { h = h.copy(frequency = it) }
        habitRepository.update(h)
        return ok(toolCallId, "updated_habit", mapOf("id" to id.toString()))
    }

    private suspend fun deleteHabit(args: JsonObject, toolCallId: String): ToolCallEnvelope {
        val id = args.optLong("habit_id") ?: return err(toolCallId, "habit_id_required")
        val h = habitRepository.getById(id) ?: return err(toolCallId, "habit_not_found")
        return ToolCallEnvelope.AwaitDeleteConfirmation(
            toolCallId = toolCallId,
            deletion = PendingChatDeletion.Habit(id, h.name),
        )
    }

    private fun parseUrgency(raw: String?): Urgency = when (raw?.uppercase()) {
        "RED" -> Urgency.RED
        "NEUTRAL" -> Urgency.NEUTRAL
        else -> Urgency.GREEN
    }

    private fun ok(toolCallId: String, action: String, fields: Map<String, String>): ToolCallEnvelope {
        val o = JsonObject()
        o.addProperty("ok", true)
        o.addProperty("action", action)
        fields.forEach { (k, v) -> o.addProperty(k, v) }
        return ToolCallEnvelope.Done(toolCallId, o.toString())
    }

    private fun err(toolCallId: String, code: String): ToolCallEnvelope =
        ToolCallEnvelope.Done(toolCallId, """{"ok":false,"error":"$code"}""")

    private fun JsonObject.optString(key: String): String? =
        get(key)?.takeUnless { it.isJsonNull }?.asString?.trim()?.takeIf { it.isNotEmpty() }

    private fun JsonObject.optLong(key: String): Long? {
        val e = get(key) ?: return null
        if (e.isJsonNull) return null
        val p = e.asJsonPrimitive
        return try {
            when {
                p.isNumber -> p.asLong
                else -> p.asString.toLongOrNull()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun JsonObject.optInt(key: String): Int? = optLong(key)?.toInt()

    private fun JsonObject.optBoolean(key: String): Boolean? {
        val e = get(key) ?: return null
        if (e.isJsonNull) return null
        return when {
            e.isJsonPrimitive && e.asJsonPrimitive.isBoolean -> e.asBoolean
            e.isJsonPrimitive -> e.asString.equals("true", true) || e.asString == "1"
            else -> null
        }
    }

    private fun JsonObject.optFloat(key: String): Float? {
        val e = get(key) ?: return null
        if (e.isJsonNull) return null
        return when {
            e.isJsonPrimitive && e.asJsonPrimitive.isNumber -> e.asFloat
            e.isJsonPrimitive -> e.asString.toFloatOrNull()
            else -> null
        }
    }
}
