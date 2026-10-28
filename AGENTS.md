# Operating Guidelines for Agents & Teammates

This repository powers the demo layer of **Hacksploit 2026 PS5 (Harassment Risk Engine Showcase)**. All participating agents and contributors must strictly adhere to these operating principles:

---

## 1. Command Execution via `just` & Luna Protocol

**The main conversation thread must never see raw build, test, install, or log output.**
- Never run `gradle`, `./gradlew`, `cargo`, `just`, `adb logcat`, `android-cli` build/install commands, or test runners directly in the main thread.
- Wrap every command in a `just` recipe that logs stdout and stderr to `build-logs/<name>.log` and writes the exit code to `build-logs/<name>.exit`.
- Delegate execution to a **Luna subagent** with this exact prompt template:
  ```
  TASK: Run `just <recipe>` from the repo root. Then read build-logs/<recipe>.log and build-logs/<recipe>.exit.
  DO NOT edit files. DO NOT paste raw logs.
  REPORT (max 15 lines):
  1. RESULT: PASS or FAIL
  2. WHAT WENT RIGHT: one line
  3. WHAT WENT WRONG: per distinct error (max 5): file:line, message in <=15 words, one-line guess at cause
  4. WARNINGS: count + the 2 most important
  5. NEXT STEP: the single most useful fix
  If PASS, stop after line 3.
  ```
- For teammate Gemini agents without subagent tools: redirect all outputs to files and inspect with `tail -n 40 build-logs/<name>.log` and `grep -n "error" -A6 build-logs/<name>.log | head -60`.

---

## 2. Material 3 Expressive Component First (R2)

- Use existing **Material 3 Expressive** components wherever possible. Do not hand-roll custom composables when an M3 Expressive component exists.
- The **only** permitted custom composables are:
  1. `SafeMessageBubble` (composed of Surface/Card and M3 Expressive elements)
  2. The image blur overlay
  3. The severity meter composition (`LinearWavyProgressIndicator` + labels)
- Use `MaterialExpressiveTheme` and `MotionScheme.expressive()`.
- Every UI element must map to a component documented in `docs/UI_SPEC.md`.

---

## 3. Honesty & Privacy Labels (R5, R6)

- Every simulated element must be explicitly labelled in UI and API (`simulated: true`, `SIMULATED`, `SYNTHETIC AUDIO`, `DEMO PROXY IMAGE`).
- The app must never pretend to tap real phone calls or read external private DMs.
- All personas and assets are fictional and synthetic.

---

## 4. Contract-First Development (R4)

- The chat server interfaces with the Harassment Risk Engine through `contracts/engine-openapi.json`.
- The Android app consumes the chat server via `docs/API_CHAT.md`.
- Any changes to either contract must be recorded in `docs/CHANGELOG_API.md`.

---

## 5. Architectural Boundaries & Ownership

- `chat-server/`: Rust (`axum`, `tokio`, `sqlx`, `reqwest`, `serde`, `tower-http`).
- `android-app/`: Kotlin, Jetpack Compose, M3 Expressive, `StateFlow`, tiny service locator.
- `scenarios/`: YAML scripts and synthetic assets.
- `docs/`: Design documents, specifications, and task cards.
