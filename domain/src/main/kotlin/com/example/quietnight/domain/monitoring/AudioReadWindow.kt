package com.example.quietnight.domain.monitoring

data class AudioReadWindow(val observedMs: Long, val missingMs: Long) {
    companion object {
        fun from(sampleCount: Int, sampleRate: Int, elapsedMs: Long): AudioReadWindow {
            require(sampleRate > 0)
            val elapsed = elapsedMs.coerceAtLeast(0)
            val observed = minOf(elapsed, sampleCount.coerceAtLeast(0).toLong() * 1000 / sampleRate)
            return AudioReadWindow(observed, elapsed - observed)
        }
    }
}
