package com.dailycurator.ui.screens.settings

import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.dailycurator.data.media.MorningPlaylistPref
import com.dailycurator.data.media.MorningVideoBucket

private fun parseMediaIdFromContentUri(uriString: String): Long? =
    uriString.trim().trimEnd('/').substringAfterLast('/').toLongOrNull()

private fun formatDurationMs(ms: Long?): String {
    if (ms == null || ms <= 0L) return ""
    val totalSec = ms / 1000L
    val m = totalSec / 60L
    val s = totalSec % 60L
    return if (m > 0L) "${m}m ${s}s" else "${s}s"
}

@Composable
fun MorningLocalVideosPickerDialog(
    bucket: MorningVideoBucket,
    savedUris: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit,
    viewModel: SettingsViewModel,
) {
    val context = LocalContext.current
    val perm = viewModel.videoReadPermission()
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { ok ->
        granted = ok
        if (!ok) {
            Toast.makeText(
                context,
                "Allow video access to list files on this device.",
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!granted) permLauncher.launch(perm)
    }

    var loading by remember { mutableStateOf(false) }
    var rows by remember { mutableStateOf<List<DeviceVideoListItem>>(emptyList()) }
    var selectedIds by remember(savedUris) {
        mutableStateOf(
            savedUris.mapNotNull { parseMediaIdFromContentUri(it) }.toMutableSet(),
        )
    }

    LaunchedEffect(granted) {
        if (!granted) return@LaunchedEffect
        loading = true
        rows = viewModel.queryDeviceVideosForPicker()
        loading = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
        ) {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Text(
                    "Videos on this device (${if (bucket == MorningVideoBucket.SPIRITUAL) "Spiritual" else "Motivation"})",
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Check the clips to include on Today. Nothing is shown on the main settings screen until you open this list again.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                if (!granted) {
                    Text(
                        "Storage permission is needed to read your video library.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { permLauncher.launch(perm) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Grant permission")
                    }
                } else if (loading) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    ) {
                        items(rows, key = { it.id }) { item ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = item.id in selectedIds,
                                    onCheckedChange = { checked ->
                                        selectedIds = selectedIds.toMutableSet().apply {
                                            if (checked) add(item.id) else remove(item.id)
                                        }
                                    },
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(item.displayName, style = MaterialTheme.typography.bodyMedium)
                                    val d = formatDurationMs(item.durationMs)
                                    if (d.isNotEmpty()) {
                                        Text(
                                            d,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(
                        onClick = {
                            val uris = selectedIds.map { viewModel.contentUriStringForVideoMediaId(it) }.distinct()
                            onConfirm(uris)
                        },
                        enabled = granted && !loading,
                    ) { Text("Save") }
                }
            }
        }
    }
}

@Composable
fun MorningPlaylistVideosDialog(
    bucket: MorningVideoBucket,
    entry: MorningPlaylistPref,
    onDismiss: () -> Unit,
    viewModel: SettingsViewModel,
) {
    var loading by remember { mutableStateOf(true) }
    var ids by remember { mutableStateOf<List<String>>(emptyList()) }
    var excluded by remember(entry.playlistId) {
        mutableStateOf(entry.excludedVideoIds.toSet())
    }

    LaunchedEffect(entry.playlistId) {
        loading = true
        ids = viewModel.fetchPlaylistVideoIdsForEditor(entry.playlistId)
        loading = false
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
        ) {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Text("Playlist videos", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    entry.playlistId,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Remove hides a clip from Today (you can add it back later).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                if (loading) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) { CircularProgressIndicator() }
                } else {
                    LazyColumn(Modifier.weight(1f)) {
                        items(ids, key = { it }) { vid ->
                            val isExcluded = vid in excluded
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        if (isExcluded) "$vid  (hidden)"
                                        else vid,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isExcluded) {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        excluded = excluded.toMutableSet().apply {
                                            if (isExcluded) remove(vid) else add(vid)
                                        }
                                    },
                                ) {
                                    Text(if (isExcluded) "Restore" else "Remove")
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        viewModel.removeMorningPlaylist(entry.playlistId, bucket)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Remove entire playlist")
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.padding(4.dp))
                    Button(
                        onClick = {
                            viewModel.updateMorningPlaylistExcluded(
                                entry.playlistId,
                                excluded.toList(),
                                bucket,
                            )
                            onDismiss()
                        },
                    ) { Text("Save") }
                }
            }
        }
    }
}

@Composable
fun MorningPlaylistsManageDialog(
    bucket: MorningVideoBucket,
    entries: List<MorningPlaylistPref>,
    onDismiss: () -> Unit,
    onEditPlaylist: (MorningPlaylistPref) -> Unit,
    viewModel: SettingsViewModel,
) {
    val context = LocalContext.current
    var newUrl by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.72f),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
        ) {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Text(
                    "YouTube playlists (${if (bucket == MorningVideoBucket.SPIRITUAL) "Spiritual" else "Motivation"})",
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Paste any watch link that includes list= (one video from the playlist is enough).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = newUrl,
                    onValueChange = { newUrl = it },
                    label = { Text("Watch URL with playlist") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    minLines = 2,
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        val ok = viewModel.addMorningPlaylistFromWatchUrl(newUrl, bucket)
                        if (ok) {
                            newUrl = ""
                            Toast.makeText(context, "Playlist added", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(
                                context,
                                "Need a valid YouTube watch URL with list=, or playlist already added.",
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Add playlist") }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Saved playlists",
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(4.dp))
                LazyColumn(Modifier.weight(1f)) {
                    items(entries, key = { it.playlistId }) { pl ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    pl.playlistId.take(36) + if (pl.playlistId.length > 36) "…" else "",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                if (pl.excludedVideoIds.isNotEmpty()) {
                                    Text(
                                        "${pl.excludedVideoIds.size} hidden clip(s)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            TextButton(onClick = { onEditPlaylist(pl) }) { Text("Edit") }
                            IconButton(
                                onClick = { viewModel.removeMorningPlaylist(pl.playlistId, bucket) },
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove playlist")
                            }
                        }
                        HorizontalDivider()
                    }
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                ) { Text("Done") }
            }
        }
    }
}
