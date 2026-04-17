package com.couplechess.ui.screens.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.couplechess.data.GameSaveManager
import com.couplechess.data.GameStateHolder
import com.couplechess.data.model.BoardCell
import com.couplechess.data.model.CellType
import com.couplechess.data.model.Gender
import com.couplechess.data.model.Player
import com.couplechess.data.model.Task
import com.couplechess.data.model.TaskLevel
import com.couplechess.ui.theme.BackgroundCard
import com.couplechess.ui.theme.BackgroundElevated
import com.couplechess.ui.theme.ButtonDanger
import com.couplechess.ui.theme.DividerColor
import com.couplechess.ui.theme.FemaleColor
import com.couplechess.ui.theme.FinishCellColor
import com.couplechess.ui.theme.Gold
import com.couplechess.ui.theme.LevelColors
import com.couplechess.ui.theme.MaleColor
import com.couplechess.ui.theme.NormalCellColor
import com.couplechess.ui.theme.PureBlack
import com.couplechess.ui.theme.SoftWhite
import com.couplechess.ui.theme.StartCellColor
import com.couplechess.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

// ============================================================
// Main Game Screen
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    players: List<Player> = emptyList(),
    tasks: Map<TaskLevel, List<Task>> = emptyMap(),
    gameSaveManager: GameSaveManager? = null,
    onGameFinished: () -> Unit
) {
    val viewModel: GameViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    // Inject save manager once
    LaunchedEffect(gameSaveManager) {
        viewModel.setSaveManager(gameSaveManager)
    }

    // Initialize or restore game when screen launches
    LaunchedEffect(players) {
        if (players.isNotEmpty()) {
            val savedState = GameStateHolder.savedGameState.value
            if (savedState != null) {
                viewModel.restoreGame(players, tasks, savedState)
            } else {
                viewModel.initializeGame(players, tasks)
            }
        }
    }

    // Background gradient based on highest player level
    val maxLevel = players.maxOfOrNull { it.currentLevel.levelInt } ?: 1
    val backgroundBrush = remember(maxLevel) {
        Brush.verticalGradient(
            colors = listOf(
                PureBlack,
                LevelColors.getOrElse(maxLevel - 1) { LevelColors[0] }.copy(alpha = 0.3f),
                PureBlack
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "情侣飞行棋",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.onExitGame()
                        onGameFinished()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "退出游戏"
                        )
                    }
                },
                actions = {
                    viewModel.getCurrentPlayer()?.let { player ->
                        PlayerChip(
                            player = player,
                            isCurrentTurn = true
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PureBlack,
                    titleContentColor = SoftWhite,
                    navigationIconContentColor = SoftWhite
                )
            )
        },
        containerColor = PureBlack
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(backgroundBrush)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingIndicator()
                }
                uiState.error != null -> {
                    ErrorDisplay(
                        error = uiState.error!!,
                        onRetry = { /* Retry logic */ }
                    )
                }
                uiState.gameState != null -> {
                    GameContent(
                        uiState = uiState,
                        viewModel = viewModel,
                        onRollDice = viewModel::rollDice,
                        onAcceptTask = viewModel::acceptTask,
                        onRejectTask = viewModel::rejectTask,
                        getPlayer = viewModel::getPlayer
                    )
                }
            }

            // Task card overlay — fade only, no flip
            AnimatedVisibility(
                visible = uiState.taskCardState != TaskCardState.Hidden && uiState.currentTask != null,
                enter = fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.9f, animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(250)) + scaleOut(targetScale = 0.9f, animationSpec = tween(250)),
                modifier = Modifier.fillMaxSize()
            ) {
                uiState.currentTask?.let { taskInfo ->
                    TaskCardOverlay(
                        taskInfo = taskInfo,
                        onAccept = viewModel::acceptTask,
                        onReject = viewModel::rejectTask
                    )
                }
            }

            // Cell preview overlay
            uiState.previewTask?.let { task ->
                CellPreviewOverlay(
                    task = task,
                    onDismiss = viewModel::dismissCellPreview
                )
            }

// Game over overlay
if (viewModel.isGameOver()) {
LaunchedEffect(Unit) {
viewModel.onGameOver()
}
GameOverOverlay(
                    winner = viewModel.getWinner(),
                    onDismiss = onGameFinished
                )
            }
        }
    }
}

