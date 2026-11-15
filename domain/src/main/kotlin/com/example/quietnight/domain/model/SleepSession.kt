package com.example.quietnight.domain.model

// 날짜별 수면 기록. 시간 단위는 밀리초이며 저장 방식에 의존하지 않는다.
data class SleepSession(
    val id: Long = 0,
    val date: Long,
    val score: Int,
    val snoreTime: Long,
    val sleepTime: Long,
    val timelineCsv: String = "",
    val insights: NightInsights = NightInsights(),
    val feedback: MorningFeedback = MorningFeedback.NONE
)
