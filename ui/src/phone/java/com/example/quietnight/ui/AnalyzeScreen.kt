package com.example.quietnight.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.quietnight.domain.model.SnoreClip

@Composable
fun AnalyzeScreen(
    state: SnoreState,
    clipProvider: suspend (Long) -> List<SnoreClip> = { emptyList() },
    onApplyGentle: () -> Unit = {},
    onOpenMonitor: () -> Unit = {}
) {
    var showJournal by rememberSaveable { mutableStateOf(true) }
    var period by rememberSaveable { mutableStateOf(ChartPeriod.DAILY) }
    var selected by remember { mutableStateOf<ChartBar?>(null) }
    val bars = remember(state.sessionHistory, period) {
        buildChartBars(state.sessionHistory, period)
    }
    val hasAnyData = bars.any { it.hasData }

    if (showJournal) {
        NightJournal(state, { showJournal = false }, onApplyGentle, clipProvider,
            onOpenMonitor = onOpenMonitor)
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        TextButton(onClick = { showJournal = true }) { Text("밤 엽서 보기") }
        Text(
            text = "수면 점수 비교",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        PeriodSelector(selected = period, onSelect = { period = it })

        Spacer(modifier = Modifier.height(24.dp))

        if (state.sessionHistory.isEmpty()) {
            EmptyAnalysisCard(state.isMonitoring, onOpenMonitor)
        } else {
            ScoreBarChart(bars = bars, hasAnyData = hasAnyData, onBarClick = { selected = it })
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (hasAnyData) {
            Text(
                text = "막대를 누르면 상세 기록을 볼 수 있어요.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
    }

    selected?.let { bar ->
        if (bar.sessions.size == 1) {
            DayDetailDialog(
                session = state.sessionHistory.firstOrNull { it.date == bar.sessions.first().date } ?: bar.sessions.first(),
                clipProvider = clipProvider,
                isMonitoring = state.isMonitoring,
                suggestGentle = state.suggestGentleVibration,
                onApplyGentle = onApplyGentle,
                message = state.journalMessage,
                onDismiss = { selected = null }
            )
        } else {
            AggregateDetailDialog(
                title = bar.rangeLabel.ifEmpty { bar.label },
                sessions = bar.sessions,
                onDismiss = { selected = null }
            )
        }
    }
}

@Composable
private fun PeriodSelector(selected: ChartPeriod, onSelect: (ChartPeriod) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ChartPeriod.entries.forEach { chartPeriod ->
            val isSelected = chartPeriod == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onSelect(chartPeriod) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = chartPeriod.label,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun ScoreBarChart(
    bars: List<ChartBar>,
    hasAnyData: Boolean,
    onBarClick: (ChartBar) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        if (!hasAnyData) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "아직 기록이 없어요.\n오늘 밤 측정을 시작해 보세요.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
            return@Card
        }
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val max = bars.maxOf { it.score }.coerceAtLeast(1).toFloat()
            bars.forEach { bar ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = if (bar.hasData) {
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onBarClick(bar) }
                    } else Modifier
                ) {
                    Column(
                        modifier = Modifier
                            .width(28.dp)
                            .fillMaxHeight(0.8f),
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = bar.score.toString(),
                            color = if (bar.hasData) scoreColor(bar.score)
                            else MaterialTheme.colorScheme.outline,
                            fontSize = 10.sp,
                            fontWeight = if (bar.hasData) FontWeight.Bold else FontWeight.Normal
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight((bar.score / max).coerceAtLeast(0.02f))
                                .background(
                                    color = if (bar.hasData) scoreColor(bar.score)
                                    else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                )
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = bar.label,
                        color = if (bar.hasData) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.outline,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

// 점수대별 색상: 80↑ 초록 / 50~79 노랑 / 50↓ 빨강
fun scoreColor(score: Int): Color = when {
    score >= 80 -> Color(0xFF4ADE80)
    score >= 50 -> Color(0xFFFBBF24)
    else -> Color(0xFFEF4444)
}
