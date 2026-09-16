package com.example.dumbphonelauncher.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.dumbphonelauncher.data.AppCategory
import com.example.dumbphonelauncher.data.AppConfig
import com.example.dumbphonelauncher.data.LauncherPreferences
import com.example.dumbphonelauncher.domain.AppRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val appRepository: AppRepository,
    private val launcherPreferences: LauncherPreferences
) : ViewModel() {

    val hasCompletedSetup: StateFlow<Boolean> = launcherPreferences.hasCompletedSetup
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
        
    val appAdditionsCount: StateFlow<Int> = launcherPreferences.appAdditionsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
        
    val appAdditionsLockUntil: StateFlow<Long> = launcherPreferences.appAdditionsLockUntil
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
        
    val themeBgColor: StateFlow<Int?> = launcherPreferences.themeBgColor
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
        
    val themeTextColor: StateFlow<Int?> = launcherPreferences.themeTextColor
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
        
    val allApps = appRepository.getAllApps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun syncApps() {
        viewModelScope.launch {
            try {
                appRepository.syncInstalledApps()
            } catch (e: Exception) {
                // Ignore errors during sync to prevent crashing the launcher
            }
        }
    }

    fun completeSetup() {
        viewModelScope.launch {
            launcherPreferences.setHasCompletedSetup(true)
        }
    }

    fun updateAppCategory(app: AppConfig, newCategory: AppCategory) {
        viewModelScope.launch {
            appRepository.updateApp(app.copy(category = newCategory))
        }
    }

    fun updateAppConfig(app: AppConfig) {
        viewModelScope.launch {
            appRepository.updateApp(app)
        }
    }
    
    fun recordAppAdditionAndLock() {
        viewModelScope.launch {
            launcherPreferences.recordAppAdditionAndLock()
        }
    }
    
    // For testing purposes — internal to prevent accidental production usage
    internal fun resetAppAdditionLock() {
        viewModelScope.launch {
            launcherPreferences.resetAppAdditionLock()
        }
    }
    
    fun setThemeColors(bgColor: Int?, textColor: Int?) {
        viewModelScope.launch {
            launcherPreferences.setThemeColors(bgColor, textColor)
        }
    }

    // PIN Lock for Settings
    val settingsPin: StateFlow<String?> = launcherPreferences.settingsPin
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setSettingsPin(pin: String?) {
        viewModelScope.launch {
            launcherPreferences.setSettingsPin(pin)
        }
    }

    // Font customization
    val fontScale: StateFlow<Float> = launcherPreferences.fontScale
        .stateIn(viewModelScope, SharingStarted.Eagerly, 1.0f)

    val fontWeight: StateFlow<Int> = launcherPreferences.fontWeight
        .stateIn(viewModelScope, SharingStarted.Eagerly, 400)

    fun setFontScale(scale: Float) {
        viewModelScope.launch {
            launcherPreferences.setFontScale(scale)
        }
    }

    fun setFontWeight(weight: Int) {
        viewModelScope.launch {
            launcherPreferences.setFontWeight(weight)
        }
    }

    val fontFamily: StateFlow<String> = launcherPreferences.fontFamily
        .stateIn(viewModelScope, SharingStarted.Eagerly, "DEFAULT")

    fun setFontFamily(family: String) {
        viewModelScope.launch {
            launcherPreferences.setFontFamily(family)
        }
    }

    class Factory(
        private val appRepository: AppRepository,
        private val launcherPreferences: LauncherPreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                return MainViewModel(appRepository, launcherPreferences) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
