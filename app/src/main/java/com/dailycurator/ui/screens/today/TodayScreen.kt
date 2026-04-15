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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dailycurator.data.model.AiInsight
import com.dailycurator.data.model.PriorityTask
import com.dailycurator.data.model.WeeklyGoal
import com.dailycurator.data.local.HomeLayoutSection
import com.dailycurator.data.media.MorningVideoBucket
import com.dailycurator.ui.components.GoalDetailBottomSheet
import com.dailycurator.ui.components.GoalTileCard
import com.dailycurator.ui.components.*
import com.dailycurator.ui.screens.goals.GoalFormDialog
import com.dailycurator.ui.theme.*
import com.dailycurator.R
import java.time.temporal.ChronoUnit
import java.util.Collections

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

    var assistantExpanded by remember { mutableStateOf(false) }
    var weeklyInsightExpanded by remember { mutableStateOf(false) }
    var top5Expanded by remember { mutableStateOf(true) }
    var showCompletedPriorities by rememberSaveable { mutableStateOf(true) }
    var homeLayoutEditMode by rememberSaveable { mutableStateOf(false) }
    val persistedOrder = state.homeLayoutOrder
    var draftOrder by remember { mutableStateOf(persistedOrder) }
    LaunchedEffect(persistedOrder, homeLayoutEditMode) {
        if (!homeLayoutEditMode) draftOrder = persistedOrder
    }
    val orderToShow = if (homeLayoutEditMode) draftOrder else persistedOrder

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

    fun swapDraftOrder(i: Int, j: Int) {
        if (!homeLayoutEditMode) return
        if (i !in draftOrder.indices || j !in draftOrder.indices) return
        draftOrder = draftOrder.toMutableList().also { Collections.swap(it, i, j) }.toList()
    }

    val blocksToRender = remember(orderToShow, homeLayoutEditMode, state) {
        if (homeLayoutEditMode) orderToShow
        else orderToShow.filter { homeSectionVisibleInNormalMode(it, state) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .appScreenBackground(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            var menuOpen by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                    ),
                )
                Spacer(Modifier.weight(1f))
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Customize home layout") },
                            onClick = {
                                draftOrder = persistedOrder
                                homeLayoutEditMode = true
                                menuOpen = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Cancel layout editing") },
                            onClick = {
                                draftOrder = persistedOrder
                                homeLayoutEditMode = false
                                menuOpen = false
                            },
                            enabled = homeLayoutEditMode,
                        )
                    }
                }
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = "AI",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        if (homeLayoutEditMode) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Move sections with the arrows. Tap Save when finished.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            onClick = {
                                draftOrder = persistedOrder
                                homeLayoutEditMode = false
                            },
                        ) { Text("Cancel") }
                        Spacer(Modifier.width(4.dp))
                        Button(
                            onClick = {
                                viewModel.saveHomeLayoutOrder(draftOrder)
                                homeLayoutEditMode = false
                            },
                        ) { Text("Save") }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }

        items(
            count = blocksToRender.size,
            key = { blocksToRender[it].storageId },
        ) { displayIndex ->
            val section = blocksToRender[displayIndex]
            val originalIndex = orderToShow.indexOf(section)

            HomeSectionEditChrome(
                active = homeLayoutEditMode,
                label = homeSectionLabel(section),
                canMoveUp = originalIndex > 0,
                canMoveDown = originalIndex < orderToShow.lastIndex,
                onMoveUp = { swapDraftOrder(originalIndex, originalIndex - 1) },
                onMoveDown = { swapDraftOrder(originalIndex, originalIndex + 1) },
                modifier = Modifier.padding(horizontal = 20.dp),
            ) {
                when (section) {
                    HomeLayoutSection.DAY_WINDOW -> {
                        DayWindowProgressBar(
                            windowStart = state.dayWindowStart,
                            windowEnd = state.dayWindowEnd,
                            modifier = Modifier.fillMaxWidth(),
                            mustDoUndoneMinutes = mustDoUndoneMinutes,
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                    HomeLayoutSection.MORNING_MOTIVATION -> {
                        val mot = state.morningMotivationClips
                        val spir = state.morningSpiritualClips
                        val tab = state.homeMorningVideoTab
                        val activeClips = when (tab) {
                            MorningVideoBucket.SPIRITUAL -> spir
                            MorningVideoBucket.MOTIVATION -> mot
                        }
                        if (mot.isEmpty() && spir.isEmpty()) {
                            if (homeLayoutEditMode) {
                                OutlinedCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.outlinedCardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    ),
                                ) {
                                    Text(
                                        "Motivation & spiritual clips are empty. Add videos in Settings (pick Motivation or Spiritual first).",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(12.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Spacer(Modifier.height(12.dp))
                            }
                        } else {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                IconButton(
                                    onClick = {
                                        viewModel.setHomeMorningVideoTab(MorningVideoBucket.MOTIVATION)
                                    },
                                ) {
                                    Icon(
                                        Icons.Default.PlayCircle,
                                        contentDescription = "Motivation videos",
                                        tint = if (tab == MorningVideoBucket.MOTIVATION) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        viewModel.setHomeMorningVideoTab(MorningVideoBucket.SPIRITUAL)
                                    },
                                ) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = "Spiritual videos",
                                        tint = if (tab == MorningVideoBucket.SPIRITUAL) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    )
                                }
                            }
                            if (activeClips.isEmpty()) {
                                Text(
                                    if (tab == MorningVideoBucket.SPIRITUAL) {
                                        "No spiritual clips yet. Add some in Settings."
                                    } else {
                                        "No motivation clips in this bucket. Add some in Settings or switch tab."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            } else {
                                MorningMotivationCard(
                                    cardTitle = if (tab == MorningVideoBucket.SPIRITUAL) "Spiritual" else "Motivation",
                                    clips = activeClips,
                                    morningMotivationAutoplay = state.morningMotivationAutoplay,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                    HomeLayoutSection.HOME_PDF -> {
                        if (state.homeDailyPdfUri.isBlank()) {
                            if (homeLayoutEditMode) {
                                OutlinedCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.outlinedCardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    ),
                                ) {
                                    Text(
                                        "No PDF selected. Choose a document in Settings → Daily PDF on Home.",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(12.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Spacer(Modifier.height(12.dp))
                            }
                        } else {
                            HomePdfReaderCard(
                                uriString = state.homeDailyPdfUri,
                                lastReadPageIndex = state.homeDailyPdfLastPage,
                                viewMode = state.homePdfViewMode,
                                onViewModeChange = viewModel::setHomePdfViewMode,
                                themeDark = state.homePdfThemeDark,
                                onThemeDarkChange = viewModel::setHomePdfThemeDark,
                                zoomScale = state.homePdfZoomScale,
                                onZoomMultiply = viewModel::multiplyHomePdfZoom,
                                onResetZoom = { viewModel.setHomePdfZoomScale(1f) },
                                onVisiblePageIndexChanged = viewModel::persistHomePdfLastPage,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                    HomeLayoutSection.ASSISTANT_INSIGHT -> {
                        if (!state.assistantInsightEnabled && homeLayoutEditMode) {
                            OutlinedCard(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                ),
                            ) {
                                Text(
                                    "Assistant insight is off. Turn it on in Settings to show this card here.",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(Modifier.height(24.dp))
                        } else if (state.assistantInsightEnabled) {
                            val insight = when {
                                !state.cerebrasConfigured -> AiInsight(
                                    insightText = "Add an LLM API key in Settings to generate a daily assistant insight from your tasks, goals, and habits.",
                                    boldPart = "Connect LLM",
                                )
                                else -> state.assistantInsight
                            }
                            AIInsightCard(
                                insight = insight,
                                modifier = Modifier.fillMaxWidth(),
                                expanded = assistantExpanded,
                                onExpandedChange = { assistantExpanded = it },
                                onRegenerate = if (state.cerebrasConfigured) {
                                    { viewModel.regenerateAssistantInsight() }
                                } else {
                                    null
                                },
                                isRegenerating = state.assistantInsightLoading,
                                showRegenerate = state.cerebrasConfigured,
                            )
                            Spacer(Modifier.height(24.dp))
                        }
                    }
                    HomeLayoutSection.TOP5_PRIORITIES -> {
                        TopPrioritiesSection(
                            top5Expanded = top5Expanded,
                            onTop5ExpandedChange = { top5Expanded = it },
                            showCompletedPriorities = showCompletedPriorities,
                            onShowCompletedPrioritiesChange = { showCompletedPriorities = it },
                            topPriorityTasks = topPriorityTasks,
                            orderedTasksForNumbers = orderedTasksForNumbers,
                            taskTagColors = state.taskTagColors,
                            onToggleDone = { viewModel.toggleTaskDone(it) },
                            onStartPomodoroForTask = { task ->
                                viewModel.startPomodoroForTask(task)
                                onNavigateToPomodoro()
                            },
                        )
                    }
                    HomeLayoutSection.WEEKLY_GOALS -> {
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
                    HomeLayoutSection.GMAIL_DIGEST -> {
                        if (!state.homeGmailSummaryEnabled && homeLayoutEditMode) {
                            OutlinedCard(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                ),
                            ) {
                                Text(
                                    "Gmail digest is off. Enable “Show Gmail digest on Home” in Settings.",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(Modifier.height(24.dp))
                        } else if (state.homeGmailSummaryEnabled) {
                            Spacer(Modifier.height(16.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                ),
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
        }
    }
}

private fun homeSectionVisibleInNormalMode(section: HomeLayoutSection, state: TodayUiState): Boolean =
    when (section) {
        HomeLayoutSection.DAY_WINDOW -> true
        HomeLayoutSection.MORNING_MOTIVATION ->
            state.morningMotivationClips.isNotEmpty() || state.morningSpiritualClips.isNotEmpty()
        HomeLayoutSection.HOME_PDF -> state.homeDailyPdfUri.isNotBlank()
        HomeLayoutSection.ASSISTANT_INSIGHT -> state.assistantInsightEnabled
        HomeLayoutSection.TOP5_PRIORITIES -> true
        HomeLayoutSection.WEEKLY_GOALS -> true
        HomeLayoutSection.GMAIL_DIGEST -> state.homeGmailSummaryEnabled
    }

private fun homeSectionLabel(section: HomeLayoutSection): String =
    when (section) {
        HomeLayoutSection.DAY_WINDOW -> "Day progress"
        HomeLayoutSection.MORNING_MOTIVATION -> "Motivation"
        HomeLayoutSection.HOME_PDF -> "Daily PDF"
        HomeLayoutSection.ASSISTANT_INSIGHT -> "Assistant insight"
        HomeLayoutSection.TOP5_PRIORITIES -> "Top 5"
        HomeLayoutSection.WEEKLY_GOALS -> "Weekly goals"
        HomeLayoutSection.GMAIL_DIGEST -> "Gmail digest"
    }

@Composable
private fun HomeSectionEditChrome(
    active: Boolean,
    label: String,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier) {
        if (active) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move section up")
                }
                IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move section down")
                }
            }
        }
        content()
    }
}

@Composable
private fun TopPrioritiesSection(
    top5Expanded: Boolean,
    onTop5ExpandedChange: (Boolean) -> Unit,
    showCompletedPriorities: Boolean,
    onShowCompletedPrioritiesChange: (Boolean) -> Unit,
    topPriorityTasks: List<PriorityTask>,
    orderedTasksForNumbers: List<PriorityTask>,
    taskTagColors: Map<String, Int>,
    onToggleDone: (PriorityTask) -> Unit,
    onStartPomodoroForTask: (PriorityTask) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onTop5ExpandedChange(!top5Expanded) }
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
                    imageVector = if (top5Expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (top5Expanded) "Collapse priorities" else "Expand priorities",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp),
                )
            }
            FilterChip(
                selected = showCompletedPriorities,
                onClick = { onShowCompletedPrioritiesChange(!showCompletedPriorities) },
                label = { Text("Show done") },
            )
        }
        Spacer(Modifier.height(10.dp))
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
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                } else {
                    topPriorityTasks.forEach { task ->
                        PriorityItem(
                            task = task,
                            listNumber = resolvedTaskListNumber(task, orderedTasksForNumbers),
                            onToggleDone = { onToggleDone(task) },
                            taskTagColors = taskTagColors,
                            onStartPomodoro = if (task.id > 0L) {
                                { onStartPomodoroForTask(task) }
                            } else {
                                null
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
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
