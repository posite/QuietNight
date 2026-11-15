package com.example.quietnight.data

import android.content.Context
import android.content.Intent
import com.example.quietnight.domain.monitoring.MonitoringController
import com.example.quietnight.service.SnoreService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidMonitoringController @Inject constructor(
    @param:ApplicationContext private val context: Context
) : MonitoringController {
    override val decibels = SnoreService.dbFlow.asStateFlow()
    override val logs = SnoreService.logFlow.asSharedFlow()
    override val isMonitoring = SnoreService.monitoringFlow.asStateFlow()
    override val startedAt = SnoreService.startTimeFlow.asStateFlow()

    override fun start() {
        context.startForegroundService(Intent(context, SnoreService::class.java))
    }

    override fun stop() {
        context.stopService(Intent(context, SnoreService::class.java))
    }
}
