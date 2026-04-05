package com.dailycurator.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailycurator.data.model.ScheduleEvent
import com.dailycurator.ui.theme.NowRed
import com.dailycurator.ui.theme.Primary
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private val centerTimeFmt = DateTimeFormatter.ofPattern("h:mm a")
private val pinLabelFmt = DateTimeFormatter.ofPattern("h a")

private fun maxTime(a: LocalTime, b: LocalTime): LocalTime = if (a.isBefore(b)) b else a

private fun minTime(a: LocalTime, b: LocalTime): LocalTime = if (a.isBefore(b)) a else b

private fun minutesOfDay(t: LocalTime): Float =
    t.hour * 60f + t.minute + t.second / 60f

/**
 * ~12h “focus” bands by time of day (examples: 10am → 6–6, 4pm → noon–midnight).
 */
private fun dynamicFocusWindow(now: LocalTime): Pair<LocalTime, LocalTime> {
    val h = now.hour
    val endOfDay = LocalTime.of(23, 59, 59)
    return when {
        h < 6 -> LocalTime.MIDNIGHT to LocalTime.NOON
        h < 12 -> LocalTime.of(6, 0) to LocalTime.of(18, 0)
        h < 18 -> LocalTime.of(12, 0) to endOfDay
        else -> LocalTime.of(18, 0) to endOfDay
    }
}

private val safeClockFallback = LocalTime.of(6, 0) to LocalTime.of(22, 0)

