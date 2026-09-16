# DumbPhone Launcher ProGuard Rules

# Keep Room entities and DAOs
-keep class com.example.dumbphonelauncher.data.** { *; }

# Keep the NotificationListenerService
-keep class com.example.dumbphonelauncher.services.DumbphoneNotificationListener { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# DataStore
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}
