package com.example.quietnight.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import com.example.quietnight.domain.model.*
import com.example.quietnight.ui.theme.QuietNightTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class HomeDashboardTest {
    @get:Rule val compose = createComposeRule()
    private val zone = ZoneId.of("Asia/Seoul")
    private val latest = SleepSession(
        date = LocalDate.of(2026, 10, 10).atStartOfDay(zone).toInstant().toEpochMilli(),
        score = 84, snoreTime = 12 * 60_000L, sleepTime = 90 * 60_000L,
        timelineCsv = "12,40,22", insights = NightInsights(observedMs = 90 * 60_000L,
            vibrations = listOf(VibrationObservation(1000, 80, 30), VibrationObservation(2000, 70)))
    )
    private val older = latest.copy(date = LocalDate.of(2026, 10, 9)
        .atStartOfDay(zone).toInstant().toEpochMilli(), score = 60, sleepTime = 45 * 60_000L)

    @Test fun lightEmptyHomeShowsFirstNightAndPreparation() = checkEmptyHome(ThemeMode.LIGHT)
    @Test fun darkEmptyHomeShowsFirstNightAndPreparation() = checkEmptyHome(ThemeMode.DARK)
    @Test fun lightHomeShowsLatestNightAndOpensAnalysis() = checkLatestNight(ThemeMode.LIGHT)
    @Test fun darkHomeShowsLatestNightAndOpensAnalysis() = checkLatestNight(ThemeMode.DARK)
    @Test fun lightLargeTextCanScrollWhileStartRemainsVisible() = checkLargeText(ThemeMode.LIGHT)
    @Test fun darkLargeTextCanScrollWhileStartRemainsVisible() = checkLargeText(ThemeMode.DARK)

    @Test fun monitoringShowsLiveInformationAndKeepsStopAction() {
        var stops = 0
        showHome(ThemeMode.DARK, SnoreState(isMonitoring = true, currentDb = 43,
            elapsedTime = "00:12:34", recentLogs = listOf("지속 소음 감지"))) { stops++ }
        compose.onNodeWithText("지금 밤을 기록하고 있어요").assertIsDisplayed()
        compose.onNodeWithText("현재 소음: 43 dB").assertIsDisplayed()
        compose.onNodeWithText("00:12:34").assertIsDisplayed()
        compose.onNodeWithText("지속 소음 감지").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("첫 밤을 준비해 볼까요?").assertDoesNotExist()
        compose.onNodeWithText("모니터링 종료").assertIsDisplayed().performClick()
        assertEquals(1, stops)
    }

    @Test fun legacyRecordDoesNotInventSoundArtOrObservation() {
        showHome(ThemeMode.LIGHT, SnoreState(sessionHistory = listOf(
            latest.copy(timelineCsv = "", insights = NightInsights())
        )))
        compose.onNodeWithText("최근 밤의 기록").assertIsDisplayed()
        compose.onNodeWithText("측정 정보 부족").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("1분 단위 소리 기록이 아직 부족해요.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("3개 소리 구간의 밤 엽서. 왼쪽에서 오른쪽으로 기록 순서이며, 별이 높을수록 소리가 컸습니다.")
            .assertDoesNotExist()
    }

    private fun checkEmptyHome(mode: ThemeMode) {
        var starts = 0
        showHome(mode, onMonitor = { starts++ })
        compose.onNodeWithText("첫 밤을 준비해 볼까요?").assertIsDisplayed()
        compose.onNodeWithText("오늘 밤 준비").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("충전기를 연결해 주세요").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("휴대폰을 침대 가까이에").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("최근 밤의 기록").assertDoesNotExist()
        compose.onNodeWithText("수면 시작하기").assertIsDisplayed().performClick()
        assertEquals(1, starts)
    }

    private fun checkLargeText(mode: ThemeMode) {
        var starts = 0
        showHome(mode, fontScale = 2f, onMonitor = { starts++ })
        compose.onNodeWithText("첫 밤을 준비해 볼까요?").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("소리 기록 안내").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("수면 시작하기").assertIsDisplayed().performClick()
        assertEquals(1, starts)
    }

    private fun checkLatestNight(mode: ThemeMode) {
        var starts = 0
        compose.setContent { QuietNightTheme(themeMode = mode) {
            QuietNightApp(
                state = SnoreState(sessionHistory = listOf(older, latest)),
                settings = AppSettings(themeMode = mode),
                onStartMonitoring = { starts++ }, onStopMonitoring = {},
                onThresholdChange = {}, onVibrationToggle = {}, onVibrationStrengthChange = {},
                onRecordClipsToggle = {}, onResetData = {}, onFeedback = { _, _ -> },
                onApplyGentle = {}, onGentleVibration = {}, onThemeModeChange = {},
                clipProvider = { emptyList() }
            )
        } }
        compose.onNodeWithText("최근 밤의 기록").assertIsDisplayed()
        compose.onNodeWithText("1시간 30분").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("진동 요청 2회").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("첫 밤을 준비해 볼까요?").assertDoesNotExist()
        compose.onNodeWithText("밤 엽서 보기").performScrollTo().performClick()
        compose.onNodeWithText("분석").assertIsSelected()
        assertEquals(0, starts)
    }

    private fun showHome(mode: ThemeMode, state: SnoreState = SnoreState(), fontScale: Float = 1f,
        onMonitor: () -> Unit = {}) {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                QuietNightTheme(themeMode = mode) { HomeScreen(state, {}, onMonitor) }
            }
        }
    }
}
