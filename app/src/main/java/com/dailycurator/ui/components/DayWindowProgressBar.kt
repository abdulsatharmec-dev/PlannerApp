package com.dailycurator.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dailycurator.ui.theme.AccentGreen
import com.dailycurator.ui.theme.NowRed
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

private fun minutesOfDay(t: LocalTime): Float =
    t.hour * 60f + t.minute + t.second / 60f

@Composable
fun DayWindowProgressBar(
    windowStart: LocalTime,
    windowEnd: LocalTime,
    modifier: Modifier = Modifier,
    /** Sum of durations (minutes) for today's incomplete must-do tasks; subtracted from time-until-day-end for the productive caption. */
    mustDoUndoneMinutes: Int = 0,
) {
    val endLabelFmt = remember { DateTimeFormatter.ofPattern("h:mm a") }
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            now = LocalTime.now()
        }
    }

    val startM = minutesOfDay(windowStart)
    val endM = minutesOfDay(windowEnd)
    val totalM = (endM - startM).coerceAtLeast(1f)
    val nowM = minutesOfDay(now)

    val (progress, caption) = when {
        nowM <= startM -> {
            0f to run {
                val mins = (startM - nowM).roundToInt().coerceAtLeast(0)
                val d = Duration.ofMinutes(mins.toLong())
                "Starts in ${formatDurationShort(d)}"
            }
        }
        nowM >= endM -> {
            1f to "Day window ended"
        }
        else -> {
            val p = ((nowM - startM) / totalM).coerceIn(0f, 1f)
            val elapsed = Duration.of((nowM - startM).roundToInt().toLong(), ChronoUnit.MINUTES)
            val left = Duration.of((endM - nowM).roundToInt().toLong(), ChronoUnit.MINUTES)
            p to "${formatDurationShort(elapsed)} elapsed · ${formatDurationShort(left)} left"
        }
    }

    val rawLeftM = if (nowM < endM) (endM - nowM).roundToInt().coerceAtLeast(0) else 0
    val productiveLeftM = (rawLeftM - mustDoUndoneMinutes.coerceAtLeast(0)).coerceAtLeast(0)
    val animatedProductiveLeft by animateIntAsState(
        targetValue = productiveLeftM,
        animationSpec = tween(durationMillis = 650, easing = LinearEasing),
        label = "productiveLeft",
    )
    val infinite = rememberInfiniteTransition(label = "productiveGlow")
    val glowPhase by infinite.animateFloat(
        initialValue = 0.88f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow",
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = caption,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        if (nowM < endM) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "Productive time left",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatDurationShort(Duration.ofMinutes(animatedProductiveLeft.toLong())),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = (0.78f + 0.22f * glowPhase).coerceIn(0.55f, 1f)),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = windowStart.format(endLabelFmt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = windowEnd.format(endLabelFmt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (nowM in startM..endM) AccentGreen else NowRed.copy(alpha = 0.7f),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}

private fun formatDurationShort(d: Duration): String {
    val h = d.toHours()
    val m = d.toMinutes() % 60
    return when {
        h > 0 && m > 0 -> "${h}h ${m}m"
        h > 0 -> "${h}h"
        m > 0 -> "${m}m"
        else -> "0m"
    }
}
