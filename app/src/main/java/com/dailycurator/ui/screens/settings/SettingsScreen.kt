package com.dailycurator.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dailycurator.ui.theme.*

@Composable
fun SettingsScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Text("Settings",
                style = MaterialTheme.typography.displayLarge.copy(color = TextPrimary),
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
                SettingsRow(icon = Icons.Default.Palette, label = "Theme") {}
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
                color = TextSecondary, fontWeight = FontWeight.SemiBold))
        Spacer(Modifier.height(8.dp))
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Surface),
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
                .background(InsightBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge.copy(
            color = TextPrimary, fontWeight = FontWeight.Medium),
            modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextTertiary)
    }
}
