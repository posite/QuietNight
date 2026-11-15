package com.example.quietnight.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.quietnight.domain.model.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun NightJournal(
    state: SnoreState,
    onShowCharts: () -> Unit,
    onFeedback: (Long, MorningFeedback) -> Unit,
    onApplyGentle: () -> Unit,
    clipProvider: suspend (Long) -> List<SnoreClip> = { emptyList() },
    onOpenMonitor: () -> Unit = {}
) {
    var selectedDate by rememberSaveable { mutableStateOf<Long?>(null) }
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Text("밤 엽서", style = MaterialTheme.typography.headlineMedium)
                Text("소리로 남긴 나의 밤", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onShowCharts) { Text("점수 비교 보기") }
            }
            if (state.sessionHistory.isEmpty()) item {
                EmptyAnalysisCard(state.isMonitoring, onOpenMonitor)
            }
            state.journalMessage?.let { message -> item { Text(message) } }
            if (state.suggestGentleVibration) item {
                GentleSuggestion(state.isMonitoring, onApplyGentle)
            }
            items(state.sessionHistory, key = { it.date }) { session ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(Instant.ofEpochMilli(session.date).atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("M월 d일 · 나의 밤")),
                            style = MaterialTheme.typography.titleMedium)
                        NightPostcardArt(session.timelineCsv)
                        if (postcardLevels(session.timelineCsv).isEmpty()) Text("1분 단위 소리 기록이 아직 부족해요.")
                        Text(measurementLabel(session))
                        Text(vibrationReport(session), style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { selectedDate = session.date }) { Text("엽서와 성적표 열기") }
                    }
                }
            }
        }
    }
    state.sessionHistory.firstOrNull { it.date == selectedDate }?.let { session ->
        DayDetailDialog(session, clipProvider, onFeedback = onFeedback,
            feedbackEnabled = !state.isMonitoring, suggestGentle = state.suggestGentleVibration,
            onApplyGentle = onApplyGentle, message = state.journalMessage,
            onDismiss = { selectedDate = null })
    }
}

@Composable
internal fun NightReportContent(
    session: SleepSession,
    feedbackEnabled: Boolean,
    onFeedback: (Long, MorningFeedback) -> Unit,
    suggestGentle: Boolean,
    onApplyGentle: () -> Unit,
    message: String?
) {
    val insights = session.insights
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        NightPostcardArt(session.timelineCsv)
        Text("별이 높을수록 소리가 컸어요. 수면 단계나 숙면을 나타내지는 않아요.",
            style = MaterialTheme.typography.bodySmall)
        Text("측정 확인 · ${measurementLabel(session)}", style = MaterialTheme.typography.titleSmall)
        Text("오디오 ${insights.observedMs / 1000}초 · 확인된 누락 ${insights.missingMs / 1000}초",
            style = MaterialTheme.typography.bodySmall)
        if (insights.ambientMs > 0) Text(
            "시작 환경 확인: ${insights.ambientMs / 1000}초 중 ${insights.ambientLoudMs / 1000}초가 감지 기준 초과",
            style = MaterialTheme.typography.bodySmall)
        Text("진동 성적표", style = MaterialTheme.typography.titleMedium)
        Text(vibrationReport(session))
        Text(OBSERVATION_NOTICE, style = MaterialTheme.typography.bodySmall)
        insights.vibrations.forEachIndexed { index, event ->
            Text("${index + 1}회 · 이전 평균 ${event.before} → 이후 ${event.after?.let { "평균 $it" } ?: "비교 불가"}",
                style = MaterialTheme.typography.bodySmall)
        }
        Text("아침에는 어땠나요?", style = MaterialTheme.typography.titleSmall)
        if (!feedbackEnabled) Text("측정을 종료한 뒤 답변할 수 있어요.")
        if (insights.vibrations.isEmpty()) Text("진동 요청 기록이 없어 진동 피드백을 받지 않아요.")
        listOf(MorningFeedback.COMFORTABLE, MorningFeedback.DISTURBED, MorningFeedback.NONE).forEach { value ->
            OutlinedButton(onClick = { onFeedback(session.date, value) },
                enabled = feedbackEnabled && insights.vibrations.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                Text(value.label() + if (value != MorningFeedback.NONE && session.feedback == value) " · 선택됨" else "")
            }
        }
        message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        if (suggestGentle) GentleSuggestion(!feedbackEnabled, onApplyGentle)
    }
}

@Composable
private fun GentleSuggestion(monitoring: Boolean, onApply: () -> Unit) {
    Column {
        Text("진동이 방해됐다는 답변이 반복됐어요. 2분 간격·하루 최대 10회인 여유 모드를 써볼까요?")
        Button(onClick = onApply, enabled = !monitoring) { Text("여유 모드 적용") }
    }
}
