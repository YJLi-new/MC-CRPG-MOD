# P45 Review Hardening Remediation — 2026-06-17

Source review: `C:\Users\lanla\Downloads\current_project_review_2026-06-17.md`.

## Implemented fixes

### Gateway auth boundary
- Added explicit `GatewayConfig` controls for `EBB_GATEWAY_REQUIRE_PLAYER_TOKEN_FOR_CHAT`, `EBB_GATEWAY_REQUIRE_SERVER_TOKEN_FOR_ADMIN_ENDPOINTS`, `EBB_GATEWAY_SERVER_SHARED_SECRET` / `EBB_GATEWAY_SERVER_TOKEN`, and `EBB_GATEWAY_ALLOW_BLANK_TOKEN_ONLY_LOCAL_DEV`.
- Added centralized player/server authorization helpers in `GatewayServer`.
- Blank player tokens are only accepted through the explicit local-dev loopback escape hatch; hardened config rejects them.
- Player tokens are now checked against the requested Minecraft player UUID.
- Memory inspect/conflicts/episodes/lessons/correct/ingest, knowledge update/inspect, chat session, and NPC profile admin routes are server-token gated when configured.

### Memory retrieval into prompt
- Gateway chat now queries `MemoryStore.search(MemorySearchRequest.forChat(...))` before provider invocation.
- Retrieved memories are rendered by `MemoryPromptRenderer` as quoted non-instruction context.
- `GatewayChatRequest.prompt()` now separates developer instruction, trusted NPC profile, visible scene/KB, retrieved memory context, and untrusted player utterance.
- Memory citations returned by the provider are filtered by `GatewayChatResponse.withValidatedMemoryCitations`.

### Server/world identity
- Added `LlmWorldIdentity` and LLM config fields `server_id`, `world_id_strategy`, and `world_id_override`.
- `HttpLlmGatewayClient` now sends request-specific server/world ids rather than fixed `minecraft-server` / `minecraft-world` constants.

### Rate limits and quota
- Added gateway-side fixed-window global and per-player/NPC rate limiting.
- Added mod-side per-player and per-player/NPC LLM chat rate limiting before provider calls.
- Quota endpoint exposes current limits and rate-limit snapshot.

### Memory authority model
- Added `MemoryAuthorityPolicy` with authority ladder: `SYSTEM_CANON`, `SCRIPTED_EFFECT`, `SERVER_EVENT`, `NPC_OBSERVED`, `PLAYER_CLAIM`, `LLM_INFERRED`, and `DEV_NOTE`.
- Expanded `memory_facts` with source/authority/certainty/visibility/validity/audit metadata.
- Low-authority conflicting claims no longer supersede higher-authority facts; they create auditable conflicts/safety lessons.

### Minor NPC profile generation endpoint
- Added `/v1/npc/profile/generate` server-token-gated endpoint.
- It returns a schema-shaped generated profile surface with character, stance, knowledge seed, suggested options, speech rules, safety flags, and deterministic fallback evidence.
- Hidden-KB leak guardrails are covered in gateway smoke.

### Memory proof route
- Gateway smoke now proves a two-turn memory recall path: first turn writes memory, second turn retrieves memory before provider, fake reply contains `memory=recall`, and response citations include `memory:record:*`.
- Added GUI automation dry-run manifest `--scenario memory_proof` for future visual proof routing.

## Verification added
- `scripts/p45_review_hardening_audit.py` static audit.
- `GatewaySmoke` hardened-config checks for blank-token rejection, wrong-player token rejection, server-token-gated memory inspect, profile generation hidden-KB guardrail, memory recall, and rate limiting.
- Updated `DeepResearchDataTest` to validate the authority-policy replacement for the old hard-coded validator assertion.

## Current validation evidence
- Gateway compile/test/smoke: passed.
- Mod compileJava/compileClientJava: passed.
- `python3 scripts/p43_llm_safety_audit.py`: passed.
- `scripts/gradle-local.sh --no-daemon test --tests com.crpg.ebb.DeepResearchDataTest`: passed.
- `python3 scripts/p45_review_hardening_audit.py`: passed.
- `python3 scripts/gui_e2e_run.py --scenario memory_proof --allow-stale-runtime`: generated manifest/report; no GUI interaction performed.
