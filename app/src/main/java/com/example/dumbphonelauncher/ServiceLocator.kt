package com.example.dumbphonelauncher

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.dumbphonelauncher.data.AppDatabase
import com.example.dumbphonelauncher.data.LauncherPreferences
import com.example.dumbphonelauncher.domain.AppRepository

object ServiceLocator {
    @Volatile private var database: AppDatabase? = null
    @Volatile private var appRepository: AppRepository? = null
    @Volatile private var launcherPreferences: LauncherPreferences? = null

    fun provideAppRepository(context: Context): AppRepository {
        return appRepository ?: synchronized(this) {
            val db = database ?: AppDatabase.getDatabase(context).also { database = it }
            val repo = AppRepository(context.applicationContext, db.appDao())
            appRepository = repo
            repo
        }
    }

    fun provideLauncherPreferences(context: Context): LauncherPreferences {
        return launcherPreferences ?: synchronized(this) {
            val prefs = LauncherPreferences(context.applicationContext)
            launcherPreferences = prefs
            prefs
        }
    }

    @Volatile private var usageStatsRepository: com.example.dumbphonelauncher.domain.UsageStatsRepository? = null

    fun provideUsageStatsRepository(context: Context): com.example.dumbphonelauncher.domain.UsageStatsRepository {
        return usageStatsRepository ?: synchronized(this) {
            val repo = com.example.dumbphonelauncher.domain.UsageStatsRepository(context.applicationContext)
            usageStatsRepository = repo
            repo
        }
    }
}