@Composable
private fun GameContent(
    uiState: GameUiState,
    viewModel: GameViewModel,
    onRollDice: () -> Unit,
    onAcceptTask: () -> Unit,
    onRejectTask: () -> Unit,
    getPlayer: (Int) -> Player?
) {
    val gameState = uiState.gameState ?: return

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Players status bar
        PlayersStatusBar(
            players = gameState.players,
            currentPlayerIndex = gameState.currentPlayerIndex,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Game board — spiral to center
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            SpiralBoard(
                board = gameState.board,
                players = gameState.players,
                movingPiece = uiState.movingPiece,
                currentPlayerIndex = gameState.currentPlayerIndex,
                viewModel = viewModel,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            )
        }

        // Bottom controls
        BottomGameControls(
            diceAnimation = uiState.diceAnimation,
            canRoll = gameState.phase is com.couplechess.data.model.GamePhase.WaitingForRoll,
            turnCount = gameState.turnCount,
            currentPlayer = gameState.players.getOrNull(gameState.currentPlayerIndex),
            onRollDice = onRollDice,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}

// ============================================================
// Spiral Board — Box-based layout, clickable cells, no lines
// ============================================================

/**
 * Data class for a cell's position on the grid.
 */
private data class SpiralCellPos(
    val row: Int,
    val col: Int
)

/**
 * Generate a clockwise rectangular spiral that converges to the center.
 * Returns list of (row, col) in traversal order. Index 0 = Start, last = Finish (center).
 */
private fun generateSpiralPath(totalCells: Int): List<SpiralCellPos> {
    // Choose grid dimensions large enough for the spiral
    // For 36 cells, a 7×7 grid works well (outer ring=24, next ring=16, …)
    // We need: perimeter of rings until we have enough cells
    val gridSize = when {
        totalCells <= 8 -> 3
        totalCells <= 24 -> 5
        totalCells <= 48 -> 7
        totalCells <= 80 -> 9
        else -> 11
    }

    val path = mutableListOf<SpiralCellPos>()
    var top = 0; var bottom = gridSize - 1; var left = 0; var right = gridSize - 1

    while (path.size < totalCells && top <= bottom && left <= right) {
        // Top row: left → right
        for (col in left..right) {
            if (path.size >= totalCells) break
            path.add(SpiralCellPos(top, col))
        }
        top++

        // Right column: top → bottom
        for (row in top..bottom) {
            if (path.size >= totalCells) break
            path.add(SpiralCellPos(row, right))
        }
        right--

        // Bottom row: right → left
        if (top <= bottom) {
            for (col in right downTo left) {
                if (path.size >= totalCells) break
                path.add(SpiralCellPos(bottom, col))
            }
            bottom--
        }

        // Left column: bottom → top
        if (left <= right) {
            for (row in bottom downTo top) {
                if (path.size >= totalCells) break
                path.add(SpiralCellPos(row, left))
            }
            left++
        }
    }

    return path
}

@Composable
private fun SpiralBoard(
    board: List<BoardCell>,
    players: List<Player>,
    movingPiece: MovingPieceState?,
    currentPlayerIndex: Int,
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val cellCount = board.size
    val spiralPath = remember(cellCount) { generateSpiralPath(cellCount) }
    val gridSize = when {
        cellCount <= 8 -> 3
        cellCount <= 24 -> 5
        cellCount <= 48 -> 7
        cellCount <= 80 -> 9
        else -> 11
    }

    // Current turn flash animation
    val flashTransition = rememberInfiniteTransition(label = "turnFlash")
    val flashAlpha by flashTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flashAlpha"
    )

    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val boardSizePx = with(density) { maxWidth.toPx() }
        val cellSizeDp = maxWidth / gridSize
        val cellSizePx = with(density) { cellSizeDp.toPx() }
        val gap = 2.dp

        // Draw cells
        spiralPath.forEachIndexed { index, pos ->
            if (index >= board.size) return@forEachIndexed
            val cell = board[index]
            val task = viewModel.getTaskForCell(cell.index)

            // Check if current player is on this cell
            val currentPlayer = players.getOrNull(currentPlayerIndex)
            val isCurrentPlayerCell = currentPlayer != null && currentPlayer.position == index

            BoardCellBox(
                cell = cell,
                task = task,
                cellSize = cellSizeDp - gap * 2,
                isCurrentPlayerCell = isCurrentPlayerCell,
                flashAlpha = flashAlpha,
                modifier = Modifier.offset(
                    x = cellSizeDp * pos.col + gap,
                    y = cellSizeDp * pos.row + gap
                ),
                onClick = { viewModel.showCellPreview(cell.index) }
            )
        }

        // Draw player pieces
        players.forEach { player ->
            val targetIndex = player.position.coerceIn(0, spiralPath.size - 1)

            // Compute position (handle animation)
            val offsetX: Float
            val offsetY: Float

            if (movingPiece != null && movingPiece.playerId == player.id) {
                val fromIdx = movingPiece.fromPosition.coerceIn(0, spiralPath.size - 1)
                val toIdx = movingPiece.toPosition.coerceIn(0, spiralPath.size - 1)
                val fromPos = spiralPath[fromIdx]
                val toPos = spiralPath[toIdx]
                val fromX = fromPos.col * cellSizePx + cellSizePx / 2
                val fromY = fromPos.row * cellSizePx + cellSizePx / 2
                val toX = toPos.col * cellSizePx + cellSizePx / 2
                val toY = toPos.row * cellSizePx + cellSizePx / 2
                offsetX = fromX + (toX - fromX) * movingPiece.progress
                offsetY = fromY + (toY - fromY) * movingPiece.progress
            } else {
                val pos = spiralPath[targetIndex]
                offsetX = pos.col * cellSizePx + cellSizePx / 2
                offsetY = pos.row * cellSizePx + cellSizePx / 2
            }

            val pieceSize = 20.dp
            val pieceSizePx = with(density) { pieceSize.toPx() }
            val playerIndex = players.indexOf(player)
            // Slight offset per player so overlapping pieces fan out
            val fanOffset = (playerIndex - players.size / 2f) * pieceSizePx * 0.4f

            PlayerPieceComposable(
                player = player,
                size = pieceSize,
                modifier = Modifier.offset {
                    IntOffset(
                        (offsetX - pieceSizePx / 2 + fanOffset).roundToInt(),
                        (offsetY - pieceSizePx / 2).roundToInt()
                    )
                }
            )
        }
    }
}

