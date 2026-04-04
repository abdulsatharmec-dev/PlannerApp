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
            onConfirm = { title ->
                viewModel.addGoal(title)
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
                        containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null,
                        tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("New Goal",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White, fontWeight = FontWeight.SemiBold))
                }
            }
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

// ── Add Goal Dialog ────────────────────────────────────────────────────────

@Composable
private fun AddGoalDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text("New Weekly Goal",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface))
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it; isError = false },
                label = { Text("Goal title") },
                isError = isError,
                supportingText = { if (isError) Text("Title is required") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = {
                if (text.isBlank()) { isError = true; return@Button }
                onConfirm(text.trim())
            }) { Text("Add Goal") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
