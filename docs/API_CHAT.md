# Aegis Chat Platform API Specification (API_CHAT.md)

Voice uploads use `POST /chats/{id}/media` with multipart `file`, `kind=voice`,
`sender_id`, and optional `simulated=true` for synthetic samples. The SafeMessage
contains the engine's masked transcript and safety metadata. Audio bytes are not
retained; missing transcription is marked unscored. Prefer PCM WAV.

**Version:** 1.0.0  
**Base URL:** `http://localhost:8080` (or `http://10.0.2.2:8080` on emulator / LAN IP)  
**Protocol:** REST (JSON) + WebSocket (`/ws`)  

This document serves as the binding contract between the `android-app` client and the `chat-server` platform service.

---

## 1. Authentication & Personas

The demo platform uses pseudo-authentication based on persona selection (no passwords required).

### `GET /personas`
List all demo personas.

**Response `200 OK`:**
```json
[
  {
    "id": "meera",
    "name": "Meera Patel",
    "avatar_url": "assets/avatars/meera.png",
    "role": "victim",
    "is_contact": true,
    "bio": "Designer & active social user"
  },
  {
    "id": "rohan",
    "name": "Rohan Sharma",
    "avatar_url": "assets/avatars/rohan.png",
    "role": "stalker",
    "is_contact": false,
    "bio": "Persistent acquaintance"
  },
  {
    "id": "ishaan",
    "name": "Ishaan Verma",
    "avatar_url": "assets/avatars/ishaan.png",
    "role": "partner",
    "is_contact": true,
    "bio": "Meera's partner"
  },
  {
    "id": "diya",
    "name": "Diya Sen",
    "avatar_url": "assets/avatars/diya.png",
    "role": "partner",
    "is_contact": true,
    "bio": "Friend & colleague"
  },
  {
    "id": "stranger_77",
    "name": "Unknown User (77)",
    "avatar_url": "assets/avatars/unknown.png",
    "role": "cyberflasher",
    "is_contact": false,
    "bio": ""
  },
  {
    "id": "mod_asha",
    "name": "Asha (Moderator)",
    "avatar_url": "assets/avatars/mod.png",
    "role": "moderator",
    "is_contact": true,
    "bio": "Safety operations team"
  }
]
```

### `POST /auth/login`
Select active persona and obtain an active session bearer token.

**Request:**
```json
{
  "persona_id": "meera"
}
```

**Response `200 OK`:**
```json
{
  "token": "bearer_meera_session_token",
  "persona": {
    "id": "meera",
    "name": "Meera Patel",
    "role": "victim"
  }
}
```

---

## 2. Chats & Messaging

### `GET /chats`
Retrieve all active conversations for the authenticated persona.

**Response `200 OK`:**
```json
[
  {
    "id": "chat_rohan",
    "participant": {
      "id": "rohan",
      "name": "Rohan Sharma",
      "avatar_url": "assets/avatars/rohan.png",
      "is_contact": false,
      "is_blocked": false,
      "is_muted": false
    },
    "last_message": {
      "id": "msg_80",
      "sender_id": "rohan",
      "text": "Are you free this weekend?",
      "timestamp": "2026-10-28T10:15:00Z",
      "safety_tier": "high"
    },
    "unread_count": 50,
    "held_count": 30,
    "max_tier": "high"
  }
]
```

### `GET /chats/{id}/messages?after={optional_msg_id}`
Fetch message history for a conversation.

**Response `200 OK`:**
```json
[
  {
    "id": "msg_01",
    "chat_id": "chat_rohan",
    "sender_id": "rohan",
    "recipient_id": "meera",
    "kind": "text",
    "text": "Hey Meera, hope you're having a great day!",
    "media_url": null,
    "transcript": null,
    "timestamp": "2026-10-28T08:00:00Z",
    "safety": {
      "severity": 12.0,
      "tier": "low",
      "reasons": ["Polite tone, initial contact"],
      "signals": {
        "text_toxicity": 0.02,
        "behavior_persistence": 0.15,
        "escalation_slope": 0.0
      },
      "recommended_action": "log",
      "state": "Delivered",
      "simulated": false,
      "tags": []
    }
  }
]
```

### `POST /chats/{id}/messages`
Send a new text message.

**Request:**
```json
{
  "text": "Hello, how are you?"
}
```