@Composable
private fun BoardCellBox(
    cell: BoardCell,
    task: Task?,
    cellSize: Dp,
    isCurrentPlayerCell: Boolean,
    flashAlpha: Float,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgColor = when (cell.cellType) {
        CellType.START -> StartCellColor
        CellType.FINISH -> FinishCellColor
        CellType.TASK -> {
            // Color based on task level if available
            if (task != null) {
                LevelColors.getOrElse(task.level.levelInt - 1) { Gold }
            } else {
                Gold
            }
        }
        CellType.NORMAL -> NormalCellColor
    }

    val borderColor = if (isCurrentPlayerCell) {
        Gold.copy(alpha = flashAlpha)
    } else {
        SoftWhite.copy(alpha = 0.2f)
    }

    val borderWidth = if (isCurrentPlayerCell) 2.dp else 1.dp

    Box(
        modifier = modifier
            .size(cellSize)
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(borderWidth, borderColor, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Cell label
            Text(
                text = when (cell.cellType) {
                    CellType.START -> "起"
                    CellType.FINISH -> "终"
                    else -> "${cell.index + 1}"
                },
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = if (cell.cellType == CellType.TASK && task != null) {
                    SoftWhite
                } else if (cell.cellType == CellType.START || cell.cellType == CellType.FINISH) {
                    PureBlack
                } else {
                    SoftWhite
                },
                maxLines = 1
            )
            // Show task level abbreviation
            if (task != null) {
                Text(
                    text = task.level.name,
                    fontSize = 6.sp,
                    color = SoftWhite.copy(alpha = 0.7f),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun PlayerPieceComposable(
    player: Player,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val color = if (player.gender == Gender.MALE) MaleColor else FemaleColor

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.4f))
    ) {
        Box(
            modifier = Modifier
                .size(size - 2.dp)
                .align(Alignment.Center)
                .clip(CircleShape)
                .background(color)
                .border(1.5f.dp, SoftWhite, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = player.name.firstOrNull()?.uppercase() ?: "?",
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = SoftWhite
            )
        }
    }
}

// ============================================================
// Players Status Bar
// ============================================================

@Composable
private fun PlayersStatusBar(
    players: List<Player>,
    currentPlayerIndex: Int,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        items(players) { player ->
            PlayerChip(
                player = player,
                isCurrentTurn = players.indexOf(player) == currentPlayerIndex
            )
        }
    }
}

@Composable
private fun PlayerChip(
    player: Player,
    isCurrentTurn: Boolean,
    modifier: Modifier = Modifier
) {
    val genderColor = if (player.gender == Gender.MALE) MaleColor else FemaleColor
    val pulseAnimation = rememberInfiniteTransition(label = "pulse")
    val scale by pulseAnimation.animateFloat(
        initialValue = 1f,
        targetValue = if (isCurrentTurn) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Card(
        modifier = modifier
            .scale(scale),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentTurn) genderColor.copy(alpha = 0.3f) else BackgroundCard
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(genderColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = player.name.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.labelSmall,
                    color = SoftWhite,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = SoftWhite,
                    fontWeight = if (isCurrentTurn) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = player.currentLevel.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}

// ============================================================
// Bottom Controls with spring dice
// ============================================================

@Composable
private fun BottomGameControls(
    diceAnimation: DiceAnimationState,
    canRoll: Boolean,
    turnCount: Int,
    currentPlayer: Player?,
    onRollDice: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundElevated)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "第 $turnCount 回合",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )

            if (currentPlayer != null) {
                Text(
                    text = "${currentPlayer.name} 的回合",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (currentPlayer.gender == Gender.MALE) MaleColor else FemaleColor,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dice with spring animation
            DiceDisplay(
                diceAnimation = diceAnimation,
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onRollDice,
                enabled = canRoll && diceAnimation is DiceAnimationState.Idle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Gold,
                    contentColor = PureBlack,
                    disabledContainerColor = DividerColor,
                    disabledContentColor = TextSecondary
                )
            ) {
                Text(
                    text = if (diceAnimation is DiceAnimationState.Rolling) "投掷中..." else "掷骰子",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ============================================================
// Dice with spring bounce + 3D rotation
// ============================================================

@Composable
private fun DiceDisplay(
    diceAnimation: DiceAnimationState,
    modifier: Modifier = Modifier
) {
    // Spring-based scale bounce when dice stops
    val springScale by animateFloatAsState(
        targetValue = when (diceAnimation) {
            is DiceAnimationState.Stopped -> 1f
            is DiceAnimationState.Rolling -> 0.85f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "springScale"
    )

    // 3D rotation during roll
    val rotationX by animateFloatAsState(
        targetValue = if (diceAnimation is DiceAnimationState.Rolling) 360f else 0f,
        animationSpec = if (diceAnimation is DiceAnimationState.Rolling) {
            tween(200, easing = LinearEasing)
        } else {
            spring(dampingRatio = Spring.DampingRatioLowBouncy)
        },
        label = "rotationX"
    )

    val rotationZ by animateFloatAsState(
        targetValue = if (diceAnimation is DiceAnimationState.Rolling) 180f else 0f,
        animationSpec = if (diceAnimation is DiceAnimationState.Rolling) {
            tween(300, easing = LinearEasing)
        } else {
            spring(dampingRatio = Spring.DampingRatioLowBouncy)
        },
        label = "rotationZ"
    )

    val currentValue = when (diceAnimation) {
        is DiceAnimationState.Idle -> 1
        is DiceAnimationState.Rolling -> diceAnimation.currentFace
        is DiceAnimationState.Stopped -> diceAnimation.value
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = springScale
                scaleY = springScale
                this.rotationX = if (diceAnimation is DiceAnimationState.Rolling) rotationX else 0f
                this.rotationZ = if (diceAnimation is DiceAnimationState.Rolling) rotationZ else 0f
                cameraDistance = 16f * density
            }
            .background(
                color = SoftWhite,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 2.dp,
                color = Gold,
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        DiceDots(value = currentValue)
    }
}

@Composable
private fun DiceDots(value: Int) {
    Canvas(modifier = Modifier
        .fillMaxSize()
        .padding(8.dp)) {
        val dotRadius = size.minDimension / 10
        val center = Offset(size.width / 2, size.height / 2)
        val offset = size.minDimension / 3.5f

        val positions = when (value) {
            1 -> listOf(center)
            2 -> listOf(
                Offset(center.x - offset, center.y - offset),
                Offset(center.x + offset, center.y + offset)
            )
            3 -> listOf(
                Offset(center.x - offset, center.y - offset),
                center,
                Offset(center.x + offset, center.y + offset)
            )
            4 -> listOf(
                Offset(center.x - offset, center.y - offset),
                Offset(center.x + offset, center.y - offset),
                Offset(center.x - offset, center.y + offset),
                Offset(center.x + offset, center.y + offset)
            )
            5 -> listOf(
                Offset(center.x - offset, center.y - offset),
                Offset(center.x + offset, center.y - offset),
                center,
                Offset(center.x - offset, center.y + offset),
                Offset(center.x + offset, center.y + offset)
            )
            6 -> listOf(
                Offset(center.x - offset, center.y - offset),
                Offset(center.x + offset, center.y - offset),
                Offset(center.x - offset, center.y),
                Offset(center.x + offset, center.y),
                Offset(center.x - offset, center.y + offset),
                Offset(center.x + offset, center.y + offset)
            )
            else -> listOf(center)
        }

        positions.forEach { pos ->
            drawCircle(
                color = Color.Black,
                radius = dotRadius,
                center = pos
            )
        }
    }
}

// ============================================================
// Task Card Overlay — FADE ONLY, no flip
// ============================================================

@Composable
private fun TaskCardOverlay(
    taskInfo: TaskDisplayInfo,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(enabled = false) { },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.85f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = BackgroundElevated),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Level badge
                Box(
                    modifier = Modifier
                        .background(
                            color = LevelColors.getOrElse(taskInfo.task.level.levelInt - 1) { Gold },
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = taskInfo.task.level.displayName,
                        style = MaterialTheme.typography.labelLarge,
                        color = SoftWhite,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Players involved
                Text(
                    text = "【${taskInfo.executorName}】对【${taskInfo.targetName}】执行惩罚",
                    style = MaterialTheme.typography.titleMedium,
                    color = Gold,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Task description
                Text(
                    text = taskInfo.task.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = SoftWhite,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = onReject,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ButtonDanger)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "拒绝", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onAccept,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Gold,
                            contentColor = PureBlack
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Done,
                            contentDescription = null,
                            tint = PureBlack,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "接受",
                            fontWeight = FontWeight.Bold,
                            color = PureBlack
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "你必须接受任务才能继续游戏",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}

// ============================================================
// Cell Preview Overlay — tapping any cell shows its task
// ============================================================

@Composable
private fun CellPreviewOverlay(
    task: Task,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.75f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BackgroundElevated),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            color = LevelColors.getOrElse(task.level.levelInt - 1) { Gold },
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = task.level.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = SoftWhite,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SoftWhite,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "点击任意处关闭",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}

// ============================================================
// Game Over Overlay
// ============================================================

@Composable
private fun GameOverOverlay(
    winner: Player?,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = BackgroundElevated)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎉",
                    style = MaterialTheme.typography.displayLarge
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "游戏结束",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Gold,
                    fontWeight = FontWeight.Bold
                )

                if (winner != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "胜者：${winner.name}",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (winner.gender == Gender.MALE) MaleColor else FemaleColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Gold,
                        contentColor = PureBlack
                    )
                ) {
                    Text(text = "返回首页", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ============================================================
// Loading & Error
// ============================================================

@Composable
private fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val rotation = rememberInfiniteTransition(label = "loading")
            val angle by rotation.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing)
                ),
                label = "angle"
            )

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .rotate(angle)
                    .border(4.dp, Gold, CircleShape)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "加载中...",
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun ErrorDisplay(
    error: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "❌",
                style = MaterialTheme.typography.displayMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = error,
                style = MaterialTheme.typography.bodyLarge,
                color = ButtonDanger,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Gold)
            ) {
                Text("重试", color = PureBlack)
            }
        }
    }
}
