package com.example.quietnight.wear

import org.junit.Assert.assertEquals
import org.junit.Test

class MonitoringActionTest {
    @Test fun grantedMicrophoneStartsMonitoring() {
        assertEquals(MonitoringAction.START, monitoringAction(false, true, true, false, false))
    }
    @Test fun firstRequestOpensPermissionPrompt() {
        assertEquals(MonitoringAction.REQUEST_PERMISSION, monitoringAction(false, true, false, false, false))
    }
    @Test fun deniedPermissionCanBeRequestedAgain() {
        assertEquals(MonitoringAction.REQUEST_PERMISSION, monitoringAction(false, true, false, true, true))
    }
    @Test fun permanentlyDeniedPermissionOpensSettings() {
        assertEquals(MonitoringAction.OPEN_SETTINGS, monitoringAction(false, true, false, true, false))
    }
    @Test fun absentMicrophoneCannotStart() {
        assertEquals(MonitoringAction.UNAVAILABLE, monitoringAction(false, false, true, false, false))
    }
    @Test fun monitoringCanAlwaysBeStoppedEvenAfterPermissionRevocation() {
        assertEquals(MonitoringAction.STOP, monitoringAction(true, false, false, true, false))
    }
}
