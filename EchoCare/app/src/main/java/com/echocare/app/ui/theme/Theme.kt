package com.echocare.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color           // ← required for Color(0xFF...) literals
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ─── Dark Color Scheme ────────────────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary              = Purple60,
    onPrimary            = DarkBackground,
    primaryContainer     = Purple20,
    onPrimaryContainer   = Purple90,

    secondary            = Teal60,
    onSecondary          = DarkBackground,
    secondaryContainer   = Teal20,
    onSecondaryContainer = Teal90,

    tertiary             = Amber60,
    onTertiary           = DarkBackground,
    tertiaryContainer    = Amber20,
    onTertiaryContainer  = Amber90,

    background           = DarkBackground,
    onBackground         = Purple90,

    surface              = DarkSurface,
    onSurface            = Purple90,
    surfaceVariant       = DarkSurfaceVar,
    onSurfaceVariant     = Purple80,

    outline              = DarkOutline,

    error                = Red40,
    onError              = DarkBackground,
    errorContainer       = Color(0xFF93000A),
    onErrorContainer     = Red90
)

// ─── Light Color Scheme ───────────────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary              = Purple40,
    onPrimary            = LightSurface,
    primaryContainer     = Purple90,
    onPrimaryContainer   = Purple10,

    secondary            = Teal40,
    onSecondary          = LightSurface,
    secondaryContainer   = Teal90,
    onSecondaryContainer = Teal20,

    tertiary             = Amber40,
    onTertiary           = LightSurface,
    tertiaryContainer    = Amber90,
    onTertiaryContainer  = Amber20,

    background           = LightBackground,
    onBackground         = Purple10,

    surface              = LightSurface,
    onSurface            = Purple10,
    surfaceVariant       = LightSurfaceVar,
    onSurfaceVariant     = Purple20,

    outline              = LightOutline,

    error                = Red40,
    onError              = LightSurface,
    errorContainer       = Red90,
    onErrorContainer     = Color(0xFF410002)
)

@Composable
fun EchoCareTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = EchoCareTypography,
        content     = content
    )
}
