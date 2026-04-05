package com.dailycurator.pomodoro

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PomodoroActionReceiver : BroadcastReceiver() {

    @Inject lateinit var controller: PomodoroTimerController

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            PomodoroNotificationActions.PAUSE -> controller.pause()
            PomodoroNotificationActions.RESUME -> controller.startOrResume()
            PomodoroNotificationActions.STOP -> controller.endSessionEarlyFromAnywhere()
        }
    }
}
