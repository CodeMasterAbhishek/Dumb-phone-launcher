package com.example.dumbphonelauncher.domain

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.example.dumbphonelauncher.data.AppCategory
import com.example.dumbphonelauncher.data.AppConfig
import com.example.dumbphonelauncher.data.AppDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppRepository(
    private val context: Context,
    private val appDao: AppDao
) {
    suspend fun syncInstalledApps() = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        
        // Use MATCH_DISABLED_COMPONENTS (512) to ensure we don't accidentally delete paused/disabled apps
        val flags = PackageManager.MATCH_DISABLED_COMPONENTS
        val resolveInfos = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(flags.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, flags)
        }
        
        val installedApps = resolveInfos.mapNotNull { resolveInfo ->
            val packageName = resolveInfo.activityInfo.packageName
            val name = resolveInfo.loadLabel(pm).toString()
            if (packageName == context.packageName) return@mapNotNull null // Skip our own launcher
            if (packageName == "com.android.settings" || packageName == "com.google.android.settings" || packageName.endsWith(".settings") || packageName.contains(".settings.")) return@mapNotNull null // Settings is managed separately via Launcher Settings button
            
            AppConfig(
                packageName = packageName,
                name = name,
                category = AppCategory.UNCATEGORIZED
            )
        }
        
        // Run all DB operations in a single transaction to reduce I/O overhead
        val packageNames = installedApps.map { it.packageName }.toMutableList()
        packageNames.add(context.packageName)
        
        val db = com.example.dumbphonelauncher.data.AppDatabase.getDatabase(context)
        db.runInTransaction {
            kotlinx.coroutines.runBlocking {
                // Insert new apps (IGNORE strategy will preserve existing user configs)
                appDao.insertApps(installedApps)
                
                // Batch-update display names (only writes if name actually changed)
                installedApps.forEach { app ->
                    appDao.updateAppName(app.packageName, app.name)
                }
                
                // Remove uninstalled apps
                val dbPackageNames = appDao.getAllPackageNames()
                val toDelete = dbPackageNames - packageNames.toSet()
                if (toDelete.isNotEmpty()) {
                    toDelete.chunked(500).forEach { chunk ->
                        appDao.deleteUninstalledApps(chunk)
                    }
                }
            }
        }
    }

    fun getAllApps() = appDao.getAllApps()
    
    fun getAppsByCategory(category: AppCategory) = appDao.getAppsByCategory(category)
    
    suspend fun updateApp(app: AppConfig) {
        appDao.updateApp(app)
    }

    suspend fun getAppByPackage(packageName: String): AppConfig? {
        return appDao.getAppByPackage(packageName)
    }
}
