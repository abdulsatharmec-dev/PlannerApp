package com.dailycurator.ui.screens.gmail

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.PostAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dailycurator.data.gmail.GmailSummarySuggestedTask
import com.dailycurator.ui.components.MarkdownSummaryBody
import com.dailycurator.ui.components.MarkdownSummaryStyle
import com.dailycurator.ui.theme.appScreenBackground
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun GmailMailboxSummaryScreen(viewModel: GmailMailboxSummaryViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val accounts by viewModel.linkedAccounts.collectAsState()
    val timeFmt = remember { DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a") }
    var menuExpanded by remember { mutableStateOf(false) }
    var rangeSettingsOpen by remember { mutableStateOf(false) }

    if (state.showSuggestTasksDialog && state.suggestedTasks.isNotEmpty()) {
        SuggestedTasksDialog(
            tasks = state.suggestedTasks,
            selected = state.suggestedTaskSelected,
            onToggle = viewModel::toggleSuggestedTask,
            onSelectAll = viewModel::selectAllSuggestedTasks,
            onDismiss = viewModel::dismissSuggestTasksDialog,
            onConfirm = viewModel::confirmAddSuggestedTasks,
        )
    }

    if (rangeSettingsOpen) {
        RangeSettingsDialog(
            state = state,
            onDismiss = { rangeSettingsOpen = false },
            onPresetDays = viewModel::setPresetRangeDays,
            onCustomDaysChange = viewModel::setCustomDaysText,
            onApplyCustom = viewModel::applyCustomRange,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .appScreenBackground()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "📬 Gmail",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            FilledTonalButton(
                onClick = { viewModel.regenerate() },
                enabled = !state.loading && accounts.isNotEmpty(),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            ) {
                if (state.loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Generate", style = MaterialTheme.typography.labelMedium)
                }
            }
            FilledTonalButton(
                onClick = { viewModel.findTasksFromSummary() },
                enabled = !state.loading && !state.suggestTasksLoading && accounts.isNotEmpty(),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            ) {
                if (state.suggestTasksLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        Icons.Outlined.PostAdd,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Tasks", style = MaterialTheme.typography.labelMedium)
                }
            }
            BoxWithSettingsMenu(
                menuExpanded = menuExpanded,
                onMenuExpandedChange = { menuExpanded = it },
                onOpenRangeSettings = {
                    rangeSettingsOpen = true
                    menuExpanded = false
                },
            )
        }

        if (accounts.isEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Link Gmail in Settings and enable this account for summary.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        state.suggestTasksBanner?.let { msg ->
            Spacer(Modifier.height(6.dp))
            Text(
                msg,
                style = MaterialTheme.typography.labelSmall,
                color = if (msg.startsWith("Added")) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }

        state.error?.let { err ->
            Spacer(Modifier.height(6.dp))
            Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        state.generatedAtMillis?.let { ms ->
            Spacer(Modifier.height(4.dp))
            val z = ZoneId.systemDefault()
            val label = Instant.ofEpochMilli(ms).atZone(z).format(timeFmt)
            Text(
                "🕐 $label · last ${state.rangeDays}d",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(12.dp))

        MarkdownSummaryBody(
            markdown = state.markdown,
            style = MarkdownSummaryStyle.CompactRich,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun BoxWithSettingsMenu(
    menuExpanded: Boolean,
    onMenuExpandedChange: (Boolean) -> Unit,
    onOpenRangeSettings: () -> Unit,
) {
    Box {
        IconButton(
            onClick = { onMenuExpandedChange(true) },
            modifier = Modifier.size(40.dp),
        ) {
            Icon(Icons.Default.MoreVert, contentDescription = "Range and settings")
        }
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { onMenuExpandedChange(false) },
        ) {
            DropdownMenuItem(
                text = { Text("Date range & options") },
                onClick = onOpenRangeSettings,
            )
        }
    }
}

@Composable
private fun RangeSettingsDialog(
    state: GmailMailboxSummaryUiState,
    onDismiss: () -> Unit,
    onPresetDays: (Int) -> Unit,
    onCustomDaysChange: (String) -> Unit,
    onApplyCustom: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mailbox scan range") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Inbox + Spam, newer_than:N days. Summary wording comes from Settings → Gmail mailbox summary.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    FilterChip(
                        selected = state.rangeDays == 1,
                        onClick = { onPresetDays(1) },
                        label = { Text("1 day") },
                    )
                    FilterChip(
                        selected = state.rangeDays == 2,
                        onClick = { onPresetDays(2) },
                        label = { Text("2 days") },
                    )
                    FilterChip(
                        selected = state.rangeDays == 7,
                        onClick = { onPresetDays(7) },
                        label = { Text("1 week") },
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = state.customDaysText,
                        onValueChange = onCustomDaysChange,
                        label = { Text("Custom days") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onApplyCustom) { Text("Apply") }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
    )
}

@Composable
private fun SuggestedTasksDialog(
    tasks: List<GmailSummarySuggestedTask>,
    selected: Set<Int>,
    onToggle: (Int) -> Unit,
    onSelectAll: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "✨ Suggested tasks",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column {
                Text(
                    "Pick what to add to today’s task list. You can edit times in Tasks.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    val allOn = selected.size == tasks.size && tasks.isNotEmpty()
                    TextButton(onClick = { onSelectAll(!allOn) }) {
                        Text(if (allOn) "Clear" else "Select all")
                    }
                }
                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    itemsIndexed(tasks, key = { i, t -> "$i-${t.title}" }) { i, t ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = i in selected,
                                onCheckedChange = { onToggle(i) },
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    t.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (t.detail.isNotBlank()) {
                                    Text(
                                        t.detail,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = selected.isNotEmpty(),
            ) {
                Text("Add to task list")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
