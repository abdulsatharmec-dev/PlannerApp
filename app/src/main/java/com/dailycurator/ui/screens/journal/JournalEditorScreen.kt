package com.dailycurator.ui.screens.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalEditorScreen(
    onBack: () -> Unit,
    viewModel: JournalEditorViewModel = hiltViewModel(),
) {
    val title by viewModel.title.collectAsState()
    val body by viewModel.body.collectAsState()
    val ready by viewModel.ready.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf(false) }

    val gradient = diaryGradientBrush()
    val paper = diaryPaperColor()
    val ink = diaryInkColor()

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

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (viewModel.isNewEntry) "New entry" else "Edit entry",
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!viewModel.isNewEntry) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    TextButton(
                        onClick = {
                            if (title.trim().isEmpty() && body.isBlank()) {
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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Text(
                    "✦ Your space",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = ink.copy(alpha = 0.75f),
                        letterSpacing = 1.2.sp,
                    ),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (viewModel.isNewEntry) "Write freely — this page is yours." else "Keep shaping your story.",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = ink,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(Modifier.height(20.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = paper),
                    elevation = CardDefaults.cardElevation(2.dp),
                ) {
                    Column(Modifier.padding(18.dp)) {
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
                                        Text("Add a title or write something in your entry.")
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = diaryAccent(),
                                    unfocusedBorderColor = ink.copy(alpha = 0.2f),
                                    focusedLabelColor = diaryAccent(),
                                ),
                            )
                            Spacer(Modifier.height(14.dp))
                            OutlinedTextField(
                                value = body,
                                onValueChange = {
                                    viewModel.setBody(it)
                                    validationError = false
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(320.dp),
                                label = { Text("Dear diary…") },
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = diaryAccent(),
                                    unfocusedBorderColor = ink.copy(alpha = 0.2f),
                                    focusedLabelColor = diaryAccent(),
                                ),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun diaryGradientBrush(): Brush {
    val scheme = MaterialTheme.colorScheme
    val top = scheme.primaryContainer.copy(alpha = 0.55f)
    val mid = scheme.tertiaryContainer.copy(alpha = 0.45f)
    val bottom = scheme.secondaryContainer.copy(alpha = 0.5f)
    return Brush.verticalGradient(listOf(top, mid, bottom))
}

@Composable
private fun diaryPaperColor(): Color {
    val scheme = MaterialTheme.colorScheme
    return scheme.surface.copy(alpha = 0.94f)
}

@Composable
private fun diaryInkColor(): Color = MaterialTheme.colorScheme.onSurface

@Composable
private fun diaryAccent(): Color = MaterialTheme.colorScheme.primary
