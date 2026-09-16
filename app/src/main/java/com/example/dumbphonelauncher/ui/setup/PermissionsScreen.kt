package com.example.dumbphonelauncher.ui.setup

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.dumbphonelauncher.util.PermissionUtils
import kotlinx.coroutines.delay

@Composable
fun PermissionsScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    var hasUsageStats by remember { mutableStateOf(PermissionUtils.hasUsageStatsPermission(context)) }
    var hasOverlay by remember { mutableStateOf(PermissionUtils.hasOverlayPermission(context)) }
    var hasNotification by remember { mutableStateOf(PermissionUtils.hasNotificationAccess(context) && com.example.dumbphonelauncher.services.DumbphoneNotificationListener.isConnected) }
    var hasPostNotification by remember { mutableStateOf(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU || androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPostNotification = isGranted
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                hasUsageStats = PermissionUtils.hasUsageStatsPermission(context)
                hasOverlay = PermissionUtils.hasOverlayPermission(context)
                hasNotification = PermissionUtils.hasNotificationAccess(context) && com.example.dumbphonelauncher.services.DumbphoneNotificationListener.isConnected
                hasPostNotification = android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU || androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Almost done.",
            style = MaterialTheme.typography.displaySmall
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "To track daily usage and enforce limits, the launcher needs a few permissions.",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(32.dp))

        PermissionItem(
            title = "Usage Access",
            description = "Allows the launcher to see how long you use apps.",
            isGranted = hasUsageStats,
            onRequest = {
                val intent = PermissionUtils.getUsageStatsIntent()
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        PermissionItem(
            title = "Display Over Other Apps",
            description = "Allows the launcher to block apps when your daily limit is reached.",
            isGranted = hasOverlay,
            onRequest = {
                val intent = PermissionUtils.getOverlayIntent(context)
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        PermissionItem(
            title = "Notification Access",
            description = "Allows the launcher to filter notifications.\n(NOTE: If the switch is already ON, you must turn it OFF and back ON again for it to work properly).",
            isGranted = hasNotification,
            onRequest = {
                val intent = PermissionUtils.getNotificationAccessIntent()
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        )

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            Spacer(modifier = Modifier.height(16.dp))
            PermissionItem(
                title = "Post Notifications",
                description = "Allows the launcher to show quiet notifications.",
                isGranted = hasPostNotification,
                onRequest = {
                    permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onComplete,
            enabled = hasUsageStats && hasOverlay && hasNotification,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Next")
        }
    }
}

@Composable
fun PermissionItem(
    title: String,
    description: String,
    isGranted: Boolean,
    onRequest: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(description, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(12.dp))
            if (isGranted) {
                Text("Granted", color = MaterialTheme.colorScheme.primary)
            } else {
                Button(onClick = onRequest) {
                    Text("Grant Permission")
                }
            }
        }
    }
}
