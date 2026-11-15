package com.example.quietnight.domain.model

enum class MorningFeedback { NONE, COMFORTABLE, DISTURBED }
enum class MeasurementQuality { INSUFFICIENT, INTERRUPTED, NOISY, RECORDED }

data class VibrationObservation(val timestamp: Long, val before: Int, val after: Int? = null)

data class NightInsights(
    val observedMs: Long = 0,
    val missingMs: Long = 0,
    val loudMs: Long = 0,
    val ambientMs: Long = 0,
    val ambientLoudMs: Long = 0,
    val vibrations: List<VibrationObservation> = emptyList()
) {
    fun quality(measuredMs: Long): MeasurementQuality = when {
        observedMs < 20_000 -> MeasurementQuality.INSUFFICIENT
        observedMs.toDouble() / maxOf(measuredMs, observedMs + missingMs, 1) < 0.9 ->
            MeasurementQuality.INTERRUPTED
        loudMs.toDouble() / observedMs >= 0.5 -> MeasurementQuality.NOISY
        else -> MeasurementQuality.RECORDED
    }
}

data class VibrationPolicy(val limit: Int, val restMs: Long) {
    companion object {
        val STANDARD = VibrationPolicy(30, 60_000)
        val GENTLE = VibrationPolicy(10, 120_000)
    }
}
