package com.example.quietnight.domain

import com.example.quietnight.domain.model.*
import com.example.quietnight.domain.monitoring.NightRecorder
import org.junit.Assert.*
import org.junit.Test

class NightRecorderTest {
    private var elapsed = 0L
    private fun feed(recorder: NightRecorder, level: Int, count: Int) {
        repeat(count) { elapsed += 100; recorder.sample(level, 75, 100, 0, elapsed) }
    }

    @Test fun calibrationNeedsTwentySecondsOfActualAudio() {
        val recorder = NightRecorder()
        feed(recorder, 80, 199)
        assertFalse(recorder.canVibrate(elapsed, 1_000_000 + elapsed, VibrationPolicy.STANDARD))
        feed(recorder, 80, 1)
        assertTrue(recorder.canVibrate(elapsed, 1_000_000 + elapsed, VibrationPolicy.STANDARD))
        assertEquals(20_000L, recorder.snapshot().ambientMs)
        assertEquals(20_000L, recorder.snapshot().ambientLoudMs)
    }

    @Test fun missingAudioIsNotCountedAsObservedAndInterruptsTheComparison() {
        val recorder = NightRecorder()
        feed(recorder, 80, 200)
        recorder.vibrationSent(elapsed, 1_000_000 + elapsed)
        elapsed += 5000
        recorder.sample(20, 75, 100, 4900, elapsed)
        feed(recorder, 20, 60)
        assertEquals(26_100L, recorder.snapshot().observedMs)
        assertEquals(4900L, recorder.snapshot().missingMs)
        assertNull(recorder.snapshot().vibrations.single().after)
    }

    @Test fun excludesActuatorNoiseAndRequiresFiveCompleteSecondsAfterIt() {
        val recorder = NightRecorder()
        feed(recorder, 80, 200)
        recorder.vibrationSent(elapsed, 1_000_000 + elapsed)
        feed(recorder, 100, 10)
        feed(recorder, 30, 49)
        assertNull(recorder.snapshot().vibrations.single().after)
        feed(recorder, 30, 1)
        assertEquals(VibrationObservation(1_020_000, 80, 30), recorder.snapshot().vibrations.single())
        assertFalse(recorder.canVibrate(elapsed, 1_000_000 + elapsed, VibrationPolicy.STANDARD))
    }

    @Test fun smallSchedulingJitterDoesNotDiscardAnOtherwiseCompleteWindow() {
        val recorder = NightRecorder()
        feed(recorder, 80, 200)
        recorder.vibrationSent(elapsed, 1_020_000)
        feed(recorder, 100, 10)
        repeat(50) {
            elapsed += 101
            recorder.sample(30, 75, 100, 1, elapsed)
        }
        assertEquals(30, recorder.snapshot().vibrations.single().after)
    }

    @Test fun restartPreservesDailyBudgetAndCooldownButNotPendingComparison() {
        val initial = NightInsights(vibrations = List(10) { VibrationObservation(1_000_000L + it, 80) })
        val recorder = NightRecorder(initial)
        feed(recorder, 30, 200)
        assertFalse(recorder.canVibrate(elapsed, 1_300_000, VibrationPolicy.GENTLE))
        assertFalse(recorder.canVibrate(elapsed, 1_000_010, VibrationPolicy.STANDARD))
        assertTrue(recorder.canVibrate(elapsed, 1_300_000, VibrationPolicy.STANDARD))
        assertEquals(initial.vibrations, recorder.snapshot().vibrations)
    }

    @Test fun oldAndPartialRecordsDoNotReceiveGoodQualityLabels() {
        assertEquals(MeasurementQuality.INSUFFICIENT, NightInsights().quality(100_000))
        assertEquals(MeasurementQuality.INTERRUPTED, NightInsights(observedMs = 20_000).quality(100_000))
        assertEquals(MeasurementQuality.NOISY,
            NightInsights(observedMs = 20_000, loudMs = 15_000).quality(20_000))
        assertEquals(MeasurementQuality.RECORDED, NightInsights(observedMs = 20_000).quality(20_000))
    }

    @Test fun stopDoesNotPretendAnUnfinishedObservationSucceeded() {
        val recorder = NightRecorder()
        feed(recorder, 80, 200)
        recorder.vibrationSent(elapsed, 1_020_000)
        feed(recorder, 30, 20)
        recorder.interrupt()
        feed(recorder, 30, 100)
        assertNull(recorder.snapshot().vibrations.single().after)
    }
}
