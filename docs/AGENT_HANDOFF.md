# Aegis Demo & Adapt Harassment Risk Engine - Agent Handoff Guide

> **Audience**: Codex / Incoming autonomous engineering agents.  
> **Purpose**: Complete context, system architecture, daemon lifecycle, debugging protocols, and operational workflows for the Aegis Harassment Risk Engine showcase.

---

## 1. System Architecture & Topology

```
+-----------------------------------------------------------------------------------+
| PHYSICAL ANDROID DEVICE (or Emulator)                                            |
| Jetpack Compose + Material 3 Expressive                                           |
| Configured API Base: http://127.0.0.1:8081/ (via USB `adb reverse tcp:8081`)     |
+------------------------------------------+----------------------------------------+
                                           | HTTP / WebSocket
                                           v
+-----------------------------------------------------------------------------------+
| AEGIS CHAT SERVER (`chat-server/`)                                                |
| Rust / Axum / Tokio / Port 8081                                                   |
| - Manages chat threads, mock contacts, calls, media, and WebSocket event bus      |
| - Evaluates incoming messages & media against Adapt Engine                        |
+------------------------------------------+----------------------------------------+
                                           | HTTP `POST /v1/events` or `/v1/analyze`
                                           | Header `X-API-Key: demo-client-key`
                                           v
+-----------------------------------------------------------------------------------+
| ADAPT HARASSMENT RISK ENGINE (`../Adapt`)                                         |
| Rust / Axum / ONNX Runtime / Port 8080                                            |
| Runs genuine pretrained ONNX models with zero heuristics when models are loaded    |
+-----------------------------------------------------------------------------------+
```

---

## 2. Model Inventory & Capabilities (`../Adapt/models`)

All genuine ONNX models are hosted in `../Adapt/models`:

1. **Text Harassment Classifier** (`text_fine_tuned.onnx` - 1.11 GB, `tokenizer.json` - 17.1 MB):
   - Fine-tuned XLM-RoBERTa evaluating 11 distinct harassment categories (`toxicity`, `threat`, `insult`, `severe_toxicity`, `identity_attack`, `sexual_harassment`, `sexual_explicit`, `coercion_sextortion`, `stalking_cue`, `doxxing_cue`, `obscene`).
2. **Message Behavior Ensemble** (`behavior_msg.onnx` - 35.5 KB, `features.json`):
   - GBDT tree ensemble evaluating 47 historical interaction features (burst rates, night activity ratio, unreplied streaks, active days streak, sender account age).
   - **Key Demo Trait**: Reciprocal chat history and contact status (`is_contact: true`) caps contextual risk, allowing playful banter between friends while penalizing identical language from strangers (`is_contact: false`).
3. **Call Behavior Ensemble** (`behavior_call.onnx` - 39.6 KB):
   - GBDT tree ensemble evaluating 8 call interaction features (`unanswered_calls`, `night_ratio`, `after_refusal`, `frequency`).
4. **Visual NSFW & Deepfake Classifiers** (`nsfw.onnx` & `deepfake.onnx`):
   - Vision Transformers (ViT) classifying explicit and synthetic manipulated imagery.
   - **Intimate Media Rule**: $(\text{Deepfake} \ge 0.70 \land \text{NSFW} \ge 0.50) \implies \text{synthetic\_intimate: true}$ (elevates threat severity $\ge 86$ and sets `action: "blur_media"`).
5. **Multi-line OCR Pipeline** (`ocr_detector.onnx` - 4.75 MB DBNet + `ocr_recognizer.onnx` + `ocr_vocab.json`):
   - Locates and extracts text regions across full screenshots, automatically routing extracted text into the text harassment model.
6. **Audio Transcription** (`whisper_encoder.onnx` - 32.9 MB + `whisper_decoder.onnx` - 118.4 MB):
   - OpenAI Whisper-tiny.en speech-to-text pipeline for voice notes and audio voicemails.

---

## 3. How to Launch & Run Services

Always use the prepared scripts to prevent PowerShell variable interpolation issues.

### A. Start Adapt Engine (Port 8080)
From the repo root:
```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/run-engine.ps1
```
*Script contents (`scripts/run-engine.ps1`)*:
```powershell
Set-Location 'C:\Users\Vibhav\Documents\Projects\Adapt'
$env:HRE_SALT = 'replace-with-at-least-32-random-characters-secure-salt-1234'
$env:HRE_API_KEYS = 'demo-client-key'
$env:HRE_MODERATOR_TOKENS = 'demo-moderator-token'
$env:HRE_MODELS_DIR = 'models'
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/just.ps1 run
```
**Verify Adapt Health**:
```powershell
curl -H "X-API-Key: demo-client-key" http://127.0.0.1:8080/v1/health
# Expected: {"status":"ok","version":"0.1.0","mock":false,"degraded":[]}
```

