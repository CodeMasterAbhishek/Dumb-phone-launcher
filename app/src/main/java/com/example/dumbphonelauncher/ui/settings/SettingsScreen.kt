package com.example.dumbphonelauncher.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.dumbphonelauncher.data.AppCategory
import com.example.dumbphonelauncher.data.AppConfig
import com.example.dumbphonelauncher.ui.MainViewModel
import com.example.dumbphonelauncher.ui.setup.AppCategorizationScreen
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    apps: List<AppConfig>,
    onUpdateAppCategory: (AppConfig, AppCategory) -> Unit,
    onConfigureApp: (AppConfig) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToMindful: (String) -> Unit = {},
    onLaunchHiddenApp: (AppConfig) -> Unit = {}
) {
    var currentSubScreen by remember { mutableStateOf("main") }

    val currentPin by viewModel.settingsPin.collectAsState()
    val fontScale by viewModel.fontScale.collectAsState()
    val fontWeight by viewModel.fontWeight.collectAsState()
    val fontFamily by viewModel.fontFamily.collectAsState()

    val additionsCount by viewModel.appAdditionsCount.collectAsState()
    val lockUntil by viewModel.appAdditionsLockUntil.collectAsState()
    val now = System.currentTimeMillis()
    val isLockedByTime = now < lockUntil
    val isLockedPermanently = additionsCount >= 3
    val isLocked = isLockedByTime || isLockedPermanently
    
    val lockRemainingMillis = lockUntil - now
    val lockDays = TimeUnit.MILLISECONDS.toDays(lockRemainingMillis)
    val lockHours = TimeUnit.MILLISECONDS.toHours(lockRemainingMillis) % 24

    androidx.activity.compose.BackHandler(enabled = currentSubScreen != "main") {
        currentSubScreen = "main"
    }

        if (currentSubScreen == "add_apps") {
            AppCategorizationScreen(
                apps = apps,
                onUpdateAppCategory = onUpdateAppCategory,
                onConfigureApp = onConfigureApp,
                onFinish = { 
                    viewModel.recordAppAdditionAndLock()
                    currentSubScreen = "main" 
                }
            )
        } else if (currentSubScreen == "set_pin_add_apps") {
            SetPinForAddAppsScreen(
                onPinSet = { pin ->
                    viewModel.setSettingsPin(pin)
                    currentSubScreen = "add_apps"
                },
                onNavigateBack = { currentSubScreen = "main" }
            )
        } else if (currentSubScreen == "enter_pin_add_apps") {
            EnterPinForAddAppsScreen(
                expectedPin = currentPin ?: "",
                onPinCorrect = {
                    currentSubScreen = "add_apps"
                },
                onNavigateBack = { currentSubScreen = "main" }
            )
        } else if (currentSubScreen == "view_hidden_apps") {
            HiddenAppsScreen(
                apps = apps,
                onConfigureApp = onConfigureApp,
                onNavigateBack = { currentSubScreen = "main" },
                onNavigateToMindful = onNavigateToMindful,
                onLaunchHiddenApp = onLaunchHiddenApp
            )
        } else if (currentSubScreen == "theme") {
            ThemePickerScreen(
                viewModel = viewModel,
                onNavigateBack = { currentSubScreen = "main" }
            )
        } else if (currentSubScreen == "font") {
            FontScreen(
                viewModel = viewModel,
                onNavigateBack = { currentSubScreen = "main" }
            )
        } else {
            var showMenu by remember { mutableStateOf(false) }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Launcher Settings") },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = { showMenu = !showMenu }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "More Options")
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { 
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("Add Apps", style = MaterialTheme.typography.titleMedium)
                                                if (isLocked) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Icon(Icons.Filled.Lock, contentDescription = "Locked", modifier = Modifier.size(16.dp))
                                                }
                                            }
                                            if (isLockedPermanently) {
                                                Text("Permanently Locked", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                            } else if (isLockedByTime) {
                                                Text("Locked for $lockDays days, $lockHours hrs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                            } else {
                                                Text("${3 - additionsCount} chances remaining", style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    },
                                    onClick = { 
                                        showMenu = false
                                        if (!isLocked) {
                                            if (currentPin == null) {
                                                currentSubScreen = "set_pin_add_apps"
                                            } else {
                                                currentSubScreen = "enter_pin_add_apps"
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    )
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    
                    // Option 2: Hidden Apps (Read-only list)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.onBackground),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth().clickable { currentSubScreen = "view_hidden_apps" },
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Hidden Apps", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("View the list of apps you have hidden.", style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Option 3: Theme / Colors
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.onBackground),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth().clickable { currentSubScreen = "theme" },
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Appearance & Colors", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Change launcher color and toggle Dark Mode.", style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    


                    // Option 5: Font
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.onBackground),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth().clickable { currentSubScreen = "font" },
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Font", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            val sizeText = when (fontScale) {
                                0.8f -> "Small"
                                1.0f -> "Normal"
                                1.2f -> "Large"
                                else -> "Extra Large"
                            }
                            val weightText = when (fontWeight) {
                                300 -> "Light"
                                400 -> "Regular"
                                500 -> "Medium"
                                else -> "Bold"
                            }
                            val familyText = when (fontFamily) {
                                "MONOSPACE" -> "Monospace"
                                "SERIF" -> "Serif"
                                "SANS_SERIF" -> "Sans-Serif"
                                "CURSIVE" -> "Cursive"
                                else -> "System"
                            }
                            Text("$familyText • $sizeText • $weightText", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemePickerScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val currentBgColor by viewModel.themeBgColor.collectAsState()
    val currentTextColor by viewModel.themeTextColor.collectAsState()
    
    var selectedBgColor by remember { mutableStateOf(currentBgColor?.let { Color(it) }) }
    var selectedTextColor by remember { mutableStateOf(currentTextColor?.let { Color(it) }) }
    var autoText by remember { mutableStateOf(true) }

    val presetColors = listOf(
        Color.Black,
        Color.White,
        Color(0xFF8B0000), // Dark Red
        Color(0xFF000080), // Navy Blue
        Color(0xFF006400), // Dark Green
        Color(0xFF4B0082)  // Indigo
    )

    fun calculateComplementaryTextColor(bg: Color): Color {
        val luminance = 0.299 * bg.red + 0.587 * bg.green + 0.114 * bg.blue
        return if (luminance > 0.5) Color.Black else Color.White
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appearance & Colors") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Sticky Preview Banner
            Surface(
                shadowElevation = 8.dp,
                color = selectedBgColor ?: MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val textColor = selectedTextColor ?: MaterialTheme.colorScheme.onBackground
                    Text("12:34", style = MaterialTheme.typography.displayMedium, color = textColor)
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Phone", style = MaterialTheme.typography.titleMedium, color = textColor)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Messages", style = MaterialTheme.typography.titleMedium, color = textColor)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Maps", style = MaterialTheme.typography.titleMedium, color = textColor)
                    }
                }
            }

            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))
            
            ElevatedCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Background Color", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val bgScrollState = rememberScrollState()
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp), 
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().horizontalScroll(bgScrollState)
                    ) {
                        presetColors.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(color, shape = CircleShape)
                                    .border(
                                        width = if (selectedBgColor == color) 3.dp else 1.dp,
                                        color = if (selectedBgColor == color) MaterialTheme.colorScheme.primary else Color.Gray,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        selectedBgColor = color
                                        if (autoText) {
                                            selectedTextColor = calculateComplementaryTextColor(color)
                                        }
                                    }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Custom Color (RGB Mixer)", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    var red by remember(selectedBgColor) { mutableStateOf(selectedBgColor?.red ?: 0f) }
                    var green by remember(selectedBgColor) { mutableStateOf(selectedBgColor?.green ?: 0f) }
                    var blue by remember(selectedBgColor) { mutableStateOf(selectedBgColor?.blue ?: 0f) }

                    val onColorChange = {
                        val newColor = Color(red, green, blue)
                        selectedBgColor = newColor
                        if (autoText) {
                            selectedTextColor = calculateComplementaryTextColor(newColor)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("R", modifier = Modifier.width(24.dp), style = MaterialTheme.typography.bodyMedium)
                        Slider(value = red, onValueChange = { red = it; onColorChange() }, modifier = Modifier.weight(1f), colors = SliderDefaults.colors(thumbColor = Color(0xFFE53935), activeTrackColor = Color(0xFFE53935)))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("G", modifier = Modifier.width(24.dp), style = MaterialTheme.typography.bodyMedium)
                        Slider(value = green, onValueChange = { green = it; onColorChange() }, modifier = Modifier.weight(1f), colors = SliderDefaults.colors(thumbColor = Color(0xFF43A047), activeTrackColor = Color(0xFF43A047)))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("B", modifier = Modifier.width(24.dp), style = MaterialTheme.typography.bodyMedium)
                        Slider(value = blue, onValueChange = { blue = it; onColorChange() }, modifier = Modifier.weight(1f), colors = SliderDefaults.colors(thumbColor = Color(0xFF1E88E5), activeTrackColor = Color(0xFF1E88E5)))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            ElevatedCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Text Color", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { 
                        autoText = !autoText
                        if (autoText && selectedBgColor != null) {
                            selectedTextColor = calculateComplementaryTextColor(selectedBgColor!!)
                        }
                    }) {
                        Checkbox(checked = autoText, onCheckedChange = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Auto-calculate contrasting color")
                    }
                    
                    if (!autoText) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        val textScrollState = rememberScrollState()
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth().horizontalScroll(textScrollState)
                        ) {
                            val textPresets = listOf(Color.Black, Color.White, Color.Gray, Color(0xFFFFF176), Color(0xFF8B0000), Color(0xFF000080), Color(0xFF006400))
                            textPresets.forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(color, shape = CircleShape)
                                        .border(
                                            width = if (selectedTextColor == color) 3.dp else 1.dp,
                                            color = if (selectedTextColor == color) MaterialTheme.colorScheme.primary else Color.Gray,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedTextColor = color }
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Custom Text Color (RGB Mixer)", style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        var tRed by remember(selectedTextColor) { mutableStateOf(selectedTextColor?.red ?: 0f) }
                        var tGreen by remember(selectedTextColor) { mutableStateOf(selectedTextColor?.green ?: 0f) }
                        var tBlue by remember(selectedTextColor) { mutableStateOf(selectedTextColor?.blue ?: 0f) }

                        val onTextColorChange = {
                            selectedTextColor = Color(tRed, tGreen, tBlue)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("R", modifier = Modifier.width(24.dp), style = MaterialTheme.typography.bodyMedium)
                            Slider(value = tRed, onValueChange = { tRed = it; onTextColorChange() }, modifier = Modifier.weight(1f), colors = SliderDefaults.colors(thumbColor = Color(0xFFE53935), activeTrackColor = Color(0xFFE53935)))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("G", modifier = Modifier.width(24.dp), style = MaterialTheme.typography.bodyMedium)
                            Slider(value = tGreen, onValueChange = { tGreen = it; onTextColorChange() }, modifier = Modifier.weight(1f), colors = SliderDefaults.colors(thumbColor = Color(0xFF43A047), activeTrackColor = Color(0xFF43A047)))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("B", modifier = Modifier.width(24.dp), style = MaterialTheme.typography.bodyMedium)
                            Slider(value = tBlue, onValueChange = { tBlue = it; onTextColorChange() }, modifier = Modifier.weight(1f), colors = SliderDefaults.colors(thumbColor = Color(0xFF1E88E5), activeTrackColor = Color(0xFF1E88E5)))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val isDark = androidx.compose.foundation.isSystemInDarkTheme()
            val defaultBg = if (isDark) Color.Black else Color.White
            val defaultText = if (isDark) Color.White else Color.Black
            val bgInt = selectedBgColor?.toArgb() ?: defaultBg.toArgb()
            val textInt = selectedTextColor?.toArgb() ?: defaultText.toArgb()
            
            val contrast = androidx.core.graphics.ColorUtils.calculateContrast(textInt, bgInt)
            val isVisible = contrast >= 1.5 // Require at least 1.5:1 contrast ratio
            
            if (!isVisible) {
                Text(
                    text = "Background and text colors are too similar! Please adjust them so you can see your screen.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            Button(
                onClick = {
                    viewModel.setThemeColors(bgInt, textInt)
                    onNavigateBack()
                },
                enabled = isVisible,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(56.dp)
            ) {
                Text("Apply Theme", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HiddenAppsScreen(
    apps: List<AppConfig>,
    onConfigureApp: (AppConfig) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToMindful: (String) -> Unit = {},
    onLaunchHiddenApp: (AppConfig) -> Unit
) {
    // Shows only apps that the user actively chose to hide in the "Hidden Apps" drawer (SECONDARY category)
    val hiddenApps = apps.filter { it.category == AppCategory.SECONDARY }
    val context = androidx.compose.ui.platform.LocalContext.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hidden Apps") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (hiddenApps.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No hidden apps.", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                items(hiddenApps.size, key = { index -> hiddenApps[index].packageName }) { index ->
                    val app = hiddenApps[index]
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    if (app.mindfulOpeningEnabled) {
                                        onNavigateToMindful(app.packageName)
                                    } else {
                                        onLaunchHiddenApp(app)
                                    }
                                },
                                onLongClick = {
                                    onConfigureApp(app)
                                }
                            )
                            .padding(vertical = 12.dp, horizontal = 16.dp)
                    ) {
                        Text(
                            text = app.name,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetPinForAddAppsScreen(
    onPinSet: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Set PIN for Add Apps") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Accountability Partner Recommended",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Give your phone to a person that you trust (a friend, partner, or family member) to set this PIN.\n\n" +
                        "If you set the PIN yourself, it is too easy to bypass in a moment of temptation. Having someone else hold the PIN adds real friction to protect your digital wellbeing.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Set a 4-digit PIN",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = pin,
                onValueChange = { input ->
                    if (input.length <= 4 && input.all { it.isDigit() }) {
                        pin = input
                        isError = false
                        if (input.length == 4) {
                            onPinSet(input)
                        }
                    }
                },
                label = { Text("4-digit PIN") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                isError = isError,
                singleLine = true,
                supportingText = if (isError) {
                    { Text("PIN must be exactly 4 digits") }
                } else null
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (pin.length == 4) {
                        onPinSet(pin)
                    } else {
                        isError = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = pin.length == 4
            ) {
                Text("Save PIN & Continue")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnterPinForAddAppsScreen(
    expectedPin: String,
    onPinCorrect: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Enter PIN") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Ask your trusted person to enter the PIN to unlock Add Apps.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Enter the 4-digit PIN",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = pin,
                onValueChange = { input ->
                    if (input.length <= 4 && input.all { it.isDigit() }) {
                        pin = input
                        isError = false
                        if (input.length == 4) {
                            if (input == expectedPin) {
                                onPinCorrect()
                            } else {
                                isError = true
                                pin = ""
                            }
                        }
                    }
                },
                label = { Text("4-digit PIN") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                isError = isError,
                singleLine = true,
                supportingText = if (isError) {
                    { Text("Incorrect PIN. Please try again.") }
                } else null
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FontScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val fontScale by viewModel.fontScale.collectAsState()
    val fontWeight by viewModel.fontWeight.collectAsState()
    val fontFamily by viewModel.fontFamily.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Font Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Preview:", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text = "The quick brown fox jumps over the lazy dog",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            
            Spacer(modifier = Modifier.height(28.dp))
            
            // Section 1: Font Family
            Text("Font Family", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            
            val fontFamilies = listOf(
                "DEFAULT" to "System Default",
                "LEXEND" to "Lexend",
                "SPACE_MONO" to "Space Mono",
                "PLAYFAIR_DISPLAY" to "Playfair Display",
                "OSWALD" to "Oswald",
                "LATO" to "Lato",
                "FIRA_CODE" to "Fira Code",
                "ATKINSON" to "Atkinson Hyperlegible",
                "CINZEL" to "Cinzel",
                "QUICKSAND" to "Quicksand",
                "UBUNTU" to "Ubuntu",
                "JOSEFIN_SANS" to "Josefin Sans",
                "MONOSPACE" to "Monospace",
                "SERIF" to "Serif",
                "SANS_SERIF" to "Sans-Serif",
                "CURSIVE" to "Cursive"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                fontFamilies.forEach { (familyKey, familyName) ->
                    FilterChip(
                        selected = fontFamily == familyKey,
                        onClick = { viewModel.setFontFamily(familyKey) },
                        label = {
                            Text(
                                text = familyName,
                                maxLines = 1,
                                softWrap = false,
                                fontFamily = com.example.dumbphonelauncher.ui.theme.getFontFamilyByName(familyKey)
                            )
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(28.dp))
            
            // Section 2: Font Size
            Text("Font Size", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            
            val sizeLabels = mapOf(0.8f to "Small", 1.0f to "Default", 1.2f to "Large", 1.4f to "Extra Large")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(sizeLabels[0.8f]!!, style = MaterialTheme.typography.bodySmall)
                Text(sizeLabels[1.4f]!!, style = MaterialTheme.typography.bodySmall)
            }
            Slider(
                value = fontScale,
                onValueChange = { viewModel.setFontScale(it) },
                valueRange = 0.8f..1.4f,
                steps = 2
            )
            Text(
                text = "Current: ${sizeLabels[fontScale] ?: "Custom"}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            
            Spacer(modifier = Modifier.height(28.dp))
            
            // Section 3: Font Weight
            Text("Font Weight", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            
            val weights = listOf(300 to "Light", 400 to "Regular", 500 to "Medium", 700 to "Bold")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                weights.forEach { (weightValue, label) ->
                    FilterChip(
                        selected = fontWeight == weightValue,
                        onClick = { viewModel.setFontWeight(weightValue) },
                        label = {
                            Text(
                                text = label,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    )
                }
            }
        }
    }
}
