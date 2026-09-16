package com.example.dumbphonelauncher.ui.settings

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dumbphonelauncher.data.AppConfig
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppConfigScreen(
    app: AppConfig,
    onSave: (AppConfig) -> Unit,
    onNavigateBack: () -> Unit
) {
    var customName by remember { mutableStateOf(app.name) }
    var mindfulEnabled by remember { mutableStateOf(app.mindfulOpeningEnabled) }
    var mindfulDuration by remember { mutableStateOf(app.mindfulOpeningDurationSeconds.toString()) }
    var dailyLimit by remember { mutableStateOf(app.dailyLimitMinutes?.toString() ?: "") }
    var continuousReminder by remember { mutableStateOf(app.continuousUsageReminderMinutes?.toString() ?: "") }
    
    val notificationModes = listOf("NORMAL", "QUIET", "WHEN_OPENED", "BLOCKED")
    var selectedNotificationMode by remember { mutableStateOf(app.notificationMode) }
    var expandedNotificationMenu by remember { mutableStateOf(false) }
    
    val categories = listOf(
        com.example.dumbphonelauncher.data.AppCategory.ESSENTIAL, 
        com.example.dumbphonelauncher.data.AppCategory.CONTROLLED, 
        com.example.dumbphonelauncher.data.AppCategory.SECONDARY,
        com.example.dumbphonelauncher.data.AppCategory.HIDDEN
    )
    var selectedCategory by remember { mutableStateOf(app.category) }
    var expandedCategoryMenu by remember { mutableStateOf(false) }
    var showOnHome by remember { mutableStateOf(app.showOnHomeScreen) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configure App") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        onSave(
                            app.copy(
                                name = if (customName.isNotBlank()) customName else app.name,
                                category = selectedCategory,
                                showOnHomeScreen = showOnHome,
                                mindfulOpeningEnabled = mindfulEnabled,
                                mindfulOpeningDurationSeconds = (mindfulDuration.toIntOrNull() ?: 0).coerceAtLeast(0),
                                dailyLimitMinutes = dailyLimit.toIntOrNull()?.coerceAtLeast(1),
                                continuousUsageReminderMinutes = continuousReminder.toIntOrNull()?.coerceAtLeast(1),
                                notificationMode = selectedNotificationMode
                            )
                        )
                        onNavigateBack()
                    }) {
                        Text("Save")
                    }
                }
            )
        }
    ) { padding ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            OutlinedTextField(
                value = customName,
                onValueChange = { customName = it },
                label = { Text("App Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("App Visibility", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            
            val categoryLabels = mapOf(
                com.example.dumbphonelauncher.data.AppCategory.ESSENTIAL to "Homepage / Quick access",
                com.example.dumbphonelauncher.data.AppCategory.CONTROLLED to "App drawer",
                com.example.dumbphonelauncher.data.AppCategory.SECONDARY to "Utility / Background (Hidden)",
                com.example.dumbphonelauncher.data.AppCategory.HIDDEN to "Strictly Blocked (Hidden)"
            )
            
            ExposedDropdownMenuBox(
                expanded = expandedCategoryMenu,
                onExpandedChange = { expandedCategoryMenu = !expandedCategoryMenu }
            ) {
                OutlinedTextField(
                    value = categoryLabels[selectedCategory] ?: selectedCategory.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategoryMenu) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedCategoryMenu,
                    onDismissRequest = { expandedCategoryMenu = false }
                ) {
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(categoryLabels[category] ?: category.name) },
                            onClick = {
                                selectedCategory = category
                                // Auto-sync the showOnHome property just in case
                                showOnHome = category == com.example.dumbphonelauncher.data.AppCategory.ESSENTIAL
                                expandedCategoryMenu = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Mindful Opening", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Switch(checked = mindfulEnabled, onCheckedChange = { mindfulEnabled = it })
            }
            if (mindfulEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                PresetDropdown(
                    label = "Duration",
                    unit = "seconds",
                    currentValue = mindfulDuration,
                    onValueChange = { mindfulDuration = it },
                    presets = mapOf(
                        "5 seconds" to "5",
                        "10 seconds" to "10",
                        "15 seconds" to "15",
                        "30 seconds" to "30"
                    )
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("Daily Limit", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            PresetDropdown(
                label = "Daily Limit",
                unit = "minutes",
                currentValue = dailyLimit,
                onValueChange = { dailyLimit = it },
                presets = mapOf(
                    "No limit" to "",
                    "15 minutes" to "15",
                    "30 minutes" to "30",
                    "1 hour" to "60",
                    "2 hours" to "120"
                )
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            Text("Continuous Usage Reminder", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            PresetDropdown(
                label = "Continuous Reminder",
                unit = "minutes",
                currentValue = continuousReminder,
                onValueChange = { continuousReminder = it },
                presets = mapOf(
                    "Disable" to "",
                    "5 minutes" to "5",
                    "10 minutes" to "10",
                    "15 minutes" to "15",
                    "30 minutes" to "30"
                )
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text("Notifications", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            
            ExposedDropdownMenuBox(
                expanded = expandedNotificationMenu,
                onExpandedChange = { expandedNotificationMenu = !expandedNotificationMenu }
            ) {
                OutlinedTextField(
                    value = when (selectedNotificationMode) {
                        "NORMAL" -> "Normal"
                        "QUIET" -> "Quiet (Silent in shade)"
                        "WHEN_OPENED" -> "Only when opened"
                        "BLOCKED" -> "Blocked"
                        else -> selectedNotificationMode
                    },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Mode") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedNotificationMenu) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedNotificationMenu,
                    onDismissRequest = { expandedNotificationMenu = false }
                ) {
                    val modeDescriptions = mapOf(
                        "NORMAL" to "Shows notifications normally",
                        "QUIET" to "Silent, minimized in notification shade",
                        "WHEN_OPENED" to "Hidden until you open the app",
                        "BLOCKED" to "Completely blocked, never shown"
                    )
                    notificationModes.forEach { mode ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        when (mode) {
                                            "NORMAL" -> "Normal"
                                            "QUIET" -> "Quiet"
                                            "WHEN_OPENED" -> "Only when opened"
                                            "BLOCKED" -> "Blocked"
                                            else -> mode
                                        },
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        modeDescriptions[mode] ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                selectedNotificationMode = mode
                                expandedNotificationMenu = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            Text("System Options", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
            
            val context = androidx.compose.ui.platform.LocalContext.current
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val scope = rememberCoroutineScope()
                OutlinedButton(
                    onClick = {
                        val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = android.net.Uri.parse("package:${app.packageName}")
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        try {
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            // Some custom ROMs may not support this intent
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("App Info")
                }
                
                OutlinedButton(
                    onClick = {
                        val intent = android.content.Intent(android.content.Intent.ACTION_DELETE).apply {
                            data = android.net.Uri.parse("package:${app.packageName}")
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        try {
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            // Some devices may not support this intent
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Uninstall")
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetDropdown(
    label: String,
    unit: String = "",
    currentValue: String,
    onValueChange: (String) -> Unit,
    presets: Map<String, String> // Display Name -> Value
) {
    var expanded by remember { mutableStateOf(false) }
    
    var isCustomMode by remember { 
        mutableStateOf((currentValue.isNotEmpty() && !presets.values.contains(currentValue)) || currentValue == "0") 
    }

    val displayText = if (isCustomMode) {
        currentValue
    } else {
        presets.entries.find { it.value == currentValue }?.key ?: presets.keys.firstOrNull() ?: ""
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = displayText,
            onValueChange = { newValue ->
                if (isCustomMode) {
                    // Prevent 024 by trimming leading zeros if length > 1
                    val cleaned = if (newValue.length > 1 && newValue.startsWith("0")) {
                        newValue.trimStart('0').ifEmpty { "0" }
                    } else {
                        newValue
                    }
                    onValueChange(cleaned)
                }
            },
            readOnly = !isCustomMode,
            label = { 
                if (isCustomMode && unit.isNotEmpty()) {
                    Text("$label (Custom in $unit)")
                } else if (isCustomMode) {
                    Text("$label (Custom value)")
                } else {
                    Text(label)
                }
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            presets.forEach { (text, value) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        isCustomMode = false
                        onValueChange(value)
                        expanded = false
                    }
                )
            }
            DropdownMenuItem(
                text = { Text("Custom...") },
                onClick = {
                    isCustomMode = true
                    // Clear the current value so they can type fresh
                    onValueChange("")
                    expanded = false
                }
            )
        }
    }
}
