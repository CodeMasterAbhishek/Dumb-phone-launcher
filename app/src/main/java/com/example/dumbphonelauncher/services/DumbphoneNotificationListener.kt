package com.example.dumbphonelauncher.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.dumbphonelauncher.ServiceLocator
import com.example.dumbphonelauncher.data.AppCategory
import com.example.dumbphonelauncher.data.AppConfig
import kotlinx.coroutines.*

data class DeferredNotification(
    val packageName: String,
    val title: String,
    val text: String,
    val timestamp: Long
)

object NotificationStore {
    private val _pendingNotifications = mutableListOf<DeferredNotification>()
    val pendingNotifications: List<DeferredNotification>
        get() = synchronized(_pendingNotifications) { _pendingNotifications.toList() }

    fun add(notification: DeferredNotification) {
        synchronized(_pendingNotifications) {
            _pendingNotifications.add(notification)
        }
    }

    fun clearForPackage(packageName: String) {
        synchronized(_pendingNotifications) {
            _pendingNotifications.removeAll { it.packageName == packageName }
        }
    }
}

class DumbphoneNotificationListener : NotificationListenerService() {

    companion object {
        const val QUIET_CHANNEL_ID = "quiet_notifications_channel"
        @Volatile
        var isConnected: Boolean = false
    }

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    // In-memory cache to avoid querying the DB on every notification
    @Volatile
    private var cachedApps: List<AppConfig> = emptyList()

    override fun onCreate() {
        super.onCreate()
        createQuietChannel()
        loadApps()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        isConnected = true
        createQuietChannel()
        loadApps()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isConnected = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                requestRebind(ComponentName(this, DumbphoneNotificationListener::class.java))
            } catch (_: Exception) {}
        }
    }

    private fun loadApps() {
        val appRepo = ServiceLocator.provideAppRepository(this)
        scope.launch {
            appRepo.getAllApps().collect { cachedApps = it }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: return
        if (packageName == this.packageName) return // Ignore notifications from our own launcher

        // Ongoing notifications (e.g. active phone calls, ongoing media playback) must not be cancelled
        if (sbn.isOngoing) return

        // Always allow critical system notifications (incoming calls, alarms, Android OS)
        if (packageName == "android" || packageName == "com.android.systemui") return
        val notifCategory = sbn.notification?.category
        if (notifCategory == android.app.Notification.CATEGORY_CALL || 
            notifCategory == android.app.Notification.CATEGORY_ALARM) return

        var config = cachedApps.find { it.packageName == packageName }
        if (config == null) {
            val appRepo = ServiceLocator.provideAppRepository(this)
            scope.launch {
                val dbConfig = appRepo.getAppByPackage(packageName)
                processNotificationWithConfig(sbn, dbConfig)
            }
        } else {
            processNotificationWithConfig(sbn, config)
        }
    }

    private fun processNotificationWithConfig(sbn: StatusBarNotification, config: AppConfig?) {
        val packageName = sbn.packageName

        // Only apps that are explicitly allowed (ESSENTIAL, CONTROLLED, or SECONDARY/UTILITY) can show notifications.
        // Any app that is HIDDEN, UNCATEGORIZED, or unlisted is completely blocked.
        val isAllowedApp = config != null && (config.category == AppCategory.ESSENTIAL || config.category == AppCategory.CONTROLLED || config.category == AppCategory.SECONDARY)
        if (!isAllowedApp) {
            safeCancelNotification(sbn)
            return
        }

        // Notification Modes for allowed apps (Essential or Controlled)
        when (config!!.notificationMode) {
            "BLOCKED" -> {
                safeCancelNotification(sbn)
            }
            "WHEN_OPENED" -> {
                val isForeground = (AppUsageMonitorService.currentForegroundPackage == packageName)
                if (!isForeground) {
                    // App is closed: cancel immediately to avoid interrupting the user
                    safeCancelNotification(sbn)

                    val extras = sbn.notification?.extras
                    val title = extras?.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString() ?: config.name
                    val text = extras?.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString() ?: ""
                    NotificationStore.add(DeferredNotification(packageName, title, text, sbn.postTime))
                } else {
                    // App is already open and in foreground: clear pending and let live notification through
                    NotificationStore.clearForPackage(packageName)
                }
            }
            "QUIET" -> {
                handleQuietNotification(sbn, config)
            }
            "NORMAL" -> {
                // Allow notification through normally
            }
        }
    }

    private fun safeCancelNotification(sbn: StatusBarNotification) {
        try {
            cancelNotification(sbn.key)
        } catch (_: Exception) {
            try {
                @Suppress("DEPRECATION")
                cancelNotification(sbn.packageName, sbn.tag, sbn.id)
            } catch (_: Exception) {}
        }
    }

    private fun handleQuietNotification(sbn: StatusBarNotification, config: AppConfig) {
        val originalNotif = sbn.notification ?: return
        val extras = originalNotif.extras ?: return
        val title = extras.getCharSequence(android.app.Notification.EXTRA_TITLE) ?: config.name
        val text = extras.getCharSequence(android.app.Notification.EXTRA_TEXT) ?: ""
        val contentIntent = originalNotif.contentIntent

        // Cancel the original noisy notification
        safeCancelNotification(sbn)

        // Re-post as silent, minimized notification with IMPORTANCE_LOW
        val quietNotif = NotificationCompat.Builder(this, QUIET_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .build()

        val manager = NotificationManagerCompat.from(this)
        try {
            manager.notify(sbn.id, quietNotif)
        } catch (_: Exception) {}
    }

    private fun createQuietChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                QUIET_CHANNEL_ID,
                "Quiet Notifications",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Minimized, non-intrusive notifications"
                setShowBadge(false)
                enableVibration(false)
                enableLights(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
