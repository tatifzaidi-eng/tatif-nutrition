package com.nutriscan.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ─────────────────────────────────────────────────────────────────────────────
//  Palette de couleurs NutriScan — Material Design 3
// ─────────────────────────────────────────────────────────────────────────────

private val NutriGreen = Color(0xFF1D9E75)
private val NutriGreenDark = Color(0xFF0F6E56)
private val NutriBlue = Color(0xFF185FA5)
private val NutriAmber = Color(0xFFBA7517)
private val NutriRed = Color(0xFFD85A30)
private val NutriPurple = Color(0xFF534AB7)
private val NutriGray = Color(0xFF5F5E5A)

private val LightColors = lightColorScheme(
    primary = NutriGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEAF3DE),
    onPrimaryContainer = Color(0xFF173404),
    secondary = NutriBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE6F1FB),
    onSecondaryContainer = Color(0xFF042C53),
    tertiary = NutriAmber,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFAEEDA),
    onTertiaryContainer = Color(0xFF412402),
    error = NutriRed,
    onError = Color.White,
    errorContainer = Color(0xFFFCEBEB),
    onErrorContainer = Color(0xFF501313),
    background = Color(0xFFFCFCFA),
    onBackground = Color(0xFF1C1C1A),
    surface = Color.White,
    onSurface = Color(0xFF1C1C1A),
    surfaceVariant = Color(0xFFF1EFE8),
    onSurfaceVariant = Color(0xFF5F5E5A),
    outline = Color(0xFFB4B2A9),
    outlineVariant = Color(0xFFD3D1C7)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5DCAA5),
    onPrimary = Color(0xFF04342C),
    primaryContainer = Color(0xFF0F6E56),
    onPrimaryContainer = Color(0xFF9FE1CB),
    secondary = Color(0xFF85B7EB),
    onSecondary = Color(0xFF042C53),
    secondaryContainer = Color(0xFF0C447C),
    onSecondaryContainer = Color(0xFFB5D4F4),
    tertiary = Color(0xFFEF9F27),
    onTertiary = Color(0xFF412402),
    tertiaryContainer = Color(0xFF854F0B),
    onTertiaryContainer = Color(0xFFFAC775),
    error = Color(0xFFF09595),
    onError = Color(0xFF501313),
    errorContainer = Color(0xFFA32D2D),
    onErrorContainer = Color(0xFFF7C1C1),
    background = Color(0xFF141412),
    onBackground = Color(0xFFE8E6DF),
    surface = Color(0xFF1C1C1A),
    onSurface = Color(0xFFE8E6DF),
    surfaceVariant = Color(0xFF2C2C2A),
    onSurfaceVariant = Color(0xFFB4B2A9),
    outline = Color(0xFF888780),
    outlineVariant = Color(0xFF444441)
)

// ─────────────────────────────────────────────────────────────────────────────
//  Typographie
// ─────────────────────────────────────────────────────────────────────────────

val NutriTypography = Typography()  // Utilise la typo Material3 par défaut

// ─────────────────────────────────────────────────────────────────────────────
//  Theme principal
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun NutriScanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,  // Material You (Android 12+)
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NutriTypography,
        content = content
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  Tokens de couleurs sémantiques pour les graphiques
// ─────────────────────────────────────────────────────────────────────────────

object NutriColors {
    val Calories = Color(0xFF378ADD)
    val Proteins = Color(0xFF1D9E75)
    val Carbs = Color(0xFFBA7517)
    val Fat = Color(0xFFD85A30)
    val Fiber = Color(0xFF534AB7)
    val Vitamins = Color(0xFF993556)
    val Minerals = Color(0xFF639922)

    val LevelLow = Color(0xFF1D9E75)
    val LevelMedium = Color(0xFFBA7517)
    val LevelHigh = Color(0xFFD85A30)
}
