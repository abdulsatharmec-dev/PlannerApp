package com.dailycurator.ui.screens.schedule

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dailycurator.ui.components.ScheduleMomentumMapView
import com.dailycurator.ui.components.DefaultScheduleTimelineViewportHeight
import com.dailycurator.ui.components.PickDateDialog
import com.dailycurator.ui.components.ScheduleTimelineView
import com.dailycurator.ui.theme.appScreenBackground
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var showFullDatePicker by remember { mutableStateOf(false) }
    val scheduleShowsToday = state.scheduleTimelineDate == LocalDate.now()
    val clockDayFmt = remember { DateTimeFormatter.ofPattern("EEE, MMM d") }

    PickDateDialog(
        visible = showFullDatePicker,
        initialDate = state.scheduleTimelineDate,
        onDismiss = { showFullDatePicker = false },
        onConfirm = {
            viewModel.setScheduleTimelineDate(it)
            showFullDatePicker = false
        },
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .appScreenBackground()
            .padding(bottom = 4.dp),
    ) {
        ScheduleDateStrip(
            selected = state.scheduleTimelineDate,
            onSelect = { viewModel.setScheduleTimelineDate(it) },
            onRequestFullPicker = { showFullDatePicker = true },
            modifier = Modifier.padding(top = 4.dp),
        )
        Spacer(Modifier.height(8.dp))
        when (state.scheduleTab) {
            ScheduleTab.TIMELINE -> BoxWithConstraints(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                val viewportH =
                    if (maxHeight != Dp.Infinity && maxHeight > 1.dp) {
                        maxHeight
                    } else {
                        DefaultScheduleTimelineViewportHeight
                    }
                ScheduleTimelineView(
                    events = state.scheduleEvents,
                    windowStart = state.dayWindowStart,
                    windowEnd = state.dayWindowEnd,
                    showNowIndicator = scheduleShowsToday,
                    scheduleDate = state.scheduleTimelineDate,
                    viewportHeight = viewportH,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            ScheduleTab.MAP -> {
                ScheduleMomentumMapView(
                    events = state.scheduleEvents,
                    scheduleDate = state.scheduleTimelineDate,
                    windowStart = state.dayWindowStart,
                    windowEnd = state.dayWindowEnd,
                    useLiveNowIndicator = scheduleShowsToday,
                    scheduleDayLabel = state.scheduleTimelineDate.format(clockDayFmt),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            }
        }
    }
}
