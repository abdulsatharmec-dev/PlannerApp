package com.dailycurator.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.view.HapticFeedbackConstants
import com.dailycurator.data.model.ScheduleEvent
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.hypot

// Aligned with Stitch reference (daily momentum / morning ascent)
private val MomentumBg = Color(0xFFF5F3FF)
private val MomentumOnSurface = Color(0xFF302950)
private val MomentumOutline = Color(0xFF79719D)
private val MomentumPathDash = Color(0xFFB0A7D6)
private val MomentumPrimary = Color(0xFF4647D3)
private val MomentumPrimarySoft = Color(0xFF6B6AE0)
private val MomentumIconPurple = Color(0xFF5E4FB3)
private val MomentumMintActive = Color(0xFF69F6B8)
private val MomentumNavy = Color(0xFF1E2D5C)
private val MomentumGreenComplete = Color(0xFF34C759)
private val MomentumPaleLavender = Color(0xFFE8E4F5)
private val MomentumSurfaceCard = Color(0xFFFFFFFF)
private val MomentumBorderSubtle = Color(0xFFE0DCF0)
private val MomentumShadowTint = Color(0xFF4647D3).copy(alpha = 0.1f)
private val MomentumPinkBolt = Color(0xFFFF8EC7)

private enum class NodePhase { Completed, Active, Upcoming, Last }

private fun minutesOfDay(t: LocalTime): Int =
    t.hour * 60 + t.minute

private fun phaseForEvent(
    event: ScheduleEvent,
    effectiveNow: LocalTime,
    index: Int,
    total: Int,
): NodePhase {
    val nowM = minutesOfDay(effectiveNow)
    val startM = minutesOfDay(event.startTime)
    val endM = minutesOfDay(event.endTime)
    return when {
        nowM >= endM -> NodePhase.Completed
        nowM in startM until endM -> NodePhase.Active
        index == total - 1 && nowM < startM -> NodePhase.Last
        else -> NodePhase.Upcoming
    }
}

private fun pathTitleForNow(now: LocalTime): String = when (now.hour) {
    in 0..10 -> "The morning ascent"
    in 11..16 -> "The midday push"
    else -> "The evening wind-down"
}

private fun iconForIndex(i: Int): ImageVector {
    val cycle = listOf(
        Icons.Filled.Alarm,
        Icons.Filled.FitnessCenter,
        Icons.Filled.SelfImprovement,
        Icons.Filled.BusinessCenter,
        Icons.Filled.Bedtime,
        Icons.Filled.Event,
    )
    return cycle[i % cycle.size]
}

private fun nodeBoxAlignment(i: Int): Alignment = when (i % 5) {
    0 -> Alignment.TopCenter
    1 -> Alignment.TopEnd
    2 -> Alignment.TopStart
    3 -> Alignment.TopEnd
    else -> Alignment.TopCenter
}

private fun horizontalPaddingForIndex(i: Int): androidx.compose.foundation.layout.PaddingValues {
    val edge = 36.dp
    return when (i % 5) {
        1 -> androidx.compose.foundation.layout.PaddingValues(end = edge)
        2 -> androidx.compose.foundation.layout.PaddingValues(start = edge)
        3 -> androidx.compose.foundation.layout.PaddingValues(end = edge)
        else -> androidx.compose.foundation.layout.PaddingValues()
    }
}

private val PillWidthDp = 96.dp
private val PathEdgePaddingDp = 36.dp

/**
 * Winding “S” path like the reference: each segment is a quadratic with the control
 * shifted horizontally at the segment midpoint (smooth alternate left/right bows).
 */
private fun buildWindingDashPath(points: List<Offset>): Path {
    val path = Path()
    if (points.size < 2) return path
    path.moveTo(points[0].x, points[0].y)
    for (i in 1 until points.size) {
        val p0 = points[i - 1]
        val p1 = points[i]
        val dx = p1.x - p0.x
        val dy = p1.y - p0.y
        val len = hypot(dx.toDouble(), dy.toDouble()).toFloat().coerceAtLeast(1f)
        val midX = (p0.x + p1.x) / 2f
        val midY = (p0.y + p1.y) / 2f
        // Horizontal bow strength scales with segment length (mostly vertical travel).
        val sway = (len * 0.52f).coerceIn(48f, 132f) * if (i % 2 == 0) 1f else -1f
        path.quadraticBezierTo(midX + sway, midY, p1.x, p1.y)
    }
    return path
}

