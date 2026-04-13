package com.couplechess.ui.screens.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.couplechess.ui.theme.TaskCellColor
import com.couplechess.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlin.math.abs

/**
 * Main game screen with rectangular spiral board.
 * 
 * Layout based on demo.jpeg:
 * - Rectangular spiral path around the edge
 * - 36 cells total (configurable)
 * - Center area for game rules / current status
 * - Dice and controls at bottom
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    players: List<Player> = emptyList(),
    tasks: Map<TaskLevel, List<Task>> = emptyMap(),
    onGameFinished: () -> Unit
) {
    val viewModel: GameViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    
    // Initialize game when screen launches
    LaunchedEffect(players) {
        if (players.isNotEmpty()) {
            viewModel.initializeGame(players, tasks)
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
                    IconButton(onClick = onGameFinished) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "退出游戏"
                        )
                    }
                },
                actions = {
                    // Turn indicator
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
                        onRollDice = viewModel::rollDice,
                        onAcceptTask = viewModel::acceptTask,
                        onRejectTask = viewModel::rejectTask,
                        getPlayer = viewModel::getPlayer
                    )
                }
            }
            
            // Task card overlay
            AnimatedVisibility(
                visible = uiState.taskCardState != TaskCardState.Hidden && uiState.currentTask != null,
                enter = fadeIn() + scaleIn(initialScale = 0.8f),
                exit = fadeOut() + scaleOut(targetScale = 0.8f),
                modifier = Modifier.fillMaxSize()
            ) {
                uiState.currentTask?.let { taskInfo ->
                    TaskCardOverlay(
                        taskInfo = taskInfo,
                        cardState = uiState.taskCardState,
                        onAccept = viewModel::acceptTask,
                        onReject = viewModel::rejectTask
                    )
                }
            }
            
            // Game over overlay
            if (viewModel.isGameOver()) {
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
        
        // Game board
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            SpiralBoard(
                board = gameState.board,
                players = gameState.players,
                movingPiece = uiState.movingPiece,
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

/**
 * Rectangular spiral board rendered with Canvas.
 * Path follows the outer edge, spiraling inward.
 */
@Composable
private fun SpiralBoard(
    board: List<BoardCell>,
    players: List<Player>,
    movingPiece: MovingPieceState?,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    
    Canvas(modifier = modifier) {
        val cellCount = board.size
        val padding = 8.dp.toPx()
        val boardWidth = size.width - padding * 2
        val boardHeight = size.height - padding * 2
        
        // Calculate cell positions for rectangular spiral
        val cellPositions = calculateSpiralPositions(cellCount, boardWidth, boardHeight, padding)
        
        // Draw cells
        board.forEachIndexed { index, cell ->
            val pos = cellPositions.getOrNull(index) ?: return@forEachIndexed
            drawBoardCell(
                cell = cell,
                position = pos.first,
                size = pos.second,
                textMeasurer = textMeasurer
            )
        }
        
        // Draw path connections
        for (i in 0 until cellCount - 1) {
            val pos1 = cellPositions.getOrNull(i) ?: continue
            val pos2 = cellPositions.getOrNull(i + 1) ?: continue
            val center1 = Offset(
                pos1.first.x + pos1.second.width / 2,
                pos1.first.y + pos1.second.height / 2
            )
            val center2 = Offset(
                pos2.first.x + pos2.second.width / 2,
                pos2.first.y + pos2.second.height / 2
            )
            drawLine(
                color = DividerColor,
                start = center1,
                end = center2,
                strokeWidth = 2.dp.toPx()
            )
        }
        
        // Draw player pieces
        players.forEach { player ->
            val position = if (movingPiece != null && movingPiece.playerId == player.id) {
                // Interpolate position during animation
                val fromPos = cellPositions.getOrNull(movingPiece.fromPosition)
                val toPos = cellPositions.getOrNull(movingPiece.toPosition)
                if (fromPos != null && toPos != null) {
                    val fromCenter = Offset(
                        fromPos.first.x + fromPos.second.width / 2,
                        fromPos.first.y + fromPos.second.height / 2
                    )
                    val toCenter = Offset(
                        toPos.first.x + toPos.second.width / 2,
                        toPos.first.y + toPos.second.height / 2
                    )
                    Offset(
                        fromCenter.x + (toCenter.x - fromCenter.x) * movingPiece.progress,
                        fromCenter.y + (toCenter.y - fromCenter.y) * movingPiece.progress
                    )
                } else {
                    cellPositions.getOrNull(player.position)?.let {
                        Offset(it.first.x + it.second.width / 2, it.first.y + it.second.height / 2)
                    }
                }
            } else {
                cellPositions.getOrNull(player.position)?.let {
                    Offset(it.first.x + it.second.width / 2, it.first.y + it.second.height / 2)
                }
            }
            
            position?.let { pos ->
                drawPlayerPiece(
                    position = pos,
                    player = player,
                    pieceRadius = 12.dp.toPx()
                )
            }
        }
    }
}

/**
 * Calculate positions for rectangular spiral layout.
 */
