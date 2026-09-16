package com.example.dumbphonelauncher.services

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class ReminderActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val packageName = intent.getStringExtra("PACKAGE_NAME") ?: return finish()
        val appName = intent.getStringExtra("APP_NAME") ?: "the app"
        val minutes = intent.getIntExtra("MINUTES", 10)

        val prefs = com.example.dumbphonelauncher.ServiceLocator.provideLauncherPreferences(this)

        setContent {
            androidx.activity.compose.BackHandler { finish() }
            val bgColor by prefs.themeBgColor.collectAsState(initial = null)
            val textColor by prefs.themeTextColor.collectAsState(initial = null)
            
            val isSystemDark = isSystemInDarkTheme()
            val defaultBg = if (isSystemDark) Color.BLACK else Color.WHITE
            
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
                        Text(
                            text = "You've been here for $minutes minutes.",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Do you want to continue?",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(64.dp))
                        
                        Button(
                            onClick = {
                                // Close reminder and resume app
                                // In a real implementation we should snooze this reminder for X minutes.
                                finish()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Continue")
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(
                            onClick = {
                                val homeIntent = Intent(Intent.ACTION_MAIN)
                                homeIntent.addCategory(Intent.CATEGORY_HOME)
                                homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                startActivity(homeIntent)
                                finish()
                            }
                        ) {
                            Text("Leave $appName")
                        }
                    }
                    }
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recreate()
    }
}
