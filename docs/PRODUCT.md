# Aegis Demo: Comprehensive Product Specification & UI Polish Blueprint

> **Hackathon Track:** Hacksploit 2026 – Problem Statement 5 (Detection & Prevention of Personal & Social Harassment)  
> **Target Audience:** Hackathon Judges, Evaluators, and Frontend Engineers  
> **Design Framework:** Material 3 Expressive (M3 Expressive)  
> **Purpose of this Document:** Serves as the single source of truth for the entire Android Demo Application. Contains complete product context, user persona journeys, 9 behavioral scenarios, screen-by-screen architectural blueprints, and the exact UI/UX polish specifications required to elevate the app from a prototype to an **impeccable, production-grade showcase**.

---

## 1. Executive Product Vision & Problem Statement

### 1.1 The Problem: Why Keyword Moderation Fails
Traditional content moderation and trust-and-safety filters rely on static keyword blacklists and regex patterns. In real-world harassment, perpetrators adapt:
1. **Polite Escalation:** A stalker sends 80 polite, unsolicited messages over 48 hours ("Hi", "Are you free?", "Just checking in"). Every individual sentence passes keyword filters, but the **velocity, lack of reciprocity, and persistence** constitute severe psychological harassment.
2. **Context-Free Slang & Code Words:** New harassment terms, emojis, or coded phrases evade dictionaries until added manually months later.
3. **Multimodal Vectors:** Harassment spills across DMs, public comment dogpiling, quiet-hour phone call floods (01:00 AM – 05:00 AM), synthetic/deepfake voice intimidation, and proxy NSFW images.
4. **False Positives:** High-frequency messaging between close friends or mutual couples is frequently misflagged by naive volume detectors.

### 1.2 The Aegis Solution
**Aegis** is a Multimodal Harassment Risk Engine paired with a high-empathy, client-side Android application that:
- Evaluates relational behavioral signals (reciprocity ratio, message burst velocity, quiet-hour persistence, off-platform spillover).
- Intervenes proactively with **graduated, victim-centric friction** (pre-send nudges, soft warning chips, media blurring, message throttling, and SHAP explainability).
- Empowers victims with **tamper-evident evidence export** (`.aegis.json`) for legal and institutional recourse.
- Operates in two resilient modes: **Live Server Mode** (connected to Rust backend & FastAPI engine over USB/WiFi) and **Offline Replay Mode** (bundled fixtures requiring zero running servers).

---

## 2. Core Personas & User Journeys

The demo features 5 fictional, synthetic personas that can be switched instantly via the **Persona Picker**:

| Persona | Role | Primary Experience / Journey | Key Screen |
| :--- | :--- | :--- | :--- |
| **Meera** | **Victim / Target** | Receives escalating co-worker messages, public comment mobbing, late-night call storms, and synthetic audio. Sees protective UI: blur overlays, risk chips, SHAP reasons sheet, and Safety Center export. | `ChatScreen`, `SafetyCenterScreen`, `CallsScreen` |
| **Rohan** | **Co-Worker / Harasser** | Sends persistent, one-sided messages. Experiences **Pre-Send Nudges (F11)** in the composer floating toolbar warning him before sending toxic drafts. | `ChatScreen` (Composer), `ComposerFloatingToolbar` |
| **Stranger 77**| **Troll / Mob Initiator** | Coordinates public dogpiling and meme defamation against Meera. Demonstrates public feed toxicity holding. | `FeedScreen` |
| **Ishaan & Diya**| **Control Pair (Healthy)**| High-volume, rapid-fire banter and frequent calls. Demonstrates **False-Positive Control (D5)**—reciprocity keeps risk strictly in `Low`. | `ChatsListScreen`, `ChatScreen` |
| **Asha** | **Safety Advocate / Moderator** | Reviews flagged incidents, verifies SHAP feature weights, and confirms emerging slang terms. | `SafetyCenterScreen`, `DemoDirectorScreen` |

---

## 3. The 9 Behavioral Scenarios (Showcase Matrix)

