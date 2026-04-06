package com.dailycurator.ui.screens.pomodoro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.hilt.navigation.compose.hiltViewModel
import com.dailycurator.data.pomodoro.PomodoroLaunchRequest
import com.dailycurator.ui.theme.appScreenBackground
import java.util.Locale
import java.util.concurrent.TimeUnit

private val PresetMinutes = listOf(10, 15, 20, 25, 30, 45, 60)

private fun presetLabel(minutes: Int): String = if (minutes == 60) "1h" else "${minutes}m"

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PomodoroScreen(viewModel: PomodoroViewModel = hiltViewModel()) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.onScreenResume()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val ui by viewModel.uiState.collectAsState()
    val sessions by viewModel.recentSessions.collectAsState()

    /** True when the user opened the custom editor via the Custom chip (even if minutes match a preset). */
    var forceCustomEditor by remember { mutableStateOf(false) }
    var customDraft by remember { mutableStateOf("") }

    LaunchedEffect(ui.plannedMinutes) {
        customDraft = ui.plannedMinutes.toString()
    }

    val showCustomEditor =
        !ui.sessionActive && (forceCustomEditor || ui.plannedMinutes !in PresetMinutes)

    val completedAll = sessions.count { it.completed }
    val totalFocusSec = sessions.filter { it.completed }.sumOf { it.actualFocusedSeconds.toLong() }

    val link = ui.launch
    val linkedSessions = if (link == null) {
        emptyList()
    } else {
        sessions.filter { session ->
            when (link.entityType) {
                PomodoroLaunchRequest.TYPE_HABIT ->
                    session.entityType == PomodoroLaunchRequest.TYPE_HABIT &&
                        link.habitSeriesId != null &&
                        session.habitSeriesId == link.habitSeriesId
                else ->
                    session.entityType == link.entityType && session.entityId == link.entityId
            }
        }
    }
    val linkedCompleted = linkedSessions.count { it.completed }
    val linkedFocusSec = linkedSessions.filter { it.completed }.sumOf { it.actualFocusedSeconds.toLong() }

    val totalSec = ui.sessionTotalSeconds ?: (ui.plannedMinutes * 60)
    val remainingSec = ui.remainingSeconds ?: totalSec
    val ringProgress = if (totalSec > 0) {
        (remainingSec.toFloat() / totalSec.toFloat()).coerceIn(0f, 1f)
    } else {
        1f
    }

    val mm = TimeUnit.SECONDS.toMinutes(remainingSec.toLong()).toInt()
    val ss = remainingSec % 60
    val timeLabel = String.format(Locale.getDefault(), "%02d:%02d", mm, ss)

    val keyboard = LocalSoftwareKeyboardController.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .appScreenBackground(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Text(
                "Pomodoro",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            )
            if (link != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    link.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "Sessions on this item: $linkedCompleted · ${formatDuration(linkedFocusSec)} focused",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Choose a length, then start. Time counts down on the ring.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                ),
            ) {
                Column(
                    Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier.size(280.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            progress = { ringProgress },
                            modifier = Modifier.size(280.dp),
                            strokeWidth = 14.dp,
                            strokeCap = StrokeCap.Round,
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                timeLabel,
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 2.sp,
                                ),
                                textAlign = TextAlign.Center,
                            )
                            if (!ui.sessionActive) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "${ui.plannedMinutes} min session",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Text(
                        "Duration",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PresetMinutes.forEach { m ->
                            FilterChip(
                                selected = !forceCustomEditor && ui.plannedMinutes == m,
                                onClick = {
                                    if (!ui.sessionActive) {
                                        forceCustomEditor = false
                                        viewModel.setPlannedMinutes(m)
                                    }
                                },
                                enabled = !ui.sessionActive,
                                label = { Text(presetLabel(m)) },
                            )
                        }
                        FilterChip(
                            selected = forceCustomEditor || ui.plannedMinutes !in PresetMinutes,
                            onClick = {
                                if (!ui.sessionActive) {
                                    forceCustomEditor = true
                                    customDraft = ui.plannedMinutes.toString()
                                }
                            },
                            enabled = !ui.sessionActive,
                            label = { Text("Custom") },
                        )
                    }

                    if (showCustomEditor) {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedTextField(
                                value = customDraft,
                                onValueChange = { v ->
                                    customDraft = v.filter { it.isDigit() }.take(3)
                                },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                label = { Text("Minutes") },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done,
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        keyboard?.hide()
                                        applyCustomMinutes(customDraft, viewModel) { m ->
                                            forceCustomEditor = m !in PresetMinutes
                                        }
                                    },
                                ),
                            )
                            Button(
                                onClick = {
                                    keyboard?.hide()
                                    applyCustomMinutes(customDraft, viewModel) { m ->
                                        forceCustomEditor = m !in PresetMinutes
                                    }
                                },
                                shape = CircleShape,
                                modifier = Modifier.size(48.dp),
                                contentPadding = PaddingValues(0.dp),
                            ) {
                                Text("OK", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    if (ui.sessionActive) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Button(
                                onClick = {
                                    if (ui.running) viewModel.pause() else viewModel.startOrResume()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape = MaterialTheme.shapes.large,
                            ) {
                                Icon(
                                    imageVector = if (ui.running) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (ui.running) "Pause" else "Resume",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                            OutlinedButton(
                                onClick = { viewModel.endSessionEarly() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape = MaterialTheme.shapes.large,
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Stop", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    } else {
                        Button(
                            onClick = { viewModel.startOrResume() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = MaterialTheme.shapes.large,
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Start", style = MaterialTheme.typography.titleMedium)
                        }
                    }

                    if (link != null && !ui.sessionActive) {
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { viewModel.clearLinkedFocus() },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Clear linked item")
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Overview", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "$completedAll completed sessions · ${formatDuration(totalFocusSec)} total focus",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            Text("Recent sessions", style = MaterialTheme.typography.titleSmall)
        }

        items(sessions.take(40), key = { it.id }) { s ->
            val end = s.endedAtMillis
            val whenStr = if (end != null) {
                java.text.DateFormat.getDateTimeInstance(
                    java.text.DateFormat.SHORT,
                    java.text.DateFormat.SHORT,
                ).format(java.util.Date(s.startedAtMillis))
            } else {
                "In progress…"
            }
            Text(
                "${s.title} · ${formatDuration(s.actualFocusedSeconds.toLong())}" +
                    if (s.completed) " ✓" else " (ended early)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                whenStr,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun applyCustomMinutes(
    draft: String,
    viewModel: PomodoroViewModel,
    onApplied: (minutes: Int) -> Unit,
) {
    val m = draft.toIntOrNull()?.coerceIn(1, 180) ?: return
    viewModel.setPlannedMinutes(m)
    onApplied(m)
}

private fun formatDuration(totalSec: Long): String {
    if (totalSec <= 0L) return "0m"
    val h = TimeUnit.SECONDS.toHours(totalSec)
    val m = TimeUnit.SECONDS.toMinutes(totalSec) % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
