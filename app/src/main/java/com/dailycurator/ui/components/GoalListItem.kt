package com.dailycurator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailycurator.data.model.PriorityTask
import com.dailycurator.data.model.WeeklyGoal
import com.dailycurator.ui.theme.AccentGreen
import com.dailycurator.ui.theme.ProgressTrack
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val isoDate: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

fun formatGoalDeadlineForDisplay(deadline: String): String =
    runCatching { LocalDate.parse(deadline, isoDate).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) }
        .getOrElse { deadline }

private fun goalProgressFraction(goal: WeeklyGoal): Float =
    when {
        goal.isCompleted -> 1f
        else -> (goal.progressPercent.coerceIn(0, 100) / 100f)
    }

@Composable
fun GoalTileCard(
    goal: WeeklyGoal,
    onOpenDetail: () -> Unit,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onRequestDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onStartPomodoro: (() -> Unit)? = null,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val progress = goalProgressFraction(goal)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onOpenDetail() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onToggleComplete,
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .border(
                                width = 1.5.dp,
                                color = if (goal.isCompleted) AccentGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                shape = CircleShape,
                            )
                            .background(if (goal.isCompleted) AccentGreen else Color.Transparent),
                    ) {
                        if (goal.isCompleted) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .size(14.dp)
                                    .align(Alignment.Center),
                            )
                        }
                    }
                }
                goal.iconEmoji?.takeIf { it.isNotBlank() }?.let { emoji ->
                    Text(emoji, fontSize = 22.sp, modifier = Modifier.padding(end = 6.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        goal.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val shortDesc = goal.description?.trim()?.takeIf { it.isNotEmpty() }
                    if (shortDesc != null) {
                        Text(
                            shortDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (onStartPomodoro != null && goal.id > 0L) {
                    IconButton(onClick = onStartPomodoro) {
                        Icon(Icons.Default.Timer, contentDescription = "Pomodoro", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Goal options")
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
                            text = { Text("Delete") },
                            onClick = {
                                menuOpen = false
                                onRequestDelete()
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = AccentGreen,
                trackColor = ProgressTrack,
                strokeCap = StrokeCap.Round,
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (goal.isCompleted) "100%" else "${goal.progressPercent.coerceIn(0, 100)}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    if (goal.isCompleted) "Completed" else "Active",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (goal.isCompleted) AccentGreen else MaterialTheme.colorScheme.primary,
                )
            }
            goal.deadline?.let { d ->
                Text(
                    "Target: ${formatGoalDeadlineForDisplay(d)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailBottomSheet(
    goal: WeeklyGoal,
    linkedTasks: List<PriorityTask>,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onRequestDelete: () -> Unit,
    onProgressChange: (Int) -> Unit,
    onToggleComplete: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var menuOpen by remember { mutableStateOf(false) }
    var sliderValue by remember(goal.id) { mutableFloatStateOf(0f) }
    LaunchedEffect(goal.id, goal.progressPercent, goal.isCompleted) {
        sliderValue = if (goal.isCompleted) 100f else goal.progressPercent.coerceIn(0, 100).toFloat()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                goal.iconEmoji?.takeIf { it.isNotBlank() }?.let { emoji ->
                    Text(emoji, fontSize = 32.sp, modifier = Modifier.padding(end = 10.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        goal.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                    Text(
                        if (goal.isCompleted) "Completed" else "Active",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
                            text = { Text("Delete") },
                            onClick = {
                                menuOpen = false
                                onRequestDelete()
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            goal.description?.trim()?.takeIf { it.isNotEmpty() }?.let { body ->
                Text(
                    body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(16.dp))
            }
            Text("Progress", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    onValueChangeFinished = { onProgressChange(sliderValue.toInt().coerceIn(0, 100)) },
                    valueRange = 0f..100f,
                    enabled = !goal.isCompleted,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text("${sliderValue.toInt().coerceIn(0, 100)}%", style = MaterialTheme.typography.bodySmall)
            }
            LinearProgressIndicator(
                progress = {
                    when {
                        goal.isCompleted -> 1f
                        else -> sliderValue.coerceIn(0f, 100f) / 100f
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = AccentGreen,
                trackColor = ProgressTrack,
                strokeCap = StrokeCap.Round,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onToggleComplete, modifier = Modifier.fillMaxWidth()) {
                Text(if (goal.isCompleted) "Mark as active" else "Mark as completed")
            }
            goal.deadline?.let {
                Text(
                    "Target date: ${formatGoalDeadlineForDisplay(it)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            Spacer(Modifier.height(20.dp))
            Text("Linked tasks", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
            Spacer(Modifier.height(8.dp))
            if (linkedTasks.isEmpty()) {
                Text(
                    "No tasks linked. Add a task and choose this goal under \"Linked Goal\".",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.height(((linkedTasks.size).coerceAtMost(5) * 48).dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(linkedTasks, key = { it.id }) { task ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                task.title,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                if (task.isDone) "Done" else "Pending",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (task.isDone) AccentGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text("Close")
            }
        }
    }
}
