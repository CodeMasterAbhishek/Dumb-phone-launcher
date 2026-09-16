package com.example.dumbphonelauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.dumbphonelauncher.ui.MainViewModel
import com.example.dumbphonelauncher.ui.home.HomeScreen
import com.example.dumbphonelauncher.ui.setup.SetupScreen
import com.example.dumbphonelauncher.ui.theme.DumbPhoneLauncherTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(
            ServiceLocator.provideAppRepository(this),
            ServiceLocator.provideLauncherPreferences(this)
        )
    }

    private var isUnlocked = false
    private var unlockReceiver: android.content.BroadcastReceiver? = null
    private var lastSyncTimeMs: Long = 0L
    private var packageChangeReceiver: android.content.BroadcastReceiver? = null
    
    companion object {
        private const val SYNC_INTERVAL_MS = 5 * 60 * 1000L // 5 minutes
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Force high refresh rate for smooth scrolling (120Hz/90Hz)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            window.let { win ->
                val display = win.windowManager.defaultDisplay
                val modes = display.supportedModes
                val maxRefreshRate = modes.maxByOrNull { it.refreshRate }
                maxRefreshRate?.let { mode ->
                    val layoutParams = win.attributes
                    layoutParams.preferredDisplayModeId = mode.modeId
                    win.attributes = layoutParams
                }
            }
        }
        
        enableEdgeToEdge()
        
        isUnlocked = try {
            val userManager = getSystemService(android.os.UserManager::class.java)
            userManager.isUserUnlocked
        } catch (e: Exception) {
            true // If we can't check, assume unlocked and proceed
        }

        if (isUnlocked) {
            initApp()
        } else {
            setContent {
                Box(
                    modifier = Modifier.fillMaxSize().systemBarsPadding(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    // Empty or simple loading screen for Direct Boot mode
                }
                // Fallback: periodically check if the user has unlocked in case we miss the broadcast
                LaunchedEffect(Unit) {
                    while (!isUnlocked) {
                        kotlinx.coroutines.delay(1000)
                        try {
                            val um = getSystemService(android.os.UserManager::class.java)
                            if (um.isUserUnlocked) {
                                isUnlocked = true
                                unlockReceiver?.let {
                                    try { unregisterReceiver(it) } catch (_: Exception) {}
                                    unlockReceiver = null
                                }
                                initApp()
                            }
                        } catch (_: Exception) {}
                    }
                }
            }
            try {
                unlockReceiver = object : android.content.BroadcastReceiver() {
                    override fun onReceive(context: android.content.Context, intent: android.content.Intent) {
                        if (intent.action == android.content.Intent.ACTION_USER_UNLOCKED) {
                            isUnlocked = true
                            try { unregisterReceiver(this) } catch (_: Exception) {}
                            unlockReceiver = null
                            initApp()
                        }
                    }
                }
                val filter = android.content.IntentFilter(android.content.Intent.ACTION_USER_UNLOCKED)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    registerReceiver(unlockReceiver, filter, android.content.Context.RECEIVER_EXPORTED)
                } else {
                    registerReceiver(unlockReceiver, filter)
                }
            } catch (e: Exception) {
                // If receiver registration fails, the LaunchedEffect fallback above will still work
            }
        }
    }

    private fun initApp() {
        try {
            lastSyncTimeMs = System.currentTimeMillis()
            viewModel.syncApps()
            startUsageServiceIfPermitted()
        } catch (e: Exception) {
            // Non-fatal — continue to show UI even if background services fail to start
        }
        
        // Listen for app installs/uninstalls to keep the app list fresh
        // without needing the expensive full sync on every onResume
        registerPackageChangeReceiver()
        
        try {
            setContent {
                val bgColor by viewModel.themeBgColor.collectAsState()
                val textColor by viewModel.themeTextColor.collectAsState()
                
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
                
                val fontScale by viewModel.fontScale.collectAsState()
                val fontWeight by viewModel.fontWeight.collectAsState()
                val fontFamily by viewModel.fontFamily.collectAsState()
                
                DumbPhoneLauncherTheme(
                    customBgColor = bgColor,
                    customTextColor = textColor,
                    fontScale = fontScale,
                    fontWeight = fontWeight,
                    fontFamily = com.example.dumbphonelauncher.ui.theme.getFontFamilyByName(fontFamily)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Box(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
                            LauncherApp(viewModel)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Last-resort fallback: show a minimal recovery screen so the launcher process
            // stays alive and Android doesn't fall back to the stock launcher.
            setContent {
                Box(
                    modifier = Modifier.fillMaxSize().systemBarsPadding(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    androidx.compose.material3.Text("Loading...")
                }
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(2000)
                    recreate() // Retry initialization
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        if (isUnlocked) {
            val now = System.currentTimeMillis()
            if (now - lastSyncTimeMs > SYNC_INTERVAL_MS) {
                lastSyncTimeMs = now
                viewModel.syncApps()
            }
            startUsageServiceIfPermitted()
            ensureNotificationListenerConnected()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unlockReceiver?.let {
            try { unregisterReceiver(it) } catch (_: Exception) {}
            unlockReceiver = null
        }
        packageChangeReceiver?.let {
            try { unregisterReceiver(it) } catch (_: Exception) {}
            packageChangeReceiver = null
        }
    }
    
    private fun startUsageServiceIfPermitted() {
        if (com.example.dumbphonelauncher.util.PermissionUtils.hasUsageStatsPermission(this) &&
            com.example.dumbphonelauncher.util.PermissionUtils.hasOverlayPermission(this)) {
            val intent = android.content.Intent(this, com.example.dumbphonelauncher.services.AppUsageMonitorService::class.java)
            try {
                androidx.core.content.ContextCompat.startForegroundService(this, intent)
            } catch (e: Exception) {
                // Catch ForegroundServiceStartNotAllowedException on Android 12+ and other issues
            }
        }
    }
    
    private fun registerPackageChangeReceiver() {
        if (packageChangeReceiver != null) return // Already registered
        
        packageChangeReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context, intent: android.content.Intent) {
                // An app was installed, uninstalled, or updated — refresh the app list
                lastSyncTimeMs = System.currentTimeMillis()
                viewModel.syncApps()
            }
        }
        val filter = android.content.IntentFilter().apply {
            addAction(android.content.Intent.ACTION_PACKAGE_ADDED)
            addAction(android.content.Intent.ACTION_PACKAGE_REMOVED)
            addAction(android.content.Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(packageChangeReceiver, filter, android.content.Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(packageChangeReceiver, filter)
        }
    }

    private fun ensureNotificationListenerConnected() {
        if (com.example.dumbphonelauncher.util.PermissionUtils.hasNotificationAccess(this)) {
            val component = android.content.ComponentName(this, com.example.dumbphonelauncher.services.DumbphoneNotificationListener::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                try {
                    android.service.notification.NotificationListenerService.requestRebind(component)
                } catch (_: Exception) {}
            }
        }
    }
    
}

@Composable
fun LauncherApp(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val hasCompletedSetup by viewModel.hasCompletedSetup.collectAsState()
    val allApps by viewModel.allApps.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    var isDefaultLauncher by androidx.compose.runtime.remember { 
        androidx.compose.runtime.mutableStateOf(com.example.dumbphonelauncher.util.PermissionUtils.isDefaultLauncher(context)) 
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                isDefaultLauncher = com.example.dumbphonelauncher.util.PermissionUtils.isDefaultLauncher(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(hasCompletedSetup, isDefaultLauncher) {
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        
        if (!isDefaultLauncher && currentRoute != "require_default") {
            navController.navigate("require_default") {
                popUpTo(0)
            }
        } else if (isDefaultLauncher) {
            if (hasCompletedSetup && (currentRoute == "loading" || currentRoute == "setup" || currentRoute == "require_default")) {
                navController.navigate("home") {
                    popUpTo(0) // clear back stack
                }
            } else if (!hasCompletedSetup && currentRoute != "setup") {
                navController.navigate("setup") {
                    popUpTo(0)
                }
            }
        }
    }

    NavHost(navController = navController, startDestination = "loading") {
        composable("loading") {
            // Empty screen while loading state
        }
        composable("require_default") {
            com.example.dumbphonelauncher.ui.setup.DefaultLauncherScreen()
        }
        composable("setup") {
            SetupScreen(
                apps = allApps,
                onUpdateAppCategory = { app, category ->
                    viewModel.updateAppCategory(app, category)
                },
                onConfigureApp = { app ->
                    navController.navigate("appConfig/${app.packageName}")
                },
                onCompleteSetup = {
                    viewModel.completeSetup()
                }
            )
        }
        composable("home") {
            HomeScreen(
                apps = allApps,
                onNavigateToSettings = {
                    navController.navigate("settings")
                },
                onNavigateToMindful = { packageName ->
                    navController.navigate("mindful/$packageName")
                },
                onConfigureApp = { app ->
                    navController.navigate("appConfig/${app.packageName}")
                }
            )
        }
        composable("settings") {
            com.example.dumbphonelauncher.ui.settings.SettingsScreen(
                viewModel = viewModel,
                apps = allApps,
                onUpdateAppCategory = { app, category ->
                    viewModel.updateAppCategory(app, category)
                },
                onConfigureApp = { app ->
                    navController.navigate("appConfig/${app.packageName}")
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToMindful = { packageName ->
                    navController.navigate("mindful/$packageName")
                },
                onLaunchHiddenApp = { app ->
                    val intent = navController.context.packageManager.getLaunchIntentForPackage(app.packageName)
                    if (intent != null) {
                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        try {
                            navController.context.startActivity(intent)
                        } catch (e: Exception) {
                            // handle error
                        }
                    }
                }
            )
        }
        composable("mindful/{packageName}") { backStackEntry ->
            val packageName = backStackEntry.arguments?.getString("packageName")
            val app = allApps.find { it.packageName == packageName }
            if (app != null) {
                com.example.dumbphonelauncher.ui.mindful.MindfulScreen(
                    appName = app.name,
                    delaySeconds = if (app.mindfulOpeningDurationSeconds > 0) app.mindfulOpeningDurationSeconds else 30, // fallback
                    onProceed = {
                        com.example.dumbphonelauncher.services.NotificationStore.clearForPackage(app.packageName)
                        val intent = navController.context.packageManager.getLaunchIntentForPackage(app.packageName)
                        if (intent != null) {
                            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            navController.context.startActivity(intent)
                        }
                        navController.popBackStack("home", false)
                    },
                    onCancel = {
                        navController.popBackStack("home", false)
                    }
                )
            } else {
                // Should not happen, just pop
                navController.popBackStack()
            }
        }
        
        composable("appConfig/{packageName}") { backStackEntry ->
            val packageName = backStackEntry.arguments?.getString("packageName")
            val app = allApps.find { it.packageName == packageName }
            if (app != null) {
                com.example.dumbphonelauncher.ui.settings.AppConfigScreen(
                    app = app,
                    onSave = { updatedApp ->
                        viewModel.updateAppConfig(updatedApp)
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            } else {
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            }
        }
    }
}