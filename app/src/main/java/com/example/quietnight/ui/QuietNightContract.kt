package com.example.quietnight.ui

import com.example.quietnight.data.SleepSession

sealed class SnoreIntent {
    data object StartMonitoring : SnoreIntent()
    data object StopMonitoring : SnoreIntent()
    data object LoadHistory : SnoreIntent()
}

data class SnoreState(
    // Home State
    val todayScore: Int = 0,
    val todaySnoreMin: Int = 0,
    val positionStats: Map<String, Float> = emptyMap(),

    // Monitor State
    val isMonitoring: Boolean = false,
    val currentDb: Int = 0,
    val elapsedTime: String = "00:00:00",
    val recentLogs: List<String> = emptyList(),

    // Weekly State
    val weeklyHistory: List<SleepSession> = emptyList()
)