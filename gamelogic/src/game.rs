//! Core game logic for Couple Chess.
//!
//! Implements:
//! - Board generation with random task cells
//! - Dice rolling
//! - Player movement with wrap-around
//! - Task triggering with opposite-gender target selection
//! - Task acceptance/rejection with retreat logic

use crate::models::*;
use rand::prelude::*;
use std::collections::HashMap;

/// Main game session holding all state.
pub struct GameSession {
    // state is private — external callers must go through dispatch() to get results.
    // Internal code can still read/write via &mut GameSession.
    state: GameState,
    tasks: HashMap<String, Vec<Task>>,
    rng: StdRng,
    // NOTE: Task state is tracked via state.phase (GamePhase::TaskTriggered).
    // No separate pending_task field needed — avoids duplicating task data.
}

impl GameSession {
    /// Create a new game session from configuration.
    ///
    /// If `config.restore_state` is `Some`, player positions are preserved and
    /// the board / turn counter / current player index are restored from the
    /// saved state instead of being initialized from scratch.
    pub fn new(config: GameConfig) -> Self {
        let seed = if config.seed == 0 {
            rand::thread_rng().gen()
        } else {
            config.seed as u64
        };
        let mut rng = StdRng::seed_from_u64(seed);

        let is_restore = config.restore_state.is_some();

        let (board, current_player_index, turn_count) =
            if let Some(ref saved) = config.restore_state {
                // Restore: use saved board, player index, and turn count
                (
                    saved.board.clone(),
                    saved.current_player_index,
                    saved.turn_count,
                )
            } else {
                // New game: generate fresh board
                let board = generate_board(config.board_size, config.task_ratio, &mut rng);
                (board, 0, 0)
            };

        let players: Vec<Player> = if is_restore {
            // Restore: keep player positions and finished flags from config
            config.players
        } else {
            // New game: initialize all players at position 0
            config
                .players
                .into_iter()
                .map(|mut p| {
                    p.position = 0;
                    p.finished = false;
                    p
                })
                .collect()
        };

        let state = GameState {
            players,
            board,
            current_player_index,
            phase: GamePhase::WaitingForRoll,
            turn_count,
        };

        Self {
            state,
            tasks: config.tasks,
            rng,
        }
    }

    /// Dispatch an action and return the result with events.
    pub fn dispatch(&mut self, action: GameAction) -> ActionResult {
        let mut events = Vec::new();

        match action {
            GameAction::RollDice => self.handle_roll_dice(&mut events),
            GameAction::AcceptTask => self.handle_accept_task(&mut events),
            GameAction::RejectTask => self.handle_reject_task(&mut events),
        }

        ActionResult {
            state: self.state.clone(),
            events,
        }
    }

    fn handle_roll_dice(&mut self, events: &mut Vec<GameEvent>) {
        // Only valid in WaitingForRoll phase
        if !matches!(self.state.phase, GamePhase::WaitingForRoll) {
            return;
        }

        let dice_value = self.rng.gen_range(1..=6);
        events.push(GameEvent::DiceRolled { value: dice_value });

        let player_idx = self.state.current_player_index as usize;
        let player = &mut self.state.players[player_idx];
        let from = player.position;
        let board_len = self.state.board.len() as i32;

        // Calculate new position (linear, not circular for finish detection)
        let finish_pos = board_len - 1;
        let to = (from + dice_value).min(finish_pos);

        player.position = to;
        events.push(GameEvent::PlayerMoved {
            player_id: player.id,
            from,
            to,
        });

        // Check for game over (reached finish)
        if to >= finish_pos {
            player.finished = true;
            self.state.phase = GamePhase::GameOver { winner: player.id };
            events.push(GameEvent::GameFinished {
                winner_id: player.id,
            });
            return;
        }

        // Check if landed on task cell
        let cell = &self.state.board[to as usize];
        if cell.cell_type == CellType::Task {
            self.trigger_task(events);
        } else {
            self.advance_turn(events);
        }
    }

    fn trigger_task(&mut self, events: &mut Vec<GameEvent>) {
        let player_idx = self.state.current_player_index as usize;
        let executor = &self.state.players[player_idx];

        // Find opposite-gender targets
        let targets: Vec<&Player> = self
            .state
            .players
            .iter()
            .filter(|p| p.gender != executor.gender && !p.finished && p.id != executor.id)
            .collect();

        if targets.is_empty() {
            // No valid targets, skip task and advance turn
            self.advance_turn(events);
            return;
        }

        // Pick random target
        let target = targets[self.rng.gen_range(0..targets.len())];

        // Pick task based on executor's current level
        let level_key = format!("{:?}", executor.current_level);
        let task = if let Some(tasks) = self.tasks.get(&level_key) {
            if tasks.is_empty() {
                self.create_fallback_task(&executor.current_level)
            } else {
                tasks[self.rng.gen_range(0..tasks.len())].clone()
            }
        } else {
            self.create_fallback_task(&executor.current_level)
        };

        events.push(GameEvent::TaskTriggered {
            task: task.clone(),
            executor_id: executor.id,
            target_id: target.id,
        });

        self.state.phase = GamePhase::TaskTriggered {
            task,
            executor: executor.id,
            target: target.id,
        };
    }

    fn create_fallback_task(&self, level: &TaskLevel) -> Task {
        Task {
            id: format!("fallback_{:?}", level),
            level: *level,
            description: format!("完成一个{:?}级别的互动任务", level),
            is_custom: false,
        }
    }

