package com.example.dumbphonelauncher.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.dumbphonelauncher.R

val LexendFont = FontFamily(Font(R.font.lexend))
val SpaceMonoFont = FontFamily(Font(R.font.space_mono))
val PlayfairDisplayFont = FontFamily(Font(R.font.playfair_display))
val OswaldFont = FontFamily(Font(R.font.oswald))
val LatoFont = FontFamily(Font(R.font.lato))
val FiraCodeFont = FontFamily(Font(R.font.fira_code))

val AtkinsonFont = FontFamily(Font(R.font.atkinson))
val CinzelFont = FontFamily(Font(R.font.cinzel))
val QuicksandFont = FontFamily(Font(R.font.quicksand))
val UbuntuFont = FontFamily(Font(R.font.ubuntu))
val JosefinSansFont = FontFamily(Font(R.font.josefin_sans))

fun getFontFamilyByName(name: String?): FontFamily {
    return when (name?.uppercase()) {
        "SERIF" -> FontFamily.Serif
        "MONOSPACE" -> FontFamily.Monospace
        "SANS_SERIF" -> FontFamily.SansSerif
        "CURSIVE" -> FontFamily.Cursive
        "LEXEND" -> LexendFont
        "SPACE_MONO" -> SpaceMonoFont
        "PLAYFAIR_DISPLAY" -> PlayfairDisplayFont
        "OSWALD" -> OswaldFont
        "LATO" -> LatoFont
        "FIRA_CODE" -> FiraCodeFont
        "ATKINSON" -> AtkinsonFont
        "CINZEL" -> CinzelFont
        "QUICKSAND" -> QuicksandFont
        "UBUNTU" -> UbuntuFont
        "JOSEFIN_SANS" -> JosefinSansFont
        else -> FontFamily.Default
    }
}

/**
 * Creates a scaled typography set based on user font preferences.
 * @param scale Multiplier for font sizes (0.8 = small, 1.0 = default, 1.2 = large, 1.4 = extra large)
 * @param weight Font weight value (300 = Light, 400 = Normal, 500 = Medium, 700 = Bold)
 * @param fontFamily Font family to apply across all styles (Default, Serif, Monospace, SansSerif, Cursive)
 */
fun createTypography(
    scale: Float = 1.0f,
    weight: Int = 400,
    fontFamily: FontFamily = FontFamily.Default
): Typography {
    val fw = FontWeight(weight)
    return Typography(
        displayLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = fw,
            fontSize = (57 * scale).sp,
            lineHeight = (64 * scale).sp,
            letterSpacing = (-0.25).sp
        ),
        displayMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = fw,
            fontSize = (45 * scale).sp,
            lineHeight = (52 * scale).sp,
        ),
        displaySmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = fw,
            fontSize = (36 * scale).sp,
            lineHeight = (44 * scale).sp,
        ),
        headlineLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = fw,
            fontSize = (32 * scale).sp,
            lineHeight = (40 * scale).sp,
        ),
        headlineMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = fw,
            fontSize = (28 * scale).sp,
            lineHeight = (36 * scale).sp,
        ),
        headlineSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = fw,
            fontSize = (24 * scale).sp,
            lineHeight = (32 * scale).sp,
        ),
        titleLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = fw,
            fontSize = (22 * scale).sp,
            lineHeight = (28 * scale).sp,
        ),
        titleMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight(maxOf(weight, 500)),
            fontSize = (16 * scale).sp,
            lineHeight = (24 * scale).sp,
            letterSpacing = 0.15.sp
        ),
        titleSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight(maxOf(weight, 500)),
            fontSize = (14 * scale).sp,
            lineHeight = (20 * scale).sp,
            letterSpacing = 0.1.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = fw,
            fontSize = (16 * scale).sp,
            lineHeight = (24 * scale).sp,
            letterSpacing = 0.5.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = fw,
            fontSize = (14 * scale).sp,
            lineHeight = (20 * scale).sp,
            letterSpacing = 0.25.sp
        ),
        bodySmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = fw,
            fontSize = (12 * scale).sp,
            lineHeight = (16 * scale).sp,
            letterSpacing = 0.4.sp
        ),
        labelLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight(maxOf(weight, 500)),
            fontSize = (14 * scale).sp,
            lineHeight = (20 * scale).sp,
            letterSpacing = 0.1.sp
        ),
        labelMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight(maxOf(weight, 500)),
            fontSize = (12 * scale).sp,
            lineHeight = (16 * scale).sp,
            letterSpacing = 0.5.sp
        ),
        labelSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight(maxOf(weight, 500)),
            fontSize = (11 * scale).sp,
            lineHeight = (16 * scale).sp,
            letterSpacing = 0.5.sp
        )
    )
}

// Default typography for backward compatibility
val Typography = createTypography()