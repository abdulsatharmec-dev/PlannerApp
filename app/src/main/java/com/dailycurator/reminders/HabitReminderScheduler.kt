package com.dailycurator.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HabitReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleDaily(hour: Int = 20, minute: Int = 0) {
        val trigger = nextDailyMillis(hour, minute)
        val intent = Intent(context, DayRouteReminderReceiver::class.java).apply {
            action = ReminderIntents.ACTION_HABIT_ALARM
        }
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = flags or PendingIntent.FLAG_IMMUTABLE
        }
        val pi = PendingIntent.getBroadcast(
            context,
            ReminderNotificationIds.HABIT_DAILY_ID,
            intent,
            flags,
        )
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setAlarmClock(AlarmManager.AlarmClockInfo(trigger, pi), pi)
            } else {
                @Suppress("DEPRECATION")
                am.setExact(AlarmManager.RTC_WAKEUP, trigger, pi)
            }
        } catch (_: SecurityException) {
            @Suppress("DEPRECATION")
            am.set(AlarmManager.RTC_WAKEUP, trigger, pi)
        }
    }

    private fun nextDailyMillis(hour: Int, minute: Int): Long {
        val zone = ZoneId.systemDefault()
        var next = ZonedDateTime.now(zone).withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!next.isAfter(ZonedDateTime.now(zone))) {
            next = next.plusDays(1)
        }
        return next.toInstant().toEpochMilli()
    }
}
