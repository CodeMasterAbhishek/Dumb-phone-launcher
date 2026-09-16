package com.example.dumbphonelauncher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "launcher_settings")

class LauncherPreferences(private val context: Context) {
    
    companion object {
        val HAS_COMPLETED_SETUP = booleanPreferencesKey("has_completed_setup")
        val APP_ADDITIONS_COUNT = androidx.datastore.preferences.core.intPreferencesKey("app_additions_count")
        val APP_ADDITIONS_LOCK_UNTIL = androidx.datastore.preferences.core.longPreferencesKey("app_additions_lock_until")
        val THEME_BG_COLOR = androidx.datastore.preferences.core.intPreferencesKey("theme_bg_color")
        val THEME_TEXT_COLOR = androidx.datastore.preferences.core.intPreferencesKey("theme_text_color")
        val SETTINGS_PIN = androidx.datastore.preferences.core.stringPreferencesKey("settings_pin")
        val FONT_SCALE = androidx.datastore.preferences.core.floatPreferencesKey("font_scale")
        val FONT_WEIGHT = androidx.datastore.preferences.core.intPreferencesKey("font_weight")
        val FONT_FAMILY = androidx.datastore.preferences.core.stringPreferencesKey("font_family")
    }

    val hasCompletedSetup: Flow<Boolean> = context.dataStore.data.map { it[HAS_COMPLETED_SETUP] ?: false }
    val appAdditionsCount: Flow<Int> = context.dataStore.data.map { it[APP_ADDITIONS_COUNT] ?: 0 }
    val appAdditionsLockUntil: Flow<Long> = context.dataStore.data.map { it[APP_ADDITIONS_LOCK_UNTIL] ?: 0L }
    val themeBgColor: Flow<Int?> = context.dataStore.data.map { it[THEME_BG_COLOR] }
    val themeTextColor: Flow<Int?> = context.dataStore.data.map { it[THEME_TEXT_COLOR] }

    suspend fun setHasCompletedSetup(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HAS_COMPLETED_SETUP] = completed
        }
    }
    
    suspend fun recordAppAdditionAndLock() {
        context.dataStore.edit { preferences ->
            val currentCount = preferences[APP_ADDITIONS_COUNT] ?: 0
            preferences[APP_ADDITIONS_COUNT] = currentCount + 1
            // Lock for 7 days
            preferences[APP_ADDITIONS_LOCK_UNTIL] = System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000)
        }
    }
    
    suspend fun resetAppAdditionLock() {
        context.dataStore.edit { preferences ->
            preferences[APP_ADDITIONS_COUNT] = 0
            preferences[APP_ADDITIONS_LOCK_UNTIL] = 0L
        }
    }
    
    suspend fun setThemeColors(bgColor: Int?, textColor: Int?) {
        context.dataStore.edit { preferences ->
            if (bgColor != null) preferences[THEME_BG_COLOR] = bgColor else preferences.remove(THEME_BG_COLOR)
            if (textColor != null) preferences[THEME_TEXT_COLOR] = textColor else preferences.remove(THEME_TEXT_COLOR)
        }
    }

    // PIN Lock
    val settingsPin: Flow<String?> = context.dataStore.data.map { it[SETTINGS_PIN] }

    suspend fun setSettingsPin(pin: String?) {
        context.dataStore.edit { preferences ->
            if (pin != null) preferences[SETTINGS_PIN] = pin else preferences.remove(SETTINGS_PIN)
        }
    }

    // Font customization
    val fontScale: Flow<Float> = context.dataStore.data.map { it[FONT_SCALE] ?: 1.0f }
    val fontWeight: Flow<Int> = context.dataStore.data.map { it[FONT_WEIGHT] ?: 400 }

    suspend fun setFontScale(scale: Float) {
        context.dataStore.edit { preferences ->
            preferences[FONT_SCALE] = scale
        }
    }

    suspend fun setFontWeight(weight: Int) {
        context.dataStore.edit { preferences ->
            preferences[FONT_WEIGHT] = weight
        }
    }

    val fontFamily: Flow<String> = context.dataStore.data.map { it[FONT_FAMILY] ?: "DEFAULT" }

    suspend fun setFontFamily(family: String) {
        context.dataStore.edit { preferences ->
            preferences[FONT_FAMILY] = family
        }
    }
}
