# PLAN.md Completion Audit — 2026-06-17

Source audited: `E:\MC\PCL\PLAN.md` (`/mnt/e/MC/PCL/PLAN.md`).
Repository audited: `CRPG_MOD` at the current working tree.

This document is a requirement-by-requirement completion audit for the active goal: implement every actionable part of PLAN.md while preserving the existing Fabric 26.1.2 / Java 25 / GeckoLib architecture.

## Audit result

Status after the final verification suite: **complete for every actionable PLAN.md requirement, with all mandatory validation commands passing**.

No remaining code/data/docs gap was found after the Phase 44 cleanup. Two strict-surface mismatches found during this audit were remediated before this document was written:

1. PLAN.md listed separate `NpcChatHistoryWidget`, `NpcChatInputWidget`, and `LlmAuthStatusWidget` classes; the implementation now has those named helper surfaces and `NpcChatScreen` / `EbbMenuScreen` use them.
2. PLAN.md used the authoring directory `data/*/npc_knowledge`; the implementation's primary directory remains `data/*/npc_knowledge_packs`, and the reload listener now also accepts the PLAN-compatible alias `data/*/npc_knowledge` with duplicate-id validation.

## Requirement matrix

| PLAN section | Explicit requirement | Current evidence | Result |
|---|---|---|---|
| 0 / project constraints | Keep existing Fabric 26.1.2, Java 25, GeckoLib 5.5.1, mod id `ebb`, package `com.crpg.ebb`; LLM must be server-authoritative, async, disableable, mockable, and secret-safe. | `build.gradle`, `gradle.properties`, `fabric.mod.json`, `LlmConfig`, `LlmChatService`, `DisabledLlmGatewayClient`, `FakeLlmGatewayClient`, `HttpLlmGatewayClient`, `scripts/p43_llm_safety_audit.py`. | Complete |
| 1.1 player experience | NPC free chat supplements scripted dialogue; memory affects later chat/script/relations/Chime/quest/dev visibility. | `llm_chat` dialogue choice in `DialogueService`, `NpcChatScreen`, `NpcKnowledgeService`, relationship delta handling in LLM responses, memory citation/dev commands, P42 GUI evidence. | Complete |
| 1.2 design principles | Scripted-first, server-authoritative, append-only temporal memory, citations/auditability, conflict records, privacy/cost controls. | `DialogueEffect` whitelist path, `LlmChatService` session validation, gateway `MemoryStore`, `MemoryConflict`, `GatewayChatResponse.sanitizeProposedEffects`, `/ebb memory *`, `/ebb llm *`, `p43_llm_safety_audit.py`. | Complete |
| 2.1 NPC tiers | Major/minor/static/disabled tiers; scripted majors; deterministic minor promotion; no all-entity fallback; OP reset/regenerate. | `NpcTier`, `NpcProfileDefinition`, `NpcProfileRegistry`, six bundled profiles, `minor_villager.json`, `NpcPromotionService`, `/ebb npc minorize/promote/review/reject_profile/regenerate_profile/demote`, entity fallback disabled. | Complete |
| 2.2 LLM gateway architecture | No client/mod-jar direct OpenAI credentials; standalone gateway; fake/gateway/real provider modes; OAuth/OIDC flow and commands. | `ebb-llm-gateway`, `GatewayConfig`, `DeviceAuthService`, `DevLocalAuthProvider`, `OidcAuthProvider`, `OpenAiResponsesChatProvider`, `LlmAuthService`, `/ebb llm auth/status/logout/consent/quota/auth_debug`, redacted token summaries. | Complete |
| 2.2.4 chat UI | `NpcChatScreen`, `NpcChatHistoryWidget`, `NpcChatInputWidget`; streaming, timeout/cancel, suggested options, dev citations, memory correction, K menu status. | `src/client/java/com/crpg/ebb/client/gui/llm/*`, `LlmChatChunkPayload`, `LlmChatOptionsPayload`, `LlmChatCancelPayload`, `EbbMenuScreen`, `scripts/gui_e2e_run.py --scenario llm_chat`. | Complete |
| 2.3 memory layers and records | Recent buffer/raw episodes/episodic summaries/facts/relationship state/KB; record/fact/conflict structures; sequence and citation ids. | Gateway `MemoryRecord`, `MemoryFact`, `MemoryConflict`, `MemorySummary`, `MemoryLink`, `MemorySafetyLesson`; mod relationship/KB registries; `MemoryAppendResult`; dev inspect/search/episodes/lessons commands. | Complete |
| 2.3 write/retrieval/conflict | LLM only proposes; validator applies; hybrid retrieval; high-authority facts not silently overwritten; supersede/conflict history. | `LlmMemoryOperationExtractor`, `DeterministicMemoryValidator`, `MemoryStore.applyFactOperation`, `MemoryStore.search`, `MemoryConsolidator`, `GatewaySmoke`, `p39MemoryExtractionConsolidationAndSafetyAreAuditable`. | Complete |
| 2.4 profiles and KB | Profile schema/data; KB schema/data; reveal conditions; story effects to add/update stance/KB/memory. | Six `npc_profiles`; seven `npc_knowledge_packs`; alias `npc_knowledge`; `NpcKnowledgePackDefinition`, `NpcKnowledgeRegistry`, `NpcKnowledgeService`; `npc_kb_add_fact`, `npc_kb_add_pack`, `npc_stance_shift`, docs schema. | Complete |
| 3 gateway API | Auth, quota, NPC profile, chat, memory, knowledge endpoints. | `GatewayServer` registers `/v1/auth/device/start`, `/v1/auth/device/status`, `/v1/auth/logout`, `/v1/player/quota`, `/v1/npc/profile/*`, `/v1/chat/*`, `/v1/memory/*`, `/v1/knowledge/*`; `GatewaySmoke` exercises them. | Complete |
| 4 mod packages/config | LLM, npc profile, memory, knowledge, network payloads, client GUI packages; server/client configs. | Packages under `src/main/java/com/crpg/ebb/{llm,memory,npc/profile,npc/knowledge,network/llm}` and `src/client/java/com/crpg/ebb/client/gui/llm`; `config/ebb-llm-server.json` parsing in `LlmConfig`; client settings. | Complete |
| 5 prompt/structured output | Prompt includes profile/stance/KB/memory/context; structured JSON with reply/mood/options/memory proposals; post-validation for any gateway-level proposed effects. | `GatewayChatRequest.prompt`, `OpenAiResponsesChatProvider` structured schema with `memory_ops`, `GatewayChatResponse.sanitizeProposedEffects`, visible-only `NpcKnowledgeService` context, hidden-KB audit. The OpenAI schema intentionally does **not** request direct `proposed_effects`; gateway response objects keep the field sanitized/empty for future low-risk advisory use. | Complete |
| 6 memory-system mapping | Gateway-owned memory; no Python frameworks in Fabric; append-only, hybrid retrieval, temporal facts, related-memory evolution, safety lessons. | Java gateway memory package; deterministic embeddings for test-safe retrieval; no Python memory framework dependency in mod; A-Mem/A-MemGuard-inspired `MemoryConsolidator`/`MemorySafetyLesson`. | Complete |
| 7 database design | Gateway DB migration with profiles/conversations/memory/facts/conflicts/knowledge concepts and embedding write path. | `V001__memory_store.sql`, `MemoryStore.migrate`, memory/fact/conflict/operation/summary/link/safety tables; profile/session/knowledge API surfaces; `EBB_MEMORY_DB_URL` env config. Production PostgreSQL/pgvector is a recommended deployment target; automated tests use H2/deterministic embeddings to avoid external services. | Complete for MVP/PLAN phases |
| 8 network payloads | LLM auth, chat open/message/chunk/options/close/cancel/error, profile sync, OP memory debug; no tokens/hidden KB. | `src/main/java/com/crpg/ebb/network/llm/*.java`, `ModPackets` registrations, client/server receivers, `p43_llm_safety_audit.py` hidden-KB/no-secret checks. | Complete |
| 9 integration | Dialogue `llm_chat`, relationship suggestions, quest/feat/chime safety, developer tabs/commands. | `DialogueService.open LLM chat`, `relationship_delta_from_llm` handling, high-risk effects ignored/sanitized, `/ebb npc`, `/ebb memory`, `/ebb kb`, `/ebb dev`, K menu. | Complete |
| 10 safety/privacy/cost | No secrets in client/resources/logs; consent text; memory delete/export; poisoning defenses; rate limits/token limits; fake default. | `p43_llm_safety_audit.py`, `LlmConfig` defaults `enabled=false`, fake provider, `LlmRateLimiter`, `/ebb llm consent`, `/ebb memory delete_player/export`, `DeterministicMemoryValidator`, `GatewayChatResponse` sanitization. | Complete |
| 11 P34 | Fake provider, config/interface/session/payload/UI skeleton, `/ebb llm status`, no-network static audit. | `LlmConfig`, `LlmGatewayClient`, `FakeLlmGatewayClient`, `LlmChatService`, `NpcChatScreen`, payload registrations, JUnit/GameTest/static audit. | Complete |
| 11 P35 | NPC profile/tier/promotion data layer and persisted promoted profiles. | `NpcTier`, `NpcProfileDefinition`, `NpcProfileRegistry`, demo profiles, `NpcPromotionService`, `NarrativeSavedData` promoted profiles, commands/tests. | Complete |
| 11 P36 | Gateway health/auth/OIDC/local auth and Minecraft auth commands; server-only token storage. | `GatewayServer`, `DeviceAuthService`, `OidcAuthProvider`, `LlmAuthService`, auth payloads, `/ebb llm auth/status/logout`, gateway smoke. | Complete |
| 11 P37 | Official OpenAI Responses provider, structured/chunked output, circuit breaker, mock tests, `store:false` default. | `com.openai:openai-java`, `OpenAiResponsesChatProvider`, `SimpleCircuitBreaker`, `MockGatewayChatProvider`, `HttpLlmGatewayClient`, P37 JUnit/gateway smoke. | Complete |
| 11 P38 | MemoryStore MVP, embeddings write path, hybrid retrieval, dev commands. | `MemoryStore`, `MemoryEmbeddingService`, `/v1/memory/search/inspect/conflicts`, `/ebb memory search/inspect/conflicts`, citation ids, P38 tests. | Complete |
| 11 P39 | Memory extraction/consolidation, safety lessons, raw/fact/conflict dev visibility. | `LlmMemoryOperationExtractor`, `MemoryConsolidator`, `MemorySafetyLesson`, `/ebb memory episodes/lessons`, P39 smoke/JUnit. | Complete |
| 11 P40 | NPC KB parser/reload/index/reveal/effects/dev inspect. | `NpcKnowledge*`, `npc_kb_add_fact`, `npc_kb_add_pack`, `npc_stance_shift`, `/ebb kb inspect/add_pack`, hidden-secret JUnit. | Complete |
| 11 P41 | Minor candidate detection/profile generator/persistence/dev review/rate limit. | `NpcProfileGenerator`, minor binding/tag detection, promoted saved profiles, `/ebb npc review/reject/regenerate/demote`, world-hour rate limit tests. | Complete |
| 11 P42 | Full LLM chat UI: streaming/options/return/correction/citations/GUI E2E/K status. | `NpcChatScreen` + helper widgets, `LlmChatService.reopenFromLlmChat`, GUI `llm_chat` evidence, timeout/cancel handling. | Complete |
| 11 P43 | Docs/schemas/audits/JUnit/GameTest/GUI dry-run. | `docs/json_authoring_guide.md`, `docs/schemas/ebb.npc_profile.schema.json`, `docs/schemas/ebb.npc_knowledge.schema.json`, `scripts/p43_llm_safety_audit.py`, JUnit/GameTest/GUI manifest. | Complete |
| 12 authoring examples | Minor binding, LLM dialogue choice, KB update examples. | Bundled `interactions/entity_bindings/llm/minor_villager.json`, `dialogues/llm/minor_intro.json`, demo dialogue effects and docs examples. | Complete |
| 13 prompt templates | NPC chat and minor NPC generation prompt skeletons reflected in runtime. | `GatewayChatRequest.prompt`, `NpcProfileGenerator` prompt/schema, docs. | Complete |
| 14 acceptance table | Major NPC chat, minor promotion, auth, memory recall/citation/conflict/order, KB update, safety, health, tests. | Six role profiles with LLM choices, minor promotion tests, auth/gateway tests, memory/KB/JUnit/GameTest/GUI/static audits; final full suite passed. | Complete |
| 15 mandatory commands | `build`, `validateEbbData`, `run_smoke_checks`, `runGametestServer`. | Final rerun completed successfully on 2026-06-17; see verification log below. | Complete |

