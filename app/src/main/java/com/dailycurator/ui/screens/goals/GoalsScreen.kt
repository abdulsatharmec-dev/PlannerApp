package com.dailycurator.ui.screens.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.dailycurator.ui.components.GoalListItem
import com.dailycurator.ui.theme.*

@Composable
fun GoalsScreen(viewModel: GoalsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val progress = if (state.total > 0) state.completedCount.toFloat() / state.total else 0f
    var showAddGoal by remember { mutableStateOf(false) }

    if (showAddGoal) {
        AddGoalDialog(
            onDismiss = { showAddGoal = false },
            onConfirm = { title, description, deadline, time, category ->
                viewModel.addGoal(title, description, deadline, time, category)
                showAddGoal = false
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
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
                    onClick = { showAddGoal = true },
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
            GoalsCalendarView(goals = state.pendingGoals + state.completedGoals)
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
            GoalListItem(
                goal = goal, onToggle = { viewModel.toggleGoal(goal) },
                modifier = Modifier.padding(horizontal = 20.dp)
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
            GoalListItem(
                goal = goal, onToggle = { viewModel.toggleGoal(goal) },
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
    }
}

@Composable
private fun AddGoalDialog(onDismiss: () -> Unit, onConfirm: (String, String?, String?, String?, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf("") }
    var timeEstimate by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Spiritual") }
    var isError by remember { mutableStateOf(false) }

    val categories = listOf("Spiritual", "Health", "Finance", "Career", "Learning")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text("New Goal",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it; isError = false },
                    label = { Text("Goal title") }, isError = isError,
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text("Description (Optional)") }, modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = deadline, onValueChange = { deadline = it },
                        label = { Text("Deadline (e.g. Fri)") }, modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = timeEstimate, onValueChange = { timeEstimate = it },
                        label = { Text("Time (e.g. 2h)") }, modifier = Modifier.weight(1f)
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
                if (title.isBlank()) { isError = true; return@Button }
                onConfirm(title.trim(), description.takeIf { it.isNotBlank() }, deadline.takeIf { it.isNotBlank() }, timeEstimate.takeIf { it.isNotBlank() }, category)
            }) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun GoalsCalendarView(goals: List<com.dailycurator.data.model.WeeklyGoal>) {
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
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
                days.forEach { day ->
                    val hasDeadline = goals.any { it.deadline?.contains(day, ignoreCase = true) == true }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(day.take(1), style = MaterialTheme.typography.labelSmall)
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
