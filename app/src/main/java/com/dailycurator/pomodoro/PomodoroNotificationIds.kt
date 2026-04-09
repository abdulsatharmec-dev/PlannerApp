package com.dailycurator.pomodoro

object PomodoroNotificationIds {
    /** Ongoing foreground timer — low importance, no sound. */
    const val CHANNEL_ID = "pomodoro_timer"
    /** Session finished — plays default notification tone. */
    const val COMPLETE_CHANNEL_ID = "pomodoro_complete"
    const val ONGOING_NOTIFICATION_ID = 7101
}

object PomodoroNotificationActions {
    const val PAUSE = "com.dailycurator.pomodoro.PAUSE"
    const val RESUME = "com.dailycurator.pomodoro.RESUME"
    const val STOP = "com.dailycurator.pomodoro.STOP"
    /** Fires at wall-clock phase end; survives Doze better than coroutine ticks alone. */
    const val TIMER_PHASE_COMPLETE = "com.dailycurator.pomodoro.TIMER_PHASE_COMPLETE"
}
