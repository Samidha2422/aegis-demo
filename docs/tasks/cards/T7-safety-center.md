# T7: Safety Center (Alerts, SplitButton Evidence Export & Helplines Carousel)

## 0. Your role in one paragraph
You are the Android UI Teammate responsible for constructing the **Safety Center Screen** in `android-app/app/src/main/java/com/aegis/demo/ui/safety/SafetyCenterScreen.kt` and its ViewModel. This screen aggregates high-tier harassment alerts with explainability reasons, provides one-tap **Evidence Export using `SplitButtonLayout`** (JSON / HTML), showcases helpline resource cards in a **`HorizontalUncontainedCarousel`**, and displays Harassment Risk Engine / model health status.

## 1. Project context
The Safety Center is the victim's primary sanctuary and action center. Rather than burying controls in settings, Aegis gives victims transparent explanations, direct access to legal reporting exports, and immediate emergency contact resources.

## 2. Repo map and your boundary
- **You may edit:** `android-app/app/src/main/java/com/aegis/demo/ui/safety/`, `android-app/app/src/test/java/com/aegis/demo/ui/safety/`.
- **You must NOT edit:** `android-app/app/src/main/java/com/aegis/demo/ui/chat/`, `chat-server/`.

## 3. What already exists & M3 Expressive Skills
- Top bar: `LargeFlexibleTopAppBar` (collapses as user scrolls down).
- Action widget: **`SplitButtonLayout`** for "Export Evidence" (Primary action: JSON, Dropdown action: HTML).
- Cards & Carousels: `ElevatedCard` for alert entries, **`HorizontalUncontainedCarousel`** for emergency resources.
- Chips: `AssistChip` for tier badges and contributing reasons.

## 4. Exact deliverables
1. `SafetyCenterScreen.kt` (`@Composable fun SafetyCenterScreen(viewModel: SafetyCenterViewModel)`)
2. `EvidenceExportSheet.kt` (`@Composable fun EvidenceExportSheet(exportData: EvidenceReport, onShare: (String) -> Unit)`)
3. `SafetyCenterViewModel.kt` (`StateFlow<SafetyCenterUiState>`)
4. `SafetyCenterViewModelTest.kt`

## 5. Step-by-step instructions
1. Build `SafetyCenterScreen` with `Scaffold` and `LargeFlexibleTopAppBar`.
2. Section 1: **Active Alerts**:
   - `LazyColumn` of `ElevatedCard`s showing incidents (e.g. Rohan stalking alert, Pile-on comment spike).
   - Display tier badge, timestamp, and plain-language reasons.
3. Section 2: **Evidence Export**:
   - Render M3 `SplitButtonLayout`:
     - Primary button: `"Export Evidence (JSON)"`
     - Secondary dropdown: `"HTML Report"`
   - On click, generate export file and launch system `Intent.ACTION_SEND` share sheet.
4. Section 3: **Help & Emergency Resources**:
   - Use `HorizontalUncontainedCarousel` to display cards for:
     - National Cyber Crime Reporting Portal (`cybercrime.gov.in`)
     - National Cyber Fraud / Harassment Helpline (`1930`)
     - Women Helpline (`181`)
     - National Emergency (`112`)
     - StopNCII.org (Non-Consensual Intimate Imagery prevention)
5. Section 4: **Engine & Model Health**:
   - Status indicators for XLM-R Toxicity, XGBoost Stalking Classifier, Whisper ASR, OCR.

## 6. Behavior rules and edge cases
- Notice: In accordance with Section 6, the phrase "verify before demo" belongs in documentation, NOT in the user-facing UI.
- If there are zero active alerts, show an expressive safe state: `"No active safety threats detected. Your conversations are protected."`

## 7. Tests to write
- `SafetyCenterViewModelTest.kt`:
  - `test_active_alerts_loaded_and_sorted_by_tier`
  - `test_evidence_export_produces_valid_json_payload`

## 8. Commands
Always redirect output:
```bash
just app-test > build-logs/app-test.log 2>&1
tail -n 40 build-logs/app-test.log
grep -n "error" -A6 build-logs/app-test.log | head -60
```

## 9. Definition of done
- [ ] Safety Center screen displays alerts with reasons.
- [ ] `SplitButtonLayout` triggers evidence share sheet.
- [ ] `HorizontalUncontainedCarousel` displays all 5 helpline resources.
- [ ] Verified via `just app-smoke`.

## 10. Do NOT
- Do NOT use standard Row/LazyRow for resources where `HorizontalUncontainedCarousel` is required.
- Do NOT hand-roll split buttons.

## 11. How to report back
Provide a 10-line summary confirming: Safety Center implementation, M3 Expressive carousel usage, and test status.

## 12. If blocked
Write `BLOCKED.md` with error details and stop.