The Android app must visibly demonstrate every one of the 9 behavioral scenarios defined in the Aegis engine contract:

| # | Scenario Title | Harassment Vector | Baseline Keyword (C1) | Aegis Risk Score | Applied Action & UI Manifestation |
|---|---|---|---|---|---|
| **1** | **Escalating Co-Worker** | 80 polite, unreciprocated DMs over 3h | **0 Flags (PASS)** | **88 / 100 (HIGH)** | Soft warning chip $\rightarrow$ message throttling $\rightarrow$ hold pending review. |
| **2** | **Public Dogpiling** | Coordinated comments stream on public post | **Partial** | **94 / 100 (CRITICAL)** | Public feed holds toxic comments in escrow; bystander shield badges. |
| **3** | **Quiet-Hour Persistence**| 18 unsolicited messages between 01:00–04:30 AM | **0 Flags (PASS)** | **79 / 100 (HIGH)** | Quiet-hour indicator banner; auto-mute notification suggestions. |
| **4** | **Subtle Gaslighting** | Manipulative corporate text ("Nobody will believe you") | **0 Flags (PASS)** | **82 / 100 (HIGH)** | Sentiment shift alert; psychological coercion flag in SHAP sheet. |
| **5** | **Proxy NSFW Image** | Unsolicited sensitive graphic media | **0 Flags (PASS)** | **91 / 100 (CRITICAL)** | Frosted glass `BlurOverlay`; "Tap to reveal" with explicit warning. |
| **6** | **Call Frequency Spike** | 40 missed calls in dead of night | **N/A (No Text)** | **96 / 100 (CRITICAL)** | Call log spike chart; incoming call warning dialog; emergency block. |
| **7** | **Healthy Banter (Control)**| Rapid reciprocal messages between mutual couple | **False Alarm** | **12 / 100 (LOW)** | Clean green status; proves false-positive resilience. |
| **8** | **Synthetic Voice Note** | AI-generated audio intimidation | **0 Flags (PASS)** | **92 / 100 (CRITICAL)** | Waveform meter (`LinearWavyProgressIndicator`), transcript shield, synthetic audio badge. |
| **9** | **Meme Defamation** | Out-of-context screenshot defamation | **0 Flags (PASS)** | **85 / 100 (HIGH)** | OCR text extraction, defamation warning chip, evidence tag. |

---

## 4. Design System: Material 3 Expressive (M3 Expressive)

The UI must adhere strictly to **Material 3 Expressive** principles to communicate empathy, precision, and state clarity.

### 4.1 Strict Component Constraints (Rule R2)
Per project guidelines, **only 3 custom composables are permitted**. All other UI must use standard M3 Expressive components:
1. **`SafeMessageBubble`**: Custom bubble composition wrapping M3 Card/Surface, dynamic risk tier badges, timestamp, and optional C1 comparator badge.
2. **`BlurOverlay`**: Custom frosted glass modifier + eye toggle button to safely shield unsolicited media.
3. **`SeverityMeter`**: Custom composition combining `LinearWavyProgressIndicator`, dynamic color steps, and risk level text (`LOW`, `REVIEW`, `MEDIUM`, `HIGH`, `CRITICAL`).

### 4.2 Standard M3 Expressive Components Used Everywhere Else
- **`LinearWavyProgressIndicator`**: For continuous risk severity meters and voice note audio waveforms.
- **`HorizontalFloatingToolbar`**: Elevated, pill-shaped toolbar for chat composer actions and pre-send nudge dialogs.
- **`SplitButtonLayout`**: Primary-secondary action pairing in Safety Center (e.g., [Export Evidence] + [▼ Share via Secure Link]).
- **`ModalBottomSheet`**: Rich slide-up sheet displaying SHAP feature contributions and explanation reasons.
- **`FilterChip` / `AssistChip`**: Rounded tags for scenario selection, persona badges, and risk category filters.
- **`ElevatedCard` / `OutlinedCard`**: Contained cards with subtle tonal elevation for feed posts, incident summaries, and call logs.
- **`NavigationBar`**: Edge-to-edge adaptive bottom navigation with expressive pill indicators.
- **`MotionScheme.expressive()`**: Standardized spring and deceleration curves for all sheet expansions, card flips, and list animations.

