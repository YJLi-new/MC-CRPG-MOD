# P45 Review Completion Audit — 2026-06-17

Source: `C:\Users\lanla\Downloads\current_project_review_2026-06-17.md`.

## Requirement-by-requirement evidence

| Review requirement | Current evidence | Status |
|---|---|---|
| Gateway blank player token must not pass outside explicit local dev. | `GatewayConfig.localDevBlankTokenAllowed`; `GatewayServer.authorizePlayer`; hardened `GatewaySmoke` asserts blank `/v1/chat/message` returns 401 `auth_required`. | Complete |
| Player token must bind to requested player UUID. | `DeviceAuthService.tokenValidForPlayer`; `GatewayServer.authorizePlayer`; hardened `GatewaySmoke` asserts wrong-player token returns 401 `wrong_player_token`. | Complete |
| Player vs server token boundaries for admin endpoints. | `GatewayServer.authorizeServer` checks `X-Ebb-Server-Token`/bearer/body/query server token; memory/knowledge/profile/session admin endpoints call it; `GatewaySmoke` asserts memory inspect without server token is rejected. | Complete |
| Memory/knowledge/profile endpoints auth-gated. | `handleMemory*`, `handleKnowledge*`, `handleNpcProfile*`, `handleChatSession` all call `authorizeServer` or `authorizePlayerOrServer`. | Complete |
| Local dev path remains available when explicitly configured. | Default `dev_local` + loopback can use blank-token escape hatch; original gateway smoke still passes without server token. | Complete |
| Memory retrieval before provider prompt. | `GatewayServer.handleChatMessage` calls `memoryStore.search(MemorySearchRequest.forChat(...))`, renders with `MemoryPromptRenderer`, then calls provider with `enrichedRequest`. | Complete |
| Memory context/citations in prompt and citation validation. | `GatewayChatRequest.prompt()` has segmented `MEMORY CONTEXT`; `GatewayChatResponse.withValidatedMemoryCitations` filters invented `memory:*` citations. | Complete |
| Real server/world identity instead of hard-coded placeholders. | `LlmConfig` adds `server_id`, `world_id_strategy`, `world_id_override`; `LlmWorldIdentity` computes ids; `HttpLlmGatewayClient` sends request ids. Static audit rejects `minecraft-server`/`minecraft-world` in the HTTP chat client. | Complete |
| Chat rate limits/quota. | `GatewayRateLimiter` gates gateway global and per-player/NPC chat; `LlmChatService` gates mod-side per player/NPC windows; quota output includes limit/snapshot. Hardened `GatewaySmoke` proves HTTP 429 `rate_limited`. | Complete |
| Memory authority/source/validity model. | `MemoryAuthorityPolicy`, expanded `MemoryFact`, and DB migration columns for source_type/authority_rank/certainty/visibility/validity/audit actors. Lower-authority conflicts create claim/safety records instead of superseding higher authority. | Complete |
| Canon conflict model no longer buried in validator map. | `DeterministicMemoryValidator` delegates canonical checks to `MemoryAuthorityPolicy`; tests/static audit updated to check authority ladder instead of old `CANONICAL_FACTS` field. | Complete |
| Minor NPC LLM profile generation endpoint. | `/v1/npc/profile/generate` exists, server-token-gated, returns character/stance/knowledge_seed/suggested_options/speech_rules/safety_flags with deterministic fallback evidence. | Complete |
| Hidden KB guardrails for profile generation. | `profileLeaksForbiddenSecret`; hardened `GatewaySmoke` asserts generated profile has `validated_no_hidden_kb_leak`. | Complete |
| Prompt trusted/untrusted segmentation. | `GatewayChatRequest.prompt()` now separates developer instruction, trusted profile, trusted visible scene/KB, retrieved memory, and untrusted player utterance. | Complete |
| Memory extraction parser hardening. | `LlmMemoryOperationExtractor.structuredMemoryWrites` uses central `HttpJson.stringArrayValue`, strips blanks, limits field length and item count before validation. | Complete |
| Memory proof mini-route/evidence. | Hardened `GatewaySmoke` proves first chat writes memory and second chat returns `memory=recall` with `memory:record` citation. `gui_e2e_run.py --scenario memory_proof --allow-stale-runtime` creates the P45 manifest/report. | Complete |
| Verification integrated. | `scripts/p45_review_hardening_audit.py` added and invoked by `scripts/run_smoke_checks.sh`; `GoalStaticAudit` updated for the authority policy. | Complete |

