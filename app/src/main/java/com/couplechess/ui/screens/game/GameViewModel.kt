package com.couplechess.ui.screens.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.couplechess.data.GameSaveManager
import com.couplechess.data.SavedGameSnapshot
import com.couplechess.data.bridge.GameBridge
import com.couplechess.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * UI state for the game screen
 */
data class GameUiState(
    // Core game state from Rust
    val gameState: GameState? = null,

    // UI-specific state
    val isLoading: Boolean = true,
    val error: String? = null,

    // Animation states
    val diceAnimation: DiceAnimationState = DiceAnimationState.Idle,
    val movingPiece: MovingPieceState? = null,
    val taskCardState: TaskCardState = TaskCardState.Hidden,

    // Current task info for display
    val currentTask: TaskDisplayInfo? = null,

    // Preview-only task info (tapping a cell on the board)
    val previewTask: Task? = null
)

sealed class DiceAnimationState {
    data object Idle : DiceAnimationState()
    data class Rolling(val currentFace: Int) : DiceAnimationState()
    data class Stopped(val value: Int) : DiceAnimationState()
}

data class MovingPieceState(
    val playerId: Int,
    val fromPosition: Int,
    val toPosition: Int,
    val progress: Float = 0f
)

sealed class TaskCardState {
    data object Hidden : TaskCardState()
    data object Showing : TaskCardState()
    data object Visible : TaskCardState()
    data object Dismissing : TaskCardState()
}

data class TaskDisplayInfo(
    val task: Task,
    val executorName: String,
    val targetName: String
)

/**
 * ViewModel for the game screen.
 * Manages communication with Rust game logic via JNI.
 */
class GameViewModel : ViewModel() {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    // Rust game session handle
    private var gameHandle: Long = 0L

    // Local copy of players for name lookup
    private var players: List<Player> = emptyList()

    // Tasks grouped by level — kept for save/restore
    private var tasksByLevel: Map<TaskLevel, List<Task>> = emptyMap()

    // Random task assigned to each board cell index
    private var cellTasks: Map<Int, Task> = emptyMap()

    // Save manager reference — injected from composable
    private var gameSaveManager: GameSaveManager? = null

    /** Set the save manager reference (called once from GameScreen). */
    fun setSaveManager(manager: GameSaveManager?) {
        gameSaveManager = manager
    }

