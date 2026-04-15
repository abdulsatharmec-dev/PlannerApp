package com.dailycurator

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.dailycurator.backup.BackupScheduler
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class DailyCuratorApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var backupScheduler: BackupScheduler

    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(applicationContext)
        backupScheduler.schedulePeriodicDriveBackup()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
