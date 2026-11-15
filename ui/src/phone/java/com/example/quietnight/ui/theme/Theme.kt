package com.example.quietnight.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.quietnight.domain.model.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFAAB6FF),
    onPrimary = Color(0xFF0C1B5A),
    primaryContainer = Color(0xFF26366F),
    onPrimaryContainer = Color(0xFFDDE1FF),
    secondary = Color(0xFF89D1F5),
    onSecondary = Color(0xFF003548),
    background = Color(0xFF101827),
    onBackground = Color(0xFFE6EAF2),
    surface = Color(0xFF192538),
    onSurface = Color(0xFFE6EAF2),
    surfaceVariant = Color(0xFF26344A),
    onSurfaceVariant = Color(0xFFBCC7DA),
    outline = Color(0xFF637087),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF4F6EF7),
    background = Color(0xFFF8F9FA),
    surface = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black
)

@Composable
fun QuietNightTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val darkTheme = themeMode.usesDarkTheme(isSystemInDarkTheme())
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