/**
 * Position for the live “now” marker: on a stop during its window, lerping along the straight
 * chord between stops in gaps (reads clearly against the winding dashed path).
 */
private fun computeNowIndicatorOnPath(
    sorted: List<ScheduleEvent>,
    centers: List<Offset>,
    now: LocalTime,
    beforeFirstNudgeYPx: Float,
): Offset? {
    if (sorted.isEmpty() || centers.size != sorted.size) return null
    val n = sorted.size
    val nowM = minutesOfDay(now)

    val firstStartM = minutesOfDay(sorted.first().startTime)
    if (nowM < firstStartM) {
        return Offset(centers[0].x, centers[0].y - beforeFirstNudgeYPx)
    }

    val lastEndM = minutesOfDay(sorted.last().endTime)
    if (nowM >= lastEndM) {
        return centers.last()
    }

    for (i in sorted.indices) {
        val ev = sorted[i]
        val startM = minutesOfDay(ev.startTime)
        val endM = minutesOfDay(ev.endTime)

        if (nowM >= startM && nowM < endM) {
            return centers[i]
        }

        if (i < n - 1) {
            val nextStartM = minutesOfDay(sorted[i + 1].startTime)
            if (nowM >= endM && nowM < nextStartM) {
                val gap = nextStartM - endM
                val t = if (gap <= 0) {
                    0f
                } else {
                    (nowM - endM).toFloat() / gap.toFloat()
                }
                val a = centers[i]
                val b = centers[i + 1]
                return Offset(
                    a.x + (b.x - a.x) * t,
                    a.y + (b.y - a.y) * t,
                )
            }
        }
    }

    return centers.last()
}

