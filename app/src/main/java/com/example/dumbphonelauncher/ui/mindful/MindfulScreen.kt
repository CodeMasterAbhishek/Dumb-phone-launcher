package com.example.dumbphonelauncher.ui.mindful

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun MindfulScreen(
    appName: String,
    delaySeconds: Int,
    onProceed: () -> Unit,
    onCancel: () -> Unit
) {
    var secondsLeft by androidx.compose.runtime.saveable.rememberSaveable { mutableIntStateOf(delaySeconds) }
    // Steps: 0=Initial, 1=FollowUpSpecific, 2=FollowUpChecking, 3=Timer, 4=Ready
    var step by androidx.compose.runtime.saveable.rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(step) {
        if (step == 3) {
            if (secondsLeft <= 0) {
                step = 4
            } else {
                while (secondsLeft > 0) {
                    delay(1000L)
                    secondsLeft -= 1
                }
                step = 4
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (step) {
            0 -> {
                Text(
                    text = "You're about to open $appName.",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Take a moment.\nWhy are you opening it?",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(48.dp))
                Button(
                    onClick = { step = 1 },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("I have something specific to do")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { step = 2 },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("I'm just checking")
                }
                Spacer(modifier = Modifier.height(32.dp))
                TextButton(onClick = onCancel) {
                    Text("Nevermind")
                }
            }
            1 -> {
                Text(
                    text = "Are you sure you won't get distracted?",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Apps are designed to pull you in.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(48.dp))
                Button(
                    onClick = { step = 3 },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("I will stay focused")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { step = 3 },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("I'll try my best")
                }
                Spacer(modifier = Modifier.height(32.dp))
                TextButton(onClick = onCancel) {
                    Text("Actually, Nevermind")
                }
            }
            2 -> {
                Text(
                    text = "Is it really important right now?",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Mindless checking becomes a habit.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(48.dp))
                Button(
                    onClick = { step = 3 },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Yes, I need to check")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onCancel() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("You're right, Nevermind")
                }
            }
            3 -> {
                Text(
                    text = "Breathe.",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "$secondsLeft",
                    style = MaterialTheme.typography.displayLarge
                )
                Spacer(modifier = Modifier.height(48.dp))
                TextButton(onClick = onCancel) {
                    Text("Cancel")
                }
            }
            4 -> {
                Text(
                    text = "Ready?",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(48.dp))
                Button(
                    onClick = onProceed,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open $appName")
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onCancel) {
                    Text("Cancel")
                }
            }
        }
    }
}
