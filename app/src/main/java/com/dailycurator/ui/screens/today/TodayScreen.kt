package com.dailycurator.ui.screens.today

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dailycurator.data.model.PriorityTask
import com.dailycurator.data.model.Urgency
import com.dailycurator.ui.components.*
import com.dailycurator.ui.theme.*
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun TodayScreen(viewModel: TodayViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val todayLabel = LocalDate.now().format(DateTimeFormatter.ofPattern("MMM d"))
    var showAddTask by remember { mutableStateOf(false) }

    if (showAddTask) {
        AddTaskDialog(
            onDismiss = { showAddTask = false },
            onConfirm = { title, start, end, urgency ->
                viewModel.addTask(title, start, end, urgency)
                showAddTask = false
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // ── Top App Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.BookmarkBorder, contentDescription = null,
                        tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text("Daily Curator",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground))
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.AutoAwesome, contentDescription = "AI",
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            }
        }

        // ── AI Insight Card
        item {
            AIInsightCard(
                insight = state.insight,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(24.dp))
        }

        // ── Top 5 Priorities header
        item {
            Text("Top 5 Priorities",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.onBackground),
                modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(10.dp))
        }

        // ── Priority items
        items(state.tasks, key = { it.id }) { task ->
            PriorityItem(
                task = task,
                onToggleDone = { viewModel.toggleTaskDone(task) },
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(8.dp))
        }

        // ── New Task button
        item {
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = { showAddTask = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(6.dp))
                Text("New Task",
                    style = MaterialTheme.typography.titleMedium.copy(color = Color.White))
            }
            Spacer(Modifier.height(24.dp))
        }

        // ── Weekly Goals section
        item {
            WeeklyGoalsSection(
                goals = state.goals,
                collapsed = state.goalsCollapsed,
                onToggleCollapse = viewModel::toggleGoalsCollapsed,
                onToggleGoal = viewModel::toggleGoal,
                onAddGoal = viewModel::addGoal,
                insight = state.insight
            )
            Spacer(Modifier.height(24.dp))
        }

        // ── Today's Schedule
        item {
            TodayScheduleSection(
                events = state.scheduleEvents,
                activeTab = state.scheduleTab,
                onTabChange = viewModel::setScheduleTab,
                dateLabel = todayLabel
            )
        }
    }
}

