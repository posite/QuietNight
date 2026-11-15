package com.example.quietnight.domain.repository

import com.example.quietnight.domain.model.AppSettings
import com.example.quietnight.domain.model.ThemeMode
import kotlinx.coroutines.flow.StateFlow

interface SettingsRepository {
    val settings: StateFlow<AppSettings>
    fun setThreshold(value: Int)
    fun setVibrationEnabled(enabled: Boolean)
    fun setVibrationStrength(value: Int)
    fun setRecordClips(enabled: Boolean)
    fun setGentleVibration(enabled: Boolean)
    fun setThemeMode(mode: ThemeMode)
}
