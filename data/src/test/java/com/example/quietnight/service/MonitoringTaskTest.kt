package com.example.quietnight.service

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MonitoringTaskTest {

    @Test
    fun unexpectedFailureUsesTheControlledShutdownCallback() = runTest {
        var captured: Throwable? = null

        launchMonitoringTask(backgroundScope, onFailure = { captured = it }) {
            error("측정 실패")
        }
        runCurrent()

        assertEquals("측정 실패", captured?.message)
    }

    @Test
    fun normalCancellationDoesNotReportAFailure() = runTest {
        var captured: Throwable? = null
        val task = launchMonitoringTask(backgroundScope, onFailure = { captured = it }) {
            awaitCancellation()
        }
        runCurrent()

        task.cancel()
        runCurrent()

        assertNull(captured)
    }
}
