package com.example.quietnight.ui

import com.example.quietnight.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class NightPresentationTest {
    @Test fun postcardIsDeterministicAndPreservesTheSoundOrder() {
        assertEquals(listOf(10, 80, 20), postcardLevels("10,80,20"))
        assertEquals(postcardLevels("10,80,20"), postcardLevels("10,80,20"))
        assertTrue(postcardLevels("bad,-1,101").isEmpty())
    }
    @Test fun allNightPostcardIsBoundedForWatchRendering() {
        val levels = postcardLevels(List(480) { it % 100 }.joinToString(","))
        assertTrue(levels.size in 1..60)
        assertEquals(7, levels.first())
        assertEquals(79, levels.last())
    }
    @Test fun reportDoesNotTreatUnknownObservationsAsQuietOrSuccessful() {
        val session = SleepSession(date = 1000, score = 90, snoreTime = 0, sleepTime = 20000,
            insights = NightInsights(observedMs = 20000, vibrations = listOf(
                VibrationObservation(1100, 80, 30), VibrationObservation(1200, 80),
                VibrationObservation(1300, 80, 79))))
        assertEquals("진동 알림 3회\n알림 후 소리가 작아짐 1회\n알림 후 소리가 비슷하거나 커짐 1회\n소리 기록 부족으로 비교하지 못함 1회",
            vibrationReport(session))
        assertEquals("오디오 읽기 확인", measurementLabel(session))
        assertEquals("측정 정보 부족", measurementLabel(session.copy(insights = NightInsights())))
    }

    @Test fun reportWithoutVibrationsDoesNotInventSoundComparisons() {
        val session = SleepSession(date = 1000, score = 90, snoreTime = 0, sleepTime = 20000)
        assertEquals("진동 알림 기록이 없어요.", vibrationReport(session))
    }

    @Test fun reportPreservesTheExistingThreeUnitDecreaseBoundary() {
        val session = SleepSession(date = 1000, score = 90, snoreTime = 0, sleepTime = 20000,
            insights = NightInsights(vibrations = listOf(
                VibrationObservation(1100, 80, 77), VibrationObservation(1200, 80, 78),
                VibrationObservation(1300, 80, 90), VibrationObservation(1400, 80))))
        assertEquals("진동 알림 4회\n알림 후 소리가 작아짐 1회\n알림 후 소리가 비슷하거나 커짐 2회\n소리 기록 부족으로 비교하지 못함 1회",
            vibrationReport(session))
    }
}
