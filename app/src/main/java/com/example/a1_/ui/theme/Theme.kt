package com.example.a1_.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Accent1C,
    onPrimary = BgColor,
    secondary = SurfaceColor,
    onSecondary = TextPrimary,
    background = BgColor,
    surface = SurfaceColor,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = WrongRed,
    onError = TextPrimary,
    surfaceVariant = SurfaceHover,
    onSurfaceVariant = TextMuted,
    outline = BorderColor
)

@Composable
fun TestTrainerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
