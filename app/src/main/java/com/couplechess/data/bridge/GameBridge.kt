package com.couplechess.data.bridge

/**
 * JNI bridge to the Rust game logic library.
 * Only 3 JNI functions are exposed; all data exchange uses JSON strings.
 */
object GameBridge {

    init {
        System.loadLibrary("couple_chess")
    }

    /**
     * Create a new game session with the given configuration JSON.
     * @param configJson JSON-serialized [com.couplechess.data.model.GameConfig]
     * @return Opaque session handle (pointer as Long); never 0 on success
     */
    external fun createGame(configJson: String): Long

    /**
     * Dispatch a player action to an existing game session.
     * @param handle Session handle returned by [createGame]
     * @param actionJson JSON-serialized [com.couplechess.data.model.GameAction]
     * @return JSON-serialized [com.couplechess.data.model.ActionResult]
     */
    external fun dispatchAction(handle: Long, actionJson: String): String

    /**
     * Destroy a game session and free Rust-side memory.
     * Must be called exactly once per handle when the game is finished.
     * @param handle Session handle returned by [createGame]
     */
    external fun destroyGame(handle: Long)
}
