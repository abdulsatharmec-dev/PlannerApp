package com.dailycurator.data.repository

import com.dailycurator.data.ai.InsightType
import com.dailycurator.data.ai.JournalContextFormatter
import com.dailycurator.data.ai.parseInsightBundle
import com.dailycurator.data.ai.parseInsightJson
import com.dailycurator.data.local.AppPreferences
import com.dailycurator.data.local.dao.CachedInsightDao
import com.dailycurator.data.local.entity.CachedInsightEntity
import com.dailycurator.data.model.AiInsight
import com.dailycurator.data.model.InsightSummarySegment
import com.dailycurator.data.model.JournalAiChannel
import com.dailycurator.data.model.SpiritualNote
import com.dailycurator.data.remote.CerebrasApiException
import com.dailycurator.data.remote.CerebrasChatMessage
import com.dailycurator.data.remote.CerebrasRestClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.google.gson.JsonParser
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InsightCacheRepository @Inject constructor(
    private val dao: CachedInsightDao,
    private val cerebras: CerebrasRestClient,
    private val prefs: AppPreferences,
    private val taskRepository: TaskRepository,
    private val habitRepository: HabitRepository,
    private val goalRepository: GoalRepository,
    private val journalRepository: JournalRepository,
    private val phoneUsageRepository: PhoneUsageRepository,
) {

    private val assistantMutex = Mutex()
    private val weeklyMutex = Mutex()

    fun observeAssistant(): Flow<CachedInsightEntity?> = dao.observe(InsightType.ASSISTANT)

    fun observeWeeklyGoals(): Flow<CachedInsightEntity?> = dao.observe(InsightType.WEEKLY_GOALS)

    suspend fun ensureAssistantForTodayIfNeeded() {
        if (!prefs.isAssistantInsightEnabled()) return
        if (!prefs.isLlmConfigured()) return
        assistantMutex.withLock {
            val dayKey = LocalDate.now().toString()
            val existing = dao.get(InsightType.ASSISTANT)
            if (existing?.dayKey == dayKey) return
            runCatching { generateAssistant(dayKey) }
        }
    }

    suspend fun regenerateAssistant(): Result<Unit> = assistantMutex.withLock {
        if (!prefs.isAssistantInsightEnabled()) {
            return@withLock Result.failure(IllegalStateException("Assistant insight is disabled in Settings."))
        }
        if (!prefs.isLlmConfigured()) {
            return@withLock Result.failure(IllegalStateException("Add an LLM API key in Settings."))
        }
        val dayKey = LocalDate.now().toString()
        runCatching { generateAssistant(dayKey) }.map { }
    }

    suspend fun ensureWeeklyGoalsForTodayIfNeeded() {
        if (!prefs.isWeeklyGoalsInsightEnabled()) return
        if (!prefs.isLlmConfigured()) return
        weeklyMutex.withLock {
            val dayKey = LocalDate.now().toString()
            val existing = dao.get(InsightType.WEEKLY_GOALS)
            if (existing?.dayKey == dayKey) return
            runCatching { generateWeeklyGoals(dayKey) }
        }
    }

    suspend fun regenerateWeeklyGoals(): Result<Unit> = weeklyMutex.withLock {
        if (!prefs.isWeeklyGoalsInsightEnabled()) {
            return@withLock Result.failure(IllegalStateException("Weekly goals insight is disabled in Settings."))
        }
        if (!prefs.isLlmConfigured()) {
            return@withLock Result.failure(IllegalStateException("Add an LLM API key in Settings."))
        }
        val dayKey = LocalDate.now().toString()
        runCatching { generateWeeklyGoals(dayKey) }.map { }
    }

    private suspend fun generateAssistant(dayKey: String) {
        val today = LocalDate.now()
        val weekStart = today.minusDays(today.dayOfWeek.value.toLong() - 1)
        val tasks = taskRepository.getTasksForDate(today).first()
        val habits = habitRepository.getHabitsForDate(today).first()
        val goals = goalRepository.getGoalsForWeek(weekStart).first()
        val ctx = buildAssistantContext(
            today,
            tasks,
            habits,
            goals,
            includeJournal = prefs.isJournalInAssistantInsight(),
            includePhoneUsage = prefs.isPhoneUsageInAssistantInsight(),
        )
        val system = prefs.getAssistantInsightPrompt()
        val content = completeInsightJson(system, ctx, maxTokens = 2048)
        val b = parseInsightBundle(content)
        dao.upsert(
            CachedInsightEntity(
                type = InsightType.ASSISTANT,
                dayKey = dayKey,
                generatedAtEpochMillis = System.currentTimeMillis(),
                insightText = b.summary,
                boldPart = b.boldHeadline,
                recoveryPlan = b.recoveryOrStrategy,
                spiritualSource = b.spiritualSource,
                spiritualArabic = b.spiritualArabic,
                spiritualEnglish = b.spiritualEnglish,
                summarySegmentsJson = b.summarySegmentsJson,
            ),
        )
    }

    private suspend fun generateWeeklyGoals(dayKey: String) {
        val today = LocalDate.now()
        val weekStart = today.minusDays(today.dayOfWeek.value.toLong() - 1)
        val goals = goalRepository.getGoalsForWeek(weekStart).first()
        val ctx = buildWeeklyGoalsContext(
            weekStart,
            goals,
            includeJournal = prefs.isJournalInWeeklyGoalsInsight(),
            includePhoneUsage = prefs.isPhoneUsageInWeeklyGoalsInsight(),
        )
        val system = prefs.getWeeklyGoalsInsightPrompt()
        val content = completeInsightJson(system, ctx, maxTokens = 1024)
        val (bold, summary, recovery) = parseInsightJson(content)
        dao.upsert(
            CachedInsightEntity(
                type = InsightType.WEEKLY_GOALS,
                dayKey = dayKey,
                generatedAtEpochMillis = System.currentTimeMillis(),
                insightText = summary,
                boldPart = bold,
                recoveryPlan = recovery,
                spiritualSource = null,
                spiritualArabic = null,
                spiritualEnglish = null,
                summarySegmentsJson = null,
            ),
        )
    }

    private suspend fun completeInsightJson(
        systemPrompt: String,
        userContext: String,
        maxTokens: Int = 1024,
    ): String {
        val result = cerebras.chatCompletion(
            messages = listOf(
                CerebrasChatMessage(role = "system", content = systemPrompt),
                CerebrasChatMessage(role = "user", content = userContext),
            ),
            tools = null,
            temperature = 0.35,
            maxTokens = maxTokens,
        )
        val msg = result.message ?: throw CerebrasApiException("Empty model response")
        return msg.content?.takeIf { it.isNotBlank() }
            ?: throw CerebrasApiException("Model returned no text")
    }

    private suspend fun buildAssistantContext(
        today: LocalDate,
        tasks: List<com.dailycurator.data.model.PriorityTask>,
        habits: List<com.dailycurator.data.model.Habit>,
        goals: List<com.dailycurator.data.model.WeeklyGoal>,
        includeJournal: Boolean,
        includePhoneUsage: Boolean,
    ): String {
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val nowDt = LocalDateTime.now()
        val time = nowDt.format(fmt)
        val dw = prefs.getDayWindow()
        val wStart = LocalTime.of(dw.startMinute / 60, dw.startMinute % 60)
        val wEnd = LocalTime.of(dw.endMinute / 60, dw.endMinute % 60)
        return buildString {
            appendLine("=== TIME AWARENESS (required for the insight) ===")
            appendLine("Current local: $time (${nowDt.dayOfWeek}), calendar date $today.")
            appendLine("User's configured active day window (same calendar day): approximately $wStart – $wEnd.")
            appendLine("Compare NOW to each task's start/end. Flag open tasks whose end time is already before NOW as missed/overdue.")
            appendLine("Say what can still be done before ~$wEnd. Be specific using task titles and times from the data below.")
            appendLine()
            appendLine("--- TASKS TODAY ---")
            if (tasks.isEmpty()) appendLine("None")
            tasks.forEach {
                val status = if (it.isDone) "done" else "open"
                appendLine("- [$status] #${it.id} rank=${it.rank} ${it.title} ${it.startTime}-${it.endTime} urgency=${it.urgency} note=${it.statusNote ?: ""}")
            }
            appendLine()
            appendLine("--- HABITS TODAY ---")
            if (habits.isEmpty()) appendLine("None")
            habits.forEach {
                val status = if (it.isDone) "done" else "open"
                appendLine("- [$status] #${it.id} ${it.name} progress=${it.currentValue}/${it.targetValue} ${it.unit} type=${it.habitType} streak=${it.streakDays}d")
            }
            appendLine()
            appendLine("--- WEEKLY GOALS (this week) ---")
            if (goals.isEmpty()) appendLine("None")
            goals.forEach {
                val st = if (it.isCompleted) "done" else "open"
                appendLine("- [$st] #${it.id} ${it.title} | ${it.description ?: ""} | deadline=${it.deadline} | est=${it.timeEstimate} | cat=${it.category}")
            }
            if (includeJournal) {
                val zone = ZoneId.systemDefault()
                val journal = journalRepository.getJournalForAiChannel(
                    JournalAiChannel.AssistantInsight,
                    today,
                    prefs.getJournalContextWindowDays(),
                    zone,
                    30,
                )
                appendLine()
                appendLine("--- JOURNAL (date window + per-entry toggles; assistant insight) ---")
                appendLine(JournalContextFormatter.format(journal))
            }
            if (includePhoneUsage) {
                val usage = phoneUsageRepository.buildCompactUsageContextBlock(prefs.getPhoneUsageAiContextDays())
                if (!usage.isNullOrBlank()) {
                    appendLine()
                    append(usage)
                }
            }
        }
    }

    private suspend fun buildWeeklyGoalsContext(
        weekStart: LocalDate,
        goals: List<com.dailycurator.data.model.WeeklyGoal>,
        includeJournal: Boolean,
        includePhoneUsage: Boolean,
    ): String = buildString {
        appendLine("Week starting: $weekStart")
        appendLine("Goals (${goals.size}):")
        if (goals.isEmpty()) appendLine("None")
        goals.forEach {
            val st = if (it.isCompleted) "completed" else "in progress"
            appendLine("- [$st] #${it.id} ${it.title}")
            it.description?.takeIf { d -> d.isNotBlank() }?.let { d -> appendLine("  description: $d") }
            appendLine("  deadline=${it.deadline} timeEstimate=${it.timeEstimate} category=${it.category}")
        }
        if (includeJournal) {
            val zone = ZoneId.systemDefault()
            val journal = journalRepository.getJournalForAiChannel(
                JournalAiChannel.WeeklyGoalsInsight,
                LocalDate.now(zone),
                prefs.getJournalContextWindowDays(),
                zone,
                24,
            )
            appendLine()
            appendLine("--- JOURNAL (date window + per-entry toggles; weekly insight) ---")
            appendLine(JournalContextFormatter.format(journal))
        }
        if (includePhoneUsage) {
            val usage = phoneUsageRepository.buildCompactUsageContextBlock(prefs.getPhoneUsageWeeklyInsightDays())
            if (!usage.isNullOrBlank()) {
                appendLine()
                append(usage)
            }
        }
    }
}

