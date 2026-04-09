package com.dailycurator.ui.screens.tasks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.Color
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
import com.dailycurator.ui.components.PickDateDialog
import com.dailycurator.ui.components.PickTimeDialog
import com.dailycurator.ui.components.PriorityItem
import com.dailycurator.ui.components.resolvedTaskListNumber
import com.dailycurator.ui.components.tasksSortedForListNumber
import com.dailycurator.ui.components.formatDurationMinutes
import com.dailycurator.ui.theme.AccentGreen
import com.dailycurator.ui.theme.AccentRed
import com.dailycurator.ui.theme.appScaffoldContainerColor
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val WeekDayCardWidth = 86.dp
private val WeekDayCardHeight = 118.dp
private val WeekDayPreviewRowHeight = 20.dp
private val WeekDayPreviewMoreLineHeight = 12.dp
private val WeekDayPreviewSlotHeight =
    WeekDayPreviewRowHeight + WeekDayPreviewRowHeight + WeekDayPreviewMoreLineHeight
private val WeekDayCountLineHeight = 12.dp

@Composable
fun TasksScreen(
    onNavigateToPomodoro: () -> Unit = {},
    viewModel: TasksViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val orderedDayTasks = remember(state.tasks) { tasksSortedForListNumber(state.tasks) }
    var viewOptionsExpanded by rememberSaveable { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<PriorityTask?>(null) }
    var showJumpDatePicker by remember { mutableStateOf(false) }

    val weekRangeFmt = remember { DateTimeFormatter.ofPattern("MMM d") }
    val selectedLongFmt = remember { DateTimeFormatter.ofPattern("EEEE, MMM d") }
    val dayLetterFmt = remember { DateTimeFormatter.ofPattern("EEE") }

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
            onDismiss = {
                showAddDialog = false
                taskToEdit = null
            },
            onConfirm = { title, date, start, end, urgency, isTop5, isMustDo, displayNumber, note, goalId, repeat, customRepeatDays ->
                if (taskToEdit == null) {
                    viewModel.addTask(
                        title, date, start, end, urgency, isTop5, isMustDo, displayNumber, note, goalId,
                        repeat, customRepeatDays,
                    )
                } else {
                    viewModel.updateTask(taskToEdit!!, title, date, start, end, urgency, isTop5, isMustDo, displayNumber, note, goalId)
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

    val weekStart = state.weekDays.firstOrNull() ?: state.listDate
    val weekEnd = state.weekDays.lastOrNull() ?: state.listDate

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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewOptionsExpanded = !viewOptionsExpanded }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Tasks",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                            )
                            Text(
                                state.listDate.format(selectedLongFmt),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Icon(
                            if (viewOptionsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (viewOptionsExpanded) "Hide view options" else "Show view options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    AnimatedVisibility(
                        visible = viewOptionsExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        Column(
                            modifier = Modifier.padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    "Layout",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    TasksViewModeToggle(
                                        mode = state.displayMode,
                                        onSelect = { viewModel.setDisplayMode(it) },
                                    )
                                    IconButton(onClick = { showJumpDatePicker = true }) {
                                        Icon(
                                            Icons.Default.CalendarMonth,
                                            contentDescription = "Jump to date",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                IconButton(onClick = { viewModel.shiftSelectedWeek(-1) }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                        contentDescription = "Previous week",
                                    )
                                }
                                Text(
                                    "${weekStart.format(weekRangeFmt)} – ${weekEnd.format(weekRangeFmt)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                IconButton(onClick = { viewModel.shiftSelectedWeek(1) }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = "Next week",
                                    )
                                }
                            }
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 0.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                items(state.weekDays, key = { it.toString() }) { day ->
                                    val dayTasks = state.tasksByDay[day].orEmpty()
                                    WeekDayColumn(
                                        day = day,
                                        selected = day == state.listDate,
                                        previewTasks = dayTasks,
                                        dayLetterFmt = dayLetterFmt,
                                        onClick = { viewModel.setListDate(day) },
                                    )
                                }
                            }
                            Text(
                                when (state.displayMode) {
                                    TasksDisplayMode.WEEK_CALENDAR -> "Tap a day to choose · list order: rank, then time"
                                    TasksDisplayMode.PRIORITY_GROUPS -> "Same day, grouped by urgency"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    "Show completed",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Switch(
                                    checked = state.showCompletedTasks,
                                    onCheckedChange = { viewModel.setShowCompletedTasks(it) },
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }

            when (state.displayMode) {
                TasksDisplayMode.WEEK_CALENDAR -> {
                    if (state.tasks.isEmpty()) {
                        item {
                            EmptyTasksHint(
                                allTasksDoneHidden = state.allTasksDoneHidden,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                            )
                        }
                    } else {
                        items(state.tasks, key = { it.id }) { task ->
                            TaskRow(
                                task = task,
                                listNumber = resolvedTaskListNumber(task, orderedDayTasks),
                                viewModel = viewModel,
                                onEdit = { taskToEdit = task },
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
                                    onEdit = { taskToEdit = task },
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

@Composable
private fun TasksViewModeToggle(
    mode: TasksDisplayMode,
    onSelect: (TasksDisplayMode) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        val weekSelected = mode == TasksDisplayMode.WEEK_CALENDAR
        val priSelected = mode == TasksDisplayMode.PRIORITY_GROUPS
        FilledTonalIconButton(
            onClick = { onSelect(TasksDisplayMode.WEEK_CALENDAR) },
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
private fun WeekDayColumn(
    day: LocalDate,
    selected: Boolean,
    previewTasks: List<PriorityTask>,
    dayLetterFmt: DateTimeFormatter,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    val bg = if (selected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    Card(
        modifier = Modifier
            .width(WeekDayCardWidth)
            .height(WeekDayCardHeight)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        elevation = CardDefaults.cardElevation(if (selected) 2.dp else 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                .padding(horizontal = 6.dp, vertical = 6.dp),
        ) {
            Text(
                day.format(dayLetterFmt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                day.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            val count = previewTasks.size
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(WeekDayCountLineHeight),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = if (count > 0) {
                        "$count task${if (count == 1) "" else "s"}"
                    } else {
                        "—"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    lineHeight = 12.sp,
                    color = if (count > 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(2.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(WeekDayPreviewSlotHeight),
            ) {
                repeat(2) { index ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(WeekDayPreviewRowHeight),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        previewTasks.getOrNull(index)?.let { t ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(3.dp)
                                        .background(
                                            when (t.urgency) {
                                                Urgency.RED -> AccentRed
                                                Urgency.GREEN -> AccentGreen
                                                Urgency.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            CircleShape,
                                        ),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    t.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    lineHeight = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(WeekDayPreviewMoreLineHeight),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (previewTasks.size > 2) {
                        Text(
                            "+${previewTasks.size - 2}",
                            style = MaterialTheme.typography.labelSmall,
                            lineHeight = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyTasksHint(
    allTasksDoneHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    Text(
        if (allTasksDoneHidden) {
            "Everything here is done. Open View options (tap the header) and turn on Show completed to see checked-off tasks."
        } else {
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
    onEdit: () -> Unit,
    onStartPomodoro: () -> Unit,
    horizontalPadding: androidx.compose.ui.unit.Dp,
) {
    SwipeToDeleteContainer(
        item = task,
        onDelete = { viewModel.deleteTask(task) },
    ) { t ->
        PriorityItem(
            task = t,
            listNumber = listNumber,
            onToggleDone = { viewModel.toggleTaskDone(t) },
            onStartPomodoro = if (t.id > 0L) onStartPomodoro else null,
            modifier = Modifier
                .padding(horizontal = horizontalPadding)
                .clickable { onEdit() },
        )
    }
    Spacer(Modifier.height(4.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SwipeToDeleteContainer(
    item: T,
    onDelete: (T) -> Unit,
    animationDuration: Int = 500,
    content: @Composable (T) -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onDelete(item)
                true
            } else {
                false
            }
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.EndToStart -> AccentRed
                else -> Color.Transparent
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .background(color, RoundedCornerShape(12.dp))
                    .padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
            }
        },
        content = { content(item) },
        enableDismissFromStartToEnd = false,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManageTaskDialog(
    task: PriorityTask?,
    defaultListDate: LocalDate,
    activeGoals: List<WeeklyGoal>,
    onDismiss: () -> Unit,
    onConfirm: (String, LocalDate, LocalTime, LocalTime, Urgency, Boolean, Boolean, Int, String?, Long?, TaskRepeatOption, Int) -> Unit,
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
    var listNumberText by remember(task?.id) {
        mutableStateOf(
            if (task != null && task.displayNumber > 0) task.displayNumber.toString() else "",
        )
    }
    var linkedGoalId by remember(task?.id) { mutableStateOf<Long?>(task?.goalId) }
    var goalMenuExpanded by remember { mutableStateOf(false) }
    var repeatMenuExpanded by remember { mutableStateOf(false) }
    var repeatOption by remember(task?.id, defaultListDate) { mutableStateOf(TaskRepeatOption.NONE) }
    var customRepeatDaysText by remember(task?.id, defaultListDate) { mutableStateOf("3") }
    val linkedGoalLabel = linkedGoalId?.let { id -> activeGoals.find { it.id == id }?.title } ?: "None"
    var titleError by remember { mutableStateOf(false) }

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

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (task == null) "New Task" else "Edit Task",
                style = MaterialTheme.typography.titleLarge,
            )
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
                Text(
                    "Ends at ${endTime.format(DateTimeFormatter.ofPattern("HH:mm"))} (${formatDurationMinutes(durationMinutes)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (task == null) {
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
                                    repeatMenuExpanded = false
                                },
                            )
                            DropdownMenuItem(
                                text = { androidx.compose.material3.Text("Daily") },
                                onClick = {
                                    repeatOption = TaskRepeatOption.DAILY
                                    repeatMenuExpanded = false
                                },
                            )
                            DropdownMenuItem(
                                text = { androidx.compose.material3.Text("Weekly") },
                                onClick = {
                                    repeatOption = TaskRepeatOption.WEEKLY
                                    repeatMenuExpanded = false
                                },
                            )
                            DropdownMenuItem(
                                text = { androidx.compose.material3.Text("Custom") },
                                onClick = {
                                    repeatOption = TaskRepeatOption.CUSTOM
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
                    val customDays = customRepeatDaysText.toIntOrNull()?.coerceIn(2, 30) ?: 3
                    onConfirm(
                        title.trim(),
                        selectedDate,
                        startTime,
                        endTime,
                        urgency,
                        isTop5,
                        isMustDo,
                        displayNumber,
                        note.takeIf { it.isNotBlank() },
                        linkedGoalId,
                        if (task == null) repeatOption else TaskRepeatOption.NONE,
                        customDays,
                    )
                },
            ) { Text("Save") }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
