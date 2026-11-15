package com.example.quietnight.wear

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.example.quietnight.ui.SettingsViewModel
import com.example.quietnight.ui.SleepViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WearActivity : ComponentActivity() {
    private val sleep: SleepViewModel by viewModels()
    private val settings: SettingsViewModel by viewModels()
    private val permissionPrefs by lazy { getSharedPreferences("wear_permissions", MODE_PRIVATE) }
    private var pendingStart = false
    private var message by mutableStateOf<String?>(null)

    private val permissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        val shouldStart = pendingStart
        pendingStart = false
        if (granted(Manifest.permission.RECORD_AUDIO)) {
            message = null
            if (shouldStart) startMonitoring()
        } else {
            message = "측정하려면 마이크 권한이 필요합니다. 측정 시작을 눌러 권한을 허용해 주세요."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingStart = savedInstanceState?.getBoolean("pending_start") ?: false
        sleep.loadSessionHistory()
        setContent {
            val state by sleep.state.collectAsStateWithLifecycle()
            val appSettings by settings.settings.collectAsStateWithLifecycle()
            val navigation = rememberSwipeDismissableNavController()
            MaterialTheme {
                AppScaffold {
                    SwipeDismissableNavHost(navController = navigation, startDestination = "monitor") {
                        composable("monitor") {
                            WearMonitorScreen(state, onToggle = ::toggleMonitoring,
                                onHistory = { navigation.navigate("history") { launchSingleTop = true } },
                                onSettings = { navigation.navigate("settings") { launchSingleTop = true } },
                                message = message)
                        }
                        composable("history") {
                            WearHistoryScreen(state.sessionHistory, onBack = { navigation.popBackStack() },
                                onNight = { navigation.navigate("night/$it") })
                        }
                        composable("night/{date}") { entry ->
                            val date = entry.arguments?.getString("date")?.toLongOrNull()
                            WearNightScreen(state.sessionHistory.firstOrNull { it.date == date }, state,
                                sleep::saveMorningFeedback, sleep::applyGentleSuggestion,
                                onBack = { navigation.popBackStack() })
                        }
                        composable("settings") {
                            WearSettingsScreen(appSettings, onThreshold = settings::setThreshold,
                                onVibration = settings::setVibrationEnabled,
                                onGentleVibration = settings::setGentleVibration,
                                onBack = { navigation.popBackStack() })
                        }
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("pending_start", pendingStart)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        if (granted(Manifest.permission.RECORD_AUDIO)) message = null
    }

    private fun toggleMonitoring() {
        when (monitoringAction(sleep.state.value.isMonitoring,
            packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE),
            granted(Manifest.permission.RECORD_AUDIO),
            permissionPrefs.getBoolean("microphone_requested", false),
            shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO))) {
            MonitoringAction.STOP -> sleep.stopMonitoring()
            MonitoringAction.START -> {
                if (Build.VERSION.SDK_INT >= 33 && !granted(Manifest.permission.POST_NOTIFICATIONS) &&
                    !permissionPrefs.getBoolean("notifications_requested", false)) {
                    requestPermissionsForStart(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                } else startMonitoring()
            }
            MonitoringAction.REQUEST_PERMISSION -> {
                val requested = mutableListOf(Manifest.permission.RECORD_AUDIO)
                if (Build.VERSION.SDK_INT >= 33 && !granted(Manifest.permission.POST_NOTIFICATIONS)) {
                    requested.add(Manifest.permission.POST_NOTIFICATIONS)
                }
                requestPermissionsForStart(requested.toTypedArray())
            }
            MonitoringAction.OPEN_SETTINGS -> {
                message = "앱 설정에서 마이크를 허용한 뒤 측정 시작을 다시 눌러 주세요."
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", packageName, null)))
            }
            MonitoringAction.UNAVAILABLE -> message = "마이크가 없는 워치에서는 측정할 수 없습니다."
        }
    }

    private fun requestPermissionsForStart(requested: Array<String>) {
        if (pendingStart) return
        permissionPrefs.edit().apply {
            if (Manifest.permission.RECORD_AUDIO in requested) putBoolean("microphone_requested", true)
            if (Manifest.permission.POST_NOTIFICATIONS in requested) putBoolean("notifications_requested", true)
        }.apply()
        pendingStart = true
        permissions.launch(requested)
    }

    private fun startMonitoring() {
        // 마이크 서비스는 사용자가 보고 있는 화면에서만 시작한다.
        try {
            sleep.startMonitoring()
            message = null
        } catch (_: IllegalStateException) {
            message = "측정을 시작하지 못했습니다. 화면에서 다시 시도해 주세요."
        } catch (_: SecurityException) {
            message = "마이크 권한을 확인한 뒤 다시 시도해 주세요."
        }
    }

    private fun granted(permission: String) =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}
