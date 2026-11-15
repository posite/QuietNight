package com.example.quietnight.data

import com.example.quietnight.domain.model.VibrationObservation
import org.junit.Assert.*
import org.junit.Test

class VibrationObservationCodecTest {
    @Test fun roundTripKeepsIncompleteAndCompletedObservationsDistinct() {
        val events = listOf(VibrationObservation(123, 80), VibrationObservation(456, 78, 0))
        assertEquals(events, VibrationObservationCodec.decode(VibrationObservationCodec.encode(events)))
    }
    @Test fun emptyLegacyDataAndUnknownVersionsDoNotInventEvents() {
        assertTrue(VibrationObservationCodec.decode("").isEmpty())
        assertTrue(VibrationObservationCodec.decode("v9;123:80:30").isEmpty())
    }
    @Test fun malformedRowsAreIgnoredWithoutCrashingHistory() {
        assertEquals(listOf(VibrationObservation(123, 80, 30)),
            VibrationObservationCodec.decode("v1;bad;123:80:30;-1:80:30;456:900:30;789:80:broken"))
    }
}
