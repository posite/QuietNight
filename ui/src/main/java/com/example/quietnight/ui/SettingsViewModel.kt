package com.example.quietnight.ui

import androidx.lifecycle.ViewModel
import com.example.quietnight.domain.model.AppSettings
import com.example.quietnight.domain.model.ThemeMode
import com.example.quietnight.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = repository.settings

    fun setThreshold(value: Int) = repository.setThreshold(value)

    fun setVibrationEnabled(enabled: Boolean) = repository.setVibrationEnabled(enabled)

    fun setVibrationStrength(value: Int) = repository.setVibrationStrength(value)

    fun setRecordClips(enabled: Boolean) = repository.setRecordClips(enabled)
    fun setGentleVibration(enabled: Boolean) = repository.setGentleVibration(enabled)

    fun setThemeMode(mode: ThemeMode) = handleIntent(SnoreIntent.SetThemeMode(mode))

    private fun handleIntent(intent: SnoreIntent) {
        when (intent) {
            is SnoreIntent.SetThemeMode -> repository.setThemeMode(intent.mode)
            else -> error("설정 화면에서 지원하지 않는 동작입니다.")
        }
    }
}
