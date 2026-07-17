package com.example.quietnight.ui

import android.icu.util.Calendar
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.quietnight.data.SleepSession
import java.text.DecimalFormat

@Composable
fun DayDetailDialog(session: SleepSession, onDismiss: () -> Unit) {
    val samples = session.timelineCsv.split(",").mapNotNull { it.toIntOrNull() }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = dateLabel(session.date),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DetailStat("수면 점수", "${session.score}")
                    DetailStat("코골이", "${formatMinutes(session.snoreTime)}분")
                    DetailStat("수면 시간", formatDuration(session.sleepTime))
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "밤새 소음 추이",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                TimelineChart(samples = samples)

                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("닫기") }
                }
            }
        }
    }
}

@Composable
private fun DetailStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = MaterialTheme.colorScheme.primary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, color = MaterialTheme.colorScheme.outline, fontSize = 11.sp)
    }
}

@Composable
private fun TimelineChart(samples: List<Int>) {
    val lineColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    if (samples.size < 2) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(trackColor, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("기록이 부족합니다", color = MaterialTheme.colorScheme.outline, fontSize = 12.sp)
        }
        return
    }

    val maxV = samples.max().coerceAtLeast(1).toFloat()
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(trackColor, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        val stepX = size.width / (samples.size - 1)
        val path = Path()
        samples.forEachIndexed { i, v ->
            val x = stepX * i
            val y = size.height * (1f - v / maxV)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = lineColor, style = Stroke(width = 3f))
        // 시작/끝 지점 강조
        val firstY = size.height * (1f - samples.first() / maxV)
        val lastY = size.height * (1f - samples.last() / maxV)
        drawCircle(lineColor, radius = 4f, center = Offset(0f, firstY))
        drawCircle(lineColor, radius = 4f, center = Offset(size.width, lastY))
    }
}

private fun formatMinutes(ms: Long): String = DecimalFormat("#.#").format(ms.toDouble() / 60000)

private fun formatDuration(ms: Long): String {
    val totalMin = ms / 60000
    val h = totalMin / 60
    val m = totalMin % 60
    return if (h > 0) "${h}시간 ${m}분" else "${m}분"
}

private fun dateLabel(dateMillis: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
    val month = cal.get(Calendar.MONTH) + 1
    val day = cal.get(Calendar.DAY_OF_MONTH)
    val dow = when (cal.get(Calendar.DAY_OF_WEEK)) {
        1 -> "일"; 2 -> "월"; 3 -> "화"; 4 -> "수"; 5 -> "목"; 6 -> "금"; else -> "토"
    }
    return "${month}월 ${day}일 (${dow})"
}
