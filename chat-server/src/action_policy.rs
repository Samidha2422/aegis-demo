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
        } else if matches!(kind, "voice" | "audio")
            && (verdict.recommended_action == "show_transcript"
                || verdict
                    .secondary_actions
                    .iter()
                    .any(|a| a == "show_transcript"))
        {
            "TranscriptOnly"
        } else {
            match verdict.recommended_action.as_str() {
                "blur_media" if !matches!(kind, "voice" | "audio") => "Blurred",
                "throttle" if held_count > 0 => "Held",
                "throttle" => "Warned",
                "escalate" | "warn_sender" | "blur_media" if matches!(kind, "voice" | "audio") => {
                    "TranscriptOnly"
                }
                "escalate" => "Escalated",
                "warn_sender" => "Warned",
                _ if matches!(verdict.tier.as_str(), "high" | "urgent") => "Escalated",
                _ if verdict.tier == "review" => "Warned",
                _ => "Delivered",
            }
        };
        let mut safety = SafetyObject {
            severity: verdict.severity,
            tier: verdict.tier.clone(),
            reasons: verdict.reasons.clone(),
            signals: verdict.signals.clone(),
            recommended_action: verdict.recommended_action.clone(),
            secondary_actions: verdict.secondary_actions.clone(),
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
        };
        let audio_unavailable = matches!(kind, "voice" | "audio")
            && verdict.modules.get("audio").is_none_or(|signal| {
                signal.backend == "disabled"
                    || matches!(signal.status.as_str(), "roadmap" | "disabled")
            });
        if audio_unavailable && safety.state == "Delivered" {
            safety.tier = "unknown".into();
            safety.state = "EngineUnavailable".into();
            safety.tags.push("UNSCORED".into());
            if !safety
                .degraded
                .iter()
                .any(|reason| reason == "audio.unavailable")
            {
                safety.degraded.push("audio.unavailable".into());
            }
        }
        safety
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
    fn voice_and_audio_protect_playback_and_keep_blocked_held() {
        for kind in ["voice", "audio"] {
            for action in ["warn_sender", "escalate", "blur_media", "show_transcript"] {
                let verdict = EngineVerdict {
                    recommended_action: action.into(),
                    ..Default::default()
                };
                assert_eq!(
                    ActionPolicy::apply(&verdict, kind, false, 0).state,
                    "TranscriptOnly"
                );
                assert_eq!(ActionPolicy::apply(&verdict, kind, true, 0).state, "Held");
            }
        }
    }

    #[test]
    fn secondary_transcript_action_survives_policy_mapping() {
        let verdict = EngineVerdict {
            recommended_action: "throttle".into(),
            secondary_actions: vec!["show_transcript".into()],
            ..Default::default()
        };
        let safety = ActionPolicy::apply(&verdict, "audio", false, 0);
        assert_eq!(safety.state, "TranscriptOnly");
        assert_eq!(safety.secondary_actions, verdict.secondary_actions);
        assert_eq!(
            ActionPolicy::apply(&verdict, "audio", true, 0).state,
            "Held"
        );
    }

    #[test]
    fn disabled_audio_preserves_other_risk_and_block_protections() {
        let mut verdict = EngineVerdict {
            tier: "low".into(),
            recommended_action: "log".into(),
            modules: [(
                "audio".into(),
                crate::models::EngineSignal {
                    score: 0.0,
                    label: "unavailable".into(),
                    status: "roadmap".into(),
                    backend: "disabled".into(),
                    confidence: 0.0,
                    details: Default::default(),
                },
            )]
            .into(),
            ..Default::default()
        };
        let unknown = ActionPolicy::apply(&verdict, "audio", false, 0);
        assert_eq!(
            (unknown.tier.as_str(), unknown.state.as_str()),
            ("unknown", "EngineUnavailable")
        );
        assert_eq!(
            ActionPolicy::apply(&verdict, "audio", true, 0).state,
            "Held"
        );
        verdict.tier = "high".into();
        verdict.severity = 80.0;
        verdict.recommended_action = "escalate".into();
        let protected = ActionPolicy::apply(&verdict, "audio", false, 0);
        assert_eq!(
            (protected.tier.as_str(), protected.state.as_str()),
            ("high", "TranscriptOnly")
        );
    }

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
