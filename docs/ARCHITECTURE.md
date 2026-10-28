# Architecture Specification

```
                ┌────────────────────────────────────────────────────────┐
                │                  Android App (Client)                  │
                │  - Jetpack Compose + Material 3 Expressive             │
                │  - ShortNavigationBar (Phone) / NavigationRail         │
                │  - SafeMessageBubble + Blur Overlay                    │
                │  - Replay Mode (FixtureRepository)                     │
                └─────────────────────────▲──────────────────────────────┘
                                          │
                     REST / WebSocket     │ (Direct Engine calls:
                     (Port 8080)          │  Nudge, Evidence Export,
                                          │  Voicemail/Log Uploads: Port 8000)
                                          │
                                          ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    chat-server (Rust Social Platform)                   │
│  - Axum HTTP & WebSocket handlers (/ws)                                 │
│  - SQLite storage (personas, chats, messages, media metadata)           │
│  - EngineClient (HTTP client to Harassment Risk Engine with fallback)   │
│  - ActionPolicy (Maps engine verdict to platform actions: warn/blur/hold)│
│  - ScenarioRunner (Scripted YAML playback with compressed speed factor)  │
│  - BaselineComparator (Keyword filter for Challenge C1 side-by-side)     │
└────────────────────────────────────▲────────────────────────────────────┘
                                     │
                     HTTP / REST     │ Event Envelope
                     (Port 8000)     │ (occurred_at, sender, recipient,
                                     │  channel, context, content)
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      Harassment Risk Engine (Rust)                      │
│  - Behavioral Feature Pipeline (XGBoost, reciprocity, volume, slope)    │
│  - Multimodal Classifiers (Text toxicity, OCR, NSFW, Whisper ASR)       │
│  - Slang Adaptation & Lexicon Management                                │
│  - Risk Fusion & Explainability (0-100 severity, tier, reasons)         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 1. Network Topologies & Ports

- **Local Development via `adb reverse`:**
  - Chat Server: `http://localhost:8080` (adb reverse `tcp:8080 tcp:8080`)
  - Engine: `http://localhost:8000` (adb reverse `tcp:8000 tcp:8000`)
- **Direct Engine Access:**
  - The Android app talks to `chat-server` for general messaging and feeds.
  - For pre-send nudges, direct evidence exports, and standalone call-log uploads, the app can either route through `chat-server` proxy endpoints or communicate directly with the engine URL configured in Settings.

---

## 2. Event Envelope & Asynchronous Safety Updates

When chat events (text, media, calls) are ingested:
1. `chat-server` stores the incoming message with state `Scanning` (or immediate verdict if synchronous).
2. The server dispatches the normalized event envelope to the Harassment Risk Engine.
3. If analysis is delayed (e.g., Whisper audio transcription or OCR processing), the message is delivered to the recipient with an initial state, followed by a WebSocket `safety_update` event containing the full verdict once processing completes.
4. If the engine is unreachable, the message is delivered unscored, tagged `engine_unavailable`, and a subtle offline notification is displayed.

---

## 3. Storage & Privacy Controls

- **Transient Media:** Uploaded images and voice notes are stored only temporarily for analysis and demo presentation, and wiped upon `/demo/reset`.
- **Synthetic Personas:** All test participants are fictional accounts (`meera`, `rohan`, `ishaan`, `diya`, `stranger_77`, `bot01`..`bot10`, `mod_asha`).