@Composable
fun ScheduleMomentumMapView(
    events: List<ScheduleEvent>,
    scheduleDate: LocalDate,
    windowStart: LocalTime,
    windowEnd: LocalTime,
    useLiveNowIndicator: Boolean,
    scheduleDayLabel: String,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val timeFmt = remember { DateTimeFormatter.ofPattern("h:mm a") }
    var now by remember(scheduleDate, useLiveNowIndicator) {
        mutableStateOf(LocalTime.now())
    }

    LaunchedEffect(useLiveNowIndicator, scheduleDate) {
        if (!useLiveNowIndicator || scheduleDate != LocalDate.now()) return@LaunchedEffect
        now = LocalTime.now()
        while (true) {
            delay(15_000L)
            now = LocalTime.now()
        }
    }

    val effectiveNow = remember(now, scheduleDate, useLiveNowIndicator, windowStart, windowEnd) {
        when {
            useLiveNowIndicator && scheduleDate == LocalDate.now() -> now
            scheduleDate.isBefore(LocalDate.now()) -> windowEnd
            scheduleDate.isAfter(LocalDate.now()) -> windowStart
            else -> windowStart
        }
    }

    val sorted = remember(events) { events.sortedBy { it.startTime } }
    val pathTitle = remember(effectiveNow, useLiveNowIndicator, scheduleDate) {
        when {
            scheduleDate.isBefore(LocalDate.now()) -> "Day complete"
            scheduleDate.isAfter(LocalDate.now()) -> "Up ahead"
            useLiveNowIndicator -> pathTitleForNow(effectiveNow)
            else -> scheduleDayLabel.lowercase()
        }
    }

    val nextIncomplete = sorted.firstOrNull { minutesOfDay(effectiveNow) < minutesOfDay(it.endTime) }
    val cardSubtitle = when {
        scheduleDate.isBefore(LocalDate.now()) ->
            "Nice work — you can review this path anytime."
        sorted.isEmpty() ->
            "Add tasks on this day to build your daily path."
        nextIncomplete == null ->
            "You're all caught up for this day."
        else ->
            "Finish ${nextIncomplete.title.lowercase()} to keep your streak going."
    }

    val rowHeight = 144.dp
    val topPad = 64.dp
    val scroll = rememberScrollState()
    val pathTransition = rememberInfiniteTransition(label = "pathFlow")
    val dashPhase by pathTransition.animateFloat(
        initialValue = 0f,
        targetValue = 40f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "dash",
    )
    val nowPulseTransition = rememberInfiniteTransition(label = "nowPulse")
    val nowPulse by nowPulseTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "nowPulseScale",
    )

    val showLiveClock = useLiveNowIndicator && scheduleDate == LocalDate.now()

    Column(
        modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(MomentumBg)
            },
    ) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(scroll)
                .padding(horizontal = 22.dp)
                .padding(top = 12.dp, bottom = 32.dp),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Current progress",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 2.2.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MomentumOutline,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = pathTitle,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.6).sp,
                    ),
                    color = MomentumOnSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = scheduleDayLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MomentumOutline,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(16.dp))

            if (sorted.isEmpty()) {
                MomentumEmptyPathState()
            } else {
                val total = sorted.size
                BoxWithConstraints(
                    Modifier
                        .fillMaxWidth()
                        .height(rowHeight * total + topPad),
                ) {
                    val wPx = constraints.maxWidth.toFloat()
                    val rowPx = with(density) { rowHeight.toPx() }
                    val topPx = with(density) { topPad.toPx() }
                    val edgePx = with(density) { PathEdgePaddingDp.toPx() }
                    val halfPillPx = with(density) { (PillWidthDp / 2).toPx() }
                    // Row is top-aligned; pill is 64.dp tall with chips — geometric center ~40.dp from row top.
                    val pillCenterYInRowPx = with(density) { 40.dp.toPx() }

                    fun anchorX(i: Int): Float = when (i % 5) {
                        0 -> wPx / 2f
                        1 -> (wPx - edgePx - halfPillPx).coerceAtLeast(halfPillPx)
                        2 -> (edgePx + halfPillPx).coerceAtMost(wPx - halfPillPx)
                        3 -> (wPx - edgePx - halfPillPx).coerceAtLeast(halfPillPx)
                        else -> wPx / 2f
                    }

                    val centers = sorted.indices.map { i ->
                        Offset(
                            x = anchorX(i),
                            y = topPx + rowPx * i + pillCenterYInRowPx,
                        )
                    }

                    val beforeFirstNudgePx = with(density) { 14.dp.toPx() }
                    val nowOnPath = if (showLiveClock) {
                        computeNowIndicatorOnPath(sorted, centers, now, beforeFirstNudgePx)
                    } else {
                        null
                    }

                    Box(Modifier.fillMaxSize()) {
                        Canvas(Modifier.fillMaxSize()) {
                            if (centers.size >= 2) {
                                val path = buildWindingDashPath(centers)
                                drawPath(
                                    path = path,
                                    color = MomentumPathDash.copy(alpha = 0.4f),
                                    style = Stroke(
                                        width = 9.dp.toPx(),
                                        cap = StrokeCap.Round,
                                    ),
                                )
                                drawPath(
                                    path = path,
                                    color = MomentumPathDash,
                                    style = Stroke(
                                        width = 3.dp.toPx(),
                                        cap = StrokeCap.Round,
                                        pathEffect = PathEffect.dashPathEffect(
                                            floatArrayOf(14f, 10f),
                                            dashPhase,
                                        ),
                                    ),
                                )
                            }
                            nowOnPath?.let { p ->
                                val baseR = 7.dp.toPx()
                                val r = baseR * nowPulse
                                val halo = 3.dp.toPx()
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.95f),
                                    radius = r + halo,
                                    center = p,
                                )
                                drawCircle(
                                    color = MomentumPrimary,
                                    radius = r,
                                    center = p,
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = r * 0.38f,
                                    center = p,
                                )
                                drawCircle(
                                    color = MomentumPrimary.copy(alpha = 0.35f),
                                    radius = r + halo + 2.dp.toPx(),
                                    center = p,
                                    style = Stroke(width = 1.5.dp.toPx()),
                                )
                            }
                        }

                        if (showLiveClock && nowOnPath != null) {
                            val p = nowOnPath
                            Text(
                                text = timeFmt.format(now),
                                modifier = Modifier.offset(
                                    x = with(density) { p.x.toDp() } + 10.dp,
                                    y = with(density) { p.y.toDp() } - 8.dp,
                                )
                                    .background(
                                        color = Color.White.copy(alpha = 0.92f),
                                        shape = RoundedCornerShape(4.dp),
                                    )
                                    .border(1.dp, MomentumPathDash.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 0.2.sp,
                                ),
                                color = MomentumOnSurface.copy(alpha = 0.88f),
                                maxLines = 1,
                            )
                        }

                        Column(Modifier.fillMaxSize()) {
                            Spacer(Modifier.height(topPad))
                            sorted.forEachIndexed { index, event ->
                                val phase = phaseForEvent(
                                    event,
                                    effectiveNow,
                                    index,
                                    total,
                                )
                                MomentumPathStopRow(
                                    event = event,
                                    index = index,
                                    scheduleDate = scheduleDate,
                                    rowHeight = rowHeight,
                                    phase = phase,
                                    timeFmt = timeFmt,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            MomentumBoostCard(cardSubtitle)
        }
    }
}

@Composable
private fun MomentumPathStopRow(
    event: ScheduleEvent,
    index: Int,
    scheduleDate: LocalDate,
    rowHeight: Dp,
    phase: NodePhase,
    timeFmt: DateTimeFormatter,
) {
    var appeared by remember(event.id, scheduleDate) { mutableStateOf(false) }
    LaunchedEffect(event.id, scheduleDate, index) {
        delay(index * 55L)
        appeared = true
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(rowHeight)
            .padding(horizontalPaddingForIndex(index)),
        contentAlignment = nodeBoxAlignment(index),
    ) {
        AnimatedVisibility(
            visible = appeared,
            enter = fadeIn(
                tween(420, easing = LinearEasing),
            ) + slideInVertically(
                initialOffsetY = { it / 5 },
                animationSpec = tween(420, easing = LinearEasing),
            ),
        ) {
            MomentumMapNode(
                title = event.title,
                subtitle = nodeSubtitle(phase, event, timeFmt),
                icon = iconForIndex(index),
                phase = phase,
            )
        }
    }
}

private fun nodeSubtitle(
    phase: NodePhase,
    event: ScheduleEvent,
    timeFmt: DateTimeFormatter,
): String = when (phase) {
    NodePhase.Completed -> "COMPLETED"
    NodePhase.Active -> timeFmt.format(event.startTime)
    NodePhase.Last -> timeFmt.format(event.startTime)
    NodePhase.Upcoming -> timeFmt.format(event.startTime)
}

@Composable
private fun MomentumEmptyPathState() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp)
            .shadow(8.dp, RoundedCornerShape(24.dp), spotColor = MomentumShadowTint)
            .clip(RoundedCornerShape(24.dp))
            .background(MomentumSurfaceCard)
            .border(1.dp, MomentumBorderSubtle, RoundedCornerShape(24.dp))
            .padding(horizontal = 28.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MomentumPrimary.copy(alpha = 0.1f),
        ) {
            Icon(
                imageVector = Icons.Filled.Map,
                contentDescription = null,
                tint = MomentumPrimary,
                modifier = Modifier.padding(14.dp).size(28.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "No stops on this map yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MomentumOnSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Tasks you schedule for this day appear here as a playful path — add one from Tasks or Timeline.",
            style = MaterialTheme.typography.bodyMedium,
            color = MomentumOutline,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
        )
    }
}

