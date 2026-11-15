package com.example.quietnight.domain

import com.example.quietnight.domain.monitoring.AudioReadWindow
import org.junit.Assert.assertEquals
import org.junit.Test

class AudioReadWindowTest {
    @Test fun stalledReadDoesNotBecomeSeveralSecondsOfSnoring() {
        assertEquals(AudioReadWindow(100, 4900), AudioReadWindow.from(4410, 44100, 5000))
    }
    @Test fun errorAndEmptyReadAreMissingRatherThanSilence() {
        assertEquals(AudioReadWindow(0, 20), AudioReadWindow.from(-3, 44100, 20))
        assertEquals(AudioReadWindow(0, 20), AudioReadWindow.from(0, 44100, 20))
    }
    @Test fun partialBuffersAndShortElapsedIntervalsCannotOverCount() {
        assertEquals(AudioReadWindow(50, 50), AudioReadWindow.from(2205, 44100, 100))
        assertEquals(AudioReadWindow(90, 0), AudioReadWindow.from(4410, 44100, 90))
        assertEquals(AudioReadWindow(0, 0), AudioReadWindow.from(4410, 44100, -100))
    }
}
