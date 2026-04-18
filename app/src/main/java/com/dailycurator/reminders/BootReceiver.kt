package com.dailycurator.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dailycurator.pomodoro.AppNotificationChannels
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var taskReminderScheduler: TaskReminderScheduler
    @Inject lateinit var habitReminderScheduler: HabitReminderScheduler
    @Inject lateinit var notificationChannels: AppNotificationChannels

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                notificationChannels.ensureAll()
                taskReminderScheduler.rescheduleAllUndone()
                habitReminderScheduler.scheduleDaily()
            } finally {
                pending.finish()
            }
        }
    }
}
