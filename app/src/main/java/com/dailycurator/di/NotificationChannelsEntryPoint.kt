package com.dailycurator.di

import com.dailycurator.data.local.AppPreferences
import com.dailycurator.pomodoro.AppNotificationChannels
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface NotificationChannelsEntryPoint {
    fun appNotificationChannels(): AppNotificationChannels
    fun appPreferences(): AppPreferences
}
