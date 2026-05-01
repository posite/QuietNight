package com.example.quietnight.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.quietnight.ui.theme.QuietNightTheme

@Composable
fun HomeScreen(state: SnoreState, onMonitorClicked: () -> Unit) {
    QuietNightTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background) // 시스템 배경색 대응
                .padding(16.dp)
        ) {
            Text(
                text = "수면 리포트",
                color = MaterialTheme.colorScheme.onBackground, // 시스템 텍스트색 대응
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 2x2 통계 그리드
            Row(modifier = Modifier.fillMaxWidth()) {
                StatCard("수면 점수", "${state.todayScore}", "↑ 12점", Modifier.weight(1f))
                Spacer(modifier = Modifier.width(8.dp))
                StatCard("코골이 시간", "${state.todaySnoreMin}분", "↓ 31%", Modifier.weight(1f))
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
                            Text(log, color = Color.Gray, fontSize = 10.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {

                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isMonitoring) Color.Gray else MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (state.isMonitoring) "모니터링 종료" else "수면 시작하기", color = Color.White)
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, badge: String, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, color = Color.Gray, fontSize = 12.sp)
            Text(
                value,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(badge, color = Color(0xFF4ADE80), fontSize = 10.sp)
        }
    }
}