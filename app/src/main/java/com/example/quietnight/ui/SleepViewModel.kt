package com.example.quietnight.ui

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quietnight.data.SleepSession
import com.example.quietnight.data.SnoreDao
import com.example.quietnight.service.SnoreService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import kotlin.math.max

@HiltViewModel
class SleepViewModel @Inject constructor(
    private val dao: SnoreDao,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(SnoreState())
    val state = _state.asStateFlow()

    private val _effect: Channel<SnoreEffect> = Channel()
    val effect = _effect.receiveAsFlow()


    init {
        // Service 데이터 구독
        viewModelScope.launch {
            SnoreService.dbFlow.collect { db ->
                _state.update {
                    it.copy(
                        currentDb = db,
                        todaySnoreMax = max(it.todaySnoreMax, db),
                    )
                }
            }
        }
        viewModelScope.launch {
            SnoreService.logFlow.collect { log ->
                _state.update {
                    it.copy(
                        recentLogs = (listOf(log) + it.recentLogs).take(7),
                        snoreTime = it.snoreTime + 1
                    )
                }
            }
        }
    }

    fun handleIntent(intent: SnoreIntent) {
        when (intent) {
            is SnoreIntent.StartMonitoring -> {
                context.startForegroundService(Intent(context, SnoreService::class.java))
                _state.update { it.copy(isMonitoring = true) }
            }

            is SnoreIntent.StopMonitoring -> {
                context.stopService(Intent(context, SnoreService::class.java))
                viewModelScope.launch {
                    upsertSleepSession(intent)
                    _state.update {
                        it.copy(
                            isMonitoring = false,
                            snoreTime = INIT_SNORE_VALUE
                        )
                    }
                    _effect.send(SnoreEffect.SessionSaved)
                }
            }

            is SnoreIntent.LoadHistory -> {
                viewModelScope.launch {
                    dao.getHistory().collect { history ->
                        _state.update { it.copy(weeklyHistory = history) }
                    }
                }
            }

            is SnoreIntent.TodayScore -> {
                viewModelScope.launch {
                    val now = System.currentTimeMillis()
                    val todayStartMillis = Instant.ofEpochMilli(now)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                    val session = dao.getSessionByDate(todayStartMillis)
                    Log.d("session", "$session")
                    session?.let {
                        _state.update {
                            it.copy(
                                todayScore = session.score,
                                todaySnoreTime = session.snoreTime / 100
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun upsertSleepSession(intent: SnoreIntent.StopMonitoring) {
        val prevSession: SleepSession? = dao.getSessionByDate(intent.session.date)
        if (prevSession != null) {
            val totalTime = intent.session.sleepTime + prevSession.sleepTime
            val totalSnoreTime = (intent.session.snoreTime + prevSession.snoreTime)
            val totalScore = (totalTime - totalSnoreTime).toDouble() / totalTime * PERCENTAGE

            Log.d(
                "snore",
                "${intent.session.sleepTime} ${intent.session.score} $totalTime $totalSnoreTime $totalScore"
            )
            dao.upsertSession(
                prevSession.copy(
                    score = totalScore.toInt(),
                    sleepTime = totalTime,
                    snoreTime = totalSnoreTime
                )
            )
        } else {
            dao.upsertSession(intent.session)
        }
    }

    companion object {
        private const val INIT_SNORE_VALUE = 0L
        private const val PERCENTAGE = 100
    }
}