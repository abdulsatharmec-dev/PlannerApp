package com.dailycurator.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ScrollState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
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

/** Default height when not embedded in a full-screen schedule page. */
val DefaultScheduleTimelineViewportHeight = 400.dp
/** Taller scale (~Google Calendar day view) so short blocks still fit a readable title. */
private const val BaseHourDp = 100f
private const val LaneStackGapDp = 3f

/** Time labels column; track uses remaining width. */
private val TimelineTimeColumnWidth = 58.dp
/** Single-column track: nearly full width with small inset. */
private val TrackEdgeInsetSingle = 4.dp
/** Multi-column track: inset from scroll viewport edges. */
private val TrackEdgeInsetMulti = 6.dp
/** Horizontal gap between simultaneous-event columns. */
private val TrackLaneSpacing = 6.dp
/** When overlaps require scrolling, each lane is at least this wide. */
private val TrackMinLaneWidth = 104.dp

private val DpPerMinute: Float get() = BaseHourDp / 60f

private fun minutesOfDayFloat(t: LocalTime): Float =
    t.hour * 60f + t.minute + t.second / 60f

/**
 * Minutes from midnight on the same notional calendar day. Used so hour iteration never wraps
 * past midnight (LocalTime.plusHours from 23:00 → 00:00 would break `while (t.isBefore(end))`).
 */
private fun minuteOfDayClock(t: LocalTime): Int =
    (t.hour * 60 + t.minute).coerceIn(0, 24 * 60 - 1)

private fun localTimeFromMinuteOfDay(m: Int): LocalTime {
    val mm = m.coerceIn(0, 24 * 60 - 1)
    return LocalTime.of(mm / 60, mm % 60)
}

private fun minTime(a: LocalTime, b: LocalTime): LocalTime = if (a.isBefore(b)) a else b

private fun maxTime(a: LocalTime, b: LocalTime): LocalTime = if (a.isBefore(b)) b else a

