package com.example.quietnight.domain

import com.example.quietnight.domain.monitoring.SnoreDetector

import org.junit.Assert.*
import org.junit.Test

class SnoreDetectorTest {
    @Test
    fun transientNoiseIsDiscarded() {
        val detector = SnoreDetector()
        repeat(3) { assertEquals(0L, detector.update(80, 75, 100).snoreMs) }
        assertEquals(0L, detector.update(75, 75, 100).snoreMs)
        assertEquals(0L, detector.update(80, 75, 100).snoreMs)
    }

    @Test
    fun confirmsAtExactly400MsIncludingTheFirstChunk() {
        val detector = SnoreDetector()
        repeat(3) { assertFalse(detector.update(80, 75, 100).justConfirmed) }
        val result = detector.update(80, 75, 100)
        assertTrue(result.justConfirmed)
        assertTrue(result.confirmed)
        assertEquals(400L, result.snoreMs)
    }

    @Test
    fun confirmedTimeIsCountedOnce() {
        val detector = SnoreDetector()
        val results = List(10) { detector.update(80, 75, 100) }
        assertEquals(1000L, results.sumOf { it.snoreMs })
        assertEquals(1, results.count { it.justConfirmed })
        assertFalse(detector.update(74, 75, 100).confirmed)
    }

    @Test
    fun usesActualChunkDurations() {
        val detector = SnoreDetector()
        detector.update(80, 75, 150)
        detector.update(80, 75, 90)
        assertEquals(410L, detector.update(80, 75, 170).snoreMs)
    }

    @Test
    fun resetDoesNotCarryPendingTimeIntoAnotherDay() {
        val detector = SnoreDetector()
        detector.update(80, 75, 300)
        detector.reset()
        assertFalse(detector.update(80, 75, 100).confirmed)
        assertEquals(400L, detector.update(80, 75, 300).snoreMs)
    }

    @Test
    fun thresholdChangesApplyToTheNextChunk() {
        val detector = SnoreDetector()
        detector.update(80, 75, 300)
        assertFalse(detector.update(80, 85, 100).confirmed)
        assertFalse(detector.update(90, 85, 100).confirmed)
    }

    @Test
    fun nonPositiveDurationsCannotConfirmOrSubtractTime() {
        val detector = SnoreDetector()
        assertEquals(0L, detector.update(80, 75, -100).snoreMs)
        assertFalse(detector.update(80, 75, 0).confirmed)
        assertEquals(400L, detector.update(80, 75, 400).snoreMs)
    }
}