**Response `201 Created`:**
```json
{
  "id": "msg_02",
  "chat_id": "chat_rohan",
  "sender_id": "meera",
  "recipient_id": "rohan",
  "kind": "text",
  "text": "Hello, how are you?",
  "timestamp": "2026-10-28T08:05:00Z",
  "safety": {
    "severity": 5.0,
    "tier": "low",
    "reasons": [],
    "signals": {},
    "recommended_action": "log",
    "state": "Delivered",
    "simulated": false,
    "tags": []
  }
}
```

### `POST /chats/{id}/media`
Multipart upload for image or audio voice note.

**Form Data:**
- `file`: binary file
- `kind`: `image` | `voice`
- `duration_ms`: (integer, for audio)

**Response `201 Created`:**
```json
{
  "id": "msg_03",
  "chat_id": "chat_stranger",
  "sender_id": "stranger_77",
  "recipient_id": "meera",
  "kind": "image",
  "media_url": "/media/temp_proxy_3910.jpg",
  "timestamp": "2026-10-28T08:10:00Z",
  "safety": {
    "severity": 82.0,
    "tier": "high",
    "reasons": ["Unsolicited adult/NSFW image from non-contact", "DEMO PROXY / SIMULATED SIGNAL"],
    "signals": {
      "image_nsfw": 0.94,
      "ocr_text": ""
    },
    "recommended_action": "blur_media",
    "state": "Blurred",
    "simulated": true,
    "tags": ["DEMO PROXY", "SIMULATED SIGNAL"]
  }
}
```

---

## 3. Realtime Push (WebSocket `/ws`)

Client connects to `ws://localhost:8080/ws?token=bearer_meera_session_token`.

### Event Types:
1. **`message`**: New incoming message.
   ```json
   {
     "event": "message",
     "payload": { /* Message object */ }
   }
   ```
2. **`safety_update`**: Late engine verdict update (e.g. Whisper transcription finished or OCR completed).
   ```json
   {
     "event": "safety_update",
     "payload": {
       "message_id": "msg_voice_01",
       "safety": {
         "severity": 78.0,
         "tier": "high",
         "reasons": ["Threatening voice clip: 'I know where you live'"],
         "signals": { "audio_toxicity": 0.88 },
         "recommended_action": "warn_sender",
         "state": "TranscriptOnly",
         "simulated": true,
         "tags": ["SYNTHETIC AUDIO"]
       },
       "transcript": "I know where you live. You can't ignore me forever."
     }
   }
   ```
3. **`alert`**: High/Urgent escalation safety alert.
   ```json
   {
     "event": "alert",
     "payload": {
       "id": "alert_01",
       "tier": "high",
       "title": "Persistent Contact Alert",
       "description": "80 messages sent in 3 hours with 0 replies and contact attempts after blocking.",
       "target_user_id": "rohan",
       "timestamp": "2026-10-28T11:00:00Z"
     }
   }
   ```
4. **`call_event`**: Incoming or simulated call state.
5. **`scenario_step`**: Live playback step progress.
   ```json
   {
     "event": "scenario_step",
     "payload": {
       "scenario_id": 1,
       "step_index": 35,
       "total_steps": 82,
       "description": "Rohan sends message #35 after block",
       "tier": "high"
     }
   }
   ```

---

## 4. Moderation, Refusal & Nudge

### `POST /users/{id}/block`
Block user. Server records refusal and forwards to engine `POST /v1/events/refusal`.

### `POST /users/{id}/unblock`
Unblock user.

### `POST /users/{id}/mute`
Mute conversation notifications.

### `POST /reports`
Submit abuse report for a user or message.
```json
{
  "message_id": "msg_03",
  "reason": "unsolicited_nsfw",
  "details": "Sent explicit image without consent"
}
```

### `POST /nudge`
Pre-send draft evaluation (proxied to engine with `draft: true`).
```json
{
  "draft": "You are pathetic and nobody likes you."
}
```
**Response:**
```json
{
  "severity": 75.0,
  "tier": "review",
  "reasons": ["Severe personal insult and harassment draft"],
  "recommended_action": "warn_sender",
  "nudge_text": "This message may hurt someone. Would you like to edit it before sending?"
}
```

---

## 5. Feed, Calls, Evidence & Demo Control

