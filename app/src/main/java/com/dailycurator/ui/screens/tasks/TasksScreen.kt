package com.dailycurator.ui.screens.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dailycurator.data.model.PriorityTask
import com.dailycurator.data.model.TaskRepeatOption
import com.dailycurator.data.model.Urgency
import com.dailycurator.data.model.WeeklyGoal
import com.dailycurator.ui.components.DurationPresetSelector
import com.dailycurator.ui.components.TaskTagChip
import com.dailycurator.ui.components.TaskTagUi
import com.dailycurator.ui.components.PickDateDialog
import com.dailycurator.ui.components.PickTimeDialog
import com.dailycurator.ui.components.PriorityItem
import com.dailycurator.ui.components.resolvedTaskListNumber
import com.dailycurator.ui.components.tasksSortedForListNumber
import com.dailycurator.ui.components.formatDurationMinutes
import com.dailycurator.ui.screens.schedule.ScheduleDateStrip
import com.dailycurator.ui.theme.AccentGreen
import com.dailycurator.ui.theme.AccentRed
import com.dailycurator.ui.theme.appScaffoldContainerColor
import com.dailycurator.ui.theme.appScreenBackground
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private sealed class TaskDeletePrompt {
    data class Ordinary(val task: PriorityTask) : TaskDeletePrompt()
    data class Repeating(val task: PriorityTask) : TaskDeletePrompt()
}

