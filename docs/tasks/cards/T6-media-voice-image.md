# T6: Media Handling: 16 kHz Voice Recorder, Transcript-Only Mode & Image Blur

## 0. Your role in one paragraph
You are the Android Media Teammate responsible for implementing the **16 kHz PCM16 WAV voice-note recorder**, audio player with transcript-only fallback, and the **Image Picker with Compose Blur Overlay** in `android-app/app/src/main/java/com/aegis/demo/ui/media/`.

## 1. Project context
Harassment encompasses multimodal content. To eliminate codec incompatibility risk with the Harassment Risk Engine's Whisper module, all recorded voice notes must be strictly formatted as **16 kHz, mono, 16-bit PCM WAV**. Flagged threatening voice notes shield the victim by showing the transcript instead of playing audio. Flagged explicit images or meme screenshots are blurred with interactive overrides.

## 2. Repo map and your boundary
- **You may edit:** `android-app/app/src/main/java/com/aegis/demo/ui/media/`, `android-app/app/src/main/java/com/aegis/demo/util/AudioRecorder.kt`.
- **You must NOT edit:** `android-app/app/src/main/java/com/aegis/demo/ui/chat/SafeMessageBubble.kt` (provide components that it integrates).

## 3. What already exists & M3 Expressive Skills
- Audio spec: 16,000 Hz, 1 channel, AudioFormat.ENCODING_PCM_16BIT.
- Blur: `Modifier.blur(32.dp)` with semi-transparent tinted container.
- Components: `LinearWavyProgressIndicator` for recording wave amplitude and playback progress, `ButtonGroup` for "View anyway / Report / Block".

## 4. Exact deliverables
1. `AudioRecorder.kt`: records raw PCM from `AudioRecord` and writes standard 44-byte RIFF/WAV header.
2. `VoiceNoteBubble.kt`:
   - Normal state: Play/pause button + duration + wavy indicator.
   - Flagged state (`TranscriptOnly`): Text transcript rendered in a protected bubble + `"Play anyway"` button + `"SYNTHETIC AUDIO"` tag.
3. `ImageAttachmentBubble.kt`:
   - Normal state: renders image via Coil.
   - Flagged state (`Blurred`): Gaussian blur overlay + reason chip (OCR text or NSFW flag) + `"View anyway / Report / Block"` button group.
4. `AudioRecorderTest.kt`: verifies RIFF header byte layout and sample rate constants.

## 5. Step-by-step instructions
1. Implement `AudioRecorder` with Coroutine background thread writing to temporary cache WAV file.
2. Build `VoiceNoteBubble` using `Surface` and `LinearWavyProgressIndicator`.
3. If `safety.state == TranscriptOnly`, conceal audio player and display:
   `"Audio hidden: threatening speech detected. Transcript: <text>"`
   Provide secondary text button: `"Play anyway"`.
4. In `ImageAttachmentBubble`, when `safety.state == Blurred`, apply `Modifier.blur(28.dp)` to the Coil image. Overlay an expressive dark scrim with centered warning text and M3 `ButtonGroup`.
5. Tapping `"View anyway"` removes blur for that message instance.

## 6. Behavior rules and edge cases
- Recording permissions: Check `RECORD_AUDIO` permission gracefully.
- If OCR text is present in `safety.signals.ocr_text`, display it in the reason chip: `"OCR: '<extracted_text>'"`
- Tag all synthetic demo clips with `"SYNTHETIC AUDIO"` or `"DEMO PROXY"`.

## 7. Tests to write
- `AudioRecorderTest.kt`:
  - `test_wav_header_has_correct_samplerate_16000_and_channels_1`
- `MediaBubbleTest.kt`:
  - `test_blurred_state_hides_raw_image_until_override`
  - `test_transcript_only_mode_displays_text_not_audio_controls`

## 8. Commands
Always redirect output:
```bash
just app-test > build-logs/app-test.log 2>&1
tail -n 40 build-logs/app-test.log
grep -n "error" -A6 build-logs/app-test.log | head -60
```

## 9. Definition of done
- [ ] Voice notes record as 16 kHz PCM16 WAV.
- [ ] Flagged voice notes render transcript-only with "Play anyway" override.
- [ ] Flagged images display blur overlay and OCR reasons.
- [ ] Verified with `just app-smoke`.

## 10. Do NOT
- Do NOT use compressed AAC/M4A/MP3 codecs for voice recording (WAV only).
- Do NOT bypass the blur without explicit user click.

## 11. How to report back
Provide a 10-line summary confirming: WAV recording verification, blur overlay interaction, and test results.

## 12. If blocked
Write `BLOCKED.md` with error details and stop.
