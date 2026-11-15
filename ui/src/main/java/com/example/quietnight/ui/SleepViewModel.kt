package com.example.quietnight.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quietnight.domain.repository.SettingsRepository
import com.example.quietnight.domain.model.SnoreClip
import com.example.quietnight.domain.repository.SleepRepository
import com.example.quietnight.domain.monitoring.MonitoringController
import com.example.quietnight.domain.usecase.ObserveSleepHistoryUseCase
import com.example.quietnight.domain.usecase.recommendGentleVibration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject
import java.util.Locale

@OptIn(FlowPreview::class)
@HiltViewModel
class SleepViewModel @Inject constructor(
    private val repository: SleepRepository,
    private val settings: SettingsRepository,
    private val monitoring: MonitoringController,
    private val observeSleepHistory: ObserveSleepHistoryUseCase,
    private val clock: Clock
) : ViewModel() {

    private val _state = MutableStateFlow(SnoreState())
    val state = _state.asStateFlow()

    // 히스토리 수집은 1회만. 화면 회전으로 loadSessionHistory()가 다시 호출돼도 콜렉터가 중복되지 않는다.
    private var historyJob: Job? = null

    private val uiObserved = _state.subscriptionCount
        .map { count -> count > 0 }
        .distinctUntilChanged()

    init {
        // 서비스 실시간 데시벨
        viewModelScope.launch {
            uiObserved.collectLatest { observed ->
                if (observed) {
                    monitoring.decibels.sample(250L).collect { db ->
                        _state.update { it.copy(currentDb = db) }
                    }
                }
            }
        }
        // 서비스 감지 로그
        viewModelScope.launch {
            monitoring.logs.collect { log ->
                _state.update { it.copy(recentLogs = (listOf(log) + it.recentLogs).take(7)) }
            }
        }
        // 측정 중 여부 (프로세스가 죽었다 살아나도 서비스가 재설정)
        viewModelScope.launch {
            monitoring.isMonitoring.collect { running ->
                _state.update { it.copy(isMonitoring = running) }
            }
        }
        // 경과 시간 타이머
        viewModelScope.launch {
            combine(monitoring.isMonitoring, uiObserved) { running, observed -> running to observed }
                .distinctUntilChanged()
                .collectLatest { (running, observed) ->
                    if (running && observed) {
                        while (true) {
                            val start = monitoring.startedAt.value
                            val elapsed = if (start > 0) clock.millis() - start else 0
                            _state.update { it.copy(elapsedTime = formatElapsed(elapsed)) }
                            delay(1000)
                        }
                    } else if (!running) {
                        _state.update { it.copy(elapsedTime = "00:00:00") }
                    }
                }
        }
        // 감지 민감도(임계값) 설정
        viewModelScope.launch {
            settings.settings.collect { s -> _state.update { it.copy(threshold = s.threshold,
                gentleVibration = s.gentleVibration,
                suggestGentleVibration = !s.gentleVibration && recommendGentleVibration(it.sessionHistory, clock)) } }
        }
    }

    private fun handleIntent(intent: SnoreIntent) {
        when (intent) {
            is SnoreIntent.SetThemeMode -> settings.setThemeMode(intent.mode)
            is SnoreIntent.SaveFeedback -> {
                viewModelScope.launch {
                    if (monitoring.isMonitoring.value) {
                        _state.update { it.copy(journalMessage = "측정을 종료한 뒤 답변할 수 있어요.") }
                        return@launch
                    }
                    val session = _state.value.sessionHistory.firstOrNull { it.date == intent.date }
                    if (session == null || session.insights.vibrations.isEmpty()) return@launch
                    try {
                        repository.saveFeedback(intent.date, intent.feedback)
                        _state.update { it.copy(journalMessage = "아침 피드백을 저장했어요.") }
                    } catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { _state.update { it.copy(journalMessage = "피드백을 저장하지 못했어요. 다시 시도해 주세요.") } }
                }
            }
            is SnoreIntent.ApplyGentleSuggestion -> {
                if (monitoring.isMonitoring.value) {
                    _state.update { it.copy(journalMessage = "측정을 종료한 뒤 설정을 적용해 주세요.") }
                    return
                }
                if (!settings.settings.value.gentleVibration && recommendGentleVibration(_state.value.sessionHistory, clock)) {
                    settings.setGentleVibration(true)
                    _state.update { it.copy(journalMessage = "여유 모드를 적용했어요. 설정에서 변경할 수 있어요.") }
                }
            }
            is SnoreIntent.StartMonitoring -> {
                _state.update { it.copy(journalMessage = null) }
                monitoring.start()
            }

            is SnoreIntent.StopMonitoring -> {
                monitoring.stop()
            }

            is SnoreIntent.ClearHistory -> {
                viewModelScope.launch {
                    if (monitoring.isMonitoring.value) {
                        _state.update { it.copy(journalMessage = "측정을 종료한 뒤 기록을 삭제해 주세요.") }
                        return@launch
                    }
                    try {
                        repository.clearHistory()
                        _state.update { it.copy(journalMessage = "기록을 삭제했어요.") }
                    } catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { _state.update { it.copy(journalMessage = "기록을 삭제하지 못했어요. 다시 시도해 주세요.") } }
                }
            }

            is SnoreIntent.LoadHistory -> {
                if (historyJob?.isActive == true) return
                // 전체 히스토리 1개 스트림으로 분석 목록 + 오늘/어제 통계를 함께 파생한다.
                // '오늘'을 매 방출 시점에 재계산하므로, 측정 중 자정을 넘겨도 홈 통계가 새 날 행으로 따라간다.
                historyJob = viewModelScope.launch {
                    observeSleepHistory().collect { history ->
                        val todaySession = history.today
                        val prevSession = history.yesterday
                        _state.update {
                            it.copy(
                                sessionHistory = history.sessions,
                                suggestGentleVibration = !settings.settings.value.gentleVibration &&
                                    recommendGentleVibration(history.sessions, clock),
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

    fun saveMorningFeedback(date: Long, feedback: com.example.quietnight.domain.model.MorningFeedback) =
        handleIntent(SnoreIntent.SaveFeedback(date, feedback))

    fun applyGentleSuggestion() = handleIntent(SnoreIntent.ApplyGentleSuggestion)

    // 상세 다이얼로그에서 해당 날짜의 코골이 클립을 로드(컴포저블에서 코루틴으로 호출)
    suspend fun clipsForDate(date: Long): List<SnoreClip> = repository.clipsForDate(date)

    private fun formatElapsed(ms: Long): String {
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
    }
}
