package com.example.dumbphonelauncher.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM app_config ORDER BY name ASC")
    fun getAllApps(): Flow<List<AppConfig>>

    @Query("SELECT * FROM app_config WHERE category = :category ORDER BY name ASC")
    fun getAppsByCategory(category: AppCategory): Flow<List<AppConfig>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertApps(apps: List<AppConfig>)

    @Update
    suspend fun updateApp(app: AppConfig)

    @Query("SELECT * FROM app_config WHERE packageName = :packageName LIMIT 1")
    suspend fun getAppByPackage(packageName: String): AppConfig?

    @Query("UPDATE app_config SET name = :name WHERE packageName = :packageName AND name != :name")
    suspend fun updateAppName(packageName: String, name: String)

    @Query("SELECT packageName FROM app_config")
    suspend fun getAllPackageNames(): List<String>

    @Query("DELETE FROM app_config WHERE packageName IN (:packageNames)")
    suspend fun deleteUninstalledApps(packageNames: List<String>)
}
