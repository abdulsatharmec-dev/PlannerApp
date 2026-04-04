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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailycurator.data.model.ScheduleEvent
import com.dailycurator.ui.theme.*
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.sin

private val timeFmt = DateTimeFormatter.ofPattern("hh:mm")

/**
 * Radial clock view. Maps a time window (windowStart..windowEnd) to 360°.
 * Each event is drawn as a coloured arc segment.
 * A red hand points to the current time.
 */
@Composable
fun ClockView(
    events: List<ScheduleEvent>,
    modifier: Modifier = Modifier,
    windowStart: LocalTime = LocalTime.of(9, 0),
    windowEnd: LocalTime = LocalTime.of(18, 0)
) {
    val now = remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now.value = LocalTime.now()
            kotlinx.coroutines.delay(30_000)
        }
    }

    val windowMinutes = windowStart.toSecondOfDay() / 60f to windowEnd.toSecondOfDay() / 60f
    val totalMinutes = windowMinutes.second - windowMinutes.first
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val centerDotColor = MaterialTheme.colorScheme.surface
    val tickColor = MaterialTheme.colorScheme.onSurfaceVariant
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

    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(32.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val outerR = size.minDimension / 2f
                val innerR = outerR * 0.62f
                val trackStroke = outerR * 0.18f

                // Background track arc (full 360°)
                drawArc(
                    color = trackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(center.x - outerR, center.y - outerR),
                    size = Size(outerR * 2, outerR * 2),
                    style = Stroke(width = trackStroke, cap = StrokeCap.Round)
                )

                // Event arc segments
                events.forEachIndexed { i, event ->
                    val startAngle = timeToAngle(event.startTime)
                    val endAngle = timeToAngle(event.endTime)
                    val sweep = (endAngle - startAngle).let {
                        if (it <= 0f) it + 360f else it
                    }.coerceAtLeast(4f)
                    drawArc(
                        color = arcColors[i % arcColors.size],
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(center.x - outerR, center.y - outerR),
                        size = Size(outerR * 2, outerR * 2),
                        style = Stroke(width = trackStroke, cap = StrokeCap.Round)
                    )
                }

                // Current time hand
                val nowAngle = timeToAngle(now.value)
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

                // Centre dot
                drawCircle(color = NowRed, radius = 7.dp.toPx(), center = center)
                drawCircle(color = centerDotColor, radius = 4.dp.toPx(), center = center)

                // Tick marks every hour
                val hours = ((windowEnd.hour - windowStart.hour)).coerceAtLeast(1)
                for (h in 0..hours) {
                    val tickTime = windowStart.plusHours(h.toLong())
                    val tickAngle = Math.toRadians(timeToAngle(tickTime).toDouble())
                    val outer = outerR - trackStroke / 2 - 2.dp.toPx()
                    val inner = outer - 8.dp.toPx()
                    drawLine(
                        color = tickColor,
                        start = Offset(
                            (center.x + outer * cos(tickAngle)).toFloat(),
                            (center.y + outer * sin(tickAngle)).toFloat()
                        ),
                        end = Offset(
                            (center.x + inner * cos(tickAngle)).toFloat(),
                            (center.y + inner * sin(tickAngle)).toFloat()
                        ),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }
            }

            // Centre time text
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    now.value.format(timeFmt),
                    style = MaterialTheme.typography.displayLarge.copy(
                        color = Primary, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                )
                Box(
                    modifier = Modifier
                        .background(NowRed, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("NOW",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White, fontWeight = FontWeight.Bold))
                }
            }
        }

        // Window labels
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("WINDOW START",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = metaLabelColor, fontSize = 9.sp))
                Text(windowStart.format(DateTimeFormatter.ofPattern("HH:mm")),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = metaValueColor, fontWeight = FontWeight.SemiBold))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("WINDOW END",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = metaLabelColor, fontSize = 9.sp))
                Text(windowEnd.format(DateTimeFormatter.ofPattern("HH:mm")),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = metaValueColor, fontWeight = FontWeight.SemiBold))
            }
        }
    }
}
