package com.example.quietnight.ui

import com.example.quietnight.data.SleepSession

sealed class SnoreIntent {
    data object StartMonitoring : SnoreIntent()
    data class StopMonitoring(val session: SleepSession) : SnoreIntent()
    data object LoadHistory : SnoreIntent()
    data object TodayScore : SnoreIntent()
}

data class SnoreState(
    // Home State
    val todayScore: Int = 0,
    val todaySnoreTime: Long = 0,
    val todaySnoreMax: Int = 0,
    val todaySleepTime: Long = 0,
    val prevScore: Int = 0,
    val prevSnoreTime: Long = 0,
    val snoreTime: Long = 0,
    val positionStats: Map<String, Float> = emptyMap(),

    // Monitor State
    val isMonitoring: Boolean = false,
    val currentDb: Int = 0,
    val elapsedTime: String = "00:00:00",
    val recentLogs: List<String> = emptyList(),

    // Weekly State
    val weeklyHistory: List<SleepSession> = emptyList()
)

sealed class SnoreEffect {
    data object SessionSaved : SnoreEffect()
}