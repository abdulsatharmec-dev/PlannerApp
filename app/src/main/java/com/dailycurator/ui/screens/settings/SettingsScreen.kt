package com.dailycurator.ui.screens.settings

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.net.Uri
import androidx.hilt.navigation.compose.hiltViewModel
import com.dailycurator.R
import com.dailycurator.data.ai.AiPromptDefaults
import com.dailycurator.data.local.ChatFontSizeCategory
import com.dailycurator.data.media.MorningPlaylistPref
import com.dailycurator.data.media.MorningVideoBucket
import com.dailycurator.data.gmail.GmailTokenResult
import com.dailycurator.ui.LocalGmailLinkActions
import com.dailycurator.ui.theme.AppBackgroundOption
import com.dailycurator.ui.theme.AppThemePalette
import com.dailycurator.ui.theme.appScreenBackground
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val sectionGap = 12.dp
private val horizontalPad = 20.dp

private sealed class AppLockPinSheet {
    data object None : AppLockPinSheet()
    data object Setup : AppLockPinSheet()
    data object Change : AppLockPinSheet()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val isDark by viewModel.isDarkTheme.collectAsState()
    val themePaletteId by viewModel.themePaletteId.collectAsState()
    val appBackgroundId by viewModel.appBackgroundId.collectAsState()
    val customWallpaperUri by viewModel.customWallpaperUri.collectAsState()
    val chatFontSizeCategoryId by viewModel.chatFontSizeCategoryId.collectAsState()
    val assistantInsightEnabled by viewModel.assistantInsightEnabled.collectAsState()
    val weeklyGoalsInsightEnabled by viewModel.weeklyGoalsInsightEnabled.collectAsState()
    val journalShareWithChat by viewModel.journalShareWithChat.collectAsState()
    val journalInAssistantInsight by viewModel.journalInAssistantInsight.collectAsState()
    val journalInWeeklyGoalsInsight by viewModel.journalInWeeklyGoalsInsight.collectAsState()
    val journalContextWindowDays by viewModel.journalContextWindowDays.collectAsState()
    val assistantInsightPrompt by viewModel.assistantInsightPrompt.collectAsState()
    val weeklyGoalsInsightPrompt by viewModel.weeklyGoalsInsightPrompt.collectAsState()
    val cerebrasKey by viewModel.cerebrasKey.collectAsState()
    val cerebrasModelId by viewModel.cerebrasModelId.collectAsState()
    val gmailAccounts by viewModel.gmailAccounts.collectAsState()
    val agentGmailReadEnabled by viewModel.agentGmailReadEnabled.collectAsState()
    val agentGmailSendEnabled by viewModel.agentGmailSendEnabled.collectAsState()
    val homeGmailSummaryEnabled by viewModel.homeGmailSummaryEnabled.collectAsState()
    val agentMemoryEnabled by viewModel.agentMemoryEnabled.collectAsState()
    val gmailMailboxSummaryPrompt by viewModel.gmailMailboxSummaryPrompt.collectAsState()
    val memoryExtractionPrompt by viewModel.memoryExtractionPrompt.collectAsState()
    val phoneUsageInChatAgent by viewModel.phoneUsageInChatAgent.collectAsState()
    val phoneUsageInAssistantInsight by viewModel.phoneUsageInAssistantInsight.collectAsState()
    val phoneUsageInWeeklyGoalsInsight by viewModel.phoneUsageInWeeklyGoalsInsight.collectAsState()
    val phoneUsageInsightPrompt by viewModel.phoneUsageInsightPrompt.collectAsState()
    val phoneUsageAiContextDays by viewModel.phoneUsageAiContextDays.collectAsState()
    val phoneUsageWeeklyInsightDays by viewModel.phoneUsageWeeklyInsightDays.collectAsState()
    val llmProfiles by viewModel.llmProfiles.collectAsState()
    val homeDailyPdfUri by viewModel.homeDailyPdfUri.collectAsState()
    val morningLocalVideoUris by viewModel.morningLocalVideoUris.collectAsState()
    val morningPlaylistEntries by viewModel.morningPlaylistEntries.collectAsState()
    val morningYoutubeBlock by viewModel.morningYoutubeLinesBlock.collectAsState()
    val morningSpiritualYoutubeBlock by viewModel.morningSpiritualYoutubeLinesBlock.collectAsState()
    val morningMotivationAutoplay by viewModel.morningMotivationAutoplay.collectAsState()
    val morningSpiritualLocalVideoUris by viewModel.morningSpiritualLocalVideoUris.collectAsState()
    val morningSpiritualPlaylistEntries by viewModel.morningSpiritualPlaylistEntries.collectAsState()
    val appLockEnabled by viewModel.appLockEnabled.collectAsState()
    val appLockAllowBiometric by viewModel.appLockAllowBiometric.collectAsState()
    val appLockPinConfigured by viewModel.appLockPinConfigured.collectAsState()

