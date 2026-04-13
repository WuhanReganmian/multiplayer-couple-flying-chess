package com.couplechess.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object PlayerSetup : Screen("player_setup")
    data object TaskManager : Screen("task_manager")
    data object Game : Screen("game")
}
