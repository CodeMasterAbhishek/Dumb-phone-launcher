package com.example.dumbphonelauncher.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.example.dumbphonelauncher.ServiceLocator
import com.example.dumbphonelauncher.data.AppCategory
import com.example.dumbphonelauncher.data.AppConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class AppUsageMonitorService : Service() {

    companion object {
        @Volatile
        var currentForegroundPackage: String? = null
    }

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, createNotification())
        startMonitoring()
    }

    override fun onDestroy() {
        super.onDestroy()
        currentForegroundPackage = null
        job.cancel()
    }

    private var currentSessionPackage: String? = null
    private var sessionStartTime: Long = 0
    private var lastReminderTime: Long = 0
    private var lastBlockedPackage: String? = null
    private var lastBlockedTime: Long = 0

    private fun startMonitoring() {
        scope.launch {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val appRepo = ServiceLocator.provideAppRepository(this@AppUsageMonitorService)
            val usageStatsRepo = ServiceLocator.provideUsageStatsRepository(this@AppUsageMonitorService)
            val prefs = ServiceLocator.provideLauncherPreferences(this@AppUsageMonitorService)
            
            // Cache state in memory to avoid slow database/datastore reads in the polling loop.
            var cachedApps: List<AppConfig> = emptyList()
            var cachedHasCompletedSetup = false
            
            launch { appRepo.getAllApps().collect { cachedApps = it } }
            launch { prefs.hasCompletedSetup.collect { cachedHasCompletedSetup = it } }
            
            while (isActive) {
                delay(2000) // Poll every 2s for near-instant blocking
                
                // If initial setup/onboarding has not been completed, do not block apps.
                // The user is currently onboarding, granting permissions, and configuring the launcher.
                if (!cachedHasCompletedSetup) {
                    continue
                }
                
                try {
                    val fg = getForegroundActivity(usageStatsManager) ?: continue
                    val currentForegroundApp = fg.first
                    val currentForegroundClass = fg.second
                    val now = System.currentTimeMillis()
                    
                    // ── CALL / ALARM BYPASS (wrapped safely – telephony can throw SecurityException) ──
                    var skipDueToCall = false
                    try {
                        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as android.telephony.TelephonyManager
                        val isCallActive = telephonyManager.callState != android.telephony.TelephonyManager.CALL_STATE_IDLE
                        val telecomManager = getSystemService(Context.TELECOM_SERVICE) as android.telecom.TelecomManager
                        val defaultDialer = telecomManager.defaultDialerPackage
                        
                        val simpleClassName = currentForegroundClass.substringAfterLast('.')
                        val isCallOrAlarmUI = simpleClassName.contains("InCall", ignoreCase = true) ||
                                              simpleClassName.contains("alarm", ignoreCase = true) ||
                                              simpleClassName.contains("ring", ignoreCase = true) ||
                                              simpleClassName.contains("Dialer", ignoreCase = true) ||
                                              simpleClassName.contains("answer", ignoreCase = true) ||
                                              simpleClassName.contains("voip", ignoreCase = true)
                                              
                        if (isCallActive || currentForegroundApp == defaultDialer || isCallOrAlarmUI) {
                            skipDueToCall = true
                        }
                    } catch (_: SecurityException) {
                        // READ_PHONE_STATE not granted – proceed without call detection
                    }
                    
                    if (skipDueToCall) continue

                    // ── APP BLOCKING ──
                    currentForegroundPackage = currentForegroundApp

                    if (currentForegroundApp != currentSessionPackage) {
                        currentSessionPackage = currentForegroundApp
                        sessionStartTime = SystemClock.elapsedRealtime()
                        lastReminderTime = 0
                    }
                    
                    val config = cachedApps.find { it.packageName == currentForegroundApp }
                    
                    if (config != null) {
                        val tempAccessEnd = config.temporaryAccessEndTime
                        val hasTempAccess = tempAccessEnd != null && now < tempAccessEnd

                        if (config.category == AppCategory.HIDDEN) {
                            if (!hasTempAccess) {
                                if (currentForegroundApp != lastBlockedPackage || now - lastBlockedTime > 2000) {
                                    showBlockingOverlay(config, isPermanentlyBlocked = true)
                                    lastBlockedPackage = currentForegroundApp
                                    lastBlockedTime = now
                                }
                            }
                            continue
                        }
                        
                        if (config.category == AppCategory.CONTROLLED) {
                            val dailyLimit = config.dailyLimitMinutes

                            if (dailyLimit != null && !hasTempAccess) {
                                val usageMs = usageStatsRepo.getTodayUsageForPackage(currentForegroundApp)
                                val limitMs = dailyLimit * 60 * 1000L
                                if (usageMs > limitMs) {
                                    if (currentForegroundApp != lastBlockedPackage || now - lastBlockedTime > 2000) {
                                        showBlockingOverlay(config, isPermanentlyBlocked = false)
                                        lastBlockedPackage = currentForegroundApp
                                        lastBlockedTime = now
                                    }
                                    continue
                                }
                            }

                            val continuousLimit = config.continuousUsageReminderMinutes
                            if (continuousLimit != null) {
                                val sessionDurationMs = SystemClock.elapsedRealtime() - sessionStartTime
                                val limitMs = continuousLimit * 60 * 1000L
                                
                                if (sessionDurationMs > limitMs && (SystemClock.elapsedRealtime() - lastReminderTime > 5 * 60 * 1000L)) {
                                    lastReminderTime = SystemClock.elapsedRealtime()
                                    showReminderOverlay(config, continuousLimit)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Prevent any crash in the polling loop from killing the service
                }
            }
        }
    }

    private var lastKnownForeground: String? = null
    private var lastKnownForegroundClass: String = ""

    private fun getForegroundActivity(usageStatsManager: UsageStatsManager): Pair<String, String>? {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 5000 // Look back 5 seconds
        val events = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        var currentForegroundPackage: String? = null
        var currentForegroundClass: String? = null
        
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                currentForegroundPackage = event.packageName
                currentForegroundClass = event.className
            }
        }
        
        if (currentForegroundPackage != null) {
            lastKnownForeground = currentForegroundPackage
            lastKnownForegroundClass = currentForegroundClass ?: ""
            return Pair(currentForegroundPackage, currentForegroundClass ?: "")
        } else if (lastKnownForeground != null) {
            return Pair(lastKnownForeground!!, lastKnownForegroundClass)
        }
        
        val longStartTime = endTime - 60000
        val longEvents = usageStatsManager.queryEvents(longStartTime, endTime)
        while (longEvents.hasNextEvent()) {
            longEvents.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                currentForegroundPackage = event.packageName
                currentForegroundClass = event.className
            }
        }
        
        if (currentForegroundPackage != null) {
            lastKnownForeground = currentForegroundPackage
            lastKnownForegroundClass = currentForegroundClass ?: ""
            return Pair(currentForegroundPackage, currentForegroundClass ?: "")
        }
        
        return null
    }

    private fun showBlockingOverlay(app: AppConfig, isPermanentlyBlocked: Boolean = false) {
        val intent = Intent(this, BlockingActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("PACKAGE_NAME", app.packageName)
            putExtra("IS_PERMANENTLY_BLOCKED", isPermanentlyBlocked)
        }
        startActivity(intent)
    }

    private fun showReminderOverlay(app: AppConfig, minutes: Int) {
        val intent = Intent(this, ReminderActivity::class.java).apply {
            // Only NEW_TASK — do NOT clear the user's app task so they can resume after dismissing
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("PACKAGE_NAME", app.packageName)
            putExtra("APP_NAME", app.name)
            putExtra("MINUTES", minutes)
        }
        startActivity(intent)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "usage_monitor_channel",
            "Usage Monitor",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "usage_monitor_channel")
            .setContentTitle("DumbPhone Launcher")
            .setContentText("Monitoring app limits...")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .build()
    }
}
