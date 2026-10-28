# Aegis 3-Minute Hackathon Demo Script (DEMO_SCRIPT.md)

**Time Limit:** Exactly 3 minutes (180 seconds)  
**Presenter:** Lead Engineer / Demo Orchestrator  
**App Setup:** Phone mirrored via `android device hub` (or physical phone in hand), running on `adb reverse` (or Replay Mode fallback).

---

## Story Overview (The 3-Minute Arc)

| Time | Segment | Focus | Screen / Action | Key Talk Track |
|---|---|---|---|---|
| **0:00 - 0:35** | **Challenge C1: The Polite Stalker** | Behavioral AI beats keyword filters | Open Chat with `rohan` & Demo Director comparison | *"Most harassment filters rely on keyword blacklists. Here, Rohan sends 80 relentlessly polite messages over 3 hours. Meera never replies and blocks him. A keyword filter flags 0 messages. But Aegis analyzes contact velocity, lack of reciprocity, and post-block evasion—escalating risk from Review to High and gating new incoming messages."* |
| **0:35 - 1:05** | **Multimodal Defense: OCR Meme & Image Blur** | Multimodal threat detection | Switch to `rohan` image & `stranger_77` | *"Harassment frequently circumvents text filters by placing abuse inside images. When an abusive screenshot meme arrives, our OCR engine extracts the text, recognizes the harassment, and blurs the image with plain-language explanations. Unsolicited NSFW images from strangers are automatically blurred with 'View anyway' and one-tap report."* |
| **1:05 - 1:35** | **Beyond Messaging: Behavioral Call Log** | Cross-modality behavioral analysis | Open **Calls Screen** | *"The exact same behavioral engine extends to phone calls. When an unknown caller dials 40 times overnight with 92% unanswered calls, Aegis surfaces the pattern immediately, categorizing it as high-risk stalking without listening to private conversations."* |
| **1:35 - 2:00** | **Precision: The Healthy Couple** | Controlling false positives | Switch to `ishaan` chat & call history | *"High volume isn't always harassment. Ishaan and Diya exchange hundreds of messages and hour-long calls. Because our model measures mutual reciprocity and balanced response times, it stays strictly Low-risk, proving high precision."* |
| **2:00 - 2:30** | **Actionable Defense: Evidence Export** | Incident reporting & victim protection | Open **Safety Center** → Tap **Export Evidence** | *"When a victim needs help, Aegis compiles a tamper-evident, cryptographically chained timeline export in JSON or HTML containing timestamps, OCR extracts, voice transcripts, and call logs ready for law enforcement."* |
| **2:30 - 3:00** | **Honesty, Scope & Real-World Limits** | Credibility & Roadmap | Settings & Q&A | *"We believe in honest AI: all audio shown today is synthetic, and our app respects OS boundaries by analyzing call logs and uploaded recordings rather than covertly tapping live phone calls. In the future, this engine can integrate directly with telecom carriers and social platforms."* |

---

## Presenter Talk Track on Limits & Ethics

When judges ask about detection boundaries:
1. **No Covert Tapping:** *"We do not tap live phone calls or intercept encrypted WhatsApp/Instagram DMs. Live call protection is an OS/carrier-level capability."*
2. **Privacy First:** *"Aegis stores transcripts, perceptual hashes, and behavioral metadata, not raw user media."*
3. **Synthetic Data Disclosure:** *"All demo personas and test media are synthetic benchmarks to ensure zero ethical risk."*
