package com.dailycurator.di

import com.dailycurator.media.MorningMotivationLocalPlaybackHolder
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface MorningMotivationPlaybackEntryPoint {
    fun morningMotivationLocalPlaybackHolder(): MorningMotivationLocalPlaybackHolder
}
