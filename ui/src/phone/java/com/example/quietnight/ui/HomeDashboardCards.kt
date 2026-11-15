package com.example.quietnight.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.quietnight.domain.model.SleepSession
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun HomeFirstNightCard() {
    Card(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Rounded.NightsStay, contentDescription = null, modifier = Modifier.size(36.dp))
                Text("첫 밤을 준비해 볼까요?", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold)
            }
            Text("아래의 수면 시작하기를 누르면 소리의 변화와 측정 시간을 기록해요.",
                style = MaterialTheme.typography.bodyMedium)
            Text("기록이 쌓이면 이곳에서 최근 밤의 엽서를 볼 수 있어요.",
                style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
internal fun HomeRecentNightCard(session: SleepSession, onOpenAnalysis: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("최근 밤의 기록", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
                Icon(Icons.Rounded.NightsStay, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary)
            }
            Text(Instant.ofEpochMilli(session.date).atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("M월 d일 · 소리로 남긴 밤", Locale.KOREAN)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (postcardLevels(session.timelineCsv).isNotEmpty()) {
                NightPostcardArt(session.timelineCsv)
            } else {
                Text("1분 단위 소리 기록이 아직 부족해요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HomeNightMetric("측정 시간", homeDuration(session.sleepTime), Modifier.weight(1f))
                HomeNightMetric("코골이 시간", homeDuration(session.snoreTime), Modifier.weight(1f))
            }
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Vibration, contentDescription = null,
                    modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Text("진동 요청 ${session.insights.vibrations.size}회", style = MaterialTheme.typography.bodySmall)
            }
            Text(measurementLabel(session), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = onOpenAnalysis, modifier = Modifier.fillMaxWidth()) {
                Text("밤 엽서 보기")
            }
        }
    }
}

@Composable
internal fun HomeMonitoringCard(state: SnoreState) {
    Card(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                Text("지금 밤을 기록하고 있어요", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
            }
            Text("현재 소음: ${state.currentDb} dB", style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary)
            Text(state.elapsedTime, style = MaterialTheme.typography.titleMedium)
            if (state.recentLogs.isEmpty()) {
                Text("소리 변화를 기다리고 있어요.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                state.recentLogs.forEach { log ->
                    Text(log, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
internal fun HomePreparationCard() {
    Card(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("오늘 밤 준비", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            HomePreparationTip(Icons.Rounded.BatteryChargingFull, "충전기를 연결해 주세요",
                "밤새 측정할 수 있도록 전원을 준비해 주세요.")
            HomePreparationTip(Icons.Rounded.Smartphone, "휴대폰을 침대 가까이에",
                "이불에 덮이지 않는 안정된 곳에 놓아 주세요.")
            Text("소리 기록 안내", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary)
            Text("소리를 기록하며, 실제 수면 여부나 수면 단계를 판단하지 않아요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HomePreparationTip(icon: ImageVector, title: String, description: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(description, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HomeNightMetric(label: String, value: String, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

private fun homeDuration(milliseconds: Long): String {
    val minutes = milliseconds.coerceAtLeast(0) / 60_000
    return if (minutes >= 60) "${minutes / 60}시간 ${minutes % 60}분" else "${minutes}분"
}
