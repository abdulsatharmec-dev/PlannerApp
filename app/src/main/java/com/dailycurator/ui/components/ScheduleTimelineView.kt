package com.dailycurator.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailycurator.data.model.ScheduleEvent
import com.dailycurator.ui.theme.NowRed
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.roundToInt

private val TimelineViewportHeight = 400.dp
private const val BaseHourDp = 72f
private val BaseDpPerMinute: Float get() = BaseHourDp / 60f

private fun minutesOfDay(t: LocalTime): Int = t.hour * 60 + t.minute

private fun minutesOfDayFloat(t: LocalTime): Float =
    t.hour * 60f + t.minute + t.second / 60f

private fun floorToHour(t: LocalTime): LocalTime =
    t.withMinute(0).withSecond(0).withNano(0)

private fun minTime(a: LocalTime, b: LocalTime): LocalTime = if (a.isBefore(b)) a else b

private fun maxTime(a: LocalTime, b: LocalTime): LocalTime = if (a.isBefore(b)) b else a

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

private fun countTasksOverlappingSlice(
    events: List<ScheduleEvent>,
    segStart: LocalTime,
    segEnd: LocalTime,
    windowStart: LocalTime,
    windowEnd: LocalTime,
): Int = events.count { e ->
    val c = clipEventToWindow(e, windowStart, windowEnd) ?: return@count false
    val (es, ee) = c
    es.isBefore(segEnd) && ee.isAfter(segStart)
}

private fun densityExpansion(overlapCount: Int): Float {
    if (overlapCount <= 1) return 1f
    val k = (overlapCount - 1).coerceAtMost(8)
    return 1f + 0.78f * k + 0.11f * k * k
}

private data class TimelineSlice(
    val segStart: LocalTime,
    val segEnd: LocalTime,
    val heightDp: Float,
    val overlapCount: Int,
    var y0: Float = 0f,
)

private fun buildTimelineSlices(
    windowStart: LocalTime,
    windowEnd: LocalTime,
    events: List<ScheduleEvent>,
): List<TimelineSlice> {
    val out = mutableListOf<TimelineSlice>()
    var t = floorToHour(windowStart)
    while (t.isBefore(windowEnd)) {
        val segStart = maxTime(t, windowStart)
        val segEnd = minTime(t.plusHours(1), windowEnd)
        val mins = ChronoUnit.MINUTES.between(segStart, segEnd)
        if (mins > 0) {
            val n = countTasksOverlappingSlice(events, segStart, segEnd, windowStart, windowEnd)
            val h = BaseDpPerMinute * mins * densityExpansion(n)
            out.add(TimelineSlice(segStart, segEnd, h, n))
        }
        t = t.plusHours(1)
    }
    var acc = 0f
    for (s in out) {
        s.y0 = acc
        acc += s.heightDp
    }
    return out
}

private fun yAtMinute(slices: List<TimelineSlice>, m: Float): Float {
    if (slices.isEmpty()) return 0f
    val mm = m.coerceIn(
        minutesOfDayFloat(slices.first().segStart),
        minutesOfDayFloat(slices.last().segEnd),
    )
    for (s in slices) {
        val s0 = minutesOfDayFloat(s.segStart)
        val s1 = minutesOfDayFloat(s.segEnd)
        if (mm < s0) return s.y0
        if (mm <= s1) {
            val span = (s1 - s0).coerceAtLeast(1f / 60f)
            val frac = ((mm - s0) / span).coerceIn(0f, 1f)
            return s.y0 + s.heightDp * frac
        }
    }
    val last = slices.last()
    return last.y0 + last.heightDp
}

private data class LaneInfo(val lane: Int, val maxLane: Int)

private fun assignOverlapLanes(
    events: List<ScheduleEvent>,
    windowStart: LocalTime,
    windowEnd: LocalTime,
): Map<Long, LaneInfo> {
    val clipped = events.mapNotNull { e ->
        clipEventToWindow(e, windowStart, windowEnd)?.let { range -> e to range }
    }.sortedBy { it.second.first }
    val laneEnds = mutableListOf<LocalTime>()
    val laneById = mutableMapOf<Long, Int>()
    for ((e, range) in clipped) {
        val es = range.first
        val ee = range.second
        // Lane free when previous block ended at or before this start (end <= es).
        val idx = laneEnds.indexOfFirst { end -> !end.isAfter(es) }
        val lane = if (idx >= 0) {
            laneEnds[idx] = ee
            idx
        } else {
            laneEnds.add(ee)
            laneEnds.lastIndex
        }
        laneById[e.id] = lane
    }
    val maxLane = laneEnds.lastIndex.coerceAtLeast(0)
    return laneById.mapValues { (_, lane) -> LaneInfo(lane, maxLane) }
}

