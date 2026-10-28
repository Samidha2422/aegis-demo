# T4: Feed Screen (Comments & Pile-On Protection)

## 0. Your role in one paragraph
You are the Android UI Teammate responsible for building the **Feed Screen** in `android-app/app/src/main/java/com/aegis/demo/ui/feed/FeedScreen.kt` and its ViewModel. This screen demonstrates **Scenario 2 (The Pile-On)**, showing a social post targeted by 10 bot accounts within 60s, triggering rate-limiting banners, collapsed abusive replies, and high-tier defensive mode using Material 3 Expressive components.

## 1. Project context
Harassment frequently occurs as coordinated public pile-ons. The engine flags sudden volume spikes from distinct accounts targeting a single user, allowing the platform to protect the post.

## 2. Repo map and your boundary
- **You may edit:** `android-app/app/src/main/java/com/aegis/demo/ui/feed/`, `android-app/app/src/test/java/com/aegis/demo/ui/feed/`.
- **You must NOT edit:** `android-app/app/src/main/java/com/aegis/demo/ui/chat/`, `chat-server/`.

## 3. What already exists & M3 Expressive Skills
- In accordance with installed `adaptive` and `edge-to-edge` skills:
  - Top bar: `MediumFlexibleTopAppBar`
  - Insets: Use `Scaffold { innerPadding -> LazyColumn(contentPadding = innerPadding) }`
  - Components: `ElevatedCard` for post container, `AssistChip` for safety badges, `FilledTonalButton` for "Show hidden replies", `LinearWavyProgressIndicator` for rate-limit threshold.
- API models: `Post`, `Comment`, `FeedProtectionState`.

## 4. Exact deliverables
1. `FeedScreen.kt` (`@Composable fun FeedScreen(viewModel: FeedViewModel, onNavigateBack: () -> Unit)`)
2. `FeedViewModel.kt` (`StateFlow<FeedUiState>`)
3. `FeedScreenTest.kt` (Unit test verifying comment collapse when protection engages)

## 5. Step-by-step instructions
1. Create `FeedScreen` with `Scaffold` and `MediumFlexibleTopAppBar`.
2. Render Meera's original post in an `ElevatedCard`.
3. Display real-time comments stream in a `LazyColumn`.
4. When `protectionState.isRateLimited == true`, display the expressive warning banner:
   `"High velocity detected (10 comments/min). Replies temporarily gated."`
5. Render toxic bot comments in a collapsed container with button `"10 comments held for review. Tap to reveal"`.
6. Tapping reveal button shows comments with `Warned` safety chips.

## 6. Behavior rules and edge cases
- Follow `jetpack-compose-performance`: provide `key = { it.id }` and `contentType` in `LazyColumn`.
- Empty state: Show clean message if post has 0 comments.
- Must display honest label `"SIMULATED FEED & BOT COMMENTS"`.

## 7. Tests to write
- `FeedViewModelTest.kt`:
  - `test_rapid_comments_triggers_protective_rate_limit`
  - `test_reveal_comments_toggles_expansion_state`

## 8. Commands
Always redirect output:
```bash
just app-test > build-logs/app-test.log 2>&1
tail -n 40 build-logs/app-test.log
grep -n "error" -A6 build-logs/app-test.log | head -60
```

## 9. Definition of done
- [ ] Feed Screen builds and displays post and comments.
- [ ] Pile-on triggers rate-limit banner and collapsed replies.
- [ ] M3 Expressive components used exclusively.
- [ ] Screenshot captured and verified via `just app-smoke`.

## 10. Do NOT
- Do NOT hand-roll custom banner animations (use standard M3 AnimatedVisibility).
- Do NOT hardcode colors; use `MaterialTheme.colorScheme`.

## 11. How to report back
Provide a 10-line summary confirming: Feed screen state handling, M3 Expressive component verification, and test status.

## 12. If blocked
Write `BLOCKED.md` with error details and stop.
