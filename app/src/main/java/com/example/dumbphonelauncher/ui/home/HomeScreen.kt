package com.example.dumbphonelauncher.ui.home

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.dumbphonelauncher.data.AppCategory
import com.example.dumbphonelauncher.data.AppConfig
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    apps: List<AppConfig>,
    onNavigateToSettings: () -> Unit,
    onNavigateToMindful: (String) -> Unit,
    onConfigureApp: (AppConfig) -> Unit
) {
    val context = LocalContext.current
    
    // Quick access are ONLY those categorized as ESSENTIAL
    val quickAccessApps = remember(apps) {
        apps.filter { it.category == AppCategory.ESSENTIAL }
            .sortedBy { it.name }
    }
        
    // Drawer contains all non-hidden apps (ESSENTIAL + CONTROLLED)
    val allAllowedApps = remember(apps) {
        apps.filter { 
            it.category == AppCategory.ESSENTIAL || 
            it.category == AppCategory.CONTROLLED 
        }.sortedBy { it.name }
    }

    val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    var currentTime by remember { androidx.compose.runtime.mutableLongStateOf(System.currentTimeMillis()) }
    
    androidx.compose.runtime.DisposableEffect(context) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(c: android.content.Context?, intent: Intent?) {
                currentTime = System.currentTimeMillis()
            }
        }
        val filter = android.content.IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        context.registerReceiver(receiver, filter)
        
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }
    val currentDate = Date(currentTime)
    
    androidx.activity.compose.BackHandler {
        // Do nothing on back press at home screen to prevent exiting launcher
    }
    
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { 2 })

    androidx.compose.foundation.pager.HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        if (page == 0) {
            // Main Home Screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = timeFormat.format(currentDate),
                    style = MaterialTheme.typography.displayMedium
                )
                Text(
                    text = dateFormat.format(currentDate),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(48.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    if (quickAccessApps.isEmpty()) {
                        item {
                            Text(
                                "Swipe left for App Drawer.\nLong-press an app to add it here.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    } else {
                        items(quickAccessApps, key = { it.packageName }) { app ->
                            AppListItem(
                                app = app,
                                onClick = {
                                    if (app.mindfulOpeningEnabled) {
                                        onNavigateToMindful(app.packageName)
                                    } else {
                                        launchApp(context, app.packageName)
                                    }
                                },
                                onLongClick = { onConfigureApp(app) }
                            )
                        }
                    }
                }
            }
        } else {
            // App Drawer
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "App Drawer",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(allAllowedApps, key = { it.packageName }) { app ->
                        AppListItem(
                            app = app,
                            onClick = {
                                if (app.mindfulOpeningEnabled) {
                                    onNavigateToMindful(app.packageName)
                                } else {
                                    launchApp(context, app.packageName)
                                }
                            },
                            onLongClick = { onConfigureApp(app) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun AppListItem(
    app: AppConfig,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = app.name,
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

// Note: AppUsageMonitorService handles limit enforcement in the background.
// If an app is launched that exceeds limits, the service will detect it within ~2s
// and overlay a BlockingActivity. This is the intended architecture.
private fun launchApp(context: Context, packageName: String) {
    com.example.dumbphonelauncher.services.NotificationStore.clearForPackage(packageName)
    val intent = context.packageManager.getLaunchIntentForPackage(packageName)
    if (intent != null) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
