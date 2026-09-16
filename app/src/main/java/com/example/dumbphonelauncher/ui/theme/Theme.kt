package com.example.dumbphonelauncher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable


// Note: The theme function below builds its own color schemes inline from
// user-configured customBgColor/customTextColor. No predefined schemes are needed.



@Composable
fun DumbPhoneLauncherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disable dynamic color to stop purple/system colors bleeding in
    customBgColor: Int? = null,
    customTextColor: Int? = null,
    fontScale: Float = 1.0f,
    fontWeight: Int = 400,
    fontFamily: androidx.compose.ui.text.font.FontFamily = androidx.compose.ui.text.font.FontFamily.Default,
    content: @Composable () -> Unit
) {
    val defaultBg = if (darkTheme) Color.Black else Color.White
    val defaultOnBg = if (darkTheme) Color.White else Color.Black

    val bg = customBgColor?.let { Color(it) } ?: defaultBg
    val onBg = customTextColor?.let { Color(it) } ?: defaultOnBg

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = onBg,
            onPrimary = bg,
            secondary = onBg,
            onSecondary = bg,
            tertiary = onBg,
            onTertiary = bg,
            background = bg,
            surface = bg,
            surfaceVariant = bg,
            surfaceContainer = bg,
            surfaceContainerHigh = bg,
            surfaceContainerHighest = bg,
            surfaceContainerLow = bg,
            surfaceContainerLowest = bg,
            onBackground = onBg,
            onSurface = onBg,
            onSurfaceVariant = onBg,
            outline = onBg.copy(alpha = 0.5f),
            outlineVariant = onBg.copy(alpha = 0.2f)
        )
    } else {
        lightColorScheme(
            primary = onBg,
            onPrimary = bg,
            secondary = onBg,
            onSecondary = bg,
            tertiary = onBg,
            onTertiary = bg,
            background = bg,
            surface = bg,
            surfaceVariant = bg,
            surfaceContainer = bg,
            surfaceContainerHigh = bg,
            surfaceContainerHighest = bg,
            surfaceContainerLow = bg,
            surfaceContainerLowest = bg,
            onBackground = onBg,
            onSurface = onBg,
            onSurfaceVariant = onBg,
            outline = onBg.copy(alpha = 0.5f),
            outlineVariant = onBg.copy(alpha = 0.2f)
        )
    }

    val typography = createTypography(scale = fontScale, weight = fontWeight, fontFamily = fontFamily)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}