package com.example.quietnight.ui

import com.example.quietnight.domain.model.SleepSession
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

enum class ChartPeriod(val label: String) {
    DAILY("일간"), WEEKLY("주간"), MONTHLY("월간")
}

// 기록이 없는 구간과 실제 0점 기록을 구분한다.
data class ChartBar(
    val label: String,
    val score: Int,
    val hasData: Boolean,
    val sessions: List<SleepSession> = emptyList(),
    val rangeLabel: String = ""
)

// 기준 시각을 한 번만 읽어 자정 경계에서도 일관된 7개 구간을 만든다.
fun buildChartBars(
    history: List<SleepSession>,
    period: ChartPeriod,
    clock: Clock = Clock.systemDefaultZone(),
    locale: Locale = Locale.getDefault()
): List<ChartBar> {
    val today = LocalDate.now(clock)
    val anchor = when (period) {
        ChartPeriod.DAILY -> today
        ChartPeriod.WEEKLY -> today.with(
            TemporalAdjusters.previousOrSame(WeekFields.of(locale).firstDayOfWeek)
        )
        ChartPeriod.MONTHLY -> today.withDayOfMonth(1)
    }
    return (6 downTo 0).map { offset ->
        val start = when (period) {
            ChartPeriod.DAILY -> anchor.minusDays(offset.toLong())
            ChartPeriod.WEEKLY -> anchor.minusWeeks(offset.toLong())
            ChartPeriod.MONTHLY -> anchor.minusMonths(offset.toLong())
        }
        val end = when (period) {
            ChartPeriod.DAILY -> start.plusDays(1)
            ChartPeriod.WEEKLY -> start.plusWeeks(1)
            ChartPeriod.MONTHLY -> start.plusMonths(1)
        }
        val startMs = start.atStartOfDay(clock.zone).toInstant().toEpochMilli()
        val endMs = end.atStartOfDay(clock.zone).toInstant().toEpochMilli()
        val sessions = history.filter { it.date >= startMs && it.date < endMs }
        val label = when (period) {
            ChartPeriod.DAILY -> listOf("월", "화", "수", "목", "금", "토", "일")[start.dayOfWeek.value - 1]
            ChartPeriod.WEEKLY -> "${start.monthValue}/${start.dayOfMonth}"
            ChartPeriod.MONTHLY -> "${start.monthValue}월"
        }
        val rangeLabel = when (period) {
            ChartPeriod.DAILY -> ""
            ChartPeriod.WEEKLY -> end.minusDays(1).let { "$label ~ ${it.monthValue}/${it.dayOfMonth} 주간" }
            ChartPeriod.MONTHLY -> "${start.year}년 $label"
        }
        ChartBar(
            label = label,
            score = if (sessions.isEmpty()) 0 else sessions.map { it.score }.average().toInt(),
            hasData = sessions.isNotEmpty(),
            sessions = sessions,
            rangeLabel = rangeLabel
        )
    }
}