### B. Start Aegis Chat Server (Port 8081)
From the repo root:
```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/run-chat-server.ps1
```
*Script contents (`scripts/run-chat-server.ps1`)*:
```powershell
Set-Location 'C:\Users\Vibhav\Documents\Projects\aegis-demo'
$env:AEGIS_ENGINE_URL = 'http://127.0.0.1:8080'
$env:AEGIS_ENGINE_API_KEY = 'demo-client-key'
$env:PORT = '8081'
cargo run --manifest-path chat-server/Cargo.toml --bin chat-server
```
**Verify Chat Server**:
```powershell
curl http://127.0.0.1:8081/chats
# Expected: JSON list of 5 seed chats (chat_rohan, chat_ishaan, chat_diya, chat_stranger_77, chat_mod_asha)
```

---

## 4. Physical Android Phone Protocol

When testing or running on the physical phone over USB:

1. **Reverse ADB Port Forwarding**:
   ```powershell
   powershell -File scripts/run-logged.ps1 -Recipe app-reverse
   # Or via just: just app-reverse
   ```
   *Required every time the phone is re-plugged or ADB server restarts.*
2. **Launch App**:
   ```powershell
   powershell -File scripts/run-logged.ps1 -Recipe app-smoke
   # Or via just: just app-smoke
   ```
3. **Build & Install APK**:
   ```powershell
   powershell -File scripts/run-logged.ps1 -Recipe app-build
   powershell -File scripts/run-logged.ps1 -Recipe app-install
   ```

---

## 5. Strict Operating Rules (AGENTS.md)

1. **Luna Protocol for Command Execution**:
   - The main conversation thread must **NEVER** see raw `cargo`, `gradle`, `./gradlew`, `adb logcat`, or `just` terminal outputs.
   - Commands must execute via `just <recipe>` or `powershell -File scripts/run-logged.ps1 -Recipe <recipe>`.
   - Logs redirect to `build-logs/<recipe>.log` and exit status to `build-logs/<recipe>.exit`.
   - If using a Luna subagent, prompt with:
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
2. **Material 3 Expressive Component First (Rule R2)**:
   - Permitted custom composables ONLY:
     1. `SafeMessageBubble` (composed of Surface/Card and M3 Expressive elements)
     2. Image blur overlay (`BlurOverlay`)
     3. Severity meter composition (`SeverityMeter`)
   - All other UI must use standard Material 3 Expressive components (`LinearWavyProgressIndicator`, `OutlinedButton`, `Card`, `TopAppBar`).
3. **Contract-First & Honesty**:
   - Any API schema adjustments must be documented in `docs/CHANGELOG_API.md`.
   - All synthetic/simulated elements must be labeled (`simulated: true`, `SIMULATED`).

---

## 6. Seed Data & Persona Roles

- **Ishaan Verma (`chat_ishaan`)**: Close friend (`is_contact: true`). Seeded with 10 reciprocal, high-reply-ratio messages. Used to demonstrate that mild insults or casual banter are de-escalated due to positive interaction history.
- **Diya Sen (`chat_diya`)**: Friend (`is_contact: true`). Seeded with 7 messages.
- **Unknown User (77) (`chat_stranger_77`)**: Hostile stranger (`is_contact: false`, 0 history). Used to demonstrate immediate risk escalation (`warn_sender` / `urgent`) on identical offensive text.
- **Asha (`chat_mod_asha`)**: Community moderator persona for testing zero-shot slang ingestion and confirmation loops.

---

## 7. Known Traps & Debugging Notes

1. **PowerShell Variable Expansion in Inline Commands**:
   - Never run `powershell -Command "$env:VAR = 'val'; ..."` inside double quotes from another shell. The parent shell expands `$env:VAR` to an empty string. Always write scripts or use single quotes.
2. **UTF-16LE Log Files**:
   - `build-logs/*.log` produced by PowerShell redirection (`*>`) are encoded in UTF-16LE. If reading logs with tools or scripts, decode using `utf-16le` (or UTF-8 if written via `Set-Content -Encoding utf8`).
3. **Header Badge Wrapping in Jetpack Compose**:
   - When displaying badges next to usernames in `TopAppBar`, always constrain the username with `Modifier.weight(1f, fill = false)` and `TextOverflow.Ellipsis`, and set `softWrap = false, maxLines = 1` on the badge to prevent ugly mid-word wrapping (e.g. `BLOC / KED`).
4. **Timestamp Display**:
   - Timestamps stored in `SafeMessage` are ISO-8601 strings. Always use `formatMessageTimestamp(timestamp)` in UI bubbles to display localized user-facing times (`h:mm a`).
