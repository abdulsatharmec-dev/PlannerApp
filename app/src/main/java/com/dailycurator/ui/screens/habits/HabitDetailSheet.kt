package com.dailycurator.ui.screens.habits

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailycurator.data.local.entity.HabitLogEntity
import com.dailycurator.data.model.Habit
import com.dailycurator.data.model.HabitType
import com.dailycurator.ui.theme.AccentDeepGreen
import com.dailycurator.ui.theme.AccentGreen
import com.dailycurator.ui.theme.AccentRed
import com.dailycurator.ui.theme.Primary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailBottomSheet(
    habit: Habit,
    logs: List<HabitLogEntity>,
    completedDays: Set<LocalDate>,
    calendarMonth: LocalDate,
    onMonthChange: (LocalDate) -> Unit,
    missedCount: Int,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onRequestDelete: () -> Unit,
    onMarkDone: (String?) -> Unit,
    onStartPomodoro: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var menuOpen by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }

    if (showNoteDialog) {
        MarkHabitDoneDialog(
            habit = habit,
            onDismiss = { showNoteDialog = false },
            onConfirm = { note ->
                onMarkDone(note.takeIf { it.isNotBlank() })
                showNoteDialog = false
            },
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    habit.iconEmoji,
                    fontSize = 28.sp,
                    modifier = Modifier.padding(end = 10.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        habit.name,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                    Text(
                        habit.frequency.replaceFirstChar { it.uppercase() } + " · ${habit.category}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { onStartPomodoro() }) {
                    Icon(Icons.Default.Timer, contentDescription = "Pomodoro")
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = {
                                menuOpen = false
                                onEdit()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("View history") },
                            onClick = {
                                menuOpen = false
                                showHistory = !showHistory
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Delete habit") },
                            onClick = {
                                menuOpen = false
                                onRequestDelete()
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            val swipeCompleteThreshold = with(LocalDensity.current) { 100.dp.toPx() }
            var dragX by remember { mutableFloatStateOf(0f) }
            val swipeAlpha by animateFloatAsState(
                targetValue = (1f - (dragX / (swipeCompleteThreshold * 2f)).coerceIn(0f, 0.35f)),
                animationSpec = tween(120),
                label = "swipeAlpha",
            )
            if (!habit.isDone) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            AccentGreen.copy(alpha = 0.15f),
                            RoundedCornerShape(14.dp),
                        )
                        .pointerInput(swipeCompleteThreshold) {
                            detectHorizontalDragGestures(
                                onHorizontalDrag = { _, dx ->
                                    dragX = (dragX + dx).coerceAtLeast(0f)
                                },
                                onDragCancel = { dragX = 0f },
                                onDragEnd = {
                                    if (dragX >= swipeCompleteThreshold) {
                                        showNoteDialog = true
                                    }
                                    dragX = 0f
                                },
                            )
                        }
                        .padding(16.dp),
                ) {
                    Row(
                        Modifier
                            .offset { IntOffset(dragX.roundToInt(), 0) }
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "Swipe right to mark done →",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.alpha(swipeAlpha),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatChip("Current streak", "${habit.streakDays}d")
                StatChip("Longest", "${habit.longestStreak}d")
                StatChip("Missed (window)", "$missedCount")
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "${habit.completionPercent}% toward target",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            LinearProgressIndicator(
                progress = { habit.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .padding(top = 6.dp),
                strokeCap = StrokeCap.Round,
                color = if (habit.habitType == HabitType.ELIMINATING) AccentRed else Primary,
            )

            Spacer(Modifier.height(18.dp))
            HabitMonthCalendar(
                month = calendarMonth,
                completedDays = completedDays,
                onPrevMonth = { onMonthChange(calendarMonth.minusMonths(1)) },
                onNextMonth = { onMonthChange(calendarMonth.plusMonths(1)) },
            )

            if (showHistory) {
                Spacer(Modifier.height(16.dp))
                Text("Recent logs", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                val fmt = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }
                LazyColumn(
                    modifier = Modifier.height(180.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(logs, key = { "${it.habitSeriesId}_${it.dayKey}" }) { log ->
                        Text(
                            "${LocalDate.parse(log.dayKey).format(fmt)} · ${log.valueCompleted} — ${log.note ?: "—"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HabitMonthCalendar(
    month: LocalDate,
    completedDays: Set<LocalDate>,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    val title = month.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " ${month.year}"
    val prevInteraction = remember { MutableInteractionSource() }
    val nextInteraction = remember { MutableInteractionSource() }
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            "<",
            Modifier
                .padding(8.dp)
                .clickable(indication = null, interactionSource = prevInteraction) { onPrevMonth() },
            style = MaterialTheme.typography.titleMedium,
        )
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(
            ">",
            Modifier
                .padding(8.dp)
                .clickable(indication = null, interactionSource = nextInteraction) { onNextMonth() },
            style = MaterialTheme.typography.titleMedium,
        )
    }
    Spacer(Modifier.height(8.dp))
    val first = month.withDayOfMonth(1)
    val daysInMonth = month.lengthOfMonth()
    val startOffset = first.dayOfWeek.value % 7
    val labels = listOf("S", "M", "T", "W", "T", "F", "S")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        labels.forEach {
            Text(
                it,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.width(36.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
    Spacer(Modifier.height(4.dp))
    val totalCells = startOffset + daysInMonth
    val rows = (totalCells + 6) / 7
    var dayNum = 1
    repeat(rows) { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            repeat(7) { col ->
                val cellIndex = row * 7 + col
                if (cellIndex < startOffset || dayNum > daysInMonth) {
                    Spacer(Modifier.size(36.dp))
                } else {
                    val d = month.withDayOfMonth(dayNum)
                    val done = completedDays.contains(d)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (done) AccentDeepGreen.copy(alpha = 0.35f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("$dayNum", style = MaterialTheme.typography.labelMedium)
                    }
                    dayNum++
                }
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}
