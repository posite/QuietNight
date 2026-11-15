package com.example.quietnight.service

import java.util.Random
import kotlin.math.log10
import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Test

class AudioLevelTest {
    @Test fun silenceAndUnitAmplitudeKeepTheExistingZeroFloor() {
        assertEquals(0, calculateDecibels(shortArrayOf(0, 0), 2))
        assertEquals(0, calculateDecibels(shortArrayOf(1, -1), 2))
    }

    @Test fun knownPcmAmplitudesKeepTheirExistingLevels() {
        assertEquals(20, calculateDecibels(shortArrayOf(10, -10), 2))
        assertEquals(40, calculateDecibels(shortArrayOf(100, -100), 2))
    }

    @Test fun usesOnlySuccessfullyReadSamplesRatherThanStaleBufferContents() {
        assertEquals(20, calculateDecibels(shortArrayOf(10, -10, Short.MAX_VALUE), 2))
    }

    @Test fun fullScaleSignedPcmDoesNotOverflowOrChangeCalibration() {
        assertEquals(90, calculateDecibels(shortArrayOf(Short.MIN_VALUE), 1))
        assertEquals(90, calculateDecibels(shortArrayOf(Short.MAX_VALUE), 1))
    }

    @Test fun fractionalDecibelsAreTruncatedAsBefore() {
        assertEquals(29, calculateDecibels(shortArrayOf(30, -30), 2))
    }

    @Test fun matchesTheOriginalServiceFormulaForDeterministicPartialReads() {
        val random = Random(20260915L)
        repeat(100) {
            val samples = ShortArray(4410) { random.nextInt(65536).minus(32768).toShort() }
            val count = random.nextInt(samples.size) + 1
            // 서비스에서 분리하기 전 계산식과 결과를 비교해 민감도 변화를 방지한다.
            var sumSq = 0.0
            for (i in 0 until count) {
                val sample = samples[i].toDouble()
                sumSq += sample * sample
            }
            val rms = sqrt(sumSq / count)
            val expected = (20 * log10(rms.coerceAtLeast(1.0))).toInt().coerceIn(0, 100)
            assertEquals("실제 읽은 샘플 수: $count", expected, calculateDecibels(samples, count))
        }
    }
}
