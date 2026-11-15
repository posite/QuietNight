package com.example.quietnight.ui.theme

import com.example.quietnight.domain.model.ThemeMode

internal fun ThemeMode.usesDarkTheme(systemDark: Boolean): Boolean = when (this) {
    ThemeMode.SYSTEM -> systemDark
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}
