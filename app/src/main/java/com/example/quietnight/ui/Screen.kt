package com.example.quietnight.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home : Screen("home", "홈", Icons.Rounded.Home)
    object Monitor : Screen("monitor", "수면", Icons.Rounded.Mic)
    object Weekly : Screen("weekly", "분석", Icons.Rounded.BarChart)
}

val BOTTOM_SCREENS = listOf(Screen.Home, Screen.Monitor, Screen.Weekly)