## Validation evidence collected in this phase

- `source scripts/env.sh && (cd ebb-llm-gateway && ../.tools/gradle-9.5.1/bin/gradle compileJava compileTestJava gatewaySmoke --no-daemon)` → BUILD SUCCESSFUL.
- `scripts/gradle-local.sh --no-daemon compileJava compileClientJava` → BUILD SUCCESSFUL.
- `python3 -m py_compile scripts/gui_e2e_run.py scripts/p45_review_hardening_audit.py scripts/goal_static_audit.py scripts/p43_llm_safety_audit.py` → pass.
- `python3 scripts/p43_llm_safety_audit.py` → pass.
- `python3 scripts/p45_review_hardening_audit.py` → pass.
- `scripts/gradle-local.sh --no-daemon test --tests com.crpg.ebb.DeepResearchDataTest` → BUILD SUCCESSFUL.
- `scripts/gradle-local.sh --no-daemon build` → BUILD SUCCESSFUL.
- `scripts/gradle-local.sh --no-daemon validateEbbData` → BUILD SUCCESSFUL.
- `python3 scripts/goal_static_audit.py` → pass.
- `scripts/run_smoke_checks.sh` → pass, including P45 static audit and hardened gateway smoke.
- `scripts/gradle-local.sh --no-daemon runGametestServer --args nogui` → BUILD SUCCESSFUL; all 14 required GameTests passed.
- `scripts/run_gui_automation_smoke.sh` → pass.
- `python3 scripts/gui_e2e_run.py --scenario memory_proof --allow-stale-runtime` → report generated at `build/gui-e2e/memory-proof-report.json`.
- `git diff --check` → pass.

## Artifact hashes after P45 build

```text
548d39bc66f548e041d8a190f8c959514e03fd4482534ff015311814bb5c2758  build/libs/ebb-0.1.0-dev.jar
a026d35abdbbeb6e2d5e03a2acd0de5fa70600841bad72f4a3d16b05e7e8f97a  build/libs/ebb-0.1.0-dev-sources.jar
```

## Remaining notes

No requirement from the review document is knowingly left unimplemented. The actual Windows GUI memory proof is represented as a dry-run manifest plus gateway smoke proof in this pass; the existing `llm_chat` GUI route remains the visual route for live-background/free-chat UI, while `memory_proof` is ready for a future save-specific visual route if desired.


## Post-audit Windows GUI pass — 2026-06-18

A later actual Windows GUI test pass is documented in `docs/windows_gui_test_result_2026-06-18.md`.
It produced a new jar because `/ebb summon_npc <routine>` was hardened to accept slash-containing routine ids and GUI automation was fixed to fail on failed report steps.

```text
9795116871dbac74ab7e34fa6ec602b30d7851f2a794651982544d78c96d2932  build/libs/ebb-0.1.0-dev.jar
01116f0b28668a4dbf832bc39da7ebbd2e24f04f7018283d1d20939f536738d1  build/libs/ebb-0.1.0-dev-sources.jar
```

Windows GUI reports after the pass: `runtime-check-report.json` 1/0, `gui-retest-report.json` 282/0, `llm-chat-report.json` 131/0, `p43-llm-validation-report.json` 110/0, `memory-proof-report.json` 3/0 (steps/failures).
