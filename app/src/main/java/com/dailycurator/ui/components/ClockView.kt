package com.dailycurator.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import kotlin.math.cos
import kotlin.math.sin

private val centerTimeFmt = DateTimeFormatter.ofPattern("h:mm a")
private val edgeLabelFmt = DateTimeFormatter.ofPattern("ha")

private fun clockHourLabelAlpha(tickStart: LocalTime, now: LocalTime): Float {
    val tickEnd = tickStart.plusHours(1)
    return when {
        now.isBefore(tickStart) -> 0.42f
        !now.isBefore(tickEnd) -> 0.36f
        else -> 1f
    }
}

private fun eventArcAlpha(event: ScheduleEvent, now: LocalTime): Float = when {
    !event.endTime.isAfter(now) -> 0.4f
    event.startTime.isAfter(now) -> 0.45f
    else -> 1f
}

/**
 * Radial clock: [windowStart]..[windowEnd] maps to 360°. Hour ticks and labels on the ring;
 * past segment of the track is dimmed; event arcs fade before/after “now”.
 */
@Composable
fun ClockView(
    events: List<ScheduleEvent>,
    modifier: Modifier = Modifier,
    windowStart: LocalTime = LocalTime.of(4, 0),
    windowEnd: LocalTime = LocalTime.of(22, 0),
) {
    val now = remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now.value = LocalTime.now()
            kotlinx.coroutines.delay(30_000)
        }
    }

    val windowMinutes = windowStart.toSecondOfDay() / 60f to windowEnd.toSecondOfDay() / 60f
    val totalMinutes = (windowMinutes.second - windowMinutes.first).coerceAtLeast(1f)
    val trackBase = MaterialTheme.colorScheme.outline.copy(alpha = 0.32f)
    val trackFuture = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
    val centerDotColor = MaterialTheme.colorScheme.surface
    val tickColorBase = MaterialTheme.colorScheme.onSurfaceVariant
    val metaLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val metaValueColor = MaterialTheme.colorScheme.onSurface

    fun timeToAngle(t: LocalTime): Float {
        val minutes = t.toSecondOfDay() / 60f
        val clamped = minutes.coerceIn(windowMinutes.first, windowMinutes.second)
        return (clamped - windowMinutes.first) / totalMinutes * 360f - 90f
    }

    val arcColors = listOf(
        Color(0xFF1565C0), Color(0xFF00897B), Color(0xFF6A1B9A),
        Color(0xFFE65100), Color(0xFF37474F)
    )

    val hourTicks = remember(windowStart, windowEnd) { hourTickTimes(windowStart, windowEnd) }

    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(36.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val outerR = size.minDimension / 2f
                val innerR = outerR * 0.62f
                val trackStroke = outerR * 0.18f

                // Base track (past + neutral)
                drawArc(
                    color = trackBase,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(center.x - outerR, center.y - outerR),
                    size = Size(outerR * 2, outerR * 2),
                    style = Stroke(width = trackStroke, cap = StrokeCap.Round)
                )

                // Brighter “upcoming” arc from now → end of window (clockwise on the dial)
                val nowAngle = timeToAngle(now.value)
                val endDial = timeToAngle(windowEnd)
                var sweepFuture = endDial - nowAngle
                if (sweepFuture < 0f) sweepFuture += 360f
                if (sweepFuture > 0.5f) {
                    drawArc(
                        color = trackFuture,
                        startAngle = nowAngle,
                        sweepAngle = sweepFuture,
                        useCenter = false,
                        topLeft = Offset(center.x - outerR, center.y - outerR),
                        size = Size(outerR * 2, outerR * 2),
                        style = Stroke(width = trackStroke, cap = StrokeCap.Round)
                    )
                }

                // Event arc segments
                events.forEachIndexed { i, event ->
                    val startAngle = timeToAngle(event.startTime)
                    val endAngle = timeToAngle(event.endTime)
                    val sweep = (endAngle - startAngle).let {
                        if (it <= 0f) it + 360f else it
                    }.coerceAtLeast(4f)
                    val a = eventArcAlpha(event, now.value)
                    drawArc(
                        color = arcColors[i % arcColors.size].copy(alpha = a),
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(center.x - outerR, center.y - outerR),
                        size = Size(outerR * 2, outerR * 2),
                        style = Stroke(width = trackStroke, cap = StrokeCap.Round)
                    )
                }

                // Current time hand
                val handRadians = Math.toRadians(nowAngle.toDouble())
                val handLength = innerR * 0.88f
                drawLine(
                    color = NowRed,
                    start = center,
                    end = Offset(
                        (center.x + handLength * cos(handRadians)).toFloat(),
                        (center.y + handLength * sin(handRadians)).toFloat()
                    ),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )

                drawCircle(color = NowRed, radius = 7.dp.toPx(), center = center)
                drawCircle(color = centerDotColor, radius = 4.dp.toPx(), center = center)

                // Hour tick marks + numeric labels outside the ring
                val labelR = outerR + 10.dp.toPx()
                val native = drawContext.canvas.nativeCanvas
                val labelPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = 11.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                }

                hourTicks.forEach { tick ->
                    val a = clockHourLabelAlpha(tick, now.value)
                    val tickAngle = Math.toRadians(timeToAngle(tick).toDouble())
                    val outer = outerR - trackStroke / 2f - 2.dp.toPx()
                    val inner = outer - 10.dp.toPx()
                    val tc = tickColorBase.copy(alpha = (0.25f + 0.75f * a))
                    drawLine(
                        color = tc,
                        start = Offset(
                            (center.x + outer * cos(tickAngle)).toFloat(),
                            (center.y + outer * sin(tickAngle)).toFloat()
                        ),
                        end = Offset(
                            (center.x + inner * cos(tickAngle)).toFloat(),
                            (center.y + inner * sin(tickAngle)).toFloat()
                        ),
                        strokeWidth = if (a >= 0.99f) 2.5.dp.toPx() else 1.5.dp.toPx()
                    )

                    labelPaint.color = tickColorBase.copy(alpha = a).toArgb()
                    val cx = center.x + labelR * cos(tickAngle).toFloat()
                    val cy = center.y + labelR * sin(tickAngle).toFloat()
                    native.drawText(
                        tick.format(edgeLabelFmt),
                        cx,
                        cy + labelPaint.textSize * 0.35f,
                        labelPaint
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    now.value.format(centerTimeFmt),
                    style = MaterialTheme.typography.displayLarge.copy(
                        color = Primary, fontSize = 30.sp, fontWeight = FontWeight.Bold
                    )
                )
                Box(
                    modifier = Modifier
                        .background(NowRed, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        "NOW",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White, fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }

        Text(
            "Ring = your day window · ticks & labels = hours · arcs = tasks · red hand = now",
            style = MaterialTheme.typography.labelSmall,
            color = metaLabelColor,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "WINDOW START",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = metaLabelColor, fontSize = 9.sp
                    )
                )
                Text(
                    windowStart.format(DateTimeFormatter.ofPattern("h:mm a")),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = metaValueColor, fontWeight = FontWeight.SemiBold
                    )
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "WINDOW END",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = metaLabelColor, fontSize = 9.sp
                    )
                )
                Text(
                    windowEnd.format(DateTimeFormatter.ofPattern("h:mm a")),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = metaValueColor, fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}
