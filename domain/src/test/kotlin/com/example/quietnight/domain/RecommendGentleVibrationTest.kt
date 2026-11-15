package com.example.quietnight.domain

import com.example.quietnight.domain.model.*
import com.example.quietnight.domain.usecase.recommendGentleVibration
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class RecommendGentleVibrationTest {
    private val clock = Clock.fixed(Instant.parse("2026-09-15T01:00:00Z"), ZoneId.of("Asia/Seoul"))
    private fun night(date: String, feedback: MorningFeedback = MorningFeedback.DISTURBED) = SleepSession(
        date = LocalDate.parse(date).atStartOfDay(clock.zone).toInstant().toEpochMilli(),
        score = 90, snoreTime = 0, sleepTime = 60000, feedback = feedback,
        insights = NightInsights(vibrations = listOf(VibrationObservation(clock.millis(), 80, 20))))

    @Test fun twoRecentDisturbedDaysSuggestLessFrequentVibration() {
        assertTrue(recommendGentleVibration(listOf(night("2026-09-14"), night("2026-09-15")), clock))
    }
    @Test fun oldFutureAndDuplicateDatesDoNotCreateASuggestion() {
        assertFalse(recommendGentleVibration(listOf(night("2026-09-01"), night("2026-09-02")), clock))
        assertFalse(recommendGentleVibration(listOf(night("2026-09-14"), night("2026-09-16")), clock))
        assertFalse(recommendGentleVibration(List(3) { night("2026-09-14") }, clock))
    }
    @Test fun soundReductionAloneOrFeedbackWithoutVibrationCannotJustifyPersonalization() {
        assertFalse(recommendGentleVibration(listOf(night("2026-09-14", MorningFeedback.COMFORTABLE),
            night("2026-09-15", MorningFeedback.COMFORTABLE)), clock))
        assertFalse(recommendGentleVibration(listOf(night("2026-09-14"),
            night("2026-09-15").copy(insights = NightInsights())), clock))
    }
}
