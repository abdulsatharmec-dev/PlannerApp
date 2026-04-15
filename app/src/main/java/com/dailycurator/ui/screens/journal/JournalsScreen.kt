package com.dailycurator.ui.screens.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dailycurator.data.model.JournalEntry
import com.dailycurator.ui.components.PickDateDialog
import com.dailycurator.ui.theme.backdropShowsThrough
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

private val dateTimeFmt: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE, MMM d · h:mm a").withZone(ZoneId.systemDefault())

private val diaryAccentStripes = listOf(
    listOf(Color(0xFFFF8A80), Color(0xFFFFB74D)),
    listOf(Color(0xFF80DEEA), Color(0xFF4FC3F7)),
    listOf(Color(0xFFCE93D8), Color(0xFF7986CB)),
    listOf(Color(0xFFA5D6A7), Color(0xFF69F0AE)),
    listOf(Color(0xFFFFE082), Color(0xFFFFAB91)),
)

@Composable
fun JournalsScreen(
    onCreateEntry: () -> Unit,
    onOpenEntry: (Long) -> Unit,
    viewModel: JournalsViewModel = hiltViewModel(),
) {
    val listRows by viewModel.listRows.collectAsState()
    val filterDate by viewModel.filterDate.collectAsState()
    val evergreenOnly by viewModel.evergreenOnly.collectAsState()
    val listVoicePlayback by viewModel.listVoicePlayback.collectAsState()
    var pendingDelete by remember { mutableStateOf<JournalEntry?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    PickDateDialog(
        visible = showDatePicker,
        initialDate = filterDate ?: LocalDate.now(),
        onDismiss = { showDatePicker = false },
        onConfirm = {
            viewModel.setFilterDate(it)
            showDatePicker = false
        },
    )

    pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete journal?") },
            text = { Text("This entry will be removed permanently.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.delete(entry)
                        pendingDelete = null
                    },
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
        )
    }

    val decorOn = backdropShowsThrough()
    val mid = if (!decorOn) MaterialTheme.colorScheme.background else Color.Transparent
    val topA = if (!decorOn) 0.35f else 0.22f
    val botA = if (!decorOn) 0.2f else 0.12f
    val dayFilterLabel = filterDate?.format(DateTimeFormatter.ofPattern("MMM d"))
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = topA),
                        mid,
                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = botA),
                    ),
                ),
            )
            .padding(bottom = 88.dp),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text(
                    "MY DIARY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                Text(
                    "Journal",
                    style = MaterialTheme.typography.displaySmall.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Text(
                    "Group by day, filter by date, or pin entries so they stay visible every day.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilterChip(
                        selected = filterDate == null && !evergreenOnly,
                        onClick = { viewModel.clearFilters() },
                        label = { Text("All") },
                    )
                    FilterChip(
                        selected = evergreenOnly,
                        onClick = {
                            if (evergreenOnly) viewModel.clearFilters() else viewModel.setEvergreenOnly(true)
                        },
                        label = { Text("Every day") },
                    )
                    if (filterDate != null) {
                        FilterChip(
                            selected = true,
                            onClick = { showDatePicker = true },
                            label = { Text(dayFilterLabel ?: "Date") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    modifier = Modifier.height(18.dp),
                                )
                            },
                        )
                    } else {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = "Filter by date",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
            if (listRows.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        ),
                        elevation = CardDefaults.cardElevation(0.dp),
                    ) {
                        Text(
                            when {
                                evergreenOnly -> "No entries are pinned for every day yet. Open an entry and turn on “Every day”, or tap the tag on a card."
                                filterDate != null -> "Nothing written on this date."
                                else -> "Your pages are empty. Tap + to start."
                            },
                            modifier = Modifier.padding(20.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            items(
                listRows,
                key = { row ->
                    when (row) {
                        is JournalListRow.SectionHeader -> "h:${row.title}"
                        is JournalListRow.EntryRow -> row.entry.id
                    }
                },
            ) { row ->
                when (row) {
                    is JournalListRow.SectionHeader -> {
                        Text(
                            row.title.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                            ),
                            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                        )
                    }
                    is JournalListRow.EntryRow -> {
                        val stripeIndex = (abs(row.entry.id) % diaryAccentStripes.size).toInt()
                        JournalListCard(
                            entry = row.entry,
                            stripeColors = diaryAccentStripes[stripeIndex],
                            onClick = { onOpenEntry(row.entry.id) },
                            onDelete = { pendingDelete = row.entry },
                            onToggleEvergreen = { viewModel.toggleEvergreen(row.entry) },
                            voicePlaying = listVoicePlayback.entryId == row.entry.id &&
                                listVoicePlayback.playing,
                            onVoiceClick = { viewModel.toggleListVoicePlayback(row.entry) },
                        )
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = onCreateEntry,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            shape = RoundedCornerShape(18.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Icon(Icons.Default.Add, contentDescription = "New journal entry")
        }
    }
}

@Composable
private fun JournalListCard(
    entry: JournalEntry,
    stripeColors: List<Color>,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onToggleEvergreen: () -> Unit,
    voicePlaying: Boolean,
    onVoiceClick: () -> Unit,
) {
    val hasVoice = !entry.voiceRelativePath.isNullOrBlank()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        ),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .fillMaxHeight()
                    .background(Brush.verticalGradient(stripeColors)),
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onClick),
                    ) {
                        Text(
                            entry.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            dateTimeFmt.format(Instant.ofEpochMilli(entry.updatedAtEpochMillis)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        if (entry.body.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                entry.body,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    FilterChip(
                        selected = entry.isEvergreen,
                        onClick = onToggleEvergreen,
                        label = { Text("Every day") },
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (hasVoice) {
                        IconButton(onClick = onVoiceClick) {
                            Icon(
                                imageVector = if (voicePlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (voicePlaying) "Pause voice note" else "Play voice note",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