// ── Add Task Dialog ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, LocalTime, LocalTime, Urgency) -> Unit
) {
    var title     by remember { mutableStateOf("") }
    var startText by remember { mutableStateOf("09:00") }
    var endText   by remember { mutableStateOf("10:00") }
    var urgency   by remember { mutableStateOf(Urgency.GREEN) }
    var titleError by remember { mutableStateOf(false) }

    val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text("New Task",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; titleError = false },
                    label = { Text("Task title") },
                    isError = titleError,
                    supportingText = { if (titleError) Text("Title is required") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startText,
                        onValueChange = { startText = it },
                        label = { Text("Start (HH:mm)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endText,
                        onValueChange = { endText = it },
                        label = { Text("End (HH:mm)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                // Urgency chips
                Text("Priority", style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Urgency.values().forEach { u ->
                        val label = when (u) {
                            Urgency.GREEN   -> "Normal"
                            Urgency.RED     -> "High"
                            Urgency.NEUTRAL -> "Low"
                        }
                        val chipColor = when (u) {
                            Urgency.GREEN   -> AccentGreen
                            Urgency.RED     -> AccentRed
                            Urgency.NEUTRAL -> MaterialTheme.colorScheme.outline
                        }
                        FilterChip(
                            selected = urgency == u,
                            onClick = { urgency = u },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = chipColor.copy(alpha = 0.15f),
                                selectedLabelColor = chipColor
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (title.isBlank()) { titleError = true; return@Button }
                val start = runCatching { LocalTime.parse(startText, timeFmt) }.getOrElse { LocalTime.of(9, 0) }
                val end   = runCatching { LocalTime.parse(endText, timeFmt) }.getOrElse { start.plusHours(1) }
                onConfirm(title.trim(), start, end, urgency)
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ── Weekly Goals Section ───────────────────────────────────────────────────

@Composable
private fun WeeklyGoalsSection(
    goals: List<com.dailycurator.data.model.WeeklyGoal>,
    collapsed: Boolean,
    onToggleCollapse: () -> Unit,
    onToggleGoal: (com.dailycurator.data.model.WeeklyGoal) -> Unit,
    onAddGoal: (String) -> Unit,
    insight: com.dailycurator.data.model.AiInsight
) {
    val completedCount = goals.count { it.isCompleted }
    var showAddGoal by remember { mutableStateOf(false) }

    if (showAddGoal) {
        AddWeeklyGoalInlineDialog(onDismiss = { showAddGoal = false }, onConfirm = { title ->
            onAddGoal(title)
            showAddGoal = false
        })
    }

    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Weekly Goals",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = MaterialTheme.colorScheme.primary))
                Text("PROGRESS: $completedCount/${goals.size} GOALS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = AccentGreen, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp))
            }
            IconButton(onClick = { showAddGoal = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add goal",
                    tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onToggleCollapse) {
                Icon(
                    if (collapsed) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                    contentDescription = "Toggle", tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        // AI Goal Insight
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                Box(modifier = Modifier
                    .width(3.dp).fillMaxHeight()
                    .background(AccentGreen, RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)))
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(AccentGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null,
                                tint = AccentGreen, modifier = Modifier.size(13.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "AI Insight: Slight delay on Strategy Audit due to unplanned syncs this morning.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface)
                        )
                    }
                    if (insight.recoveryPlan != null) {
                        Spacer(Modifier.height(6.dp))
                        Text("Recovery plan: ${insight.recoveryPlan}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Normal))
                    }
                }
            }
        }

        // Goals list (animated collapse)
        AnimatedVisibility(visible = !collapsed) {
            Column {
                Spacer(Modifier.height(4.dp))
                goals.forEach { goal ->
                    GoalListItem(goal = goal, onToggle = { onToggleGoal(goal) })
                }
            }
        }
    }
}

@Composable
private fun AddWeeklyGoalInlineDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("New Weekly Goal",
            style = MaterialTheme.typography.titleLarge.copy(
                color = MaterialTheme.colorScheme.onSurface)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Goal title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { if (text.isNotBlank()) onConfirm(text.trim()) }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ── Today's Schedule Section ───────────────────────────────────────────────

@Composable
private fun TodayScheduleSection(
    events: List<com.dailycurator.data.model.ScheduleEvent>,
    activeTab: ScheduleTab,
    onTabChange: (ScheduleTab) -> Unit,
    dateLabel: String
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text("TODAY",
            style = MaterialTheme.typography.labelSmall.copy(
                color = AccentGreen, fontWeight = FontWeight.Bold, letterSpacing = 1.sp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Schedule",
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground),
                modifier = Modifier.weight(1f))
            Text(dateLabel,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant))
        }
        Spacer(Modifier.height(10.dp))

        // Tab chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ScheduleTab.values().forEach { tab ->
                val isActive = activeTab == tab
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onTabChange(tab) }
                        .padding(horizontal = 16.dp, vertical = 7.dp)
                ) {
                    Text(tab.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal))
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        // Timeline or Clock
        Crossfade(targetState = activeTab, label = "schedule_tab") { tab ->
            when (tab) {
                ScheduleTab.TIMELINE -> TimelineView(events = events)
                ScheduleTab.CLOCK    -> ClockView(events = events)
            }
        }
    }
}

// ── Timeline View with live "NOW" indicator ────────────────────────────────

@Composable
private fun TimelineView(events: List<com.dailycurator.data.model.ScheduleEvent>) {
    // Refresh current time every minute
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            now = LocalTime.now()
        }
    }
    val fmt = DateTimeFormatter.ofPattern("HH:mm")

    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        // Track whether we've already shown the now-marker
        var nowInserted = false

        // Show NOW at the very top if it's before all events
        if (events.isNotEmpty() && now.isBefore(events.first().startTime)) {
            NowMarker(now = now, fmt = fmt)
            nowInserted = true
        }

        events.forEachIndexed { index, event ->
            // Insert NOW between events if it falls in this gap
            if (!nowInserted && now.isAfter(event.endTime)) {
                val nextEvent = events.getOrNull(index + 1)
                if (nextEvent == null || now.isBefore(nextEvent.startTime)) {
                    NowMarker(now = now, fmt = fmt)
                    nowInserted = true
                }
            }
            // Insert NOW if it's currently during this event
            if (!nowInserted && now.isAfter(event.startTime) && now.isBefore(event.endTime)) {
                NowMarker(now = now, fmt = fmt)
                nowInserted = true
            }

            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                Text(event.startTime.format(fmt),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant),
                    modifier = Modifier.width(48.dp).padding(top = 10.dp))
                Spacer(Modifier.width(8.dp))
                TimelineEventCard(event = event, modifier = Modifier.weight(1f))
            }
        }

        // Show NOW after all events have passed
        if (!nowInserted && events.isNotEmpty() && now.isAfter(events.last().endTime)) {
            NowMarker(now = now, fmt = fmt)
        }
    }
}

@Composable
private fun NowMarker(now: LocalTime, fmt: DateTimeFormatter) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Time pill badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(NowRed)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                now.format(fmt),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
            )
        }
        Spacer(Modifier.width(8.dp))
        NowIndicator(modifier = Modifier.weight(1f))
        // Red dot at the end
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(NowRed)
        )
    }
}