### `GET /feed/posts` & `POST /feed/posts/{id}/comments`
Social comment feed for pile-on demonstration (Scenario 2). Supports rate limiting and protection states.

### `POST /calls/simulate` & `GET /calls/log`
Simulate incoming voice calls and retrieve behavioral call logs.

### `GET /evidence/export?format={json|html}`
Export cryptographic, timestamped incident report suitable for filing complaints.

### `GET /demo/scenarios` & `POST /demo/scenarios/{n}/play?speed={1|10|60}`
Control execution of the 9 scripted behavioral scenarios.

### `POST /demo/reset`
Reset database, active chats, stored media, and engine state back to a clean initial slate.

### `GET /demo/baseline?conversation={id}`
Returns comparison between simple keyword filter (0 flags) vs. Aegis Engine behavioral verdict (Review -> High) for Challenge C1.

## Live engine contract (2026-10-29)

This section supersedes provisional engine integration details. Base URL is
http://localhost:8081 (Android emulator: http://10.0.2.2:8081). Android only calls
chat-server. See RUNBOOK.md for host credentials, JDK/SDK setup and logged checks.

- GET /engine/status returns health {status,version,mock,degraded} and models
  [{name,version,hash_verified,backend,degraded}]. Hash verification is not inference.
- POST /chats/{id}/messages accepts {text,sender_id?}; sender defaults to meera.
  UUID message IDs are also engine idempotency IDs. In chat_rohan, sender rohan
  targets meera; sender meera targets rohan. Other non-owner personas must match
  the thread peer. The selected persona is explicit in requests, not global server state.
- POST /nudge requires {draft,chat_id,sender_id?}. Response adds simulated and
  degraded. Engine errors use HTTP 502/503 {error,detail}; no clean fallback score.
- POST /chats/{id}/media accepts file, kind=image and optional sender_id. Maximum
  image size is 10 MiB. PNG/JPEG/WebP MIME is derived from bytes. The actual image
  bytes are sent to Adapt and retained in a bounded in-memory store (100 MiB total).
  Successful uploads return media_url=/media/{uuid}; GET returns the original bytes
  with their validated MIME and Cache-Control: no-store. URLs are bearer capabilities,
  not authenticated; anyone who knows one can retrieve it. Reset and restart erase the
  store. When full, uploads return 507. Engine failures produce unknown, unscored
  messages, and blocked senders remain held. No proxy is substituted.
- SafetyObject adds degraded[], model_versions{} and modules{}; each module
  contains score, label, status (built/simulated/roadmap), backend and confidence.
  Numeric signals{} remains compatible. Unknown/unscored results use tier=unknown.
- POST /users/{id}/block, unblock and mute forward actual engine refusal events;
  failures are HTTP errors. POST /reports requires message_id and reason. A report
  without an analyzed event ID returns 422; it is never silently accepted.
- Comments use actual engine verdicts. Simulated call inputs remain labelled
  simulated even when real Adapt scoring is used. GET /calls/log starts empty.
- POST /calls/log/import accepts the UTF-8 safe-import CSV expected by Adapt and
  returns its {atomic:false,rows:[{row,event_id,outcome,assessment?,problem?}]} body.
  Row failures do not roll back previous successful rows. No caller-name scoring.
- POST /recordings accepts a multipart audio file and returns an engine-scored
  SafeMessage. Unavailable transcription is reported as unscored.
- GET /evidence/export?format=json|html returns actual Adapt evidence for meera.
  Non-2xx is an error, not an empty successful export. Drafts are absent from evidence.
- POST /demo/reset clears chat and media memory. Adapt history must be managed separately.
  Existing scenario playback is a SIMULATED progress feed, not engine verification.

### Refusal actor body and inbox view

POST /users/{id}/block, unblock and mute accept optional {"actor_id":"rohan"}.
Missing body or actor_id defaults to meera for existing clients. Invalid JSON,
unknown actor or actor==target returns 422 with no refusal side effects. The target
is the refused sender; the actor is the refusal recipient in Adapt. State is per
(actor,target); unblocking does not remove a mute. Android always supplies its
selected persona. GET /chats?actor_id=rohan returns chat_rohan with Meera as the
participant; no query keeps the existing Meera inbox. Conversation IDs stay stable.
