use crate::models::{EngineVerdict, SafetyObject};

pub struct ActionPolicy;

impl ActionPolicy {
    /// Determines the client safety state and tags based on the engine verdict and interaction context
    pub fn apply(
        verdict: &EngineVerdict,
        kind: &str,
        is_blocked: bool,
        held_count: usize,
    ) -> SafetyObject {
        let mut reasons = verdict.reasons.clone();
        let mut tags = Vec::new();

        // Determine client presentation state
        let state = match verdict.recommended_action.as_str() {
            "blur_media" => {
                tags.push("DEMO PROXY".to_string());
                tags.push("SIMULATED SIGNAL".to_string());
                "Blurred".to_string()
            }
            "throttle" => {
                if is_blocked || held_count > 0 {
                    "Held".to_string()
                } else {
                    "Warned".to_string()
                }
            }
            "escalate" => {
                if kind == "voice" {
                    tags.push("SYNTHETIC AUDIO".to_string());
                    "TranscriptOnly".to_string()
                } else {
                    "Escalated".to_string()
                }
            }
            "warn_sender" => {
                if kind == "voice" {
                    tags.push("SYNTHETIC AUDIO".to_string());
                    "TranscriptOnly".to_string()
                } else {
                    "Warned".to_string()
                }
            }
            _ => {
                if verdict.tier == "high" || verdict.tier == "urgent" {
                    if is_blocked {
                        "Held".to_string()
                    } else {
                        "Escalated".to_string()
                    }
                } else if verdict.tier == "review" {
                    "Warned".to_string()
                } else {
                    "Delivered".to_string()
                }
            }
        };

        if is_blocked && !reasons.contains(&"Contact attempt after user block".to_string()) {
            reasons.push("Contact attempt after user block".to_string());
        }

        let is_simulated = !tags.is_empty();

        SafetyObject {
            severity: verdict.severity,
            tier: verdict.tier.clone(),
            reasons,
            signals: verdict.signals.clone(),
            recommended_action: verdict.recommended_action.clone(),
            state,
            simulated: is_simulated,
            tags,
        }
    }

    /// Default fallback safety object when engine is unreachable
    pub fn fallback_unscored() -> SafetyObject {
        SafetyObject {
            severity: 0.0,
            tier: "low".to_string(),
            reasons: vec!["Safety engine unavailable: delivered unscored".to_string()],
            signals: std::collections::HashMap::new(),
            recommended_action: "log".to_string(),
            state: "EngineUnavailable".to_string(),
            simulated: false,
            tags: vec!["OFFLINE_FALLBACK".to_string()],
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::collections::HashMap;

    #[test]
    fn test_low_verdict_delivers_normally() {
        let verdict = EngineVerdict {
            severity: 10.0,
            tier: "low".to_string(),
            reasons: vec!["Friendly greeting".to_string()],
            signals: HashMap::new(),
            recommended_action: "log".to_string(),
        };
        let safety = ActionPolicy::apply(&verdict, "text", false, 0);
        assert_eq!(safety.state, "Delivered");
        assert_eq!(safety.tier, "low");
        assert!(!safety.simulated);
    }

    #[test]
    fn test_warn_sender_maps_to_warned_state() {
        let verdict = EngineVerdict {
            severity: 45.0,
            tier: "review".to_string(),
            reasons: vec!["Rising contact persistence".to_string()],
            signals: HashMap::new(),
            recommended_action: "warn_sender".to_string(),
        };
        let safety = ActionPolicy::apply(&verdict, "text", false, 0);
        assert_eq!(safety.state, "Warned");
        assert_eq!(safety.tier, "review");
    }

    #[test]
    fn test_blur_media_adds_simulated_tags_and_blurred_state() {
        let verdict = EngineVerdict {
            severity: 85.0,
            tier: "high".to_string(),
            reasons: vec!["Unsolicited explicit media".to_string()],
            signals: HashMap::new(),
            recommended_action: "blur_media".to_string(),
        };
        let safety = ActionPolicy::apply(&verdict, "image", false, 0);
        assert_eq!(safety.state, "Blurred");
        assert!(safety.simulated);
        assert!(safety.tags.contains(&"DEMO PROXY".to_string()));
    }

    #[test]
    fn test_throttle_after_block_maps_to_held_state() {
        let verdict = EngineVerdict {
            severity: 78.0,
            tier: "high".to_string(),
            reasons: vec!["Excessive unreplied messages".to_string()],
            signals: HashMap::new(),
            recommended_action: "throttle".to_string(),
        };
        let safety = ActionPolicy::apply(&verdict, "text", true, 5);
        assert_eq!(safety.state, "Held");
        assert!(safety
            .reasons
            .contains(&"Contact attempt after user block".to_string()));
    }

    #[test]
    fn test_threatening_voice_maps_to_transcript_only() {
        let verdict = EngineVerdict {
            severity: 90.0,
            tier: "urgent".to_string(),
            reasons: vec!["Explicit threatening vocal content".to_string()],
            signals: HashMap::new(),
            recommended_action: "escalate".to_string(),
        };
        let safety = ActionPolicy::apply(&verdict, "voice", false, 0);
        assert_eq!(safety.state, "TranscriptOnly");
        assert!(safety.tags.contains(&"SYNTHETIC AUDIO".to_string()));
    }
}
