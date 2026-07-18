package com.example.quietnight.ui

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quietnight.data.SettingsRepository
import com.example.quietnight.data.SnoreClip
import com.example.quietnight.data.SnoreDao
import com.example.quietnight.service.SnoreService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import java.io.File
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

    // 히스토리 수집은 1회만. 화면 회전으로 loadSessionHistory()가 다시 호출돼도 콜렉터가 중복되지 않는다.
    private var historyJob: Job? = null

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

            is SnoreIntent.ClearHistory -> {
                viewModelScope.launch {
                    dao.clearAllClips()
                    dao.clearAll()
                    // 저장된 코골이 클립 파일도 함께 삭제
                    File(context.filesDir, "snore_clips").deleteRecursively()
                }
            }

            is SnoreIntent.LoadHistory -> {
                if (historyJob?.isActive == true) return
                // 전체 히스토리 1개 스트림으로 분석 목록 + 오늘/어제 통계를 함께 파생한다.
                // '오늘'을 매 방출 시점에 재계산하므로, 측정 중 자정을 넘겨도 홈 통계가 새 날 행으로 따라간다.
                historyJob = viewModelScope.launch {
                    dao.getHistory().collect { history ->
                        val today = startOfDayMillis(0)
                        val yesterday = startOfDayMillis(-1)
                        val todaySession = history.find { it.date == today }
                        val prevSession = history.find { it.date == yesterday }
                        _state.update {
                            it.copy(
                                sessionHistory = history,
                                todayScore = todaySession?.score ?: 0,
                                todaySnoreTime = todaySession?.snoreTime ?: 0L,
                                todaySleepTime = todaySession?.sleepTime ?: 0L,
                                prevScore = prevSession?.score ?: 0,
                                prevSnoreTime = prevSession?.snoreTime ?: 0L
                            )
                        }
                    }
                }
            }
        }
    }

    fun startMonitoring() = handleIntent(SnoreIntent.StartMonitoring)

    fun stopMonitoring() = handleIntent(SnoreIntent.StopMonitoring)

    fun loadSessionHistory() = handleIntent(SnoreIntent.LoadHistory)

    fun clearAllSessions() = handleIntent(SnoreIntent.ClearHistory)

    // 상세 다이얼로그에서 해당 날짜의 코골이 클립을 로드(컴포저블에서 코루틴으로 호출)
    suspend fun clipsForDate(date: Long): List<SnoreClip> = dao.getClipsByDate(date)

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
