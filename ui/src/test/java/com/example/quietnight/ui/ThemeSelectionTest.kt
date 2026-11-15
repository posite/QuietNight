package com.example.quietnight.ui

import com.example.quietnight.domain.model.AppSettings
import com.example.quietnight.domain.model.ThemeMode
import com.example.quietnight.domain.repository.SettingsRepository
import com.example.quietnight.ui.theme.usesDarkTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Test

class ThemeSelectionTest {
    @Test fun defaultKeepsExistingDarkAppearance() {
        assertEquals(ThemeMode.DARK, AppSettings().themeMode)
    }

    @Test fun systemTracksDeviceWhileExplicitChoicesOverrideIt() {
        assertFalse(ThemeMode.SYSTEM.usesDarkTheme(systemDark = false))
        assertTrue(ThemeMode.SYSTEM.usesDarkTheme(systemDark = true))
        listOf(false, true).forEach { systemDark ->
            assertFalse(ThemeMode.LIGHT.usesDarkTheme(systemDark))
            assertTrue(ThemeMode.DARK.usesDarkTheme(systemDark))
        }
    }

    @Test fun settingThemePublishesRepositoryStateWithoutChangingMonitoringPreferences() {
        val repository = FakeSettingsRepository()
        val original = repository.settings.value
        val viewModel = SettingsViewModel(repository)
        listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK).forEach { mode ->
            viewModel.setThemeMode(mode)
            assertEquals(original.copy(themeMode = mode), viewModel.settings.value)
        }
        repository.settings.value = original.copy(themeMode = ThemeMode.SYSTEM)
        assertEquals(ThemeMode.SYSTEM, viewModel.settings.value.themeMode)
    }

    private class FakeSettingsRepository : SettingsRepository {
        override val settings = MutableStateFlow(AppSettings())
        override fun setThemeMode(mode: ThemeMode) { settings.value = settings.value.copy(themeMode = mode) }
        override fun setThreshold(value: Int) = error("테마 변경이 감지 설정을 바꾸면 안 됩니다.")
        override fun setVibrationEnabled(enabled: Boolean) = error("테마 변경이 진동 설정을 바꾸면 안 됩니다.")
        override fun setVibrationStrength(value: Int) = error("테마 변경이 진동 세기를 바꾸면 안 됩니다.")
        override fun setRecordClips(enabled: Boolean) = error("테마 변경이 녹음을 켜면 안 됩니다.")
        override fun setGentleVibration(enabled: Boolean) = error("테마 변경이 여유 모드를 바꾸면 안 됩니다.")
    }
}
