# P47 LLM Memory Review Remediation — 2026-06-20

Source review: `C:\Users\lanla\Downloads\current_project_review_llm_memory_2026-06-19.md`.

## Requirement-by-requirement closure

| Review item | Implemented behavior | Evidence |
|---|---|---|
| P0-1 chat-before-provider memory recall | `/v1/chat/message` now calls `memoryStore.recall(MemorySearchRequest.forChat(...))` before `chatProvider.send`. The enriched request includes `MEMORY CONTEXT` and allowed citation ids. | `GatewayServer.handleChatMessage`, `MemoryRecall`, `MemoryPromptRenderer`, `GatewaySmoke` Riverside memory proof. |
| P0-2 structured `memory_ops` parsing | `LlmMemoryOperationExtractor` parses structured `memory_ops` with Gson `JsonParser`, keeps legacy `memory_writes`, and persists unsupported/invalid/low-confidence ops as rejected audit rows. | `runStructuredMemoryOpsSmoke`, `DeterministicMemoryValidator`. |
| P1-1 unified auth / authorization guard | `GatewayAuthGuard` centralizes player token, server token, bearer token, scope, wrong-player, and 401/403 status decisions. Sensitive endpoints call `authHttpStatus(auth)`. | Hardened gateway smoke checks blank token 401 and wrong-player 403. |
| P1-2 actual quota/rate limiting | Gateway chat now reserves per-player minute/daily quota in `quota_windows` and reports `remaining`, `reset_epoch_ms`, `daily_remaining`, and `daily_reset_epoch_ms` through `/v1/player/quota`. | `QuotaDecision`, `MemoryStore.reserveQuota`, `GatewaySmoke` quota assertion. |
| P1-3 persistent gateway state | NPC profiles, chat sessions, NPC knowledge updates, and quota windows are persisted in H2 rather than only in memory maps. | Migration tables `npc_profiles`, `chat_sessions`, `npc_knowledge_updates`, `quota_windows`; `runGatewayPersistenceSmoke`. |
| P1-4 correction semantics | `correctFact` creates an accepted manual correction operation, inserts a replacement active fact, supersedes the old fact, creates a correction conflict, and records a safety lesson; raw episodes stay append-only. | `replacement_fact_id`, `superseded_fact_id`, `correction_conflict_id`, `correction_lesson_id` in smoke result. |
| P1-5 recall priority | Prompt context renders active/current facts first, then open/corrected conflicts, safety/correction lessons, then raw episodes. | `MemoryPromptRenderer.render(MemoryRecall)`. |
| P1-6 status consistency | This doc and `docs/current_status.md` supersede stale Phase 44/45 wording with a P47 status section and verification checklist. | Current status P47 section. |
| P2-1 staging checklist | Added a manual Gateway/OIDC/OpenAI staging checklist below. | This doc + authoring guide. |
| P2-2 prompt injection behavior | Added behavior expectations and tests/docs for quoted scene/memory/player text and high-risk output rejection. | `GatewayChatRequest.prompt`, `p43_llm_safety_audit.py`, P47 audit. |
| P2-3 LLM/script boundaries | Documented that `memory_ops`, `relationship_hints`, and `script_hooks` are advisory only unless validated by server code. | Output-boundary section below and authoring guide. |

## Acceptance test route

The non-GUI gateway proof is deterministic:

1. Chat A sends `fact:player.hometown=Riverside`.
2. Gateway appends raw episode and accepted fact under the current player's canonical subject (`player:<uuid>`).
3. Chat B asks `Do you remember where I am from?`.
4. Before provider execution, recall injects active fact context containing `Riverside` and a `memory:fact:*` citation.
5. Fake provider echoes a `memory=recall` marker and memory excerpt so the smoke test can prove the provider saw the memory.

## Gateway/OIDC/OpenAI staging checklist

Use this only against a staging server/profile, never a bundled demo profile:

