package com.dailycurator.ui.screens.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyRow
import androidx.hilt.navigation.compose.hiltViewModel
import com.dailycurator.data.model.HabitType
import com.dailycurator.ui.components.HabitCard
import com.dailycurator.ui.theme.*

import com.dailycurator.data.model.Habit

@Composable
fun HabitsScreen(viewModel: HabitsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    var showAddHabit by remember { mutableStateOf(false) }
    var habitToMarkDone by remember { mutableStateOf<Habit?>(null) }

    if (showAddHabit) {
        AddHabitDialog(
            onDismiss = { showAddHabit = false },
            onConfirm = { name, category, type, emoji, target, unit, trigger, freq ->
                viewModel.addHabit(name, category, type, emoji, target, unit, trigger, freq)
                showAddHabit = false
            }
        )
    }

    if (habitToMarkDone != null) {
        MarkHabitDoneDialog(
            habit = habitToMarkDone!!,
            onDismiss = { habitToMarkDone = null },
            onConfirm = { note ->
                viewModel.markHabitDone(habitToMarkDone!!, note.takeIf { it.isNotBlank() })
                habitToMarkDone = null
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // App bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text("Curator",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = MaterialTheme.colorScheme.primary))
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.GridView, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text("A", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("PERFORMANCE TRACK",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp))
                    Text("Habits",
                        style = MaterialTheme.typography.displayLarge.copy(
                            color = MaterialTheme.colorScheme.onBackground))
                }
                Button(
                    onClick = { showAddHabit = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null,
                        tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("New Habit",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White, fontWeight = FontWeight.SemiBold))
                }
            }
            Spacer(Modifier.height(16.dp))

            // Toggle show completed
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Show completed habits", style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                Switch(
                    checked = state.showCompleted,
                    onCheckedChange = { viewModel.toggleShowCompleted() }
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        // AI Habit Extractor card
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
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AccentTeal.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null,
                            tint = AccentTeal, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("AI Habit Extractor",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface))
                        Text("Analyze your journal for new habits",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant))
                    }
                    Icon(Icons.Default.Add, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // ── BUILDING MOMENTUM
        item {
            Text("BUILDING MOMENTUM",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.5.sp),
                modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(10.dp))
        }

        items(state.buildingHabits, key = { it.id }) { habit ->
            HabitCard(
                habit = habit,
                modifier = Modifier.padding(horizontal = 20.dp),
                onMarkDoneClick = { habitToMarkDone = habit }
            )
            Spacer(Modifier.height(12.dp))
        }

        // ── ELIMINATING FRICTION
        item {
            Spacer(Modifier.height(8.dp))
            Text("ELIMINATING FRICTION",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = AccentRed, letterSpacing = 1.5.sp),
                modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(10.dp))
        }

        items(state.eliminatingHabits, key = { it.id }) { habit ->
            HabitCard(
                habit = habit,
                modifier = Modifier.padding(horizontal = 20.dp),
                onMarkDoneClick = { habitToMarkDone = habit }
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun MarkHabitDoneDialog(
    habit: Habit,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        title = { Text("Mark '${habit.name}' as Done") },
        text = {
            Column {
                Text("Add an optional note about today's progress:")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(note) }) { Text("Mark Done") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ── Add Habit Dialog ───────────────────────────────────────────────────────

@Composable
private fun AddHabitDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, HabitType, String, Float, String, String?, String) -> Unit
) {
    var name      by remember { mutableStateOf("") }
    var emoji     by remember { mutableStateOf("⭐") }
    var target    by remember { mutableStateOf("1") }
    var unit      by remember { mutableStateOf("times") }
    var category  by remember { mutableStateOf("Physical") }
    var habitType by remember { mutableStateOf(HabitType.BUILDING) }
    var trigger   by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("daily") }
    var nameError by remember { mutableStateOf(false) }

    val unitOptions = listOf("times", "mins", "hours", "ml", "pages", "custom")
    val defaultCategories = listOf("Physical", "Mental", "Spiritual")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        title = {
            Text("New Habit",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface))
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.heightIn(max = 450.dp)
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = emoji,
                            onValueChange = { if (it.length <= 2) emoji = it },
                            label = { Text("Icon") },
                            singleLine = true,
                            modifier = Modifier.width(72.dp)
                        )
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it; nameError = false },
                            label = { Text("Habit name") },
                            isError = nameError,
                            supportingText = { if (nameError) Text("Required") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = target,
                            onValueChange = { target = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Target") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Text("Unit", style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(unitOptions) { opt ->
                            FilterChip(
                                selected = unit == opt,
                                onClick = { unit = opt },
                                label = { Text(opt) }
                            )
                        }
                    }
                    if (unit == "custom") {
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            label = { Text("Custom Unit") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                item {
                    Text("Category", style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(defaultCategories + listOf("Custom")) { cat ->
                            FilterChip(
                                selected = if(cat == "Custom") category !in defaultCategories else category == cat,
                                onClick = { category = if(cat == "Custom") "" else cat },
                                label = { Text(cat) }
                            )
                        }
                    }
                    if (category !in defaultCategories) {
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text("Custom Category Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                item {
                    Text("Type", style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = habitType == HabitType.BUILDING,
                            onClick = { habitType = HabitType.BUILDING },
                            label = { Text("Building") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentGreen.copy(alpha = 0.15f),
                                selectedLabelColor = AccentGreen
                            )
                        )
                        FilterChip(
                            selected = habitType == HabitType.ELIMINATING,
                            onClick = { habitType = HabitType.ELIMINATING },
                            label = { Text("Eliminating") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentRed.copy(alpha = 0.15f),
                                selectedLabelColor = AccentRed
                            )
                        )
                    }
                }

                item {
                    Text("Habit Stacking (Optional)", style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                    OutlinedTextField(
                        value = trigger,
                        onValueChange = { trigger = it },
                        label = { Text("After I do... (Trigger)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Text("Frequency", style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("daily", "weekly", "weekdays")) { f ->
                            FilterChip(
                                selected = frequency == f,
                                onClick = { frequency = f },
                                label = { Text(f.replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isBlank()) { nameError = true; return@Button }
                val targetFloat = target.toFloatOrNull() ?: 1f
                onConfirm(
                    name.trim(), 
                    category.trim().ifBlank { "Uncategorized" }, 
                    habitType, 
                    emoji.trim().ifBlank { "⭐" },
                    targetFloat, 
                    unit.trim().ifBlank { "times" },
                    trigger.trim().takeIf { it.isNotBlank() },
                    frequency
                )
            }) { Text("Add Habit") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
