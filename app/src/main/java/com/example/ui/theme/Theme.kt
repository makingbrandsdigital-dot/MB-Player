package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DevotionalColorScheme = darkColorScheme(
    primary = AmberPrimary,
    secondary = AmberSecondary,
    background = DarkBackground,
    surface = SurfaceDark,
    onPrimary = OnPrimaryText,
    onSecondary = OnPrimaryText,
    onBackground = OnDarkPrimary,
    onSurface = OnDarkPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = OnDarkSecondary
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DevotionalColorScheme,
        typography = Typography,
        content = content
    )
}
