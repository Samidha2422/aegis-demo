# API Changelog (CHANGELOG_API.md)

## 2026-10-29: Ephemeral uploaded media

- Media uploads return a `/media/{uuid}` URL for validated bytes kept in memory, capped at 100 MiB total; full storage returns 507. Reset or server restart removes them.
- GET returns the validated MIME type with `Cache-Control: no-store`. URLs are bearer capabilities: anyone with the URL can access the media.
- Engine errors preserve the upload with an unknown, unscored safety state; blocked senders remain held.

## 2026-10-29: Engine media integration

- Forward voice uploads to Adapt's multipart `/v1/analyze/audio` endpoint; preserve
  masked transcripts, module details, secondary actions, degradation and provenance.
- `POST /chats/{id}/media` and `POST /recordings` accept an optional `simulated`
  boolean multipart field. Missing audio inference is unscored.
- `POST /recordings` accepts multipart `file`, optional `chat_id` and `sender_id`,
  and returns a SafeMessage. Media retention is updated by the ephemeral media entry above.
- The engine OpenAPI snapshot includes the new audio endpoint and synthetic context.

All changes, extensions, or adjustments between contracts/engine-openapi.json and docs/API_CHAT.md are tracked here per Operating Rule R4.

---

### 2026-10-29: 100% Feature Parity Completion (Voice Notes, CSV Import, Slang Loop)
- **Voice Notes in Chat Composer**: Enabled `kind = "voice"` / `"audio"` in `POST /chats/{id}/media`. Added in-app recording UI with M3 Expressive `LinearWavyProgressIndicator` timer and mic toggle in `ComposerFloatingToolbar.kt`.
- **Call Log CSV Import**: Connected `POST /calls/log/import` in `ChatApiService` and `CallsScreen.kt`. Added device CSV file picker and one-tap bundled 40-call overnight spike loader.
- **Adaptive Slang Intelligence (F10 / Scenario 8)**: Added `confirmSlangTerm` to `ChatRepository` and interactive zero-shot community slang adaptation card to `DemoDirectorScreen.kt` demonstrating pre- vs. post-confirmation engine scoring for emerging code-words.

### 2026-10-29: Call Simulation Audio Transcript / Voicemail Support
- Added optional `transcript: Option<String>` to `SimulateCallRequest` (`POST /calls/simulate`).
- When present, `simulate_call` populates `EngineMessage` and `voicemail: true`, routing audio transcript text to the live Adapt engine text analyzer while fusing with behavioral timing/frequency.
- Added optional `transcript: Option<String>` to `CallRecord` in both chat-server and Android models.

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

## 2026-10-29: Actual Adapt integration (authoritative)

The earlier event/nudge routes above were provisional and did not match Adapt.
The copied contracts/engine-openapi.json now comes from Adapt/docs/openapi.json;
current Rust source was also inspected (including image multipart and safe CSV import).
Chat now uses X-API-Key, UUID event IDs, structured channel, message/media/call
payloads and POST /v1/analyze. Drafts use that same endpoint with draft=true.
Image multipart is POST /v1/analyze/image with request JSON and image bytes.

Chat defaults move from port 8080 to 8081, leaving Adapt's default 8080 available.
Android defaults to live at http://10.0.2.2:8081/; explicit replay remains available.

Add GET /engine/status: {health:{status,version,mock,degraded},models:[{name,version,
hash_verified,backend,degraded}]}; upstream failures return 502/503 with error/detail.
Add sender_id (default meera) to POST messages and multipart media. The selected
persona supplies it; a non-Meera sender must match the chat peer. POST /nudge now
requires chat_id and accepts sender_id along with draft. It returns simulated and
degraded as well as the existing fields. No local keyword moderation fallback.
SafetyObject retains numeric signals and adds modules (typed status/backend/score/
label/confidence), model_versions and degraded. EngineUnavailable verdicts have
tier unknown. A simulated engine result stays simulated regardless of action.

Block/unblock/mute forward RefusalRequest in the refused sender -> recipient direction;
failed operations return errors. Reports require message_id containing an engine UUID.
Comments and simulated calls use engine scores; caller/account names do not assign
severity. GET calls/log no longer auto-seeds verdicts. Call simulation inputs retain
simulated=true. CSV import forwards the engine safe-import contract with per-row
outcomes. Recording uploads return 501 roadmap rather than invented transcripts.
Evidence export proxies actual Adapt JSON/HTML. Chat reset leaves engine history intact.

## 2026-10-29: Refusal actor direction correction

POST /users/{id}/block, /unblock and /mute accept an optional JSON body
{"actor_id":"rohan"}. Empty body, {} or omitted actor_id preserves the legacy
Meera default. Actor must be an existing persona; self-targeting, malformed JSON
and unknown actors return 422 without calling the engine or changing local state.
The engine receives sender_id={id}, recipient_id=actor_id, platform=demo-chat.
Local block/mute state is keyed by (actor,target), including reverse-direction
message and image context, calls and comments. Unblock leaves mute intact.
GET /chats accepts optional actor_id (default meera), returns only the selected
persona's canonical Meera conversation for non-Meera actors, and presents the
actual counterpart and that actor's refusal flags. Android sends the selected
actor for all refusal actions and inbox requests. Synthetic persona selection is
still demo identity selection, not production authentication.
