package com.example.quietnight.domain.monitoring

// 오디오 입출력과 분리한 지속 시간 판정. 누적·진동·저장은 서비스가 담당한다.
class SnoreDetector {
    data class Result(val snoreMs: Long = 0, val confirmed: Boolean = false, val justConfirmed: Boolean = false)

    private var pendingMs = 0L
    private var confirmed = false

    fun update(db: Int, threshold: Int, durationMs: Long): Result {
        val delta = durationMs.coerceAtLeast(0)
        if (db <= threshold) {
            reset()
            return Result()
        }
        if (confirmed) return Result(snoreMs = delta, confirmed = true)
        // 첫 청크도 이미 읽은 오디오이므로 지속 시간에 포함한다.
        pendingMs += delta
        if (pendingMs < MIN_SNORE_MS) return Result()
        confirmed = true
        return Result(pendingMs, confirmed = true, justConfirmed = true).also { pendingMs = 0 }
    }

    fun reset() {
        pendingMs = 0
        confirmed = false
    }

    private companion object {
        const val MIN_SNORE_MS = 400L
    }
}
