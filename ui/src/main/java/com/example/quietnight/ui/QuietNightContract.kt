package com.example.quietnight.ui

import com.example.quietnight.domain.model.SleepSession
import com.example.quietnight.domain.model.MorningFeedback
import com.example.quietnight.domain.model.ThemeMode

sealed class SnoreIntent {
    data object StartMonitoring : SnoreIntent()
    data object StopMonitoring : SnoreIntent()
    data object LoadHistory : SnoreIntent()
    data object ClearHistory : SnoreIntent()
    data class SaveFeedback(val date: Long, val feedback: MorningFeedback) : SnoreIntent()
    data object ApplyGentleSuggestion : SnoreIntent()
    data class SetThemeMode(val mode: ThemeMode) : SnoreIntent()
}

data class SnoreState(
    // Home State
    val todayScore: Int = 0,
    val todaySnoreTime: Long = 0,   // 밀리초
    val todaySleepTime: Long = 0,   // 밀리초
    val prevScore: Int = 0,         // 어제 점수 (증감 비교용)
    val prevSnoreTime: Long = 0,    // 어제 코골이 시간

    // Monitor State
    val isMonitoring: Boolean = false,
    val currentDb: Int = 0,
    val threshold: Int = 75,
    val elapsedTime: String = "00:00:00",
    val recentLogs: List<String> = emptyList(),

    // Analyze State
    val sessionHistory: List<SleepSession> = emptyList(),
    val gentleVibration: Boolean = false,
    val suggestGentleVibration: Boolean = false,
    val journalMessage: String? = null
)
