package com.example.quietnight.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.quietnight.domain.model.AppSettings
import com.example.quietnight.domain.model.ThemeMode
import com.example.quietnight.ui.theme.QuietNightTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ThemeScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun settingsSelectAndImmediatelyApplyEachChoice() {
        var settings by mutableStateOf(AppSettings())
        compose.setContent {
            QuietNightTheme(themeMode = settings.themeMode) {
                SettingsScreen(settings, {}, {}, {}, {}, {}, onThemeModeChange = {
                    settings = settings.copy(themeMode = it)
                }, onBack = {})
            }
        }
        compose.onNodeWithText("다크").assertIsSelected()
        compose.onNodeWithText("라이트").performClick().assertIsSelected()
        compose.runOnIdle { assertEquals(ThemeMode.LIGHT, settings.themeMode) }
        assertBackground(Color(0xFFF8F9FA))
        compose.onNodeWithText("시스템").performClick().assertIsSelected()
        compose.runOnIdle { assertEquals(ThemeMode.SYSTEM, settings.themeMode) }
        compose.onNodeWithText("다크").performClick().assertIsSelected()
        assertBackground(Color(0xFF101827))
    }

    @Test fun homeInheritsLightThemeFromApp() {
        compose.setContent { QuietNightTheme(themeMode = ThemeMode.LIGHT) {
            HomeScreen(SnoreState(), {}, {})
        } }
        assertBackground(Color(0xFFF8F9FA))
    }

    @Test fun systemThemeReactsToDeviceConfigurationChanges() {
        var systemDark by mutableStateOf(false)
        compose.setContent {
            val configuration = Configuration(LocalConfiguration.current).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                    if (systemDark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
            }
            CompositionLocalProvider(LocalConfiguration provides configuration) {
                QuietNightTheme(themeMode = ThemeMode.SYSTEM) {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                }
            }
        }
        assertBackground(Color(0xFFF8F9FA))
        compose.runOnIdle { systemDark = true }
        assertBackground(Color(0xFF101827))
        compose.runOnIdle { systemDark = false }
        assertBackground(Color(0xFFF8F9FA))
    }

    @Test fun analysisChartInheritsLightThemeFromApp() {
        compose.setContent { QuietNightTheme(themeMode = ThemeMode.LIGHT) {
            AnalyzeScreen(SnoreState())
        } }
        compose.onNodeWithText("점수 비교 보기").performClick()
        assertBackground(Color(0xFFF8F9FA))
    }

    private fun assertBackground(expected: Color) {
        assertEquals(expected, compose.onRoot().captureToImage().toPixelMap()[0, 0])
    }
}
