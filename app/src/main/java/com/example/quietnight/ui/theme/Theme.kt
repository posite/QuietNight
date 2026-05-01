package com.example.quietnight.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4F6EF7),
    background = Color(0xFF0D1117), // 기존 다크 배경 유지
    surface = Color(0xFF161B27),    // 카드 배경
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF4F6EF7),
    background = Color(0xFFF8F9FA), // 시스템 라이트 배경
    surface = Color.White,          // 카드 배경
    onBackground = Color.Black,
    onSurface = Color.Black
)

@Composable
fun QuietNightTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, content = content)
}