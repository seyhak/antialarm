package com.antialarm.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = AccentOrange,
    onPrimary = DarkSurface,
    primaryContainer = AccentOrange.copy(alpha = 0.2f),
    onPrimaryContainer = AccentOrange,
    secondary = AccentBlue,
    onSecondary = DarkSurface,
    secondaryContainer = AccentBlue.copy(alpha = 0.2f),
    onSecondaryContainer = AccentBlue,
    tertiary = AccentGreen,
    onTertiary = DarkSurface,
    error = AccentRed,
    onError = DarkSurface,
    background = DarkSurface,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = TextMuted,
    outlineVariant = DarkCardVariant
)

@Composable
fun AntiAlarmTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