## Notes on external services

- Real OpenAI calls are implemented through the official Java SDK, but the verification suite intentionally uses fake/mock providers so tests do not consume API quota or require user secrets.
- Production PostgreSQL + pgvector remains the recommended deployment backend in PLAN.md. The committed MVP uses a gateway-owned JDBC store with H2 for local tests and deterministic embeddings for reproducibility; this satisfies the implemented P34-P43 acceptance gates without requiring an external database during CI/local validation.

## Final verification log

```text
scripts/gradle-local.sh --no-daemon build                  -> BUILD SUCCESSFUL
sha256sum build/libs/ebb-0.1.0-dev.jar build/libs/ebb-0.1.0-dev-sources.jar
  build/libs/ebb-0.1.0-dev.jar         57590ae1bc202644c24961f2d9ccd829873ed04843d15b617cefb32ce94cded9
  build/libs/ebb-0.1.0-dev-sources.jar f6a31d48944c44c3c7bf4e14285c6cb47b4b7c3458b1809e69eaaf33dcef15e9
scripts/gradle-local.sh --no-daemon validateEbbData        -> BUILD SUCCESSFUL
scripts/run_smoke_checks.sh                                -> passed; includes gateway smoke, authoring validation, static audits, P43 safety audit, goal static audit, and GUI retest issue audit
scripts/gradle-local.sh --no-daemon runGametestServer --args nogui -> BUILD SUCCESSFUL; all 14 required GameTests passed
scripts/run_gui_automation_smoke.sh                        -> passed; dry-run report generated
python3 scripts/p43_llm_safety_audit.py                    -> passed
git diff --check                                           -> passed
```

Full-suite review notes:

- The first final-suite run exposed a stale JUnit assertion after the LLM auth-status widget split; the assertion was updated to inspect `LlmAuthStatusWidget` as well as `EbbMenuScreen`.
- A temporary strict-schema edit that asked the OpenAI provider for direct `proposed_effects` was reverted because the P43 safety invariant requires direct LLM effects to be absent from the OpenAI schema. `GatewayChatResponse` retains sanitized future-proof support, and Minecraft still ignores proposed effects.
- GameTest promotion assertions initially shared a world-hour rate-limit counter with prior tests; the tests now reset only their own current-hour counter before asserting first-promotion behavior, preserving production rate limiting while restoring deterministic test isolation.
