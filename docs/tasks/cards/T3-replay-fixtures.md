# T3: Offline Replay Fixtures & FixtureRepository

## 0. Your role in one paragraph
You are the Android Client Teammate responsible for generating the 9 offline scenario fixture JSON files in `android-app/app/src/main/assets/replay/*.json` and implementing `FixtureRepository.kt`. You ensure the entire Aegis Android app can run completely offline with 0 servers, playing every scenario with authentic delays, simulated badges, and safety state transitions.

## 1. Project context
Hackathon demo resilience requires a bulletproof offline fallback. Replay mode allows judges to experience every scenario without network connectivity or external servers.

## 2. Repo map and your boundary
- **You may edit:** `android-app/app/src/main/assets/replay/*.json`, `android-app/app/src/main/java/com/aegis/demo/data/repository/FixtureRepository.kt`.
- **You must NOT edit:** `android-app/app/src/main/java/com/aegis/demo/ui/`, `chat-server/`.

## 3. What already exists
- Common Repository interface: `com.aegis.demo.data.repository.ChatRepository`
- Safety & Message data classes in `com.aegis.demo.data.model.*`
- Scenario specifications in `docs/SCENARIOS.md`

## 4. Exact deliverables
1. 9 JSON files: `scenario_1.json` through `scenario_9.json` in `assets/replay/`.
2. `FixtureRepository.kt` implementing `ChatRepository`:
   ```kotlin
   class FixtureRepository(
       private val context: Context,
       private val json: Json
   ) : ChatRepository {
       override fun getPersonas(): Flow<List<Persona>>
       override fun getChats(): Flow<List<ChatThread>>
       override fun getMessages(chatId: String): Flow<List<SafeMessage>>
       override fun playScenario(scenarioId: Int, speed: Int): Flow<ScenarioProgress>
       override fun resetDemo()
   }
   ```

## 5. Step-by-step instructions
1. Create `assets/replay/` directory inside `android-app/app/src/main/`.
2. Author JSON fixtures matching the exact data schema of `docs/API_CHAT.md`.
3. Ensure `scenario_1.json` has all 80 messages from Rohan with canned escalating safety objects (`Delivered` -> `Warned` -> `Held`).
4. Include late safety update simulation in `scenario_5.json` (starts `Scanning`, updates to `TranscriptOnly`).
5. In `FixtureRepository`, parse fixtures using `kotlinx.serialization.json.Json` and emit updates into `MutableStateFlow` using coroutine delays calculated as `delay_ms / speed`.

## 6. Behavior rules and edge cases
- Speed multiplier (1x, 10x, 60x) must accelerate the delays accurately.
- `resetDemo()` must immediately restore state flows back to initial clean state.
- Every fixture must include `"simulated": true` and appropriate tags (`["SIMULATED", "SYNTHETIC AUDIO", "DEMO PROXY"]`).

## 7. Tests to write
- `FixtureRepositoryTest.kt`:
  - `test_all_9_fixtures_parse_successfully`
  - `test_scenario_1_progression_holds_messages_after_block`
  - `test_reset_restores_initial_state`

## 8. Commands
Always redirect output:
```bash
just app-test > build-logs/app-test.log 2>&1
tail -n 40 build-logs/app-test.log
grep -n "error" -A6 build-logs/app-test.log | head -60
```

## 9. Definition of done
- [ ] All 9 JSON fixture files exist and parse cleanly.
- [ ] `FixtureRepositoryTest` passes 100%.
- [ ] Seamless runtime switching between live and replay modes.

## 10. Do NOT
- Do NOT perform blocking file I/O on the main thread (use `Dispatchers.IO`).
- Do NOT alter UI composables directly.

## 11. How to report back
Provide a 10-line summary confirming: fixture count, total canned messages verified, and test pass confirmation.

## 12. If blocked
Write `BLOCKED.md` with error details and stop.
