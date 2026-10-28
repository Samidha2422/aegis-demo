# T8: Documentation Consolidation & Pitch Synchronization

## 0. Your role in one paragraph
You are the Technical Documentation Teammate responsible for verifying that all specifications, runbooks, checklists, and demo scripts across `docs/` are 100% synchronized with the implemented codebase, that all honesty and privacy rules are strictly upheld, and that the 3-minute hackathon pitch script (`DEMO_SCRIPT.md`) perfectly reflects on-screen flows.

## 1. Project context
Judges evaluate clarity, ethical transparency, and engineering rigor. Discrepancies between what the pitch claims and what the code does severely undermine credibility.

## 2. Repo map and your boundary
- **You may edit:** `docs/UI_SPEC.md`, `docs/SCENARIOS.md`, `docs/DEMO_SCRIPT.md`, `docs/RUNBOOK.md`, `docs/DEMO_CHECKLIST.md`.
- **You must NOT edit:** `android-app/`, `chat-server/`, `contracts/`.

## 3. What already exists
- Initial doc skeletons in `docs/`
- Problem statement deliverables 1–5 in `docs/PRODUCT.md`

## 4. Exact deliverables
1. Verified `docs/UI_SPEC.md` reflecting actual composable hierarchy and component mappings.
2. Verified `docs/SCENARIOS.md` matching exact step numbers and timestamps.
3. Polished `docs/DEMO_SCRIPT.md` timed to 180 seconds.
4. Complete `docs/DEMO_CHECKLIST.md` with step-by-step verification commands.
5. Fully tested fallback instructions in `docs/RUNBOOK.md`.

## 5. Step-by-step instructions
1. Cross-reference `docs/SCENARIOS.md` against `scenarios/*.yaml` to ensure actor IDs, message counts, and expected tiers match exactly.
2. Audit `docs/UI_SPEC.md` against Jetpack Compose code to ensure every custom component justification remains valid.
3. Review `docs/DEMO_SCRIPT.md` to ensure presentation timing allows for 3 minutes without rushing.
4. Verify all emergency helplines in `docs/DEMO_CHECKLIST.md` and ensure honesty labels (`SIMULATED`, `DEMO PROXY`) are noted.

## 6. Behavior rules and edge cases
- Keep `verify before demo` strictly inside `DEMO_CHECKLIST.md`. Never let internal instructions leak into user-facing text.
- Ensure all markdown links resolve correctly.

## 7. Tests to write
- Markdown link audit: verify zero broken relative links in `docs/`.

## 8. Commands
```bash
# Link audit verification
python -c "import os, glob; [print(f) for f in glob.glob('docs/**/*.md', recursive=True)]"
```

## 9. Definition of done
- [ ] All 5 docs reviewed and synchronized.
- [ ] Zero broken links.
- [ ] Demo script verified at 3 minutes.

## 10. Do NOT
- Do NOT alter core API contracts (`docs/API_CHAT.md`).
- Do NOT remove honesty or simulation disclosures.

## 11. How to report back
Provide a 10-line summary confirming: doc review status, link audit results, and script timing check.

## 12. If blocked
Write `BLOCKED.md` with error details and stop.
