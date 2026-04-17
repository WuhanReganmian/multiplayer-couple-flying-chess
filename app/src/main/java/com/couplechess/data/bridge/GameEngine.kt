package com.couplechess.data.bridge

import com.couplechess.data.model.ActionResult
import com.couplechess.data.model.GameAction
import com.couplechess.data.model.GameConfig

/**
 * Abstraction over game logic execution engine.
 * Allows dependency injection and mock implementations for testing.
 *
 * The default implementation uses JNI to communicate with the Rust engine.
 * Tests can provide a mock [GameEngine] that returns predefined results
 * without requiring native library loading.
 */
interface GameEngine {

    /**
     * Create a new game session.
     * @param configJson Serialized [GameConfig]
     * @return Session handle (pointer as Long); 0 on failure
     */
    fun createGame(configJson: String): Long

    /**
     * Dispatch a player action to an existing game session.
     * @param handle Session handle from [createGame]
     * @param actionJson Serialized [GameAction]
     * @return [Result.success] with serialized [ActionResult] on success,
     *         [Result.failure] with error message on failure
     */
    fun dispatchAction(handle: Long, actionJson: String): Result<String>

    /**
     * Destroy a game session and free native memory.
     * Must be called exactly once per handle.
     * @param handle Session handle from [createGame]
     */
    fun destroyGame(handle: Long)
}

/**
 * JNI-based [GameEngine] implementation.
 * Uses the native Rust library via JNI calls.
 */
object JvmGameEngine : GameEngine {

    init {
        System.loadLibrary("couple_chess")
    }

    override fun createGame(configJson: String): Long {
        return GameBridge.createGame(configJson)
    }

    override fun dispatchAction(handle: Long, actionJson: String): Result<String> {
        return try {
            val result = GameBridge.dispatchAction(handle, actionJson)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun destroyGame(handle: Long) {
        GameBridge.destroyGame(handle)
    }
}