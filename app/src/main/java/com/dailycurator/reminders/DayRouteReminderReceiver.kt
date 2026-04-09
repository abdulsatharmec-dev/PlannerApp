package com.dailycurator.reminders

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dailycurator.MainActivity
import com.dailycurator.data.pomodoro.PomodoroLaunchRequest
import com.dailycurator.data.pomodoro.PomodoroNavBridge
import com.dailycurator.data.repository.TaskRepository
import com.dailycurator.pomodoro.ReminderNotificationIds
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DayRouteReminderReceiver : BroadcastReceiver() {

    @Inject lateinit var taskRepo: TaskRepository
    @Inject lateinit var pomodoroNavBridge: PomodoroNavBridge
    @Inject lateinit var taskReminderScheduler: TaskReminderScheduler
    @Inject lateinit var habitReminderScheduler: HabitReminderScheduler

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val pending = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                when (action) {
                    ReminderIntents.ACTION_TASK_ALARM -> {
                        val id = intent.getLongExtra(ReminderIntents.EXTRA_TASK_ID, -1L)
                        if (id > 0) showTaskNotification(context, id)
                    }
                    ReminderIntents.ACTION_TASK_DONE -> {
                        val id = intent.getLongExtra(ReminderIntents.EXTRA_TASK_ID, -1L)
                        if (id > 0) {
                            taskRepo.setDoneById(id, true)
                            NotificationManagerCompat.from(context).cancel(taskNotifId(id))
                            taskReminderScheduler.cancel(id)
                        }
                    }
                    ReminderIntents.ACTION_TASK_POMODORO -> {
                        val id = intent.getLongExtra(ReminderIntents.EXTRA_TASK_ID, -1L)
                        if (id > 0) {
                            val t = taskRepo.getById(id)
                            if (t != null) {
                                pomodoroNavBridge.push(
                                    PomodoroLaunchRequest(
                                        entityType = PomodoroLaunchRequest.TYPE_TASK,
                                        entityId = t.id,
                                        title = t.title,
                                    ),
                                )
                            }
                            NotificationManagerCompat.from(context).cancel(taskNotifId(id))
                        }
                    }
                    ReminderIntents.ACTION_TASK_DISMISS -> {
                        val id = intent.getLongExtra(ReminderIntents.EXTRA_TASK_ID, -1L)
                        if (id > 0) {
                            NotificationManagerCompat.from(context).cancel(taskNotifId(id))
                        }
                    }
                    ReminderIntents.ACTION_HABIT_ALARM -> showHabitNotification(context)
                    ReminderIntents.ACTION_HABIT_OPEN -> {
                        context.startActivity(
                            Intent(context, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                putExtra(MainActivity.EXTRA_OPEN_HABITS, true)
                            },
                        )
                        NotificationManagerCompat.from(context).cancel(ReminderNotificationIds.HABIT_DAILY_ID)
                    }
                    ReminderIntents.ACTION_HABIT_DISMISS -> {
                        NotificationManagerCompat.from(context).cancel(ReminderNotificationIds.HABIT_DAILY_ID)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun showTaskNotification(context: Context, taskId: Long) {
        val task = taskRepo.getById(taskId) ?: return
        if (task.isDone) return

        val nm = NotificationManagerCompat.from(context)
        val notifId = taskNotifId(taskId)

        val openSheet = PendingIntent.getActivity(
            context,
            notifId,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_TASK_REMINDER_SHEET_ID, taskId)
            },
            pendingFlags(),
        )

        fun actionPi(a: String, code: Int): PendingIntent {
            val i = Intent(context, DayRouteReminderReceiver::class.java).apply {
                this.action = a
                putExtra(ReminderIntents.EXTRA_TASK_ID, taskId)
            }
            return PendingIntent.getBroadcast(context, code, i, pendingFlags())
        }

        val donePi = actionPi(ReminderIntents.ACTION_TASK_DONE, notifId + 1)
        val pomoPi = actionPi(ReminderIntents.ACTION_TASK_POMODORO, notifId + 2)
        val dismissPi = actionPi(ReminderIntents.ACTION_TASK_DISMISS, notifId + 3)

        val notif = NotificationCompat.Builder(context, ReminderNotificationIds.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Task starting: ${task.title}")
            .setContentText("${task.startTime} – ${task.endTime} · Open for options")
            .setContentIntent(openSheet)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(0, "Mark done", donePi)
            .addAction(0, "Pomodoro", pomoPi)
            .addAction(0, "Dismiss", dismissPi)
            .build()

        nm.notify(notifId, notif)
    }

    private fun showHabitNotification(context: Context) {
        val nm = NotificationManagerCompat.from(context)
        val openPi = PendingIntent.getBroadcast(
            context,
            ReminderNotificationIds.HABIT_DAILY_ID + 1,
            Intent(context, DayRouteReminderReceiver::class.java).apply {
                action = ReminderIntents.ACTION_HABIT_OPEN
            },
            pendingFlags(),
        )
        val dismissPi = PendingIntent.getBroadcast(
            context,
            ReminderNotificationIds.HABIT_DAILY_ID + 2,
            Intent(context, DayRouteReminderReceiver::class.java).apply {
                action = ReminderIntents.ACTION_HABIT_DISMISS
            },
            pendingFlags(),
        )
        val notif = NotificationCompat.Builder(context, ReminderNotificationIds.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Habits check-in")
            .setContentText("Review or log today’s habits")
            .setContentIntent(openPi)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(0, "Open habits", openPi)
            .addAction(0, "Dismiss", dismissPi)
            .build()
        nm.notify(ReminderNotificationIds.HABIT_DAILY_ID, notif)
        habitReminderScheduler.scheduleDaily()
    }

    private fun pendingFlags(): Int {
        var f = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            f = f or PendingIntent.FLAG_IMMUTABLE
        }
        return f
    }

    private fun taskNotifId(taskId: Long): Int =
        ReminderNotificationIds.TASK_BASE_ID + (taskId % 29_000).toInt()
}
