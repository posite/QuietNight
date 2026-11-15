package com.example.quietnight.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MonitoringCadenceTest {

    @Test
    fun databaseAndNotificationUseIndependentIntervals() {
        val cadence = MonitoringCadence(startedAtMs = 1_000)

        assertFalse(cadence.shouldFlush(30_999))
        assertTrue(cadence.shouldFlush(31_000))
        assertFalse(cadence.shouldNotify(60_999))
        assertTrue(cadence.shouldNotify(61_000))

        cadence.markFlushed(31_000)
        cadence.markNotified(61_000)
        assertFalse(cadence.shouldFlush(60_999))
        assertTrue(cadence.shouldFlush(61_000))
        assertFalse(cadence.shouldNotify(120_999))
        assertTrue(cadence.shouldNotify(121_000))
    }

    @Test
    fun immediateSaveRestartsOnlyTheDatabaseInterval() {
        val cadence = MonitoringCadence(startedAtMs = 0)

        cadence.markFlushed(20_000)

        assertFalse(cadence.shouldFlush(49_999))
        assertTrue(cadence.shouldFlush(50_000))
        assertTrue(cadence.shouldNotify(60_000))
    }
}
