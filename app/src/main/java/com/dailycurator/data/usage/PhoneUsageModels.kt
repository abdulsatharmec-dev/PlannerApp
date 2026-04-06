package com.dailycurator.data.usage

data class AppUsageRow(
    val packageName: String,
    val appLabel: String,
    val foregroundMs: Long,
    val launchCount: Int,
)

/** One continuous foreground interval (from usage events; end may be window end if still in foreground). */
data class AppUsageSession(
    val packageName: String,
    val appLabel: String,
    val startMillis: Long,
    val endMillis: Long,
) {
    val durationMs: Long get() = (endMillis - startMillis).coerceAtLeast(0L)
}

data class PhoneUsageSnapshot(
    val rangeDays: Int,
    val rangeLabel: String,
    val rangeStartMillis: Long,
    val rangeEndMillis: Long,
    val totalForegroundMs: Long,
    val apps: List<AppUsageRow>,
    val sessions: List<AppUsageSession>,
)

/** Human-readable foreground duration for UI (and consistent with usage stats). */
fun formatPhoneUsageDuration(ms: Long): String {
    if (ms <= 0L) return "0m"
    val minutes = ms / 60_000L
    val hours = minutes / 60L
    val m = minutes % 60L
    return if (hours > 0L) "${hours}h ${m}m" else "${m}m"
}
