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

private val LightColors = lightColorScheme(
    primary = Color(0xFF1D9E75),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEAF3DE),
    onPrimaryContainer = Color(0xFF173404),
    secondary = Color(0xFF185FA5),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE6F1FB),
    onSecondaryContainer = Color(0xFF042C53),
    tertiary = Color(0xFFBA7517),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFAEEDA),
    error = Color(0xFFD85A30),
    errorContainer = Color(0xFFFCEBEB),
    background = Color(0xFFFCFCFA),
    surface = Color.White,
    surfaceVariant = Color(0xFFF1EFE8),
    outline = Color(0xFFB4B2A9)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5DCAA5),
    onPrimary = Color(0xFF04342C),
    primaryContainer = Color(0xFF0F6E56),
    onPrimaryContainer = Color(0xFF9FE1CB),
    secondary = Color(0xFF85B7EB),
    onSecondary = Color(0xFF042C53),
    secondaryContainer = Color(0xFF0C447C),
    tertiary = Color(0xFFEF9F27),
    error = Color(0xFFF09595),
    errorContainer = Color(0xFFA32D2D),
    background = Color(0xFF141412),
    surface = Color(0xFF1C1C1A),
    surfaceVariant = Color(0xFF2C2C2A),
    outline = Color(0xFF888780)
)

object NutriColors {
    val Calories = Color(0xFF378ADD)
    val Proteins = Color(0xFF1D9E75)
    val Carbs    = Color(0xFFBA7517)
    val Fat      = Color(0xFFD85A30)
    val Fiber    = Color(0xFF534AB7)
    val Vitamins = Color(0xFF993556)
    val Minerals = Color(0xFF639922)
    val LevelLow    = Color(0xFF1D9E75)
    val LevelMedium = Color(0xFFBA7517)
    val LevelHigh   = Color(0xFFD85A30)
}

@Composable
fun NutriScanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx)
            else dynamicLightColorScheme(ctx)
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
    MaterialTheme(colorScheme = colorScheme, content = content)
}
