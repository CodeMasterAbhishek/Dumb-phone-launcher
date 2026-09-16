package com.example.dumbphonelauncher.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_config")
data class AppConfig(
    @PrimaryKey val packageName: String,
    val name: String,
    val category: AppCategory = AppCategory.UNCATEGORIZED,
    val dailyLimitMinutes: Int? = null,
    val mindfulOpeningEnabled: Boolean = false,
    val mindfulOpeningDurationSeconds: Int = 0,
    val continuousUsageReminderMinutes: Int? = null,
    val notificationMode: String = "NORMAL",
    val temporaryAccessEndTime: Long? = null,
    val showOnHomeScreen: Boolean = false
)