    fn handle_accept_task(&mut self, events: &mut Vec<GameEvent>) {
        if !matches!(self.state.phase, GamePhase::TaskTriggered { .. }) {
            return;
        }

        events.push(GameEvent::TaskAccepted);
        self.advance_turn(events);
    }

    fn handle_reject_task(&mut self, events: &mut Vec<GameEvent>) {
        if !matches!(self.state.phase, GamePhase::TaskTriggered { .. }) {
            return;
        }

        // Retreat 1-3 steps randomly
        let retreat_steps = self.rng.gen_range(1..=3);
        events.push(GameEvent::TaskRejected { retreat_steps });

        let player_idx = self.state.current_player_index as usize;
        let player = &mut self.state.players[player_idx];
        let from = player.position;
        let to = (from - retreat_steps).max(0);

        player.position = to;
        events.push(GameEvent::PlayerRetreated {
            player_id: player.id,
            from,
            to,
        });

        // Check if new position is also a task cell
        let cell = &self.state.board[to as usize];
        if cell.cell_type == CellType::Task && to > 0 {
            // Trigger another task
            self.trigger_task(events);
        } else {
            self.advance_turn(events);
        }
    }

    fn advance_turn(&mut self, events: &mut Vec<GameEvent>) {
        self.state.turn_count += 1;

        // Find next non-finished player
        let total = self.state.players.len();
        let mut next_idx = (self.state.current_player_index as usize + 1) % total;
        let start_idx = next_idx;

        loop {
            if !self.state.players[next_idx].finished {
                break;
            }
            next_idx = (next_idx + 1) % total;
            if next_idx == start_idx {
                // All players finished - should not happen normally
                break;
            }
        }

        self.state.current_player_index = next_idx as i32;
        self.state.phase = GamePhase::WaitingForRoll;

        events.push(GameEvent::TurnChanged {
            next_player_id: self.state.players[next_idx].id,
        });
    }
}

/// Generate a board where every internal cell is a Task cell.
/// Only the first cell (Start) and last cell (Finish) are special.
fn generate_board(size: i32, task_ratio: f32, rng: &mut StdRng) -> Vec<BoardCell> {
    let size = size as usize;
    (0..size)
        .map(|i| {
            let cell_type = if i == 0 {
                CellType::Start
            } else if i == size - 1 {
                CellType::Finish
            } else {
                // Use the provided task_ratio to decide between Task and Normal cells
                if rng.gen_bool(task_ratio as f64) {
                    CellType::Task
                } else {
                    CellType::Normal
                }
            };
            BoardCell {
                index: i as i32,
                cell_type,
            }
        })
        .collect()
}

#[cfg(test)]
mod tests {
    use super::*;

    fn create_test_config() -> GameConfig {
        GameConfig {
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
            seed: 12345,
            tasks: HashMap::new(),
            restore_state: None,
        }
    }

    #[test]
    fn test_board_generation() {
        let mut rng = StdRng::seed_from_u64(42);
        let board = generate_board(36, 0.25, &mut rng);

        assert_eq!(board.len(), 36);
        assert_eq!(board[0].cell_type, CellType::Start);
        assert_eq!(board[35].cell_type, CellType::Finish);

        // Internal cells should be a mix of Task and Normal according to task_ratio
        let internal: Vec<&BoardCell> = board[1..35].iter().collect();
        let task_count = internal
            .iter()
            .filter(|c| c.cell_type == CellType::Task)
            .count();
        let normal_count = internal
            .iter()
            .filter(|c| c.cell_type == CellType::Normal)
            .count();
        // Ensure counts sum to internal cells
        assert_eq!(task_count + normal_count, internal.len());
        // Expected number of Task cells based on ratio (0.25) with tolerance +/-2
        let expected_tasks = ((36 - 2) as f32 * 0.25).round() as usize;
        let tolerance = 2usize;
        assert!(
            (task_count as isize - expected_tasks as isize).abs() as usize <= tolerance,
            "Task count {} not within tolerance of expected {}",
            task_count,
            expected_tasks
        );
    }

    #[test]
    fn test_game_session_creation() {
        let config = create_test_config();
        let session = GameSession::new(config);

        assert_eq!(session.state.players.len(), 2);
        assert_eq!(session.state.board.len(), 36);
        assert!(matches!(session.state.phase, GamePhase::WaitingForRoll));
    }

    #[test]
    fn test_roll_dice_moves_player() {
        let config = create_test_config();
        let mut session = GameSession::new(config);

        let result = session.dispatch(GameAction::RollDice);

        // Should have DiceRolled and PlayerMoved events at minimum
        assert!(result
            .events
            .iter()
            .any(|e| matches!(e, GameEvent::DiceRolled { .. })));
        assert!(result
            .events
            .iter()
            .any(|e| matches!(e, GameEvent::PlayerMoved { .. })));
    }

    #[test]
    fn test_task_rejection_causes_retreat() {
        let config = create_test_config();
        let mut session = GameSession::new(config);

        // Force a task trigger scenario by manually setting phase
        session.state.phase = GamePhase::TaskTriggered {
            task: Task {
                id: "test".to_string(),
                level: TaskLevel::L1,
                description: "Test task".to_string(),
                is_custom: false,
            },
            executor: 1,
            target: 2,
        };
        session.state.players[0].position = 10;

        let result = session.dispatch(GameAction::RejectTask);

        assert!(result
            .events
            .iter()
            .any(|e| matches!(e, GameEvent::TaskRejected { .. })));
        assert!(result
            .events
            .iter()
            .any(|e| matches!(e, GameEvent::PlayerRetreated { .. })));
        // Player should have retreated
        assert!(result.state.players[0].position < 10);
    }
}