@Composable
fun TasksScreen(
    onNavigateToPomodoro: () -> Unit = {},
    onOpenDrawer: () -> Unit = {},
    onNavigateSettings: () -> Unit = {},
    viewModel: TasksViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val orderedDayTasks = remember(state.tasks) { tasksSortedForListNumber(state.tasks) }
    var showAddDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<PriorityTask?>(null) }
    var showJumpDatePicker by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var taskDeletePrompt by remember { mutableStateOf<TaskDeletePrompt?>(null) }

    when (val prompt = taskDeletePrompt) {
        is TaskDeletePrompt.Ordinary -> {
            val task = prompt.task
            AlertDialog(
                onDismissRequest = { taskDeletePrompt = null },
                title = { Text("Delete task?") },
                text = { Text("Remove \"${task.title}\"?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteTask(task, TaskDeleteScope.THIS_DAY_ONLY)
                            taskDeletePrompt = null
                        },
                    ) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { taskDeletePrompt = null }) { Text("Cancel") }
                },
            )
        }
        is TaskDeletePrompt.Repeating -> {
            val task = prompt.task
            AlertDialog(
                onDismissRequest = { taskDeletePrompt = null },
                title = { Text("Delete repeating task?") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "This task appears on more than one day. Choose what to remove for \"${task.title}\".",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        OutlinedButton(
                            onClick = {
                                viewModel.deleteTask(task, TaskDeleteScope.THIS_DAY_ONLY)
                                taskDeletePrompt = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Only this day") }
                        OutlinedButton(
                            onClick = {
                                viewModel.deleteTask(task, TaskDeleteScope.THIS_AND_FUTURE_DAYS)
                                taskDeletePrompt = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("This day and future days") }
                        OutlinedButton(
                            onClick = {
                                viewModel.deleteTask(task, TaskDeleteScope.ALL_SERIES_DAYS)
                                taskDeletePrompt = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("All days (entire series)") }
                    }
                },
                confirmButton = {
                    Spacer(Modifier.height(0.dp))
                },
                dismissButton = {
                    TextButton(onClick = { taskDeletePrompt = null }) { Text("Cancel") }
                },
            )
        }
        null -> Unit
    }

    var taskMenuExpanded by remember { mutableStateOf(false) }

    PickDateDialog(
        visible = showJumpDatePicker,
        initialDate = state.listDate,
        onDismiss = { showJumpDatePicker = false },
        onConfirm = {
            viewModel.setListDate(it)
            showJumpDatePicker = false
        },
    )

    if (showAddDialog || taskToEdit != null) {
        ManageTaskDialog(
            task = taskToEdit,
            defaultListDate = state.listDate,
            activeGoals = state.activeGoals,
            taskTagColors = state.taskTagColors,
            repeatDefaultUntil = { anchor, repeat, customDays ->
                viewModel.suggestedRepeatUntil(anchor, repeat, customDays)
            },
            onDismiss = {
                showAddDialog = false
                taskToEdit = null
            },
            onEnsureDefaultTagColor = { viewModel.ensureDefaultTaskTagColor(it) },
            onTaskTagColorPicked = { name, argb -> viewModel.setTaskTagColor(name, argb) },
            onMarkWontDo = if (taskToEdit != null && taskToEdit!!.id > 0L) {
                {
                    viewModel.markTaskWontDo(taskToEdit!!)
                    showAddDialog = false
                    taskToEdit = null
                }
            } else {
                null
            },
            onClearWontDo = if (taskToEdit != null && taskToEdit!!.id > 0L) {
                {
                    viewModel.clearTaskWontDo(taskToEdit!!)
                    showAddDialog = false
                    taskToEdit = null
                }
            } else {
                null
            },
            onConfirm = { title, date, start, end, urgency, isTop5, isMustDo, isCantComplete, displayNumber, note, goalId, repeat, customRepeatDays, repeatUntil, tags ->
                if (taskToEdit == null) {
                    viewModel.addTask(
                        title, date, start, end, urgency, isTop5, isMustDo, isCantComplete, displayNumber, note, goalId,
                        repeat, customRepeatDays, repeatUntil, tags,
                    )
                } else {
                    viewModel.updateTask(
                        taskToEdit!!,
                        title,
                        date,
                        start,
                        end,
                        urgency,
                        isTop5,
                        isMustDo,
                        isCantComplete,
                        displayNumber,
                        note,
                        goalId,
                        repeat,
                        customRepeatDays,
                        repeatUntil,
                        tags,
                    )
                }
                showAddDialog = false
                taskToEdit = null
            },
            onStartPomodoro = if (taskToEdit != null && taskToEdit!!.id > 0L) {
                {
                    viewModel.startPomodoroForTask(taskToEdit!!)
                    showAddDialog = false
                    taskToEdit = null
                    onNavigateToPomodoro()
                }
            } else {
                null
            },
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        },
        containerColor = appScaffoldContainerColor(),
    ) { padding ->
        // Match Schedule: parent NavHost already applies top safe inset when Tasks has no app bar;
        // avoid statusBarsPadding here (it doubled the inset and pushed the bar down).
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .appScreenBackground()
                .padding(bottom = 4.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .sizeIn(minHeight = 48.dp)
                    .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 0.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onOpenDrawer) {
                    Icon(
                        Icons.Default.Menu,
                        contentDescription = "Open menu",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    "Tasks",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                TasksViewModeToggle(
                    mode = state.displayMode,
                    onSelect = { viewModel.setDisplayMode(it) },
                )
                IconButton(onClick = { showJumpDatePicker = true }) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = "Pick date",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box {
                    IconButton(onClick = { taskMenuExpanded = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    DropdownMenu(
                        expanded = taskMenuExpanded,
                        onDismissRequest = { taskMenuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Show completed") },
                            onClick = {
                                viewModel.setShowCompletedTasks(!state.showCompletedTasks)
                            },
                            trailingIcon = {
                                Switch(
                                    checked = state.showCompletedTasks,
                                    onCheckedChange = null,
                                    enabled = false,
                                )
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Show must-do") },
                            onClick = {
                                viewModel.setShowMustDoTasks(!state.showMustDoTasks)
                            },
                            trailingIcon = {
                                Switch(
                                    checked = state.showMustDoTasks,
                                    onCheckedChange = null,
                                    enabled = false,
                                )
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Show won't do") },
                            onClick = {
                                viewModel.setShowWontDoTasks(!state.showWontDoTasks)
                            },
                            trailingIcon = {
                                Switch(
                                    checked = state.showWontDoTasks,
                                    onCheckedChange = null,
                                    enabled = false,
                                )
                            },
                        )
                    }
                }
                IconButton(onClick = onNavigateSettings) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            ScheduleDateStrip(
                selected = state.listDate,
                onSelect = { viewModel.setListDate(it) },
                onRequestFullPicker = { showJumpDatePicker = true },
                contentPaddingVertical = 0.dp,
                dayCellVerticalPadding = 6.dp,
            )
            Spacer(Modifier.height(6.dp))
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
            when (state.displayMode) {
                TasksDisplayMode.WEEK_CALENDAR -> {
                    if (state.tasks.isEmpty()) {
                        item {
                            EmptyTasksHint(
                                allTasksDoneHidden = state.allTasksDoneHidden,
                                allMustDoHidden = state.allMustDoHidden,
                                allWontDoHidden = state.allWontDoHidden,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                            )
                        }
                    } else {
                        items(state.tasks, key = { it.id }) { task ->
                            TaskRow(
                                task = task,
                                listNumber = resolvedTaskListNumber(task, orderedDayTasks),
                                viewModel = viewModel,
                                taskTagColors = state.taskTagColors,
                                onEdit = { taskToEdit = task },
                                onRequestDelete = {
                                    coroutineScope.launch {
                                        taskDeletePrompt =
                                            if (viewModel.shouldOfferRepeatDeleteOptions(task)) {
                                                TaskDeletePrompt.Repeating(task)
                                            } else {
                                                TaskDeletePrompt.Ordinary(task)
                                            }
                                    }
                                },
                                onStartPomodoro = {
                                    if (task.id > 0L) {
                                        viewModel.startPomodoroForTask(task)
                                        onNavigateToPomodoro()
                                    }
                                },
                                horizontalPadding = 20.dp,
                            )
                        }
                    }
                }
                TasksDisplayMode.PRIORITY_GROUPS -> {
                    if (state.prioritySections.isEmpty()) {
                        item {
                            EmptyTasksHint(
                                allTasksDoneHidden = state.allTasksDoneHidden,
                                allMustDoHidden = state.allMustDoHidden,
                                allWontDoHidden = state.allWontDoHidden,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                            )
                        }
                    } else {
                        state.prioritySections.forEach { (label, tasks) ->
                            item(key = label) {
                                Text(
                                    label.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        letterSpacing = 1.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    ),
                                    modifier = Modifier.padding(start = 20.dp, top = 8.dp, end = 20.dp),
                                )
                            }
                            items(tasks, key = { it.id }) { task ->
                                TaskRow(
                                    task = task,
                                    listNumber = resolvedTaskListNumber(task, orderedDayTasks),
                                    viewModel = viewModel,
                                    taskTagColors = state.taskTagColors,
                                    onEdit = { taskToEdit = task },
                                    onRequestDelete = {
                                    coroutineScope.launch {
                                        taskDeletePrompt =
                                            if (viewModel.shouldOfferRepeatDeleteOptions(task)) {
                                                TaskDeletePrompt.Repeating(task)
                                            } else {
                                                TaskDeletePrompt.Ordinary(task)
                                            }
                                    }
                                },
                                    onStartPomodoro = {
                                        if (task.id > 0L) {
                                            viewModel.startPomodoroForTask(task)
                                            onNavigateToPomodoro()
                                        }
                                    },
                                    horizontalPadding = 20.dp,
                                )
                            }
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun TasksViewModeToggle(
    mode: TasksDisplayMode,
    onSelect: (TasksDisplayMode) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val weekSelected = mode == TasksDisplayMode.WEEK_CALENDAR
        val priSelected = mode == TasksDisplayMode.PRIORITY_GROUPS
        FilledTonalIconButton(
            onClick = { onSelect(TasksDisplayMode.WEEK_CALENDAR) },
            modifier = Modifier.size(40.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = if (weekSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                },
                contentColor = if (weekSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            ),
        ) {
            Icon(Icons.Default.ViewWeek, contentDescription = "Week and day list")
        }
        FilledTonalIconButton(
            onClick = { onSelect(TasksDisplayMode.PRIORITY_GROUPS) },
            modifier = Modifier.size(40.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = if (priSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                },
                contentColor = if (priSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            ),
        ) {
            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Priority groups")
        }
    }
}

@Composable
private fun EmptyTasksHint(
    allTasksDoneHidden: Boolean,
    allMustDoHidden: Boolean,
    allWontDoHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    Text(
        when {
            allTasksDoneHidden && allMustDoHidden && allWontDoHidden ->
                "Nothing to show with current filters. Open the ⋮ menu to adjust Show completed, Show must-do, and Show won't do."
            allTasksDoneHidden && allMustDoHidden ->
                "Nothing to show with current filters. Open the ⋮ menu to turn on Show completed and Show must-do."
            allTasksDoneHidden && allWontDoHidden ->
                "Nothing to show with current filters. Open the ⋮ menu to turn on Show completed and Show won't do."
            allMustDoHidden && allWontDoHidden ->
                "Nothing to show with current filters. Open the ⋮ menu to turn on Show must-do and Show won't do."
            allWontDoHidden ->
                "Only “won’t do” tasks are left for this day. Open the ⋮ menu and turn on Show won't do to see them."
            allTasksDoneHidden ->
                "Everything here is done. Open the ⋮ menu and turn on Show completed to see checked-off tasks."
            allMustDoHidden ->
                "Only must-do tasks are scheduled today. Open the ⋮ menu and turn on Show must-do to see them."
            else ->
                "No tasks for this day. Tap + to add one."
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Composable
private fun TaskRow(
    task: PriorityTask,
    listNumber: Int,
    viewModel: TasksViewModel,
    taskTagColors: Map<String, Int>,
    onEdit: () -> Unit,
    onRequestDelete: () -> Unit,
    onStartPomodoro: () -> Unit,
    horizontalPadding: androidx.compose.ui.unit.Dp,
) {
    var menuExpanded by remember(task.id) { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PriorityItem(
            task = task,
            listNumber = listNumber,
            onToggleDone = { viewModel.toggleTaskDone(task) },
            taskTagColors = taskTagColors,
            onStartPomodoro = if (task.id > 0L) onStartPomodoro else null,
            modifier = Modifier
                .weight(1f)
                .clickable { onEdit() },
        )
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Task options")
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = {
                        menuExpanded = false
                        onEdit()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = AccentRed) },
                    onClick = {
                        menuExpanded = false
                        onRequestDelete()
                    },
                )
            }
        }
    }
    Spacer(Modifier.height(2.dp))
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ManageTaskDialog(
    task: PriorityTask?,
    defaultListDate: LocalDate,
    activeGoals: List<WeeklyGoal>,
    taskTagColors: Map<String, Int>,
    repeatDefaultUntil: (LocalDate, TaskRepeatOption, Int) -> LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (String, LocalDate, LocalTime, LocalTime, Urgency, Boolean, Boolean, Boolean, Int, String?, Long?, TaskRepeatOption, Int, LocalDate?, List<String>) -> Unit,
    onEnsureDefaultTagColor: (String) -> Unit,
    onTaskTagColorPicked: (String, Int) -> Unit,
    onMarkWontDo: (() -> Unit)? = null,
    onClearWontDo: (() -> Unit)? = null,
    onStartPomodoro: (() -> Unit)? = null,
) {
    var title by remember(task?.id) { mutableStateOf(task?.title ?: "") }
    var note by remember(task?.id) { mutableStateOf(task?.statusNote ?: "") }
    val dateFmt = DateTimeFormatter.ofPattern("EEE, MMM d")
    var selectedDate by remember(task?.id, defaultListDate) {
        mutableStateOf(task?.date ?: defaultListDate)
    }
    var startTime by remember(task?.id) {
        mutableStateOf(task?.startTime ?: LocalTime.of(9, 0))
    }
    var durationMinutes by remember(task?.id) {
        val init = if (task == null) {
            60
        } else {
            val m = ChronoUnit.MINUTES.between(task.startTime, task.endTime).toInt()
            if (m <= 0) 60 else m
        }
        mutableStateOf(init)
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var urgency by remember(task?.id) { mutableStateOf(task?.urgency ?: Urgency.GREEN) }
    var isTop5 by remember(task?.id) { mutableStateOf(task?.isTopFive == true) }
    var isMustDo by remember(task?.id) { mutableStateOf(task?.isMustDo == true) }
    var isCantComplete by remember(task?.id) { mutableStateOf(task?.isCantComplete == true) }
    var tags by remember(task?.id) { mutableStateOf(task?.tags ?: emptyList()) }
    var newTagText by remember { mutableStateOf("") }
    val suggestedTags = remember { listOf("Work", "Personal", "Prayer") }
    var listNumberText by remember(task?.id) {
        mutableStateOf(
            if (task != null && task.displayNumber > 0) task.displayNumber.toString() else "",
        )
    }
    var linkedGoalId by remember(task?.id) { mutableStateOf<Long?>(task?.goalId) }
    var goalMenuExpanded by remember { mutableStateOf(false) }
    var repeatMenuExpanded by remember { mutableStateOf(false) }
    var repeatOption by remember(task?.id, defaultListDate) {
        mutableStateOf(task?.repeatOption ?: TaskRepeatOption.NONE)
    }
    var customRepeatDaysText by remember(task?.id, defaultListDate) {
        mutableStateOf((task?.customRepeatIntervalDays ?: 3).toString())
    }
    var repeatUntilDate by remember(task?.id, defaultListDate) {
        mutableStateOf(task?.repeatUntilDate)
    }
    var showRepeatUntilPicker by remember { mutableStateOf(false) }
    fun customIntervalDaysParsed(): Int =
        customRepeatDaysText.toIntOrNull()?.coerceIn(2, 30) ?: 3
    val linkedGoalLabel = linkedGoalId?.let { id -> activeGoals.find { it.id == id }?.title } ?: "None"
    var titleError by remember { mutableStateOf(false) }
    var dialogMenuExpanded by remember { mutableStateOf(false) }
    var showNewTagColorPicker by remember { mutableStateOf(false) }
    var pendingNewTagLabel by remember { mutableStateOf("") }

    val dialogScroll = rememberScrollState()
    val dialogMaxHeight = (LocalConfiguration.current.screenHeightDp.dp * 0.58f)

    val endTime = remember(startTime, durationMinutes) {
        startTime.plusMinutes(durationMinutes.toLong())
    }

    PickDateDialog(
        visible = showDatePicker,
        initialDate = selectedDate,
        onDismiss = { showDatePicker = false },
        onConfirm = { selectedDate = it },
    )
    PickTimeDialog(
        visible = showTimePicker,
        initialTime = startTime,
        title = "Start time",
        onDismiss = { showTimePicker = false },
        onConfirm = { startTime = it },
    )
    PickDateDialog(
        visible = showRepeatUntilPicker,
        initialDate = repeatUntilDate
            ?: repeatDefaultUntil(selectedDate, repeatOption, customIntervalDaysParsed()),
        onDismiss = { showRepeatUntilPicker = false },
        onConfirm = {
            repeatUntilDate = maxOf(it, selectedDate)
            showRepeatUntilPicker = false
        },
    )

    if (showNewTagColorPicker) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                showNewTagColorPicker = false
                pendingNewTagLabel = ""
            },
            title = { Text("Choose color for \"$pendingNewTagLabel\"") },
            text = {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TaskTagUi.presetArgbSwatches.forEach { argb ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(TaskTagUi.composeColorFromArgb(argb))
                                .clickable {
                                    onTaskTagColorPicked(pendingNewTagLabel, argb)
                                    tags = tags + pendingNewTagLabel
                                    newTagText = ""
                                    showNewTagColorPicker = false
                                    pendingNewTagLabel = ""
                                },
                        )
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        showNewTagColorPicker = false
                        pendingNewTagLabel = ""
                    },
                ) { Text("Cancel") }
            },
        )
    }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    if (task == null) "New Task" else "Edit Task",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                if (task != null && task.id > 0L) {
                    Box {
                        IconButton(onClick = { dialogMenuExpanded = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        DropdownMenu(
                            expanded = dialogMenuExpanded,
                            onDismissRequest = { dialogMenuExpanded = false },
                        ) {
                            if (!isCantComplete) {
                                DropdownMenuItem(
                                    text = { Text("Mark as won't do") },
                                    onClick = {
                                        dialogMenuExpanded = false
                                        onMarkWontDo?.invoke()
                                    },
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text("Clear won't do") },
                                    onClick = {
                                        dialogMenuExpanded = false
                                        onClearWontDo?.invoke()
                                    },
                                )
                            }
                        }
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = dialogMaxHeight)
                    .verticalScroll(dialogScroll),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                androidx.compose.material3.OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; titleError = false },
                    label = { androidx.compose.material3.Text("Task title") },
                    isError = titleError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                androidx.compose.material3.OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { androidx.compose.material3.Text("Notes / Description") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = "Pick date",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(
                        selectedDate.format(dateFmt),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showDatePicker = true },
                    )
                    IconButton(onClick = { showTimePicker = true }) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = "Pick start time",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(
                        startTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.clickable { showTimePicker = true },
                    )
                }
                DurationPresetSelector(
                    selectedMinutes = durationMinutes,
                    onMinutesSelected = { durationMinutes = it },
                    useCompactTaskRow = true,
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        "Ends at ${endTime.format(DateTimeFormatter.ofPattern("HH:mm"))} (${formatDurationMinutes(durationMinutes)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                    tags.forEach { tg ->
                        TaskTagChip(
                            label = tg,
                            backgroundArgb = TaskTagUi.argbForTag(tg, taskTagColors),
                        )
                    }
                }
                val repeatLabel = when (repeatOption) {
                    TaskRepeatOption.NONE -> "Does not repeat"
                    TaskRepeatOption.DAILY -> "Daily (14 days)"
                    TaskRepeatOption.WEEKLY -> "Weekly (4 weeks)"
                    TaskRepeatOption.CUSTOM -> "Custom interval"
                }
                ExposedDropdownMenuBox(
                    expanded = repeatMenuExpanded,
                    onExpandedChange = { repeatMenuExpanded = it },
                ) {
                    androidx.compose.material3.OutlinedTextField(
                        value = repeatLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { androidx.compose.material3.Text("Repeat") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.EventRepeat,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = repeatMenuExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = repeatMenuExpanded,
                        onDismissRequest = { repeatMenuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { androidx.compose.material3.Text("Does not repeat") },
                            onClick = {
                                repeatOption = TaskRepeatOption.NONE
                                repeatUntilDate = null
                                repeatMenuExpanded = false
                            },
                        )
                        DropdownMenuItem(
                            text = { androidx.compose.material3.Text("Daily") },
                            onClick = {
                                repeatOption = TaskRepeatOption.DAILY
                                repeatUntilDate = repeatDefaultUntil(
                                    selectedDate,
                                    TaskRepeatOption.DAILY,
                                    customIntervalDaysParsed(),
                                )
                                repeatMenuExpanded = false
                            },
                        )
                        DropdownMenuItem(
                            text = { androidx.compose.material3.Text("Weekly") },
                            onClick = {
                                repeatOption = TaskRepeatOption.WEEKLY
                                repeatUntilDate = repeatDefaultUntil(
                                    selectedDate,
                                    TaskRepeatOption.WEEKLY,
                                    customIntervalDaysParsed(),
                                )
                                repeatMenuExpanded = false
                            },
                        )
                        DropdownMenuItem(
                            text = { androidx.compose.material3.Text("Custom") },
                            onClick = {
                                repeatOption = TaskRepeatOption.CUSTOM
                                repeatUntilDate = repeatDefaultUntil(
                                    selectedDate,
                                    TaskRepeatOption.CUSTOM,
                                    customIntervalDaysParsed(),
                                )
                                repeatMenuExpanded = false
                            },
                        )
                    }
                }
                if (repeatOption == TaskRepeatOption.CUSTOM) {
                    androidx.compose.material3.OutlinedTextField(
                        value = customRepeatDaysText,
                        onValueChange = { v ->
                            if (v.length <= 2 && v.all { it.isDigit() }) customRepeatDaysText = v
                        },
                        label = { androidx.compose.material3.Text("Every N days") },
                        supportingText = {
                            androidx.compose.material3.Text(
                                "2–30 days between each occurrence (up to 8 times).",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (repeatOption != TaskRepeatOption.NONE) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { showRepeatUntilPicker = true }) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = "Pick repeat end date",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Text(
                            "Repeat until ${(repeatUntilDate ?: repeatDefaultUntil(selectedDate, repeatOption, customIntervalDaysParsed())).format(dateFmt)}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showRepeatUntilPicker = true },
                        )
                    }
                    Text(
                        "No new occurrences are added after this date (still within each pattern’s max count).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (task != null && task.repeatSeriesId != null) {
                    Text(
                        "Changes to repeat or task details apply to every day in this series.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                androidx.compose.material3.OutlinedTextField(
                    value = listNumberText,
                    onValueChange = { v ->
                        if (v.length <= 3 && v.all { it.isDigit() }) listNumberText = v
                    },
                    label = { androidx.compose.material3.Text("List # (optional)") },
                    placeholder = { androidx.compose.material3.Text("Auto") },
                    supportingText = {
                        androidx.compose.material3.Text(
                            "Leave blank for 1, 2, 3… by rank and start time for this day.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                ExposedDropdownMenuBox(
                    expanded = goalMenuExpanded,
                    onExpandedChange = { goalMenuExpanded = it },
                ) {
                    androidx.compose.material3.OutlinedTextField(
                        value = linkedGoalLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { androidx.compose.material3.Text("Linked Goal") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = goalMenuExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = goalMenuExpanded,
                        onDismissRequest = { goalMenuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { androidx.compose.material3.Text("None") },
                            onClick = {
                                linkedGoalId = null
                                goalMenuExpanded = false
                            },
                        )
                        activeGoals.forEach { g ->
                            DropdownMenuItem(
                                text = {
                                    androidx.compose.material3.Text(
                                        g.title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                onClick = {
                                    linkedGoalId = g.id
                                    goalMenuExpanded = false
                                },
                            )
                        }
                    }
                }
                Text("Priority", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Urgency.entries.forEach { u ->
                        val label = when (u) {
                            Urgency.GREEN -> "Normal"
                            Urgency.RED -> "High"
                            Urgency.NEUTRAL -> "Low"
                        }
                        val color = when (u) {
                            Urgency.GREEN -> AccentGreen
                            Urgency.RED -> AccentRed
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        androidx.compose.material3.FilterChip(
                            selected = urgency == u,
                            onClick = { urgency = u },
                            label = { Text(label) },
                            colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                selectedContainerColor = color.copy(alpha = 0.15f),
                                selectedLabelColor = color,
                            ),
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                ) {
                    androidx.compose.material3.Checkbox(checked = isTop5, onCheckedChange = { isTop5 = it })
                    Text(
                        "Mark as Top 5 Priority",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                ) {
                    androidx.compose.material3.Checkbox(checked = isMustDo, onCheckedChange = { isMustDo = it })
                    Text(
                        "Must-do (meals, sleep, etc.). Not counted as productive time.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                }
                Text("Tags", style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    suggestedTags.forEach { st ->
                        val selected = tags.contains(st)
                        androidx.compose.material3.FilterChip(
                            selected = selected,
                            onClick = {
                                tags = if (selected) {
                                    tags.filterNot { it == st }
                                } else {
                                    onEnsureDefaultTagColor(st)
                                    tags + st
                                }
                            },
                            label = { Text(st) },
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    androidx.compose.material3.OutlinedTextField(
                        value = newTagText,
                        onValueChange = { newTagText = it },
                        label = { Text("New tag") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    androidx.compose.material3.Button(
                        onClick = {
                            val t = newTagText.trim()
                            if (t.isNotEmpty() && !tags.contains(t)) {
                                pendingNewTagLabel = t
                                showNewTagColorPicker = true
                            }
                        },
                    ) { Text("Add") }
                }
                if (tags.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        tags.forEach { tg ->
                            androidx.compose.material3.InputChip(
                                selected = true,
                                onClick = { tags = tags.filterNot { it == tg } },
                                label = { Text(tg) },
                            )
                        }
                    }
                }
                if (task != null && task.id > 0L && onStartPomodoro != null) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            onStartPomodoro()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Start Pomodoro")
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = true
                        return@Button
                    }
                    val displayNumber = listNumberText.toIntOrNull()?.coerceIn(1, 999) ?: 0
                    val customDays = customIntervalDaysParsed()
                    val repeatUntil = if (repeatOption == TaskRepeatOption.NONE) {
                        null
                    } else {
                        maxOf(
                            repeatUntilDate
                                ?: repeatDefaultUntil(selectedDate, repeatOption, customDays),
                            selectedDate,
                        )
                    }
                    onConfirm(
                        title.trim(),
                        selectedDate,
                        startTime,
                        endTime,
                        urgency,
                        isTop5,
                        isMustDo,
                        isCantComplete,
                        displayNumber,
                        note.takeIf { it.isNotBlank() },
                        linkedGoalId,
                        repeatOption,
                        customDays,
                        repeatUntil,
                        tags.map { it.trim() }.filter { it.isNotEmpty() }.distinct(),
                    )
                },
            ) { Text("Save") }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
