package com.dailycurator.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

@Composable
fun PriorityItem(
    task: PriorityTask,
    onToggleDone: () -> Unit,
    modifier: Modifier = Modifier
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
                // Rank number
                Text(
                    text = "%02d".format(task.rank),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier.width(22.dp)
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
                            }
                        ))
                    Spacer(Modifier.height(2.dp))
                    val subtitle = buildString {
                        append("${task.startTime} - ${task.endTime}")
                        task.dueInfo?.let { append(" • $it") }
                        task.statusNote?.let { append(" • $it") }
                    }
                    Text(subtitle, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.width(8.dp))
                // Done toggle
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(checkColor)
                        .border(
                            1.5.dp,
                            if (task.isDone) AccentGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            CircleShape
                        )
                        .clickable { onToggleDone() },
                    contentAlignment = Alignment.Center
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