1. Start the gateway with explicit production-like env:
   - `EBB_GATEWAY_REQUIRE_PLAYER_TOKEN_FOR_CHAT=true`
   - `EBB_GATEWAY_REQUIRE_SERVER_TOKEN_FOR_ADMIN_ENDPOINTS=true`
   - `EBB_GATEWAY_SERVER_SHARED_SECRET=<server-only secret>`
   - `EBB_GATEWAY_CHAT_PROVIDER=openai_responses` or `mock_openai_responses` for dry-run
   - `EBB_GATEWAY_AUTH_PROVIDER=oidc` or `openai_codex`
   - `EBB_MEMORY_DB_URL=jdbc:h2:...` pointing at a staging DB
2. Verify `/v1/health` returns `memory.status=ok` and no secret values.
3. Use `/ebb llm auth` from the client and verify only the opaque Ebb player token is used server-side.
4. Send an unauthenticated `/v1/chat/message`; expect HTTP 401 `auth_required`.
5. Send a valid token for player A while claiming player B; expect HTTP 403 `wrong_player_token`.
6. Call admin routes without `X-Ebb-Server-Token`; expect HTTP 401.
7. Run the Riverside memory proof route and inspect citations in the dev citations overlay.
8. Confirm `/v1/player/quota` decreases `remaining` after chat and resets on the next minute/day window.
9. Restart the gateway and verify NPC profile, chat session, knowledge update, and quota window rows persist.
10. Disable staging credentials and archive logs with secrets redacted.

## Prompt injection behavior

Scene context, memory context, and the player utterance are explicitly quoted as context, not instructions. Model output may propose `memory_ops`, but all mutations pass through `DeterministicMemoryValidator`; unsupported or high-risk operations are rejected and recorded. Direct quest/item/flag/routine effects from LLM output remain blocked by `GatewayChatResponse.sanitizeProposedEffects` and ignored by the Minecraft-side gateway client.

## LLM output boundaries

- `memory_ops`: advisory proposals only. Accepted only after deterministic validation; correction ops from LLM output are rejected and must go through `/v1/memory/correct`.
- Legacy `memory_writes`: supported for backward compatibility, then validated through the same path.
- `relationship_hints`: future advisory hints only; current authoritative relationship mutations remain scripted server effects.
- `script_hooks`: future advisory hooks only; current authoritative dialogue/quest/routine changes remain scripted server effects.
- `proposed_effects`: not requested from OpenAI structured output; if a future provider sends them, high-risk verbs are filtered and the Minecraft client ignores the field.

## Verification commands

```bash
python3 -m py_compile scripts/p47_llm_memory_recall_audit.py scripts/goal_static_audit.py
source scripts/env.sh && (cd ebb-llm-gateway && ../.tools/gradle-9.5.1/bin/gradle --no-daemon gatewaySmoke)
python3 scripts/p47_llm_memory_recall_audit.py
python3 scripts/goal_static_audit.py
scripts/run_smoke_checks.sh
scripts/gradle-local.sh --no-daemon build
```

## Final verification result

Completed after implementation:

- Gateway `compileJava compileTestJava gatewaySmoke`: passed, including Riverside recall, structured `memory_ops`, auth, quota, correction, and persistence checks.
- Mod `compileJava compileClientJava` and `DeepResearchDataTest`: passed.
- `scripts/p47_llm_memory_recall_audit.py`: passed.
- `scripts/goal_static_audit.py`: passed with P47 guardrails.
- `scripts/run_smoke_checks.sh`: passed.
- `scripts/gradle-local.sh --no-daemon validateEbbData runGametestServer --args nogui`: passed; all 14 required GameTests passed.
- `scripts/run_gui_automation_smoke.sh`: passed.
- `git diff --check`: passed.
- Artifact hashes: jar `fa078b18baf82075b0401c9b75ac44c285e0303ccc5e9bdb6d3368acad13d473`; sources `b98fa79759c454b86bffc21c2854a595c6480619e1d1c2ff73f6a119c60753c3`.
