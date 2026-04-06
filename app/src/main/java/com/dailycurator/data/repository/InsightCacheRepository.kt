package com.dailycurator.data.repository

import com.dailycurator.data.ai.InsightType
import com.dailycurator.data.ai.JournalContextFormatter
import com.dailycurator.data.ai.parseInsightJson
import com.dailycurator.data.local.AppPreferences
import com.dailycurator.data.local.dao.CachedInsightDao
import com.dailycurator.data.local.entity.CachedInsightEntity
import com.dailycurator.data.model.AiInsight
import com.dailycurator.data.remote.CerebrasApiException
import com.dailycurator.data.remote.CerebrasChatMessage
import com.dailycurator.data.remote.CerebrasRestClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
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
        if (prefs.getCerebrasKey().isBlank()) return
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
        if (prefs.getCerebrasKey().isBlank()) {
            return@withLock Result.failure(IllegalStateException("Set your Cerebras API key in Settings."))
        }
        val dayKey = LocalDate.now().toString()
        runCatching { generateAssistant(dayKey) }.map { }
    }

    suspend fun ensureWeeklyGoalsForTodayIfNeeded() {
        if (!prefs.isWeeklyGoalsInsightEnabled()) return
        if (prefs.getCerebrasKey().isBlank()) return
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
        if (prefs.getCerebrasKey().isBlank()) {
            return@withLock Result.failure(IllegalStateException("Set your Cerebras API key in Settings."))
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
        val content = completeInsightJson(system, ctx)
        val (bold, summary, recovery) = parseInsightJson(content)
        dao.upsert(
            CachedInsightEntity(
                type = InsightType.ASSISTANT,
                dayKey = dayKey,
                generatedAtEpochMillis = System.currentTimeMillis(),
                insightText = summary,
                boldPart = bold,
                recoveryPlan = recovery,
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
        val content = completeInsightJson(system, ctx)
        val (bold, summary, recovery) = parseInsightJson(content)
        dao.upsert(
            CachedInsightEntity(
                type = InsightType.WEEKLY_GOALS,
                dayKey = dayKey,
                generatedAtEpochMillis = System.currentTimeMillis(),
                insightText = summary,
                boldPart = bold,
                recoveryPlan = recovery,
            ),
        )
    }

    private suspend fun completeInsightJson(systemPrompt: String, userContext: String): String {
        val result = cerebras.chatCompletion(
            messages = listOf(
                CerebrasChatMessage(role = "system", content = systemPrompt),
                CerebrasChatMessage(role = "user", content = userContext),
            ),
            tools = null,
            temperature = 0.35,
            maxTokens = 1024,
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
        val time = java.time.LocalDateTime.now().format(fmt)
        return buildString {
            appendLine("Current local time: $time (calendar date $today)")
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
                val journal = journalRepository.getRecentForAiContext(30)
                appendLine()
                appendLine("--- JOURNAL (recent, user-enabled for assistant insight) ---")
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
            val journal = journalRepository.getRecentForAiContext(24)
            appendLine()
            appendLine("--- JOURNAL (recent, user-enabled for weekly insight) ---")
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

fun CachedInsightEntity.toAiInsight(): AiInsight = AiInsight(
    insightText = insightText,
    boldPart = boldPart,
    recoveryPlan = recoveryPlan,
    generatedAtEpochMillis = generatedAtEpochMillis,
    insightDayKey = dayKey,
)

