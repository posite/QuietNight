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
                        VibrationSummaryCard(session)
                        TextButton(onClick = { selectedDate = session.date }) { Text("엽서와 성적표 열기") }
                    }
                }
            }
        }
    }
    state.sessionHistory.firstOrNull { it.date == selectedDate }?.let { session ->
        DayDetailDialog(session, clipProvider,
            isMonitoring = state.isMonitoring, suggestGentle = state.suggestGentleVibration,
            onApplyGentle = onApplyGentle, message = state.journalMessage,
            onDismiss = { selectedDate = null })
    }
}

@Composable
internal fun NightVibrationReport(
    session: SleepSession,
    isMonitoring: Boolean,
    suggestGentle: Boolean,
    onApplyGentle: () -> Unit,
    message: String?
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("진동 성적표", style = MaterialTheme.typography.titleMedium)
        VibrationSummaryCard(session)
        message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        if (suggestGentle) GentleSuggestion(isMonitoring, onApplyGentle)
    }
}

@Composable
private fun GentleSuggestion(monitoring: Boolean, onApply: () -> Unit) {
    Column {
        Text("진동이 방해됐다는 답변이 반복됐어요. 2분 간격·하루 최대 10회인 여유 모드를 써볼까요?")
        Button(onClick = onApply, enabled = !monitoring) { Text("여유 모드 적용") }
    }
}
