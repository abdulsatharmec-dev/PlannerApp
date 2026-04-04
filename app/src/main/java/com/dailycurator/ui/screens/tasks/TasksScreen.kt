package com.dailycurator.ui.screens.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dailycurator.data.model.PriorityTask
import com.dailycurator.data.model.Urgency
import com.dailycurator.ui.components.PriorityItem
import com.dailycurator.ui.theme.*
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun TasksScreen(viewModel: TasksViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<PriorityTask?>(null) }

    if (showAddDialog || taskToEdit != null) {
        ManageTaskDialog(
            task = taskToEdit,
            onDismiss = { 
                showAddDialog = false
                taskToEdit = null 
            },
            onConfirm = { title, start, end, urgency, isTop5, note ->
                if (taskToEdit == null) {
                    viewModel.addTask(title, start, end, urgency, isTop5, note)
                } else {
                    viewModel.updateTask(taskToEdit!!, title, start, end, urgency, isTop5, note)
                }
                showAddDialog = false
                taskToEdit = null
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
        ) {
            item {
                Text(
                    "All Tasks",
                    style = MaterialTheme.typography.displayLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
            
            items(state.tasks, key = { it.id }) { task ->
                SwipeToDeleteContainer(
                    item = task,
                    onDelete = { viewModel.deleteTask(task) }
                ) {
                    PriorityItem(
                        task = task,
                        onToggleDone = { viewModel.toggleTaskDone(task) },
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .clickable { taskToEdit = task }
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SwipeToDeleteContainer(
    item: T,
    onDelete: (T) -> Unit,
    animationDuration: Int = 500,
    content: @Composable (T) -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onDelete(item)
                true
            } else false
        }
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
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
            }
        },
        content = { content(item) },
        enableDismissFromStartToEnd = false
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManageTaskDialog(
    task: PriorityTask?,
    onDismiss: () -> Unit,
    onConfirm: (String, LocalTime, LocalTime, Urgency, Boolean, String?) -> Unit
) {
    var title by remember { mutableStateOf(task?.title ?: "") }
    var note by remember { mutableStateOf(task?.statusNote ?: "") }
    val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
    var startText by remember { mutableStateOf(task?.startTime?.format(timeFmt) ?: "09:00") }
    var endText by remember { mutableStateOf(task?.endTime?.format(timeFmt) ?: "10:00") }
    var urgency by remember { mutableStateOf(task?.urgency ?: Urgency.GREEN) }
    var isTop5 by remember { mutableStateOf(task?.rank == 1) }
    var titleError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (task == null) "New Task" else "Edit Task",
                style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it; titleError = false },
                    label = { Text("Task title") }, isError = titleError,
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note, onValueChange = { note = it },
                    label = { Text("Notes / Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startText, onValueChange = { startText = it },
                        label = { Text("Start (HH:mm)") }, modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endText, onValueChange = { endText = it },
                        label = { Text("End (HH:mm)") }, modifier = Modifier.weight(1f)
                    )
                }
                Text("Priority", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Urgency.values().forEach { u ->
                        val label = when (u) {
                            Urgency.GREEN -> "Normal"; Urgency.RED -> "High"; Urgency.NEUTRAL -> "Low"
                        }
                        val color = when(u) { Urgency.GREEN -> AccentGreen; Urgency.RED -> AccentRed; else -> Color.Gray }
                        FilterChip(
                            selected = urgency == u, onClick = { urgency = u }, label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = color.copy(alpha = 0.15f), selectedLabelColor = color
                            )
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isTop5, onCheckedChange = { isTop5 = it })
                    Text("Mark as Top 5 Priority", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (title.isBlank()) { titleError = true; return@Button }
                val start = runCatching { LocalTime.parse(startText, timeFmt) }.getOrElse { LocalTime.of(9, 0) }
                val end = runCatching { LocalTime.parse(endText, timeFmt) }.getOrElse { start.plusHours(1) }
                onConfirm(title.trim(), start, end, urgency, isTop5, note.takeIf { it.isNotBlank() })
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
