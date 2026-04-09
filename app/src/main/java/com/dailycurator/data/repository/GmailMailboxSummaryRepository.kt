package com.dailycurator.data.repository

import android.content.Context
import com.dailycurator.data.ai.AiPromptDefaults
import com.dailycurator.data.ai.InsightType
import com.dailycurator.data.gmail.GmailSummarySuggestedTask
import com.dailycurator.data.gmail.GmailTokenResult
import com.dailycurator.data.gmail.GmailTokenProvider
import com.dailycurator.data.gmail.GmailRestClient
import com.dailycurator.data.local.AppPreferences
import com.dailycurator.data.local.dao.CachedInsightDao
import com.dailycurator.data.local.entity.CachedInsightEntity
import com.dailycurator.data.model.Urgency
import com.dailycurator.data.remote.CerebrasChatMessage
import com.dailycurator.data.remote.CerebrasRestClient
import com.google.gson.JsonParser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GmailMailboxSummaryRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: CachedInsightDao,
    private val prefs: AppPreferences,
    private val cerebras: CerebrasRestClient,
    private val gmailRest: GmailRestClient,
    private val tokenProvider: GmailTokenProvider,
) {

    private val mutex = Mutex()

    fun observe(): Flow<CachedInsightEntity?> = dao.observe(InsightType.GMAIL_MAILBOX_SUMMARY)

    suspend fun regenerate(rangeDays: Int): Result<Unit> = mutex.withLock {
        if (!prefs.isLlmConfigured()) {
            return@withLock Result.failure(IllegalStateException("Add an LLM API key in Settings."))
        }
        val accounts = prefs.getGmailLinkedAccounts().filter { it.showInSummary }
        if (accounts.isEmpty()) {
            return@withLock Result.failure(IllegalStateException("Link at least one Gmail account in Settings."))
        }

        val dayKey = "${LocalDate.now()}_${rangeDays.coerceIn(1, 90)}d"
        val q = "newer_than:${rangeDays.coerceIn(1, 90)}d (in:inbox OR in:spam)"

        val sb = StringBuilder()
        for (acc in accounts) {
            when (val tok = tokenProvider.getAccessToken(acc.email)) {
                is GmailTokenResult.NeedsUserInteraction -> {
                    context.startActivity(tok.intent)
                    return@withLock Result.failure(
                        IllegalStateException("Complete Google sign-in, then try again."),
                    )
                }
                is GmailTokenResult.Failure ->
                    sb.appendLine("### ${acc.email}\n_Error: ${tok.message}_\n")
                is GmailTokenResult.Ok -> {
                    sb.appendLine("### Mailbox: ${acc.email}")
                    runCatching {
                        val ids = gmailRest.listMessageIds(tok.accessToken, q, maxResults = 42)
                        if (ids.isEmpty()) {
                            sb.appendLine("_No messages in range._\n")
                        } else {
                            ids.take(36).forEach { ref ->
                                runCatching {
                                    val d = gmailRest.getMessageDigest(tok.accessToken, ref.id)
                                    sb.appendLine(
                                        "- **From:** ${d.from} | **Subject:** ${d.subject} | **Date:** ${d.date}",
                                    )
                                    sb.appendLine("  Snippet: ${d.snippet}\n")
                                }.onFailure { ex ->
                                    sb.appendLine("- _(skipped message ${ref.id}: ${ex.message})_\n")
                                }
                            }
                        }
                    }.onFailure { ex ->
                        sb.appendLine("_Failed to list messages: ${ex.message}_\n")
                    }
                }
            }
        }

        val system = prefs.getGmailMailboxSummaryPrompt()
        val userContent = "Time range: last $rangeDays day(s).\n\n${sb.toString().trim()}"

        val markdown = runCatching {
            cerebras.completePlainText(
                messages = listOf(
                    CerebrasChatMessage(role = "system", content = system),
                    CerebrasChatMessage(role = "user", content = userContent),
                ),
                temperature = 0.25,
                maxTokens = 4096,
            )
        }.getOrElse { return@withLock Result.failure(it) }

        val homeBlurb = markdown.lineSequence()
            .firstOrNull { it.isNotBlank() }
            ?.take(200)
            ?: markdown.take(200)

        dao.upsert(
            CachedInsightEntity(
                type = InsightType.GMAIL_MAILBOX_SUMMARY,
                dayKey = dayKey,
                generatedAtEpochMillis = System.currentTimeMillis(),
                insightText = markdown,
                boldPart = "Gmail mailbox summary",
                recoveryPlan = homeBlurb,
            ),
        )
        prefs.setMailboxSummaryRangeDays(rangeDays)
        Result.success(Unit)
    }

    /**
     * Uses the configured LLM to infer concrete planner tasks from an already-generated summary.
     */
    suspend fun suggestTasksFromSummaryMarkdown(markdown: String): Result<List<GmailSummarySuggestedTask>> {
        if (!prefs.isLlmConfigured()) {
            return Result.failure(IllegalStateException("Add an LLM API key in Settings."))
        }
        val trimmed = if (markdown.length > 14_000) {
            markdown.take(14_000) + "\n\n…"
        } else {
            markdown
        }
        val raw = runCatching {
            cerebras.completePlainText(
                messages = listOf(
                    CerebrasChatMessage(role = "system", content = AiPromptDefaults.GMAIL_SUMMARY_EXTRACT_TASKS),
                    CerebrasChatMessage(
                        role = "user",
                        content = "Gmail summary (markdown):\n\n$trimmed",
                    ),
                ),
                temperature = 0.2,
                maxTokens = 1024,
            )
        }.getOrElse { return Result.failure(it) }

        return runCatching { parseSuggestedTasksJson(raw) }
    }

    private fun parseSuggestedTasksJson(raw: String): List<GmailSummarySuggestedTask> {
        var t = raw.trim()
        if (t.startsWith("```")) {
            t = t.removePrefix("```json").removePrefix("```").trim()
            val fence = t.lastIndexOf("```")
            if (fence >= 0) t = t.substring(0, fence).trim()
        }
        val root = JsonParser.parseString(t).asJsonObject
        val arr = root.getAsJsonArray("tasks") ?: return emptyList()
        val out = ArrayList<GmailSummarySuggestedTask>()
        for (el in arr) {
            if (!el.isJsonObject) continue
            val o = el.asJsonObject
            val title = o.get("title")?.takeUnless { it.isJsonNull }?.asString?.trim().orEmpty()
            if (title.isEmpty()) continue
            val detail = o.get("detail")?.takeUnless { it.isJsonNull }?.asString?.trim().orEmpty()
            val urgStr = o.get("urgency")?.takeUnless { it.isJsonNull }?.asString?.trim()?.lowercase()
            val urgency = when (urgStr) {
                "high" -> Urgency.RED
                "low" -> Urgency.NEUTRAL
                else -> Urgency.GREEN
            }
            out.add(GmailSummarySuggestedTask(title = title, detail = detail, urgency = urgency))
        }
        return out
    }
}
