package com.example.quietnight.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.quietnight.domain.model.AppSettings
import com.example.quietnight.domain.model.ThemeMode
import com.example.quietnight.ui.theme.QuietNightTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BottomNavAndEmptyAnalysisTest {
    @get:Rule val compose = createComposeRule()
    private var indicatorColor = Color.Unspecified
    private var navigationColor = Color.Unspecified
    private var bottomButtonAllowance = 0f

    @Test fun lightThemeClearlyHighlightsOnlyTheCurrentTab() = checkTabSelection(ThemeMode.LIGHT)
    @Test fun darkThemeClearlyHighlightsOnlyTheCurrentTab() = checkTabSelection(ThemeMode.DARK)
    @Test fun lightEmptyAnalysisGuidesToMonitoringWithoutStartingIt() = checkEmptyAnalysis(ThemeMode.LIGHT)
    @Test fun darkEmptyAnalysisGuidesToMonitoringWithoutStartingIt() = checkEmptyAnalysis(ThemeMode.DARK)
    @Test fun lightTabLabelsFitAtNormalFontSize() = checkLabelLayout(ThemeMode.LIGHT, 1f)
    @Test fun darkTabLabelsFitAtNormalFontSize() = checkLabelLayout(ThemeMode.DARK, 1f)
    @Test fun lightTabLabelsFitAtLargeFontSize() = checkLabelLayout(ThemeMode.LIGHT, 2f)
    @Test fun darkTabLabelsFitAtLargeFontSize() = checkLabelLayout(ThemeMode.DARK, 2f)
    @Test fun lightNavigationBarReachesTheBottom() = checkBarReachesBottom(ThemeMode.LIGHT)
    @Test fun darkNavigationBarReachesTheBottom() = checkBarReachesBottom(ThemeMode.DARK)
    @Test fun lightNavigationBarHasNoBottomOutline() = checkBarHasNoBottomOutline(ThemeMode.LIGHT)
    @Test fun darkNavigationBarHasNoBottomOutline() = checkBarHasNoBottomOutline(ThemeMode.DARK)
    @Test fun lightTabButtonsStayNearTheBottom() = checkButtonsNearBottom(ThemeMode.LIGHT)
    @Test fun darkTabButtonsStayNearTheBottom() = checkButtonsNearBottom(ThemeMode.DARK)

    private fun showApp(mode: ThemeMode, onStart: () -> Unit = {}, fontScale: Float = 1f) {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                QuietNightTheme(themeMode = mode) {
                    val density = LocalDensity.current
                    bottomButtonAllowance = WindowInsets.navigationBars.getBottom(density) +
                        with(density) { 16.dp.toPx() }
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    navigationColor = MaterialTheme.colorScheme.surface
                    QuietNightApp(
                        state = SnoreState(), settings = AppSettings(themeMode = mode),
                        onStartMonitoring = onStart, onStopMonitoring = {},
                        onThresholdChange = {}, onVibrationToggle = {}, onVibrationStrengthChange = {},
                        onRecordClipsToggle = {}, onResetData = {}, onFeedback = { _, _ -> },
                        onApplyGentle = {}, onGentleVibration = {}, onThemeModeChange = {},
                        clipProvider = { emptyList() }
                    )
                }
            }
        }
    }

    private fun navigationBar() = compose.onNode(
        SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup)
    )

    private fun checkBarReachesBottom(mode: ThemeMode) {
        showApp(mode)
        val rootBottom = compose.onRoot().fetchSemanticsNode().boundsInRoot.bottom
        val barBottom = navigationBar().fetchSemanticsNode().boundsInRoot.bottom
        assertEquals("하단 바는 화면 아래까지 이어져야 합니다.", rootBottom, barBottom, 0.5f)
    }

    private fun checkBarHasNoBottomOutline(mode: ThemeMode) {
        showApp(mode)
        val pixels = navigationBar().captureToImage().toPixelMap()
        assertEquals("탭 아래에 테두리 선이 없어야 합니다.", navigationColor,
            pixels[pixels.width / 4, pixels.height - 1])
    }

    private fun checkButtonsNearBottom(mode: ThemeMode) {
        showApp(mode)
        val barBottom = navigationBar().fetchSemanticsNode().boundsInRoot.bottom
        listOf("홈", "수면", "분석").forEach { label ->
            val buttonBottom = compose.onNodeWithText(label).fetchSemanticsNode().boundsInRoot.bottom
            assertTrue("$label 버튼 아래에 추가 제스처 여백을 두면 안 됩니다.",
                barBottom - buttonBottom <= bottomButtonAllowance + 0.5f)
        }
    }

    private fun checkLabelLayout(mode: ThemeMode, fontScale: Float) {
        showApp(mode, fontScale = fontScale)
        listOf("홈", "수면", "분석").forEach { label ->
            compose.onNodeWithText(label).performClick()
            val results = mutableListOf<TextLayoutResult>()
            compose.onNodeWithText(label, useUnmergedTree = true)
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
            assertTrue("탭 라벨의 실제 글자 배치를 확인해야 합니다.", results.isNotEmpty())
            val layout = results.single()
            assertFalse("$label 라벨의 세로 공간이 부족합니다. 글자 크기: $fontScale 배",
                layout.didOverflowHeight)
            // 문단의 가로 측정값은 부모의 여유 폭을 포함하므로 실제 글자 경계로 확인한다.
            label.indices.forEach { index ->
                val glyph = layout.getBoundingBox(index)
                assertTrue("$label 글자가 라벨 영역 밖으로 잘립니다. 글자 크기: $fontScale 배",
                    glyph.left >= 0 && glyph.top >= 0 &&
                        glyph.right <= layout.size.width && glyph.bottom <= layout.size.height)
            }
        }
    }

    private fun checkTabSelection(mode: ThemeMode) {
        showApp(mode)
        listOf("홈", "수면", "분석", "홈").forEach { current ->
            compose.onNodeWithText(current).performClick().assertIsSelected()
            listOf("홈", "수면", "분석").filter { it != current }.forEach { other ->
                compose.onNodeWithText(other).assertIsNotSelected()
            }
            // 터치 잔물결이 사라진 뒤 선택 배경을 확인한다. 기기의 잔물결은 실제 시간으로 움직인다.
            compose.waitUntil(timeoutMillis = 3_000) {
                val pixels = compose.onNodeWithText(current).captureToImage().toPixelMap()
                pixels[pixels.width / 2, pixels.height - 4] == indicatorColor
            }
        }
        compose.onNodeWithContentDescription("설정").performClick()
        compose.onNodeWithText("홈").assertIsSelected()
        compose.onNodeWithContentDescription("뒤로").performClick()
        compose.onNodeWithText("홈").assertIsSelected()
    }

    private fun checkEmptyAnalysis(mode: ThemeMode) {
        var starts = 0
        showApp(mode, onStart = { starts++ })
        compose.onNodeWithText("분석").performClick()
        compose.onNodeWithText("첫 밤 엽서를 기다리고 있어요").assertIsDisplayed()
        compose.onNodeWithText("소리 변화").assertIsDisplayed()
        compose.onNodeWithText("측정 시간").assertIsDisplayed()
        compose.onNodeWithText("진동 기록").assertIsDisplayed()
        compose.onNodeWithText("수면 탭으로 가기").performScrollTo().performClick()
        compose.onNodeWithText("수면").assertIsSelected()
        compose.runOnIdle { assertEquals(0, starts) }
    }
}
