package com.example.dumbphonelauncher.services

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dumbphonelauncher.ServiceLocator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

class BlockingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val packageName = intent.getStringExtra("PACKAGE_NAME") ?: return finish()
        val isPermanentlyBlocked = intent.getBooleanExtra("IS_PERMANENTLY_BLOCKED", false)
        val isSettingsBlock = intent.getBooleanExtra("IS_SETTINGS_BLOCK", false)
        val appRepo = ServiceLocator.provideAppRepository(this)
        
        val prefs = ServiceLocator.provideLauncherPreferences(this)

        setContent {
            androidx.activity.compose.BackHandler { }

            var app by remember { mutableStateOf<com.example.dumbphonelauncher.data.AppConfig?>(null) }
            var isLoading by remember { mutableStateOf(!isSettingsBlock && packageName != "com.android.settings") }
            
            LaunchedEffect(packageName) {
                if (!isSettingsBlock && packageName != "com.android.settings") {
                    app = appRepo.getAppByPackage(packageName)
                    if (app == null) {
                        finish()
                    }
                    isLoading = false
                }
            }
            
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@setContent
            }
            val bgColor by prefs.themeBgColor.collectAsState(initial = null)
            val textColor by prefs.themeTextColor.collectAsState(initial = null)
            
            val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val defaultBg = if (isSystemDark) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            
            LaunchedEffect(bgColor, isSystemDark) {
                val currentBgInt = bgColor ?: defaultBg
                val bgLuminance = androidx.core.graphics.ColorUtils.calculateLuminance(currentBgInt)
                val isDarkBg = bgLuminance < 0.5
                
                val style = if (isDarkBg) {
                    androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                } else {
                    androidx.activity.SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                }
                
                enableEdgeToEdge(
                    statusBarStyle = style,
                    navigationBarStyle = style
                )
            }
            
            com.example.dumbphonelauncher.ui.theme.DumbPhoneLauncherTheme(
                customBgColor = bgColor,
                customTextColor = textColor
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (isPermanentlyBlocked) {
                                Text(
                                    text = "App is blocked.",
                                    style = MaterialTheme.typography.headlineMedium
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = "You can't use this app.\n(${app?.packageName ?: packageName})",
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            } else {
                                Text(
                                    text = "You've used your ${app?.name} time for today.",
                                    style = MaterialTheme.typography.headlineMedium
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = "Come back tomorrow.",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(64.dp))
                            
                            val scope = rememberCoroutineScope()
                            
                            Button(
                                onClick = {
                                    val homeIntent = Intent(Intent.ACTION_MAIN)
                                    homeIntent.addCategory(Intent.CATEGORY_HOME)
                                    homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    startActivity(homeIntent)
                                    finish()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Go Home")
                            }

                            if (isPermanentlyBlocked && app != null) {
                                Spacer(modifier = Modifier.height(32.dp))
                                TextButton(
                                    onClick = {
                                        val currentApp = app ?: return@TextButton
                                        scope.launch {
                                            val newTime = System.currentTimeMillis() + 15 * 1000L 
                                            val appRepo = ServiceLocator.provideAppRepository(this@BlockingActivity)
                                            appRepo.updateApp(currentApp.copy(temporaryAccessEndTime = newTime))
                                            delay(500)
                                            finish() 
                                        }
                                    }
                                ) {
                                    Text("Quick Access (15 seconds)")
                                }
                            }
                            
                            val currentApp = app
                            if (!isPermanentlyBlocked && currentApp != null) {
                                Spacer(modifier = Modifier.height(32.dp))
                                Text(
                                    text = "Need it for something?",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                TextButton(
                                    onClick = {
                                        scope.launch {
                                            val newTime = System.currentTimeMillis() + 5 * 60 * 1000L // 5 mins
                                            val appRepo = com.example.dumbphonelauncher.ServiceLocator.provideAppRepository(this@BlockingActivity)
                                            appRepo.updateApp(currentApp.copy(temporaryAccessEndTime = newTime))
                                            delay(500)
                                            finish() // Let them go back to the app
                                        }
                                    }
                                ) {
                                    Text("Allow for 5 minutes")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
}
