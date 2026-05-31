# Third Review Runtime Wiring Reconciliation — 2026-05-31

Source review file: `C:\Users\lanla\Downloads\ebb_project_review_2026-05-31_third.md`.

## Summary

The third review correctly identified the highest-risk failure mode: documentation had advanced to claiming dedicated-style entity prediction fixes, while a Drive source sample appeared to show stale runtime wiring. I re-audited the active checkout at `/mnt/e/MC/PCL/CRPG_MOD`, added an explicit static audit script so this mismatch is machine-checkable, made two small robustness fixes, rebuilt, re-ran smoke checks, and refreshed the playable PCL profile.

This document intentionally separates **code/build/source-audit complete** from **Windows GUI retest pending**. The first GUI attempt already failed before `EntityTargetSyncPayload`; after this reconciliation the profile has been refreshed again, but a human must still relaunch Minecraft and retest the highlight/prompt path.

## Requirement mapping

| Third-review concern | Current action / evidence | Status |
|---|---|---:|
| `ClientTargetDetector` must not create `ebb:debug/entity` for every pickable entity | Current detector checks `ClientEntityTargetIndex.byUuid(...)` first, otherwise `EntityBindingRegistry.resolve(...)`; stale hardcoded debug target is forbidden by `scripts/third_review_static_audit.py`. | Complete |
| Entity prediction must use synced UUIDs or explicit binding/fallback | `ClientEntityTargetIndex` is populated by `EntityTargetSyncPayload`; detector uses synced dialogue/ranges and now also enforces synced/local `highlightRange`. | Complete |
| Block-group client prediction must use synced client index | Detector uses `ClientBlockGroupIndex.byBlock(...)`, not common `BlockGroupIndex`. Static audit checks this. | Complete |
| Sync payloads must be registered | `ModPackets` registers `BlockGroupSyncPayload`, `EntityBindingSyncPayload`, and `EntityTargetSyncPayload`. Static audit checks exact registrations. | Complete |
| Client receivers must rebuild/clear synced state | `ClientInteractionNetworking` receives all three sync payloads and rebuilds client indexes/settings. It now clears synced block groups, entity targets, and entity bindings/settings on both JOIN and DISCONNECT. | Complete |
| Server sync service must send full interaction data and periodic entity targets | `InteractionSyncService` sends block groups + entity bindings/settings + entity target snapshots on data sync/reload; it also sends per-player registered entity target snapshots every 20 ticks. | Complete |
| Entry initialization must wire NPC/sync/dialogue lifecycle | `EbbMod` calls `ModEntityTypes.register()`, `ModPackets.register()`, data reload registration, `DialogueService.registerLifecycleEvents()`, `InteractionSyncService.registerLifecycleEvents()`, and command registration. | Complete |
| Client initialization must wire NPC renderer | `EbbClient` calls `ModEntityRenderers.register()`. | Complete |
| Commands must match docs | `/ebb attributes`, `/ebb attr`, spend/grant/set/reset, `/ebb dev`, and `/ebb summon_npc <routine>` are present and included in static audit. | Complete |
| Dialogue enter/outcome effects and lifecycle must match docs | `DialogueNode.enterEffects`, `DialogueCheck` outcome effects, ACTION revalidation, disconnect/respawn/leave/level-change/server-stop cleanup, and timeout tick are present and included in static audit. | Complete |
| Docs must not overclaim GUI completion | `manual_client_test_result_2026-05-30_second.md` and this document mark the pre-hotfix GUI test as failed and the post-reconciliation GUI retest as pending. | Complete |

## Code changes made during this reconciliation

- `ClientTargetDetector`: entity predictions now enforce the matched binding/synced target `highlightRange`, reducing stale target-sync or smaller-range content from highlighting outside its authored range.
- `ClientInteractionNetworking`: synced prediction state is cleared on `ClientPlayConnectionEvents.JOIN` as well as `DISCONNECT`, preventing cross-world/server stale indexes.
- Added `scripts/third_review_static_audit.py`, a tracked audit script for the exact wiring points listed by the third review.

## Verification run

- `scripts/third_review_static_audit.py` → `ThirdReviewStaticAudit passed: runtime wiring and documented command/effect surfaces are present.`
- `scripts/gradle-local.sh --no-daemon build` → `BUILD SUCCESSFUL in 46s`.
- `ReviewSmoke` → `ReviewSmoke passed: dialogues=3, attributes=8, block_groups=1, entity_bindings=2, npc_routines=1, messages=1`.
- `AttributePointsSmoke` → `AttributePointsSmoke passed: attributes=8, points=8`.
- `SecondReviewSmoke` → passed, including `EntityTargetSyncPayload` construction.
- `scripts/configure_pcl_test_client.sh` refreshed `26.1.2-Fabric-Ebb-Test`; build jar and installed test-profile jar both hash to `01d880f0e424e0b6ff3592e9bf89189199254167ebb891baffa18330231758b0`.
- Jar inspection confirms `EntityTargetSyncPayload`, `ClientTargetDetector`, and `ClientInteractionNetworking` classes are packaged.
- `scripts/gradle-local.sh --no-daemon runServer --args nogui` → `BUILD SUCCESSFUL in 1m 44s`; Ebb initialized and the server stopped at the normal EULA gate.
- `git diff --check` → no whitespace/path errors.
- Drive mirror verification after push/sync: pulled Google Drive folder `1cGZxWHdCeYYI3ttzL6ilXEGjkOlCz2nt` into `build/tmp/drive-third-audit`, ran the same static audit from the Drive copy successfully, and matched SHA-256 for critical runtime wiring files against local commit `4085b5fbe5f515fa1966d583e8c5ce66298f7aed`.

## Remaining manual retest

A human GUI retest is still required:

1. Fully close any running Minecraft client.
2. Relaunch `26.1.2-Fabric-Ebb-Test` so the refreshed jar hash above is loaded.
3. Spawn/tag an `ebb:npc`, wait up to 1 second for target sync, look at it within highlight range, verify cyan outline and `X` prompt.
4. Press `X` and verify `ebb:demo/innkeeper_intro` opens.
5. Record the result in `docs/manual_client_test_result_2026-05-30_second.md`.
