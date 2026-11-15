package com.example.quietnight.wear

internal enum class MonitoringAction { START, STOP, REQUEST_PERMISSION, OPEN_SETTINGS, UNAVAILABLE }

internal fun monitoringAction(
    isMonitoring: Boolean,
    hasMicrophone: Boolean,
    microphoneGranted: Boolean,
    requestedBefore: Boolean,
    canRequestAgain: Boolean
): MonitoringAction = when {
    // 권한이 취소되었거나 하드웨어 오류가 발생해도 종료는 가능해야 한다.
    isMonitoring -> MonitoringAction.STOP
    !hasMicrophone -> MonitoringAction.UNAVAILABLE
    microphoneGranted -> MonitoringAction.START
    !requestedBefore || canRequestAgain -> MonitoringAction.REQUEST_PERMISSION
    else -> MonitoringAction.OPEN_SETTINGS
}
