package com.dailycurator.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dailycurator.data.local.CerebrasModelOption
import com.dailycurator.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val isDark by viewModel.isDarkTheme.collectAsState()
    val cerebrasKey by viewModel.cerebrasKey.collectAsState()
    val cerebrasModelId by viewModel.cerebrasModelId.collectAsState()
    var modelMenuExpanded by remember { mutableStateOf(false) }
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Text("Settings",
                style = MaterialTheme.typography.displayLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp))
        }
        item {
            SettingsGroup(title = "Account") {
                SettingsRow(icon = Icons.Default.Person, label = "Profile") {}
                SettingsRow(icon = Icons.Default.Notifications, label = "Notifications") {}
            }
        }
        item {
            Spacer(Modifier.height(16.dp))
            SettingsGroup(title = "AI Assistant") {
                SettingsRow(icon = Icons.Default.AutoAwesome, label = "AI Preferences") {}
                SettingsRow(icon = Icons.Default.Psychology, label = "Insight Frequency") {}
            }
        }
        item {
            Spacer(Modifier.height(16.dp))
            SettingsGroup(title = "Appearance") {
                // Dark Theme toggle row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.toggleDarkTheme() }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        if (isDark) "Dark Theme" else "Light Theme",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium),
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = isDark,
                        onCheckedChange = { viewModel.toggleDarkTheme() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }
        item {
            Spacer(Modifier.height(16.dp))
            SettingsGroup(title = "Cerebras API") {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("API Key", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = cerebrasKey,
                        onValueChange = { viewModel.onCerebrasKeyChange(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Enter your Cerebras key") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Model",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    ExposedDropdownMenuBox(
                        expanded = modelMenuExpanded,
                        onExpandedChange = { modelMenuExpanded = it }
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
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = modelMenuExpanded,
                            onDismissRequest = { modelMenuExpanded = false }
                        ) {
                            pickerOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.displayName) },
                                    onClick = {
                                        viewModel.onCerebrasModelSelected(option.modelId)
                                        modelMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.saveCerebrasKey() },
                        modifier = Modifier.align(Alignment.End),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Key")
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(16.dp))
            SettingsGroup(title = "Data") {
                SettingsRow(icon = Icons.Default.Sync, label = "Sync") {}
                SettingsRow(icon = Icons.Default.Info, label = "About") {}
            }
        }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text(title.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold))
        Spacer(Modifier.height(8.dp))
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(0.dp)
        ) { Column { content() } }
    }
}

@Composable
private fun SettingsRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium),
            modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
