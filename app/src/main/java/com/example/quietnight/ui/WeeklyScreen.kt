package com.example.quietnight.ui

import android.icu.util.Calendar
import android.util.Log
import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.quietnight.data.SleepSession
import com.example.quietnight.ui.theme.QuietNightTheme

@Composable
fun WeeklyScreen(state: SnoreState) {
    Log.d("sessions", state.weeklyHistory.toString())
    val history = fillMissingDays(state.weeklyHistory)
    QuietNightTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            Text(
                text = "주간 개선 통계",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 주간 바 차트 섹션
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val max = history.maxOf { session -> session.score }.toFloat()
                    history.takeLast(7).forEach { session ->
                        val cal = Calendar.getInstance().apply { timeInMillis = session.date }
                        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1(일)~7(토)
                        val dayLabel = when (dayOfWeek) {
                            1 -> "일"; 2 -> "월"; 3 -> "화"; 4 -> "수"; 5 -> "목"; 6 -> "금"; else -> "토"
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Column(
                                modifier = Modifier
                                    .width(28.dp)
                                    .fillMaxHeight((0.8f)),
                                verticalArrangement = Arrangement.Bottom,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = session.score.toString(),
                                    color = if (session.score > 0) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.outline,
                                    fontSize = 10.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight((session.score / max).coerceAtLeast(0.02f))
                                        .background(
                                            color = if (session.score > 0) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outlineVariant,
                                            shape = RoundedCornerShape(
                                                topStart = 6.dp,
                                                topEnd = 6.dp
                                            )
                                        )
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = dayLabel,
                                color = if (session.score > 0) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.outline,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

fun fillMissingDays(history: List<SleepSession>): List<SleepSession> {
    val result = mutableListOf<SleepSession>()

    // 오늘부터 6일 전까지 반복 (총 7일)
    for (i in 6 downTo 0) {
        val targetCalendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -i)
            // 시간/분/초를 0으로 맞추어 날짜 비교를 정확하게 함
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // 해당 날짜에 기록이 있는지 확인
        val sessionOfDay = history.find { session ->
            val sessionCal = Calendar.getInstance().apply { timeInMillis = session.date }
            sessionCal.get(Calendar.YEAR) == targetCalendar.get(Calendar.YEAR) &&
                    sessionCal.get(Calendar.DAY_OF_YEAR) == targetCalendar.get(Calendar.DAY_OF_YEAR)
        }

        // 기록이 없으면 점수가 0인 더미 세션을 넣음
        result.add(
            sessionOfDay ?: SleepSession(
                date = targetCalendar.timeInMillis,
                score = 0,
                sleepTime = 0,
                snoreTime = 0,
                positionStatsJson = ""
            )
        )
    }
    return result
}