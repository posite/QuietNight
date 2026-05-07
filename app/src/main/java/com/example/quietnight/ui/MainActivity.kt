package com.example.quietnight.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.quietnight.data.SleepSession
import com.example.quietnight.ui.theme.QuietNightTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val notGranted = permissions.keys.filter { !permissions[it]!! }
        if (notGranted.isEmpty()) {
            Log.d("Main", "All granted!!")
        } else {
            Log.d("grant", notGranted.toString())
        }
    }

    private val viewModel: SleepViewModel by viewModels<SleepViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkPermissions()
        viewModel.handleIntent(SnoreIntent.LoadHistory)
        viewModel.handleIntent(SnoreIntent.TodayScore)
        onEffect()

        var startTime = 0L
        setContent {
            QuietNightTheme {
                val state by viewModel.state.collectAsState()
                QuietNightApp(state = state, onStopMonitoring = {
                    viewModel.handleIntent(
                        createSnoreSessionIntent(
                            startTime,
                            state.snoreTime
                        )
                    )
                }, onStartMonitoring = {
                    startTime = System.currentTimeMillis()
                    viewModel.handleIntent(SnoreIntent.StartMonitoring)
                })
            }
        }
    }

    private fun onEffect() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.effect.collect {
                    when (it) {
                        is SnoreEffect.SessionSaved -> {
                            Log.d("session", "saved")
                            viewModel.handleIntent(SnoreIntent.TodayScore)
                        }
                    }
                }
            }
        }
    }

    private fun checkPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.VIBRATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.VIBRATE)
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.FOREGROUND_SERVICE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.FOREGROUND_SERVICE)
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.FOREGROUND_SERVICE_MICROPHONE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                permissionsToRequest.add(Manifest.permission.FOREGROUND_SERVICE_MICROPHONE)
            }
        }
        if (ActivityCompat.checkSelfPermission(
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
    onStopMonitoring: () -> Unit,
    onStartMonitoring: () -> Unit
) {
    val navController = rememberNavController()
    //val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 12.dp)
    ) {

        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 68.dp)
        ) {

            composable(Screen.Home.route) {
                HomeScreen(state) {
                    if (state.isMonitoring) {
                        onStopMonitoring()
                    } else {
                        onStartMonitoring()
                    }
                }
            }
            composable(Screen.Monitor.route) {
                MonitorScreen(state) { onStopMonitoring() }
            }
            composable(Screen.Weekly.route) {
                WeeklyScreen(state)
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
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BOTTOM_SCREENS.forEach { screen ->
                val selected =
                    currentDestination?.hierarchy?.any { it.route == screen.route } == true
                BottomNavItem(
                    screen = screen,
                    selected = selected,
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

private fun createSnoreSessionIntent(startTime: Long, snoreTime: Long): SnoreIntent.StopMonitoring {
    val now = System.currentTimeMillis()
    val todayStartMillis = Instant.ofEpochMilli(now)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

    val sleepTime = now - startTime
    val diff = sleepTime - snoreTime * 100
    Log.d("log", "$sleepTime $snoreTime $diff")
    return SnoreIntent.StopMonitoring(
        SleepSession(
            date = todayStartMillis,
            score = (diff.toDouble() / sleepTime * 100).toInt(),
            snoreTime = snoreTime * 100,
            sleepTime = sleepTime,
            positionStatsJson = "등:82,옆:18"
        )
    )
}

@Composable
private fun BottomNavItem(
    screen: Screen,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp)
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
