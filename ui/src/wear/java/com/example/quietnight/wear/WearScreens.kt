package com.example.quietnight.wear

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnScope
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.example.quietnight.domain.model.AppSettings
import com.example.quietnight.domain.model.SleepSession
import com.example.quietnight.domain.model.MorningFeedback
import com.example.quietnight.ui.SnoreState
import com.example.quietnight.ui.NightPostcardArt
import com.example.quietnight.ui.measurementLabel
import com.example.quietnight.ui.vibrationReport
import com.example.quietnight.ui.label
import com.example.quietnight.ui.OBSERVATION_NOTICE
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun WearMonitorScreen(
    state: SnoreState,
    onToggle: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    message: String? = null
) {
    WatchList {
        item { Heading("QuietNight") }
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (state.isMonitoring) "측정 중" else "편안한 밤을 준비해요")
                Text(if (state.isMonitoring) "${state.currentDb} dB" else "워치로 코골이 측정",
                    style = MaterialTheme.typography.titleMedium)
                if (state.isMonitoring) Text(state.elapsedTime)
            }
        }
        item { WatchButton(if (state.isMonitoring) "측정 종료" else "측정 시작", onToggle) }
        item { Paragraph("처음 20초는 환경 확인 · 진동 없음. 소리값은 PCM 진폭 기준입니다.") }
        if (message != null) item { Paragraph(message) }
        item { Paragraph("오늘 점수 ${state.todayScore}점 · 코골이 ${state.todaySnoreTime / 60000}분") }
        item { WatchButton("수면 기록", onHistory) }
        item { WatchButton("설정", onSettings) }
        item { Paragraph("기록은 이 워치에 저장됩니다. 밤새 측정하기 전에 배터리를 확인해 주세요.") }
    }
}

@Composable
fun WearHistoryScreen(history: List<SleepSession>, onBack: () -> Unit, onNight: (Long) -> Unit = {}) {
    WatchList {
        item { Heading("수면 기록") }
        if (history.isEmpty()) item { Paragraph("아직 기록이 없어요.\n측정을 시작해 보세요.") }
        // 원형 화면에서도 날짜와 시간 단위를 나누어 읽을 수 있게 표시한다.
        items(history.size) { index ->
            val session = history[index]
            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text(Instant.ofEpochMilli(session.date).atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("M월 d일")))
                Text("${session.score}점", style = MaterialTheme.typography.titleMedium)
                Text("측정 ${session.sleepTime / 60000}분")
                Text("코골이 ${session.snoreTime / 60000}분")
            }
            WatchButton("밤 엽서 · 성적표") { onNight(session.date) }
        }
        item { WatchButton("뒤로", onBack) }
    }
}

@Composable
fun WearSettingsScreen(
    settings: AppSettings,
    onThreshold: (Int) -> Unit,
    onVibration: (Boolean) -> Unit,
    onBack: () -> Unit,
    onGentleVibration: (Boolean) -> Unit = {}
) {
    WatchList {
        item { Heading("설정") }
        item { Paragraph("감지 기준 ${settings.threshold} dB") }
        listOf("민감" to 60, "보통" to 75, "둔감" to 85).forEach { (label, threshold) ->
            item {
                WatchButton("$label ${threshold} dB" + if (settings.threshold == threshold) " · 선택됨" else "") {
                    onThreshold(threshold)
                }
            }
        }
        item {
            WatchButton(if (settings.vibrationEnabled) "진동 켜짐" else "진동 꺼짐") {
                onVibration(!settings.vibrationEnabled)
            }
        }
        item { Paragraph("코골이가 감지되면 워치가 진동합니다. 워치에서는 소리 파일을 녹음하지 않습니다.") }
        item {
            WatchButton(if (settings.gentleVibration) "여유 모드 · 선택됨" else "기본 진동 모드") {
                onGentleVibration(!settings.gentleVibration)
            }
        }
        item { Paragraph(if (settings.gentleVibration) "2분 간격 · 하루 최대 10회" else "1분 간격 · 하루 최대 30회") }
        item { WatchButton("뒤로", onBack) }
    }
}

@Composable
fun WearNightScreen(
    session: SleepSession?,
    state: SnoreState,
    onFeedback: (Long, MorningFeedback) -> Unit,
    onApplyGentle: () -> Unit,
    onBack: () -> Unit
) {
    WatchList {
        item { Heading("밤 엽서") }
        if (session == null) item { Paragraph("기록을 찾을 수 없어요.") }
        else {
            item { Paragraph(Instant.ofEpochMilli(session.date).atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("M월 d일"))) }
            item { NightPostcardArt(session.timelineCsv, Modifier.fillMaxWidth().height(120.dp)) }
            item { Paragraph("별이 높을수록 소리가 컸어요. 수면 단계 그림은 아니에요.") }
            item { Heading(measurementLabel(session)) }
            item { Paragraph("오디오 ${session.insights.observedMs / 1000}초\n누락 ${session.insights.missingMs / 1000}초") }
            item { Heading("진동 성적표") }
            item { Paragraph(vibrationReport(session)) }
            item { Paragraph(OBSERVATION_NOTICE) }
            items(session.insights.vibrations.size) { index ->
                val event = session.insights.vibrations[index]
                Paragraph("${index + 1}회 · 이전 ${event.before}\n이후 ${event.after?.toString() ?: "비교 불가"}")
            }
            item { Heading("아침에는 어땠나요?") }
            if (state.isMonitoring) item { Paragraph("측정을 종료한 뒤 답변할 수 있어요.") }
            else if (session.insights.vibrations.isEmpty()) item { Paragraph("진동 요청 기록이 없어 피드백을 받지 않아요.") }
            else {
                listOf(MorningFeedback.COMFORTABLE, MorningFeedback.DISTURBED, MorningFeedback.NONE).forEach { value ->
                    item {
                        WatchButton(value.label() + if (value != MorningFeedback.NONE && session.feedback == value) " · 선택됨" else "") {
                            onFeedback(session.date, value)
                        }
                    }
                }
            }
            state.journalMessage?.let { item { Paragraph(it) } }
            if (state.suggestGentleVibration && !state.isMonitoring) {
                item { Paragraph("방해됐다는 답변이 반복됐어요. 2분 간격·하루 최대 10회로 줄여볼까요?") }
                item { WatchButton("여유 모드 적용", onApplyGentle) }
            }
        }
        item { WatchButton("뒤로", onBack) }
    }
}

@Composable
private fun WatchList(content: TransformingLazyColumnScope.() -> Unit) {
    val scrollState = rememberTransformingLazyColumnState()
    ScreenScaffold(scrollState = scrollState) { contentPadding ->
        TransformingLazyColumn(
            state = scrollState,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp).testTag("watch-list"),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

@Composable
private fun TransformingLazyColumnItemScope.WatchButton(label: String, onClick: () -> Unit) {
    val transformation = rememberTransformationSpec()
    Button(onClick = onClick,
        modifier = Modifier.fillMaxWidth().transformedHeight(this, transformation),
        transformation = SurfaceTransformation(transformation)) {
        Text(label, textAlign = TextAlign.Center)
    }
}

@Composable
private fun Heading(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp))
}

@Composable
private fun Paragraph(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp))
}