@Composable
private fun MomentumBoostCard(subtitle: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = MomentumShadowTint,
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MomentumSurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MomentumBorderSubtle),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MomentumPinkBolt),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Bolt,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp),
                )
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = "momentum boost",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MomentumOnSurface,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MomentumOutline,
                    lineHeight = 22.sp,
                )
            }
        }
    }
}

@Composable
private fun MomentumMapNode(
    title: String,
    subtitle: String,
    icon: ImageVector,
    phase: NodePhase,
) {
    val view = LocalView.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = tween(180),
        label = "press",
    )

    val glowTransition = rememberInfiniteTransition(label = "activeGlow")
    val glowAlpha by glowTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow",
    )

    val pillShape = RoundedCornerShape(50)
    val nodeShadow = when (phase) {
        NodePhase.Completed -> Modifier.shadow(
            6.dp,
            pillShape,
            spotColor = MomentumPrimary.copy(alpha = 0.2f),
        )
        NodePhase.Active -> Modifier.shadow(
            8.dp,
            pillShape,
            spotColor = MomentumMintActive.copy(alpha = 0.35f),
        )
        NodePhase.Upcoming -> Modifier.shadow(
            2.dp,
            pillShape,
            spotColor = Color.Black.copy(alpha = 0.06f),
        )
        NodePhase.Last -> Modifier.shadow(
            4.dp,
            pillShape,
            spotColor = MomentumPathDash.copy(alpha = 0.2f),
        )
    }
    val subtitleColor = MomentumOutline

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                interactionSource = interaction,
                indication = rememberRipple(bounded = true, radius = 88.dp),
            ) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            },
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (phase == NodePhase.Active) {
                Box(
                    Modifier
                        .size(width = 112.dp, height = 76.dp)
                        .graphicsLayer { alpha = glowAlpha }
                        .background(
                            color = MomentumMintActive.copy(alpha = 0.35f),
                            shape = pillShape,
                        ),
                )
            }
            when (phase) {
                NodePhase.Completed -> {
                    Box(
                        modifier = nodeShadow
                            .graphicsLayer {
                                scaleX = pressScale
                                scaleY = pressScale
                            }
                            .size(width = 96.dp, height = 64.dp)
                            .clip(pillShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(MomentumPrimary, MomentumPrimarySoft),
                                ),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = Color.White,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 6.dp, y = (-6).dp)
                            .size(22.dp),
                        shape = RoundedCornerShape(50),
                        color = MomentumGreenComplete,
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp),
                            )
                        }
                    }
                }
                NodePhase.Active -> {
                    Box(
                        modifier = nodeShadow
                            .graphicsLayer {
                                scaleX = pressScale
                                scaleY = pressScale
                            }
                            .size(width = 96.dp, height = 64.dp)
                            .clip(pillShape)
                            .background(MomentumMintActive),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = MomentumNavy,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-8).dp),
                        shape = RoundedCornerShape(40),
                        color = MomentumNavy,
                    ) {
                        Text(
                            text = "ACTIVE",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.3.sp,
                            ),
                            color = Color.White,
                        )
                    }
                }
                NodePhase.Upcoming -> {
                    Box(
                        modifier = nodeShadow
                            .graphicsLayer {
                                scaleX = pressScale
                                scaleY = pressScale
                            }
                            .size(width = 96.dp, height = 64.dp)
                            .clip(pillShape)
                            .background(MomentumPaleLavender.copy(alpha = 0.92f))
                            .border(1.dp, Color.White.copy(alpha = 0.55f), pillShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = MomentumIconPurple,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                }
                NodePhase.Last -> {
                    Box(
                        modifier = nodeShadow
                            .graphicsLayer {
                                scaleX = pressScale
                                scaleY = pressScale
                            }
                            .size(width = 96.dp, height = 64.dp)
                            .clip(pillShape)
                            .background(MomentumPaleLavender.copy(alpha = 0.88f))
                            .drawBehind {
                                val sw = 2.dp.toPx()
                                val r = 32.dp.toPx()
                                drawRoundRect(
                                    color = MomentumPathDash,
                                    topLeft = Offset(sw / 2, sw / 2),
                                    size = Size(size.width - sw, size.height - sw),
                                    style = Stroke(
                                        width = sw,
                                        pathEffect = PathEffect.dashPathEffect(
                                            floatArrayOf(8f, 6f),
                                            0f,
                                        ),
                                    ),
                                    cornerRadius = CornerRadius(r, r),
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = MomentumIconPurple,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = title.lowercase(),
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.2).sp,
            ),
            color = MomentumOnSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = if (phase == NodePhase.Completed) 1.1.sp else 0.5.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = subtitleColor,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}
