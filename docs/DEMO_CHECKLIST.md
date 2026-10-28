# Pre-Demo Verification Checklist (DEMO_CHECKLIST.md)

Perform this checklist 15 minutes before presenting to judges.

---

## 1. Hardware & Environment Checks
- [ ] Laptop connected to power adapter.
- [ ] Test Android device battery ≥ 50%.
- [ ] Device "Do Not Disturb" (DND) enabled to block external notifications.
- [ ] Media and ringer volume set to 60% for audio playback demo.
- [ ] USB debugging enabled; `adb devices` shows 1 authorized device.

---

## 2. Port Forwarding & Networking
- [ ] Run `adb reverse tcp:8080 tcp:8080` (chat-server).
- [ ] Run `adb reverse tcp:8000 tcp:8000` (risk engine).
- [ ] Verify curl check:
  ```bash
  curl -s http://localhost:8080/personas | grep meera
  curl -s http://localhost:8000/v1/health | grep status
  ```

---

## 3. Server & Engine Sanity
- [ ] Verify engine models: `GET http://localhost:8000/v1/health/models`.
- [ ] Execute automated scenario dry-run: `just scenario-check`.
- [ ] Clean state reset: `POST http://localhost:8080/demo/reset`.

---

## 4. Android App Sanity
- [ ] Launch Aegis app on device (`android run` or tap icon).
- [ ] Verify Persona Picker defaults to `meera`.
- [ ] Open **Demo Director**: verify scenario cards load.
- [ ] Toggle **Replay Mode** off and on to confirm fixture availability.
- [ ] Confirm screen mirror is active if presenting on projector (`android device hub`).
