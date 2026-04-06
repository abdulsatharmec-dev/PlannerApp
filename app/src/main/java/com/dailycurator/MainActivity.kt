package com.dailycurator

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.dailycurator.data.gmail.buildGoogleSignInClient
import com.dailycurator.data.gmail.processGmailSignInActivityResult
import com.dailycurator.data.gmail.tryGmailSilentLinkFromLastAccount
import com.dailycurator.data.local.AppPreferences
import com.dailycurator.pomodoro.AppNotificationChannels
import com.dailycurator.reminders.HabitReminderScheduler
import com.dailycurator.reminders.TaskReminderScheduler
import com.dailycurator.ui.GmailLinkActions
import com.dailycurator.ui.LocalGmailLinkActions
import com.dailycurator.ui.navigation.AppNavHost
import com.dailycurator.ui.reminders.TaskReminderBottomSheet
import com.dailycurator.ui.theme.AppBackgroundOption
import com.dailycurator.ui.theme.AppDecorBackdrop
import com.dailycurator.ui.theme.AppThemePalette
import com.dailycurator.ui.theme.DailyCuratorTheme
import com.dailycurator.ui.theme.LocalAppBackgroundOption
import com.dailycurator.ui.theme.LocalCustomWallpaperUri
import com.dailycurator.ui.theme.WallpaperReadableScrim
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainNavIncoming(
    val openPomodoro: Boolean = false,
    val openHabits: Boolean = false,
    val taskReminderSheetId: Long? = null,
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val googleSignInClient by lazy { buildGoogleSignInClient(this) }

    private lateinit var gmailSignInLauncher: ActivityResultLauncher<Intent>

    @Inject lateinit var prefs: AppPreferences
    @Inject lateinit var notificationChannels: AppNotificationChannels
    @Inject lateinit var taskReminderScheduler: TaskReminderScheduler
    @Inject lateinit var habitReminderScheduler: HabitReminderScheduler

    private val incomingState = mutableStateOf(MainNavIncoming())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gmailSignInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            processGmailSignInActivityResult(this, result, prefs)
            lifecycleScope.launch {
                delay(450)
                tryGmailSilentLinkFromLastAccount(this@MainActivity, prefs)
            }
        }
        notificationChannels.ensureAll()
        mergeIntent(intent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        }
        lifecycleScope.launch {
            taskReminderScheduler.rescheduleAllUndone()
            habitReminderScheduler.scheduleDaily()
        }
        enableEdgeToEdge()
        setContent {
            val incoming by incomingState
            val isDark by prefs.darkThemeFlow.collectAsState(initial = prefs.isDarkTheme())
            val paletteId by prefs.themePaletteIdFlow.collectAsState(initial = prefs.getThemePaletteId())
            val bgId by prefs.appBackgroundIdFlow.collectAsState(initial = prefs.getAppBackgroundId())
            val palette = remember(paletteId) { AppThemePalette.fromStorageId(paletteId) }
            val backgroundOption = remember(bgId) { AppBackgroundOption.fromStorageId(bgId) }
            val customWallpaperUri by prefs.customWallpaperUriFlow.collectAsState(
                initial = prefs.getCustomWallpaperUri(),
            )
            val wallpaperActive =
                backgroundOption != AppBackgroundOption.NONE || customWallpaperUri.isNotBlank()
            DailyCuratorTheme(darkTheme = isDark, palette = palette) {
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                    AppDecorBackdrop(backgroundOption, customWallpaperUri)
                    CompositionLocalProvider(
                        LocalAppBackgroundOption provides backgroundOption,
                    ) {
                        CompositionLocalProvider(
                            LocalCustomWallpaperUri provides customWallpaperUri,
                        ) {
                        CompositionLocalProvider(
                            LocalGmailLinkActions provides GmailLinkActions(
                                linkGmail = {
                                    gmailSignInLauncher.launch(googleSignInClient.signInIntent)
                                },
                                linkDifferentGoogleAccount = {
                                    googleSignInClient.signOut().addOnCompleteListener {
                                        gmailSignInLauncher.launch(googleSignInClient.signInIntent)
                                    }
                                },
                            ),
                        ) {
                            Box(Modifier.fillMaxSize()) {
                                WallpaperReadableScrim(
                                    wallpaperActive = wallpaperActive,
                                    hasCustomPhoto = customWallpaperUri.isNotBlank(),
                                )
                                AppNavHost(
                                    openPomodoroRequest = incoming.openPomodoro,
                                    openHabitsRequest = incoming.openHabits,
                                    onConsumedOpenPomodoro = {
                                        incomingState.value = incomingState.value.copy(openPomodoro = false)
                                    },
                                    onConsumedOpenHabits = {
                                        incomingState.value = incomingState.value.copy(openHabits = false)
                                    },
                                )
                                incoming.taskReminderSheetId?.let { tid ->
                                    TaskReminderBottomSheet(
                                        taskId = tid,
                                        onDismiss = {
                                            incomingState.value =
                                                incomingState.value.copy(taskReminderSheetId = null)
                                        },
                                        onNavigateToPomodoro = {
                                            incomingState.value = incomingState.value.copy(
                                                taskReminderSheetId = null,
                                                openPomodoro = true,
                                            )
                                        },
                                    )
                                }
                            }
                        }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        mergeIntent(intent)
    }

    private fun mergeIntent(i: Intent?) {
        if (i == null) return
        var n = incomingState.value
        if (i.getBooleanExtra(EXTRA_OPEN_POMODORO, false)) {
            n = n.copy(openPomodoro = true)
        }
        if (i.getBooleanExtra(EXTRA_OPEN_HABITS, false)) {
            n = n.copy(openHabits = true)
        }
        val tid = i.getLongExtra(EXTRA_TASK_REMINDER_SHEET_ID, -1L)
        if (tid > 0) {
            n = n.copy(taskReminderSheetId = tid)
        }
        incomingState.value = n
    }

    companion object {
        const val EXTRA_OPEN_POMODORO = "com.dailycurator.OPEN_POMODORO"
        const val EXTRA_OPEN_HABITS = "com.dailycurator.OPEN_HABITS"
        const val EXTRA_TASK_REMINDER_SHEET_ID = "com.dailycurator.TASK_REMINDER_SHEET_ID"
    }
}
