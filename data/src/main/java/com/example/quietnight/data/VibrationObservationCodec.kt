package com.example.quietnight.data

import com.example.quietnight.domain.model.VibrationObservation

// 버전이 있는 숫자 전용 포맷. 원음이나 사용자 입력은 포함하지 않는다.
internal object VibrationObservationCodec {
    fun encode(events: List<VibrationObservation>): String =
        "v1;" + events.joinToString(";") { "${it.timestamp}:${it.before}:${it.after ?: "?"}" }

    fun decode(value: String): List<VibrationObservation> {
        if (!value.startsWith("v1;")) return emptyList()
        return value.removePrefix("v1;").split(';').mapNotNull { row ->
            val fields = row.split(':')
            if (fields.size != 3) return@mapNotNull null
            val timestamp = fields[0].toLongOrNull()?.takeIf { it >= 0 } ?: return@mapNotNull null
            val before = fields[1].toIntOrNull()?.takeIf { it in 0..100 } ?: return@mapNotNull null
            val after = if (fields[2] == "?") null else
                fields[2].toIntOrNull()?.takeIf { it in 0..100 } ?: return@mapNotNull null
            VibrationObservation(timestamp, before, after)
        }
    }
}
