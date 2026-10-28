# Aegis Product Specification: What the Demo Proves

**Hackathon Track:** Hacksploit 2026 – Problem Statement 5 (Detection & Prevention of Personal & Social Harassment)

Aegis Demo serves as the interactive frontend client and social simulation harness for the **Multimodal Harassment Risk Engine**. Its primary mission is to prove that multi-signal, context-aware AI outperforms traditional keyword moderation, protects victims across multiple modalities, and provides transparent, actionable evidence without breaching user privacy.

---

## 1. Problem Statement Deliverables Mapped to Demo Proofs

| Deliverable | Requirement | How the Demo Proves It | Scenario / Screen |
|---|---|---|---|
| **D1: Behavioral AI Analysis** | Detect persistent, one-sided, escalating contact even when individual messages appear polite. | Rohan sends 80 courteous messages over 3 simulated hours. Recipient does not reply and blocks at message ~30. Keyword filter flags 0 messages; Aegis Engine raises risk from `Review` to `High`, applies message request gating, and holds further messages. | Scenario 1 (`rohan`), Chat Screen, Safety Center, Demo Director C1 panel |
| **D2: Multimodal Threat Filtering** | Detect threats across text, screenshots/memes, unsolicited media, synthetic audio, and phone calls. | - Unsolicited image blurred with "View anyway" (Scenario 3)<br>- Abusive text embedded in an image extracted via OCR and flagged (Scenario 4)<br>- Threatening voice note transcribed; transcript rendered with "Play anyway" button (Scenario 5)<br>- Frequent late-night repeated calls flagged in Call Log (Scenario 6) | Chat Screen, Calls Screen, Media Viewer |
| **D3: Sentiment & Slang Adaptation** | Adapt rapidly to emerging code words and regional harassment terms without requiring full model retraining. | Moderator (`mod_asha`) confirms an emerging harassment slang term ("vexling"). The engine incorporates the term, and the very next message containing "vexling" is immediately caught. | Scenario 8 (`mod_asha`), Chat Screen |
| **D4: PoC Detection & Reporting Pipeline** | Deliver end-to-end detection, client-side intervention, and victim-facing evidence generation. | - **Pre-send nudge (F11)**: Warns user typing abusive draft before sending.<br>- **One-tap Report & Block** with refusal signals sent to engine.<br>- **Evidence Export**: Produces a cryptographically hash-chained timeline export in JSON/HTML for reporting to cybercrime authorities. | Safety Center, Chat composer, Evidence Export bottom sheet |
| **D5: False-Positive Control & Precision** | Differentiate high-volume healthy interactions from obsessive harassment. | Ishaan and Diya exchange high-volume rapid messages and long answered calls. The engine measures reciprocity, mutual replies, and balanced duration, remaining strictly in the `Low` risk tier. | Scenario 7 (`ishaan` & `diya`), Chats List, Calls Screen |

---

## 2. Core Value Propositions

1. **Context Over Keywords:** Harassment is behavioral and relational. Single-word blacklists fail against modern online harassment.
2. **Victim-Centric Interventions:** Instead of blunt censorship, provide graded, protective UI: soft warning chips, media blurring, message throttling, and transcript shielding.
3. **Transparent Explanations:** Every risk score is accompanied by plain-language justifications and module contribution meters.
4. **Resilient Architecture:** Fully functional offline replay mode ensures the demo delivers 100% confidence even in challenging hackathon environments.