fun hourTickTimes(windowStart: LocalTime, windowEnd: LocalTime): List<LocalTime> {
    val startMin = minuteOfDayClock(windowStart)
    val endMin = minuteOfDayClock(windowEnd)
    if (endMin <= startMin) return emptyList()
    val out = mutableListOf<LocalTime>()
    var tickMin = (startMin / 60) * 60
    if (tickMin < startMin) tickMin += 60
    while (tickMin <= endMin) {
        out.add(localTimeFromMinuteOfDay(tickMin))
        tickMin += 60
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
    dpPerMinute: Float,
): List<TimelineSlice> {
    val out = mutableListOf<TimelineSlice>()
    val startMin = minuteOfDayClock(windowStart)
    val endMin = minuteOfDayClock(windowEnd)
    if (endMin <= startMin) return emptyList()
    var hourStartMin = (startMin / 60) * 60
    while (hourStartMin < endMin) {
        val segStart = localTimeFromMinuteOfDay(maxOf(startMin, hourStartMin))
        val segEnd = localTimeFromMinuteOfDay(minOf(endMin, hourStartMin + 60))
        val mins = ChronoUnit.MINUTES.between(segStart, segEnd)
        if (mins > 0) {
            val n = countTasksOverlappingSlice(events, segStart, segEnd, windowStart, windowEnd)
            // Fixed scale per minute: hour bands never stretch when more tasks overlap that hour;
            // overlaps use horizontal lanes + min card height instead.
            val h = dpPerMinute * mins
            out.add(TimelineSlice(segStart, segEnd, h, n))
        }
        hourStartMin += 60
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
    }.sortedWith(
        compareBy<Pair<ScheduleEvent, Pair<LocalTime, LocalTime>>> { it.second.first }
            .thenByDescending { r ->
                ChronoUnit.MINUTES.between(r.second.first, r.second.second)
            },
    )
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

private data class ClippedEvent(val event: ScheduleEvent, val start: LocalTime, val end: LocalTime)

private fun buildClippedSorted(
    events: List<ScheduleEvent>,
    windowStart: LocalTime,
    windowEnd: LocalTime,
): List<ClippedEvent> = events.mapNotNull { e ->
    clipEventToWindow(e, windowStart, windowEnd)?.let { (s, ed) -> ClippedEvent(e, s, ed) }
}.sortedWith(
    compareBy<ClippedEvent> { it.start }
        .thenByDescending { ChronoUnit.MINUTES.between(it.start, it.end) },
)

/**
 * Peak concurrent event count during [rangeStart, rangeEnd) using half-open intervals:
 * active at t iff !start.isAfter(t) && end.isAfter(t).
 */
private fun maxConcurrentDuring(
    rangeStart: LocalTime,
    rangeEnd: LocalTime,
    clipped: List<ClippedEvent>,
): Int {
    if (!rangeStart.isBefore(rangeEnd)) return 1
    val active = clipped.filter { it.start < rangeEnd && it.end > rangeStart }
    if (active.isEmpty()) return 1
    val checkPoints = mutableSetOf<LocalTime>()
    checkPoints.add(rangeStart)
    for (c in active) {
        if (!c.start.isBefore(rangeStart) && c.start.isBefore(rangeEnd)) checkPoints.add(c.start)
        if (c.end.isAfter(rangeStart) && c.end.isBefore(rangeEnd)) checkPoints.add(c.end)
    }
    var maxC = 1
    for (t in checkPoints.sorted()) {
        if (!t.isBefore(rangeStart) && t.isBefore(rangeEnd)) {
            val c = active.count { ev -> !ev.start.isAfter(t) && ev.end.isAfter(t) }
            maxC = maxOf(maxC, c)
        }
    }
    return maxC.coerceAtLeast(1)
}

/** Widest simultaneous stack any event participates in; drives min track width / horizontal scroll. */
private fun maxTrackColumnsNeeded(clipped: List<ClippedEvent>): Int =
    if (clipped.isEmpty()) 1
    else clipped.maxOf { maxConcurrentDuring(it.start, it.end, clipped) }.coerceAtLeast(1)

/** Y (dp axis) of the next block top in the same lane, or the window bottom if none. */
private fun ceilingYBeforeNextInLane(
    clipped: List<ClippedEvent>,
    laneMap: Map<Long, LaneInfo>,
    current: ScheduleEvent,
    currentEnd: LocalTime,
    slices: List<TimelineSlice>,
    windowEnd: LocalTime,
): Float {
    val myLane = laneMap[current.id]?.lane ?: 0
    var nextStart: LocalTime? = null
    for (c in clipped) {
        if (c.event.id == current.id) continue
        if ((laneMap[c.event.id]?.lane ?: 0) != myLane) continue
        if (c.start.isBefore(currentEnd)) continue
        if (nextStart == null || c.start.isBefore(nextStart)) nextStart = c.start
    }
    val boundaryTime = nextStart ?: windowEnd
    return yAtMinute(slices, minutesOfDayFloat(boundaryTime))
}

/**
 * Scrollable timeline with uniform hour scale, minute ticks in busy slices,
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
    /** Scroll viewport height; use a large value or [Dp.Infinity]-avoiding parent height for full-page schedule. */
    viewportHeight: Dp = DefaultScheduleTimelineViewportHeight,
) {
    val labelFmt = remember { DateTimeFormatter.ofPattern("h:mm a") }
    val minuteFmt = remember { DateTimeFormatter.ofPattern("h:mm") }

    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(showNowIndicator) {
        now = LocalTime.now()
        val interval = if (showNowIndicator) 30_000L else 120_000L
        while (true) {
            delay(interval)
            now = LocalTime.now()
        }
    }

    val slices = remember(windowStart, windowEnd, events) {
        buildTimelineSlices(windowStart, windowEnd, events, DpPerMinute)
    }
    val totalHeightInner = remember(slices) {
        slices.sumOf { it.heightDp.toDouble() }.toFloat()
    }

    val laneMap = remember(events, windowStart, windowEnd) {
        assignOverlapLanes(events, windowStart, windowEnd)
    }

    val clippedSorted = remember(events, windowStart, windowEnd) {
        buildClippedSorted(events, windowStart, windowEnd)
    }
    val maxTrackColumns = remember(clippedSorted) {
        maxTrackColumnsNeeded(clippedSorted)
    }
    val sortedEvents = remember(clippedSorted) {
        clippedSorted.map { it.event }
    }

    val hourTicks = remember(windowStart, windowEnd) { hourTickTimes(windowStart, windowEnd) }
    val density = LocalDensity.current
    val scrollState = remember(scheduleDate) { ScrollState(0) }

    val nowM = minutesOfDayFloat(now)
    val nowInDayWindow =
        !now.isBefore(windowStart) && !now.isAfter(windowEnd)

    var didSnapToNow by remember(windowStart, windowEnd, scheduleDate, showNowIndicator) {
        mutableStateOf(false)
    }
    LaunchedEffect(
        totalHeightInner,
        slices.size,
        windowStart,
        windowEnd,
        showNowIndicator,
        scheduleDate,
        viewportHeight,
    ) {
        if (!showNowIndicator || didSnapToNow || slices.isEmpty()) return@LaunchedEffect
        delay(64)
        val maxScroll = scrollState.maxValue
        if (maxScroll <= 0) {
            didSnapToNow = true
            return@LaunchedEffect
        }
        val snapNowM = minutesOfDayFloat(LocalTime.now())
        val nowY = yAtMinute(slices, snapNowM)
        val nowPx = with(density) { nowY.dp.toPx() }.roundToInt()
        val viewportPx = with(density) { viewportHeight.toPx() }.roundToInt()
        val target = (nowPx - viewportPx / 2).coerceIn(0, maxScroll)
        val delta = target - scrollState.value
        if (delta != 0) {
            scrollState.animateScrollBy(
                delta.toFloat(),
                animationSpec = tween(durationMillis = 420, easing = LinearEasing),
            )
        }
        didSnapToNow = true
    }

    val nowBandPulse = rememberInfiniteTransition(label = "now_band")
    val bandGlow by nowBandPulse.animateFloat(
        initialValue = 0.22f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bandGlow",
    )
    val dotPulse by nowBandPulse.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dotPulse",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(viewportHeight)
            .verticalScroll(scrollState),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalHeightInner.dp),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(totalHeightInner.dp),
            ) {
                Box(
                    Modifier
                        .width(TimelineTimeColumnWidth)
                        .fillMaxHeight(),
                ) {
                    val primary = MaterialTheme.colorScheme.primary
                    if (showNowIndicator && nowInDayWindow && slices.isNotEmpty()) {
                        val nowYDraw = yAtMinute(slices, nowM)
                        Canvas(Modifier.fillMaxSize()) {
                            val yPx = nowYDraw.dp.toPx()
                            val bandHalf = 44.dp.toPx()
                            val barW = 5.dp.toPx()
                            val top = yPx - bandHalf
                            val h = bandHalf * 2f
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        primary.copy(alpha = 0f),
                                        primary.copy(alpha = bandGlow * 0.95f),
                                        primary.copy(alpha = 0f),
                                    ),
                                    startY = top,
                                    endY = top + h,
                                ),
                                topLeft = Offset(0f, top),
                                size = Size(barW, h),
                                cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx()),
                            )
                            drawCircle(
                                color = NowRed.copy(alpha = dotPulse),
                                radius = 5.5.dp.toPx(),
                                center = Offset(barW / 2f, yPx),
                            )
                            drawCircle(
                                color = Color.White.copy(alpha = 0.65f * dotPulse),
                                radius = 2.5.dp.toPx(),
                                center = Offset(barW / 2f, yPx),
                            )
                        }
                    }
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
                LaunchedEffect(maxTrackColumns) {
                    hTrackScroll.scrollTo(0)
                }
                BoxWithConstraints(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) {
                    val viewportW = maxWidth
                    val nLaneCount = maxTrackColumns.coerceAtLeast(1)
                    val trackNeedsMultiColumnMinWidth = nLaneCount > 1
                    val contentW = if (!trackNeedsMultiColumnMinWidth) {
                        viewportW
                    } else {
                        val minNeeded =
                            TrackEdgeInsetMulti * 2 +
                                TrackMinLaneWidth * nLaneCount +
                                TrackLaneSpacing * (nLaneCount - 1)
                        maxOf(viewportW, minNeeded)
                    }
                    val needsHScroll = contentW > viewportW
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .then(
                                if (needsHScroll) Modifier.horizontalScroll(hTrackScroll) else Modifier,
                            ),
                    ) {
                        BoxWithConstraints(
                            Modifier
                                .then(
                                    if (needsHScroll) Modifier.width(contentW) else Modifier.fillMaxWidth(),
                                )
                                .fillMaxHeight(),
                        ) {
                            val trackWidth = maxWidth
                            val lineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.32f)

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

                            sortedEvents.forEach { event ->
                                val range = clipEventToWindow(event, windowStart, windowEnd) ?: return@forEach
                                val (es, ee) = range
                                val esM = minutesOfDayFloat(es)
                                val eeM = minutesOfDayFloat(ee)
                                val topY = yAtMinute(slices, esM)
                                val bottomY = yAtMinute(slices, eeM)
                                val naturalH = (bottomY - topY).coerceAtLeast(1f)
                                val ceilingY = ceilingYBeforeNextInLane(
                                    clippedSorted,
                                    laneMap,
                                    event,
                                    ee,
                                    slices,
                                    windowEnd,
                                )
                                val maxExpand =
                                    (ceilingY - topY - LaneStackGapDp).coerceAtLeast(naturalH)
                                val colsForEvent =
                                    maxConcurrentDuring(es, ee, clippedSorted).coerceAtLeast(1)
                                val minReadableH = when {
                                    colsForEvent >= 4 -> 42f
                                    colsForEvent >= 3 -> 44f
                                    else -> 48f
                                }
                                val heightY = max(naturalH, minReadableH)
                                    .coerceAtMost(maxExpand)
                                    .coerceAtLeast(6f)
                                val lane = laneMap[event.id] ?: LaneInfo(0, 0)
                                val laneIndex =
                                    lane.lane.coerceIn(0, (colsForEvent - 1).coerceAtLeast(0))
                                val eventIsSoloOnTimeline = colsForEvent <= 1
                                val (slotW, xOffDp) = if (eventIsSoloOnTimeline) {
                                    val inset = TrackEdgeInsetSingle
                                    val w = (trackWidth - inset * 2).coerceAtLeast(1.dp)
                                    w to inset
                                } else {
                                    val inset = TrackEdgeInsetMulti
                                    val inner =
                                        (trackWidth - inset * 2 - TrackLaneSpacing * (colsForEvent - 1))
                                            .coerceAtLeast(1.dp)
                                    val w = (inner / colsForEvent).coerceAtLeast(1.dp)
                                    val x = inset + (w + TrackLaneSpacing) * laneIndex
                                    w to x
                                }

                                TimelineEventCard(
                                    event = event,
                                    contentAlpha = if (showNowIndicator) {
                                        eventTemporalAlpha(event, now)
                                    } else {
                                        0.94f
                                    },
                                    accentColorOverride = scheduleTaskAccentColor(event.id),
                                    showDurationMinutes = true,
                                    narrowTrackColumn = !eventIsSoloOnTimeline && slotW < 118.dp,
                                    modifier = Modifier
                                        .width(slotW)
                                        .offset(x = xOffDp, y = topY.dp)
                                        .height(heightY.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                )
                            }
                        }
                    }
                }
            }

            if (showNowIndicator && nowInDayWindow && slices.isNotEmpty()) {
                val nowY = yAtMinute(slices, nowM)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(20f),
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val yPx = yAtMinute(slices, nowM).dp.toPx()
                        val w = size.width
                        val halo = 7.dp.toPx()
                        val core = 3.5.dp.toPx()
                        drawLine(
                            color = Color.Black.copy(alpha = 0.4f),
                            start = Offset(0f, yPx),
                            end = Offset(w, yPx),
                            strokeWidth = halo,
                            cap = StrokeCap.Round,
                        )
                        drawLine(
                            color = NowRed,
                            start = Offset(0f, yPx),
                            end = Offset(w, yPx),
                            strokeWidth = core,
                            cap = StrokeCap.Round,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .offset(x = 4.dp, y = (nowY - 16f).dp)
                            .background(NowRed, RoundedCornerShape(4.dp))
                            .padding(horizontal = 7.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "NOW",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                            ),
                        )
                    }
                }
            }
        }
    }
}
