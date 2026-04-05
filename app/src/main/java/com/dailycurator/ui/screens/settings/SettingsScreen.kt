package com.dailycurator.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dailycurator.data.ai.AiPromptDefaults
import com.dailycurator.data.local.CerebrasModelOption
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val sectionGap = 12.dp
private val horizontalPad = 20.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val isDark by viewModel.isDarkTheme.collectAsState()
    val assistantInsightEnabled by viewModel.assistantInsightEnabled.collectAsState()
    val weeklyGoalsInsightEnabled by viewModel.weeklyGoalsInsightEnabled.collectAsState()
    val journalShareWithChat by viewModel.journalShareWithChat.collectAsState()
    val journalInAssistantInsight by viewModel.journalInAssistantInsight.collectAsState()
    val journalInWeeklyGoalsInsight by viewModel.journalInWeeklyGoalsInsight.collectAsState()
    val assistantInsightPrompt by viewModel.assistantInsightPrompt.collectAsState()
    val weeklyGoalsInsightPrompt by viewModel.weeklyGoalsInsightPrompt.collectAsState()
    val cerebrasKey by viewModel.cerebrasKey.collectAsState()
    val cerebrasModelId by viewModel.cerebrasModelId.collectAsState()

    var modelMenuExpanded by remember { mutableStateOf(false) }
    var showAssistantPromptDialog by remember { mutableStateOf(false) }
    var showWeeklyPromptDialog by remember { mutableStateOf(false) }

    val catalog = viewModel.cerebrasModelChoices
    val pickerOptions = remember(cerebrasModelId, catalog) {
        buildList {
            addAll(catalog)
            val known = catalog.map { it.modelId }.toSet()
            if (cerebrasModelId.isNotBlank() && cerebrasModelId !in known) {
                add(CerebrasModelOption("Other (saved id)", cerebrasModelId))
            }
        }
    }
    val selectedModelLabel =
        pickerOptions.find { it.modelId == cerebrasModelId }?.displayName ?: cerebrasModelId

    if (showAssistantPromptDialog) {
        InsightPromptEditorDialog(
            title = "Assistant insight prompt",
            initialText = assistantInsightPrompt,
            defaultText = AiPromptDefaults.ASSISTANT_INSIGHT,
            onDismiss = { showAssistantPromptDialog = false },
            onSave = {
                viewModel.persistAssistantPrompt(it)
                showAssistantPromptDialog = false
            },
        )
    }
    if (showWeeklyPromptDialog) {
        InsightPromptEditorDialog(
            title = "Weekly goals prompt",
            initialText = weeklyGoalsInsightPrompt,
            defaultText = AiPromptDefaults.WEEKLY_GOALS_INSIGHT,
            onDismiss = { showWeeklyPromptDialog = false },
            onSave = {
                viewModel.persistWeeklyGoalsPrompt(it)
                showWeeklyPromptDialog = false
            },
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(sectionGap),
    ) {
        item {
            Text(
                "Settings",
                style = MaterialTheme.typography.displaySmall.copy(
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold,
                ),
                modifier = Modifier.padding(horizontal = horizontalPad, vertical = 8.dp),
            )
        }

        item {
            SettingsSection(title = "Appearance") {
                SettingsToggleRow(
                    icon = if (isDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                    label = if (isDark) "Dark theme" else "Light theme",
                    checked = isDark,
                    onCheckedChange = { viewModel.toggleDarkTheme() },
                )
            }
        }

        item {
            SettingsSection(title = "Home day window") {
                val dayWindow by viewModel.dayWindow.collectAsState()
                var showDayStartPicker by remember { mutableStateOf(false) }
                var showDayEndPicker by remember { mutableStateOf(false) }
                val timeFmt = remember { DateTimeFormatter.ofPattern("h:mm a") }

                if (showDayStartPicker) {
                    DayWindowTimePickerDialog(
                        title = "Day starts",
                        initial = minuteOfDayToLocalTime(dayWindow.startMinute),
                        onDismiss = { showDayStartPicker = false },
                        onConfirm = {
                            viewModel.setDayWindowStart(it)
                            showDayStartPicker = false
                        },
                    )
                }
                if (showDayEndPicker) {
                    DayWindowTimePickerDialog(
                        title = "Day ends",
                        initial = minuteOfDayToLocalTime(dayWindow.endMinute),
                        onDismiss = { showDayEndPicker = false },
                        onConfirm = {
                            viewModel.setDayWindowEnd(it)
                            showDayEndPicker = false
                        },
                    )
                }

                Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Text(
                        "Top progress bar on Home and the schedule timeline/clock use this range.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    SettingsNavRow(
                        icon = Icons.Default.Schedule,
                        title = "Day starts",
                        subtitle = minuteOfDayToLocalTime(dayWindow.startMinute).format(timeFmt),
                        onClick = { showDayStartPicker = true },
                    )
                    HorizontalDivider(Modifier.padding(start = 16.dp))
                    SettingsNavRow(
                        icon = Icons.Default.Schedule,
                        title = "Day ends",
                        subtitle = minuteOfDayToLocalTime(dayWindow.endMinute).format(timeFmt),
                        onClick = { showDayEndPicker = true },
                    )
                }
            }
        }

        item {
            SettingsSection(title = "Cerebras") {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        "API key",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = cerebrasKey,
                        onValueChange = { viewModel.onCerebrasKeyChange(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Paste key") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Model",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    ExposedDropdownMenuBox(
                        expanded = modelMenuExpanded,
                        onExpandedChange = { modelMenuExpanded = it },
                    ) {
                        OutlinedTextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            readOnly = true,
                            value = selectedModelLabel,
                            onValueChange = {},
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelMenuExpanded)
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        )
                        ExposedDropdownMenu(
                            expanded = modelMenuExpanded,
                            onDismissRequest = { modelMenuExpanded = false },
                        ) {
                            pickerOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.displayName) },
                                    onClick = {
                                        viewModel.onCerebrasModelSelected(option.modelId)
                                        modelMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.saveCerebrasKey() },
                        modifier = Modifier.align(Alignment.End),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Save key")
                    }
                }
            }
        }

        item {
            SettingsSection(title = "Journal & AI") {
                SettingsToggleRow(
                    icon = Icons.Default.Book,
                    label = "Share journal with chat agent",
                    subtitle = "AI Agent can read recent entries in its context",
                    checked = journalShareWithChat,
                    onCheckedChange = { viewModel.setJournalShareWithChat(it) },
                )
                HorizontalDivider(Modifier.padding(start = 16.dp))
                SettingsToggleRow(
                    icon = null,
                    label = "Journal in assistant insight",
                    subtitle = "Home “Assistant insight” may use recent entries",
                    checked = journalInAssistantInsight,
                    onCheckedChange = { viewModel.setJournalInAssistantInsight(it) },
                )
                HorizontalDivider(Modifier.padding(start = 16.dp))
                SettingsToggleRow(
                    icon = null,
                    label = "Journal in weekly goals insight",
                    subtitle = "Weekly goals coaching may use recent entries",
                    checked = journalInWeeklyGoalsInsight,
                    onCheckedChange = { viewModel.setJournalInWeeklyGoalsInsight(it) },
                )
            }
        }

        item {
            SettingsSection(title = "Home insights") {
                SettingsToggleRow(
                    icon = null,
                    label = "Assistant insight card",
                    subtitle = "Daily summary on the home screen",
                    checked = assistantInsightEnabled,
                    onCheckedChange = { viewModel.setAssistantInsightEnabled(it) },
                )
                HorizontalDivider(Modifier.padding(start = 16.dp))
                SettingsToggleRow(
                    icon = null,
                    label = "Weekly goals insight",
                    subtitle = "Coaching under weekly goals",
                    checked = weeklyGoalsInsightEnabled,
                    onCheckedChange = { viewModel.setWeeklyGoalsInsightEnabled(it) },
                )
                HorizontalDivider(Modifier.padding(start = 16.dp))
                SettingsNavRow(
                    icon = Icons.Default.EditNote,
                    title = "Assistant system prompt",
                    subtitle = "Tap to edit · used for future generations",
                    onClick = { showAssistantPromptDialog = true },
                )
                HorizontalDivider(Modifier.padding(start = 16.dp))
                SettingsNavRow(
                    icon = Icons.Default.EditNote,
                    title = "Weekly goals system prompt",
                    subtitle = "Tap to edit · used for future generations",
                    onClick = { showWeeklyPromptDialog = true },
                )
            }
        }

        item {
            SettingsSection(title = "About") {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Daily Curator",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            "Version 1.0",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(Modifier.padding(horizontal = horizontalPad)) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        Spacer(Modifier.height(6.dp))
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(0.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector?,
    label: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(12.dp))
        } else {
            Spacer(Modifier.width(34.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
            ),
        )
    }
}

@Composable
private fun SettingsNavRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showChevron: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (showChevron) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun minuteOfDayToLocalTime(minuteOfDay: Int): LocalTime {
    val c = minuteOfDay.coerceIn(0, 24 * 60 - 1)
    return LocalTime.of(c / 60, c % 60)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayWindowTimePickerDialog(
    title: String,
    initial: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = false,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) {
                Text("OK")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun InsightPromptEditorDialog(
    title: String,
    initialText: String,
    defaultText: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var draft by remember { mutableStateOf(initialText) }
    LaunchedEffect(initialText) {
        draft = initialText
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    "Applies only to new or regenerated insights.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 10,
                    maxLines = 16,
                    shape = RoundedCornerShape(12.dp),
                )
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = { draft = defaultText },
                    modifier = Modifier.align(Alignment.Start),
                ) {
                    Text("Restore default")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(draft) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
