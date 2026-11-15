package com.example.quietnight.ui

import com.example.quietnight.domain.model.SleepSession
import com.example.quietnight.domain.model.MeasurementQuality
import com.example.quietnight.domain.model.MorningFeedback

internal fun postcardLevels(timelineCsv: String): List<Int> {
    val samples = timelineCsv.split(',').mapNotNull { it.toIntOrNull()?.takeIf { level -> level in 0..100 } }
    if (samples.isEmpty()) return emptyList()
    return samples.chunked((samples.size + 59) / 60).map { it.max() }
}

internal fun measurementLabel(session: SleepSession): String = when (session.insights.quality(session.sleepTime)) {
    MeasurementQuality.INSUFFICIENT -> "측정 정보 부족"
    MeasurementQuality.INTERRUPTED -> "누락 구간 주의"
    MeasurementQuality.NOISY -> "지속 소음 주의"
    MeasurementQuality.RECORDED -> "오디오 읽기 확인"
}

internal data class VibrationCounts(
    val total: Int,
    val quieter: Int,
    val similarOrLouder: Int,
    val incomplete: Int
)

internal fun vibrationCounts(session: SleepSession): VibrationCounts {
    val events = session.insights.vibrations
    val reduced = events.count { event -> event.after?.let { event.before - it >= 3 } == true }
    val unknown = events.count { it.after == null }
    return VibrationCounts(events.size, reduced, events.size - reduced - unknown, unknown)
}

internal fun vibrationReport(session: SleepSession): String {
    val counts = vibrationCounts(session)
    if (counts.total == 0) return "진동 알림 기록이 없어요."
    return listOf(
        "진동 알림 ${counts.total}회",
        "알림 후 소리가 작아짐 ${counts.quieter}회",
        "알림 후 소리가 비슷하거나 커짐 ${counts.similarOrLouder}회",
        "소리 기록 부족으로 비교하지 못함 ${counts.incomplete}회"
    ).joinToString("\n")
}

internal fun MorningFeedback.label(): String = when (this) {
    MorningFeedback.NONE -> "답변 지우기"
    MorningFeedback.COMFORTABLE -> "괜찮았어요"
    MorningFeedback.DISTURBED -> "진동이 방해됐어요"
}

internal const val OBSERVATION_NOTICE = "소리 크기만 비교합니다. 감소는 진동의 효과나 숙면을 뜻하지 않습니다. 값은 보정되지 않은 PCM 진폭 기준입니다."
