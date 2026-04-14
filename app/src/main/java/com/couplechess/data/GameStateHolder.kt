package com.couplechess.data

import com.couplechess.data.model.GameState
import com.couplechess.data.model.Player
import com.couplechess.data.model.Task
import com.couplechess.data.model.TaskLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 游戏状态持有者 - 单例
 * 
 * 用于在 PlayerSetupScreen 和 GameScreen 之间传递数据。
 * Navigation Compose 不支持直接传递复杂对象，所以使用此单例作为桥梁。
 * 
 * 生命周期：
 * - PlayerSetupScreen 开始游戏时调用 setGameData()
 * - GameScreen 启动时读取 players/tasks
 * - 游戏结束或返回时调用 clear()
 */
object GameStateHolder {
    
    private val _players = MutableStateFlow<List<Player>>(emptyList())
    val players: StateFlow<List<Player>> = _players.asStateFlow()
    
    private val _tasks = MutableStateFlow<Map<TaskLevel, List<Task>>>(emptyMap())
    val tasks: StateFlow<Map<TaskLevel, List<Task>>> = _tasks.asStateFlow()
    
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    /** 存档中的游戏状态（仅"继续游戏"时非 null） */
    private val _savedGameState = MutableStateFlow<GameState?>(null)
    val savedGameState: StateFlow<GameState?> = _savedGameState.asStateFlow()
    
    /**
     * 设置游戏数据（从 PlayerSetupScreen 调用，新游戏）
     */
    fun setGameData(players: List<Player>, tasks: Map<TaskLevel, List<Task>>) {
        _players.value = players
        _tasks.value = tasks
        _savedGameState.value = null
        _isReady.value = true
    }
    
    /**
     * 从存档快照恢复游戏数据（继续游戏时调用）
     * SavedGameSnapshot 的 tasks key 是 "L1"…"L5" 字符串，需转回 TaskLevel。
     */
    fun setGameDataFromSnapshot(snapshot: SavedGameSnapshot) {
        val levelMap = snapshot.tasks.mapKeys { (key, _) ->
            TaskLevel.entries.first { it.name == key }
        }
        // Use players from gameState (with correct positions) rather than
        // snapshot.players which may have stale position=0
        _players.value = snapshot.gameState.players
        _tasks.value = levelMap
        _savedGameState.value = snapshot.gameState
        _isReady.value = true
    }

    /**
     * 清除游戏数据（游戏结束或返回时调用）
     */
    fun clear() {
        _players.value = emptyList()
        _tasks.value = emptyMap()
        _savedGameState.value = null
        _isReady.value = false
    }
    
    /**
     * 获取当前快照（同步读取）
     */
    fun getSnapshot(): GameDataSnapshot? {
        return if (_isReady.value) {
            GameDataSnapshot(
                players = _players.value,
                tasks = _tasks.value
            )
        } else {
            null
        }
    }
}

/**
 * 游戏数据快照
 */
data class GameDataSnapshot(
    val players: List<Player>,
    val tasks: Map<TaskLevel, List<Task>>
)
