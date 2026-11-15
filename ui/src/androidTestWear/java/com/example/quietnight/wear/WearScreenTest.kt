package com.example.quietnight.wear

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import com.example.quietnight.domain.model.AppSettings
import com.example.quietnight.domain.model.SleepSession
import com.example.quietnight.domain.model.NightInsights
import com.example.quietnight.domain.model.VibrationObservation
import com.example.quietnight.domain.model.MorningFeedback
import com.example.quietnight.ui.SnoreState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class WearScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun nightReportFeedbackIsReachableOnSmallWatchScreens() {
        var feedback = MorningFeedback.NONE
        val session = SleepSession(date = 1000, score = 90, snoreTime = 1000, sleepTime = 60000,
            timelineCsv = "10,80,20", insights = NightInsights(observedMs = 60000,
                vibrations = listOf(VibrationObservation(1500, 80, 30))))
        compose.setContent { MaterialTheme { AppScaffold {
            WearNightScreen(session, SnoreState(), { _, value -> feedback = value }, {}, {})
        } } }
        compose.onNodeWithTag("watch-list").performScrollToNode(hasText("진동이 방해됐어요"))
        compose.onNodeWithText("진동이 방해됐어요").performClick()
        assertEquals(MorningFeedback.DISTURBED, feedback)
    }

    @Test fun idleScreenOffersStart() {
        var clicks = 0
        compose.setContent {
            MaterialTheme { AppScaffold {
                WearMonitorScreen(SnoreState(), onToggle = { clicks++ }, onHistory = {}, onSettings = {})
            } }
        }
        compose.onNodeWithTag("watch-list").performScrollToNode(hasText("측정 시작"))
        compose.onNodeWithText("측정 시작").performClick()
        assertEquals(1, clicks)
    }

    @Test fun runningScreenOffersStop() {
        var clicks = 0
        compose.setContent {
            MaterialTheme { AppScaffold {
                WearMonitorScreen(SnoreState(isMonitoring = true), onToggle = { clicks++ }, onHistory = {}, onSettings = {})
            } }
        }
        compose.onNodeWithTag("watch-list").performScrollToNode(hasText("측정 종료"))
        compose.onNodeWithText("측정 종료").performClick()
        assertEquals(1, clicks)
    }

    @Test fun vibrationToggleUsesCurrentSetting() {
        var enabled = true
        compose.setContent {
            MaterialTheme { AppScaffold {
                WearSettingsScreen(AppSettings(), onThreshold = {}, onVibration = { enabled = it }, onBack = {})
            } }
        }
        compose.onNodeWithTag("watch-list").performScrollToNode(hasText("진동 켜짐"))
        compose.onNodeWithText("진동 켜짐").performClick()
        assertEquals(false, enabled)
    }
}
