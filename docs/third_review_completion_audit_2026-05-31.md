# Third Review Completion Audit — 2026-05-31

Objective audited: `C:\Users\lanla\Downloads\ebb_project_review_2026-05-31_third.md` plus the user's instruction to fix all described issues and implement all proposed schemes.

## Scope and evidence standard

This audit treats the active checkout `/mnt/e/MC/PCL/CRPG_MOD` as authoritative source. The third review's central concern was not only feature behavior, but **source/documentation divergence**, especially between a Drive source sample and documentation claims. Therefore evidence must cover both runtime wiring and reproducible checks.

The post-reconciliation source also needs to be pushed/synced so a later Drive sample sees the same files audited here.

## Requirement-by-requirement audit

| Requirement / risk from third review | Evidence in current source | Verification gate | Result |
|---|---|---|---:|
| Read the third review file and map findings | `.kiro/plan/progress.md` records reading `C:\Users\lanla\Downloads\ebb_project_review_2026-05-31_third.md`; this audit and `docs/third_review_reconciliation_2026-05-31.md` map the findings. | File exists and progress entry present. | Complete |
| `ClientTargetDetector` must not generate `ebb:debug/entity` for arbitrary pickable entities | `src/client/java/com/crpg/ebb/client/interaction/ClientTargetDetector.java` checks `ClientEntityTargetIndex.byUuid(...)` first, otherwise `EntityBindingRegistry.resolve(...)`; there is no hardcoded `EbbMod.id("debug/entity")` target creation. | `scripts/third_review_static_audit.py` forbids that stale pattern and requires the new prediction calls. | Complete |
| Entity highlight/prompt prediction must use server-synced registered UUIDs or explicit client binding/fallback | Detector uses `SyncedEntityTarget` from `ClientEntityTargetIndex` for dialogue id, interaction range, and highlight range; otherwise it uses `EntityBindingRegistry.resolve(...)` only. | Static audit plus Gradle build. | Complete |
| Unbound entities must remain inert when fallback is false | `EntityBindingRegistry.resolve` only returns debug fallback when `InteractionSettings.enableDebugEntityFallback()` is true; bundled `data/ebb/interactions/settings/default.json` sets fallback false; detector predicate requires synced UUID or registered binding. | `SecondReviewSmoke` verifies fallback false; static audit checks detector/binding path. | Complete |
| Block-group client prediction must use the synced client index | `ClientTargetDetector.detectBlockGroup` calls `ClientBlockGroupIndex.byBlock(...)`. | Static audit requires `ClientBlockGroupIndex.byBlock`. | Complete |
| Sync payload classes must be registered | `ModPackets.registerPayloadTypes()` registers `BlockGroupSyncPayload`, `EntityBindingSyncPayload`, and `EntityTargetSyncPayload`. | Static audit checks exact registrations. | Complete |
| Client receivers must rebuild and clear sync state | `ClientInteractionNetworking` receives block-group, entity-binding/settings, and entity-target sync payloads; it clears `ClientBlockGroupIndex`, `ClientEntityTargetIndex`, and synced entity bindings/settings on JOIN and DISCONNECT. | Static audit checks receivers and clearing. | Complete |
| Server sync service must send full interaction sync and periodic target snapshots | `InteractionSyncService` sends block groups, entity bindings/settings, and entity targets on data-pack sync/reload; it also scans nearby server-matched entities and sends `EntityTargetSyncPayload` every 20 ticks. | Static audit checks lifecycle and sync calls; `runServer --args nogui` verifies server initialization reaches EULA gate. | Complete |
| `EbbMod` entrypoint must register entity types, packets, data reloads, dialogue lifecycle, sync lifecycle, and commands | `EbbMod.onInitialize()` calls `ModEntityTypes.register()`, `ModPackets.register()`, `NarrativeDataRegistries.registerReloadListeners()`, `DialogueService.registerLifecycleEvents()`, `InteractionSyncService.registerLifecycleEvents()`, and `ModCommands.register()`. | Static audit checks all calls. | Complete |
| `EbbClient` entrypoint must register NPC renderer | `EbbClient.onInitializeClient()` calls `ModEntityRenderers.register()`. | Static audit checks call. | Complete |
| Commands must match docs: `/ebb attributes`, `/ebb attr`, spend/grant/set/reset, `/ebb summon_npc` | `ModCommands` includes those literals/subcommands and `ModEntityTypes.NPC.spawn(...)`. | Static audit checks command surface; build compiles command code. | Complete |
| Dialogue `enter_effects` and check outcome effects must match docs | `DialogueNode` parses `enter_effects`; `DialogueCheck` parses `success_effects`, `failure_effects`, `critical_success_effects`, `critical_failure_effects`; `DialogueService` applies choice pre-effects, check outcome effects, and next-node enter effects. | Static audit checks parse/apply surfaces; `ReviewSmoke` validates review-remediation dialogue/effect parsing. | Complete |
| Dialogue session lifecycle and ACTION revalidation must match docs | `DialogueService.registerLifecycleEvents()` handles disconnect, respawn, leave, level change, server stop, and timeout ticks; ACTION choices with `revalidateTarget` revalidate before effects/check resolution. | Static audit checks lifecycle and revalidation code. | Complete |
| Block-group sync limit must remain explicit | `InteractionSyncLimits`, `InteractionSettings.max_blocks_per_group`, `BlockGroupDefinition` invalidation, and `BlockGroupSyncPayload` validation remain present. | `SecondReviewSmoke` validates oversized block group invalidation. | Complete |
| Documentation must be downgraded where GUI retest is not proven | `docs/manual_client_test_result_2026-05-30_second.md`, `docs/second_review_completion_audit_2026-05-30.md`, and `docs/third_review_reconciliation_2026-05-31.md` explicitly state the pre-hotfix GUI failure and post-reconciliation GUI retest pending. | Manual file inspection; this audit. | Complete |
| Source/document divergence risk must be made reproducible | Added `scripts/third_review_static_audit.py`, which fails if stale Drive-sample wiring patterns reappear. | Script execution passes in current checkout. | Complete |
| Build/smoke/profile evidence must be current | Recorded in `.kiro/plan/progress.md` and `docs/third_review_reconciliation_2026-05-31.md`. | Commands listed below. | Complete for command-line/source verification |
| Full Windows/PCL GUI retest | Requires operating Minecraft GUI: relaunch `26.1.2-Fabric-Ebb-Test`, aim at tagged/summoned NPC, verify cyan highlight/prompt, press `X`, and record result. | Not possible to prove from command-line-only automation; needs human GUI run. | Pending external manual verification |

