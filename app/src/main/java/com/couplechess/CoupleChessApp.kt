package com.couplechess

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.couplechess.data.DefaultTasks
import com.couplechess.data.GameSaveManager
import com.couplechess.data.GameStateHolder
import com.couplechess.data.db.AppDatabase
import com.couplechess.data.repository.TaskRepository
import com.couplechess.ui.navigation.Screen
import com.couplechess.ui.screens.game.GameScreen
import com.couplechess.ui.screens.home.HomeScreen
import com.couplechess.ui.screens.playersetup.PlayerSetupScreen
import com.couplechess.ui.screens.taskmanager.TaskManagerScreen
import com.couplechess.ui.theme.CoupleChessTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CoupleChessApp(
    context: Context,
    navController: NavHostController = rememberNavController()
) {
    val db = AppDatabase.getInstance(context)
    val taskRepository = TaskRepository(db.taskDao())
    val gameSaveManager = remember { GameSaveManager(context) }
    val scope = rememberCoroutineScope()

    // Track whether a saved game exists (refreshed on every Home visit)
    var hasSavedGame by remember { mutableStateOf(gameSaveManager.hasSave()) }

    // Seed default tasks into Room on first-ever launch
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            taskRepository.seedIfEmpty(DefaultTasks.all)
        }
    }

    CoupleChessTheme {
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route
        ) {
            composable(Screen.Home.route) {
                // Refresh saved-game flag each time Home is shown
                LaunchedEffect(Unit) {
                    hasSavedGame = gameSaveManager.hasSave()
                }

                HomeScreen(
                    hasSavedGame = hasSavedGame,
                    onStartGame = {
                        // Clear any stale save before starting fresh
                        gameSaveManager.clear()
                        hasSavedGame = false
                        navController.navigate(Screen.PlayerSetup.route)
                    },
                    onContinueGame = {
                        // Load saved snapshot → populate GameStateHolder → navigate to Game
                        scope.launch {
                            val snapshot = withContext(Dispatchers.IO) {
                                gameSaveManager.load()
                            }
                            if (snapshot != null) {
                                GameStateHolder.setGameDataFromSnapshot(snapshot)
                                navController.navigate(Screen.Game.route) {
                                    popUpTo(Screen.Home.route) { inclusive = false }
                                }
                            } else {
                                // Save was corrupted / cleared between tap and load
                                hasSavedGame = false
                            }
                        }
                    },
                    onManageTasks = { navController.navigate(Screen.TaskManager.route) }
                )
            }

            composable(Screen.PlayerSetup.route) {
                PlayerSetupScreen(
                    taskRepository = taskRepository,
                    onNavigateToGame = { players ->
                        scope.launch {
                            val tasks = withContext(Dispatchers.IO) {
                                taskRepository.getAllGroupedByLevel()
                            }
                            GameStateHolder.setGameData(players, tasks)
                            navController.navigate(Screen.Game.route) {
                                // Remove setup from backstack so back goes to Home
                                popUpTo(Screen.Home.route) { inclusive = false }
                            }
                        }
                    },
                    onNavigateToTaskManager = {
                        navController.navigate(Screen.TaskManager.route)
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.TaskManager.route) {
                TaskManagerScreen(
                    taskRepository = taskRepository,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Game.route) {
                val players by GameStateHolder.players.collectAsState()
                val tasks by GameStateHolder.tasks.collectAsState()

                GameScreen(
                    players = players,
                    tasks = tasks,
                    gameSaveManager = gameSaveManager,
                    onGameFinished = {
                        // Back from game → straight to Home, clear entire backstack above Home
                        GameStateHolder.clear()
                        navController.popBackStack(Screen.Home.route, inclusive = false)
                    }
                )
            }
        }
    }
}
