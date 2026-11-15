package com.example.quietnight.ui

import androidx.lifecycle.ViewModelStore
import com.example.quietnight.domain.model.AppSettings
import com.example.quietnight.domain.model.SleepSession
import com.example.quietnight.domain.model.SnoreClip
import com.example.quietnight.domain.model.MorningFeedback
import com.example.quietnight.domain.model.NightInsights
import com.example.quietnight.domain.model.VibrationObservation
import com.example.quietnight.domain.monitoring.MonitoringController
import com.example.quietnight.domain.repository.SettingsRepository
import com.example.quietnight.domain.repository.SleepRepository
import com.example.quietnight.domain.usecase.ObserveSleepHistoryUseCase
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SleepViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val clock = Clock.fixed(Instant.parse("2026-09-14T01:00:00Z"), ZoneId.of("Asia/Seoul"))
    private val repository = FakeSleepRepository()
    private val monitoring = FakeMonitoringController()
    private val settings = FakeSettingsRepository()

    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { store.clear(); Dispatchers.resetMain() }

    private fun viewModel() = SleepViewModel(repository, settings, monitoring,
        ObserveSleepHistoryUseCase(repository, clock), clock).also { store.put("sleep", it) }

    // 실패한 assertion도 타이머를 정리한 뒤 보고한다. @After는 runTest 종료 후라 너무 늦다.
    private fun runViewModelTest(body: suspend TestScope.() -> Unit) = runTest(dispatcher) {
        try { body() } finally { store.clear() }
    }

    private fun TestScope.observe(vm: SleepViewModel) =
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect() }

    @Test fun commandsDelegateButServiceRemainsTheSourceOfRunningState() = runViewModelTest {
        val vm = viewModel()
        observe(vm)
        runCurrent()
        vm.startMonitoring()
        assertEquals(1, monitoring.starts)
        // 시작 요청만으로 실제 측정 상태를 미리 확정하지 않는다.
        assertFalse(vm.state.value.isMonitoring)
        monitoring.startedAt.value = clock.millis() - 65_000
        monitoring.isMonitoring.value = true
        runCurrent()
        assertTrue(vm.state.value.isMonitoring)
        assertEquals("00:01:05", vm.state.value.elapsedTime)
        vm.stopMonitoring()
        assertEquals(1, monitoring.stops)
        assertTrue(vm.state.value.isMonitoring)
        monitoring.isMonitoring.value = false
        runCurrent()
        assertFalse(vm.state.value.isMonitoring)
        assertEquals("00:00:00", vm.state.value.elapsedTime)
        store.clear()
    }

    @Test fun historySubscriptionIsIdempotentAndClearUsesRepository() = runViewModelTest {
        val vm = viewModel()
        val today = clock.instant().atZone(clock.zone).toLocalDate().atStartOfDay(clock.zone).toInstant().toEpochMilli()
        repository.rows.value = listOf(SleepSession(date = today, score = 81, snoreTime = 1200, sleepTime = 70000))
        vm.loadSessionHistory()
        vm.loadSessionHistory()
        runCurrent()
        assertEquals(1, repository.subscriptions)
        assertEquals(81, vm.state.value.todayScore)
        assertEquals(1200L, vm.state.value.todaySnoreTime)
        vm.clearAllSessions()
        runCurrent()
        assertEquals(1, repository.clears)
        assertTrue(vm.state.value.sessionHistory.isEmpty())
        assertEquals(0, vm.state.value.todayScore)
        store.clear()
    }

    @Test fun realtimeSettingsLogsAndClipsUseDomainContracts() = runViewModelTest {
        val vm = viewModel()
        observe(vm)
        runCurrent()
        monitoring.decibels.value = 82
        settings.setThreshold(65)
        repeat(9) { monitoring.logs.emit("감지 $it") }
        advanceTimeBy(250)
        runCurrent()
        assertEquals(82, vm.state.value.currentDb)
        assertEquals(65, vm.state.value.threshold)
        assertEquals(7, vm.state.value.recentLogs.size)
        assertEquals("감지 8", vm.state.value.recentLogs.first())
        assertEquals(repository.clips, vm.clipsForDate(123))
        assertEquals(123L, repository.requestedDate)
        store.clear()
    }

    @Test fun fastUiUpdatesRunOnlyWhileObservedAndDecibelsAreThrottled() = runViewModelTest {
        val vm = viewModel()
        runCurrent()

        monitoring.startedAt.value = clock.millis() - 65_000
        monitoring.isMonitoring.value = true
        monitoring.decibels.value = 40
        runCurrent()
        assertEquals(0, vm.state.value.currentDb)
        assertEquals("00:00:00", vm.state.value.elapsedTime)

        val observer = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.state.collect()
        }
        runCurrent()
        monitoring.decibels.value = 50
        advanceTimeBy(249)
        runCurrent()
        assertEquals(0, vm.state.value.currentDb)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(50, vm.state.value.currentDb)
        assertEquals("00:01:05", vm.state.value.elapsedTime)

        observer.cancel()
        runCurrent()
        monitoring.decibels.value = 70
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(50, vm.state.value.currentDb)
        assertEquals("00:01:05", vm.state.value.elapsedTime)
    }

    @Test fun feedbackPersistsAndPersonalizationNeedsExplicitApproval() = runViewModelTest {
        val vm = viewModel()
        val today = clock.instant().atZone(clock.zone).toLocalDate().atStartOfDay(clock.zone).toInstant().toEpochMilli()
        val row = SleepSession(date = today, score = 90, snoreTime = 0, sleepTime = 60000,
            insights = NightInsights(vibrations = listOf(VibrationObservation(today + 1000, 80, 30))))
        repository.rows.value = listOf(row, row.copy(date = today - 86_400_000, feedback = MorningFeedback.DISTURBED))
        vm.loadSessionHistory()
        runCurrent()
        vm.saveMorningFeedback(today, MorningFeedback.DISTURBED)
        runCurrent()
        assertEquals(MorningFeedback.DISTURBED, repository.rows.value.first().feedback)
        assertTrue(vm.state.value.suggestGentleVibration)
        assertFalse(settings.settings.value.gentleVibration)
        vm.applyGentleSuggestion()
        runCurrent()
        assertTrue(settings.settings.value.gentleVibration)
        assertFalse(vm.state.value.suggestGentleVibration)
    }

    @Test fun activeMonitoringBlocksFeedbackAndHistoryDeletion() = runViewModelTest {
        val vm = viewModel()
        repository.rows.value = listOf(SleepSession(date = 1000, score = 90, snoreTime = 0, sleepTime = 1000))
        vm.loadSessionHistory()
        monitoring.isMonitoring.value = true
        runCurrent()
        vm.saveMorningFeedback(1000, MorningFeedback.DISTURBED)
        vm.clearAllSessions()
        runCurrent()
        // assertion이 실패해도 테스트 스케줄러가 실시간 타이머를 무한히 진행하지 않도록 먼저 정리한다.
        store.clear()
        assertEquals(0, repository.clears)
        assertEquals(MorningFeedback.NONE, repository.rows.value.single().feedback)
        assertNotNull(vm.state.value.journalMessage)
    }

    @Test fun applyingWithoutASuggestionDoesNotChangeSettings() = runViewModelTest {
        val vm = viewModel()
        vm.applyGentleSuggestion()
        runCurrent()
        assertFalse(settings.settings.value.gentleVibration)
    }

    private class FakeSleepRepository : SleepRepository {
        val rows = MutableStateFlow<List<SleepSession>>(emptyList())
        val clips = listOf(SnoreClip(1, 123, 456, 80, "/clips/1.wav", 6000))
        var subscriptions = 0
        var clears = 0
        var requestedDate = 0L
        override fun observeHistory() = rows.also { subscriptions++ }
        override suspend fun clipsForDate(date: Long): List<SnoreClip> {
            requestedDate = date
            return clips
        }
        override suspend fun clearHistory() { clears++; rows.value = emptyList() }
        override suspend fun saveFeedback(date: Long, feedback: MorningFeedback) {
            rows.value = rows.value.map { if (it.date == date) it.copy(feedback = feedback) else it }
        }
    }

    private class FakeMonitoringController : MonitoringController {
        override val decibels = MutableStateFlow(0)
        override val logs = MutableSharedFlow<String>()
        override val isMonitoring = MutableStateFlow(false)
        override val startedAt = MutableStateFlow(0L)
        var starts = 0
        var stops = 0
        override fun start() { starts++ }
        override fun stop() { stops++ }
    }

    private class FakeSettingsRepository : SettingsRepository {
        override val settings = MutableStateFlow(AppSettings())
        override fun setThreshold(value: Int) { settings.value = settings.value.copy(threshold = value) }
        override fun setVibrationEnabled(enabled: Boolean) { settings.value = settings.value.copy(vibrationEnabled = enabled) }
        override fun setVibrationStrength(value: Int) { settings.value = settings.value.copy(vibrationStrength = value) }
        override fun setRecordClips(enabled: Boolean) { settings.value = settings.value.copy(recordClips = enabled) }
        override fun setGentleVibration(enabled: Boolean) { settings.value = settings.value.copy(gentleVibration = enabled) }
        override fun setThemeMode(mode: com.example.quietnight.domain.model.ThemeMode) {
            settings.value = settings.value.copy(themeMode = mode)
        }
    }
}
