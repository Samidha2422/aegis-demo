# Behavioral Demo Scenarios (SCENARIOS.md)

This document specifies the 9 scripted behavioral scenarios for the Aegis Harassment Risk Engine showcase. Each scenario tests a unique technical capability and includes precise expected on-screen behaviors.

---

## Scenario Index

| # | Scenario Title | Actor(s) | Modality | Key Proving Point | Expected On-Screen Behavior |
|---|---|---|---|---|---|
| **1** | **The Polite Stalker** | `rohan` → `meera` | Text | Behavioral AI beats keyword filtering (Challenge C1) | `rohan` sends 80 courteous messages over 3 simulated hours. `meera` does not reply and blocks at message ~30; `rohan` continues. Keyword filter shows 0 flags; engine escalates from `Low` → `Review` → `High`. Rohan is throttled; Meera sees collapsed held-messages badge and an alert with SHAP reasons ("80 messages in 3 h, 0 replies, contact after block"). |
| **2** | **The Pile-On** | `bot01`..`bot10` → `meera` | Feed Comments | High-velocity coordinated volume spike | 10 distinct accounts flood comments on Meera's post within 60s. Feed applies auto-rate-limiting, collapses replies into a protective banner, and raises a High-tier pile-on alert in the Safety Center. |
| **3** | **Cyberflashing** | `stranger_77` → `meera` | Image | Multimodal NSFW image protection with honest labelling | Unsolicited benign proxy image arrives from an unknown sender. Engine detects explicit score. Image is blurred on screen with a visible badge: `"DEMO PROXY / SIMULATED SIGNAL"` and buttons `"View anyway"`, `"Report"`, `"Block"`. |
| **4** | **The Screenshot Meme** | `rohan` → `meera` | OCR / Screenshot | Text extraction from images evading text filters | Image contains rendered toxic text. Text pipeline alone misses it; OCR module extracts the text and flags it with High severity. The extracted OCR text is explicitly cited as the primary reason. |
| **5** | **The Voice Note** | `unknown_caller` → `meera` | Synthetic Audio | Speech-to-text + transcript protection | Inbound voice note contains synthetic threatening speech. Engine transcribes clip via Whisper, detects threat, and replaces the audio player with the written transcript and a `"Play anyway"` override. Labelled `"SYNTHETIC AUDIO"`. |
| **6** | **The Call Log** | `unknown_number` → `meera` | Behavioral Call Log | Pattern detection on phone calls | Synthetic log import reveals 40 overnight calls with high unanswered ratio continuing after a block. Calls screen highlights the caller in red with reasons: `"40 calls overnight, 92% unanswered, contact after block"`. |
| **7** | **The Healthy Couple** | `ishaan` ↔ `diya` | High-Volume Reciprocal | False-positive control & precision (Challenge C4) | High message volume and long answered calls between mutual contacts. Reciprocity and reply ratio keep the risk strictly at `Low` (severity < 15), with zero disruptive banners or holds. |
| **8** | **Slang Adaptation** | `mod_asha` + test sender | Lexicon / Slang | Dynamic adaptation to emerging coded terms | Moderator Asha confirms the invented slang term ("vexling"). The lexicon is updated in real time. The next message containing "vexling" is instantly flagged with reason `"Confirmed community term"`. |
| **9** | **Evidence Export** | `meera` | Cryptographic Export | Victim-centric legal and platform reporting | Meera taps `"Export Evidence"` in Safety Center. App generates an export in JSON/HTML containing a hash-chained chronological timeline of all flagged events (text, OCR, voice, call logs). |

---

## Honesty & Labelling Requirements (Rule R5)

To uphold research credibility and prevent misleading claims:
1. **Scenario 3:** The proxy image must clearly state `DEMO PROXY IMAGE (Simulated NSFW signal)`.
2. **Scenario 5:** The audio player must clearly show `SYNTHETIC AUDIO (TTS Generated)`.
3. **Scenario 6:** The calls screen must display `SIMULATED CALL LOG`.
4. **General:** No claim may be made that the app intercepts third-party WhatsApp or raw phone lines.
