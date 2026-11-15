package com.example.quietnight.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.quietnight.domain.model.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class NightJournalTest {
    @get:Rule val compose = createComposeRule()
    private val session = SleepSession(date = 1000, score = 80, snoreTime = 1000, sleepTime = 60000,
        timelineCsv = "10,80,20", insights = NightInsights(observedMs = 60000,
            vibrations = listOf(VibrationObservation(1500, 80, 30))))

    @Test fun postcardDetailsAllowMorningFeedback() {
        var feedback = MorningFeedback.NONE
        compose.setContent { MaterialTheme {
            NightJournal(SnoreState(sessionHistory = listOf(session)), {}, { _, value -> feedback = value }, {})
        } }
        compose.onNodeWithText("첫 밤 엽서를 기다리고 있어요").assertDoesNotExist()
        compose.onNodeWithText("엽서와 성적표 열기").performClick()
        compose.onNodeWithText("진동이 방해됐어요").performScrollTo().performClick()
        assertEquals(MorningFeedback.DISTURBED, feedback)
    }

    @Test fun feedbackCannotBeSubmittedWhileMonitoring() {
        compose.setContent { MaterialTheme {
            NightJournal(SnoreState(isMonitoring = true, sessionHistory = listOf(session)), {}, { _, _ -> }, {})
        } }
        compose.onNodeWithText("엽서와 성적표 열기").performClick()
        compose.onNodeWithText("진동이 방해됐어요").performScrollTo().assertIsNotEnabled()
    }
}
