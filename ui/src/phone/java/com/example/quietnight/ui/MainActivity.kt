package com.example.quietnight.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.quietnight.domain.model.AppSettings
import com.example.quietnight.domain.model.ThemeMode
import com.example.quietnight.ui.theme.QuietNightTheme
import com.example.quietnight.ui.theme.usesDarkTheme
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
            val state by viewModel.state.collectAsStateWithLifecycle()
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
            val darkTheme = settings.themeMode.usesDarkTheme(isSystemInDarkTheme())
            SideEffect {
                val barStyle = if (darkTheme) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
            }
            QuietNightTheme(themeMode = settings.themeMode) {

                QuietNightApp(
                    state = state,
                    settings = settings,
                    onStartMonitoring = { viewModel.startMonitoring() },
                    onStopMonitoring = { viewModel.stopMonitoring() },
                    onThresholdChange = { settingsViewModel.setThreshold(it) },
                    onVibrationToggle = { settingsViewModel.setVibrationEnabled(it) },
                    onVibrationStrengthChange = { settingsViewModel.setVibrationStrength(it) },
                    onRecordClipsToggle = { settingsViewModel.setRecordClips(it) },
                    onResetData = { viewModel.clearAllSessions() },
                    onApplyGentle = viewModel::applyGentleSuggestion,
                    onGentleVibration = settingsViewModel::setGentleVibration,
                    onThemeModeChange = settingsViewModel::setThemeMode,
                    clipProvider = { date -> viewModel.clipsForDate(date) }
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

    // 런타임 요청이 필요한 위험 권한만 요청한다.
    // VIBRATE / FOREGROUND_SERVICE / FOREGROUND_SERVICE_MICROPHONE 은 설치 시 자동 부여되는
    // normal 권한이라 런타임 요청 대상이 아니다(매니페스트 선언만으로 충분).
    private fun checkPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
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
    onVibrationStrengthChange: (Int) -> Unit,
    onRecordClipsToggle: (Boolean) -> Unit,
    onResetData: () -> Unit,
    onApplyGentle: () -> Unit,
    onGentleVibration: (Boolean) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    clipProvider: suspend (Long) -> List<com.example.quietnight.domain.model.SnoreClip>
) {
    val navController = rememberNavController()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()        // 상태바 높이만큼 실제 인셋 적용(기기별 노치 대응)
    ) {

        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {

            composable(Screen.Home.route) {
                HomeScreen(
                    state = state,
                    onOpenSettings = { navController.navigate(Screen.Settings.route) },
                    onOpenAnalysis = {
                        navController.navigate(Screen.Analyze.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onMonitorClicked = {
                        if (state.isMonitoring) {
                            onStopMonitoring()
                        } else {
                            onStartMonitoring()
                            // 측정 시작과 동시에 실시간 모니터 화면으로 이동
                            navController.navigate(Screen.Monitor.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
            composable(Screen.Monitor.route) {
                MonitorScreen(
                    state = state,
                    onStart = onStartMonitoring,
                    onStop = onStopMonitoring
                )
            }
            composable(Screen.Analyze.route) {
                AnalyzeScreen(state, clipProvider = clipProvider,
                    onApplyGentle = onApplyGentle, onOpenMonitor = {
                        navController.navigate(Screen.Monitor.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    })
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    settings = settings,
                    onThresholdChange = onThresholdChange,
                    onVibrationToggle = onVibrationToggle,
                    onVibrationStrengthChange = onVibrationStrengthChange,
                    onRecordClipsToggle = onRecordClipsToggle,
                    onResetData = onResetData,
                    onGentleVibration = onGentleVibration,
                    onThemeModeChange = onThemeModeChange,
                    message = state.journalMessage,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .selectableGroup()
                // 바 배경은 화면 끝까지 이어지고, 버튼만 시스템 내비게이션 영역을 피한다.
                .navigationBarsPadding()
                .heightIn(min = 80.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BOTTOM_SCREENS.forEach { screen ->
                val selected =
                    currentDestination?.hierarchy?.any { it.route == screen.route } == true ||
                        (screen == Screen.Home && currentDestination?.route == Screen.Settings.route)
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
            .padding(horizontal = 6.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(vertical = 8.dp)
    ) {
        Icon(
            imageVector = screen.icon,
            contentDescription = screen.label,
            tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(3.dp))
        Text(
            screen.label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