private fun calculateSpiralPositions(
    cellCount: Int,
    boardWidth: Float,
    boardHeight: Float,
    padding: Float
): List<Pair<Offset, Size>> {
    val positions = mutableListOf<Pair<Offset, Size>>()
    
    // Calculate cells per side (approximate)
    // For 36 cells: 10-8-10-8 pattern (top, right, bottom, left)
    val perimeter = cellCount
    val aspectRatio = boardWidth / boardHeight
    
    // Distribute cells based on aspect ratio
    val horizontalCells = (perimeter * aspectRatio / (2 * (1 + aspectRatio))).toInt().coerceIn(6, 14)
    val verticalCells = ((perimeter - 2 * horizontalCells) / 2).coerceIn(4, 10)
    
    val cellWidth = (boardWidth - padding * 2) / horizontalCells
    val cellHeight = (boardHeight - padding * 2) / verticalCells
    val cellSize = Size(cellWidth * 0.9f, cellHeight * 0.9f)
    
    var index = 0
    
    // Top row (left to right)
    for (i in 0 until horizontalCells) {
        if (index >= cellCount) break
        positions.add(
            Offset(
                padding + i * cellWidth + cellWidth * 0.05f,
                padding
            ) to cellSize
        )
        index++
    }
    
    // Right column (top to bottom)
    for (i in 1 until verticalCells) {
        if (index >= cellCount) break
        positions.add(
            Offset(
                padding + (horizontalCells - 1) * cellWidth + cellWidth * 0.05f,
                padding + i * cellHeight
            ) to cellSize
        )
        index++
    }
    
    // Bottom row (right to left)
    for (i in (horizontalCells - 2) downTo 0) {
        if (index >= cellCount) break
        positions.add(
            Offset(
                padding + i * cellWidth + cellWidth * 0.05f,
                padding + (verticalCells - 1) * cellHeight
            ) to cellSize
        )
        index++
    }
    
    // Left column (bottom to top)
    for (i in (verticalCells - 2) downTo 1) {
        if (index >= cellCount) break
        positions.add(
            Offset(
                padding,
                padding + i * cellHeight
            ) to cellSize
        )
        index++
    }
    
    return positions
}

private fun DrawScope.drawBoardCell(
    cell: BoardCell,
    position: Offset,
    size: Size,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    val color = when (cell.cellType) {
        CellType.START -> StartCellColor
        CellType.FINISH -> FinishCellColor
        CellType.TASK -> TaskCellColor
        CellType.NORMAL -> NormalCellColor
    }
    
    // Draw cell background
    drawRoundRect(
        color = color,
        topLeft = position,
        size = size,
        cornerRadius = CornerRadius(8.dp.toPx())
    )
    
    // Draw cell border
    drawRoundRect(
        color = SoftWhite.copy(alpha = 0.3f),
        topLeft = position,
        size = size,
        cornerRadius = CornerRadius(8.dp.toPx()),
        style = Stroke(width = 1.dp.toPx())
    )
    
    // Draw cell index
    val indexText = "${cell.index + 1}"
    val textStyle = TextStyle(
        color = if (cell.cellType == CellType.TASK) PureBlack else SoftWhite,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold
    )
    val textLayoutResult = textMeasurer.measure(indexText, textStyle)
    drawText(
        textLayoutResult = textLayoutResult,
        topLeft = Offset(
            position.x + (size.width - textLayoutResult.size.width) / 2,
            position.y + (size.height - textLayoutResult.size.height) / 2
        )
    )
}

private fun DrawScope.drawPlayerPiece(
    position: Offset,
    player: Player,
    pieceRadius: Float
) {
    val color = if (player.gender == Gender.MALE) MaleColor else FemaleColor
    
    // Draw shadow
    drawCircle(
        color = Color.Black.copy(alpha = 0.3f),
        radius = pieceRadius,
        center = position + Offset(2.dp.toPx(), 2.dp.toPx())
    )
    
    // Draw piece
    drawCircle(
        color = color,
        radius = pieceRadius,
        center = position
    )
    
    // Draw border
    drawCircle(
        color = SoftWhite,
        radius = pieceRadius,
        center = position,
        style = Stroke(width = 2.dp.toPx())
    )
}

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
            // Avatar circle
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
            // Turn info
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
            
            // Dice display
            DiceDisplay(
                diceAnimation = diceAnimation,
                modifier = Modifier.size(80.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Roll button
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

@Composable
private fun DiceDisplay(
    diceAnimation: DiceAnimationState,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (diceAnimation is DiceAnimationState.Rolling) 360f else 0f,
        animationSpec = if (diceAnimation is DiceAnimationState.Rolling) {
            tween(200, easing = LinearEasing)
        } else {
            tween(0)
        },
        label = "rotation"
    )
    
    val currentValue = when (diceAnimation) {
        is DiceAnimationState.Idle -> 1
        is DiceAnimationState.Rolling -> diceAnimation.currentFace
        is DiceAnimationState.Stopped -> diceAnimation.value
    }
    
    Box(
        modifier = modifier
            .graphicsLayer {
                rotationZ = if (diceAnimation is DiceAnimationState.Rolling) rotation else 0f
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
        // Draw dice dots based on value
        DiceDots(value = currentValue)
    }
}

@Composable
private fun DiceDots(value: Int) {
    Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
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

@Composable
private fun TaskCardOverlay(
    taskInfo: TaskDisplayInfo,
    cardState: TaskCardState,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = when (cardState) {
            TaskCardState.Hidden -> 180f
            TaskCardState.FlippingIn -> 0f
            TaskCardState.Visible -> 0f
            TaskCardState.FlippingOut -> 180f
        },
        animationSpec = tween(300),
        label = "cardRotation"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(enabled = false) { },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 12f * density
                },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = BackgroundElevated
            ),
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
                    text = "【${taskInfo.executorName}】→【${taskInfo.targetName}】",
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
                    // Reject button
                    Button(
                        onClick = onReject,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ButtonDanger
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "拒绝",
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // Accept button
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
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "接受",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                // Warning text
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "拒绝将后退 1-3 步",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}

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
                    Text(
                        text = "返回首页",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

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
