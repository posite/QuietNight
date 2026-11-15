package com.example.quietnight.data

import org.junit.Assert.assertEquals
import org.junit.Test

class DomainMappersTest {
    @Test fun nightInsightsAndFeedbackAreMappedWithoutLosingUnknownComparisons() {
        val entity = SleepSession(date = 1000, score = 80, snoreTime = 1000, sleepTime = 60000,
            observedMs = 50000, missingMs = 10000, loudMs = 5000, ambientMs = 20000, ambientLoudMs = 1000,
            vibrationEventsCsv = "v1;1500:80:?;2500:80:20", morningFeedback = "DISTURBED")
        val result = entity.toDomain()
        assertEquals(50000L, result.insights.observedMs)
        assertEquals(10000L, result.insights.missingMs)
        assertEquals(5000L, result.insights.loudMs)
        assertEquals(20000L, result.insights.ambientMs)
        assertEquals(1000L, result.insights.ambientLoudMs)
        assertEquals(listOf(null, 20), result.insights.vibrations.map { it.after })
        assertEquals(com.example.quietnight.domain.model.MorningFeedback.DISTURBED, result.feedback)
        assertEquals(com.example.quietnight.domain.model.MorningFeedback.NONE,
            entity.copy(morningFeedback = "unknown").toDomain().feedback)
    }
    @Test fun sessionMappingPreservesEveryPersistedValueWithoutConvertingMilliseconds() {
        val entity = SleepSession(7, 1700000000000, 0, 123456, 987654, "12,0,88")
        assertEquals(com.example.quietnight.domain.model.SleepSession(
            7, 1700000000000, 0, 123456, 987654, "12,0,88"), entity.toDomain())
    }

    @Test fun clipMappingPreservesIdentityDateTimestampAndPlaybackPath() {
        val entity = SnoreClip(4, 1700000000000, 1700000012345, 82, "/private/snore_clips/4.wav", 6150)
        assertEquals(com.example.quietnight.domain.model.SnoreClip(
            4, 1700000000000, 1700000012345, 82, "/private/snore_clips/4.wav", 6150), entity.toDomain())
    }
}