fun CachedInsightEntity.toAiInsight(): AiInsight {
    val segments = summarySegmentsJson.parseInsightSegmentsList()
    val spiritual = when {
        spiritualEnglish.isNullOrBlank() && spiritualArabic.isNullOrBlank() -> null
        else -> SpiritualNote(
            source = spiritualSource.orEmpty(),
            arabic = spiritualArabic.orEmpty(),
            english = spiritualEnglish.orEmpty(),
        )
    }
    return AiInsight(
        insightText = insightText,
        boldPart = boldPart,
        recoveryPlan = recoveryPlan,
        generatedAtEpochMillis = generatedAtEpochMillis,
        insightDayKey = dayKey,
        summarySegments = segments,
        spiritualNote = spiritual,
    )
}

private fun String?.parseInsightSegmentsList(): List<InsightSummarySegment>? {
    if (isNullOrBlank()) return null
    return runCatching {
        val arr = JsonParser.parseString(this).asJsonArray
        val out = mutableListOf<InsightSummarySegment>()
        for (el in arr) {
            if (!el.isJsonObject) continue
            val o = el.asJsonObject
            val t = o.get("text")?.takeIf { it.isJsonPrimitive }?.asString ?: continue
            val tone = o.get("tone")?.takeIf { it.isJsonPrimitive }?.asString?.lowercase()?.trim()
                ?: "default"
            out.add(InsightSummarySegment(text = t, tone = tone))
        }
        out.takeIf { it.isNotEmpty() }
    }.getOrNull()
}

