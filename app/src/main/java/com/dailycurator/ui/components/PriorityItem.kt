package com.dailycurator.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailycurator.data.model.PriorityTask
import com.dailycurator.data.model.Urgency
import com.dailycurator.ui.theme.*

private val TaskDoneToggleShape = RoundedCornerShape(5.dp)

/** Day-wide order for auto list numbers: rank, then start time, then stable id. */
fun tasksSortedForListNumber(tasks: List<PriorityTask>): List<PriorityTask> =
    tasks.sortedWith(compareBy({ it.rank }, { it.startTime }, { it.id }))

/** [PriorityTask.displayNumber] > 0 overrides; otherwise 1-based index in [orderedForDay]. */
fun resolvedTaskListNumber(task: PriorityTask, orderedForDay: List<PriorityTask>): Int {
    if (task.displayNumber > 0) return task.displayNumber.coerceIn(1, 999)
    val idx = orderedForDay.indexOfFirst { it.id == task.id }
    return if (idx >= 0) idx + 1 else 1
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PriorityItem(
    task: PriorityTask,
    listNumber: Int,
    onToggleDone: () -> Unit,
    modifier: Modifier = Modifier,
    taskTagColors: Map<String, Int> = emptyMap(),
    onStartPomodoro: (() -> Unit)? = null,
) {
    val accentColor = when (task.urgency) {
        Urgency.GREEN   -> AccentGreen
        Urgency.RED     -> AccentRed
        Urgency.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val checkColor by animateColorAsState(
        targetValue = if (task.isDone) AccentGreen else Color.Transparent,
        animationSpec = tween(300), label = "check"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Accent bar
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(accentColor, RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
            )
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // List # (rank ≠ display order)
                Text(
                    text = if (listNumber in 1..99) "%02d".format(listNumber) else listNumber.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier.widthIn(min = 22.dp, max = 36.dp)
                )
                Spacer(Modifier.width(8.dp))
                // Content
                Column(modifier = Modifier.weight(1f)) {
                    Text(task.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = if (task.isDone) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            textDecoration = if (task.isDone) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                        ))
                    if (task.isMustDo) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Must-do",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            "${task.startTime} - ${task.endTime}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterVertically),
                        )
                        task.tags.forEach { tag ->
                            TaskTagChip(
                                label = tag,
                                backgroundArgb = TaskTagUi.argbForTag(tag, taskTagColors),
                            )
                        }
                    }
                    val extraMeta = buildString {
                        task.dueInfo?.let { append(it) }
                        task.statusNote?.let {
                            if (isNotEmpty()) append(" • ")
                            append(it)
                        }
                    }
                    if (extraMeta.isNotEmpty()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            extraMeta,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (task.isCantComplete) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Won't do",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                if (onStartPomodoro != null && task.id > 0L) {
                    IconButton(
                        onClick = onStartPomodoro,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = "Start Pomodoro",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                }
                // Done toggle — rounded square (not circular)
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(TaskDoneToggleShape)
                        .background(checkColor)
                        .border(
                            1.5.dp,
                            if (task.isDone) AccentGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            TaskDoneToggleShape,
                        )
                        .clickable { onToggleDone() },
                    contentAlignment = Alignment.Center,
                ) {
                    if (task.isDone) {
                        Icon(Icons.Default.Check, contentDescription = "Done",
                            tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}
