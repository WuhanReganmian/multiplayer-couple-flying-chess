//! Data models for Couple Chess game logic.
//! These structs must serialize to JSON that matches Kotlin's kotlinx.serialization.

use serde::{Deserialize, Serialize};

// ===== Enums =====

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum Gender {
    Male,
    Female,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum TaskLevel {
    L1,
    L2,
    L3,
    L4,
    L5,
}

impl TaskLevel {
    pub fn from_int(level: i32) -> Self {
        match level {
            1 => TaskLevel::L1,
            2 => TaskLevel::L2,
            3 => TaskLevel::L3,
            4 => TaskLevel::L4,
            5 => TaskLevel::L5,
            _ => TaskLevel::L1,
        }
    }

    pub fn to_int(self) -> i32 {
        match self {
            TaskLevel::L1 => 1,
            TaskLevel::L2 => 2,
            TaskLevel::L3 => 3,
            TaskLevel::L4 => 4,
            TaskLevel::L5 => 5,
        }
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum CellType {
    Normal,
    Task,
    Start,
    Finish,
}

// ===== Core Data Structs =====

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Player {
    pub id: i32,
    pub name: String,
    pub gender: Gender,
    #[serde(default)]
    pub current_level: TaskLevel,
    #[serde(default)]
    pub position: i32,
    #[serde(default)]
    pub finished: bool,
}

impl Default for TaskLevel {
    fn default() -> Self {
        TaskLevel::L1
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Task {
    pub id: String,
    pub level: TaskLevel,
    pub description: String,
    #[serde(default)]
    pub is_custom: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct BoardCell {
    pub index: i32,
    pub cell_type: CellType,
}

// ===== Game Phase (tagged enum for Kotlin sealed class) =====

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(tag = "type")]
pub enum GamePhase {
    WaitingForRoll,
    Moving {
        dice_value: i32,
    },
    TaskTriggered {
        task: Task,
        executor: i32,
        target: i32,
    },
    TaskDecision,
    Retreating {
        steps: i32,
    },
    GameOver {
        winner: i32,
    },
}

impl Default for GamePhase {
    fn default() -> Self {
        GamePhase::WaitingForRoll
    }
}

// ===== Full Game State =====

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct GameState {
    pub players: Vec<Player>,
    pub board: Vec<BoardCell>,
    #[serde(default)]
    pub current_player_index: i32,
    #[serde(default)]
    pub phase: GamePhase,
    #[serde(default)]
    pub turn_count: i32,
}

// ===== Game Actions (Kotlin → Rust) =====

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(tag = "type")]
pub enum GameAction {
    RollDice,
    AcceptTask,
    RejectTask,
}

// ===== Game Events (Rust → Kotlin, drives UI animations) =====

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(tag = "type")]
pub enum GameEvent {
    DiceRolled {
        value: i32,
    },
    PlayerMoved {
        player_id: i32,
        from: i32,
        to: i32,
    },
    TaskTriggered {
        task: Task,
        executor_id: i32,
        target_id: i32,
    },
    TaskAccepted,
    TaskRejected {
        retreat_steps: i32,
    },
    PlayerRetreated {
        player_id: i32,
        from: i32,
        to: i32,
    },
    TurnChanged {
        next_player_id: i32,
    },
    GameFinished {
        winner_id: i32,
    },
}

// ===== JNI Response Envelope =====

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ActionResult {
    pub state: GameState,
    pub events: Vec<GameEvent>,
}

// ===== Game Configuration (passed to createGame) =====

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct GameConfig {
    pub players: Vec<Player>,
    #[serde(default = "default_board_size")]
    pub board_size: i32,
    #[serde(default = "default_task_ratio")]
    pub task_ratio: f32,
    #[serde(default = "default_seed")]
    pub seed: i64,
    #[serde(default)]
    pub tasks: std::collections::HashMap<String, Vec<Task>>,
}

fn default_board_size() -> i32 {
    36
}

fn default_task_ratio() -> f32 {
    0.25
}

fn default_seed() -> i64 {
    0 // Will use current time if 0
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_task_level_conversion() {
        assert_eq!(TaskLevel::from_int(1), TaskLevel::L1);
        assert_eq!(TaskLevel::from_int(5), TaskLevel::L5);
        assert_eq!(TaskLevel::L3.to_int(), 3);
    }

    #[test]
    fn test_player_serialization() {
        let player = Player {
            id: 1,
            name: "Alice".to_string(),
            gender: Gender::Female,
            current_level: TaskLevel::L2,
            position: 5,
            finished: false,
        };
        let json = serde_json::to_string(&player).unwrap();
        assert!(json.contains("Alice"));
        assert!(json.contains("Female"));
    }

    #[test]
    fn test_game_phase_serialization() {
        let phase = GamePhase::Moving { dice_value: 4 };
        let json = serde_json::to_string(&phase).unwrap();
        assert!(json.contains("Moving"));
        assert!(json.contains("dice_value"));
    }
}
