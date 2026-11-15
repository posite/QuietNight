package com.example.quietnight.service

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal fun launchMonitoringTask(
    scope: CoroutineScope,
    onFailure: (Throwable) -> Unit,
    block: suspend CoroutineScope.() -> Unit
): Job = scope.launch {
    try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        onFailure(failure)
    }
}
