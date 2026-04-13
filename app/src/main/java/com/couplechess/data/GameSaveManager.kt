package com.couplechess.data

import android.content.Context
import com.couplechess.data.model.GameState
import com.couplechess.data.model.Player
import com.couplechess.data.model.Task
import com.couplechess.data.model.TaskLevel
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 棋局存档管理器
 *
 * 使用 SharedPreferences + kotlinx.serialization 持久化完整棋局状态。
 * 支持 save / load / clear / hasSave 四个操作。
 */
class GameSaveManager(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /** 保存当前棋局快照 */
    fun save(snapshot: SavedGameSnapshot) {
        val encoded = json.encodeToString(snapshot)
        prefs.edit().putString(KEY_SNAPSHOT, encoded).apply()
    }

    /** 加载存档，无存档时返回 null */
    fun load(): SavedGameSnapshot? {
        val raw = prefs.getString(KEY_SNAPSHOT, null) ?: return null
        return try {
            json.decodeFromString<SavedGameSnapshot>(raw)
        } catch (_: Exception) {
            // 格式损坏则清除
            clear()
            null
        }
    }

    /** 清除存档 */
    fun clear() {
        prefs.edit().remove(KEY_SNAPSHOT).apply()
    }

    /** 是否存在存档 */
    fun hasSave(): Boolean = prefs.contains(KEY_SNAPSHOT)

    companion object {
        private const val PREFS_NAME = "couple_chess_save"
        private const val KEY_SNAPSHOT = "game_snapshot"
    }
}

/**
 * 完整棋局快照，包含恢复游戏所需的一切数据。
 */
@Serializable
data class SavedGameSnapshot(
    val gameState: GameState,
    val players: List<Player>,
    val tasks: Map<String, List<Task>>,
    val savedAtMillis: Long = System.currentTimeMillis()
)
