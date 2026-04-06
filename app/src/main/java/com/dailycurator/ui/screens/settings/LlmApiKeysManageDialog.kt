package com.dailycurator.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dailycurator.data.local.CEREBRAS_MODEL_OPTIONS
import com.dailycurator.data.local.CerebrasModelOption
import com.dailycurator.data.local.GROQ_MODEL_OPTIONS
import com.dailycurator.data.local.GroqModelOption
import com.dailycurator.data.local.LlmApiKeyProfile
import com.dailycurator.data.local.LlmProviderIds
import java.util.UUID

@Composable
fun LlmApiKeysManageDialog(
    profiles: List<LlmApiKeyProfile>,
    legacyCerebrasKey: String,
    onLegacyCerebrasKeyChange: (String) -> Unit,
    legacyCerebrasModelId: String,
    onLegacyCerebrasModelSelected: (String) -> Unit,
    onSaveLegacyCerebras: () -> Unit,
    onDismiss: () -> Unit,
    onToggleProfileEnabled: (id: String, enabled: Boolean) -> Unit,
    onDeleteProfile: (id: String) -> Unit,
    onUpsertProfile: (LlmApiKeyProfile) -> Unit,
) {
    var editorProfile by remember { mutableStateOf<LlmApiKeyProfile?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("LLM API keys") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    "Requests try each enabled key in order. On HTTP 429 or 503 the app switches to the next key automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))

                if (profiles.isEmpty()) {
                    Text(
                        "Legacy Cerebras (optional)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "Used only while you have no profiles below. Add profiles to use Groq or multiple keys.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = legacyCerebrasKey,
                        onValueChange = onLegacyCerebrasKeyChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Cerebras API key") },
                        singleLine = true,
                    )
                    Spacer(Modifier.height(8.dp))
                    LegacyCerebrasModelMenu(
                        modelId = legacyCerebrasModelId,
                        onModelSelected = onLegacyCerebrasModelSelected,
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onSaveLegacyCerebras,
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text("Save legacy key")
                    }
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))
                }

                profiles.forEach { p ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Switch(
                            checked = p.enabled,
                            onCheckedChange = { onToggleProfileEnabled(p.id, it) },
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                p.displayName.ifBlank { "(unnamed)" },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                "${providerLabel(p.providerId)} · ${p.modelId}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(onClick = { editorProfile = p }) {
                            Text("Edit")
                        }
                        IconButton(onClick = { onDeleteProfile(p.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                    HorizontalDivider()
                }

                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        editorProfile = LlmApiKeyProfile(
                            id = "",
                            displayName = "",
                            providerId = LlmProviderIds.CEREBRAS,
                            modelId = CEREBRAS_MODEL_OPTIONS.first().modelId,
                            apiKey = "",
                            enabled = true,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Add API key profile")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        },
    )

    editorProfile?.let { draft ->
        LlmProfileEditorDialog(
            initial = draft,
            onDismiss = { editorProfile = null },
            onSave = { saved ->
                onUpsertProfile(saved)
                editorProfile = null
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LegacyCerebrasModelMenu(
    modelId: String,
    onModelSelected: (String) -> Unit,
) {
    val catalog = CEREBRAS_MODEL_OPTIONS
    val pickerOptions = remember(modelId, catalog) {
        buildList {
            addAll(catalog)
            val known = catalog.map { it.modelId }.toSet()
            if (modelId.isNotBlank() && modelId !in known) {
                add(CerebrasModelOption("Other (saved id)", modelId))
            }
        }
    }
    val label = pickerOptions.find { it.modelId == modelId }?.displayName ?: modelId
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            readOnly = true,
            value = label,
            onValueChange = {},
            label = { Text("Cerebras model (legacy)") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            pickerOptions.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt.displayName) },
                    onClick = {
                        onModelSelected(opt.modelId)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LlmProfileEditorDialog(
    initial: LlmApiKeyProfile,
    onDismiss: () -> Unit,
    onSave: (LlmApiKeyProfile) -> Unit,
) {
    var name by remember(initial.id) { mutableStateOf(initial.displayName) }
    var provider by remember(initial.id) { mutableStateOf(initial.providerId) }
    var modelId by remember(initial.id) { mutableStateOf(initial.modelId) }
    var apiKey by remember(initial.id) { mutableStateOf(initial.apiKey) }
    var enabled by remember(initial.id) { mutableStateOf(initial.enabled) }

    val cerebrasModels = CEREBRAS_MODEL_OPTIONS
    val groqModels = GROQ_MODEL_OPTIONS

    fun ensureModelForProvider() {
        when (provider) {
            LlmProviderIds.CEREBRAS -> {
                if (cerebrasModels.none { it.modelId == modelId }) {
                    modelId = cerebrasModels.first().modelId
                }
            }
            LlmProviderIds.GROQ -> {
                if (groqModels.none { it.modelId == modelId }) {
                    modelId = groqModels.first().modelId
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id.isBlank()) "New API key" else "Edit API key") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                ProviderMenu(
                    providerId = provider,
                    onProviderSelected = {
                        provider = it
                        ensureModelForProvider()
                    },
                )
                Spacer(Modifier.height(8.dp))
                ModelMenu(
                    providerId = provider,
                    modelId = modelId,
                    cerebrasModels = cerebrasModels,
                    groqModels = groqModels,
                    onModelSelected = { modelId = it },
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API key") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Use this key (failover order)", modifier = Modifier.weight(1f))
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val id = initial.id.ifBlank { UUID.randomUUID().toString() }
                    onSave(
                        LlmApiKeyProfile(
                            id = id,
                            displayName = name.trim(),
                            providerId = provider,
                            modelId = modelId.trim(),
                            apiKey = apiKey.trim(),
                            enabled = enabled,
                        ),
                    )
                },
                enabled = apiKey.isNotBlank() && modelId.isNotBlank(),
            ) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderMenu(
    providerId: String,
    onProviderSelected: (String) -> Unit,
) {
    val options = listOf(
        LlmProviderIds.CEREBRAS to "Cerebras",
        LlmProviderIds.GROQ to "Groq",
    )
    val label = options.find { it.first == providerId }?.second ?: providerId
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            readOnly = true,
            value = label,
            onValueChange = {},
            label = { Text("Provider") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { (id, title) ->
                DropdownMenuItem(
                    text = { Text(title) },
                    onClick = {
                        onProviderSelected(id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelMenu(
    providerId: String,
    modelId: String,
    cerebrasModels: List<CerebrasModelOption>,
    groqModels: List<GroqModelOption>,
    onModelSelected: (String) -> Unit,
) {
    val options: List<Pair<String, String>> = when (providerId) {
        LlmProviderIds.GROQ -> groqModels.map { it.modelId to it.displayName }
        else -> cerebrasModels.map { it.modelId to it.displayName }
    }
    val label = options.find { it.first == modelId }?.second ?: modelId
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            readOnly = true,
            value = label,
            onValueChange = {},
            label = { Text("Model") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { (id, title) ->
                DropdownMenuItem(
                    text = { Text(title) },
                    onClick = {
                        onModelSelected(id)
                        expanded = false
                    },
                )
            }
        }
    }
}

private fun providerLabel(id: String): String = when (id) {
    LlmProviderIds.CEREBRAS -> "Cerebras"
    LlmProviderIds.GROQ -> "Groq"
    else -> id
}
