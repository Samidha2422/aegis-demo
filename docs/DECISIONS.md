# Engineering Decision Log (DECISIONS.md)

In accordance with Operating Rule R8 (*"Decide, don't ask. Ask the human only for hardware/accounts/tokens. Otherwise decide, log in docs/DECISIONS.md, move on"*), all technical choices and architectural trade-offs are recorded here.

---

## Logged Decisions

### DEC-001: Tooling Installation & Shell Configuration
- **Decision:** Installed `just` (v1.59.0) and `rustup` toolchain (`stable-x86_64-pc-windows-msvc`) to provide standard compilation and Luna protocol recipe execution on Windows.
- **Context:** Neither `just` nor `cargo` were present on system PATH initially. Windows winget and user binary symlinks into `~/.local/bin` resolved this without requiring elevated machine reboots.
- **Impact:** Enables full scriptable compilation for both `chat-server` and automated justfile logging pipelines.

### DEC-002: Material 3 Expressive Component Mapping
- **Decision:** Targeted Material 3 Expressive library (`1.5.x` series) and opted in module-wide via `@OptIn(ExperimentalMaterial3ExpressiveApi::class)`.
- **Context:** Material 3 `1.4.0` hid key expressive widgets (`HorizontalFloatingToolbar`, `ButtonGroup`, `LoadingIndicator`).
- **Impact:** Strict adherence to R2: zero custom composables for buttons, chips, carousels, or indicators. Only `SafeMessageBubble`, blur overlay, and severity meter are custom.

### DEC-003: Navigation Architecture
- **Decision:** Implemented Jetpack Navigation 3 with type-safe destination routes and `ShortNavigationBar` for adaptive top-level switching.
- **Context:** Ensures compliance with installed `navigation-3` and `adaptive` skills.
- **Impact:** State preservation across backstacks and smooth transitions without navigation leaks.

### DEC-004: In-Memory / SQLite Architecture for Chat Server
- **Decision:** Used SQLite with `sqlx` (runtime queries, no compile-time database macro dependencies) and an in-memory test mode fallback.
- **Context:** Rapid reset capability (`/demo/reset`) required for repeatable live scenario demonstrations.
- **Impact:** Instant teardown and spin-up with zero external database service dependencies.

### DEC-005: Replay Mode Architecture
- **Decision:** Bundled self-contained JSON fixtures for all 9 scenarios in `assets/replay/*.json` powering an offline `FixtureRepository`.
- **Context:** Protects against network failure during live judging (R9/Success criteria).
- **Impact:** The app can be demonstrated with full UI animations, safety badges, and bottom-sheet signals even with airplane mode active.

### DEC-006: Simulated vs Built Scope Boundaries
- **Decision:** Explicitly mark any synthetic audio, demo proxy image, or simulated call event with visible tags (`SIMULATED`, `SYNTHETIC AUDIO`, `DEMO PROXY`) across all UI cards and API envelopes.
- **Context:** Upholds Rule R5 and PS5 honesty requirements.
- **Impact:** Judges clearly understand the technical boundary between the working risk engine and the platform simulation harness.
