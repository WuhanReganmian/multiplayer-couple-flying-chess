package com.couplechess.ui.screens.playersetup

import androidx.lifecycle.ViewModel
import com.couplechess.data.model.Gender
import com.couplechess.data.model.Player
import com.couplechess.data.model.PlayerForm
import com.couplechess.data.model.TaskLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 玩家设置界面状态
 */
data class PlayerSetupState(
    val playerForms: List<PlayerForm> = listOf(
        PlayerForm(name = "玩家1", gender = Gender.MALE, currentLevel = TaskLevel.L1),
        PlayerForm(name = "玩家2", gender = Gender.FEMALE, currentLevel = TaskLevel.L1)
    ),
    val globalLevel: TaskLevel = TaskLevel.L1,
    val validationError: String? = null,
    val isValid: Boolean = true
) {
    val playerCount: Int get() = playerForms.size
    val canAddPlayer: Boolean get() = playerCount < MAX_PLAYERS
    val canRemovePlayer: Boolean get() = playerCount > MIN_PLAYERS
    
    companion object {
        const val MIN_PLAYERS = 2
        const val MAX_PLAYERS = 4
    }
}

/**
 * 玩家设置 ViewModel
 * 
 * 功能：
 * - 管理2-4个玩家的创建表单
 * - 验证性别规则（必须有异性）
 * - 生成最终 Player 列表供游戏使用
 */
class PlayerSetupViewModel : ViewModel() {
    
    private val _state = MutableStateFlow(PlayerSetupState())
    val state: StateFlow<PlayerSetupState> = _state.asStateFlow()
    
    /**
     * 更新玩家名称
     */
    fun updatePlayerName(index: Int, name: String) {
        _state.update { currentState ->
            val updatedForms = currentState.playerForms.toMutableList().apply {
                if (index in indices) {
                    this[index] = this[index].copy(name = name)
                }
            }
            currentState.copy(playerForms = updatedForms).withValidation()
        }
    }
    
    /**
     * 更新玩家性别
     */
    fun updatePlayerGender(index: Int, gender: Gender) {
        _state.update { currentState ->
            val updatedForms = currentState.playerForms.toMutableList().apply {
                if (index in indices) {
                    this[index] = this[index].copy(gender = gender)
                }
            }
            currentState.copy(playerForms = updatedForms).withValidation()
        }
    }
    
    /**
     * 更新全局阶段等级（统一设置，不再单独设置每个玩家）
     */
    fun updateGlobalLevel(level: TaskLevel) {
        _state.update { currentState ->
            currentState.copy(globalLevel = level)
        }
    }
    
    /**
     * 添加玩家（最多4人）
     */
    fun addPlayer() {
        _state.update { currentState ->
            if (!currentState.canAddPlayer) return@update currentState
            
            val newIndex = currentState.playerCount + 1
            // 自动分配性别：交替男女以满足异性要求
            val newGender = if (currentState.playerForms.count { it.gender == Gender.MALE } <=
                currentState.playerForms.count { it.gender == Gender.FEMALE }) {
                Gender.MALE
            } else {
                Gender.FEMALE
            }
            
            val newForm = PlayerForm(
                name = "玩家$newIndex",
                gender = newGender,
                currentLevel = TaskLevel.L1
            )
            
            currentState.copy(
                playerForms = currentState.playerForms + newForm
            ).withValidation()
        }
    }
    
    /**
     * 移除玩家（最少2人）
     */
    fun removePlayer(index: Int) {
        _state.update { currentState ->
            if (!currentState.canRemovePlayer) return@update currentState
            if (index !in currentState.playerForms.indices) return@update currentState
            
            val updatedForms = currentState.playerForms.toMutableList().apply {
                removeAt(index)
            }
            
            currentState.copy(playerForms = updatedForms).withValidation()
        }
    }
    
    /**
     * 验证当前配置并生成 Player 列表
     * @return 如果验证通过返回 Player 列表，否则返回 null
     */
    fun getValidatedPlayers(): List<Player>? {
        val currentState = _state.value
        if (!currentState.isValid) return null
        
        return currentState.playerForms.mapIndexed { index, form ->
            Player(
                id = index,
                name = form.name.ifBlank { "玩家${index + 1}" },
                gender = form.gender,
                currentLevel = _state.value.globalLevel,
                position = 0,
                finished = false
            )
        }
    }
    
    /**
     * 清除验证错误
     */
    fun clearError() {
        _state.update { it.copy(validationError = null) }
    }
    
    /**
     * 扩展函数：验证状态并返回更新后的状态
     */
    private fun PlayerSetupState.withValidation(): PlayerSetupState {
        val maleCount = playerForms.count { it.gender == Gender.MALE }
        val femaleCount = playerForms.count { it.gender == Gender.FEMALE }
        val hasEmptyName = playerForms.any { it.name.isBlank() }
        val hasDuplicateName = playerForms.map { it.name.trim() }
            .filter { it.isNotBlank() }
            .let { names -> names.size != names.distinct().size }
        
        val (isValid, error) = when {
            playerCount < PlayerSetupState.MIN_PLAYERS -> {
                false to "至少需要2名玩家"
            }
            playerCount > PlayerSetupState.MAX_PLAYERS -> {
                false to "最多支持4名玩家"
            }
            maleCount == 0 -> {
                false to "需要至少1名男性玩家"
            }
            femaleCount == 0 -> {
                false to "需要至少1名女性玩家"
            }
            hasEmptyName -> {
                false to "请输入所有玩家的名称"
            }
            hasDuplicateName -> {
                false to "玩家名称不能重复"
            }
            else -> {
                true to null
            }
        }
        
        return copy(isValid = isValid, validationError = error)
    }
}
