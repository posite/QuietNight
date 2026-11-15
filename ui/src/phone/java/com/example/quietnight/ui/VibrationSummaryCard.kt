package com.example.quietnight.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.quietnight.domain.model.SleepSession

@Composable
internal fun VibrationSummaryCard(session: SleepSession) {
    val counts = vibrationCounts(session)
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth().testTag("vibration_summary"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant.copy(alpha = .45f),
            contentColor = colors.onSurface),
        border = BorderStroke(1.dp, colors.outline.copy(alpha = .18f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(shape = RoundedCornerShape(14.dp), color = colors.primaryContainer) {
                    Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Vibration, contentDescription = null,
                            tint = colors.onPrimaryContainer, modifier = Modifier.size(24.dp))
                    }
                }
                if (counts.total == 0) {
                    Text("진동 알림 기록이 없어요.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text("진동 알림", modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall, color = colors.onSurfaceVariant)
                    Text("${counts.total}회", style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold, color = colors.primary, softWrap = false)
                }
            }
            if (counts.total > 0) {
                HorizontalDivider(color = colors.outline.copy(alpha = .18f))
                Column {
                    Row(Modifier.fillMaxWidth().padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("알림 후 소리 변화", style = MaterialTheme.typography.labelMedium,
                            color = colors.onSurfaceVariant)
                        Text("횟수", style = MaterialTheme.typography.labelMedium,
                            color = colors.onSurfaceVariant)
                    }
                    VibrationCountRow(Icons.Rounded.TrendingDown, "소리가 작아짐", counts.quieter, colors.primary)
                    HorizontalDivider(Modifier.padding(start = 44.dp), color = colors.outline.copy(alpha = .12f))
                    VibrationCountRow(Icons.Rounded.GraphicEq, "비슷하거나 커짐", counts.similarOrLouder, colors.secondary)
                    HorizontalDivider(Modifier.padding(start = 44.dp), color = colors.outline.copy(alpha = .12f))
                    VibrationCountRow(Icons.Rounded.HelpOutline, "비교할 기록 부족", counts.incomplete, colors.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun VibrationCountRow(icon: ImageVector, label: String, count: Int, accent: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(vertical = 10.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(shape = RoundedCornerShape(10.dp), color = accent.copy(alpha = .12f)) {
            Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            }
        }
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text("${count}회", modifier = Modifier.background(MaterialTheme.colorScheme.surface,
            RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, softWrap = false)
    }
}
