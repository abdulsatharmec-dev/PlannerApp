package com.dailycurator.ui.screens.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CalendarViewDay
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dailycurator.ui.theme.appScaffoldContainerColor
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleTopAppBar(
    viewModel: ScheduleViewModel,
    onOpenMenu: () -> Unit,
    onNavigateSettings: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val monthFmt = remember { DateTimeFormatter.ofPattern("MMMM yyyy") }
    val monthLabel = state.scheduleTimelineDate.format(monthFmt)

    TopAppBar(
        title = { },
        navigationIcon = {
            IconButton(onClick = onOpenMenu) {
                Icon(Icons.Default.Menu, contentDescription = "Open menu")
            }
        },
        actions = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                IconButton(
                    onClick = { viewModel.setScheduleTab(ScheduleTab.TIMELINE) },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (state.scheduleTab == ScheduleTab.TIMELINE) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        contentColor = if (state.scheduleTab == ScheduleTab.TIMELINE) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    ),
                ) {
                    Icon(
                        Icons.Outlined.CalendarViewDay,
                        contentDescription = "Timeline view",
                    )
                }
                IconButton(
                    onClick = { viewModel.setScheduleTab(ScheduleTab.MAP) },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (state.scheduleTab == ScheduleTab.MAP) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        contentColor = if (state.scheduleTab == ScheduleTab.MAP) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    ),
                ) {
                    Icon(Icons.Filled.Map, contentDescription = "Map view")
                }
                IconButton(onClick = onNavigateSettings) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = monthLabel,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .widthIn(max = 120.dp)
                        .padding(end = 8.dp),
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = appScaffoldContainerColor(),
        ),
    )
}
