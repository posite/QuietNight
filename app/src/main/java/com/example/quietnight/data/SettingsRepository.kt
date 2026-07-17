package com.example.quietnight.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

// 사용자 설정.
// threshold = 코골이 감지 데시벨 기준, vibrationEnabled = 진동 교정 사용 여부,
// vibrationStrength = 진동 세기(VibrationEffect amplitude, 1~255)
data class AppSettings(
    val threshold: Int = 75,
    val vibrationEnabled: Boolean = true,
    val vibrationStrength: Int = 150
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("quiet_night_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun load() = AppSettings(
        threshold = prefs.getInt(KEY_THRESHOLD, DEFAULT_THRESHOLD),
        vibrationEnabled = prefs.getBoolean(KEY_VIBRATION, true),
        vibrationStrength = prefs.getInt(KEY_STRENGTH, DEFAULT_STRENGTH)
    )

    fun setThreshold(value: Int) {
        prefs.edit().putInt(KEY_THRESHOLD, value).apply()
        _settings.value = _settings.value.copy(threshold = value)
    }

    fun setVibrationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIBRATION, enabled).apply()
        _settings.value = _settings.value.copy(vibrationEnabled = enabled)
    }

    fun setVibrationStrength(value: Int) {
        val clamped = value.coerceIn(1, 255)
        prefs.edit().putInt(KEY_STRENGTH, clamped).apply()
        _settings.value = _settings.value.copy(vibrationStrength = clamped)
    }

    companion object {
        const val DEFAULT_THRESHOLD = 75
        const val DEFAULT_STRENGTH = 150
        private const val KEY_THRESHOLD = "threshold"
        private const val KEY_VIBRATION = "vibration"
        private const val KEY_STRENGTH = "vibration_strength"
    }
}
