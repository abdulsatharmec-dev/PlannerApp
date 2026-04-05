package com.dailycurator.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.dailycurator.data.model.PriorityTask
import com.dailycurator.data.repository.TaskRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repo: TaskRepository,
) {
    private val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(task: PriorityTask) {
        if (task.id <= 0L || task.isDone) {
            cancel(task.id)
            return
        }
        val trigger = triggerMillis(task) ?: run {
            cancel(task.id)
            return
        }
        val pi = alarmPendingIntent(task.id)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setAlarmClock(
                    AlarmManager.AlarmClockInfo(trigger, pi),
                    pi,
                )
            } else {
                @Suppress("DEPRECATION")
                am.setExact(AlarmManager.RTC_WAKEUP, trigger, pi)
            }
        } catch (_: SecurityException) {
            @Suppress("DEPRECATION")
            am.set(AlarmManager.RTC_WAKEUP, trigger, pi)
        }
    }

    fun cancel(taskId: Long) {
        if (taskId <= 0L) return
        am.cancel(alarmPendingIntent(taskId))
    }

    suspend fun rescheduleAllUndone() {
        repo.getUndoneTasks().forEach { schedule(it) }
    }

    private fun triggerMillis(task: PriorityTask): Long? {
        val ldt = LocalDateTime.of(task.date, task.startTime)
        val zdt = ldt.atZone(ZoneId.systemDefault())
        val ms = zdt.toInstant().toEpochMilli()
        if (ms <= System.currentTimeMillis()) return null
        return ms
    }

    private fun alarmPendingIntent(taskId: Long): PendingIntent {
        val intent = Intent(context, DayRouteReminderReceiver::class.java).apply {
            action = ReminderIntents.ACTION_TASK_ALARM
            putExtra(ReminderIntents.EXTRA_TASK_ID, taskId)
        }
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = flags or PendingIntent.FLAG_IMMUTABLE
        }
        return PendingIntent.getBroadcast(
            context,
            taskId.toInt().coerceAtLeast(1),
            intent,
            flags,
        )
    }
}
