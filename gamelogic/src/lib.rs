//! Couple Chess Game Logic Library
//!
//! Rust core for the Android app. Exposes 3 JNI functions:
//! - `createGame` - Initialize session, returns handle
//! - `dispatchAction` - Process action, returns JSON result  
//! - `destroyGame` - Free session memory

mod game;
mod models;

pub use game::GameSession;
pub use models::*;

use jni::objects::{JClass, JString};
use jni::sys::jlong;
use jni::JNIEnv;

/// Convert a Rust GameSession pointer to a JNI handle (jlong).
fn session_to_handle(session: Box<GameSession>) -> jlong {
    Box::into_raw(session) as jlong
}

/// Recover a mutable reference to GameSession from a JNI handle.
///
/// # Safety
/// - `handle` must be a valid pointer returned by `session_to_handle`.
/// - The session must not have been destroyed.
unsafe fn handle_to_session<'a>(handle: jlong) -> &'a mut GameSession {
    &mut *(handle as *mut GameSession)
}

/// Create a new game session.
///
/// # Arguments
/// * `config_json` - JSON string matching `GameConfig` schema
///
/// # Returns
/// Session handle (pointer as jlong), or 0 on error.
#[no_mangle]
pub extern "system" fn Java_com_couplechess_data_bridge_GameBridge_createGame<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    config_json: JString<'local>,
) -> jlong {
    // Extract Java string
    let config_str: String = match env.get_string(&config_json) {
        Ok(s) => s.into(),
        Err(e) => {
            let _ = env.throw_new(
                "java/lang/IllegalArgumentException",
                format!("Invalid config string: {e}"),
            );
            return 0;
        }
    };

    // Parse JSON config
    let config: GameConfig = match serde_json::from_str(&config_str) {
        Ok(c) => c,
        Err(e) => {
            let _ = env.throw_new(
                "java/lang/IllegalArgumentException",
                format!("Invalid config JSON: {e}"),
            );
            return 0;
        }
    };

    // Create session and convert to handle
    let session = Box::new(GameSession::new(config));
    session_to_handle(session)
}

/// Dispatch an action to an existing game session.
///
/// # Arguments
/// * `handle` - Session handle from `createGame`
/// * `action_json` - JSON string matching `GameAction` schema
///
/// # Returns
/// JSON string containing `ActionResult`, or error JSON on failure.
#[no_mangle]
pub extern "system" fn Java_com_couplechess_data_bridge_GameBridge_dispatchAction<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    handle: jlong,
    action_json: JString<'local>,
) -> JString<'local> {
    // Validate handle
    if handle == 0 {
        return error_response(&mut env, "Invalid session handle (null)");
    }

    // Extract action JSON
    let action_str: String = match env.get_string(&action_json) {
        Ok(s) => s.into(),
        Err(e) => {
            return error_response(&mut env, &format!("Invalid action string: {e}"));
        }
    };

    // Parse action
    let action: GameAction = match serde_json::from_str(&action_str) {
        Ok(a) => a,
        Err(e) => {
            return error_response(&mut env, &format!("Invalid action JSON: {e}"));
        }
    };

    // SAFETY: handle was returned by createGame and not yet destroyed
    let session = unsafe { handle_to_session(handle) };

    // Dispatch and serialize result
    let result = session.dispatch(action);
    let result_json = match serde_json::to_string(&result) {
        Ok(json) => json,
        Err(e) => {
            return error_response(&mut env, &format!("Failed to serialize result: {e}"));
        }
    };

    // Return as Java string
    match env.new_string(&result_json) {
        Ok(s) => s,
        Err(_) => {
            // Last resort - return empty string
            env.new_string("{}").unwrap_or_else(|_| JString::default())
        }
    }
}

/// Destroy a game session and free memory.
///
/// # Safety
/// Must be called exactly once per handle. Using the handle after this is UB.
#[no_mangle]
pub extern "system" fn Java_com_couplechess_data_bridge_GameBridge_destroyGame<'local>(
    _env: JNIEnv<'local>,
    _class: JClass<'local>,
    handle: jlong,
) {
    if handle == 0 {
        return;
    }

    // SAFETY: handle was returned by createGame and not yet destroyed
    unsafe {
        let _ = Box::from_raw(handle as *mut GameSession);
        // Box dropped here, memory freed
    }
}

/// Helper to create an error response JSON string.
fn error_response<'local>(env: &mut JNIEnv<'local>, message: &str) -> JString<'local> {
    // Create a minimal error state to return
    let error_json = format!(
        r#"{{"state":{{"players":[],"board":[],"current_player_index":0,"phase":{{"type":"WaitingForRoll"}},"turn_count":0}},"events":[{{"type":"Error","message":"{}"}}]}}"#,
        message.replace('"', "\\\"")
    );

    env.new_string(&error_json)
        .unwrap_or_else(|_| JString::default())
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::collections::HashMap;

    #[test]
    fn test_session_lifecycle() {
        let config = GameConfig {
            players: vec![
                Player {
                    id: 1,
                    name: "Alice".to_string(),
                    gender: Gender::Female,
                    current_level: TaskLevel::L1,
                    position: 0,
                    finished: false,
                },
                Player {
                    id: 2,
                    name: "Bob".to_string(),
                    gender: Gender::Male,
                    current_level: TaskLevel::L1,
                    position: 0,
                    finished: false,
                },
            ],
            board_size: 36,
            task_ratio: 0.25,
            seed: 42,
            tasks: HashMap::new(),
        };

        // Create session
        let session = Box::new(GameSession::new(config));
        let handle = session_to_handle(session);
        assert_ne!(handle, 0);

        // Use session
        let session_ref = unsafe { handle_to_session(handle) };
        let result = session_ref.dispatch(GameAction::RollDice);
        assert!(!result.events.is_empty());

        // Destroy session
        unsafe {
            let _ = Box::from_raw(handle as *mut GameSession);
        }
    }

    #[test]
    fn test_json_roundtrip() {
        let config = GameConfig {
            players: vec![Player {
                id: 1,
                name: "Test".to_string(),
                gender: Gender::Male,
                current_level: TaskLevel::L2,
                position: 0,
                finished: false,
            }],
            board_size: 20,
            task_ratio: 0.3,
            seed: 123,
            tasks: HashMap::new(),
        };

        // Serialize and deserialize
        let json = serde_json::to_string(&config).unwrap();
        let parsed: GameConfig = serde_json::from_str(&json).unwrap();

        assert_eq!(parsed.players.len(), 1);
        assert_eq!(parsed.board_size, 20);
    }
}