private fun labelAlphaForHourTick(tickStart: LocalTime, now: LocalTime): Float {
    val tickEnd = tickStart.plusHours(1)
    return when {
        now.isBefore(tickStart) -> 0.64f
        !now.isBefore(tickEnd) -> 0.58f
        else -> 1f
    }
}

private fun eventTemporalAlpha(event: ScheduleEvent, now: LocalTime): Float = when {
    !event.endTime.isAfter(now) -> 0.72f
    !event.startTime.isAfter(now) && now.isBefore(event.endTime) -> 1f
    else -> 0.68f
}

/**
 * Scrollable timeline with density-expanded hours, minute ticks in busy slices,
 * lane layout for overlaps, per-task accent colors, and a prominent “now” marker.
 */
@Composable
fun ScheduleTimelineView(
    events: List<ScheduleEvent>,
    windowStart: LocalTime,
    windowEnd: LocalTime,
    modifier: Modifier = Modifier,
    /** When false, hides “now” marker and scroll-to-now (e.g. browsing another day). */
    showNowIndicator: Boolean = true,
    scheduleDate: LocalDate = LocalDate.now(),
) {
    val labelFmt = remember { DateTimeFormatter.ofPattern("h:mm a") }
    val minuteFmt = remember { DateTimeFormatter.ofPattern("h:mm") }
    val startM = minutesOfDay(windowStart)
    val endM = minutesOfDay(windowEnd)

    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            now = LocalTime.now()
        }
    }

    val slices = remember(windowStart, windowEnd, events) {
        buildTimelineSlices(windowStart, windowEnd, events)
    }
    val totalHeightInner = remember(slices) {
        slices.sumOf { it.heightDp.toDouble() }.toFloat()
    }

    val laneMap = remember(events, windowStart, windowEnd) {
        assignOverlapLanes(events, windowStart, windowEnd)
    }
    val maxLanes = remember(laneMap) {
        if (laneMap.isEmpty()) 1
        else laneMap.values.maxOf { it.maxLane + 1 }.coerceAtLeast(1)
    }

    val sortedEvents = remember(events) {
        events.sortedWith(
            compareBy<ScheduleEvent> { it.startTime }
                .thenByDescending { ChronoUnit.MINUTES.between(it.startTime, it.endTime) },
        )
    }

    val hourTicks = remember(windowStart, windowEnd) { hourTickTimes(windowStart, windowEnd) }
    val density = LocalDensity.current
    val scrollState = rememberScrollState()

    val nowM = minutesOfDayFloat(now)
    var didSnapToNow by remember(windowStart, windowEnd, scheduleDate) { mutableStateOf(false) }
    LaunchedEffect(totalHeightInner, slices.size, windowStart, windowEnd, showNowIndicator, scheduleDate) {
        if (!showNowIndicator || didSnapToNow || slices.isEmpty()) return@LaunchedEffect
        delay(48)
        val maxScroll = scrollState.maxValue
        if (maxScroll <= 0) {
            didSnapToNow = true
            return@LaunchedEffect
        }
        val nowY = yAtMinute(slices, nowM)
        val nowPx = with(density) { nowY.dp.toPx() }.roundToInt()
        val viewportPx = with(density) { TimelineViewportHeight.toPx() }.roundToInt()
        val target = (nowPx - viewportPx / 2).coerceIn(0, maxScroll)
        scrollState.scrollTo(target)
        didSnapToNow = true
    }

    val pulse = rememberInfiniteTransition(label = "now_line")
    val pulseAlpha by pulse.animateFloat(
        initialValue = 0.88f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(TimelineViewportHeight)
            .verticalScroll(scrollState),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalHeightInner.dp),
        ) {
            Row(Modifier.fillMaxWidth()) {
                Box(
                    Modifier
                        .width(58.dp)
                        .fillMaxHeight(),
                ) {
                    hourTicks.forEach { tick ->
                        val tickM = minutesOfDayFloat(tick)
                        val yPx = yAtMinute(slices, tickM)
                        Text(
                            text = tick.format(labelFmt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .offset { IntOffset(0, with(density) { yPx.dp.roundToPx() } - 6.dp.roundToPx()) }
                                .alpha(
                                    if (showNowIndicator) labelAlphaForHourTick(tick, now) else 0.84f,
                                ),
                        )
                    }
                    slices.filter { it.overlapCount >= 2 }.forEach { slice ->
                        var q = slice.segStart.plusMinutes(15)
                        while (q.isBefore(slice.segEnd)) {
                            val yPx = yAtMinute(slices, minutesOfDayFloat(q))
                            Text(
                                text = q.format(minuteFmt),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .offset {
                                        IntOffset(
                                            0,
                                            with(density) { yPx.dp.roundToPx() } - 5.dp.roundToPx(),
                                        )
                                    }
                                    .alpha(0.75f),
                            )
                            q = q.plusMinutes(15)
                        }
                    }
                }

                val hTrackScroll = rememberScrollState()
                BoxWithConstraints(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) {
                    val viewportW = maxWidth
                    val gapTrack = 6.dp
                    val minSlotW = 116.dp
                    val minContentW = minSlotW * maxLanes + gapTrack * (maxLanes + 1)
                    val contentW = maxOf(viewportW, minContentW)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(hTrackScroll),
                    ) {
                        BoxWithConstraints(
                            Modifier
                                .width(contentW)
                                .fillMaxHeight(),
                        ) {
                            val trackWidth = maxWidth
                            val lineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.32f)
                            val currentHourFill = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)

                            Canvas(Modifier.fillMaxWidth().fillMaxHeight()) {
                                hourTicks.forEach { tick ->
                                    val yDp = yAtMinute(slices, minutesOfDayFloat(tick))
                                    val yPx = yDp.dp.toPx()
                                    drawLine(
                                        color = lineColor,
                                        start = Offset(0f, yPx),
                                        end = Offset(size.width, yPx),
                                        strokeWidth = 1.dp.toPx(),
                                    )
                                }
                            }

                            if (showNowIndicator && nowM > startM && nowM < endM) {
                                val hourFloor = ((nowM / 60f).toInt() * 60).coerceAtLeast(startM)
                                val hourCeil = (hourFloor + 60).coerceAtMost(endM)
                                val top = yAtMinute(slices, hourFloor.toFloat())
                                val bottom = yAtMinute(slices, hourCeil.toFloat())
                                val h = (bottom - top).coerceAtLeast(8f)
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .offset(y = top.dp)
                                        .height(h.dp)
                                        .background(currentHourFill, RoundedCornerShape(8.dp)),
                                )
                            }

                            sortedEvents.forEach { event ->
                                val range = clipEventToWindow(event, windowStart, windowEnd) ?: return@forEach
                                val (es, ee) = range
                                val esM = minutesOfDayFloat(es)
                                val eeM = minutesOfDayFloat(ee)
                                val topY = yAtMinute(slices, esM)
                                val bottomY = yAtMinute(slices, eeM)
                                val minH = when {
                                    maxLanes >= 4 -> 74f
                                    maxLanes >= 3 -> 66f
                                    maxLanes >= 2 -> 58f
                                    else -> 52f
                                }
                                val heightY = (bottomY - topY).coerceAtLeast(minH)
                                val lane = laneMap[event.id] ?: LaneInfo(0, 0)
                                val nLanes = (lane.maxLane + 1).coerceAtLeast(1)
                                val gap = gapTrack
                                val usable = trackWidth - gap * (nLanes + 1)
                                val slotW = usable / nLanes
                                val xOff = gap + (slotW + gap) * lane.lane.toFloat()

                                TimelineEventCard(
                                    event = event,
                                    contentAlpha = if (showNowIndicator) {
                                        eventTemporalAlpha(event, now)
                                    } else {
                                        0.94f
                                    },
                                    accentColorOverride = scheduleTaskAccentColor(event.id),
                                    showDurationMinutes = true,
                                    modifier = Modifier
                                        .width(slotW)
                                        .offset(x = xOff, y = topY.dp)
                                        .height(heightY.dp),
                                )
                            }

                            if (showNowIndicator && nowM >= startM && nowM <= endM) {
                                val nowY = yAtMinute(slices, nowM)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .offset(y = (nowY - 5f).dp)
                                        .alpha(pulseAlpha),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(10.dp)
                                            .background(NowRed.copy(alpha = 0.25f), RoundedCornerShape(5.dp)),
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .background(NowRed, RoundedCornerShape(2.dp)),
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .offset(x = 6.dp, y = (nowY - 11f).dp)
                                        .background(NowRed, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 3.dp),
                                ) {
                                    Text(
                                        text = "NOW",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
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
