# T5: Calls Screen (Call Log, Voicemail Upload & Simulated Live Call)

## 0. Your role in one paragraph
You are the Android UI Teammate responsible for creating the **Calls Screen** in `android-app/app/src/main/java/com/aegis/demo/ui/calls/CallsScreen.kt` and its ViewModel. This screen showcases phone-call behavioral analysis (Scenario 6), CSV log import, voicemail upload with evidence report, and the **Simulated Live Call UI** streaming chunked transcripts with a real-time wavy severity meter.

## 1. Project context
Ordinary apps cannot covertly tap live calls due to OS and legal boundaries. We honestly demonstrate behavioral analysis on call logs (frequency, overnight calls, unanswered ratio) and showcase live call assistance through a clearly labelled simulation.

## 2. Repo map and your boundary
- **You may edit:** `android-app/app/src/main/java/com/aegis/demo/ui/calls/`, `android-app/app/src/test/java/com/aegis/demo/ui/calls/`.
- **You must NOT edit:** `android-app/app/src/main/java/com/aegis/demo/ui/chat/`, `chat-server/`.

## 3. What already exists & M3 Expressive Skills
- Top bar: `MediumFlexibleTopAppBar`
- Insets: `Scaffold` inner padding consumed properly.
- List items: `ListItem` / Segmented list items for call log entries.
- Progress & Metrics: `LinearWavyProgressIndicator` for the real-time severity meter.
- Action group: `ButtonGroup` for "Simulate Call / Import CSV / Upload Voicemail".
- Chips: `AssistChip` for call flags ("92% Unanswered", "Overnight Burst", "Contact After Block").

## 4. Exact deliverables
1. `CallsScreen.kt` (`@Composable fun CallsScreen(viewModel: CallsViewModel)`)
2. `LiveCallDialog.kt` (`@Composable fun LiveCallDialog(callState: LiveCallState, onEndCall: () -> Unit)`)
3. `CallsViewModel.kt` (`StateFlow<CallsUiState>`)
4. `CallsViewModelTest.kt`

## 5. Step-by-step instructions
1. Build `CallsScreen` with `Scaffold` and top action bar.
2. Render call records in a `LazyColumn` using M3 `ListItem`:
   - Caller number, timestamp, call duration, status icon (missed, answered).
   - High-risk records (`unknown_number`) highlighted with amber/red tonal container and `AssistChip` reasons.
3. Add action buttons: `"Simulate Incoming Call"`, `"Import Call Log (CSV)"`, `"Upload Voicemail"`.
4. Build `LiveCallDialog`:
   - Displays caller identity `"Unknown Number (+91-98765-XXXXX)"`.
   - Streaming transcript container updating every 5 seconds.
   - Severity Meter: `LinearWavyProgressIndicator` dynamically shifting from yellow to red as severity score increases.
   - Prominently label `"SIMULATED LIVE CALL - DEMO AUDIO STREAM"`.

## 6. Behavior rules and edge cases
- Follow Rule R5: all synthetic call logs and simulated calls must have visible badges.
- When CSV is imported, immediately trigger engine call-log analysis and update UI flags.
- Healthy calls (`ishaan` ↔ `diya`) must display green/neutral checkmarks and `Low` risk.

## 7. Tests to write
- `CallsViewModelTest.kt`:
  - `test_stalker_call_log_triggers_high_risk_flags`
  - `test_live_call_stream_updates_severity_progressively`

## 8. Commands
Always redirect output:
```bash
just app-test > build-logs/app-test.log 2>&1
tail -n 40 build-logs/app-test.log
grep -n "error" -A6 build-logs/app-test.log | head -60
```

## 9. Definition of done
- [ ] Calls Screen renders behavioral call history.
- [ ] Stalker call pattern flagged with reasons.
- [ ] Simulated Live Call dialog streams transcripts and updates severity meter.
- [ ] Verified via `just app-smoke`.

## 10. Do NOT
- Do NOT claim or simulate covert call recording.
- Do NOT build custom progress meters when `LinearWavyProgressIndicator` exists.

## 11. How to report back
Provide a 10-line summary confirming: Calls screen implementation, live call simulation behavior, and test results.

## 12. If blocked
Write `BLOCKED.md` with error details and stop.
