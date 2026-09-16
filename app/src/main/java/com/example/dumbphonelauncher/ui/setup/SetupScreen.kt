package com.example.dumbphonelauncher.ui.setup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.items

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import com.example.dumbphonelauncher.data.AppCategory
import com.example.dumbphonelauncher.data.AppConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    apps: List<AppConfig>,
    onUpdateAppCategory: (AppConfig, AppCategory) -> Unit,
    onConfigureApp: (AppConfig) -> Unit,
    onCompleteSetup: () -> Unit
) {
    val context = LocalContext.current
    
    var hasDefaultLauncher by remember { mutableStateOf(com.example.dumbphonelauncher.util.PermissionUtils.isDefaultLauncher(context)) }
    var hasOtherPermissions by remember {
        mutableStateOf(
            com.example.dumbphonelauncher.util.PermissionUtils.hasUsageStatsPermission(context) &&
            com.example.dumbphonelauncher.util.PermissionUtils.hasOverlayPermission(context) &&
            com.example.dumbphonelauncher.util.PermissionUtils.hasNotificationAccess(context)
        )
    }

    // Skip permissions screen if all permissions already granted
    var showPermissions by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(!hasOtherPermissions) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                hasDefaultLauncher = com.example.dumbphonelauncher.util.PermissionUtils.isDefaultLauncher(context)
                hasOtherPermissions = com.example.dumbphonelauncher.util.PermissionUtils.hasUsageStatsPermission(context) &&
                                      com.example.dumbphonelauncher.util.PermissionUtils.hasOverlayPermission(context) &&
                                      com.example.dumbphonelauncher.util.PermissionUtils.hasNotificationAccess(context)
                if (!hasOtherPermissions) {
                    showPermissions = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (!hasDefaultLauncher) {
        DefaultLauncherScreen()
    } else if (showPermissions) {
        PermissionsScreen(onComplete = { showPermissions = false })
    } else {
        AppCategorizationScreen(
            apps = apps,
            onUpdateAppCategory = onUpdateAppCategory,
            onConfigureApp = onConfigureApp,
            onFinish = onCompleteSetup
        )
    }
}

@Composable
fun DefaultLauncherScreen() {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome.",
            style = MaterialTheme.typography.displaySmall
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "To turn your smartphone into a mindful device, this app must become your Default Home Screen.",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = {
                val intent = android.content.Intent(android.provider.Settings.ACTION_HOME_SETTINGS)
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(intent)
                } catch (_: Exception) {
                    // Some custom ROMs may not support ACTION_HOME_SETTINGS
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Set as Default Launcher")
        }
    }
}

@Composable
fun WelcomeScreen(onContinue: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Turn your smartphone into the phone you actually need.",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Essential apps will feel normal and fast. Potentially addictive apps will require intention. Everything else will disappear.",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(onClick = onContinue) {
            Text("Start Setup")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppCategorizationScreen(
    apps: List<AppConfig>,
    onUpdateAppCategory: (AppConfig, AppCategory) -> Unit,
    onConfigureApp: (AppConfig) -> Unit,
    onFinish: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Select your apps") })
        },
        bottomBar = {
            val hasSelectedApps = apps.any { it.category != AppCategory.HIDDEN && it.category != AppCategory.UNCATEGORIZED }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = {
                        // Ensure any UNCATEGORIZED apps become HIDDEN upon finish
                        apps.filter { it.category == AppCategory.UNCATEGORIZED }.forEach { 
                            onUpdateAppCategory(it, AppCategory.HIDDEN) 
                        }
                        onFinish()
                    },
                    enabled = hasSelectedApps
                ) {
                    Text(if (hasSelectedApps) "Finish Setup" else "Select at least one app")
                }
            }
        }
    ) { padding ->
        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            items(apps, key = { it.packageName }) { app ->
                AppSelectionItem(
                    app = app,
                    onToggle = { isSelected ->
                        val newCategory = if (isSelected) AppCategory.CONTROLLED else AppCategory.HIDDEN
                        onUpdateAppCategory(app, newCategory)
                    }
                )
            }
        }
    }
}

@Composable
fun AppSelectionItem(
    app: AppConfig,
    onToggle: (Boolean) -> Unit
) {
    val isSelected = app.category == AppCategory.ESSENTIAL || app.category == AppCategory.CONTROLLED || app.category == AppCategory.SECONDARY
    val context = LocalContext.current
    val packageManager = context.packageManager
    var iconBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    
    LaunchedEffect(app.packageName) {
        try {
            val drawable = packageManager.getApplicationIcon(app.packageName)
            iconBitmap = com.example.dumbphonelauncher.util.ImageUtils.drawableToImageBitmap(drawable)
        } catch (e: Exception) {
            // Ignore
        }
    }

    val colorMatrix = remember(isSelected) {
        if (isSelected) {
            androidx.compose.ui.graphics.ColorMatrix() // Normal colors
        } else {
            androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(0f) } // Grayscale
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!isSelected) },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            if (iconBitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = iconBitmap!!,
                    contentDescription = app.name,
                    modifier = Modifier.size(56.dp),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.colorMatrix(colorMatrix)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Placeholder if no icon
                }
            }
            if (isSelected) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 8.dp, y = (-8).dp)
                        .size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = app.name,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}
