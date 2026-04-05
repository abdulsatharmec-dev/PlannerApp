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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dailycurator.data.model.AiInsight
import com.dailycurator.data.model.PriorityTask
import com.dailycurator.data.model.Urgency
import com.dailycurator.ui.components.*
import com.dailycurator.ui.theme.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun TodayScreen(viewModel: TodayViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val todayLabel = LocalDate.now().format(DateTimeFormatter.ofPattern("MMM d"))
    var assistantExpanded by remember { mutableStateOf(true) }
    var weeklyInsightExpanded by remember { mutableStateOf(true) }

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
                        tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
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

        item {
            DayWindowProgressBar(
                windowStart = state.dayWindowStart,
                windowEnd = state.dayWindowEnd,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(16.dp))
        }

        // ── Assistant insight (optional)
        item {
            if (state.assistantInsightEnabled) {
                val insight = when {
                    !state.cerebrasConfigured -> AiInsight(
                        insightText = "Add your Cerebras API key in Settings to generate a daily assistant insight from your tasks, goals, and habits.",
                        boldPart = "Connect Cerebras",
                    )
                    else -> state.assistantInsight
                }
                AIInsightCard(
                    insight = insight,
                    modifier = Modifier.padding(horizontal = 20.dp),
                    expanded = assistantExpanded,
                    onExpandedChange = { assistantExpanded = it },
                    onRegenerate = if (state.cerebrasConfigured) {
                        { viewModel.regenerateAssistantInsight() }
                    } else null,
                    isRegenerating = state.assistantInsightLoading,
                    showRegenerate = state.cerebrasConfigured,
                )
                Spacer(Modifier.height(24.dp))
            }
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



        // ── Weekly Goals section
        item {
            WeeklyGoalsSection(
                goals = state.goals,
                collapsed = state.goalsCollapsed,
                onToggleCollapse = viewModel::toggleGoalsCollapsed,
                onToggleGoal = viewModel::toggleGoal,
                onAddGoal = viewModel::addGoal,
                weeklyInsightEnabled = state.weeklyGoalsInsightEnabled,
                cerebrasConfigured = state.cerebrasConfigured,
                weeklyInsight = state.weeklyGoalsInsight,
                weeklyInsightExpanded = weeklyInsightExpanded,
                onWeeklyInsightExpandedChange = { weeklyInsightExpanded = it },
                onRegenerateWeeklyInsight = { viewModel.regenerateWeeklyGoalsInsight() },
                weeklyInsightLoading = state.weeklyGoalsInsightLoading,
            )
            Spacer(Modifier.height(24.dp))
        }

        // ── Today's Schedule
        item {
            TodayScheduleSection(
                events = state.scheduleEvents,
                activeTab = state.scheduleTab,
                onTabChange = viewModel::setScheduleTab,
                dateLabel = todayLabel,
                windowStart = state.dayWindowStart,
                windowEnd = state.dayWindowEnd,
            )
        }
    }
}


// ── Weekly Goals Section ───────────────────────────────────────────────────

@Composable
private fun WeeklyGoalsSection(
    goals: List<com.dailycurator.data.model.WeeklyGoal>,
    collapsed: Boolean,
    onToggleCollapse: () -> Unit,
    onToggleGoal: (com.dailycurator.data.model.WeeklyGoal) -> Unit,
    onAddGoal: (String) -> Unit,
    weeklyInsightEnabled: Boolean,
    cerebrasConfigured: Boolean,
    weeklyInsight: AiInsight,
    weeklyInsightExpanded: Boolean,
    onWeeklyInsightExpandedChange: (Boolean) -> Unit,
    onRegenerateWeeklyInsight: () -> Unit,
    weeklyInsightLoading: Boolean,
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

        if (weeklyInsightEnabled) {
            val insight = when {
                !cerebrasConfigured -> AiInsight(
                    insightText = "Add your Cerebras API key in Settings to get weekly goal coaching.",
                    boldPart = "Connect Cerebras",
                )
                else -> weeklyInsight
            }
            WeeklyGoalsInsightCard(
                insight = insight,
                expanded = weeklyInsightExpanded,
                onExpandedChange = onWeeklyInsightExpandedChange,
                onRegenerate = if (cerebrasConfigured) onRegenerateWeeklyInsight else null,
                isRegenerating = weeklyInsightLoading,
                showRegenerate = cerebrasConfigured,
            )
            Spacer(Modifier.height(10.dp))
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
    dateLabel: String,
    windowStart: LocalTime,
    windowEnd: LocalTime,
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
                            color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal))
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        // Timeline or Clock
        Crossfade(targetState = activeTab, label = "schedule_tab") { tab ->
            when (tab) {
                ScheduleTab.TIMELINE -> ScheduleTimelineView(
                    events = events,
                    windowStart = windowStart,
                    windowEnd = windowEnd,
                )
                ScheduleTab.CLOCK -> ClockView(
                    events = events,
                    windowStart = windowStart,
                    windowEnd = windowEnd,
                )
            }
        }
    }
}
