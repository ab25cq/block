package com.ab25cq.block.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DarkBackground = Color(0xFF0A0E1A)
val DarkSurface = Color(0xFF111827)
val DarkCard = Color(0xFF1A2235)
val AccentCyan = Color(0xFF00E5FF)
val AccentPurple = Color(0xFFD500F9)

private val DarkColorScheme = darkColorScheme(
    primary = AccentCyan,
    secondary = AccentPurple,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
)

@Composable
fun BlockBlastTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
