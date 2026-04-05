package com.dailycurator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailycurator.data.model.EventPriority
import com.dailycurator.data.model.ScheduleEvent
import com.dailycurator.ui.theme.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun TimelineEventCard(
    event: ScheduleEvent,
    modifier: Modifier = Modifier,
    contentAlpha: Float = 1f,
    accentColorOverride: Color? = null,
    showDurationMinutes: Boolean = true,
) {
    val accentColor = accentColorOverride ?: when (event.priority) {
        EventPriority.HIGH   -> AccentRed
        EventPriority.MEDIUM -> TimelineBlue
        EventPriority.LOW    -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val timeFmt = remember { DateTimeFormatter.ofPattern("h:mm a") }
    val durationMin = remember(event.startTime, event.endTime) {
        ChronoUnit.MINUTES.between(event.startTime, event.endTime).coerceAtLeast(1)
    }
    Card(
        modifier = modifier.fillMaxWidth().alpha(contentAlpha),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(accentColor, RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
            )
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(event.title,
                        style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier.weight(1f))
                    if (event.isProtected) {
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.Lock, contentDescription = "Protected",
                            tint = Primary, modifier = Modifier.size(16.dp))
                    }
                }
                Text(
                    buildString {
                        append(event.startTime.format(timeFmt))
                        append(" – ")
                        append(event.endTime.format(timeFmt))
                        if (showDurationMinutes) {
                            append(" · ")
                            append(durationMin)
                            append(" min")
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                event.location?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
                if (event.tags.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        event.tags.forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Primary.copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(tag,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Primary, fontWeight = FontWeight.SemiBold,
                                        fontSize = 9.sp))
                            }
                        }
                    }
                }
            }
        }
    }
}
