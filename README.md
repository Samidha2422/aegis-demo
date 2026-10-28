# Aegis Demo – Multimodal Harassment Risk Engine Showcase

**Hacksploit 2026 – Problem Statement 5: Detection and Prevention of Personal & Social Harassment**

Aegis Demo showcases the **Harassment Risk Engine** through a realistic social messaging and calling experience, featuring:
1. **`chat-server/`**: Rust service simulating a modern social platform with real-time WebSocket push, engine verdict forwarding, and platform-side actions (warn, blur, throttle, hold, escalate).
2. **`android-app/`**: Native Kotlin + Jetpack Compose application built with **Material 3 Expressive** components, edge-to-edge support, adaptive layouts, and an offline **Replay mode**.
3. **`scenarios/`**: 9 scripted behavioral scenarios proving engine efficacy across text, images, synthetic audio, and call logs.

---

## Architecture Overview

```
Android App  <──REST / WS──>  chat-server (Rust: axum)  ──HTTP──>  Harassment Risk Engine
    │                                ├─ Personas, chats, messages
    │ (direct calls:                 ├─ Platform actions (ActionPolicy)
    │  nudge, export, uploads)       ├─ Scenario runner (YAML)
    └─ Replay Mode (fixtures)        ├─ Baseline comparator (C1)
                                     └─ WS: message, safety_update, alert
```

---

## Key Highlights

- **M3 Expressive Design**: Styled using `MaterialExpressiveTheme`, `MotionScheme.expressive()`, expressive shapes, and official Expressive components (`HorizontalFloatingToolbar`, `ButtonGroup`, `LoadingIndicator`, `HorizontalUncontainedCarousel`).
- **Honesty First**: Every synthetic or simulated signal is explicitly marked with UI badges (`SIMULATED`, `SYNTHETIC AUDIO`, `DEMO PROXY`).
- **Resilient Replay Mode**: Full demo runnable directly on-device from bundled JSON fixtures with zero backend servers required.
- **Pre-Send Nudge**: Proactive on-device intervention debouncing text drafts and warning before sending abusive content.
- **Keyword vs. Behavioral AI (C1)**: Side-by-side comparison demonstrating how behavioral detection catches polite stalkers that evade keyword filters.

---

## Quick Start

See [RUNBOOK.md](docs/RUNBOOK.md) for full setup instructions and [DEMO_SCRIPT.md](docs/DEMO_SCRIPT.md) for the 3-minute hackathon presentation track.
