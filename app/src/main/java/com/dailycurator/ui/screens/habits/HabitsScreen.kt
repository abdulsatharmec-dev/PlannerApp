package com.dailycurator.ui.screens.habits

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyRow
import androidx.hilt.navigation.compose.hiltViewModel
import com.dailycurator.data.model.HabitType
import com.dailycurator.ui.components.HabitCard
import com.dailycurator.ui.theme.*

import com.dailycurator.data.model.Habit
import com.dailycurator.ui.components.DurationPresetSelector
import com.dailycurator.ui.components.NumericTargetStepper
import com.dailycurator.ui.components.formatDurationMinutes
import java.time.LocalDate
import kotlin.math.roundToInt

@Composable
fun HabitsScreen(
    onNavigateToPomodoro: () -> Unit = {},
    viewModel: HabitsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var showAddHabit by remember { mutableStateOf(false) }
    var habitsMenuOpen by remember { mutableStateOf(false) }
    var habitToMarkDone by remember { mutableStateOf<Habit?>(null) }
    var selectedHabit by remember { mutableStateOf<Habit?>(null) }
    var habitToEdit by remember { mutableStateOf<Habit?>(null) }
    var habitPendingDelete by remember { mutableStateOf<Habit?>(null) }
    var detailCalendarMonth by remember { mutableStateOf(LocalDate.now()) }
    var completedDays by remember { mutableStateOf<Set<LocalDate>>(emptySet()) }
    var missedWindow by remember { mutableStateOf(0) }

    val seriesKey = selectedHabit?.let { h -> h.seriesId.ifBlank { h.id.toString() } } ?: ""
    val habitLogs by viewModel.habitLogsFlow(seriesKey).collectAsState(initial = emptyList())

    LaunchedEffect(selectedHabit?.id, detailCalendarMonth) {
        val h = selectedHabit ?: return@LaunchedEffect
        completedDays = viewModel.completedDaysForMonth(h, detailCalendarMonth)
    }
    LaunchedEffect(selectedHabit?.id) {
        val h = selectedHabit ?: return@LaunchedEffect
        missedWindow = viewModel.missedCountFor(h)
    }

    if (showAddHabit) {
        AddHabitDialog(
            habit = null,
            onDismiss = { showAddHabit = false },
            onSave = { _, name, category, type, emoji, target, unit, trigger, freq ->
                viewModel.addHabit(name, category, type, emoji, target, unit, trigger, freq)
                showAddHabit = false
            },
        )
    }

    habitToEdit?.let { editing ->
        AddHabitDialog(
            habit = editing,
            onDismiss = { habitToEdit = null },
            onSave = { base, name, category, type, emoji, target, unit, trigger, freq ->
                if (base != null) {
                    viewModel.updateHabit(base, name, category, type, emoji, target, unit, trigger, freq)
                }
                habitToEdit = null
            },
        )
    }

    habitPendingDelete?.let { toDelete ->
        AlertDialog(
            onDismissRequest = { habitPendingDelete = null },
            title = { Text("Delete habit?") },
            text = { Text("Remove \"${toDelete.name}\" and all of its history? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteHabitSeries(toDelete)
                        habitPendingDelete = null
                        if (selectedHabit?.id == toDelete.id) selectedHabit = null
                    },
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { habitPendingDelete = null }) { Text("Cancel") }
            },
        )
    }

    if (habitToMarkDone != null) {
        MarkHabitDoneDialog(
            habit = habitToMarkDone!!,
            onDismiss = { habitToMarkDone = null },
            onConfirm = { note ->
                viewModel.markHabitDone(habitToMarkDone!!, note.takeIf { it.isNotBlank() })
                habitToMarkDone = null
            },
        )
    }

    selectedHabit?.let { detail ->
        HabitDetailBottomSheet(
            habit = detail,
            logs = habitLogs,
            completedDays = completedDays,
            calendarMonth = detailCalendarMonth,
            onMonthChange = { detailCalendarMonth = it },
            missedCount = missedWindow,
            onDismiss = { selectedHabit = null },
            onEdit = {
                habitToEdit = detail
                selectedHabit = null
            },
            onRequestDelete = {
                habitPendingDelete = detail
                selectedHabit = null
            },
            onMarkDone = { note ->
                viewModel.markHabitDone(detail, note)
                selectedHabit = null
            },
            onStartPomodoro = {
                viewModel.startPomodoroForHabit(detail)
                selectedHabit = null
                onNavigateToPomodoro()
            },
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
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Habits",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.weight(1f))
                Box {
                    IconButton(onClick = { habitsMenuOpen = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "More options",
                        )
                    }
                    DropdownMenu(
                        expanded = habitsMenuOpen,
                        onDismissRequest = { habitsMenuOpen = false },
                    ) {
                        Row(
                            modifier = Modifier
                                .widthIn(min = 220.dp)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Show completed",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Switch(
                                checked = state.showCompleted,
                                onCheckedChange = {
                                    viewModel.toggleShowCompleted()
                                },
                            )
                        }
                    }
                }
                IconButton(onClick = { showAddHabit = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add habit")
                }
            }
        }

        item {
            Text(
                "Building",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 20.dp, top = 8.dp, end = 20.dp),
            )
            Spacer(Modifier.height(8.dp))
        }

        items(state.buildingHabits, key = { it.id }) { habit ->
            HabitCard(
                habit = habit,
                modifier = Modifier.padding(horizontal = 20.dp),
                onClick = { selectedHabit = habit },
                onQuickDone = { habitToMarkDone = habit },
            )
            Spacer(Modifier.height(12.dp))
        }

        item {
            Spacer(Modifier.height(8.dp))
            Text(
                "Eliminating",
                style = MaterialTheme.typography.titleSmall,
                color = AccentRed,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(8.dp))
        }

        items(state.eliminatingHabits, key = { it.id }) { habit ->
            HabitCard(
                habit = habit,
                modifier = Modifier.padding(horizontal = 20.dp),
                onClick = { selectedHabit = habit },
                onQuickDone = { habitToMarkDone = habit },
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
internal fun MarkHabitDoneDialog(
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
    habit: Habit?,
    onDismiss: () -> Unit,
    onSave: (Habit?, String, String, HabitType, String, Float, String, String?, String) -> Unit,
) {
    var name      by remember(habit?.id) { mutableStateOf(habit?.name ?: "") }
    var emoji     by remember(habit?.id) { mutableStateOf(habit?.iconEmoji ?: "⭐") }
    var targetFloat by remember(habit?.id) { mutableStateOf(habit?.targetValue ?: 1f) }
    var unit      by remember(habit?.id) { mutableStateOf(habit?.unit ?: "times") }
    var category  by remember(habit?.id) { mutableStateOf(habit?.category ?: "Physical") }
    var habitType by remember(habit?.id) { mutableStateOf(habit?.habitType ?: HabitType.BUILDING) }
    var trigger   by remember(habit?.id) { mutableStateOf(habit?.trigger ?: "") }
    var frequency by remember(habit?.id) { mutableStateOf(habit?.frequency ?: "daily") }
    var nameError by remember { mutableStateOf(false) }

    LaunchedEffect(unit) {
        if (unit == "mins" && targetFloat < 5f) targetFloat = 15f
    }

    val unitOptions = listOf("times", "mins", "hours", "ml", "pages", "custom")
    val defaultCategories = listOf("Physical", "Mental", "Spiritual")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        title = {
            Text(
                if (habit == null) "New Habit" else "Edit Habit",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
            )
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
                    when (unit) {
                        "mins", "hours" -> {
                            val selectedMinutes = if (unit == "mins") {
                                targetFloat.roundToInt().coerceIn(1, 24 * 60)
                            } else {
                                (targetFloat * 60f).roundToInt().coerceIn(15, 24 * 60)
                            }
                            DurationPresetSelector(
                                selectedMinutes = selectedMinutes,
                                onMinutesSelected = { m ->
                                    targetFloat = if (unit == "mins") m.toFloat() else m / 60f
                                }
                            )
                            val hint = if (unit == "mins") {
                                "${targetFloat.roundToInt()} minutes total"
                            } else {
                                val mins = (targetFloat * 60f).roundToInt()
                                "${"%.1f".format(targetFloat)} hours (${formatDurationMinutes(mins)})"
                            }
                            Text(
                                hint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        else -> {
                            NumericTargetStepper(
                                value = targetFloat,
                                onValueChange = { targetFloat = it },
                                step = 1f,
                                range = 1f..500f,
                                label = "Target"
                            )
                        }
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
                        items(listOf("daily", "weekly", "weekdays", "monthly")) { f ->
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
                onSave(
                    habit,
                    name.trim(),
                    category.trim().ifBlank { "Uncategorized" },
                    habitType,
                    emoji.trim().ifBlank { "⭐" },
                    targetFloat,
                    unit.trim().ifBlank { "times" },
                    trigger.trim().takeIf { it.isNotBlank() },
                    frequency,
                )
            }) { Text(if (habit == null) "Add Habit" else "Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
