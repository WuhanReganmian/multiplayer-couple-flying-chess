package com.couplechess.ui.screens.playersetup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.couplechess.data.model.Player
import com.couplechess.data.repository.TaskRepository
import com.couplechess.ui.components.AddPlayerButton
import com.couplechess.ui.components.PlayerCard
import com.couplechess.ui.theme.BackgroundCard
import com.couplechess.ui.theme.BackgroundElevated
import com.couplechess.ui.theme.ButtonDanger
import com.couplechess.ui.theme.DividerColor
import com.couplechess.ui.theme.Gold
import com.couplechess.ui.theme.PureBlack
import com.couplechess.ui.theme.SoftWhite
import com.couplechess.ui.theme.TextSecondary

/**
 * 玩家设置界面
 * 
 * 功能：
 * - 添加/删除玩家（2-4人）
 * - 设置玩家名称、性别、当前阶段
 * - 验证性别规则（必须有异性）
 * - 进入游戏或任务管理
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSetupScreen(
    taskRepository: TaskRepository,
    onNavigateToGame: (List<Player>) -> Unit = {},
    onNavigateToTaskManager: () -> Unit = {},
    onNavigateBack: () -> Unit
) {
    val viewModel: PlayerSetupViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    
    // 渐变背景
    val backgroundGradient = remember {
        Brush.verticalGradient(
            colors = listOf(
                PureBlack,
                BackgroundElevated.copy(alpha = 0.5f),
                PureBlack
            )
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "玩家设置",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    // 任务管理入口
                    IconButton(onClick = onNavigateToTaskManager) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "任务管理",
                            tint = Gold
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
                .background(backgroundGradient)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 玩家列表
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 说明卡片
                    item {
                        InstructionCard()
                    }
                    
                    // 玩家卡片列表
                    itemsIndexed(
                        items = state.playerForms,
                        key = { index, _ -> "player_$index" }
                    ) { index, playerForm ->
                        PlayerCard(
                            index = index,
                            playerForm = playerForm,
                            onNameChange = { viewModel.updatePlayerName(index, it) },
                            onGenderChange = { viewModel.updatePlayerGender(index, it) },
                            onLevelChange = { viewModel.updatePlayerLevel(index, it) },
                            onRemove = if (state.canRemovePlayer) {
                                { viewModel.removePlayer(index) }
                            } else null,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    // 添加玩家按钮
                    item {
                        AddPlayerButton(
                            onClick = { viewModel.addPlayer() },
                            enabled = state.canAddPlayer
                        )
                    }
                    
                    // 底部间距
                    item {
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
                
                // 底部操作栏
                BottomActionBar(
                    isValid = state.isValid,
                    validationError = state.validationError,
                    onStartGame = {
                        viewModel.getValidatedPlayers()?.let { players ->
                            onNavigateToGame(players)
                        }
                    }
                )
            }
        }
    }
}

/**
 * 说明卡片
 */
@Composable
private fun InstructionCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = BackgroundCard.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "游戏规则",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Gold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "• 支持 2-4 名玩家参与\n" +
                       "• 必须同时有男性和女性玩家\n" +
                       "• 每位玩家可独立设置当前阶段 (L1-L5)\n" +
                       "• 任务仅在异性玩家间触发",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
            )
        }
    }
}

/**
 * 底部操作栏
 */
@Composable
private fun BottomActionBar(
    isValid: Boolean,
    validationError: String?,
    onStartGame: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(
            containerColor = BackgroundElevated
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 验证错误提示
            AnimatedVisibility(
                visible = validationError != null,
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .background(
                            color = ButtonDanger.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = ButtonDanger,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = validationError ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = ButtonDanger,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
            
            // 开始游戏按钮
            Button(
                onClick = onStartGame,
                enabled = isValid,
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
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "开始游戏",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}
