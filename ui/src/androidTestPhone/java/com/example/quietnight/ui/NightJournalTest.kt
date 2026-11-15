package com.example.quietnight.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.quietnight.domain.model.*
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NightJournalTest {
    @get:Rule val compose = createComposeRule()
    private val session = SleepSession(date = 1000, score = 80, snoreTime = 1000, sleepTime = 60000,
        timelineCsv = "10,80,20", insights = NightInsights(observedMs = 60000, missingMs = 1000,
            ambientMs = 20000, ambientLoudMs = 5000,
            vibrations = listOf(VibrationObservation(1500, 80, 30))))

    @Test fun postcardDetailsOmitMeasurementDiagnostics() {
        compose.setContent { MaterialTheme {
            NightJournal(SnoreState(sessionHistory = listOf(session)), {}, {})
        } }
        compose.onNodeWithText("엽서와 성적표 열기").performClick()
        compose.onNodeWithText("별이 높을수록 소리가 컸어요.").assertIsDisplayed()
        listOf("수면 단계나 숙면", "측정 확인", "오디오 읽기 확인", "오디오 60초",
            "확인된 누락", "시작 환경 확인").forEach { text ->
            compose.onNode(hasText(text, substring = true) and hasAnyAncestor(isDialog())).assertDoesNotExist()
        }
    }

    @Test fun vibrationReportFollowsSleepStatsAndNoiseChart() {
        compose.setContent { MaterialTheme {
            DayDetailDialog(session, onDismiss = {})
        } }
        compose.onNodeWithText("진동 성적표").performScrollTo().assertIsDisplayed()
        val reportTop = compose.onNodeWithText("진동 성적표").fetchSemanticsNode().positionInRoot.y
        listOf("수면 점수", "코골이", "수면 시간", "밤새 소음 추이", "총 3분 기록").forEach { label ->
            val node = compose.onNodeWithText(label).fetchSemanticsNode()
            assertTrue("$label 아래에 진동 성적표가 표시되어야 합니다.",
                node.positionInRoot.y + node.size.height <= reportTop)
        }
    }

    @Test fun postcardCaptionHasSpaceBelowArtwork() {
        compose.setContent { MaterialTheme {
            DayDetailDialog(session, onDismiss = {})
        } }
        val artwork = compose.onNodeWithContentDescription(
            "3개 소리 구간의 밤 엽서. 왼쪽에서 오른쪽으로 기록 순서이며, 별이 높을수록 소리가 컸습니다."
        ).getUnclippedBoundsInRoot()
        val caption = compose.onNodeWithText("별이 높을수록 소리가 컸어요.").getUnclippedBoundsInRoot()
        assertTrue("그림과 설명 사이에 12dp 이상의 여백이 있어야 합니다.",
            caption.top - artwork.bottom >= 12.dp)
    }

    @Test fun vibrationReportUsesPlainSoundChangesWithoutTechnicalDetails() {
        val mixed = session.copy(insights = session.insights.copy(vibrations = listOf(
            VibrationObservation(1500, 80, 30), VibrationObservation(2500, 80, 79),
            VibrationObservation(3500, 80))))
        compose.setContent { MaterialTheme {
            DayDetailDialog(mixed, onDismiss = {})
        } }
        listOf("진동 알림" to "3회", "소리가 작아짐" to "1회", "비슷하거나 커짐" to "1회",
            "비교할 기록 부족" to "1회").forEach { (label, count) ->
            compose.onNode(hasText(label) and hasText(count)).performScrollTo().assertIsDisplayed()
        }
        listOf("소리 크기만 비교합니다.", "진동의 효과나 숙면", "PCM", "이전 평균", "이후 평균")
            .forEach { text -> compose.onNodeWithText(text, substring = true).assertDoesNotExist() }
    }

    @Test fun postcardDetailsShowReportWithoutMorningFeedback() {
        compose.setContent { MaterialTheme {
            NightJournal(SnoreState(sessionHistory = listOf(session)), {}, {})
        } }
        compose.onNodeWithText("첫 밤 엽서를 기다리고 있어요").assertDoesNotExist()
        compose.onNodeWithText("엽서와 성적표 열기").performClick()
        assertNoMorningFeedback()
        compose.onNodeWithText("진동 성적표").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("닫기").performScrollTo().performClick()
        compose.onNodeWithText("엽서와 성적표 열기").assertIsDisplayed()
    }

    @Test fun monitoringDetailsShowReportWithoutMorningFeedback() {
        compose.setContent { MaterialTheme {
            NightJournal(SnoreState(isMonitoring = true, sessionHistory = listOf(session),
                suggestGentleVibration = true), {}, {})
        } }
        compose.onNodeWithText("엽서와 성적표 열기").performClick()
        assertNoMorningFeedback()
        compose.onNodeWithText("진동 성적표").performScrollTo().assertIsDisplayed()
        compose.onNode(hasText("여유 모드 적용") and hasAnyAncestor(isDialog()))
            .performScrollTo().assertIsNotEnabled()
    }

    private fun assertNoMorningFeedback() {
        listOf("아침에는 어땠나요?", "괜찮았어요", "진동이 방해됐어요", "답변 지우기",
            "측정을 종료한 뒤 답변할 수 있어요.", "진동 요청 기록이 없어 진동 피드백을 받지 않아요.")
            .forEach { text -> compose.onNodeWithText(text, substring = true).assertDoesNotExist() }
    }
}
