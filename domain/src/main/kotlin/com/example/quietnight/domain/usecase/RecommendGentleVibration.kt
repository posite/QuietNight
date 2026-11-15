package com.example.quietnight.domain.usecase

import com.example.quietnight.domain.model.SleepSession
import java.time.Clock
import java.time.LocalDate
import com.example.quietnight.domain.model.MorningFeedback

fun recommendGentleVibration(history: List<SleepSession>, clock: Clock): Boolean {
    val today = LocalDate.now(clock)
    val start = today.minusDays(6).atStartOfDay(clock.zone).toInstant().toEpochMilli()
    val end = today.plusDays(1).atStartOfDay(clock.zone).toInstant().toEpochMilli()
    return history.filter { it.date in start until end && it.insights.vibrations.isNotEmpty() }
        .distinctBy { it.date }
        .sortedByDescending { it.date }
        .filter { it.feedback != MorningFeedback.NONE }
        .take(3).count { it.feedback == MorningFeedback.DISTURBED } >= 2
}
