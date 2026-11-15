package com.example.quietnight.domain.monitoring

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

// 구현체가 측정 서비스를 제어한다. UI는 서비스 상태를 읽을 뿐 누적·저장하지 않는다.
interface MonitoringController {
    val decibels: StateFlow<Int>
    val logs: Flow<String>
    val isMonitoring: StateFlow<Boolean>
    val startedAt: StateFlow<Long>
    fun start()
    fun stop()
}