## Verification results from current reconciliation

- `scripts/third_review_static_audit.py` → `ThirdReviewStaticAudit passed: runtime wiring and documented command/effect surfaces are present.`
- `scripts/gradle-local.sh --no-daemon build` → `BUILD SUCCESSFUL in 46s`.
- `ReviewSmoke` → `ReviewSmoke passed: dialogues=3, attributes=8, block_groups=1, entity_bindings=2, npc_routines=1, messages=1`.
- `AttributePointsSmoke` → `AttributePointsSmoke passed: attributes=8, points=8`.
- `SecondReviewSmoke` → passed, including entity target sync payload construction.
- `scripts/configure_pcl_test_client.sh` refreshed `26.1.2-Fabric-Ebb-Test`; build jar and installed profile jar both hash to `01d880f0e424e0b6ff3592e9bf89189199254167ebb891baffa18330231758b0`.
- Jar inspection confirmed `EntityTargetSyncPayload`, `ClientTargetDetector`, and `ClientInteractionNetworking` classes are packaged.
- `scripts/gradle-local.sh --no-daemon runServer --args nogui` → `BUILD SUCCESSFUL in 1m 44s`; Ebb initialized and the server stopped at the normal EULA gate.
- `git diff --check` → no errors.

## Google Drive source-tree verification

After pushing commit `4085b5fbe5f515fa1966d583e8c5ce66298f7aed`, I synced the tracked repo tree to Google Drive folder `1cGZxWHdCeYYI3ttzL6ilXEGjkOlCz2nt`, then pulled that Drive folder back into `build/tmp/drive-third-audit` and verified the Drive copy directly:

- Drive pull file count: `129` files.
- `python3 build/tmp/drive-third-audit/scripts/third_review_static_audit.py` → `ThirdReviewStaticAudit passed: runtime wiring and documented command/effect surfaces are present.`
- Critical file SHA-256 comparisons between local checkout and Drive mirror all matched: `ClientTargetDetector.java`, `ClientInteractionNetworking.java`, `ModPackets.java`, `InteractionSyncService.java`, `EbbMod.java`, `EbbClient.java`, `ModCommands.java`, `third_review_static_audit.py`, and the third-review audit/reconciliation docs.
- The Drive mirror's `ClientTargetDetector.java` was explicitly checked for the stale hardcoded `EbbMod.id("debug/entity")` pattern; it is absent.

This directly addresses the third review's Drive/source divergence concern for the currently synced Drive tree.

## Completion decision

All third-review P0/P1 code, wiring, documentation-consistency, and command-line verification requirements are satisfied in the current checkout. The only remaining evidence gap is the full Windows/PCL GUI retest, which must be performed by a human GUI operator. Until that retest is recorded, the broader thread goal remains active rather than marked complete.