/** Clip the focus window to the user’s day settings; fall back if empty or overnight (unsupported on one ring). */
private fun displayClockWindow(
    prefsStart: LocalTime,
    prefsEnd: LocalTime,
    now: LocalTime,
): Pair<LocalTime, LocalTime> {
    val (dStart, dEnd) = dynamicFocusWindow(now)
    val a = maxTime(dStart, prefsStart)
    val b = minTime(dEnd, prefsEnd)
    val candidate = if (a.isBefore(b)) a to b else prefsStart to prefsEnd
    val (s, e) = candidate
    // Single-day arc only: reject overnight (e.g. 22:00–06:00) — avoids coerceIn crash & label loops.
    return if (s.isBefore(e)) candidate else safeClockFallback
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

/** A small set of anchor hours on the ring (every 3h + window ends). Stops if LocalTime wraps past midnight. */
private fun pinnedLabelTimes(displayStart: LocalTime, displayEnd: LocalTime): List<LocalTime> {
    if (!displayStart.isBefore(displayEnd)) {
        return listOf(displayStart, displayEnd).distinct().sorted()
    }
    val out = LinkedHashSet<LocalTime>()
    var t = displayStart.withMinute(0).withSecond(0).withNano(0)
    if (t.isBefore(displayStart)) t = t.plusHours(1)
    var steps = 0
    while (!t.isAfter(displayEnd) && steps < 16) {
        out.add(t)
        val next = t.plusHours(3)
        if (!next.isAfter(t)) break // wrapped past midnight — would loop forever vs same-day end
        t = next
        steps++
    }
    out.add(displayEnd)
    return out.filter { !it.isBefore(displayStart) && !it.isAfter(displayEnd) }
        .distinct()
        .sorted()
}

private fun eventArcAlpha(event: ScheduleEvent, now: LocalTime): Float = when {
    !event.endTime.isAfter(now) -> 0.5f
    event.startTime.isAfter(now) -> 0.55f
    else -> 1f
}

/**
 * Radial clock: a **focus window** (derived from current time, clipped to your day settings)
 * maps to 360°. Pinned hour labels stay horizontal; tasks only in the visible arc.
 */
@Composable
fun ClockView(
    events: List<ScheduleEvent>,
    modifier: Modifier = Modifier,
    windowStart: LocalTime = LocalTime.of(4, 0),
    windowEnd: LocalTime = LocalTime.of(22, 0),
) {
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(30_000)
            now = LocalTime.now()
        }
    }

    val (displayStart, displayEnd) = remember(now, windowStart, windowEnd) {
        displayClockWindow(windowStart, windowEnd, now)
    }

    val windowMinRaw = minutesOfDay(displayStart)
    val windowMaxRaw = minutesOfDay(displayEnd)
    val windowMin = min(windowMinRaw, windowMaxRaw)
    val windowMax = max(windowMinRaw, windowMaxRaw)
    val totalMinutes = (windowMax - windowMin).coerceAtLeast(1f)

    fun timeToAngle(t: LocalTime): Float {
        val m = minutesOfDay(t).coerceIn(windowMin, windowMax)
        return (m - windowMin) / totalMinutes * 360f - 90f
    }

    val visibleEvents = remember(events, displayStart, displayEnd) {
        events.mapNotNull { e ->
            clipEventToWindow(e, displayStart, displayEnd)?.let { clip -> e to clip }
        }
    }

    val labelTimes = remember(displayStart, displayEnd) {
        pinnedLabelTimes(displayStart, displayEnd)
    }

    val scheme = MaterialTheme.colorScheme
    val trackMuted = scheme.outline.copy(alpha = 0.22f)
    val trackActive = scheme.primary.copy(alpha = 0.45f)
    val trackGlow = scheme.primary.copy(alpha = 0.12f)
    val centerDotColor = scheme.surface
    val tickMajor = scheme.onSurface.copy(alpha = 0.85f)
    val metaLabel = scheme.onSurfaceVariant
    val metaValue = scheme.onSurface

    val arcColors = listOf(
        Color(0xFF1565C0), Color(0xFF00897B), Color(0xFF6A1B9A),
        Color(0xFFE65100), Color(0xFF5C6BC0),
    )

    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp),
            ) {
                val cw = constraints.maxWidth.toFloat()
                val ch = constraints.maxHeight.toFloat()
                val center = Offset(cw / 2f, ch / 2f)
                val outerR = min(cw, ch) / 2f * 0.94f
                val innerR = outerR * 0.58f
                val trackStroke = outerR * 0.14f

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val labelRadius = outerR + 14.dp.toPx()
                    // Soft outer glow ring (attention, still clean)
                    drawArc(
                        color = trackGlow,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(center.x - outerR * 1.04f, center.y - outerR * 1.04f),
                        size = Size(outerR * 2.08f, outerR * 2.08f),
                        style = Stroke(width = trackStroke * 1.8f, cap = StrokeCap.Round),
                    )

                    drawArc(
                        color = trackMuted,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(center.x - outerR, center.y - outerR),
                        size = Size(outerR * 2, outerR * 2),
                        style = Stroke(width = trackStroke, cap = StrokeCap.Round),
                    )

                    val nowAngle = timeToAngle(now)
                    val endAngle = timeToAngle(displayEnd)
                    var sweepFuture = endAngle - nowAngle
                    if (sweepFuture < 0f) sweepFuture += 360f
                    if (sweepFuture > 0.5f) {
                        drawArc(
                            color = trackActive,
                            startAngle = nowAngle,
                            sweepAngle = sweepFuture,
                            useCenter = false,
                            topLeft = Offset(center.x - outerR, center.y - outerR),
                            size = Size(outerR * 2, outerR * 2),
                            style = Stroke(width = trackStroke, cap = StrokeCap.Round),
                        )
                    }

                    visibleEvents.forEachIndexed { i, (ev, range) ->
                        val (es, ee) = range
                        val startAngle = timeToAngle(es)
                        val endA = timeToAngle(ee)
                        val sweep = (endA - startAngle).let { if (it <= 0f) it + 360f else it }
                            .coerceAtLeast(5f)
                        val a = eventArcAlpha(ev, now)
                        drawArc(
                            color = arcColors[i % arcColors.size].copy(alpha = a),
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = Offset(center.x - outerR, center.y - outerR),
                            size = Size(outerR * 2, outerR * 2),
                            style = Stroke(width = trackStroke * 0.92f, cap = StrokeCap.Round),
                        )
                    }

                    val handRad = Math.toRadians(nowAngle.toDouble())
                    val handLen = innerR * 0.92f
                    drawLine(
                        color = NowRed.copy(alpha = 0.35f),
                        start = center,
                        end = Offset(
                            center.x + (handLen + 3.dp.toPx()) * cos(handRad).toFloat(),
                            center.y + (handLen + 3.dp.toPx()) * sin(handRad).toFloat(),
                        ),
                        strokeWidth = 8.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = NowRed,
                        start = center,
                        end = Offset(
                            center.x + handLen * cos(handRad).toFloat(),
                            center.y + handLen * sin(handRad).toFloat(),
                        ),
                        strokeWidth = 4.dp.toPx(),
                        cap = StrokeCap.Round,
                    )

                    drawCircle(color = NowRed, radius = 8.dp.toPx(), center = center)
                    drawCircle(color = centerDotColor, radius = 4.5.dp.toPx(), center = center)

                    // Major ticks + pinned labels (sparse, not 24h)
                    val native = drawContext.canvas.nativeCanvas
                    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        textSize = 12.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    }

                    labelTimes.forEach { tick ->
                        val ang = Math.toRadians(timeToAngle(tick).toDouble())
                        val outer = outerR - trackStroke / 2f - 1.dp.toPx()
                        val inner = outer - 12.dp.toPx()
                        val isNearNow = abs(minutesOfDay(tick) - minutesOfDay(now)) < 75f
                        val tc = if (isNearNow) tickMajor else tickMajor.copy(alpha = 0.72f)
                        drawLine(
                            color = tc,
                            start = Offset(
                                center.x + outer * cos(ang).toFloat(),
                                center.y + outer * sin(ang).toFloat(),
                            ),
                            end = Offset(
                                center.x + inner * cos(ang).toFloat(),
                                center.y + inner * sin(ang).toFloat(),
                            ),
                            strokeWidth = if (isNearNow) 2.5.dp.toPx() else 1.5.dp.toPx(),
                        )
                        paint.color = tc.toArgb()
                        val cx = center.x + labelRadius * cos(ang).toFloat()
                        val cy = center.y + labelRadius * sin(ang).toFloat()
                        native.drawText(
                            tick.format(pinLabelFmt),
                            cx,
                            cy + paint.textSize * 0.32f,
                            paint,
                        )
                    }
                }

                // Center readout (above canvas z-order handled by draw order — keep in Box on top)
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            now.format(centerTimeFmt),
                            style = MaterialTheme.typography.displayLarge.copy(
                                color = Primary,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .background(NowRed, RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                "NOW",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                ),
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = "Focus shows the next stretch of your day (not all 24 hours). Tasks outside this arc are hidden here.",
            style = MaterialTheme.typography.labelSmall,
            color = metaLabel,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    "FOCUS START",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = metaLabel,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                Text(
                    displayStart.format(DateTimeFormatter.ofPattern("h:mm a")),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = metaValue,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "FOCUS END",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = metaLabel,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                Text(
                    displayEnd.format(DateTimeFormatter.ofPattern("h:mm a")),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = metaValue,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
        Text(
            text = "Your day setting: ${windowStart.format(DateTimeFormatter.ofPattern("h:mm a"))} – ${windowEnd.format(DateTimeFormatter.ofPattern("h:mm a"))}",
            style = MaterialTheme.typography.labelSmall,
            color = metaLabel.copy(alpha = 0.85f),
            modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 8.dp),
        )
    }
}
