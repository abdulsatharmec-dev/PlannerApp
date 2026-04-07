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
import androidx.compose.ui.text.style.TextOverflow
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
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val cardMaxH = this.maxHeight
            val tiny = cardMaxH < 40.dp
            val compact = cardMaxH < 76.dp
            val showDuration = showDurationMinutes && cardMaxH >= 42.dp
            val padH = if (compact) 8.dp else 12.dp
            val padV = if (tiny) 4.dp else if (compact) 6.dp else 10.dp

            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(accentColor, RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)),
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = padH, vertical = padV),
                    verticalArrangement = Arrangement.Top,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = event.title,
                            style = if (tiny) {
                                MaterialTheme.typography.labelLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold,
                                    lineHeight = 16.sp,
                                )
                            } else if (compact) {
                                MaterialTheme.typography.titleSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold,
                                    lineHeight = 18.sp,
                                )
                            } else {
                                MaterialTheme.typography.titleMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            },
                            modifier = Modifier.weight(1f),
                            maxLines = when {
                                tiny -> 1
                                compact -> 2
                                else -> 4
                            },
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (event.isProtected) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Protected",
                                tint = Primary,
                                modifier = Modifier.size(if (tiny) 14.dp else 16.dp),
                            )
                        }
                    }
                    if (cardMaxH >= 28.dp) {
                        if (!tiny) Spacer(Modifier.height(if (compact) 2.dp else 4.dp))
                        Text(
                            text = buildString {
                                append(event.startTime.format(timeFmt))
                                append(" – ")
                                append(event.endTime.format(timeFmt))
                                if (showDuration) {
                                    append(" · ")
                                    append(durationMin)
                                    append(" min")
                                }
                            },
                            style = if (tiny) {
                                MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
                            } else {
                                MaterialTheme.typography.labelSmall
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (!tiny && cardMaxH >= 52.dp) {
                        event.location?.let {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    if (!tiny && cardMaxH >= 70.dp && event.tags.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            event.tags.forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Primary.copy(alpha = 0.12f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                ) {
                                    Text(
                                        tag,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Primary,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 9.sp,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
