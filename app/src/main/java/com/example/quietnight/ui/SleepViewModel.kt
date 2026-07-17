package com.example.quietnight.ui

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quietnight.data.SettingsRepository
import com.example.quietnight.data.SnoreDao
import com.example.quietnight.service.SnoreService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import java.util.Locale

@HiltViewModel
class SleepViewModel @Inject constructor(
    private val dao: SnoreDao,
    private val settings: SettingsRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(SnoreState())
    val state = _state.asStateFlow()

    init {
        // 서비스 실시간 데시벨
        viewModelScope.launch {
            SnoreService.dbFlow.collect { db -> _state.update { it.copy(currentDb = db) } }
        }
        // 서비스 감지 로그
        viewModelScope.launch {
            SnoreService.logFlow.collect { log ->
                _state.update { it.copy(recentLogs = (listOf(log) + it.recentLogs).take(7)) }
            }
        }
        // 측정 중 여부 (프로세스가 죽었다 살아나도 서비스가 재설정)
        viewModelScope.launch {
            SnoreService.monitoringFlow.collect { running ->
                _state.update { it.copy(isMonitoring = running) }
            }
        }
        // 경과 시간 타이머
        viewModelScope.launch {
            SnoreService.monitoringFlow.collectLatest { running ->
                if (running) {
                    while (true) {
                        val start = SnoreService.startTimeFlow.value
                        val elapsed = if (start > 0) System.currentTimeMillis() - start else 0
                        _state.update { it.copy(elapsedTime = formatElapsed(elapsed)) }
                        delay(1000)
                    }
                } else {
                    _state.update { it.copy(elapsedTime = "00:00:00") }
                }
            }
        }
        // 감지 민감도(임계값) 설정
        viewModelScope.launch {
            settings.settings.collect { s -> _state.update { it.copy(threshold = s.threshold) } }
        }
        // 오늘 세션(실시간) + 어제 세션(증감 비교)
        viewModelScope.launch {
            val yesterday = dao.getSessionByDate(startOfDayMillis(-1))
            dao.getSessionByDateFlow(startOfDayMillis(0)).collect { today ->
                _state.update {
                    it.copy(
                        todayScore = today?.score ?: 0,
                        todaySnoreTime = today?.snoreTime ?: 0L,
                        todaySleepTime = today?.sleepTime ?: 0L,
                        prevScore = yesterday?.score ?: 0,
                        prevSnoreTime = yesterday?.snoreTime ?: 0L
                    )
                }
            }
        }
    }

    private fun handleIntent(intent: SnoreIntent) {
        when (intent) {
            is SnoreIntent.StartMonitoring -> {
                context.startForegroundService(Intent(context, SnoreService::class.java))
                _state.update { it.copy(isMonitoring = true) }
            }

            is SnoreIntent.StopMonitoring -> {
                context.stopService(Intent(context, SnoreService::class.java))
                _state.update { it.copy(isMonitoring = false) }
            }

            is SnoreIntent.LoadHistory -> {
                viewModelScope.launch {
                    dao.getHistory().collect { history ->
                        _state.update { it.copy(weeklyHistory = history) }
                    }
                }
            }
        }
    }

    fun startMonitoring() = handleIntent(SnoreIntent.StartMonitoring)

    fun stopMonitoring() = handleIntent(SnoreIntent.StopMonitoring)

    fun loadSessionHistory() = handleIntent(SnoreIntent.LoadHistory)

    private fun startOfDayMillis(offsetDays: Long): Long =
        Instant.ofEpochMilli(System.currentTimeMillis())
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .plusDays(offsetDays)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    private fun formatElapsed(ms: Long): String {
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
    }
}
