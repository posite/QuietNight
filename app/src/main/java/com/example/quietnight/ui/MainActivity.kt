package com.example.quietnight.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.quietnight.data.AppSettings
import com.example.quietnight.ui.theme.QuietNightTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // 핵심 권한(마이크) 여부로 안내 다이얼로그 표시 결정
        showMicDialog = !isRecordAudioGranted()
    }

    private val viewModel: SleepViewModel by viewModels<SleepViewModel>()
    private val settingsViewModel: SettingsViewModel by viewModels<SettingsViewModel>()

    // 마이크 권한 거부 시 노출되는 안내 다이얼로그
    private var showMicDialog by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkPermissions()
        viewModel.loadSessionHistory()

        setContent {
            QuietNightTheme {
                val state by viewModel.state.collectAsState()
                val settings by settingsViewModel.settings.collectAsState()

                QuietNightApp(
                    state = state,
                    settings = settings,
                    onStartMonitoring = { viewModel.startMonitoring() },
                    onStopMonitoring = { viewModel.stopMonitoring() },
                    onThresholdChange = { settingsViewModel.setThreshold(it) },
                    onVibrationToggle = { settingsViewModel.setVibrationEnabled(it) },
                    onVibrationStrengthChange = { settingsViewModel.setVibrationStrength(it) }
                )

                if (showMicDialog) {
                    AlertDialog(
                        onDismissRequest = { },
                        title = { Text("마이크 권한 필요") },
                        text = { Text("코골이 감지를 위해 마이크 권한이 필요합니다. 권한을 허용하거나 앱을 종료해 주세요.") },
                        confirmButton = {
                            TextButton(onClick = { requestMic() }) { Text("권한 허용") }
                        },
                        dismissButton = {
                            TextButton(onClick = { finish() }) { Text("앱 종료") }
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 설정에서 권한을 허용하고 돌아온 경우 다이얼로그 닫기
        if (isRecordAudioGranted()) showMicDialog = false
    }

    private fun isRecordAudioGranted(): Boolean =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

    private fun requestMic() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
            permissionsLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
        } else {
            // 영구 거부(다시 묻지 않음) → 앱 설정 화면으로 이동
            startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", packageName, null)
                )
            )
        }
    }

    private fun checkPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.VIBRATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.VIBRATE)
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.FOREGROUND_SERVICE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.FOREGROUND_SERVICE)
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.FOREGROUND_SERVICE_MICROPHONE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                permissionsToRequest.add(Manifest.permission.FOREGROUND_SERVICE_MICROPHONE)
            }
        }
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionsLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            Log.d("Main", "All granted!!")
        }
    }
}

@Composable
fun QuietNightApp(
    state: SnoreState,
    settings: AppSettings,
    onStartMonitoring: () -> Unit,
    onStopMonitoring: () -> Unit,
    onThresholdChange: (Int) -> Unit,
    onVibrationToggle: (Boolean) -> Unit,
    onVibrationStrengthChange: (Int) -> Unit
) {
    val navController = rememberNavController()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 24.dp)
    ) {

        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 68.dp)
        ) {

            composable(Screen.Home.route) {
                HomeScreen(
                    state = state,
                    onOpenSettings = { navController.navigate(Screen.Settings.route) }
                ) {
                    if (state.isMonitoring) onStopMonitoring() else onStartMonitoring()
                }
            }
            composable(Screen.Monitor.route) {
                MonitorScreen(state) { onStopMonitoring() }
            }
            composable(Screen.Analyze.route) {
                AnalyzeScreen(state)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    settings = settings,
                    onThresholdChange = onThresholdChange,
                    onVibrationToggle = onVibrationToggle,
                    onVibrationStrengthChange = onVibrationStrengthChange,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(68.dp)
                .border(
                    width = 0.5.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BOTTOM_SCREENS.forEach { screen ->
                val selected =
                    currentDestination?.hierarchy?.any { it.route == screen.route } == true
                BottomNavItem(
                    screen = screen,
                    selected = selected,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    screen: Screen,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
    ) {
        Icon(
            imageVector = screen.icon,
            contentDescription = screen.label,
            tint = if (selected) MaterialTheme.colorScheme.onBackground else Color.Gray,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(3.dp))
        Text(
            screen.label,
            fontSize = 10.sp,
            color = if (selected) MaterialTheme.colorScheme.onBackground else Color.Gray
        )
    }
}
