# Team Task Matrix & Phase Plan (TASKS.md)

This matrix defines all work packages, ownership, dependencies, and delivery phases across the hackathon lifecycle.

---

## Task Matrix

| Task ID | Title | Owner | Phase | Deliverable / Component | Status |
|---|---|---|---|---|---|
| **D0** | Repo scaffold, `justfile`, docs skeletons, Android project via `android-cli`, theme setup | Lead | P0 | Foundation & Build Harness | **In Progress** |
| **D1** | `docs/API_CHAT.md` + chat-server skeleton (auth, chats, messages, WS, engine client) | Lead | P0–P1 | `chat-server/` core | **In Progress** |
| **D2** | App shell: navigation, persona picker, chats list, repository interfaces, networking | Lead | P1 | `android-app/` foundation | Ready to Start |
| **D3** | Chat screen + `SafeMessageBubble` + safety states + reasons bottom sheet + composer toolbar | Lead | P1 | `android-app/` chat UI | Blocked on D2 |
| **D4** | `ActionPolicy` + block/mute/report + refusal events + throttle/hold logic | Lead | P1 | `chat-server/` policy engine | Blocked on D1 |
| **D5** | Pre-send nudge + Demo Director shell + Settings | Lead | P2 | `android-app/` director | Blocked on D3 |
| **T1** | Scenario YAML for all 9 + scenario runner + `scenario-check` | Teammate (Server) | P1–P2 | `scenarios/*.yaml` | Card Ready |
| **T2** | Demo asset generator scripts + SHA-256 list for engine overrides | Teammate (Assets) | P1 | `scenarios/assets/` | Card Ready |
| **T3** | Replay fixtures (JSON) for all 9 scenarios + `FixtureRepository` | Teammate (App) | P2 | `android-app/replay/` | Card Ready |
| **T4** | Feed screen (comments, collapse, rate-limit banner) | Teammate (App) | P2 | `android-app/` feed UI | Card Ready |
| **T5** | Calls screen: log, simulate call, CSV import, voicemail upload, simulated live call UI | Teammate (App) | P2–P3 | `android-app/` calls UI | Card Ready |
| **T6** | Voice-note recorder (WAV 16 kHz) + player + transcript rendering; image picker + blur overlay | Teammate (App) | P2 | `android-app/` media UI | Card Ready |
| **T7** | Safety Center: alerts, evidence export (SplitButton), resources carousel, model status | Teammate (App) | P2–P3 | `android-app/` safety UI | Card Ready |
| **T8** | Docs consolidation: UI_SPEC, SCENARIOS, DEMO_SCRIPT, RUNBOOK, DEMO_CHECKLIST | Teammate (Docs) | P3 | `docs/` finalization | Card Ready |
| **D9** | Integration, polish, quality gates, bug-bash, final report | Lead | P4 | Release Verification | Pending P3 |
