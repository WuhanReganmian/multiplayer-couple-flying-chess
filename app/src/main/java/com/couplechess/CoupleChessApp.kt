package com.couplechess

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
    val scope = rememberCoroutineScope()

    CoupleChessTheme {
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onStartGame = { navController.navigate(Screen.PlayerSetup.route) },
                    onManageTasks = { navController.navigate(Screen.TaskManager.route) }
                )
            }

            composable(Screen.PlayerSetup.route) {
                PlayerSetupScreen(
                    taskRepository = taskRepository,
                    onNavigateToGame = { players ->
                        // 异步加载任务，设置 GameStateHolder，然后导航
                        scope.launch {
                            val tasks = withContext(Dispatchers.IO) {
                                taskRepository.getAllGroupedByLevel()
                            }
                            GameStateHolder.setGameData(players, tasks)
                            navController.navigate(Screen.Game.route)
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
                // 从 GameStateHolder 读取数据
                val players by GameStateHolder.players.collectAsState()
                val tasks by GameStateHolder.tasks.collectAsState()
                
                GameScreen(
                    players = players,
                    tasks = tasks,
                    onGameFinished = {
                        // 清理状态并返回主页
                        GameStateHolder.clear()
                        navController.popBackStack(Screen.Home.route, inclusive = false)
                    }
                )
            }
        }
    }
}
