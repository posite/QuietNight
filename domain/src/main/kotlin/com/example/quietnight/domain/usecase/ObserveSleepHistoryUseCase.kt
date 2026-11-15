package com.example.quietnight.domain.usecase

import com.example.quietnight.domain.model.SleepSession
import com.example.quietnight.domain.repository.SleepRepository
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class SleepHistory(
    val sessions: List<SleepSession>,
    val today: SleepSession?,
    val yesterday: SleepSession?
)

class ObserveSleepHistoryUseCase(
    private val repository: SleepRepository,
    private val clock: Clock
) {
    operator fun invoke(): Flow<SleepHistory> = repository.observeHistory().map { history ->
        // 매 방출 시 날짜를 한 번만 읽어 자정 경계에서도 일관된 비교를 한다.
        val today = LocalDate.now(clock)
        fun epoch(date: LocalDate) = date.atStartOfDay(clock.zone).toInstant().toEpochMilli()
        SleepHistory(history,
            history.find { it.date == epoch(today) },
            history.find { it.date == epoch(today.minusDays(1)) })
    }
}
