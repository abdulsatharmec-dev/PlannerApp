package com.dailycurator.ui.screens.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dailycurator.ui.components.DurationPresetSelector
import com.dailycurator.data.model.WeeklyGoal
import com.dailycurator.ui.components.GoalDetailBottomSheet
import com.dailycurator.ui.components.GoalTileCard
import com.dailycurator.ui.components.OutlinedPickerButton
import com.dailycurator.ui.components.PickDateDialog
import com.dailycurator.ui.components.formatDurationMinutes
import com.dailycurator.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun GoalsScreen(
    onNavigateToPomodoro: () -> Unit = {},
    viewModel: GoalsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val openDetailId by viewModel.openDetailGoalId.collectAsState()
    val linkedTasks by viewModel.goalDetailLinkedTasks.collectAsState()
    val progress = if (state.total > 0) state.completedCount.toFloat() / state.total else 0f
    var showGoalForm by remember { mutableStateOf(false) }
    var goalFormInitial by remember { mutableStateOf<WeeklyGoal?>(null) }
    var pendingDelete by remember { mutableStateOf<WeeklyGoal?>(null) }

    val detailGoal = remember(openDetailId, state.pendingGoals, state.completedGoals) {
        openDetailId?.let { id -> (state.pendingGoals + state.completedGoals).find { it.id == id } }
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
                    viewModel.addGoal(title, description, deadline, time, category, iconEmoji, 0)
                } else {
                    viewModel.updateGoal(
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
            text = { Text("Remove \"${g.title}\"? Tasks linked to this goal will have the link cleared.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteGoal(g)
                        pendingDelete = null
                    },
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
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
            onProgressChange = { pct -> viewModel.setGoalProgress(g.id, pct) },
            onToggleComplete = { viewModel.toggleGoal(g) },
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .appScreenBackground(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("WEEKLY GOALS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp))
                    Text("This Week",
                        style = MaterialTheme.typography.displayLarge.copy(
                            color = MaterialTheme.colorScheme.onBackground))
                }
                Button(
                    onClick = {
                        goalFormInitial = null
                        showGoalForm = true
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("New Goal",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.SemiBold))
                }
            }
        }
        
        // Calendar View Integration
        item {
            GoalsCalendarView(
                goals = state.pendingGoals + state.completedGoals,
                weekStart = state.weekStart
            )
            Spacer(Modifier.height(16.dp))
        }

        // Progress overview card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Progress Overview",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface))
                        Text("${state.completedCount} / ${state.total} Goals",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = AccentGreen, fontWeight = FontWeight.SemiBold))
                    }
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(ProgressTrack, RoundedCornerShape(4.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight()
                                .background(AccentGreen, RoundedCornerShape(4.dp))
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(state.total) { i ->
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(
                                        if (i < state.completedCount) AccentGreen else ProgressTrack,
                                        androidx.compose.foundation.shape.CircleShape
                                    )
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        Text("${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = AccentGreen, fontWeight = FontWeight.Bold))
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // Completed
        item {
            Text("COMPLETED (${state.completedCount})",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = AccentGreen, letterSpacing = 1.sp),
                modifier = Modifier.padding(horizontal = 20.dp))
        }
        items(state.completedGoals, key = { it.id }) { goal ->
            GoalTileCard(
                goal = goal,
                onOpenDetail = { viewModel.openGoalDetail(goal.id) },
                onToggleComplete = { viewModel.toggleGoal(goal) },
                onEdit = {
                    goalFormInitial = goal
                    showGoalForm = true
                },
                onRequestDelete = { pendingDelete = goal },
                onStartPomodoro = if (goal.id > 0L) {
                    {
                        viewModel.startPomodoroForGoal(goal)
                        onNavigateToPomodoro()
                    }
                } else {
                    null
                },
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }

        // Pending
        item {
            Spacer(Modifier.height(12.dp))
            Text("IN PROGRESS (${state.pendingGoals.size})",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp),
                modifier = Modifier.padding(horizontal = 20.dp))
        }
        items(state.pendingGoals, key = { it.id }) { goal ->
            GoalTileCard(
                goal = goal,
                onOpenDetail = { viewModel.openGoalDetail(goal.id) },
                onToggleComplete = { viewModel.toggleGoal(goal) },
                onEdit = {
                    goalFormInitial = goal
                    showGoalForm = true
                },
                onRequestDelete = { pendingDelete = goal },
                onStartPomodoro = if (goal.id > 0L) {
                    {
                        viewModel.startPomodoroForGoal(goal)
                        onNavigateToPomodoro()
                    }
                } else {
                    null
                },
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
    }
}

@Composable
fun GoalFormDialog(
    initial: WeeklyGoal?,
    onDismiss: () -> Unit,
    onSave: (String, String?, String?, String?, String, String?) -> Unit,
) {
    var title by remember(initial?.id) { mutableStateOf(initial?.title ?: "") }
    var description by remember(initial?.id) { mutableStateOf(initial?.description ?: "") }
    var iconEmoji by remember(initial?.id) { mutableStateOf(initial?.iconEmoji ?: "") }
    var deadlineDate by remember(initial?.id) {
        mutableStateOf(initial?.deadline?.let { runCatching { LocalDate.parse(it) }.getOrNull() })
    }
    var showDeadlinePicker by remember { mutableStateOf(false) }
    var includeTimeEstimate by remember(initial?.id) {
        mutableStateOf(!initial?.timeEstimate.isNullOrBlank())
    }
    var estimateMinutes by remember(initial?.id) {
        mutableStateOf(
            initial?.timeEstimate?.let { est ->
                est.filter { it.isDigit() }.toIntOrNull()?.takeIf { it > 0 } ?: 60
            } ?: 60,
        )
    }
    var category by remember(initial?.id) { mutableStateOf(initial?.category ?: "Spiritual") }
    var isError by remember { mutableStateOf(false) }
    val dateFmt = remember { DateTimeFormatter.ofPattern("EEE, MMM d") }

    PickDateDialog(
        visible = showDeadlinePicker,
        initialDate = deadlineDate ?: LocalDate.now(),
        onDismiss = { showDeadlinePicker = false },
        onConfirm = { deadlineDate = it }
    )

    val categories = listOf("Spiritual", "Health", "Finance", "Career", "Learning")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                if (initial == null) "New Goal" else "Edit Goal",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it; isError = false },
                    label = { Text("Goal title") }, isError = isError,
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text("Short description (optional)") }, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = iconEmoji,
                    onValueChange = { if (it.length <= 4) iconEmoji = it },
                    label = { Text("Icon (emoji, optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedPickerButton(
                    text = deadlineDate?.format(dateFmt) ?: "Pick deadline (optional)",
                    onClick = { showDeadlinePicker = true }
                )
                if (deadlineDate != null) {
                    TextButton(onClick = { deadlineDate = null }, modifier = Modifier.fillMaxWidth()) {
                        Text("Clear deadline")
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = includeTimeEstimate, onCheckedChange = { includeTimeEstimate = it })
                    Text("Time estimate", style = MaterialTheme.typography.bodyMedium)
                }
                if (includeTimeEstimate) {
                    DurationPresetSelector(
                        selectedMinutes = estimateMinutes,
                        onMinutesSelected = { estimateMinutes = it }
                    )
                    Text(
                        "Stored as: ${formatDurationMinutes(estimateMinutes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text("Category", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    categories.take(3).forEach { cat ->
                        FilterChip(
                            selected = category == cat, onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    categories.takeLast(2).forEach { cat ->
                        FilterChip(
                            selected = category == cat, onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (title.isBlank()) {
                    isError = true
                    return@Button
                }
                onSave(
                    title.trim(),
                    description.takeIf { it.isNotBlank() },
                    deadlineDate?.toString(),
                    formatDurationMinutes(estimateMinutes).takeIf { includeTimeEstimate },
                    category,
                    iconEmoji.trim().takeIf { it.isNotEmpty() },
                )
            }) { Text(if (initial == null) "Add" else "Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun GoalsCalendarView(
    goals: List<com.dailycurator.data.model.WeeklyGoal>,
    weekStart: LocalDate
) {
    val weekDays = remember(weekStart) { (0L..6L).map { weekStart.plusDays(it) } }
    val dayLabelFmt = remember { DateTimeFormatter.ofPattern("EEE") }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Deadlines Calendar", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weekDays.forEach { date ->
                    val hasDeadline = goals.any { g ->
                        g.deadline?.let { d ->
                            runCatching { LocalDate.parse(d) }.getOrNull()
                        } == date
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(date.format(dayLabelFmt).take(1), style = MaterialTheme.typography.labelSmall)
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(if (hasDeadline) AccentGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.background),
                            contentAlignment = Alignment.Center
                        ) {
                            if (hasDeadline) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(AccentGreen)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
