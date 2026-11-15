package com.example.quietnight.data

// Room 모델의 이름과 스키마는 유지하고 모듈 경계에서만 도메인 모델로 변환한다.
internal fun SleepSession.toDomain() = com.example.quietnight.domain.model.SleepSession(
    id = id, date = date, score = score, snoreTime = snoreTime,
    sleepTime = sleepTime, timelineCsv = timelineCsv,
    insights = toInsights(),
    feedback = com.example.quietnight.domain.model.MorningFeedback.entries
        .firstOrNull { it.name == morningFeedback } ?: com.example.quietnight.domain.model.MorningFeedback.NONE
)

internal fun SleepSession.toInsights() = com.example.quietnight.domain.model.NightInsights(
    observedMs, missingMs, loudMs, ambientMs, ambientLoudMs,
    VibrationObservationCodec.decode(vibrationEventsCsv)
)

internal fun SnoreClip.toDomain() = com.example.quietnight.domain.model.SnoreClip(
    id = id, date = date, timestamp = timestamp, dbLevel = dbLevel,
    filePath = filePath, durationMs = durationMs
)
