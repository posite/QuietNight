package com.example.quietnight.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

// 소리 수치만 사용하는 공통 그림. 날짜나 녹음 파일을 외부로 전송하지 않는다.
@Composable
internal fun NightPostcardArt(timelineCsv: String, modifier: Modifier = Modifier) {
    val levels = remember(timelineCsv) { postcardLevels(timelineCsv) }
    Canvas(modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(20.dp))
        .background(Brush.verticalGradient(listOf(Color(0xFF10152F), Color(0xFF344365))))
        .semantics {
            contentDescription = if (levels.isEmpty()) "밤 엽서를 그릴 소리 기록이 부족합니다" else
                "${levels.size}개 소리 구간의 밤 엽서. 왼쪽에서 오른쪽으로 기록 순서이며, 별이 높을수록 소리가 컸습니다."
        }) {
        if (levels.isEmpty()) return@Canvas
        val moon = Offset(size.width * .86f, size.height * .18f)
        drawCircle(Color(0xFFFFE8B5), size.height * .065f, moon)
        drawCircle(Color(0xFF171E3C), size.height * .06f,
            moon + Offset(size.height * .03f, -size.height * .015f))
        val path = Path()
        levels.forEachIndexed { index, level ->
            val x = size.width * (.08f + .84f * index / (levels.size - 1).coerceAtLeast(1))
            val y = size.height * (.80f - .48f * level / 100f)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            drawCircle(Color(0xFFF4D9B2).copy(alpha = .13f), 7.dp.toPx(), Offset(x, y))
            drawCircle(Color(0xFFFFE8C6), (1.5f + level / 100f).dp.toPx(), Offset(x, y))
        }
        drawPath(path, Color(0xFFAECFF0).copy(alpha = .55f), style = Stroke(1.dp.toPx()))
        drawLine(Color(0xFF9DBBDF).copy(alpha = .25f), Offset(size.width * .08f, size.height * .89f),
            Offset(size.width * .92f, size.height * .89f), 1.dp.toPx())
    }
}
