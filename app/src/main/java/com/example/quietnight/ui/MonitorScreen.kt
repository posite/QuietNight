package com.example.quietnight.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.quietnight.ui.theme.QuietNightTheme

@Composable
fun MonitorScreen(state: SnoreState, onStopClicked: () -> Unit) {

    QuietNightTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "실시간 모니터링",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(40.dp))

            // 실시간 데시벨 인디케이터
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
                CircularProgressIndicator(
                    progress = { state.currentDb / 100f },
                    modifier = Modifier.fillMaxSize(),
                    color = if (state.currentDb > 75) Color.Red else MaterialTheme.colorScheme.primary,
                    strokeWidth = 12.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = ProgressIndicatorDefaults.CircularDeterminateStrokeCap,
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${state.currentDb}",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text("dB", color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // 실시간 로그 리스트
            Text(
                "실시간 감지 로그",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(state.recentLogs) { log ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = log,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            Button(
                onClick = { if (state.isMonitoring) onStopClicked() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (state.isMonitoring) Color.Red else Color.Gray),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("측정 종료 및 저장", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}