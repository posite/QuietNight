package com.example.quietnight.ui

import android.icu.util.Calendar
import android.media.MediaPlayer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.quietnight.domain.model.SleepSession
import com.example.quietnight.domain.model.SnoreClip
import com.example.quietnight.domain.model.MorningFeedback
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DayDetailDialog(
    session: SleepSession,
    clipProvider: suspend (Long) -> List<SnoreClip> = { emptyList() },
    onFeedback: (Long, MorningFeedback) -> Unit = { _, _ -> },
    feedbackEnabled: Boolean = true,
    suggestGentle: Boolean = false,
    onApplyGentle: () -> Unit = {},
    message: String? = null,
    onDismiss: () -> Unit
) {
    val samples = session.timelineCsv.split(",").mapNotNull { it.toIntOrNull() }
    val clips by produceState(initialValue = emptyList<SnoreClip>(), session.date) {
        value = clipProvider(session.date)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp)
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(20.dp)) {
                Text(
                    text = dateLabel(session.date),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                NightReportContent(session, feedbackEnabled, onFeedback, suggestGentle, onApplyGentle, message)
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

                if (samples.size >= 2) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "총 ${samples.size}분 기록",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "최고 ${samples.max()}dB",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }

                ClipSection(clips = clips)

                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("닫기") }
                }
            }
        }
    }
}

// 해당 날짜에 저장된 코골이 소리 클립 목록 + 재생. 클립이 없으면 아무것도 그리지 않는다.
@Composable
private fun ClipSection(clips: List<SnoreClip>) {
    if (clips.isEmpty()) return

    var playingId by remember { mutableStateOf<Long?>(null) }
    val playerHolder = remember { mutableStateOf<MediaPlayer?>(null) }

    fun stop() {
        playerHolder.value?.let { runCatching { it.release() } }
        playerHolder.value = null
        playingId = null
    }

    fun play(clip: SnoreClip) {
        stop()
        val mp = MediaPlayer()
        try {
            mp.setDataSource(clip.filePath)
            mp.setOnCompletionListener { stop() }
            mp.setOnErrorListener { _, _, _ -> stop(); true }
            mp.setOnPreparedListener { it.start() }
            mp.prepareAsync()
            playerHolder.value = mp
            playingId = clip.id
        } catch (_: Exception) {
            runCatching { mp.release() }
            stop()
        }
    }

    DisposableEffect(Unit) { onDispose { stop() } }

    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "코골이 소리 ${clips.size}개",
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(4.dp))
    LazyColumn(modifier = Modifier.heightIn(max = 176.dp)) {
        items(clips) { clip ->
            ClipRow(
                clip = clip,
                isPlaying = playingId == clip.id,
                onToggle = { if (playingId == clip.id) stop() else play(clip) }
            )
        }
    }
}

@Composable
private fun ClipRow(clip: SnoreClip, isPlaying: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = clipTime(clip.timestamp),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp
            )
            Text(
                text = "${clip.dbLevel}dB · ${DecimalFormat("#.#").format(clip.durationMs / 1000.0)}초",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
        IconButton(onClick = onToggle) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                contentDescription = if (isPlaying) "정지" else "재생",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun clipTime(ts: Long): String =
    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(ts))

// 주간/월간 막대 탭 시: 해당 구간 집계 요약
@Composable
fun AggregateDetailDialog(title: String, sessions: List<SleepSession>, onDismiss: () -> Unit) {
    val nights = sessions.size
    val avgScore = if (nights > 0) sessions.map { it.score }.average().toInt() else 0
    val totalSnoreMin = sessions.sumOf { it.snoreTime } / 60000
    val best = sessions.maxOfOrNull { it.score } ?: 0
    val worst = sessions.minOfOrNull { it.score } ?: 0

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DetailStat("평균 점수", "$avgScore")
                    DetailStat("기록 일수", "${nights}일")
                    DetailStat("총 코골이", "${totalSnoreMin}분")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    DetailStat("최고 점수", "$best")
                    DetailStat("최저 점수", "$worst")
                }

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
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
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
            Text("기록이 부족합니다", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
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
