# Aegis Operational Runbook & Fallback Procedures (RUNBOOK.md)

This runbook outlines how to start the full system, connect the Android app to local servers, and handle any network or environment failures during live hackathon judging.

---

## 1. Standard Live Setup (`adb reverse`)

When presenting with a physical Android phone connected via USB:

1. **Start the Harassment Risk Engine:**
   ```bash
   # Engine runs on port 8000
   cd ../harassment-risk-engine && cargo run --release
   ```
2. **Start the Chat Server:**
   ```bash
   just server-run
   ```
3. **Configure Port Forwarding over USB:**
   ```bash
   adb reverse tcp:8080 tcp:8080
   adb reverse tcp:8000 tcp:8000
   ```
4. **Deploy & Launch Android App:**
   ```bash
   just app-install
   just app-smoke
   ```
5. **Open Screen Mirror (Optional for Projectors):**
   ```bash
   android device hub
   ```

---

## 2. Fallback Procedures

### Fallback A: USB Disconnected or Flaky (Wi-Fi Hotspot with LAN IPs)
1. Turn on personal mobile hotspot on the laptop or phone.
2. Connect both phone and laptop to the hotspot.
3. Determine laptop's LAN IP: `ipconfig` (e.g., `192.168.43.120`).
4. In the Aegis Android App, navigate to **Settings Screen**:
   - Chat Server URL: `http://192.168.43.120:8080`
   - Engine URL: `http://192.168.43.120:8000`
5. Tap **Save & Reconnect**.

### Fallback B: Engine Down or Crashed (Engine Fallback Mode)
1. `chat-server` automatically detects engine connection dropouts via its circuit breaker.
2. Outgoing and incoming messages continue to be delivered without dropping.
3. Messages are tagged with `engine_unavailable`, and the app displays a subtle offline notice banner.
4. Chat does not freeze or crash.

### Fallback C: Total Network / Laptop Failure (Offline Replay Mode)
1. Open the Aegis app on the phone.
2. In the top app bar or inside **Demo Director**, toggle **Replay Mode** to `ON`.
3. The app immediately swaps the network repository for `FixtureRepository`.
4. All 9 scenarios can be played sequentially with realistic timers, safety chips, blur overlays, and bottom sheet signal breakdowns using local JSON fixtures embedded in the APK.
5. **Zero network or laptop connection required.**

### Fallback D: Hardware Failure (Backup Video)
1. Keep a pre-recorded 3-minute screen capture of a perfect run stored in device gallery and on laptop desktop.
