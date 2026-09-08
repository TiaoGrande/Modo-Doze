package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val AmoledColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = AmoledBlack,
    primaryContainer = DarkSurfaceElevated,
    onPrimaryContainer = EmeraldPrimary,
    secondary = CyanAccent,
    onSecondary = AmoledBlack,
    secondaryContainer = DarkSurfaceCard,
    onSecondaryContainer = CyanAccent,
    tertiary = AmberAlert,
    onTertiary = AmoledBlack,
    background = AmoledBlack,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = DarkSurfaceBorder,
    error = CoralWarning,
    onError = AmoledBlack
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            window?.let {
                it.statusBarColor = AmoledBlack.toArgb()
                it.navigationBarColor = AmoledBlack.toArgb()
                val insetsController = WindowCompat.getInsetsController(it, view)
                insetsController.isAppearanceLightStatusBars = false
                insetsController.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = AmoledColorScheme,
        typography = Typography,
        content = content
    )
}
