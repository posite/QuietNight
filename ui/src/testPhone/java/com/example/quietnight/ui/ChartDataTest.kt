package com.example.quietnight.ui

import com.example.quietnight.domain.model.SleepSession
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class ChartDataTest {
    private val zone = ZoneId.of("Asia/Seoul")
    private fun clock(date: String, zone: ZoneId = this.zone): Clock =
        Clock.fixed(LocalDate.parse(date).atTime(12, 0).atZone(zone).toInstant(), zone)

    private fun session(date: String, score: Int, zone: ZoneId = this.zone) = SleepSession(
        date = LocalDate.parse(date).atStartOfDay(zone).toInstant().toEpochMilli(),
        score = score, snoreTime = 1000, sleepTime = 10000
    )

    @Test
    fun emptyHistoryAlwaysHasSevenEmptyBars() {
        ChartPeriod.entries.forEach { period ->
            val bars = buildChartBars(emptyList(), period, clock("2026-09-12"), Locale.KOREA)
            assertEquals(7, bars.size)
            assertTrue(bars.none { it.hasData })
        }
    }

    @Test
    fun dailyWindowIncludesTodayAndSixPreviousDaysOnly() {
        val history = listOf(session("2026-09-05", 10), session("2026-09-06", 0),
            session("2026-09-12", 90), session("2026-09-13", 20))
        val bars = buildChartBars(history, ChartPeriod.DAILY, clock("2026-09-12"), Locale.KOREA)
        assertEquals(listOf("일", "월", "화", "수", "목", "금", "토"), bars.map { it.label })
        assertTrue(bars.first().hasData)
        assertEquals(0, bars.first().score)
        assertEquals(listOf(history[1], history[2]), bars.flatMap { it.sessions })
    }

    @Test
    fun weeklyBucketsUseLocaleWeekStartAndExclusiveEnd() {
        val history = listOf(session("2026-09-06", 30), session("2026-09-07", 80),
            session("2026-09-12", 100))
        val bars = buildChartBars(history, ChartPeriod.WEEKLY, clock("2026-09-12"), Locale.UK)
        assertEquals("9/7", bars.last().label)
        assertEquals(90, bars.last().score)
        assertEquals(listOf(history[0]), bars[5].sessions)
        assertEquals(history.drop(1), bars.last().sessions)
    }

    @Test
    fun monthlyBucketsHandleLeapDayAndYearBoundary() {
        val history = listOf(session("2023-12-31", 60), session("2024-01-01", 80),
            session("2024-02-29", 100), session("2024-03-01", 10))
        val bars = buildChartBars(history, ChartPeriod.MONTHLY, clock("2024-02-29"), Locale.KOREA)
        assertEquals(listOf("8월", "9월", "10월", "11월", "12월", "1월", "2월"), bars.map { it.label })
        assertEquals(listOf(60, 80, 100), bars.takeLast(3).map { it.score })
        assertEquals("2024년 2월", bars.last().rangeLabel)
    }

    @Test
    fun daylightSavingDaysDoNotDropOrDuplicateSessions() {
        val dstZone = ZoneId.of("America/New_York")
        val history = listOf(session("2026-03-07", 60, dstZone),
            session("2026-03-08", 80, dstZone), session("2026-03-09", 100, dstZone))
        val bars = buildChartBars(history, ChartPeriod.DAILY, clock("2026-03-09", dstZone), Locale.US)
        assertEquals(history, bars.flatMap { it.sessions })
        assertEquals(listOf(60, 80, 100), bars.takeLast(3).map { it.score })
    }
}
