package com.dailycurator.ui.screens.gmail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dailycurator.ui.components.MarkdownSummaryBody
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun GmailMailboxSummaryScreen(viewModel: GmailMailboxSummaryViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val accounts by viewModel.linkedAccounts.collectAsState()
    val timeFmt = remember { DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            "Mailbox summary",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        )
        Text(
            "Scans Inbox and Spam for the selected range, then uses your Cerebras model with the prompt from Settings.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))

        if (accounts.isEmpty()) {
            Text(
                "Link Gmail in Settings, enable visibility per account, then return here.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
            return
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            FilterChip(
                selected = state.rangeDays == 1,
                onClick = { viewModel.setPresetRangeDays(1) },
                label = { Text("1 day") },
            )
            FilterChip(
                selected = state.rangeDays == 2,
                onClick = { viewModel.setPresetRangeDays(2) },
                label = { Text("2 days") },
            )
            FilterChip(
                selected = state.rangeDays == 7,
                onClick = { viewModel.setPresetRangeDays(7) },
                label = { Text("1 week") },
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = state.customDaysText,
                onValueChange = viewModel::setCustomDaysText,
                label = { Text("Custom days") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = { viewModel.applyCustomRange() }) { Text("Apply") }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Button(
                onClick = { viewModel.regenerate() },
                enabled = !state.loading,
                modifier = Modifier.weight(1f),
            ) {
                if (state.loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Generate summary")
                }
            }
        }
        state.error?.let { err ->
            Spacer(Modifier.height(8.dp))
            Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        state.generatedAtMillis?.let { ms ->
            Spacer(Modifier.height(8.dp))
            val z = ZoneId.systemDefault()
            val label = Instant.ofEpochMilli(ms).atZone(z).format(timeFmt)
            Text("Last generated: $label", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(16.dp))
        MarkdownSummaryBody(markdown = state.markdown)
        Spacer(Modifier.height(32.dp))
    }
}