### 4.3 Color & Semantic Palette
- **Primary / Empathic Accent:** Soft Indigo / Violet (`#6750A4`) representing security, calm, and protection.
- **Risk Tiers:**
  - `Low` (0–29): Forest Emerald (`#2E7D32` / container `#C8E6C9`) – Healthy interaction.
  - `Review` (30–59): Amber Gold (`#F57F17` / container `#FFF9C4`) – Elevated monitoring.
  - `Medium` (60–74): Deep Tangerine (`#E65100` / container `#FFE0B2`) – Soft intervention.
  - `High` (75–89): Vivid Crimson (`#C62828` / container `#FFCDD2`) – Active shielding / throttling.
  - `Critical` (90–100): Wine Red / Burgundy (`#880E4F` / container `#F8BBD0`) – Immediate hold & evidence packaging.

### 4.4 Mandatory Honesty & Privacy Labels (Rules R5 & R6)
The app must display visible tags on all simulated content to maintain 100% academic and demo integrity:
- Persona badges: `[SIMULATED PERSONA]`
- Demo mode indicator: `[DEMO REPLAY MODE - 0 SERVERS]`
- Voice notes: `[SYNTHETIC AUDIO - 16kHz MONO]`
- Sensitive images: `[DEMO PROXY IMAGE]`
- Call records: `[SIMULATED CALL LOG - 40 OVERNIGHT ATTEMPTS]`

---

## 5. Screen-by-Screen Architecture & UI Blueprint

```
┌────────────────────────────────────────────────────────────────────────┐
│                        BOTTOM NAVIGATION                               │
│  [💬 Chats]     [📰 Public Feed]     [📞 Calls]     [🛡️ Safety Center] │
└────────────────────────────────────────────────────────────────────────┘
```

### 5.1 Screen 1: Chats List Screen (`ChatsListScreen.kt`)
- **Header:**
  - Large expressive title ("Aegis Messages").
  - Persona Quick Switcher Pill (Avatar + "Viewing as Meera [Victim]" + Dropdown chevron).
  - Mode chip: Green `[Live]` or Blue `[Replay Fixture]`.
- **List Items (Conversations):**
  - Left: Contact Avatar with persona indicator ring (Red for Rohan [Escalating], Green for Diya [Normal], Gray for Unknown).
  - Center: Contact Name, role badge (e.g. `Co-Worker`), last message snippet, timestamp.
  - Right: Real-time `SeverityMeter` mini wavy bar (0–100) + unread pill.
  - Touch interaction: Expressive ripple leading to `ChatScreen`.

### 5.2 Screen 2: Chat Detail Screen (`ChatScreen.kt`)
- **Top App Bar:**
  - Back arrow, contact avatar, contact name, safety status chip (`Safe`, `Caution`, `Shielded`).
  - Actions: "Why?" button (opens SHAP sheet) + Persona Switcher shortcut.
- **Pinned Pinned Safety Banner:**
  - Displays when risk exceeds `Review` (>59).
  - Wavy progress bar showing current conversation score (e.g., `88/100 HIGH`).
  - Text: *"Aegis detected 80 unsolicited messages without replies. Message gating is active."*
  - Action button: *"View Safety Options"*.
- **Message List (`SafeMessageBubble.kt`):**
  - **Text Bubbles:** Distinct styling for incoming vs outgoing. Subtle colored border matching the message's assigned risk tier.
  - **Blurred Images:** Frosted glass overlay obscuring the image with warning text: *"Potentially sensitive media. Tap eye icon to unblur."*
  - **Voice Notes:** Play/Pause pill, audio duration, wavy progress track, `[SYNTHETIC AUDIO]` label, and *"View Transcript"* expandable button.
  - **C1 Comparison Badge:** On Scenario 1 messages: *"❌ Missed by Keyword Filter | ✅ Caught by Aegis AI (Score: 88)"*.
