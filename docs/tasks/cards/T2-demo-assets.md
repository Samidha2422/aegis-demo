# T2: Demo Asset Generator Scripts & Override Registrations

## 0. Your role in one paragraph
You are the Assets Engineer teammate responsible for creating the automated generation scripts and synthetic assets in `scenarios/assets/` and `scenarios/tools/`. You produce benign proxy images, screenshot memes with rendered toxic text, synthetic TTS voice clips (16 kHz mono PCM16 WAV), synthetic call logs (CSV), and register their perceptual SHA-256 hashes for the engine's demo overrides.

## 1. Project context
Hacksploit 2026 PS5 requires strict safety and ethical integrity. Real abusive imagery or real voice recordings must NEVER be used. All demo assets are synthetic, benign proxies, or TTS clips, and must carry visible honesty labels.

## 2. Repo map and your boundary
- **You may edit:** `scenarios/tools/`, `scenarios/assets/`, `scenarios/demo_overrides.toml`.
- **You must NOT edit:** `chat-server/`, `android-app/`, `docs/`.

## 3. What already exists
- Specs in `DEMO_APP_PROMPT.md` Section 5 & 8.
- Audio requirement: 16 kHz, mono, PCM16 WAV format.

## 4. Exact deliverables
1. `scenarios/tools/generate_assets.py`: master generation script.
2. `scenarios/assets/proxy_nsfw.png`: benign proxy image representing unsolicited adult content, with visible overlay "DEMO PROXY IMAGE".
3. `scenarios/assets/screenshot_meme.png`: rendered image containing mildly toxic text ("You will never escape our comments") for testing OCR.
4. `scenarios/assets/voice_threatening.wav`: synthetic audio (16 kHz mono WAV) stating "I know where you live. You cannot ignore me forever", labelled synthetic.
5. `scenarios/assets/call_log_stalker.csv`: CSV log containing 40 overnight call records from `unknown_number` with high unanswered ratio.
6. `scenarios/assets/call_log_healthy.csv`: CSV log containing balanced calls between `ishaan` and `diya`.
7. `scenarios/demo_overrides.toml`: computed SHA-256 and pHash entries for the proxy image and meme for engine demo override matching.

## 5. Step-by-step instructions
1. Create Python virtual environment or use existing Python 3.13/3.14 on system.
2. Use Pillow (PIL) to generate `proxy_nsfw.png` (abstract geometric pattern with red banner "DEMO PROXY - SIMULATED EXPLICIT SIGNAL").
3. Use Pillow to generate `screenshot_meme.png` (clean text render on light background).
4. Use standard Python `wave` module or TTS library to generate 16 kHz 16-bit mono WAV clips.
5. Write CSV generator for call records: `timestamp, caller, callee, duration_sec, status`.
6. Compute SHA-256 for all generated files and write `scenarios/demo_overrides.toml`.

## 6. Behavior rules and edge cases
- Every image generated MUST have the text "SIMULATED / DEMO PROXY" clearly rendered into the pixels.
- Audio MUST be 16,000 Hz sample rate, single channel (mono), 16-bit PCM little-endian.
- CSV timestamps must match the 3-hour window of Scenario 1 & 6.

## 7. Tests to write
- `test_audio_format.py`: asserts WAV file is exactly 16000 Hz, 1 channel, 16 bits per sample.
- `test_hash_integrity.py`: verifies SHA-256 in `demo_overrides.toml` matches disk files.

## 8. Commands
Always redirect output:
```bash
python scenarios/tools/generate_assets.py > build-logs/generate-assets.log 2>&1
tail -n 40 build-logs/generate-assets.log
```

## 9. Definition of done
- [ ] All 6 asset files generated in `scenarios/assets/`.
- [ ] `scenarios/demo_overrides.toml` contains correct SHA-256 hashes.
- [ ] WAV files verified 16 kHz mono.

## 10. Do NOT
- Do NOT download or store real explicit or harassing media.
- Do NOT use unlabelled synthetic media.

## 11. How to report back
Provide a 10-line summary listing: generated asset paths, image dimensions, audio durations, and SHA-256 hashes.

## 12. If blocked
Write `BLOCKED.md` with error details and stop.
