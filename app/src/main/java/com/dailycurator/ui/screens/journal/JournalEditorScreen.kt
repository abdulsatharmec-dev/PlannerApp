package com.dailycurator.ui.screens.journal

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.ContextCompat
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalEditorScreen(
    onBack: () -> Unit,
    viewModel: JournalEditorViewModel = hiltViewModel(),
) {
    val title by viewModel.title.collectAsState()
    val body by viewModel.body.collectAsState()
    val ready by viewModel.ready.collectAsState()
    val includeChat by viewModel.includeInAgentChat.collectAsState()
    val includeAssistant by viewModel.includeInAssistantInsight.collectAsState()
    val includeWeekly by viewModel.includeInWeeklyGoalsInsight.collectAsState()
    val isEvergreenEntry by viewModel.isEvergreen.collectAsState()
    val voicePath by viewModel.voiceRelativePath.collectAsState()
    val context = LocalContext.current
    var showVoiceRecordDialog by remember { mutableStateOf(false) }
    var editorVoicePlaying by remember { mutableStateOf(false) }
    val voicePlayer = remember { MediaPlayer() }

    val recordMicLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            showVoiceRecordDialog = true
        } else {
            Toast.makeText(
                context,
                "Microphone permission is required to record.",
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    fun requestOpenVoiceRecorder() {
        when {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED -> showVoiceRecordDialog = true
            else -> recordMicLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                if (voicePlayer.isPlaying) voicePlayer.stop()
                voicePlayer.reset()
                voicePlayer.release()
            } catch (_: Throwable) {
            }
        }
    }

    LaunchedEffect(voicePath) {
        try {
            if (voicePlayer.isPlaying) voicePlayer.stop()
            voicePlayer.reset()
        } catch (_: Throwable) {
        }
        editorVoicePlaying = false
        val p = voicePath?.trim().orEmpty()
        if (p.isNotEmpty()) {
            val f = JournalVoiceFiles.absoluteFile(context, p)
            if (f.exists()) {
                try {
                    voicePlayer.setDataSource(f.absolutePath)
                    voicePlayer.prepare()
                    voicePlayer.setOnCompletionListener {
                        editorVoicePlaying = false
                    }
                } catch (_: Throwable) {
                }
            }
        }
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showAiSettings by remember { mutableStateOf(false) }

    val gradient = diaryGradientBrush()
    val paper = diaryPaperColor()
    val ink = diaryInkColor()
    val dateLine = remember(viewModel.isNewEntry) {
        val fmt = DateTimeFormatter.ofPattern("EEEE, MMMM d")
        Instant.now().atZone(ZoneId.systemDefault()).toLocalDate().format(fmt)
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this entry?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.delete(onDone = onBack)
                    },
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }

    if (showAiSettings) {
        AlertDialog(
            onDismissRequest = { showAiSettings = false },
            title = { Text("AI sharing") },
            text = {
                Column {
                    Text(
                        "Matches Settings → Journal date window. Turn off to keep this entry out of that surface.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    RowSwitchRow(
                        checked = includeChat,
                        onCheckedChange = { viewModel.setIncludeInAgentChat(it) },
                        label = "Include in agent chat",
                    )
                    RowSwitchRow(
                        checked = includeAssistant,
                        onCheckedChange = { viewModel.setIncludeInAssistantInsight(it) },
                        label = "Include in assistant insight",
                    )
                    RowSwitchRow(
                        checked = includeWeekly,
                        onCheckedChange = { viewModel.setIncludeInWeeklyGoalsInsight(it) },
                        label = "Include in weekly goals insight",
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAiSettings = false }) { Text("Done") }
            },
        )
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                title = {
                    Text(
                        if (viewModel.isNewEntry) "Today" else "Entry",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!voicePath.isNullOrBlank()) {
                        IconButton(
                            onClick = {
                                try {
                                    if (editorVoicePlaying) {
                                        voicePlayer.pause()
                                        editorVoicePlaying = false
                                    } else {
                                        voicePlayer.start()
                                        editorVoicePlaying = true
                                    }
                                } catch (_: Throwable) {
                                    Toast.makeText(context, "Could not play recording.", Toast.LENGTH_SHORT).show()
                                }
                            },
                        ) {
                            Icon(
                                imageVector = if (editorVoicePlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (editorVoicePlaying) "Pause voice" else "Play voice",
                            )
                        }
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("AI sharing…") },
                                onClick = {
                                    menuExpanded = false
                                    showAiSettings = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Record voice…") },
                                onClick = {
                                    menuExpanded = false
                                    requestOpenVoiceRecorder()
                                },
                            )
                            if (!voicePath.isNullOrBlank()) {
                                DropdownMenuItem(
                                    text = { Text("Remove voice recording") },
                                    onClick = {
                                        menuExpanded = false
                                        try {
                                            if (voicePlayer.isPlaying) voicePlayer.stop()
                                            voicePlayer.reset()
                                        } catch (_: Throwable) {
                                        }
                                        editorVoicePlaying = false
                                        viewModel.clearVoiceAttachment()
                                    },
                                )
                            }
                            if (!viewModel.isNewEntry) {
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Delete entry", color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        menuExpanded = false
                                        showDeleteConfirm = true
                                    },
                                )
                            }
                        }
                    }
                    TextButton(
                        onClick = {
                            val hasVoiceAfterStop = !viewModel.voiceRelativePath.value.isNullOrBlank()
                            if (title.trim().isEmpty() && body.isBlank() && !hasVoiceAfterStop) {
                                validationError = true
                                return@TextButton
                            }
                            validationError = false
                            viewModel.save(onDone = onBack)
                        },
                    ) {
                        Text("Save", fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                ),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp, bottom = 8.dp),
            ) {
                Text(
                    dateLine,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = ink.copy(alpha = 0.65f),
                        letterSpacing = 0.8.sp,
                    ),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (viewModel.isNewEntry) "A quiet page for your thoughts." else "Continue your entry.",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = ink,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(Modifier.height(16.dp))
                if (!ready) {
                    Text("Loading…", color = ink.copy(alpha = 0.6f))
                } else {
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            viewModel.setTitle(it)
                            validationError = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Title (optional)") },
                        singleLine = true,
                        isError = validationError,
                        supportingText = {
                            if (validationError) {
                                Text("Add a title, write something, or attach a voice note.")
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = ink.copy(alpha = 0.22f),
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            focusedContainerColor = paper,
                            unfocusedContainerColor = paper,
                        ),
                    )
                    Spacer(Modifier.height(12.dp))
                    RowSwitchRow(
                        checked = isEvergreenEntry,
                        onCheckedChange = { viewModel.setEvergreen(it) },
                        label = "Show every day (affirmations & reminders)",
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = body,
                        onValueChange = {
                            viewModel.setBody(it)
                            validationError = false
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        label = { Text("Write here…") },
                        shape = RoundedCornerShape(20.dp),
                        minLines = 16,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = ink.copy(alpha = 0.22f),
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            focusedContainerColor = paper,
                            unfocusedContainerColor = paper,
                        ),
                    )
                }
            }
        }
    }

        if (showVoiceRecordDialog) {
            BackHandler { showVoiceRecordDialog = false }
            val yOffsetPx = with(LocalDensity.current) { 80.dp.roundToPx() }
            Popup(
                alignment = Alignment.TopCenter,
                offset = IntOffset(0, yOffsetPx),
                properties = PopupProperties(
                    focusable = false,
                    dismissOnBackPress = false,
                    clippingEnabled = true,
                ),
            ) {
                JournalVoiceRecordOverlay(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    onDismissRequest = { showVoiceRecordDialog = false },
                    onSaved = { rel -> viewModel.replaceVoiceAttachment(rel) },
                )
            }
        }
    }
}

@Composable
private fun RowSwitchRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Switch(checked = checked, onCheckedChange = onCheckedChange)
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun diaryGradientBrush(): Brush {
    val scheme = MaterialTheme.colorScheme
    val top = scheme.primaryContainer.copy(alpha = 0.5f)
    val mid = scheme.tertiaryContainer.copy(alpha = 0.42f)
    val bottom = scheme.secondaryContainer.copy(alpha = 0.48f)
    return Brush.verticalGradient(listOf(top, mid, bottom))
}

@Composable
private fun diaryPaperColor(): Color {
    val scheme = MaterialTheme.colorScheme
    return scheme.surface.copy(alpha = 0.96f)
}

@Composable
private fun diaryInkColor(): Color = MaterialTheme.colorScheme.onSurface
