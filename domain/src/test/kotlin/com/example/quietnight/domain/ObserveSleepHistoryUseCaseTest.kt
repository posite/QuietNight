package com.example.quietnight.domain

import com.example.quietnight.domain.model.SleepSession
import com.example.quietnight.domain.model.SnoreClip
import com.example.quietnight.domain.repository.SleepRepository
import com.example.quietnight.domain.usecase.ObserveSleepHistoryUseCase
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class ObserveSleepHistoryUseCaseTest {
    private class Repository(val rows: MutableStateFlow<List<SleepSession>>) : SleepRepository {
        override fun observeHistory() = rows
        override suspend fun clipsForDate(date: Long) = emptyList<SnoreClip>()
        override suspend fun clearHistory() { rows.value = emptyList() }
        override suspend fun saveFeedback(date: Long, feedback: com.example.quietnight.domain.model.MorningFeedback) {}
    }

    @Test fun missingDaysStayAbsentRatherThanReusingTheLatestRecord() = runTest {
        val clock = Clock.fixed(Instant.parse("2026-09-14T01:00:00Z"), ZoneId.of("Asia/Seoul"))
        val old = session("2026-09-10", clock.zone)
        val history = ObserveSleepHistoryUseCase(Repository(MutableStateFlow(listOf(old))), clock)().first()
        assertNull(history.today)
        assertNull(history.yesterday)
        assertEquals(listOf(old), history.sessions)
    }

    @Test fun usesLocalDateAcrossDstRatherThanSubtracting24Hours() = runTest {
        val zone = ZoneId.of("America/New_York")
        val clock = Clock.fixed(Instant.parse("2026-03-09T05:00:00Z"), zone)
        val today = session("2026-03-09", zone)
        val yesterday = session("2026-03-08", zone)
        assertEquals(23L * 60 * 60 * 1000, today.date - yesterday.date)
        val history = ObserveSleepHistoryUseCase(
            Repository(MutableStateFlow(listOf(today, yesterday))), clock)().first()
        assertEquals(today, history.today)
        assertEquals(yesterday, history.yesterday)
    }

    @Test fun seoulMidnightUsesNextCalendarDayEvenWhenUtcIsPreviousDay() = runTest {
        val zone = ZoneId.of("Asia/Seoul")
        val clock = Clock.fixed(Instant.parse("2026-12-31T15:00:00Z"), zone)
        val today = session("2027-01-01", zone)
        val yesterday = session("2026-12-31", zone)
        val history = ObserveSleepHistoryUseCase(
            Repository(MutableStateFlow(listOf(today, yesterday))), clock)().first()
        assertEquals(today, history.today)
        assertEquals(yesterday, history.yesterday)
    }

    private fun session(date: String, zone: ZoneId) = SleepSession(
        date = LocalDate.parse(date).atStartOfDay(zone).toInstant().toEpochMilli(),
        score = 80, snoreTime = 1200, sleepTime = 60_000
    )
}
