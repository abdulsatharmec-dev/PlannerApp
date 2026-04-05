package com.dailycurator

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.dailycurator.data.local.AppPreferences
import com.dailycurator.pomodoro.AppNotificationChannels
import com.dailycurator.reminders.HabitReminderScheduler
import com.dailycurator.reminders.TaskReminderScheduler
import com.dailycurator.ui.navigation.AppNavHost
import com.dailycurator.ui.reminders.TaskReminderBottomSheet
import com.dailycurator.ui.theme.DailyCuratorTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainNavIncoming(
    val openPomodoro: Boolean = false,
    val openHabits: Boolean = false,
    val taskReminderSheetId: Long? = null,
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var prefs: AppPreferences
    @Inject lateinit var notificationChannels: AppNotificationChannels
    @Inject lateinit var taskReminderScheduler: TaskReminderScheduler
    @Inject lateinit var habitReminderScheduler: HabitReminderScheduler

    private val incomingState = mutableStateOf(MainNavIncoming())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            DailyCuratorTheme(darkTheme = isDark) {
                Box(Modifier.fillMaxSize()) {
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
                                incomingState.value = incomingState.value.copy(taskReminderSheetId = null)
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
