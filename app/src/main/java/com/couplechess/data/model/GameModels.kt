package com.couplechess.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ===== 枚举 =====

@Serializable
enum class Gender {
    @SerialName("Male") MALE,
    @SerialName("Female") FEMALE
}

@Serializable
enum class TaskLevel(val displayName: String, val levelInt: Int) {
    @SerialName("L1") L1("破冰", 1),
    @SerialName("L2") L2("暧昧挑逗", 2),
    @SerialName("L3") L3("褪衣相亲", 3),
    @SerialName("L4") L4("边缘取悦", 4),
    @SerialName("L5") L5("实战冲刺", 5);

    companion object {
        fun fromInt(level: Int): TaskLevel = entries.firstOrNull { it.levelInt == level } ?: L1
    }
}

@Serializable
enum class CellType {
    @SerialName("Normal") NORMAL,
    @SerialName("Task") TASK,
    @SerialName("Start") START,
    @SerialName("Finish") FINISH
}

// ===== 核心数据类 =====

@Serializable
data class Player(
    val id: Int,
    val name: String,
    val gender: Gender,
    @SerialName("current_level") val currentLevel: TaskLevel = TaskLevel.L1,
    val position: Int = 0,
    val finished: Boolean = false
)

@Serializable
data class Task(
    val id: String,
    val level: TaskLevel,
    val description: String,
    @SerialName("is_custom") val isCustom: Boolean = false
)

@Serializable
data class BoardCell(
    val index: Int,
    @SerialName("cell_type") val cellType: CellType
)

// ===== 游戏阶段状态 =====

@Serializable
sealed class GamePhase {
    @Serializable
    @SerialName("WaitingForRoll")
    data object WaitingForRoll : GamePhase()

    @Serializable
    @SerialName("Moving")
    data class Moving(@SerialName("dice_value") val diceValue: Int) : GamePhase()

    @Serializable
    @SerialName("TaskTriggered")
    data class TaskTriggered(
        val task: Task,
        val executor: Int,
        val target: Int
    ) : GamePhase()

    @Serializable
    @SerialName("TaskDecision")
    data object TaskDecision : GamePhase()

    @Serializable
    @SerialName("Retreating")
    data class Retreating(val steps: Int) : GamePhase()

    @Serializable
    @SerialName("GameOver")
    data class GameOver(val winner: Int) : GamePhase()
}

// ===== 完整游戏状态 =====

@Serializable
data class GameState(
    val players: List<Player>,
    val board: List<BoardCell>,
    @SerialName("current_player_index") val currentPlayerIndex: Int = 0,
    val phase: GamePhase = GamePhase.WaitingForRoll,
    @SerialName("turn_count") val turnCount: Int = 0
)

// ===== 游戏动作（Kotlin → Rust）=====

@Serializable
sealed class GameAction {
    @Serializable
    @SerialName("RollDice")
    data object RollDice : GameAction()

    @Serializable
    @SerialName("AcceptTask")
    data object AcceptTask : GameAction()

    @Serializable
    @SerialName("RejectTask")
    data object RejectTask : GameAction()
}

// ===== 游戏事件（Rust → Kotlin，驱动 UI 动画）=====

@Serializable
sealed class GameEvent {
    @Serializable
    @SerialName("DiceRolled")
    data class DiceRolled(val value: Int) : GameEvent()

    @Serializable
    @SerialName("PlayerMoved")
    data class PlayerMoved(
        @SerialName("player_id") val playerId: Int,
        val from: Int,
        val to: Int
    ) : GameEvent()

    @Serializable
    @SerialName("TaskTriggered")
    data class TaskTriggered(
        val task: Task,
        @SerialName("executor_id") val executorId: Int,
        @SerialName("target_id") val targetId: Int
    ) : GameEvent()

    @Serializable
    @SerialName("TaskAccepted")
    data object TaskAccepted : GameEvent()

    @Serializable
    @SerialName("TaskRejected")
    data class TaskRejected(@SerialName("retreat_steps") val retreatSteps: Int) : GameEvent()

    @Serializable
    @SerialName("PlayerRetreated")
    data class PlayerRetreated(
        @SerialName("player_id") val playerId: Int,
        val from: Int,
        val to: Int
    ) : GameEvent()

    @Serializable
    @SerialName("TurnChanged")
    data class TurnChanged(@SerialName("next_player_id") val nextPlayerId: Int) : GameEvent()

    @Serializable
    @SerialName("GameFinished")
    data class GameFinished(@SerialName("winner_id") val winnerId: Int) : GameEvent()
}

// ===== JNI 响应信封 =====

@Serializable
data class ActionResult(
    val state: GameState,
    val events: List<GameEvent>
)

// ===== 游戏配置（传给 Rust createGame）=====

@Serializable
data class GameConfig(
    val players: List<Player>,
    @SerialName("board_size") val boardSize: Int = 36,
    @SerialName("task_ratio") val taskRatio: Float = 0.25f,
    val seed: Long = System.currentTimeMillis(),
    val tasks: Map<String, List<Task>> = emptyMap()  // level -> tasks
)

// ===== 玩家创建表单（UI 用）=====

data class PlayerForm(
    val name: String = "",
    val gender: Gender = Gender.MALE,
    val currentLevel: TaskLevel = TaskLevel.L1
)
