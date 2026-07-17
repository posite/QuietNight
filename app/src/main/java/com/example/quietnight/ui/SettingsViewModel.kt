package com.example.quietnight.ui

import androidx.lifecycle.ViewModel
import com.example.quietnight.data.AppSettings
import com.example.quietnight.data.SettingsRepository
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
}
