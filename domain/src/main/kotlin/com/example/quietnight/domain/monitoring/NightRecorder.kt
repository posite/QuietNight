package com.example.quietnight.domain.monitoring

import com.example.quietnight.domain.model.NightInsights
import com.example.quietnight.domain.model.VibrationPolicy
import com.example.quietnight.domain.model.VibrationObservation

// 서비스가 소유하는 순수 계산기. 마이크·시계·저장 작업은 수행하지 않는다.
class NightRecorder(private val initial: NightInsights = NightInsights()) {
    private var observedMs = initial.observedMs
    private var missingMs = initial.missingMs
    private var loudMs = initial.loudMs
    private var ambientMs = initial.ambientMs
    private var ambientLoudMs = initial.ambientLoudMs
    private var runAudioMs = 0L
    private val events = initial.vibrations.toMutableList()
    private val before = ArrayDeque<Pair<Int, Long>>()
    private var beforeMs = 0L
    private var pendingAt: Long? = null
    private var afterMs = 0L
    private var afterSum = 0L
    private var lastSentAt: Long? = null

    fun sample(level: Int, threshold: Int, audioMs: Long, gapMs: Long, elapsedMs: Long) {
        val duration = audioMs.coerceAtLeast(0)
        val loud = level > threshold
        missingMs += gapMs.coerceAtLeast(0)
        if (gapMs > 200) interrupt()
        observedMs += duration
        if (loud) loudMs += duration
        val calibration = minOf(duration, (20_000 - runAudioMs).coerceAtLeast(0))
        ambientMs += calibration
        if (loud) ambientLoudMs += calibration
        runAudioMs += duration
        if (duration == 0L) return

        pendingAt?.let { started ->
            // 진동 직후 1초는 구동음 때문에 제외하고 다음 5초를 관찰한다.
            val overlap = (minOf(elapsedMs, started + 6000) -
                maxOf(elapsedMs - duration, started + 1000)).coerceAtLeast(0)
            afterMs += overlap
            afterSum += level * overlap
            if (elapsedMs >= started + 6000) {
                // 5초 창의 95% 이상이 관찰됐을 때만 비교한다. 짧은 스케줄링 지터는 허용한다.
                if (afterMs >= 4750L) {
                    events[events.lastIndex] = events.last().copy(after = (afterSum / afterMs).toInt())
                }
                pendingAt = null
            }
        }
        before.addLast(level to duration)
        beforeMs += duration
        while (beforeMs > 5000 && before.isNotEmpty()) {
            val first = before.removeFirst()
            val removed = minOf(first.second, beforeMs - 5000)
            beforeMs -= removed
            if (removed < first.second) before.addFirst(first.first to first.second - removed)
        }
    }

    fun canVibrate(elapsedMs: Long, timestamp: Long, policy: VibrationPolicy): Boolean {
        val wallRest = events.lastOrNull()?.let { timestamp - it.timestamp >= policy.restMs } ?: true
        val monotonicRest = lastSentAt?.let { elapsedMs - it >= policy.restMs } ?: true
        return runAudioMs >= 20_000 && beforeMs == 5000L && pendingAt == null &&
            events.size < policy.limit && wallRest && monotonicRest
    }

    // 실제 진동 요청에 성공했을 때만 서비스가 호출한다. 미지원/비활성 진동은 기록하지 않는다.
    fun vibrationSent(elapsedMs: Long, timestamp: Long) {
        check(beforeMs > 0 && pendingAt == null)
        events.add(VibrationObservation(timestamp, (before.sumOf { it.first * it.second } / beforeMs).toInt()))
        pendingAt = elapsedMs
        lastSentAt = elapsedMs
        afterMs = 0
        afterSum = 0
    }

    fun interrupt() {
        pendingAt = null
        before.clear()
        beforeMs = 0
    }

    fun snapshot() = NightInsights(observedMs, missingMs, loudMs, ambientMs, ambientLoudMs, events.toList())
}
