# Material 3 Expressive UI Specification (UI_SPEC.md)

This specification governs the design and implementation of the Aegis Android App, adhering to Operating Rule R2 (Material 3 Expressive Component First).

---

## 1. Theme, Color & Motion Foundation

- **Theme Container:** `MaterialExpressiveTheme`
- **Motion:** `MotionScheme.expressive()` with expressive entrance/exit springs.
- **Shapes:** `MaterialShapes` with morphing corner radii for risk badges, avatars, and message bubbles.
- **Color Palette:** Expressive dynamic color scheme with branded semantic fallback roles:
  - `RiskLow`: Subtle tonal container, neutral green/teal tint.
  - `RiskReview`: Amber / Yellow expressive tonal container.
  - `RiskHigh`: Tangerine / Orange expressive high-contrast container.
  - `RiskUrgent`: Deep crimson / Red container with high-contrast text.

---

## 2. Component Mapping Table (Section 7 Compliance)

| Screen / UI Element | M3 Expressive Component | Rationale & Configuration |
|---|---|---|
| **Top App Bars** (Chats, Safety Center) | `LargeFlexibleTopAppBar` | Expressive collapsible header expanding into large typography when at top. |
| **Top App Bars** (Chat, Feed, Calls, Director) | `MediumFlexibleTopAppBar` | Medium flexible bar preserving vertical real estate. |
| **Primary Navigation** | `ShortNavigationBar` (phone) / `WideNavigationRail` (tablet/foldable) | Adaptive navigation via `NavigationSuiteScaffold` with edge-to-edge support. |
| **Chat Composer Actions** | `HorizontalFloatingToolbar` | Pill-shaped floating bar housing attach, voice note, and send actions. |
| **Attach & Mode Menu** | `FloatingActionButtonMenu` + `FloatingActionButtonMenuItem` | Smooth expanding menu for media attachments and quick scenario shortcuts. |
| **Buttons & Action Groups** | `Button`, `FilledTonalButton`, `OutlinedButton`, `ButtonGroup` | Grouped actions for "View anyway / Report / Block" and Pre-send Nudge dialogs. |
| **Split Buttons** | `SplitButtonLayout` | "Export evidence" (Primary action: JSON, dropdown secondary: HTML). |
| **Toggles & Selectors** | `ToggleButton`, `TonalToggleButton` | Mode toggles: Demo Mode, Replay Mode, Speed selection (1x / 10x / 60x). |
| **Progress & Indicators** | `LoadingIndicator`, `ContainedLoadingIndicator`, `LinearWavyProgressIndicator`, `CircularWavyProgressIndicator` | "Scanning…" indicator, severity meters, audio recording wave progress, media upload spinner. |
| **Lists & Alerts** | `ListItem`, Segmented `ListItem` | Chat threads, Safety Center alert cards, Call log entries. |
| **Cards & Carousels** | `ElevatedCard`, `HorizontalUncontainedCarousel` | Demo Director scenario cards and Safety Center help-resource cards. |
| **Chips & Badges** | `AssistChip`, `SuggestionChip`, `Badge`, `BadgedBox` | Explanatory reasons chips, unread/held message count badges. |
| **Sheets & Dialogs** | `ModalBottomSheet`, `AlertDialog` | Detailed signals/reasons sheet, Pre-send Nudge intervention dialog. |
| **Text Inputs** | `OutlinedTextField`, `SearchBar` | Chat message composer with IME insets, conversation search. |

---

## 3. Justification for Custom Composables

In accordance with Section 7, only three custom UI composables are constructed, each justified by unique domain requirements:

1. **`SafeMessageBubble`**:
   - *Need:* Displays contextual state transitions across 9 distinct safety states (`Delivered`, `Scanning`, `Warned`, `Blurred`, `Hidden`, `Held`, `Escalated`, `TranscriptOnly`, `EngineUnavailable`) with interactive explainability chips.
   - *Composition:* Built strictly from M3 `Surface`, `MaterialShapes`, `LinearWavyProgressIndicator`, and `AssistChip`.
2. **`ImageBlurOverlay`**:
   - *Need:* Renders a privacy-preserving Gaussian blur over flagged explicit images or harassment screenshots, with "View anyway" and "Report" overlays.
   - *Composition:* Built using `Modifier.blur()` with semi-transparent scrim and M3 `ButtonGroup`.
3. **`SeverityMeter`**:
   - *Need:* Real-time visual representation of risk (0-100) combining module signals (text, OCR, voice, behavioral slope).
   - *Composition:* Built combining `LinearWavyProgressIndicator` styled with dynamic risk tier colors and expressive typography.

---

## 4. Screen-by-Screen Specifications (8 Allowed Screens)

### Screen 1: Persona Picker
- Choose active identity (`meera` default, `rohan`, `ishaan`, `diya`, `stranger_77`, `mod_asha`).
- Visual toggle for Live vs. Replay mode.
- Expressive avatar cards using `MaterialShapes`.

### Screen 2: Chats List
- Active conversations with real-time safety badge (tier color + severity icon).
- Held messages counter (`Badge`).
- Search bar filter.

### Screen 3: Chat Screen
- Message thread with `SafeMessageBubble`.
- "Scanning…" animated loading state while engine processes asynchronously.
- Bottom composer with `HorizontalFloatingToolbar` (attach image, voice note record, send).
- Pre-send nudge triggered on abusive typing.
- Overflow menu: Block, Mute, Report.
- Tapping safety chip opens `ModalBottomSheet` with engine signals and plain-language reasons.

### Screen 4: Feed (Comments)
- Scenario 2 pile-on demonstration.
- Post with stream of bot comments (`bot01`..`bot10`).
- Collapsed replies banner, rate-limiting banner, and automated high-tier protection card.

### Screen 5: Safety Center
- Aggregated alert feed (tiers + reasons).
- `SplitButtonLayout` for Evidence Export (JSON / HTML).
- `HorizontalUncontainedCarousel` for helpline resources (cybercrime.gov.in, 1930, 181, 112, StopNCII.org).
- Engine & ML model status card.

### Screen 6: Calls Screen
- Behavioral call log highlighting repeated overnight calls from `unknown_number`.
- "Simulate Call" trigger.
- CSV Call Log import action.
- Voicemail upload card.
- **Simulated Live Call UI**: Live transcript stream with real-time severity meter updating dynamically.

### Screen 7: Demo Director
- Interactive scenario control center.
- `HorizontalUncontainedCarousel` for the 9 scenarios (Play, Speed selector 1x/10x/60x, Reset).
- Real-time step progress log with active WebSocket feedback.
- **Keyword vs. Engine Comparison Panel (Challenge C1)**: Displays Rohan's polite stalking passing keyword filters (0 flags) while the engine escalates to Review -> High.
- Quick test shortcuts: "Try the Nudge", "Inject Voice Clip".

### Screen 8: Settings
- Server URL (`http://localhost:8080`) & Engine URL (`http://localhost:8000`).
- API Key entry.
- Sensitivity threshold selector.
- Judge mode toggle: "Show Raw Verdict JSON" in message sheets.

### Live settings and error states (2026-10-29)
Settings uses TopAppBar, OutlinedTextField (inline invalid-URL state), Switch,
Button/OutlinedButton and ListItem for engine backend/degradation status. Engine
credentials are server-side only. The initial live URL is http://10.0.2.2:8081/;
Replay is an explicit SIMULATED choice. Navigation displays repository errors in
Material Text; the reasons sheet shows actual module status/backend and degradation.
