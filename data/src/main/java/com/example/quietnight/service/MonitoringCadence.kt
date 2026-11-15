package com.example.quietnight.service

internal class MonitoringCadence(
    startedAtMs: Long,
    private val flushIntervalMs: Long = 30_000L,
    private val notificationIntervalMs: Long = 60_000L
) {
    private var lastFlushedAtMs = startedAtMs
    private var lastNotifiedAtMs = startedAtMs

    fun shouldFlush(nowMs: Long): Boolean = nowMs - lastFlushedAtMs >= flushIntervalMs

    fun shouldNotify(nowMs: Long): Boolean = nowMs - lastNotifiedAtMs >= notificationIntervalMs

    fun markFlushed(nowMs: Long) {
        lastFlushedAtMs = nowMs
    }

    fun markNotified(nowMs: Long) {
        lastNotifiedAtMs = nowMs
    }
}
