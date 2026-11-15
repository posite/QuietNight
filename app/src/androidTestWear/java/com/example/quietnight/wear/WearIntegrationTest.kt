package com.example.quietnight.wear

import android.content.ComponentName
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import com.example.quietnight.service.SnoreService
import org.junit.Assert.*
import org.junit.Test

class WearIntegrationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun notificationLaunchTargetIsTheWatchActivity() {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        assertEquals(WearActivity::class.java.name, launch?.component?.className)
    }

    @Test fun mergedApplicationContainsPrivateMicrophoneService() {
        val service = context.packageManager.getServiceInfo(ComponentName(context, SnoreService::class.java), 0)
        assertFalse(service.exported)
        assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE, service.foregroundServiceType)
        if (Build.VERSION.SDK_INT >= 34) {
            assertEquals(PackageManager.PERMISSION_GRANTED,
                context.packageManager.checkPermission("android.permission.FOREGROUND_SERVICE_MICROPHONE", context.packageName))
        }
    }

    @Test fun watchNeverEnablesAudioClipStorage() {
        assertFalse(context.resources.getBoolean(com.example.quietnight.data.R.bool.supports_snore_clips))
    }
}
