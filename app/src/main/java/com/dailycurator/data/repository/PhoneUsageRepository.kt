package com.dailycurator.data.repository

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import com.dailycurator.data.ai.parseInsightJson
import com.dailycurator.data.local.AppPreferences
import com.dailycurator.data.model.AiInsight
import com.dailycurator.data.remote.CerebrasApiException
import com.dailycurator.data.remote.CerebrasChatMessage
import com.dailycurator.data.remote.CerebrasRestClient
import com.dailycurator.data.usage.AppUsageRow
import com.dailycurator.data.usage.AppUsageSession
import com.dailycurator.data.usage.PhoneUsageSnapshot
import com.dailycurator.data.usage.formatPhoneUsageDuration
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhoneUsageRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cerebras: CerebrasRestClient,
    private val prefs: AppPreferences,
) {

    private val usageStatsManager: UsageStatsManager?
        get() = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

    private val packageManager: PackageManager get() = context.packageManager

    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * @param rangeDays 1 = calendar day from local midnight to now; 2+ = rolling N×24h ending now.
     */
    suspend fun loadSnapshot(rangeDays: Int): PhoneUsageSnapshot = withContext(Dispatchers.Default) {
        val usm = usageStatsManager ?: throw IllegalStateException("Usage stats are not available on this device.")
        val d = rangeDays.coerceAtLeast(1)
        val zone = ZoneId.systemDefault()
        val (startMillis, endMillis) = computeWindowMillis(d, zone)
        val rangeLabel = rangeLabelForDays(d)
        val timeByPkg = queryForegroundTimeByPackage(usm, startMillis, endMillis)
        val launchesByPkg = countForegroundEventsByPackage(usm, startMillis, endMillis)
        val rows = timeByPkg.map { (pkg, ms) ->
            AppUsageRow(
                packageName = pkg,
                appLabel = labelForPackage(pkg),
                foregroundMs = ms,
                launchCount = launchesByPkg[pkg] ?: 0,
            )
        }
            .filter { it.foregroundMs >= 15_000L }
            .sortedByDescending { it.foregroundMs }
        val total = rows.sumOf { it.foregroundMs }
        val sessions = collectForegroundSessions(usm, startMillis, endMillis)
        PhoneUsageSnapshot(
            rangeDays = d,
            rangeLabel = rangeLabel,
            rangeStartMillis = startMillis,
            rangeEndMillis = endMillis,
            totalForegroundMs = total,
            apps = rows,
            sessions = sessions,
        )
    }

    private fun computeWindowMillis(rangeDays: Int, zone: ZoneId): Pair<Long, Long> {
        val now = ZonedDateTime.now(zone)
        val endMillis = now.toInstant().toEpochMilli()
        val startMillis = if (rangeDays <= 1) {
            now.toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
        } else {
            endMillis - rangeDays.toLong() * 24L * 60L * 60L * 1000L
        }
        return startMillis to endMillis
    }

    private fun rangeLabelForDays(days: Int): String =
        if (days <= 1) {
            "Today (since midnight, local time)"
        } else {
            "Last $days days (rolling)"
        }

    /**
     * Single window for AI: totals, top apps, and session intervals (local time).
     */
    suspend fun buildCompactUsageContextBlock(rangeDays: Int): String? = withContext(Dispatchers.Default) {
        if (!hasUsageStatsPermission()) return@withContext null
        runCatching {
            val snap = loadSnapshot(rangeDays)
            formatSnapshotForAiBlock(snap)
        }.getOrNull()
    }

    private fun formatSnapshotForAiBlock(snap: PhoneUsageSnapshot): String = buildString {
        appendLine("--- PHONE USAGE (Android foreground time + session intervals from usage events) ---")
        appendLine("Range: ${snap.rangeLabel}")
        appendLine("Total foreground (apps ≥15s in aggregate): ${formatPhoneUsageDuration(snap.totalForegroundMs)} · ${snap.apps.size} apps")
        if (snap.apps.isNotEmpty()) {
            appendLine("Top apps: ${compactAppsLine(snap.apps, 12)}")
        }
        val sess = snap.sessions
            .sortedBy { it.startMillis }
            .take(MAX_SESSIONS_COMPACT_BLOCK)
        if (sess.isNotEmpty()) {
            appendLine()
            appendLine("--- SESSIONS (local time; end = background or range end) ---")
            val tf = DateTimeFormatter.ofPattern("MMM d, h:mm a")
            val zone = ZoneId.systemDefault()
            sess.forEach { s ->
                val st = ZonedDateTime.ofInstant(Instant.ofEpochMilli(s.startMillis), zone)
                val en = ZonedDateTime.ofInstant(Instant.ofEpochMilli(s.endMillis), zone)
                appendLine(
                    "- ${s.appLabel}: ${tf.format(st)} → ${tf.format(en)} " +
                        "(${formatPhoneUsageDuration(s.durationMs)})",
                )
            }
            if (snap.sessions.size > sess.size) {
                appendLine("… +${snap.sessions.size - sess.size} more sessions omitted")
            }
        }
    }

    suspend fun generateUsageInsight(snapshot: PhoneUsageSnapshot): Result<AiInsight> = withContext(Dispatchers.IO) {
        runCatching {
            if (!prefs.isLlmConfigured()) {
                throw IllegalStateException("Add an LLM API key in Settings.")
            }
            val userBlock = buildUsageContextForLlm(snapshot)
            val result = cerebras.chatCompletion(
                messages = listOf(
                    CerebrasChatMessage(role = "system", content = prefs.getPhoneUsageInsightPrompt()),
                    CerebrasChatMessage(role = "user", content = userBlock),
                ),
                tools = null,
                temperature = 0.35,
                maxTokens = 2048,
            )
            val msg = result.message ?: throw CerebrasApiException("Empty model response")
            val raw = msg.content?.takeIf { it.isNotBlank() }
                ?: throw CerebrasApiException("Model returned no text")
            val (bold, summary, recovery) = parseInsightJson(raw)
            AiInsight(
                insightText = summary,
                boldPart = bold,
                recoveryPlan = recovery,
                generatedAtEpochMillis = System.currentTimeMillis(),
                insightDayKey = null,
            )
        }
    }

    private fun queryForegroundTimeByPackage(
        usm: UsageStatsManager,
        begin: Long,
        end: Long,
    ): Map<String, Long> {
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, begin, end) ?: return emptyMap()
        val merged = mutableMapOf<String, Long>()
        for (s in stats) {
            val pkg = s.packageName ?: continue
            val t = s.totalTimeInForeground
            if (t <= 0L) continue
            merged[pkg] = (merged[pkg] ?: 0L) + t
        }
        return merged
    }

    private fun countForegroundEventsByPackage(
        usm: UsageStatsManager,
        begin: Long,
        end: Long,
    ): Map<String, Int> {
        val counts = mutableMapOf<String, Int>()
        for (e in collectForegroundRawEvents(usm, begin, end)) {
            counts[e.packageName] = counts.getOrDefault(e.packageName, 0) + 1
        }
        return counts
    }

    private data class ForegroundEvent(val time: Long, val packageName: String)

    private fun collectForegroundRawEvents(
        usm: UsageStatsManager,
        begin: Long,
        end: Long,
    ): List<ForegroundEvent> {
        val events = usm.queryEvents(begin, end)
        val out = mutableListOf<ForegroundEvent>()
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (!isForegroundEvent(event.eventType)) continue
            val pkg = event.packageName ?: continue
            out.add(ForegroundEvent(event.timeStamp, pkg))
        }
        return out.sortedBy { it.time }
    }

    private fun collectForegroundSessions(
        usm: UsageStatsManager,
        begin: Long,
        end: Long,
    ): List<AppUsageSession> {
        val raw = mutableListOf<Pair<Long, String>>()
        val event = UsageEvents.Event()
        val q = usm.queryEvents(begin, end)
        while (q.hasNextEvent()) {
            q.getNextEvent(event)
            val pkg = event.packageName ?: continue
            val t = event.timeStamp
            when {
                isForegroundEvent(event.eventType) -> raw.add(t to pkg)
                isBackgroundEvent(event.eventType) -> raw.add(t to "!bg:$pkg")
            }
        }
        raw.sortBy { it.first }

        val sessions = mutableListOf<AppUsageSession>()
        var currentPkg: String? = null
        var currentStart = 0L

        fun closeSession(endTime: Long) {
            val p = currentPkg ?: return
            if (endTime > currentStart + MIN_SESSION_MS) {
                sessions.add(
                    AppUsageSession(
                        packageName = p,
                        appLabel = labelForPackage(p),
                        startMillis = currentStart,
                        endMillis = endTime,
                    ),
                )
            }
            currentPkg = null
        }

        for ((t, marker) in raw) {
            if (marker.startsWith("!bg:")) {
                val pkg = marker.removePrefix("!bg:")
                if (pkg == currentPkg) {
                    closeSession(t)
                }
                continue
            }
            closeSession(t)
            currentPkg = marker
            currentStart = t
        }
        closeSession(end)
        return sessions.sortedByDescending { it.startMillis }.take(MAX_SESSIONS_STORED)
    }

    private fun isForegroundEvent(type: Int): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            type == UsageEvents.Event.ACTIVITY_RESUMED
        } else {
            @Suppress("DEPRECATION")
            type == UsageEvents.Event.MOVE_TO_FOREGROUND
        }

    private fun isBackgroundEvent(type: Int): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            type == UsageEvents.Event.ACTIVITY_PAUSED
        } else {
            @Suppress("DEPRECATION")
            type == UsageEvents.Event.MOVE_TO_BACKGROUND
        }

    private fun labelForPackage(packageName: String): String {
        val fromPm = runCatching {
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getApplicationInfo(
                    packageName,
                    PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong()),
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            }
            info.loadLabel(packageManager).toString().trim()
        }.getOrNull()
        if (!fromPm.isNullOrEmpty()) return fromPm
        return humanizePackageName(packageName)
    }

    private fun humanizePackageName(packageName: String): String {
        val parts = packageName.split('.').filter { it.isNotBlank() }
        if (parts.size <= 1) return packageName
        val skip = setOf("com", "android", "google", "apps", "huawei", "honor", "samsung", "xiaomi", "oppo", "vivo", "oneplus", "microsoft", "mozilla", "org")
        val meaningful = parts.asReversed().firstOrNull { it.lowercase(Locale.US) !in skip && it.length > 2 }
            ?: parts.last()
        return meaningful.replace('_', ' ')
            .replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase(Locale.getDefault()) else c.toString() }
            .ifBlank { packageName }
    }

    private fun buildUsageContextForLlm(snapshot: PhoneUsageSnapshot): String {
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z")
        val timeFmt = DateTimeFormatter.ofPattern("MMM d, h:mm a")
        val zone = ZoneId.systemDefault()
        val start = ZonedDateTime.ofInstant(Instant.ofEpochMilli(snapshot.rangeStartMillis), zone)
        val end = ZonedDateTime.ofInstant(Instant.ofEpochMilli(snapshot.rangeEndMillis), zone)
        val top = snapshot.apps.take(TOP_APPS_FOR_LLM)
        val tail = snapshot.apps.drop(TOP_APPS_FOR_LLM)
        val tailTime = tail.sumOf { it.foregroundMs }
        val tailLaunches = tail.sumOf { it.launchCount }
        val sessionsForLlm = snapshot.sessions.sortedBy { it.startMillis }.take(MAX_SESSIONS_FOR_LLM)
        return buildString {
            appendLine("Phone usage report (foreground time from UsageStats; sessions from ACTIVITY_RESUMED/PAUSED or MOVE events).")
            appendLine("Range: ${snapshot.rangeLabel}")
            appendLine("Window: ${fmt.format(start)} → ${fmt.format(end)}")
            appendLine("Total foreground time (listed apps, ≥15s each): ${formatDurationForLlm(snapshot.totalForegroundMs)}")
            appendLine("Apps shown below: ${snapshot.apps.size} (sorted by time).")
            appendLine()
            appendLine("--- TOP APPS ---")
            top.forEachIndexed { i, row ->
                appendLine(
                    "${i + 1}. ${row.appLabel} [${row.packageName}] — " +
                        "${formatDurationForLlm(row.foregroundMs)}, ~${row.launchCount} foreground open(s)",
                )
            }
            if (tail.isNotEmpty()) {
                appendLine()
                appendLine(
                    "--- OTHER APPS (${tail.size}) — combined ~${formatDurationForLlm(tailTime)}, " +
                        "~$tailLaunches open(s) ---",
                )
            }
            if (sessionsForLlm.isNotEmpty()) {
                appendLine()
                appendLine("--- SESSION TIMELINES (local time, approximate; end = background or window end) ---")
                sessionsForLlm.forEach { s ->
                    val st = ZonedDateTime.ofInstant(Instant.ofEpochMilli(s.startMillis), zone)
                    val en = ZonedDateTime.ofInstant(Instant.ofEpochMilli(s.endMillis), zone)
                    appendLine(
                        "- ${s.appLabel}: ${timeFmt.format(st)} – ${timeFmt.format(en)} " +
                            "(${formatDurationForLlm(s.durationMs)})",
                    )
                }
                if (snapshot.sessions.size > sessionsForLlm.size) {
                    appendLine("… ${snapshot.sessions.size - sessionsForLlm.size} more sessions omitted for length")
                }
            }
        }
    }

    private fun formatDurationForLlm(ms: Long): String = formatPhoneUsageDuration(ms)

    private fun compactAppsLine(apps: List<AppUsageRow>, n: Int): String =
        apps.take(n).joinToString(" · ") {
            "${it.appLabel} ${formatPhoneUsageDuration(it.foregroundMs)} (${it.launchCount}×)"
        }

    companion object {
        private const val TOP_APPS_FOR_LLM = 35
        private const val MAX_SESSIONS_FOR_LLM = 100
        private const val MAX_SESSIONS_COMPACT_BLOCK = 60
        private const val MAX_SESSIONS_STORED = 400
        private const val MIN_SESSION_MS = 10_000L
    }
}
