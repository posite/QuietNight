package com.example.quietnight.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home : Screen("home", "홈", Icons.Rounded.Home)
    object Monitor : Screen("monitor", "수면", Icons.Rounded.Mic)
    object Analyze : Screen("analyze", "분석", Icons.Rounded.BarChart)
    object Settings : Screen("settings", "설정", Icons.Rounded.Settings)
}

// 하단 네비게이션은 3개(홈/수면/분석). 설정은 홈 화면의 톱니 아이콘으로 진입한다.
val BOTTOM_SCREENS = listOf(Screen.Home, Screen.Monitor, Screen.Analyze)
