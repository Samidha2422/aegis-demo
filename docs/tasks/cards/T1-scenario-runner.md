# T1: Scenario YAML Scripts & Automated Scenario Runner

## 0. Your role in one paragraph
You are the Server Engineer teammate responsible for authoring the 9 behavioral scenario YAML definitions in `scenarios/` and implementing the automated `ScenarioRunner` within `chat-server/`. Your code reads the YAML files, simulates compressed-time playback against the platform, preserves realistic simulated timestamps (`occurred_at`) for the engine, broadcasts `scenario_step` WebSocket events, and implements the `just scenario-check` self-check recipe.

## 1. Project context
The demo showcases the Harassment Risk Engine (PS5: Hacksploit 2026). The chat server relays chat, calls the engine, and applies platform actions. Everything simulated must carry `simulated: true` and visible labels. No real personal data or abusive text may be used.

## 2. Repo map and your boundary
- **You may edit:** `scenarios/*.yaml`, `chat-server/src/scenario_runner.rs`, `chat-server/src/bin/scenario_check.rs`.
- **You must NOT edit:** `android-app/`, `docs/API_CHAT.md`, `chat-server/src/action_policy.rs`.

## 3. What already exists
- Contract: `docs/API_CHAT.md`
- Engine contract / mock shapes:
  ```json
  {
    "event_id": "evt_123",
    "kind": "message",
    "occurred_at": "2026-10-28T09:00:00Z",
    "sender_id": "rohan",
    "recipient_id": "meera",
    "platform": "demo-chat",
    "channel": "dm",
    "context": {
      "is_contact": false,
      "recipient_blocked_sender": false,
      "stop_requested": false,
      "sender_account_age_days": 180
    },
    "content": { "text": "Hi Meera" }
  }
  ```

## 4. Exact deliverables
1. `scenarios/scenario_1_polite_stalker.yaml` (80 messages over 3 simulated hours, block at ~30)
2. `scenarios/scenario_2_pile_on.yaml` (10 bot accounts commenting on post in 60s)
3. `scenarios/scenario_3_cyberflashing.yaml` (stranger_77 sending proxy image)
4. `scenarios/scenario_4_screenshot_meme.yaml` (rohan sending meme with abusive text)
5. `scenarios/scenario_5_voice_note.yaml` (audio clip with synthetic threat)
6. `scenarios/scenario_6_call_log.yaml` (40 overnight calls from unknown_number)
7. `scenarios/scenario_7_healthy_couple.yaml` (ishaan & diya mutual high volume)
8. `scenarios/scenario_8_slang_update.yaml` (mod_asha confirms "vexling", subsequent catch)
9. `scenarios/scenario_9_evidence_export.yaml` (export timeline generation)
10. `chat-server/src/scenario_runner.rs` (runner struct with `play(scenario_id, speed_factor)`)
11. `just scenario-check` recipe running `scenario_check.rs`.

## 5. Step-by-step instructions
1. Define YAML schema: `step`, `at_ms`, `actor`, `to`, `kind` (`message|image|voice|comment|call|block|reply`), `payload`, `expect_tier`.
2. Implement `ScenarioRunner`: load YAML via `serde_yaml`. Calculate real sleep delay: `step.at_ms / speed_factor`.
3. Dispatch events to chat server database and forward to engine with simulated `occurred_at`.
4. Emit WebSocket `scenario_step` progress event on each step.
5. In `scenario_check.rs`, play each scenario and assert that expected tiers match engine responses.

## 6. Behavior rules and edge cases
- Speed factor defaults to `60` (1 real second = 60 simulated seconds).
- Never send real current clock time to the engine during scenario playback; send the scripted `occurred_at` timestamp.
- If engine returns an unexpected tier during check, log distinct mismatch: `Step X: expected High, got Review`.

## 7. Tests to write
- `test_scenario_yaml_parsing`: verify all 9 YAML files parse cleanly without syntax error.
- `test_speed_compression_math`: verify timestamp projection math at 1x, 10x, and 60x.

## 8. Commands
Always redirect output:
```bash
just scenario-check > build-logs/scenario-check.log 2>&1
tail -n 40 build-logs/scenario-check.log
grep -n "error" -A6 build-logs/scenario-check.log | head -60
```

## 9. Definition of done
- [ ] All 9 YAML files exist and validate against schema.
- [ ] `ScenarioRunner` plays scenarios with configurable speed.
- [ ] `just scenario-check` passes with 0 failures.

## 10. Do NOT
- Do NOT use `unwrap()` outside test cases.
- Do NOT modify app or core platform API contracts.

## 11. How to report back
Provide a 10-line summary stating: RESULT (PASS/FAIL), total steps per scenario, execution time at 60x, and verification status.

## 12. If blocked
Write `BLOCKED.md` with error details and stop.
