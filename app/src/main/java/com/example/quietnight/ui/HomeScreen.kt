package com.example.quietnight.ui

import android.icu.text.DecimalFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.quietnight.ui.theme.QuietNightTheme
import kotlin.math.abs

@Composable
fun HomeScreen(state: SnoreState, onOpenSettings: () -> Unit, onMonitorClicked: () -> Unit) {
    QuietNightTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background) // 시스템 배경색 대응
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "수면 리포트",
                    color = MaterialTheme.colorScheme.onBackground, // 시스템 텍스트색 대응
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "설정",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2x2 통계 그리드 (어제 대비 증감. 점수는 오를수록, 코골이는 줄수록 긍정)
            val scoreDelta = percentDelta(state.todayScore, state.prevScore, higherIsBetter = true)
            val snoreDelta =
                percentDelta(state.todaySnoreTime, state.prevSnoreTime, higherIsBetter = false)
            Row(modifier = Modifier.fillMaxWidth()) {
                StatCard(
                    "수면 점수",
                    "${state.todayScore}",
                    scoreDelta.text,
                    scoreDelta.color,
                    Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                StatCard(
                    "코골이 시간",
                    "${DecimalFormat("#.#").format(state.todaySnoreTime.toDouble() / 60000)}분",
                    snoreDelta.text,
                    snoreDelta.color,
                    Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 모니터링 상태 인디케이터
            if (state.isMonitoring) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "현재 소음: ${state.currentDb} dB",
                            color = MaterialTheme.colorScheme.primary
                        )
                        state.recentLogs.forEach { log ->
                            Text(log, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    onMonitorClicked()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isMonitoring) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (state.isMonitoring) "모니터링 종료" else "수면 시작하기", color = Color.White)
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, badge: String, diffColor: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Text(
                value,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(badge, color = diffColor, fontSize = 10.sp)
        }
    }
}

// 어제 대비 증감 표시(문구 + 색). 분모가 0이면 비교 불가로 빈 값 반환(∞/NaN 방지).
private data class PercentDelta(val text: String, val color: Color)

private fun percentDelta(current: Number, previous: Number, higherIsBetter: Boolean): PercentDelta {
    val prev = previous.toDouble()
    val cur = current.toDouble()
    if (prev == 0.0) return PercentDelta("", Color.Unspecified)
    val pct = (cur - prev) / prev * 100
    if (pct == 0.0) return PercentDelta("", Color.Unspecified)
    val isUp = pct > 0
    val isGood = if (higherIsBetter) isUp else !isUp
    val arrow = if (isUp) "↑" else "↓"
    val color = if (isGood) Color(0xFF4ADE80) else Color(0xFFEF4444)
    return PercentDelta("$arrow ${DecimalFormat("#.#").format(abs(pct))}%", color)
}