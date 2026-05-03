package com.example.quietnight.ui

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quietnight.data.SleepSession
import com.example.quietnight.data.SnoreDao
import com.example.quietnight.service.SnoreService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

@HiltViewModel
class SleepViewModel @Inject constructor(
    private val dao: SnoreDao,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(SnoreState())
    val state = _state.asStateFlow()

    init {
        // Service 데이터 구독
        viewModelScope.launch {
            SnoreService.dbFlow.collect { db ->
                _state.update {
                    it.copy(
                        currentDb = db,
                        todaySnoreMax = max(it.todaySnoreMax, db),
                        todaySnoreMin = min(it.todaySnoreMin, db)
                    )
                }
            }
        }
        viewModelScope.launch {
            SnoreService.logFlow.collect { log ->
                _state.update { it.copy(recentLogs = (listOf(log) + it.recentLogs).take(7)) }
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
                _state.update { it.copy(isMonitoring = false, todaySnoreMax = 0) }
                saveSession(intent.session)
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

    private fun saveSession(session: SleepSession) {
        viewModelScope.launch {
            dao.insertSession(session)
        }
    }
}