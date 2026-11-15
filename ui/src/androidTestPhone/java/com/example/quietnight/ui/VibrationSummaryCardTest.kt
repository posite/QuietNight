package com.example.quietnight.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.example.quietnight.domain.model.*
import com.example.quietnight.ui.theme.QuietNightTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

class VibrationSummaryCardTest {
    @get:Rule val compose = createComposeRule()
    private val session = SleepSession(date = 1000, score = 80, snoreTime = 1000, sleepTime = 60000,
        insights = NightInsights(vibrations = listOf(
            VibrationObservation(1500, 80, 30), VibrationObservation(2500, 80, 79),
            VibrationObservation(3500, 80))))

    @Test fun lightSummaryAlignsLabelsAndCounts() = checkSummary(ThemeMode.LIGHT, 1f)
    @Test fun darkSummaryAlignsLabelsAndCounts() = checkSummary(ThemeMode.DARK, 1f)
    @Test fun lightLargeTextKeepsEveryValueReadable() = checkSummary(ThemeMode.LIGHT, 2f)
    @Test fun darkLargeTextKeepsEveryValueReadable() = checkSummary(ThemeMode.DARK, 2f)

    @Test fun emptySummaryDoesNotInventComparisonRows() {
        showReport(ThemeMode.DARK, 1f, session.copy(insights = NightInsights()))
        compose.onNodeWithText("진동 알림 기록이 없어요.").assertIsDisplayed()
        compose.onNodeWithText("소리가 작아짐").assertDoesNotExist()
        compose.onNodeWithText("비교할 기록 부족").assertDoesNotExist()
    }

    private fun showReport(mode: ThemeMode, fontScale: Float, value: SleepSession = session) {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                QuietNightTheme(themeMode = mode) {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp),
                        contentAlignment = Alignment.TopCenter) {
                        Column(Modifier.width(280.dp).verticalScroll(rememberScrollState())) {
                            NightVibrationReport(value, false, false, {}, null)
                        }
                    }
                }
            }
        }
    }

    private fun checkSummary(mode: ThemeMode, fontScale: Float) {
        showReport(mode, fontScale)
        compose.onNodeWithTag("vibration_summary").assertExists()
        compose.onNode(hasText("진동 알림") and hasText("3회")).assertExists()
        listOf("소리가 작아짐", "비슷하거나 커짐", "비교할 기록 부족").forEach { label ->
            compose.onNode(hasText(label) and hasText("1회")).performScrollTo().assertIsDisplayed()
            val layouts = mutableListOf<TextLayoutResult>()
            compose.onNodeWithText(label, useUnmergedTree = true)
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertTrue("$label 글자 배치를 확인해야 합니다.", layouts.isNotEmpty())
            assertFalse("큰 글자에서도 $label 문구가 잘리지 않아야 합니다.", layouts.single().hasVisualOverflow)
            val value = compose.onNode(hasText("1회") and hasAnySibling(hasText(label)), useUnmergedTree = true)
            val labelBounds = compose.onNodeWithText(label, useUnmergedTree = true).getUnclippedBoundsInRoot()
            val valueBounds = value.getUnclippedBoundsInRoot()
            val rowBounds = compose.onNode(hasText(label) and hasText("1회")).getUnclippedBoundsInRoot()
            assertTrue("$label 문구와 횟수가 겹치지 않아야 합니다.", labelBounds.right <= valueBounds.left)
            assertTrue("횟수는 표의 오른쪽 영역 안에 있어야 합니다.", valueBounds.right <= rowBounds.right)
        }
        if (fontScale == 1f) {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val output = File(checkNotNull(context.getExternalFilesDir(null)), "vibration-summary-${mode.name.lowercase()}.png")
            output.outputStream().use {
                compose.onNodeWithTag("vibration_summary").captureToImage().asAndroidBitmap()
                    .compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }
}
