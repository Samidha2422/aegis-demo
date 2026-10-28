use crate::models::{EngineVerdict, SafetyObject};

pub struct ActionPolicy;

impl ActionPolicy {
    pub fn apply(
        verdict: &EngineVerdict,
        kind: &str,
        is_blocked: bool,
        held_count: usize,
    ) -> SafetyObject {
        let state = if is_blocked {
            "Held"
        } else {
            match verdict.recommended_action.as_str() {
                "blur_media" => "Blurred",
                "throttle" if held_count > 0 => "Held",
                "throttle" => "Warned",
                "escalate" | "warn_sender" if kind == "voice" => "TranscriptOnly",
                "escalate" => "Escalated",
                "warn_sender" => "Warned",
                _ if matches!(verdict.tier.as_str(), "high" | "urgent") => "Escalated",
                _ if verdict.tier == "review" => "Warned",
                _ => "Delivered",
            }
        };
        SafetyObject {
            severity: verdict.severity,
            tier: verdict.tier.clone(),
            reasons: verdict.reasons.clone(),
            signals: verdict.signals.clone(),
            recommended_action: verdict.recommended_action.clone(),
            state: state.into(),
            simulated: verdict.simulated,
            tags: if verdict.simulated {
                vec!["SIMULATED".into()]
            } else {
                Vec::new()
            },
            degraded: verdict.degraded.clone(),
            model_versions: verdict.model_versions.clone(),
            modules: verdict.modules.clone(),
        }
    }

    pub fn unscored(reason: &str) -> SafetyObject {
        SafetyObject {
            tier: "unknown".into(),
            reasons: vec![reason.into()],
            state: "EngineUnavailable".into(),
            tags: vec!["UNSCORED".into()],
            degraded: vec!["engine.unavailable".into()],
            ..SafetyObject::default()
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn live_blur_does_not_claim_simulation() {
        let verdict = EngineVerdict {
            severity: 80.0,
            tier: "high".into(),
            recommended_action: "blur_media".into(),
            ..Default::default()
        };
        let result = ActionPolicy::apply(&verdict, "image", false, 0);
        assert_eq!(result.state, "Blurred");
        assert!(!result.simulated);
        assert!(result.tags.is_empty());
    }

    #[test]
    fn simulation_and_degradation_survive_mapping() {
        let verdict = EngineVerdict {
            simulated: true,
            degraded: vec!["text.onnx_missing".into()],
            ..Default::default()
        };
        let result = ActionPolicy::apply(&verdict, "text", false, 0);
        assert!(result.simulated);
        assert_eq!(result.degraded, verdict.degraded);
    }

    #[test]
    fn unavailable_is_unknown_and_blocked_messages_stay_held() {
        assert_eq!(ActionPolicy::unscored("unavailable").tier, "unknown");
        assert_eq!(
            ActionPolicy::apply(&EngineVerdict::default(), "text", true, 0).state,
            "Held"
        );
    }
}
