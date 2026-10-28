# API Changelog (CHANGELOG_API.md)

All changes, extensions, or adjustments between `contracts/engine-openapi.json` and `docs/API_CHAT.md` are tracked here per Operating Rule R4.

---

## [Initial Contract Definitions] - 2026-10-28

### Engine API Contract (`contracts/engine-openapi.json`)
- Event Ingestion: `POST /v1/events` (event envelope supporting `message`, `image`, `audio`, `call_event`, `refusal`).
- Pre-send Nudge: `POST /v1/events/nudge` (supports `draft: true` flag for proactive text checks).
- Evidence Export: `GET /v1/evidence/export?format={json|html}&user_id={id}`.
- Model Health: `GET /v1/health` and `GET /v1/health/models`.
- Lexicon / Slang Feedback: `POST /v1/moderation/lexicon/confirm` (moderator feedback loop).

### Chat Platform API (`docs/API_CHAT.md`)
- Auth: `GET /personas`, `POST /auth/login`.
- Messaging: `GET /chats`, `GET /chats/{id}/messages`, `POST /chats/{id}/messages`, `POST /chats/{id}/media`.
- Realtime: `WS /ws` emitting `message`, `safety_update`, `alert`, `call_event`, `scenario_step`.
- Moderation & Refusal: `POST /users/{id}/block`, `POST /users/{id}/unblock`, `POST /users/{id}/mute`, `POST /reports`.
- Feed & Calls: `GET /feed/posts`, `POST /feed/posts/{id}/comments`, `POST /calls/simulate`, `GET /calls/log`.
- Scenario & Baseline: `GET /demo/scenarios`, `POST /demo/scenarios/{n}/play`, `POST /demo/reset`, `GET /demo/baseline`.