    /**
     * Initialize a new game with the given players and tasks.
     * Must be called before any other game actions.
     */
    fun initializeGame(
        players: List<Player>,
        tasks: Map<TaskLevel, List<Task>>
    ) {
        this.players = players
        this.tasksByLevel = tasks

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val config = GameConfig(
                    players = players,
                    boardSize = 36,
                    taskRatio = 0.25f,
                    seed = System.currentTimeMillis(),
                    tasks = tasks.mapKeys { it.key.name }
                )

                val configJson = json.encodeToString(config)

                // Create game on background thread (JNI call)
                val handle = withContext(Dispatchers.IO) {
                    GameBridge.createGame(configJson)
                }

                if (handle == 0L) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to create game session"
                        )
                    }
                    return@launch
                }

                gameHandle = handle

                // Generate initial board — all internal cells are TASK
                val initialBoard = generateInitialBoard(36)

                // Assign a random task to each TASK cell (filtered by player levels)
                cellTasks = assignTasksToCells(initialBoard, tasks, players)

                val initialState = GameState(
                    players = players,
                    board = initialBoard,
                    currentPlayerIndex = 0,
                    phase = GamePhase.WaitingForRoll,
                    turnCount = 0
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        gameState = initialState
                    )
                }

                // Auto-save initial state
                autoSave()

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "初始化游戏失败: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Restore a game from a saved snapshot (skip Rust init for now, use UI-only state).
     */
    fun restoreGame(
        players: List<Player>,
        tasks: Map<TaskLevel, List<Task>>,
        savedState: GameState
    ) {
        this.players = players
        this.tasksByLevel = tasks

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // Re-create Rust session for continued play
                val config = GameConfig(
                    players = players,
                    boardSize = savedState.board.size,
                    taskRatio = 0.25f,
                    seed = System.currentTimeMillis(),
                    tasks = tasks.mapKeys { it.key.name }
                )

                val configJson = json.encodeToString(config)
                val handle = withContext(Dispatchers.IO) {
                    GameBridge.createGame(configJson)
                }

                if (handle != 0L) {
                    gameHandle = handle
                }

                // Assign tasks to cells from saved board (filtered by player levels)
                cellTasks = assignTasksToCells(savedState.board, tasks, players)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        gameState = savedState
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "恢复游戏失败: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Roll the dice for the current player.
     */
    fun rollDice() {
        val state = _uiState.value.gameState ?: return
        if (state.phase !is GamePhase.WaitingForRoll) return

        viewModelScope.launch {
            // Start dice animation
            _uiState.update { it.copy(diceAnimation = DiceAnimationState.Rolling(1)) }

            // Animate dice faces for ~800ms
            repeat(8) { i ->
                delay(100)
                _uiState.update {
                    it.copy(diceAnimation = DiceAnimationState.Rolling((i % 6) + 1))
                }
            }

            // Dispatch action to Rust
            val result = dispatchAction(GameAction.RollDice)

            if (result != null) {
                // Find dice value from events
                val diceValue = result.events
                    .filterIsInstance<GameEvent.DiceRolled>()
                    .firstOrNull()?.value ?: 1

                // Show final dice value
                _uiState.update {
                    it.copy(
                        diceAnimation = DiceAnimationState.Stopped(diceValue),
                        gameState = result.state
                    )
                }

                // Process movement animation if applicable
                val moveEvent = result.events
                    .filterIsInstance<GameEvent.PlayerMoved>()
                    .firstOrNull()

                if (moveEvent != null) {
                    animateMovement(moveEvent)
                }

                // Check for task trigger
                val taskEvent = result.events
                    .filterIsInstance<GameEvent.TaskTriggered>()
                    .firstOrNull()

                if (taskEvent != null) {
                    showTaskCard(taskEvent)
                }

                // Auto-save after move
                autoSave()

                // Reset dice animation after a delay
                delay(1500)
                _uiState.update { it.copy(diceAnimation = DiceAnimationState.Idle) }
            }
        }
    }

    /**
     * Accept the current task.
     */
    fun acceptTask() {
        viewModelScope.launch {
            _uiState.update { it.copy(taskCardState = TaskCardState.Dismissing) }
            delay(300)

            val result = dispatchAction(GameAction.AcceptTask)

            if (result != null) {
                _uiState.update {
                    it.copy(
                        gameState = result.state,
                        taskCardState = TaskCardState.Hidden,
                        currentTask = null
                    )
                }
                autoSave()
            }
        }
    }

    /**
     * Reject the current task — retreat 1-5 steps (Rust decides), then
     * execute the task on the new cell (if it is a TASK cell).
     */
    fun rejectTask() {
        viewModelScope.launch {
            // Dismiss current task card
            _uiState.update { it.copy(taskCardState = TaskCardState.Dismissing) }
            delay(400)
            _uiState.update {
                it.copy(taskCardState = TaskCardState.Hidden, currentTask = null)
            }

            // Ask Rust to handle the rejection (retreat + possible new task)
            val result = dispatchAction(GameAction.RejectTask) ?: return@launch

            // Update game state with Rust result
            _uiState.update { it.copy(gameState = result.state) }

            // Animate the retreat movement
            val retreatEvent = result.events
                .filterIsInstance<GameEvent.PlayerRetreated>()
                .firstOrNull()

            if (retreatEvent != null) {
                // Reuse movement animation for the retreat
                animateMovement(
                    GameEvent.PlayerMoved(
                        playerId = retreatEvent.playerId,
                        from = retreatEvent.from,
                        to = retreatEvent.to
                    )
                )
            }

            // Check if Rust triggered a new task at the retreat position
            val newTaskEvent = result.events
                .filterIsInstance<GameEvent.TaskTriggered>()
                .firstOrNull()

            if (newTaskEvent != null) {
                // Show the new task (showTaskCard already uses cellTasks)
                showTaskCard(newTaskEvent)
            }

            autoSave()
        }
    }

    /** Show task preview for a board cell (tapping on the board). */
    fun showCellPreview(cellIndex: Int) {
        val task = cellTasks[cellIndex]
        _uiState.update { it.copy(previewTask = task) }
    }

    /** Dismiss task preview. */
    fun dismissCellPreview() {
        _uiState.update { it.copy(previewTask = null) }
    }

    /** Get the task assigned to a specific cell. */
    fun getTaskForCell(cellIndex: Int): Task? = cellTasks[cellIndex]

    /**
     * Dispatch an action to the Rust game engine.
     */
    private suspend fun dispatchAction(action: GameAction): ActionResult? {
        if (gameHandle == 0L) return null

        return try {
            val actionJson = json.encodeToString(action)

            val resultJson = withContext(Dispatchers.IO) {
                GameBridge.dispatchAction(gameHandle, actionJson)
            }

            json.decodeFromString<ActionResult>(resultJson)
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Action failed: ${e.message}") }
            null
        }
    }

    /**
     * Animate piece movement.
     */
    private suspend fun animateMovement(event: GameEvent.PlayerMoved) {
        _uiState.update {
            it.copy(
                movingPiece = MovingPieceState(
                    playerId = event.playerId,
                    fromPosition = event.from,
                    toPosition = event.to
                )
            )
        }

        animateProgress(800)

        _uiState.update { it.copy(movingPiece = null) }
    }

    /**
     * Animate progress from 0 to 1 over duration.
     */
    private suspend fun animateProgress(durationMs: Long) {
        val steps = 20
        val stepDelay = durationMs / steps

        for (i in 1..steps) {
            delay(stepDelay)
            val progress = i.toFloat() / steps
            _uiState.update { state ->
                state.copy(
                    movingPiece = state.movingPiece?.copy(progress = progress)
                )
            }
        }
    }

    /**
     * Show task card with fade-in animation.
     */
    private suspend fun showTaskCard(event: GameEvent.TaskTriggered) {
        val executor = players.find { it.id == event.executorId }
        val target = players.find { it.id == event.targetId }

        if (executor != null && target != null) {
            // Use the task assigned to this cell (Kotlin-side) so it matches
            // what the board UI displays; fall back to the Rust event task.
            val playerPosition = _uiState.value.gameState?.players
                ?.find { it.id == event.executorId }?.position
            val displayTask = playerPosition?.let { cellTasks[it] } ?: event.task

            _uiState.update {
                it.copy(
                    taskCardState = TaskCardState.Showing,
                    currentTask = TaskDisplayInfo(
                        task = displayTask,
                        executorName = executor.name,
                        targetName = target.name
                    )
                )
            }

            delay(300)
            _uiState.update { it.copy(taskCardState = TaskCardState.Visible) }
        }
    }

    /**
     * Generate initial board configuration.
     * All internal cells are TASK type (except Start at 0 and Finish at last).
     */
    private fun generateInitialBoard(size: Int): List<BoardCell> {
        return List(size) { index ->
            val cellType = when {
                index == 0 -> CellType.START
                index == size - 1 -> CellType.FINISH
                else -> CellType.TASK
            }
            BoardCell(index = index, cellType = cellType)
        }
    }

    /**
     * Assign a random task to each TASK cell, filtered by the players' chosen levels.
     * Only tasks from levels that at least one player has selected are included.
     */
    private fun assignTasksToCells(
        board: List<BoardCell>,
        tasks: Map<TaskLevel, List<Task>>,
        players: List<Player>
    ): Map<Int, Task> {
        val activeLevels = players.map { it.currentLevel }.toSet()
        val filteredTasks = tasks.filterKeys { it in activeLevels }.values.flatten()
        // Fallback to all tasks if no match (shouldn't happen normally)
        val pool = filteredTasks.ifEmpty { tasks.values.flatten() }
        if (pool.isEmpty()) return emptyMap()

        val result = mutableMapOf<Int, Task>()
        board.forEach { cell ->
            if (cell.cellType == CellType.TASK) {
                result[cell.index] = pool.random()
            }
        }
        return result
    }

    /**
     * Persist current game state to SharedPreferences.
     */
    private fun autoSave() {
        val state = _uiState.value.gameState ?: return
        val manager = gameSaveManager ?: return

        viewModelScope.launch(Dispatchers.IO) {
            val snapshot = SavedGameSnapshot(
                gameState = state,
                players = players,
                tasks = tasksByLevel.mapKeys { it.key.name }
            )
            manager.save(snapshot)
        }
    }

    /**
     * Get player by ID.
     */
    fun getPlayer(playerId: Int): Player? = players.find { it.id == playerId }

    /**
     * Get current player.
     */
    fun getCurrentPlayer(): Player? {
        val state = _uiState.value.gameState ?: return null
        return state.players.getOrNull(state.currentPlayerIndex)
    }

    /**
     * Check if game is over.
     */
    fun isGameOver(): Boolean {
        return _uiState.value.gameState?.phase is GamePhase.GameOver
    }

    /**
     * Get winner if game is over.
     */
    fun getWinner(): Player? {
        val phase = _uiState.value.gameState?.phase as? GamePhase.GameOver
            ?: return null
        return players.find { it.id == phase.winner }
    }

    /**
     * Called when user leaves the game — save is already auto-saved.
     */
    fun onExitGame() {
        // Save is already done by autoSave() after each action.
        // Nothing extra needed.
    }

    /**
     * Called when game is finished — clear the save.
     */
    fun onGameOver() {
        gameSaveManager?.clear()
    }

    /**
     * Clean up Rust resources when ViewModel is cleared.
     */
    override fun onCleared() {
        super.onCleared()
        if (gameHandle != 0L) {
            try {
                GameBridge.destroyGame(gameHandle)
            } catch (_: Exception) {
                // Ignore cleanup errors
            }
            gameHandle = 0L
        }
    }
}
