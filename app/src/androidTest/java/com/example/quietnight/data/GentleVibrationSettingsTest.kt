package com.example.quietnight.data

import android.content.Context
import android.content.ContextWrapper
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class GentleVibrationSettingsTest {
    private val target = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "night_settings_test_${UUID.randomUUID()}"
    private val context = object : ContextWrapper(target) {
        override fun getSharedPreferences(name: String?, mode: Int) =
            target.getSharedPreferences(this@GentleVibrationSettingsTest.name, Context.MODE_PRIVATE)
    }
    @After fun cleanup() { target.deleteSharedPreferences(name) }

    @Test fun gentleChoiceSurvivesRepositoryRecreationWithoutEnablingClips() {
        val repository = SharedPreferencesSettingsRepository(context)
        assertFalse(repository.settings.value.gentleVibration)
        repository.setGentleVibration(true)
        assertTrue(repository.settings.value.gentleVibration)
        val recreated = SharedPreferencesSettingsRepository(context)
        assertTrue(recreated.settings.value.gentleVibration)
        assertFalse(recreated.settings.value.recordClips)
        recreated.setGentleVibration(false)
        assertFalse(SharedPreferencesSettingsRepository(context).settings.value.gentleVibration)
    }
}
