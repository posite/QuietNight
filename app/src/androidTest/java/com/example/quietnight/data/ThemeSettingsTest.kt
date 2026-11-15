package com.example.quietnight.data

import android.content.Context
import android.content.ContextWrapper
import androidx.test.platform.app.InstrumentationRegistry
import com.example.quietnight.domain.model.ThemeMode
import java.util.UUID
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class ThemeSettingsTest {
    private val target = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "theme_settings_test_${UUID.randomUUID()}"
    private val context = object : ContextWrapper(target) {
        override fun getSharedPreferences(name: String?, mode: Int) =
            target.getSharedPreferences(this@ThemeSettingsTest.name, Context.MODE_PRIVATE)
    }
    @After fun cleanup() { target.deleteSharedPreferences(name) }

    @Test fun allChoicesSurviveRecreationAndPreserveOtherSettings() {
        val repository = SharedPreferencesSettingsRepository(context)
        assertEquals(ThemeMode.DARK, repository.settings.value.themeMode)
        repository.setThreshold(60)
        repository.setVibrationEnabled(false)
        repository.setGentleVibration(true)
        val original = repository.settings.value
        listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK).forEach { mode ->
            repository.setThemeMode(mode)
            assertEquals(original.copy(themeMode = mode), repository.settings.value)
            assertEquals(original.copy(themeMode = mode),
                SharedPreferencesSettingsRepository(context).settings.value)
        }
    }

    @Test fun unsupportedStoredValueFallsBackToExistingDefault() {
        context.getSharedPreferences(name, Context.MODE_PRIVATE).edit()
            .putString("theme_mode", "unsupported").commit()
        assertEquals(ThemeMode.DARK, SharedPreferencesSettingsRepository(context).settings.value.themeMode)
    }
}
