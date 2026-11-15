package com.example.quietnight.domain.model

// 녹음은 명시적으로 켰을 때만 허용하며, 기기 지원 정책은 서비스에서 추가 확인한다.
data class AppSettings(
    val threshold: Int = 75,
    val vibrationEnabled: Boolean = true,
    val vibrationStrength: Int = 150,
    val recordClips: Boolean = false,
    val gentleVibration: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.DARK
)
