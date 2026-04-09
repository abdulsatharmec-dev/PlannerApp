package com.dailycurator.pomodoro

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dailycurator.di.ApplicationScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PomodoroActionReceiver : BroadcastReceiver() {

    @Inject lateinit var controller: PomodoroTimerController

    @Inject @ApplicationScope lateinit var appScope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            PomodoroNotificationActions.PAUSE -> controller.pause()
            PomodoroNotificationActions.RESUME -> controller.startOrResume()
            PomodoroNotificationActions.STOP -> controller.endSessionEarlyFromAnywhere()
            PomodoroNotificationActions.TIMER_PHASE_COMPLETE -> {
                val pending = goAsync()
                appScope.launch {
                    try {
                        controller.completeDueToAlarm()
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }
}
