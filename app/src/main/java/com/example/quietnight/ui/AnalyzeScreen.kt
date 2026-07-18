package com.example.quietnight.ui

import android.icu.util.Calendar
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.example.quietnight.data.SleepSession
import com.example.quietnight.data.SnoreClip
import com.example.quietnight.ui.theme.QuietNightTheme

// 차트 집계 기간
enum class ChartPeriod(val label: String) {
    DAILY("일간"), WEEKLY("주간"), MONTHLY("월간")
}

// 막대 하나에 대응하는 데이터. hasData=false 면 기록이 없는 더미 막대(회색).
// sessions 에는 해당 버킷의 세션이 담긴다(일간=0~1개, 주간/월간=구간 내 전체).
data class ChartBar(
    val label: String,
    val score: Int,
    val hasData: Boolean,
    val sessions: List<SleepSession> = emptyList(),
    val rangeLabel: String = ""
)

@Composable
fun AnalyzeScreen(
    state: SnoreState,
    clipProvider: suspend (Long) -> List<SnoreClip> = { emptyList() }
) {
    var period by rememberSaveable { mutableStateOf(ChartPeriod.DAILY) }
    var selected by remember { mutableStateOf<ChartBar?>(null) }
    val bars = remember(state.sessionHistory, period) {
        buildChartBars(state.sessionHistory, period)
    }
    val hasAnyData = bars.any { it.hasData }

    QuietNightTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            Text(
                text = "수면 점수 비교",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            PeriodSelector(selected = period, onSelect = { period = it })

            Spacer(modifier = Modifier.height(24.dp))

            ScoreBarChart(bars = bars, hasAnyData = hasAnyData, onBarClick = { selected = it })

            Spacer(modifier = Modifier.height(12.dp))

            if (hasAnyData) {
                Text(
                    text = "막대를 누르면 상세 기록을 볼 수 있어요.",
                    color = MaterialTheme.colorScheme.outline,
                    fontSize = 12.sp
                )
            }
        }
    }

    selected?.let { bar ->
        if (bar.sessions.size == 1) {
            DayDetailDialog(
                session = bar.sessions.first(),
                clipProvider = clipProvider,
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
                    color = MaterialTheme.colorScheme.outline,
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

// ── 집계 로직 ────────────────────────────────────────────────

fun buildChartBars(history: List<SleepSession>, period: ChartPeriod): List<ChartBar> =
    when (period) {
        ChartPeriod.DAILY -> buildDailyBars(history)
        ChartPeriod.WEEKLY -> buildWeeklyBars(history)
        ChartPeriod.MONTHLY -> buildMonthlyBars(history)
    }

// 최근 7일, 하루당 점수 (막대 = 실제 세션이므로 상세 보기 가능)
private fun buildDailyBars(history: List<SleepSession>): List<ChartBar> {
    val bars = mutableListOf<ChartBar>()
    for (i in 6 downTo 0) {
        val cal = midnightToday().apply { add(Calendar.DAY_OF_YEAR, -i) }
        val session = history.find { isSameDay(it.date, cal.timeInMillis) }
        val label = weekdayLabel(cal.get(Calendar.DAY_OF_WEEK))
        bars.add(ChartBar(label, session?.score ?: 0, session != null, listOfNotNull(session)))
    }
    return bars
}

// 최근 7주, 주별 평균 점수
private fun buildWeeklyBars(history: List<SleepSession>): List<ChartBar> {
    val bars = mutableListOf<ChartBar>()
    val weekStart = midnightToday().apply { set(Calendar.DAY_OF_WEEK, firstDayOfWeek) }
    for (i in 6 downTo 0) {
        val start = (weekStart.clone() as Calendar).apply { add(Calendar.WEEK_OF_YEAR, -i) }
        val end = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 7) }
        val inRange = history.filter { it.date >= start.timeInMillis && it.date < end.timeInMillis }
        val label = "${start.get(Calendar.MONTH) + 1}/${start.get(Calendar.DAY_OF_MONTH)}"
        val last = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 6) }
        val rangeLabel = "$label ~ ${last.get(Calendar.MONTH) + 1}/${last.get(Calendar.DAY_OF_MONTH)} 주간"
        bars.add(toAverageBar(label, inRange, rangeLabel))
    }
    return bars
}

// 최근 7개월, 월별 평균 점수
private fun buildMonthlyBars(history: List<SleepSession>): List<ChartBar> {
    val bars = mutableListOf<ChartBar>()
    val monthStart = midnightToday().apply { set(Calendar.DAY_OF_MONTH, 1) }
    for (i in 6 downTo 0) {
        val start = (monthStart.clone() as Calendar).apply { add(Calendar.MONTH, -i) }
        val end = (start.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
        val inRange = history.filter { it.date >= start.timeInMillis && it.date < end.timeInMillis }
        val label = "${start.get(Calendar.MONTH) + 1}월"
        val rangeLabel = "${start.get(Calendar.YEAR)}년 $label"
        bars.add(toAverageBar(label, inRange, rangeLabel))
    }
    return bars
}

private fun toAverageBar(label: String, sessions: List<SleepSession>, rangeLabel: String): ChartBar =
    if (sessions.isEmpty()) ChartBar(label, 0, false)
    else ChartBar(label, sessions.map { it.score }.average().toInt(), true, sessions, rangeLabel)

private fun midnightToday(): Calendar = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}

private fun isSameDay(a: Long, b: Long): Boolean {
    val ca = Calendar.getInstance().apply { timeInMillis = a }
    val cb = Calendar.getInstance().apply { timeInMillis = b }
    return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) &&
            ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
}

private fun weekdayLabel(dayOfWeek: Int): String = when (dayOfWeek) {
    1 -> "일"; 2 -> "월"; 3 -> "화"; 4 -> "수"; 5 -> "목"; 6 -> "금"; else -> "토"
}
