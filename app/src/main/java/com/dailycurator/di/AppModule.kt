package com.dailycurator.di

import android.content.Context
import com.dailycurator.data.local.AppDatabase
import com.dailycurator.data.local.dao.GoalDao
import com.dailycurator.data.local.dao.HabitDao
import com.dailycurator.data.local.dao.TaskDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.getDatabase(context)

    @Provides @Singleton
    fun provideTaskDao(db: AppDatabase): TaskDao = db.taskDao()

    @Provides @Singleton
    fun provideHabitDao(db: AppDatabase): HabitDao = db.habitDao()

    @Provides @Singleton
    fun provideGoalDao(db: AppDatabase): GoalDao = db.goalDao()
}