    var showLlmKeysDialog by remember { mutableStateOf(false) }
    var showAssistantPromptDialog by remember { mutableStateOf(false) }
    var showWeeklyPromptDialog by remember { mutableStateOf(false) }
    var showPhoneUsagePromptDialog by remember { mutableStateOf(false) }
    var showGmailSummaryPromptDialog by remember { mutableStateOf(false) }
    var showMemoryExtractionPromptDialog by remember { mutableStateOf(false) }
    var manualGmailAddress by rememberSaveable { mutableStateOf("") }
    var showMorningLocalVideoPicker by remember { mutableStateOf(false) }
    var showMorningPlaylistManager by remember { mutableStateOf(false) }
    var morningPlaylistToEdit by remember { mutableStateOf<MorningPlaylistPref?>(null) }
    var morningSettingsBucket by remember { mutableStateOf(MorningVideoBucket.MOTIVATION) }
    var morningPlaylistEditBucket by remember { mutableStateOf(MorningVideoBucket.MOTIVATION) }
    var appLockPinSheet by remember { mutableStateOf<AppLockPinSheet>(AppLockPinSheet.None) }

    val context = LocalContext.current
    val gmailLinkActions = LocalGmailLinkActions.current

    val pickWallpaperLauncher = rememberLauncherForActivityResult(
        contract = PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            } catch (_: SecurityException) {
                // Some providers still allow read without persistable permission.
            }
            viewModel.setCustomWallpaperUri(uri.toString())
        }
    }

    val pickHomePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            } catch (_: SecurityException) {
            }
            viewModel.setHomeDailyPdfUri(uri.toString())
        }
    }

    if (showLlmKeysDialog) {
        LlmApiKeysManageDialog(
            profiles = llmProfiles,
            legacyCerebrasKey = cerebrasKey,
            onLegacyCerebrasKeyChange = { viewModel.onCerebrasKeyChange(it) },
            legacyCerebrasModelId = cerebrasModelId,
            onLegacyCerebrasModelSelected = { viewModel.onCerebrasModelSelected(it) },
            onSaveLegacyCerebras = { viewModel.saveCerebrasKey() },
            onDismiss = { showLlmKeysDialog = false },
            onToggleProfileEnabled = { id, en -> viewModel.setLlmProfileEnabled(id, en) },
            onDeleteProfile = { viewModel.removeLlmProfile(it) },
            onUpsertProfile = { viewModel.upsertLlmProfile(it) },
        )
    }

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
    if (showGmailSummaryPromptDialog) {
        InsightPromptEditorDialog(
            title = "Gmail mailbox summary prompt",
            initialText = gmailMailboxSummaryPrompt,
            defaultText = AiPromptDefaults.GMAIL_MAILBOX_SUMMARY,
            onDismiss = { showGmailSummaryPromptDialog = false },
            onSave = {
                viewModel.persistGmailMailboxSummaryPrompt(it)
                showGmailSummaryPromptDialog = false
            },
        )
    }
    if (showMemoryExtractionPromptDialog) {
        InsightPromptEditorDialog(
            title = "Memory extraction prompt",
            initialText = memoryExtractionPrompt,
            defaultText = AiPromptDefaults.MEMORY_EXTRACTION,
            onDismiss = { showMemoryExtractionPromptDialog = false },
            onSave = {
                viewModel.persistMemoryExtractionPrompt(it)
                showMemoryExtractionPromptDialog = false
            },
        )
    }
    if (showMorningLocalVideoPicker) {
        MorningLocalVideosPickerDialog(
            bucket = morningSettingsBucket,
            savedUris = when (morningSettingsBucket) {
                MorningVideoBucket.MOTIVATION -> morningLocalVideoUris
                MorningVideoBucket.SPIRITUAL -> morningSpiritualLocalVideoUris
            },
            onDismiss = { showMorningLocalVideoPicker = false },
            onConfirm = { uris ->
                viewModel.setMorningLocalVideoUrisForBucket(morningSettingsBucket, uris)
                showMorningLocalVideoPicker = false
            },
            viewModel = viewModel,
        )
    }
    val morningPlaylistEditEntry = morningPlaylistToEdit
    if (morningPlaylistEditEntry != null) {
        MorningPlaylistVideosDialog(
            bucket = morningPlaylistEditBucket,
            entry = morningPlaylistEditEntry,
            onDismiss = { morningPlaylistToEdit = null },
            viewModel = viewModel,
        )
    }
    if (showMorningPlaylistManager) {
        MorningPlaylistsManageDialog(
            bucket = morningSettingsBucket,
            entries = when (morningSettingsBucket) {
                MorningVideoBucket.MOTIVATION -> morningPlaylistEntries
                MorningVideoBucket.SPIRITUAL -> morningSpiritualPlaylistEntries
            },
            onDismiss = { showMorningPlaylistManager = false },
            onEditPlaylist = {
                morningPlaylistEditBucket = morningSettingsBucket
                morningPlaylistToEdit = it
                showMorningPlaylistManager = false
            },
            viewModel = viewModel,
        )
    }
    when (val sheet = appLockPinSheet) {
        AppLockPinSheet.None -> Unit
        AppLockPinSheet.Setup -> {
            var pin by remember { mutableStateOf("") }
            var confirm by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { appLockPinSheet = AppLockPinSheet.None },
                title = { Text("Create app lock PIN") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "4–8 digits. You will enter this after the app has been in the background.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedTextField(
                            value = pin,
                            onValueChange = { if (it.length <= 8 && it.all { c -> c.isDigit() }) pin = it },
                            label = { Text("PIN") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = confirm,
                            onValueChange = { if (it.length <= 8 && it.all { c -> c.isDigit() }) confirm = it },
                            label = { Text("Confirm PIN") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (pin.length < 4 || pin != confirm) {
                                Toast.makeText(context, "PINs must match (4–8 digits).", Toast.LENGTH_SHORT).show()
                                return@TextButton
                            }
                            viewModel.setAppLockPin(pin)
                            viewModel.setAppLockEnabled(true)
                            appLockPinSheet = AppLockPinSheet.None
                        },
                    ) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { appLockPinSheet = AppLockPinSheet.None }) { Text("Cancel") }
                },
            )
        }
        AppLockPinSheet.Change -> {
            var oldPin by remember { mutableStateOf("") }
            var pin by remember { mutableStateOf("") }
            var confirm by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { appLockPinSheet = AppLockPinSheet.None },
                title = { Text("Change app lock PIN") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = oldPin,
                            onValueChange = { if (it.length <= 8 && it.all { c -> c.isDigit() }) oldPin = it },
                            label = { Text("Current PIN") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = pin,
                            onValueChange = { if (it.length <= 8 && it.all { c -> c.isDigit() }) pin = it },
                            label = { Text("New PIN") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = confirm,
                            onValueChange = { if (it.length <= 8 && it.all { c -> c.isDigit() }) confirm = it },
                            label = { Text("Confirm new PIN") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (!viewModel.verifyAppLockPin(oldPin)) {
                                Toast.makeText(context, "Current PIN is incorrect.", Toast.LENGTH_SHORT).show()
                                return@TextButton
                            }
                            if (pin.length < 4 || pin != confirm) {
                                Toast.makeText(context, "New PINs must match (4–8 digits).", Toast.LENGTH_SHORT).show()
                                return@TextButton
                            }
                            viewModel.setAppLockPin(pin)
                            appLockPinSheet = AppLockPinSheet.None
                        },
                    ) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { appLockPinSheet = AppLockPinSheet.None }) { Text("Cancel") }
                },
            )
        }
    }
    if (showPhoneUsagePromptDialog) {
        InsightPromptEditorDialog(
            title = "Phone usage insight prompt",
            initialText = phoneUsageInsightPrompt,
            defaultText = AiPromptDefaults.PHONE_USAGE_INSIGHT,
            onDismiss = { showPhoneUsagePromptDialog = false },
            onSave = {
                viewModel.persistPhoneUsageInsightPrompt(it)
                showPhoneUsagePromptDialog = false
            },
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .appScreenBackground(),
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
                Column(
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "Theme color",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Applies to light and dark mode. Violet matches the original look.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AppThemePalette.entries.forEach { p ->
                            val selected = p.storageId == themePaletteId
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.setThemePalette(p) },
                                label = { Text(p.displayLabel) },
                                leadingIcon = {
                                    ThemePaletteSwatch(
                                        p.previewPrimary(isDark),
                                        Modifier.size(18.dp),
                                    )
                                },
                            )
                        }
                    }
                    Text(
                        "Background",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Gradients and patterns sit behind your screens. Your own photo can be dark or colorful — a soft veil keeps lists readable.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AppBackgroundOption.entries.forEach { opt ->
                            FilterChip(
                                selected = opt.storageId == appBackgroundId,
                                onClick = { viewModel.setAppBackground(opt) },
                                label = { Text(opt.displayLabel) },
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Your photo",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Optional full-screen image from your gallery (combined with the style you picked above).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = {
                                pickWallpaperLauncher.launch(
                                    PickVisualMediaRequest(PickVisualMedia.ImageOnly),
                                )
                            },
                        ) {
                            Text("Choose from gallery")
                        }
                        if (customWallpaperUri.isNotBlank()) {
                            TextButton(onClick = { viewModel.clearCustomWallpaper() }) {
                                Text("Remove photo")
                            }
                        }
                    }
                }
            }
        }

        item {
            SettingsSection(title = "Chat appearance") {
                Column(
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "Text size",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Bubbles and composer use your theme colors. Size applies to messages, markdown, and the input field.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ChatFontSizeCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = cat.storageId == chatFontSizeCategoryId,
                                onClick = { viewModel.setChatFontSizeCategory(cat) },
                                label = { Text(cat.displayLabel) },
                            )
                        }
                    }
                }
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
            SettingsSection(title = "App lock") {
                Column(
                    Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        "When you leave DayRoute (home button or another app), the screen locks until you enter your PIN. " +
                            "Fingerprint or device credential can be offered if you allow it below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SettingsToggleRow(
                        icon = Icons.Default.Lock,
                        label = "Lock with PIN when you leave the app",
                        checked = appLockEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                if (appLockPinConfigured) viewModel.setAppLockEnabled(true)
                                else appLockPinSheet = AppLockPinSheet.Setup
                            } else {
                                viewModel.setAppLockEnabled(false)
                            }
                        },
                    )
                    if (appLockPinConfigured) {
                        OutlinedButton(
                            onClick = { appLockPinSheet = AppLockPinSheet.Change },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Change PIN")
                        }
                        SettingsToggleRow(
                            icon = null,
                            label = "Offer fingerprint or screen lock on the lock screen",
                            subtitle = "Uses the same biometrics you use to unlock your phone (when available).",
                            checked = appLockAllowBiometric,
                            onCheckedChange = { viewModel.setAppLockAllowBiometric(it) },
                        )
                    }
                }
            }
        }

        item {
            SettingsSection(title = "Home videos (Today tab)") {
                var youtubeDraft by rememberSaveable { mutableStateOf("") }
                LaunchedEffect(morningSettingsBucket, morningYoutubeBlock, morningSpiritualYoutubeBlock) {
                    youtubeDraft = when (morningSettingsBucket) {
                        MorningVideoBucket.MOTIVATION -> morningYoutubeBlock
                        MorningVideoBucket.SPIRITUAL -> morningSpiritualYoutubeBlock
                    }
                }

                Column(
                    Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        "Choose a category first, then add YouTube links, playlists, or device files for that shelf. " +
                            "On Today you can switch between Motivation and Spiritual.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = morningSettingsBucket == MorningVideoBucket.MOTIVATION,
                            onClick = { morningSettingsBucket = MorningVideoBucket.MOTIVATION },
                            label = { Text("Motivation") },
                        )
                        FilterChip(
                            selected = morningSettingsBucket == MorningVideoBucket.SPIRITUAL,
                            onClick = { morningSettingsBucket = MorningVideoBucket.SPIRITUAL },
                            label = { Text("Spiritual") },
                        )
                    }
                    SettingsToggleRow(
                        icon = Icons.Default.PlayCircle,
                        label = "Autoplay on Today tab",
                        subtitle = "Off by default. When on, the selected clip starts as soon as the player is ready.",
                        checked = morningMotivationAutoplay,
                        onCheckedChange = { viewModel.setMorningMotivationAutoplay(it) },
                    )
                    OutlinedTextField(
                        value = youtubeDraft,
                        onValueChange = { youtubeDraft = it },
                        label = {
                            Text(
                                if (morningSettingsBucket == MorningVideoBucket.SPIRITUAL) {
                                    "Spiritual — single YouTube videos (one URL or id per line)"
                                } else {
                                    "Motivation — single YouTube videos (one URL or id per line)"
                                },
                            )
                        },
                        minLines = 2,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = {
                            when (morningSettingsBucket) {
                                MorningVideoBucket.MOTIVATION -> viewModel.setMorningYoutubeLinesBlock(youtubeDraft)
                                MorningVideoBucket.SPIRITUAL ->
                                    viewModel.setMorningSpiritualYoutubeLinesBlock(youtubeDraft)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Save single-video list")
                    }
                    val playlistCount = when (morningSettingsBucket) {
                        MorningVideoBucket.MOTIVATION -> morningPlaylistEntries.size
                        MorningVideoBucket.SPIRITUAL -> morningSpiritualPlaylistEntries.size
                    }
                    val deviceCount = when (morningSettingsBucket) {
                        MorningVideoBucket.MOTIVATION -> morningLocalVideoUris.size
                        MorningVideoBucket.SPIRITUAL -> morningSpiritualLocalVideoUris.size
                    }
                    OutlinedButton(
                        onClick = { showMorningPlaylistManager = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.AddLink, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("YouTube playlists ($playlistCount)")
                    }
                    OutlinedButton(
                        onClick = { showMorningLocalVideoPicker = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.VideoLibrary, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Device videos ($deviceCount) — pick in list")
                    }
                }
            }
        }

        item {
            SettingsSection(title = "Daily PDF on Home") {
                Column(
                    Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        "Opens on the Today tab (section order can be changed from Today → ⋮ → Customize home layout). The last page you viewed is remembered.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = { pickHomePdfLauncher.launch(arrayOf("application/pdf")) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Choose PDF file")
                    }
                    if (homeDailyPdfUri.isNotBlank()) {
                        val short = Uri.parse(homeDailyPdfUri).lastPathSegment?.take(48) ?: homeDailyPdfUri.take(48)
                        Text(
                            "Current: $short",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedButton(
                            onClick = { viewModel.clearHomeDailyPdf() },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Clear PDF")
                        }
                    }
                }
            }
        }

        item {
            SettingsSection(title = "Language model (API)") {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        "Add Cerebras or Groq keys with models. When a key hits rate limits (HTTP 429 or 503), the next enabled key is used automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { showLlmKeysDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Manage API keys")
                    }
                }
            }
        }

        item {
            SettingsSection(title = "Journal & AI") {
                Text(
                    "Entries use the calendar day of their last update. Default is today only; widen the window below if you want more history. Each journal has toggles in the editor.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(1, 3, 7, 14, 30).forEach { d ->
                        FilterChip(
                            selected = journalContextWindowDays == d,
                            onClick = { viewModel.setJournalContextWindowDays(d) },
                            label = {
                                Text(
                                    if (d == 1) "Today" else "${d}d",
                                )
                            },
                        )
                    }
                }
                HorizontalDivider(Modifier.padding(start = 16.dp))
                SettingsToggleRow(
                    icon = Icons.Default.Book,
                    label = "Share journal with chat agent",
                    subtitle = "When on, eligible entries (window + per-entry) go to chat context",
                    checked = journalShareWithChat,
                    onCheckedChange = { viewModel.setJournalShareWithChat(it) },
                )
                HorizontalDivider(Modifier.padding(start = 16.dp))
                SettingsToggleRow(
                    icon = null,
                    label = "Journal in assistant insight",
                    subtitle = "Home assistant insight uses the same date window and per-entry flags",
                    checked = journalInAssistantInsight,
                    onCheckedChange = { viewModel.setJournalInAssistantInsight(it) },
                )
                HorizontalDivider(Modifier.padding(start = 16.dp))
                SettingsToggleRow(
                    icon = null,
                    label = "Journal in weekly goals insight",
                    subtitle = "Weekly coaching uses the same date window and per-entry flags",
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
            SettingsSection(title = "Phone usage & AI") {
                SettingsToggleRow(
                    icon = null,
                    label = "Phone usage in AI Agent",
                    subtitle = "Adds foreground totals, top apps, and session intervals to chat context. Requires usage access.",
                    checked = phoneUsageInChatAgent,
                    onCheckedChange = { viewModel.setPhoneUsageInChatAgent(it) },
                )
                HorizontalDivider(Modifier.padding(start = 16.dp))
                SettingsToggleRow(
                    icon = null,
                    label = "Phone usage in Assistant insight",
                    subtitle = "Home Assistant insight may include the same usage block when it generates",
                    checked = phoneUsageInAssistantInsight,
                    onCheckedChange = { viewModel.setPhoneUsageInAssistantInsight(it) },
                )
                HorizontalDivider(Modifier.padding(start = 16.dp))
                SettingsToggleRow(
                    icon = null,
                    label = "Phone usage in Weekly goals insight",
                    subtitle = "Weekly goals coaching may include a separate usage block when it generates",
                    checked = phoneUsageInWeeklyGoalsInsight,
                    onCheckedChange = { viewModel.setPhoneUsageInWeeklyGoalsInsight(it) },
                )
                HorizontalDivider(Modifier.padding(start = 16.dp))
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        "AI Agent & Assistant usage window",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Default 1 day (since local midnight). Use 2–14 for rolling 24h windows.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = phoneUsageAiContextDays.toString(),
                        onValueChange = { s ->
                            val digits = s.filter { it.isDigit() }.take(2)
                            val n = digits.toIntOrNull()
                            if (n != null && n in 1..14) viewModel.setPhoneUsageAiContextDays(n)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                    )
                }
                HorizontalDivider(Modifier.padding(start = 16.dp))
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        "Weekly goals insight usage window",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Default 7 days (rolling). Range 1–30.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = phoneUsageWeeklyInsightDays.toString(),
                        onValueChange = { s ->
                            val digits = s.filter { it.isDigit() }.take(2)
                            val n = digits.toIntOrNull()
                            if (n != null && n in 1..30) viewModel.setPhoneUsageWeeklyInsightDays(n)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                    )
                }
                HorizontalDivider(Modifier.padding(start = 16.dp))
                SettingsNavRow(
                    icon = Icons.Default.EditNote,
                    title = "Phone usage insight prompt",
                    subtitle = "System prompt for the Phone usage page summary · future generations only",
                    onClick = { showPhoneUsagePromptDialog = true },
                )
            }
        }

        item {
            SettingsSection(title = "Gmail & AI agent") {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        "Two ways to authorize: (1) Google Sign-In, or (2) save the address below, then “Grant Gmail API access” — that opens Google’s permission screen in the browser and often works when the Sign-In sheet fails. Cloud Console still needs an Android OAuth client (package + SHA-1), Gmail API enabled, and a Web client ID in default_web_client_id.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val actions = gmailLinkActions
                            if (actions != null) actions.linkGmail()
                            else Toast.makeText(context, "Gmail sign-in is not wired (restart the app).", Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.AddLink, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Link Gmail account")
                    }
                    Spacer(Modifier.height(6.dp))
                    TextButton(
                        onClick = {
                            val actions = gmailLinkActions
                            if (actions != null) actions.linkDifferentGoogleAccount()
                            else Toast.makeText(context, "Gmail sign-in is not wired (restart the app).", Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Use a different Google account (reset session)")
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val email = manualGmailAddress.trim().takeIf { it.contains('@') }
                                ?: gmailAccounts.firstOrNull()?.email
                            if (email.isNullOrBlank()) {
                                Toast.makeText(
                                    context,
                                    "Save a linked address first (or type it above).",
                                    Toast.LENGTH_LONG,
                                ).show()
                                return@Button
                            }
                            viewModel.startGmailAccessProbe(email) { r ->
                                when (r) {
                                    is GmailTokenResult.Ok ->
                                        Toast.makeText(
                                            context,
                                            "Gmail access OK for $email",
                                            Toast.LENGTH_LONG,
                                        ).show()
                                    is GmailTokenResult.NeedsUserInteraction ->
                                        context.startActivity(r.intent)
                                    is GmailTokenResult.Failure ->
                                        Toast.makeText(
                                            context,
                                            r.message,
                                            Toast.LENGTH_LONG,
                                        ).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Grant Gmail API access (browser)")
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = manualGmailAddress,
                        onValueChange = { manualGmailAddress = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Or add address manually") },
                        supportingText = {
                            Text(
                                "Same Google account must exist on this device; OAuth setup above still applies for API access.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            val e = manualGmailAddress.trim()
                            if (!e.contains('@')) {
                                Toast.makeText(context, "Enter a valid email address.", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.onGmailLinked(e)
                                manualGmailAddress = ""
                                Toast.makeText(context, "Saved: $e", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Save linked address")
                    }
                }
                gmailAccounts.forEach { acc ->
                    HorizontalDivider(Modifier.padding(start = 16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(acc.email, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Show in mailbox summary page",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = acc.showInSummary,
                            onCheckedChange = { viewModel.setGmailSummaryVisible(acc.email, it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    ) {
                        TextButton(onClick = { viewModel.removeGmailAccount(acc.email) }) {
                            Text("Remove", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                HorizontalDivider(Modifier.padding(start = 16.dp))
                SettingsToggleRow(
                    icon = Icons.Default.Email,
                    label = "Agent can read Gmail",
                    subtitle = "Exposes gmail_list_messages and gmail_get_message tools in chat",
                    checked = agentGmailReadEnabled,
                    onCheckedChange = { viewModel.setAgentGmailReadEnabled(it) },
                )
                HorizontalDivider(Modifier.padding(start = 16.dp))
                SettingsToggleRow(
                    icon = null,
                    label = "Agent can send Gmail",
                    subtitle = "Separate permission: gmail_send_email tool",
                    checked = agentGmailSendEnabled,
                    onCheckedChange = { viewModel.setAgentGmailSendEnabled(it) },
                )
            }
        }

        item {
            SettingsSection(title = "Gmail mailbox summary") {
                SettingsNavRow(
                    icon = Icons.Default.EditNote,
                    title = "Summarization system prompt",
                    subtitle = "Used on the Gmail Mailbox Summary page",
                    onClick = { showGmailSummaryPromptDialog = true },
                )
                HorizontalDivider(Modifier.padding(start = 16.dp))
                SettingsToggleRow(
                    icon = null,
                    label = "Show condensed summary on Home",
                    subtitle = "Uses the cached digest line from the last mailbox run",
                    checked = homeGmailSummaryEnabled,
                    onCheckedChange = { viewModel.setHomeGmailSummaryEnabled(it) },
                )
            }
        }

        item {
            SettingsSection(title = "Long-term memory") {
                SettingsToggleRow(
                    icon = null,
                    label = "Use memory in chat",
                    subtitle = "Injects stored memory into the agent context; disable to ignore",
                    checked = agentMemoryEnabled,
                    onCheckedChange = { viewModel.setAgentMemoryEnabled(it) },
                )
                HorizontalDivider(Modifier.padding(start = 16.dp))
                SettingsNavRow(
                    icon = Icons.Default.EditNote,
                    title = "Memory extraction prompt",
                    subtitle = "Used after chats and for planner-based updates",
                    onClick = { showMemoryExtractionPromptDialog = true },
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
                            stringResource(R.string.app_name),
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

@Composable
private fun ThemePaletteSwatch(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .background(color, RoundedCornerShape(4.dp)),
    )
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
