package com.dailycurator.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.dailycurator.data.model.ScheduleEvent
import com.dailycurator.ui.theme.NowRed
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.roundToInt

private fun minutesOfDay(t: LocalTime): Int = t.hour * 60 + t.minute

private fun minutesOfDayFloat(t: LocalTime): Float =
    t.hour * 60f + t.minute + t.second / 60f

fun hourTickTimes(windowStart: LocalTime, windowEnd: LocalTime): List<LocalTime> {
    var t = windowStart.withMinute(0).withSecond(0).withNano(0)
    if (t.isBefore(windowStart)) t = t.plusHours(1)
    val out = mutableListOf<LocalTime>()
    while (!t.isAfter(windowEnd)) {
        out.add(t)
        t = t.plusHours(1)
    }
    return out
}

private fun labelAlphaForHourTick(tickStart: LocalTime, now: LocalTime): Float {
    val tickEnd = tickStart.plusHours(1)
    return when {
        now.isBefore(tickStart) -> 0.42f
        !now.isBefore(tickEnd) -> 0.36f
        else -> 1f
    }
}

private fun clipEventToWindow(
    event: ScheduleEvent,
    windowStart: LocalTime,
    windowEnd: LocalTime,
): Pair<LocalTime, LocalTime>? {
    val s = if (event.startTime.isBefore(windowStart)) windowStart else event.startTime
    val e = if (event.endTime.isAfter(windowEnd)) windowEnd else event.endTime
    if (!s.isBefore(e)) return null
    return s to e
}

private fun eventTemporalAlpha(event: ScheduleEvent, now: LocalTime): Float = when {
    !event.endTime.isAfter(now) -> 0.42f
    !event.startTime.isAfter(now) && !now.isBefore(event.endTime) -> 1f
    else -> 0.48f
}

/**
 * Vertical timeline: [windowStart]–[windowEnd] mapped to height. Hour grid, fading past/future labels,
 * tasks positioned by start/end, “now” line, current-hour highlight.
 */
@Composable
fun ScheduleTimelineView(
    events: List<ScheduleEvent>,
    windowStart: LocalTime,
    windowEnd: LocalTime,
    modifier: Modifier = Modifier,
) {
    val labelFmt = remember { DateTimeFormatter.ofPattern("h:mm a") }
    val startM = minutesOfDay(windowStart)
    val endM = minutesOfDay(windowEnd)
    val totalM = (endM - startM).coerceAtLeast(1)

    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            now = LocalTime.now()
        }
    }

    val hourHeightDp = 52.dp
    val totalHeightDp = hourHeightDp * (totalM / 60f)
    val hourTicks = remember(windowStart, windowEnd) { hourTickTimes(windowStart, windowEnd) }
    val density = LocalDensity.current

    val sortedEvents = remember(events) {
        events.sortedWith(
            compareBy<ScheduleEvent> { it.startTime }
                .thenByDescending { ChronoUnit.MINUTES.between(it.startTime, it.endTime) },
        )
    }

    Row(modifier = modifier.fillMaxWidth()) {
        BoxWithConstraints(
            Modifier
                .width(58.dp)
                .height(totalHeightDp),
        ) {
            val boxH = constraints.maxHeight.toFloat()
            hourTicks.forEach { tick ->
                val tickM = minutesOfDay(tick)
                val yPx = (tickM - startM) / totalM.toFloat() * boxH
                Text(
                    text = tick.format(labelFmt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .offset { IntOffset(0, (yPx - 6f).roundToInt().coerceAtLeast(0)) }
                        .alpha(labelAlphaForHourTick(tick, now)),
                )
            }
        }

        BoxWithConstraints(
            Modifier
                .weight(1f)
                .height(totalHeightDp),
        ) {
            val boxH = constraints.maxHeight.toFloat()
            val lineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
            val highlight = MaterialTheme.colorScheme.primary.copy(alpha = 0.09f)

            Canvas(Modifier.fillMaxSize()) {
                hourTicks.forEach { tick ->
                    val tickM = minutesOfDay(tick)
                    val y = (tickM - startM) / totalM.toFloat() * size.height
                    drawLine(
                        color = lineColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
            }

            val nowM = minutesOfDayFloat(now)
            if (nowM > startM && nowM < endM) {
                val hourFloor = ((nowM / 60f).toInt() * 60).coerceAtLeast(startM)
                val hourCeil = (hourFloor + 60).coerceAtMost(endM)
                val topFrac = (hourFloor - startM) / totalM.toFloat()
                val hFrac = (hourCeil - hourFloor) / totalM.toFloat()
                Box(
                    Modifier
                        .fillMaxWidth()
                        .offset(y = with(density) { (topFrac * boxH).toDp() })
                        .height(with(density) { (hFrac * boxH).toDp() })
                        .background(highlight, RoundedCornerShape(6.dp)),
                )
            }

            sortedEvents.forEach { event ->
                val range = clipEventToWindow(event, windowStart, windowEnd) ?: return@forEach
                val (es, ee) = range
                val esM = minutesOfDayFloat(es)
                val eeM = minutesOfDayFloat(ee)
                val topPx = (esM - startM) / totalM * boxH
                val heightPx = max(((eeM - esM) / totalM * boxH).toDouble(), 44.0).toFloat()
                TimelineEventCard(
                    event = event,
                    contentAlpha = eventTemporalAlpha(event, now),
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .width(maxWidth)
                        .offset(y = with(density) { topPx.toDp() })
                        .height(with(density) { heightPx.toDp() }),
                )
            }

            if (nowM >= startM && nowM <= endM) {
                val nowY = (nowM - startM) / totalM * boxH
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .offset(y = with(density) { nowY.toDp() })
                        .background(NowRed, RoundedCornerShape(2.dp)),
                )
            }
        }
    }
}
