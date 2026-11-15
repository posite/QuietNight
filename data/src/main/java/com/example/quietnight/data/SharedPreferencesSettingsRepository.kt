package com.example.quietnight.data

import android.content.Context
import com.example.quietnight.domain.model.AppSettings
import com.example.quietnight.domain.model.ThemeMode
import com.example.quietnight.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedPreferencesSettingsRepository @Inject constructor(
    @ApplicationContext context: Context
) : SettingsRepository {
    private val prefs = context.getSharedPreferences("quiet_night_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    override val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun load() = AppSettings(
        threshold = prefs.getInt(KEY_THRESHOLD, DEFAULT_THRESHOLD),
        vibrationEnabled = prefs.getBoolean(KEY_VIBRATION, true),
        vibrationStrength = prefs.getInt(KEY_STRENGTH, DEFAULT_STRENGTH),
        recordClips = prefs.getBoolean(KEY_RECORD_CLIPS, false),
        gentleVibration = prefs.getBoolean(KEY_GENTLE, false),
        themeMode = ThemeMode.entries.firstOrNull {
            it.name == prefs.getString(KEY_THEME_MODE, null)
        } ?: ThemeMode.DARK
    )

    override fun setThreshold(value: Int) {
        prefs.edit().putInt(KEY_THRESHOLD, value).apply()
        _settings.value = _settings.value.copy(threshold = value)
    }

    override fun setVibrationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIBRATION, enabled).apply()
        _settings.value = _settings.value.copy(vibrationEnabled = enabled)
    }

    override fun setVibrationStrength(value: Int) {
        val clamped = value.coerceIn(1, 255)
        prefs.edit().putInt(KEY_STRENGTH, clamped).apply()
        _settings.value = _settings.value.copy(vibrationStrength = clamped)
    }

    override fun setRecordClips(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_RECORD_CLIPS, enabled).apply()
        _settings.value = _settings.value.copy(recordClips = enabled)
    }

    override fun setGentleVibration(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GENTLE, enabled).apply()
        _settings.value = _settings.value.copy(gentleVibration = enabled)
    }

    override fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _settings.value = _settings.value.copy(themeMode = mode)
    }

    companion object {
        const val DEFAULT_THRESHOLD = 75
        const val DEFAULT_STRENGTH = 150
        private const val KEY_THRESHOLD = "threshold"
        private const val KEY_VIBRATION = "vibration"
        private const val KEY_STRENGTH = "vibration_strength"
        private const val KEY_RECORD_CLIPS = "record_clips"
        private const val KEY_GENTLE = "gentle_vibration"
        private const val KEY_THEME_MODE = "theme_mode"
    }
}
