package com.dailycurator.ui.screens.today

import androidx.compose.animation.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.dailycurator.data.model.WeeklyGoal
import com.dailycurator.ui.components.GoalDetailBottomSheet
import com.dailycurator.ui.components.GoalTileCard
import com.dailycurator.ui.components.PickDateDialog
import com.dailycurator.ui.components.*
import com.dailycurator.ui.screens.goals.GoalFormDialog
import com.dailycurator.ui.theme.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    onNavigateToPomodoro: () -> Unit = {},
    onOpenGmailMailboxSummary: () -> Unit = {},
    viewModel: TodayViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val openDetailId by viewModel.openDetailGoalId.collectAsState()
    val linkedTasks by viewModel.goalDetailLinkedTasks.collectAsState()
    var showGoalForm by remember { mutableStateOf(false) }
    var goalFormInitial by remember { mutableStateOf<WeeklyGoal?>(null) }
    var pendingDelete by remember { mutableStateOf<WeeklyGoal?>(null) }

    val detailGoal = remember(openDetailId, state.goals) {
        openDetailId?.let { id -> state.goals.find { it.id == id } }
    }

    if (showGoalForm) {
        GoalFormDialog(
            initial = goalFormInitial,
            onDismiss = {
                showGoalForm = false
                goalFormInitial = null
            },
            onSave = { title, description, deadline, time, category, iconEmoji ->
                val existing = goalFormInitial
                if (existing == null) {
                    viewModel.addGoalFull(title, description, deadline, time, category, iconEmoji, 0)
                } else {
                    viewModel.updateWeeklyGoal(
                        existing.copy(
                            title = title,
                            description = description,
                            deadline = deadline,
                            timeEstimate = time,
                            category = category,
                            iconEmoji = iconEmoji?.trim()?.takeIf { it.isNotEmpty() },
                        ),
                    )
                }
                showGoalForm = false
                goalFormInitial = null
            },
        )
    }

    pendingDelete?.let { g ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete goal?") },
            text = { Text("Remove \"${g.title}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteWeeklyGoal(g)
                        pendingDelete = null
                    },
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
        )
    }

    detailGoal?.let { g ->
        GoalDetailBottomSheet(
            goal = g,
            linkedTasks = linkedTasks,
            onDismiss = { viewModel.dismissGoalDetail() },
            onEdit = {
                viewModel.dismissGoalDetail()
                goalFormInitial = g
                showGoalForm = true
            },
            onRequestDelete = {
                viewModel.dismissGoalDetail()
                pendingDelete = g
            },
            onProgressChange = { pct -> viewModel.setWeeklyGoalProgress(g.id, pct) },
            onToggleComplete = { viewModel.toggleGoal(g) },
        )
    }

    var showScheduleDatePicker by remember { mutableStateOf(false) }
    val scheduleDateHeaderFmt = remember { DateTimeFormatter.ofPattern("EEE, MMM d") }
    val scheduleDateLabel = state.scheduleTimelineDate.format(scheduleDateHeaderFmt)
    val scheduleShowsToday = state.scheduleTimelineDate == LocalDate.now()

    PickDateDialog(
        visible = showScheduleDatePicker,
        initialDate = state.scheduleTimelineDate,
        onDismiss = { showScheduleDatePicker = false },
        onConfirm = {
            viewModel.setScheduleTimelineDate(it)
            showScheduleDatePicker = false
        },
    )
    var assistantExpanded by remember { mutableStateOf(true) }
    var weeklyInsightExpanded by remember { mutableStateOf(true) }
    var top5Expanded by remember { mutableStateOf(true) }
    var showCompletedPriorities by rememberSaveable { mutableStateOf(true) }

    val orderedTasksForNumbers = remember(state.tasks) { tasksSortedForListNumber(state.tasks) }

    val topPriorityTasks = remember(state.tasks, showCompletedPriorities) {
        val visible = if (showCompletedPriorities) state.tasks else state.tasks.filter { !it.isDone }
        visible
            .filter { it.isTopFive }
            .sortedWith(compareBy<PriorityTask> { it.rank }.thenBy { it.startTime }.thenBy { it.id })
    }

    val mustDoUndoneMinutes = remember(state.tasks) {
        state.tasks
            .filter { it.isMustDo && !it.isDone }
            .sumOf { ChronoUnit.MINUTES.between(it.startTime, it.endTime).toInt().coerceAtLeast(0) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .appScreenBackground(),
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
                mustDoUndoneMinutes = mustDoUndoneMinutes,
            )
            Spacer(Modifier.height(16.dp))
        }

        // ── Assistant insight (optional)
        item {
            if (state.assistantInsightEnabled) {
                val insight = when {
                    !state.cerebrasConfigured -> AiInsight(
                        insightText = "Add an LLM API key in Settings to generate a daily assistant insight from your tasks, goals, and habits.",
                        boldPart = "Connect LLM",
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

        // ── Top 5 Priorities (collapsible; ranks 1–5; optional hide completed)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { top5Expanded = !top5Expanded }
                        .padding(top = 6.dp, bottom = 6.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Top 5 Priorities",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = MaterialTheme.colorScheme.onBackground,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = if (top5Expanded) {
                            Icons.Default.ExpandLess
                        } else {
                            Icons.Default.ExpandMore
                        },
                        contentDescription = if (top5Expanded) "Collapse priorities" else "Expand priorities",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp),
                    )
                }
                FilterChip(
                    selected = showCompletedPriorities,
                    onClick = { showCompletedPriorities = !showCompletedPriorities },
                    label = { Text("Show done") },
                )
            }
            Spacer(Modifier.height(10.dp))
        }

        item {
            AnimatedVisibility(
                visible = top5Expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column {
                    if (topPriorityTasks.isEmpty()) {
                        Text(
                            "No tasks marked as Top 5. Open Tasks and edit a task to include it here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                    } else {
                        topPriorityTasks.forEach { task ->
                            PriorityItem(
                                task = task,
                                listNumber = resolvedTaskListNumber(task, orderedTasksForNumbers),
                                onToggleDone = { viewModel.toggleTaskDone(task) },
                                onStartPomodoro = if (task.id > 0L) {
                                    {
                                        viewModel.startPomodoroForTask(task)
                                        onNavigateToPomodoro()
                                    }
                                } else {
                                    null
                                },
                                modifier = Modifier.padding(horizontal = 20.dp),
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }

        // ── Weekly Goals section
        item {
            WeeklyGoalsSection(
                goals = state.goals,
                collapsed = state.goalsCollapsed,
                onToggleCollapse = viewModel::toggleGoalsCollapsed,
                onToggleGoal = viewModel::toggleGoal,
                onAddGoal = viewModel::addGoal,
                onOpenGoalDetail = viewModel::openGoalDetail,
                onEditGoal = {
                    goalFormInitial = it
                    showGoalForm = true
                },
                onRequestDeleteGoal = { pendingDelete = it },
                onStartPomodoroForGoal = {
                    viewModel.startPomodoroForGoal(it)
                    onNavigateToPomodoro()
                },
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
                scheduleDate = state.scheduleTimelineDate,
                scheduleDateLabel = scheduleDateLabel,
                onScheduleDateClick = { showScheduleDatePicker = true },
                showNowIndicator = scheduleShowsToday,
                windowStart = state.dayWindowStart,
                windowEnd = state.dayWindowEnd,
            )
        }

        item {
            if (state.homeGmailSummaryEnabled) {
                Spacer(Modifier.height(16.dp))
                Card(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Gmail digest",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        )
                        Spacer(Modifier.height(8.dp))
                        when {
                            !state.cerebrasConfigured -> Text(
                                "Add an LLM API key in Settings to generate mailbox summaries.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            state.gmailHomeDigestMarkdown.isBlank() -> Text(
                                "Open Gmail Mailbox Summary from the menu and tap Generate to refresh this card.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            else -> MarkdownSummaryBody(markdown = state.gmailHomeDigestMarkdown)
                        }
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = onOpenGmailMailboxSummary) {
                            Text("Open full mailbox summary")
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}


// ── Weekly Goals Section ───────────────────────────────────────────────────

@Composable
private fun WeeklyGoalsSection(
    goals: List<WeeklyGoal>,
    collapsed: Boolean,
    onToggleCollapse: () -> Unit,
    onToggleGoal: (WeeklyGoal) -> Unit,
    onAddGoal: (String) -> Unit,
    onOpenGoalDetail: (Long) -> Unit,
    onEditGoal: (WeeklyGoal) -> Unit,
    onRequestDeleteGoal: (WeeklyGoal) -> Unit,
    onStartPomodoroForGoal: (WeeklyGoal) -> Unit,
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
                    insightText = "Add an LLM API key in Settings to get weekly goal coaching.",
                    boldPart = "Connect LLM",
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
                    GoalTileCard(
                        goal = goal,
                        onOpenDetail = { onOpenGoalDetail(goal.id) },
                        onToggleComplete = { onToggleGoal(goal) },
                        onEdit = { onEditGoal(goal) },
                        onRequestDelete = { onRequestDeleteGoal(goal) },
                        onStartPomodoro = if (goal.id > 0L) {
                            { onStartPomodoroForGoal(goal) }
                        } else {
                            null
                        },
                    )
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
    scheduleDate: LocalDate,
    scheduleDateLabel: String,
    onScheduleDateClick: () -> Unit,
    showNowIndicator: Boolean,
    windowStart: LocalTime,
    windowEnd: LocalTime,
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text("SCHEDULE",
            style = MaterialTheme.typography.labelSmall.copy(
                color = AccentGreen, fontWeight = FontWeight.Bold, letterSpacing = 1.sp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Your day",
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground,
                ),
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = onScheduleDateClick,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = "Pick timeline date: $scheduleDateLabel",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Text(
            scheduleDateLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Spacer(Modifier.height(4.dp))

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
                    showNowIndicator = showNowIndicator,
                    scheduleDate = scheduleDate,
                )
                ScheduleTab.CLOCK -> ClockView(
                    events = events,
                    windowStart = windowStart,
                    windowEnd = windowEnd,
                    useLiveNowIndicator = showNowIndicator,
                    scheduleDayLabel = scheduleDateLabel,
                )
            }
        }
    }
}
