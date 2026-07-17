package com.example.quietnight.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.quietnight.data.AppSettings
import com.example.quietnight.ui.theme.QuietNightTheme

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onThresholdChange: (Int) -> Unit,
    onVibrationToggle: (Boolean) -> Unit,
    onVibrationStrengthChange: (Int) -> Unit,
    onBack: () -> Unit
) {
    QuietNightTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "뒤로",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.height(0.dp))
                Text(
                    text = "설정",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 감지 민감도(임계값)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "감지 민감도",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${settings.threshold} dB",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        "이 데시벨을 넘으면 코골이로 감지합니다. 낮출수록 민감해집니다.",
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = 12.sp
                    )
                    Slider(
                        value = settings.threshold.toFloat(),
                        onValueChange = { onThresholdChange(it.toInt()) },
                        valueRange = 50f..90f,
                        steps = 39
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 진동 교정 on/off + 세기
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp)
                        ) {
                            Text(
                                "진동 교정",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "코골이 감지 시 진동으로 자세 교정을 유도합니다.",
                                color = MaterialTheme.colorScheme.outline,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = settings.vibrationEnabled,
                            onCheckedChange = onVibrationToggle
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val strengthTint =
                        if (settings.vibrationEnabled) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.outline
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("진동 세기", color = strengthTint, fontSize = 14.sp)
                        Text(
                            "${settings.vibrationStrength * 100 / 255}%",
                            color = if (settings.vibrationEnabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = settings.vibrationStrength.toFloat(),
                        onValueChange = { onVibrationStrengthChange(it.toInt()) },
                        valueRange = 1f..255f,
                        enabled = settings.vibrationEnabled
                    )
                }
            }
        }
    }
}
