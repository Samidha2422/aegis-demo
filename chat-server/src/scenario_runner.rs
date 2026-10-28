use crate::models::WsEvent;
use chrono::{Duration as ChronoDuration, Utc};
use serde::{Deserialize, Serialize};
use tokio::sync::broadcast;
use tracing::info;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ScenarioStep {
    pub at_ms: u64,
    pub actor: String,
    pub to: String,
    pub kind: String, // "message", "image", "voice", "comment", "call", "block", "reply"
    #[serde(default)]
    pub payload: String,
    #[serde(default)]
    pub asset: Option<String>,
    #[serde(default)]
    pub expect_tier: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ScenarioDefinition {
    pub id: u32,
    pub title: String,
    pub description: String,
    pub steps: Vec<ScenarioStep>,
}

pub struct ScenarioRunner {
    tx: broadcast::Sender<WsEvent>,
}

impl ScenarioRunner {
    pub fn new(tx: broadcast::Sender<WsEvent>) -> Self {
        Self { tx }
    }

    /// Loads a scenario definition from disk or embedded fallback
    pub fn load_scenario(&self, scenario_id: u32) -> Result<ScenarioDefinition, String> {
        let file_path = format!("scenarios/scenario_{}.yaml", scenario_id);
        if let Ok(content) = std::fs::read_to_string(&file_path) {
            serde_yaml::from_str(&content).map_err(|e| format!("YAML parse error: {}", e))
        } else {
            // Self-contained fallback definition for testing
            Ok(self.generate_fallback_scenario(scenario_id))
        }
    }

    /// Plays a scenario with the specified speed compression factor
    pub async fn play(&self, scenario: ScenarioDefinition, speed: u32) {
        let speed_factor = if speed == 0 { 60 } else { speed };
        info!(
            "Starting Scenario {} ('{}') at {}x speed compression",
            scenario.id, scenario.title, speed_factor
        );

        let total_steps = scenario.steps.len();
        let base_start_time = Utc::now() - ChronoDuration::hours(3); // Project 3 simulated hours into past
        let mut prev_at_ms = 0u64;

        for (idx, step) in scenario.steps.into_iter().enumerate() {
            let step_delay_ms = (step.at_ms.saturating_sub(prev_at_ms)) / (speed_factor as u64);
            if step_delay_ms > 0 {
                tokio::time::sleep(std::time::Duration::from_millis(step_delay_ms.min(1000))).await;
            }
            prev_at_ms = step.at_ms;

            let simulated_occurred_at =
                base_start_time + ChronoDuration::milliseconds(step.at_ms as i64);

            // Broadcast scenario step update to UI
            let _ = self.tx.send(WsEvent {
                event: "scenario_step".to_string(),
                payload: serde_json::json!({
                    "scenario_id": scenario.id,
                    "step_index": idx + 1,
                    "total_steps": total_steps,
                    "actor": step.actor,
                    "kind": step.kind,
                    "description": format!("{} sends {} to {}", step.actor, step.kind, step.to),
                    "expect_tier": step.expect_tier.unwrap_or_else(|| "low".to_string()),
                    "occurred_at": simulated_occurred_at
                }),
            });
        }

        info!("Scenario {} playback complete", scenario.id);
    }

    fn generate_fallback_scenario(&self, id: u32) -> ScenarioDefinition {
        let mut steps = Vec::new();
        match id {
            1 => {
                // Scenario 1: Polite stalker (80 polite messages over 3 simulated hours)
                for i in 1..=80 {
                    let at_ms = (i as u64) * (180 * 60 * 1000 / 80);
                    let expect_tier = if i < 30 {
                        "low"
                    } else if i < 60 {
                        "review"
                    } else {
                        "high"
                    };
                    steps.push(ScenarioStep {
                        at_ms,
                        actor: "rohan".to_string(),
                        to: "meera".to_string(),
                        kind: "message".to_string(),
                        payload: format!("Hey Meera, message #{}! Hope your day is wonderful.", i),
                        asset: None,
                        expect_tier: Some(expect_tier.to_string()),
                    });
                }
            }
            _ => {
                steps.push(ScenarioStep {
                    at_ms: 1000,
                    actor: "test_actor".to_string(),
                    to: "meera".to_string(),
                    kind: "message".to_string(),
                    payload: "Test message".to_string(),
                    asset: None,
                    expect_tier: Some("low".to_string()),
                });
            }
        }

        ScenarioDefinition {
            id,
            title: format!("Scenario {}", id),
            description: "Generated scenario definition".to_string(),
            steps,
        }
    }
}
