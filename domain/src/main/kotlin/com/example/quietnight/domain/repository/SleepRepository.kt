package com.example.quietnight.domain.repository

import com.example.quietnight.domain.model.SleepSession
import com.example.quietnight.domain.model.SnoreClip
import kotlinx.coroutines.flow.Flow

interface SleepRepository {
    fun observeHistory(): Flow<List<SleepSession>>
    suspend fun clipsForDate(date: Long): List<SnoreClip>
    // 기록과 연결된 녹음 파일까지 제거한다. 세션 누적·저장은 서비스의 책임이다.
    suspend fun clearHistory()
    suspend fun saveFeedback(date: Long, feedback: com.example.quietnight.domain.model.MorningFeedback)
}