- **Composer & Pre-Send Nudge (`ComposerFloatingToolbar.kt`):**
  - Floating pill toolbar above the keyboard.
  - As Rohan types toxic words ("You will regret this"), a debounce timer triggers a soft warning chip: *"⚠️ This message may violate workplace harassment guidelines. Consider rewording before sending."*
  - Quick action buttons: Rewrite suggestion, Discard, Send anyway.

### 5.3 Screen 3: Public Feed Screen (`FeedScreen.kt`)
- **Purpose:** Demonstrates Scenario 2 (Public Dogpiling / Mob Harassment).
- **Post Header:** Meera's public work announcement post.
- **Comment Stream:**
  - Multiple comments arriving in rapid succession from strangers.
  - Held Comments: Highlighted in amber with status badge: *"Held in Escrow by Aegis Engine (Dogpiling detected: 14 comments in 90 seconds)"*.
  - User Controls: *"Approve"*, *"Delete All Held"*, *"Turn on Mob Shield"*.

### 5.4 Screen 4: Call Frequency Log Screen (`CallsScreen.kt`)
- **Purpose:** Demonstrates Scenario 6 (Off-Platform Quiet-Hour Spillover).
- **Spike Summary Card:**
  - Bar visualization of calls by hour: Tall red cluster between 01:00 AM and 05:30 AM (40 missed calls from Rohan).
  - Insight card: *"Unusual frequency detected: 40 calls during quiet hours. 0 answered. Risk: 96/100 CRITICAL."*
- **Incoming Call Overlay Dialog:**
  - Simulated ringing dialog with red flashing shield: *"⚠️ High-Risk Call Detected. Rohan has called 40 times tonight. Suggested action: Send to Voicemail & Log Evidence."*

### 5.5 Screen 5: Safety Center Screen (`SafetyCenterScreen.kt`)
- **Purpose:** Central command for victim empowerment, evidence preservation, and crisis helplines.
- **Threat Vector Breakdown:**
  - Grid of 4 signal cards:
    - Text Persistence: `High (88/100)`
    - Night-Time Spikes: `Critical (96/100)`
    - Media Toxicity: `Critical (91/100)`
    - Reciprocity Ratio: `0.02 (Extremely One-Sided)`
- **Evidence Bundle Generator:**
  - Card with `SplitButtonLayout`:
    - Button 1: *"Export Signed Evidence (.aegis.json)"* – Generates SHA-256 hash-chained JSON timeline suitable for police or HR reporting.
    - Button 2 (Dropdown): *"Share with Trusted Contact"* / *"Copy Summary"*.
- **Emergency Helplines Carousel:**
  - Emergency contact cards:
    - **1930** – National Cyber Crime Reporting Portal (Direct Call Button).
    - **Vandrevala Foundation** – 24/7 Mental Health Helpline.
    - **NCW 7827170170** – National Commission for Women Helpline.

### 5.6 Screen 6: Demo Director Screen (`DemoDirectorScreen.kt`)
- **Purpose:** Allows hackathon judges to control scenarios and witness live engine behavior.
- **Components:**
  - **Scenario Carousel:** Horizontal carousel of all 9 scenarios with quick "Run Scenario" button.
  - **Speed Slider:** 1x, 5x, 10x, 30x, 60x playback speed multiplier.
  - **Live C1 Keyword Comparator Panel:**
    - Side-by-side view showing what the legacy keyword filter saw (0 flags) vs what Aegis saw (94/100 critical).
  - **Reset Demo State Button:** Flushes state back to pristine demo starting point.

### 5.7 Screen 7: Persona Picker (`PersonaPickerScreen.kt`)
- Switch active user view on the fly (Meera, Rohan, Stranger 77, Ishaan, Asha).
- Toggle Replay Mode (Offline fixtures vs Live network).

### 5.8 Screen 8: Settings & Transparency (`SettingsScreen.kt`)
- Server Base URL input (`http://localhost:8080`).
- Test Connection button with ping latency readout.
- Honesty & Synthetic Data statement disclosure.

