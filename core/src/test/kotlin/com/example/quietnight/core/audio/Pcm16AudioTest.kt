package com.example.quietnight.core.audio

import kotlin.math.sqrt
import org.junit.Assert.*
import org.junit.Test

class Pcm16AudioTest {
    @Test fun emptyInputHasZeroRms() {
        assertEquals(0.0, Pcm16Audio.rms(shortArrayOf(), 0), 0.0)
    }

    @Test fun silenceHasZeroRms() {
        assertEquals(0.0, Pcm16Audio.rms(shortArrayOf(0, 0), 2), 0.0)
    }

    @Test fun oppositePolaritiesDoNotCancelEachOther() {
        assertEquals(100.0, Pcm16Audio.rms(shortArrayOf(100, -100), 2), 0.0)
    }

    @Test fun computesRootMeanSquareRatherThanMeanAbsoluteAmplitude() {
        assertEquals(sqrt(12.5), Pcm16Audio.rms(shortArrayOf(3, -4), 2), 0.0)
    }

    @Test fun ignoresTheUnreadPartOfAReusedBuffer() {
        assertEquals(10.0, Pcm16Audio.rms(shortArrayOf(10, -10, Short.MAX_VALUE), 2), 0.0)
        assertEquals(0.0, Pcm16Audio.rms(shortArrayOf(Short.MAX_VALUE), 0), 0.0)
    }

    @Test fun extremeSignedPcmValuesAreSquaredWithoutOverflow() {
        assertEquals(32768.0, Pcm16Audio.rms(shortArrayOf(Short.MIN_VALUE), 1), 0.0)
        assertEquals(32767.0, Pcm16Audio.rms(shortArrayOf(Short.MAX_VALUE), 1), 0.0)
    }

    @Test fun cannotReadBeforeOrBeyondTheBuffer() {
        assertThrows(IllegalArgumentException::class.java) { Pcm16Audio.rms(shortArrayOf(1), -1) }
        assertThrows(IllegalArgumentException::class.java) { Pcm16Audio.rms(shortArrayOf(1), 2) }
    }

    @Test fun doesNotModifySamplesNeededByOtherConsumers() {
        val samples = shortArrayOf(10, -10, 32000)
        val original = samples.copyOf()
        Pcm16Audio.rms(samples, 2)
        assertArrayEquals(original, samples)
    }
}
