package com.dailycurator.ui.screens.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

private const val DayWindowPast = 120
private const val DayWindowFuture = 120

/** Monday of the ISO week containing [date] (e.g. Apr 6–12 for Apr 10). */
private fun startOfIsoWeek(date: LocalDate): LocalDate =
    date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

@Composable
fun ScheduleDateStrip(
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit,
    onRequestFullPicker: () -> Unit,
    modifier: Modifier = Modifier,
    /** LazyRow top/bottom inset; use a smaller value when embedding under a tight toolbar (e.g. Tasks). */
    contentPaddingVertical: Dp = 6.dp,
    /** Padding inside each day chip (vertical). */
    dayCellVerticalPadding: Dp = 8.dp,
) {
    val today = LocalDate.now()
    // Anchor the strip to the current calendar day so the list does not drift after days/weeks
    // of process uptime (a plain remember { } would freeze rangeStart to first launch).
    val rangeStart = today.minusDays(DayWindowPast.toLong())
    val days = remember(rangeStart) {
        (0..(DayWindowPast + DayWindowFuture)).map { rangeStart.plusDays(it.toLong()) }
    }
    val selectedIndex = remember(selected, days, rangeStart) {
        val idx = days.indexOfFirst { it == selected }
        when {
            idx >= 0 -> idx
            selected.isBefore(days.first()) -> 0
            selected.isAfter(days.last()) -> days.lastIndex
            else -> ChronoUnit.DAYS.between(rangeStart, selected).toInt().coerceIn(0, days.lastIndex)
        }
    }
    val weekStart = remember(selected) { startOfIsoWeek(selected) }
    val anchorIndex = remember(weekStart, days, rangeStart, selectedIndex) {
        val i = days.indexOfFirst { it == weekStart }
        when {
            i >= 0 -> i
            weekStart.isBefore(days.first()) -> 0
            weekStart.isAfter(days.last()) -> days.lastIndex
            else -> selectedIndex
        }
    }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = anchorIndex.coerceIn(0, days.lastIndex.coerceAtLeast(0)),
        initialFirstVisibleItemScrollOffset = 0,
    )
    val density = LocalDensity.current
    var verticalDrag by remember { mutableFloatStateOf(0f) }
    var rowWidthPx by remember { mutableIntStateOf(0) }

    // Pin the ISO week (Mon–Sun): put Monday of the selected week at the leading edge so the
    // current week (e.g. Apr 6–12) is visible instead of centering mid-week on the selected day.
    LaunchedEffect(anchorIndex, rowWidthPx) {
        if (rowWidthPx <= 0) return@LaunchedEffect
        val idx = anchorIndex.coerceIn(0, days.lastIndex)
        val leadPad = with(density) { 12.dp.roundToPx() }
        listState.animateScrollToItem(idx, scrollOffset = leadPad)
    }

    val dayLetterFmt = remember { DateTimeFormatter.ofPattern("EEE") }

    LazyRow(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { rowWidthPx = it.width }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        verticalDrag += dragAmount
                    },
                    onDragEnd = {
                        val threshold = with(density) { 40.dp.toPx() }
                        if (verticalDrag > threshold) {
                            onRequestFullPicker()
                        }
                        verticalDrag = 0f
                    },
                    onDragCancel = { verticalDrag = 0f },
                )
            },
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = contentPaddingVertical),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        itemsIndexed(days, key = { _, d -> d.toString() }) { _, date ->
            val isSelected = date == selected
            val isToday = date == today
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .widthIn(min = 46.dp, max = 56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when {
                            isSelected -> MaterialTheme.colorScheme.primary
                            isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                        },
                    )
                    .clickable { onSelect(date) }
                    .padding(horizontal = 6.dp, vertical = dayCellVerticalPadding),
            ) {
                Text(
                    text = date.format(dayLetterFmt),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = when {
                            isSelected -> MaterialTheme.colorScheme.onPrimary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = when {
                            isSelected -> MaterialTheme.colorScheme.onPrimary
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                    ),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