---

## 6. Impeccable UI Polish Checklist (Eliminating "Vibe-Coded" Flaws)

Use this checklist when applying the **impeccable** skill to refine the application's visual and tactile feel:

### 6.1 Layout Rhythm & Spacing Grid
- [ ] **Strict 8dp Grid:** All margins, paddings, and spacers must be multiples of 4dp / 8dp (e.g. 4dp, 8dp, 12dp, 16dp, 24dp, 32dp). Eliminate arbitrary values like 7dp, 13dp, 19dp.
- [ ] **Edge-to-Edge Insets:** Ensure all screens respect status bar and navigation bar insets using `Modifier.windowInsetsPadding(WindowInsets.safeDrawing)` so content never collides with system cutouts or home indicators.
- [ ] **Card Padding Consistency:** All cards must use 16dp outer padding and 16dp internal content padding.

### 6.2 Typography & Visual Hierarchy
- [ ] **Typography Scale:** Strictly map every text element to Material 3 typography tokens (`headlineLarge`, `titleMedium`, `bodyMedium`, `labelSmall`). Never declare raw `fontSize = 13.sp` or hardcoded line heights.
- [ ] **Label Legibility:** All badges (`SIMULATED`, `SYNTHETIC AUDIO`, `HIGH RISK`) must use `MaterialTheme.typography.labelSmall`, bold tracking, and distinct container color with high contrast ratios (WCAG AAA compliant).
- [ ] **Text Truncation:** Ensure long contact names or message previews use `TextOverflow.Ellipsis` with clean `maxLines` constraints.

### 6.3 Micro-Interactions & Expressive Motion
- [ ] **Expressive Springs:** Use `MotionScheme.expressive()` for all transitions:
  - Bottom sheet sliding in/out with soft overshoot deceleration.
  - Message bubble appearance with subtle spring scale (0.95 to 1.0).
  - Pre-send nudge toolbar floating into view with alpha fade + slide up.
- [ ] **Wavy Progress Indicator Dynamics:** Ensure the `LinearWavyProgressIndicator` animates smoothly as severity changes rather than jumping abruptly.
- [ ] **Tactile Feedback:** Add subtle haptic vibration (`LocalHapticFeedback`) when tapping emergency export or toggling blur reveal.

### 6.4 Component Refinement & Surface Polish
- [ ] **Bubble Grouping Geometry:** Consecutive messages from the same sender should have grouped corners:
  - Top bubble: 16dp top-left, 16dp top-right, 4dp bottom-right, 16dp bottom-left.
  - Middle bubble: 4dp right side corners.
  - Bottom bubble: 16dp bottom corners.
- [ ] **Frosted Glass Blur:** Ensure `BlurOverlay` uses a legitimate blur modifier (`Modifier.blur(24.dp)`) combined with a translucent surface overlay (`surface.copy(alpha = 0.85f)`) and a clear, high-contrast action button.
- [ ] **Empty States:** When a chat or feed has 0 items, display an expressive vector icon, a friendly headline, and an action chip rather than a blank screen.

---

## 7. Architectural Integrity & Verification

1. **Dual Repository Locator:** `ServiceLocator.kt` provides either `NetworkChatRepository` (OkHttp WebSocket + Retrofit) or `FixtureRepository` (offline replay). The UI must behave identically regardless of which repository is active.
2. **Quality Gates:** Any UI modification must continue to pass all project quality gates:
   - `just app-build` (must compile clean with AGP 9.0.0 and `compileSdk = 37`).
   - `just app-test` (all unit tests must pass).
   - `just app-install` & `just app-smoke` (must deploy cleanly to physical POCO device `bf459d04`).

---
*Document Version: 2.0.0 (Hacksploit 2026 PS5 Release)*  
*Engine Contract: `contracts/engine-openapi.json` | API Spec: `docs/API_CHAT.md` | UI Spec: `docs/UI_SPEC.md`*
