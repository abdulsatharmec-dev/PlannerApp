package com.dailycurator.ui.screens.phoneusage

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.dailycurator.data.usage.AppUsageRow
import com.dailycurator.data.usage.AppUsageSession
import com.dailycurator.data.usage.formatPhoneUsageDuration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import com.dailycurator.ui.components.WeeklyGoalsInsightCard
import com.dailycurator.ui.theme.appScreenBackground

@Composable
fun PhoneUsageScreen(viewModel: PhoneUsageViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val llmOk by viewModel.llmConfigured.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var usageInsightExpanded by rememberSaveable { mutableStateOf(true) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.onResume()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val snap = state.snapshot
    val insight = state.insight

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .appScreenBackground()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Text(
                "Phone usage",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
            )
            Text(
                "Foreground time from Android usage access.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
        }

        if (!state.hasPermission) {
            item {
                Text(
                    "Allow usage access for DayRoute in system settings.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    },
                ) {
                    Text("Open usage access settings")
                }
            }
        } else {
        item {
            Text(
                "AI summary",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "Prompt: Settings → Phone usage insight prompt.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
        }

        item {
            state.error?.let { err ->
                Text(
                    err,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            when {
                !llmOk -> {
                    Text(
                        "Add an LLM API key in Settings to generate a summary.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                snap == null && state.loadingStats -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                        )
                        Text("Loading usage…", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                snap == null -> {
                    Text(
                        "Pick a range below, tap Refresh, then generate.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                insight == null -> {
                    Button(
                        onClick = { viewModel.generateInsight() },
                        enabled = !state.generatingInsight,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (state.generatingInsight) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.size(8.dp))
                        }
                        Text("Generate summary")
                    }
                }
                else -> {
                    WeeklyGoalsInsightCard(
                        insight = insight,
                        expanded = usageInsightExpanded,
                        onExpandedChange = { usageInsightExpanded = it },
                        insightTitle = "Usage insight",
                        onRegenerate = { viewModel.generateInsight() },
                        isRegenerating = state.generatingInsight,
                        scrollableBodyMaxHeight = 320.dp,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                FilterChip(
                    selected = state.rangeDays == 1,
                    onClick = { viewModel.setRangeDays(1) },
                    label = { Text("Today") },
                )
                FilterChip(
                    selected = state.rangeDays == 7,
                    onClick = { viewModel.setRangeDays(7) },
                    label = { Text("7 days") },
                )
                FilterChip(
                    selected = state.rangeDays == 14,
                    onClick = { viewModel.setRangeDays(14) },
                    label = { Text("14 days") },
                )
                Spacer(Modifier.weight(1f))
                OutlinedButton(
                    onClick = { viewModel.refreshStats() },
                    enabled = !state.loadingStats,
                    modifier = Modifier.height(36.dp),
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Refresh")
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        if (state.loadingStats && state.snapshot == null) {
            item {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }

        if (snap != null) {
            item {
                Text(
                    "Total: ${formatPhoneUsageDuration(snap.totalForegroundMs)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
                Text(
                    snap.rangeLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text(
                    "Apps (≥15s)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(6.dp))
            }
            if (snap.apps.isEmpty()) {
                item {
                    Text(
                        "No apps reached 15s in this range.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(snap.apps, key = { it.packageName }) { row ->
                    UsageAppRow(row = row, totalMs = snap.totalForegroundMs)
                }
            }
            val sessions = snap.sessions.sortedByDescending { it.startMillis }.take(50)
            if (sessions.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Sessions (sample)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        "Foreground intervals from usage events (local time).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                }
                items(sessions, key = { "${it.packageName}-${it.startMillis}" }) { s ->
                    UsageSessionRow(session = s)
                }
            }
        } else if (!state.loadingStats) {
            item {
                Text("No data for this range yet.", style = MaterialTheme.typography.bodyMedium)
            }
        }
        }
    }
}

@Composable
private fun UsageAppRow(row: AppUsageRow, totalMs: Long) {
    val share = if (totalMs > 0) row.foregroundMs.toFloat() / totalMs.toFloat() else 0f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PhoneUsageAppIcon(packageName = row.packageName)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                row.appLabel,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            )
            Text(
                "${row.launchCount} foreground open(s)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (share > 0.02f) {
                LinearProgressIndicator(
                    progress = { share },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            formatPhoneUsageDuration(row.foregroundMs),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}

@Composable
private fun UsageSessionRow(session: AppUsageSession) {
    val tf = remember { DateTimeFormatter.ofPattern("MMM d, h:mm a") }
    val zone = remember { ZoneId.systemDefault() }
    val start = remember(session.startMillis, zone) {
        ZonedDateTime.ofInstant(Instant.ofEpochMilli(session.startMillis), zone)
    }
    val end = remember(session.endMillis, zone) {
        ZonedDateTime.ofInstant(Instant.ofEpochMilli(session.endMillis), zone)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PhoneUsageAppIcon(packageName = session.packageName)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                session.appLabel,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            )
            Text(
                "${tf.format(start)} → ${tf.format(end)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            formatPhoneUsageDuration(session.durationMs),
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}
