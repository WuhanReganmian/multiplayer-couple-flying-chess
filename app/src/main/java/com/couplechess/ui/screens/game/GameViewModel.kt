package com.couplechess.ui.screens.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val currentTask: TaskDisplayInfo? = null
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
    data object FlippingIn : TaskCardState()
    data object Visible : TaskCardState()
    data object FlippingOut : TaskCardState()
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
    
    /**
     * Initialize a new game with the given players and tasks.
     * Must be called before any other game actions.
     */
    fun initializeGame(
        players: List<Player>,
        tasks: Map<TaskLevel, List<Task>>
    ) {
        this.players = players
        
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
                
                // Get initial state by dispatching a no-op or reading initial state
                // For now, we construct initial state from config
                val initialBoard = generateInitialBoard(36)
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
            _uiState.update { it.copy(taskCardState = TaskCardState.FlippingOut) }
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
            }
        }
    }
    
    /**
     * Reject the current task (will cause retreat).
     */
    fun rejectTask() {
        viewModelScope.launch {
            _uiState.update { it.copy(taskCardState = TaskCardState.FlippingOut) }
            delay(300)
            
            val result = dispatchAction(GameAction.RejectTask)
            
            if (result != null) {
                // Find retreat event and animate
                val retreatEvent = result.events
                    .filterIsInstance<GameEvent.PlayerRetreated>()
                    .firstOrNull()
                    
                if (retreatEvent != null) {
                    _uiState.update { 
                        it.copy(
                            movingPiece = MovingPieceState(
                                playerId = retreatEvent.playerId,
                                fromPosition = retreatEvent.from,
                                toPosition = retreatEvent.to
                            )
                        ) 
                    }
                    
                    // Animate retreat
                    animateProgress(500)
                }
                
                _uiState.update { 
                    it.copy(
                        gameState = result.state,
                        taskCardState = TaskCardState.Hidden,
                        currentTask = null,
                        movingPiece = null
                    ) 
                }
            }
        }
    }
    
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
     * Show task card with flip animation.
     */
    private suspend fun showTaskCard(event: GameEvent.TaskTriggered) {
        val executor = players.find { it.id == event.executorId }
        val target = players.find { it.id == event.targetId }
        
        if (executor != null && target != null) {
            _uiState.update { 
                it.copy(
                    taskCardState = TaskCardState.FlippingIn,
                    currentTask = TaskDisplayInfo(
                        task = event.task,
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
     */
    private fun generateInitialBoard(size: Int): List<BoardCell> {
        return List(size) { index ->
            val cellType = when {
                index == 0 -> CellType.START
                index == size - 1 -> CellType.FINISH
                index % 4 == 0 -> CellType.TASK  // ~25% task cells
                else -> CellType.NORMAL
            }
            BoardCell(index = index, cellType = cellType)
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
     * Clean up Rust resources when ViewModel is cleared.
     */
    override fun onCleared() {
        super.onCleared()
        if (gameHandle != 0L) {
            try {
                GameBridge.destroyGame(gameHandle)
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
            gameHandle = 0L
        }
    }
}
