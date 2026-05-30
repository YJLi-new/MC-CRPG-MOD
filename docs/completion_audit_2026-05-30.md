# Completion Audit — Review Remediation Objective

Date: 2026-05-30 Asia/Shanghai  
Audit source: `C:\Users\lanla\Downloads\ebb_project_review.md` and the user's repeated explicit issue list.

## Requirement-by-requirement status

| Requirement | Current evidence | Status |
|---|---|---:|
| Dedicated-server-safe block-group sync | `BlockGroupSyncPayload`, `InteractionSyncService`, `ClientBlockGroupIndex`; payload registered in `ModPackets`; server sync on data-pack contents/reload; client receiver rebuilds index and clears it on disconnect. | Complete |
| Entity bindings, not all entities debug | `EntityBindingDefinition`, `EntityBindingRegistry`, `EntityBindingRegistry.resolve(entity)` in `InteractionService.validateEntity`; samples under `data/ebb/interactions/entity_bindings/demo`. | Complete |
| Dev tree viewer, not just snapshot | `DialogueDebugDumper` called by `DevSnapshotService`; dumps dialogue id/start/node/choice/condition/effect/check/outcome effects plus entity bindings/routines. | Complete |
| Check effects semantics | `DialogueCheck` parses `success_effects`, `failure_effects`, `critical_success_effects`, `critical_failure_effects`; `DialogueService` applies outcome effects after roll; `DialogueNode` parses/applies `enter_effects`. | Complete |
| Dialogue session lifecycle | `DialogueService.registerLifecycleEvents()` handles disconnect/leave/respawn/level change/server stopping; server tick timeout; ACTION target revalidation; rejected choice attempts refresh session activity before returning status. | Complete |
| Packet count hardening | `DialoguePayloadCodecs`, `DevSnapshotPayload`, and `BlockGroupSyncPayload` reject invalid counts with `DecoderException`. | Complete |
| Missing dialogue refs hard invalid | `DialogueDefinition.validateReferences` returns false for missing refs, causing parse failure. | Complete |
| Attribute default clamp | `AttributeDefinition.parse` clamps default score into `[min,max]` and records a message. | Complete |
| Block group optional block predicate | `BlockGroupDefinition` accepts legacy `[x,y,z]` and object `{pos, block}` entries; `InteractionService` checks predicates server-side. | Complete |
| Dialogue UI improvements | `DialogueScreen` is non-pausing, has scrollable body text, paged choices, wait-state button disabling, explicit done/end button, and text-key support. | Complete |
| Text localization strategy | Dialogue/node/choice `text_key` is parsed and transmitted; client renders text keys as translatable components while preserving literal text fallback. | Complete |
| Vanilla entity binding before NPC | Entity binding registry/samples exist and server validates entity dialogue/range through bindings. | Complete |
| Custom NPC skeleton | `ModEntityTypes`, `EbbNpcEntity`, `ModEntityRenderers`, GeckoLib assets, and `/ebb summon_npc <routine>` with invalid-id guard. | Complete |
| Routine controller | `NpcRoutineDefinition`, `NpcRoutineRegistry`, `NpcRoutineController` implement stand/walk destination and `look_at_player`. | Complete |
| Routine/dialogue state bridge | `DialogueEffect.ROUTINE_PLACEHOLDER` sets routine flags and, when interacting with `EbbNpcEntity`, updates that NPC's routine id. | Complete |
| No vanilla 26.1.2 profile modification | Work stayed under active project checkout; run-server smoke created ignored files under project-local `run/`, not `.minecraft/versions/26.1.2`. | Complete |

## Verification evidence

Commands run:

```bash
scripts/gradle-local.sh --no-daemon build
java -cp "build/tmp/verify:$RUNTIME_CP" ReviewSmoke
scripts/gradle-local.sh --no-daemon runServer --args nogui
```

Results:

- Build: `BUILD SUCCESSFUL`.
- `ReviewSmoke`: passed registry/effects/missing-reference checks. The standalone smoke still warns about `ebb:npc` because it does not run through Fabric's normal mod registration lifecycle before parsing data; the `runServer` smoke verifies the actual Fabric lifecycle initializes the mod without that crash path.
- `runServer` smoke: Fabric Loader loaded Minecraft `26.1.2`, Fabric Loader `0.19.2`, Fabric API `0.150.0+26.1.2`, GeckoLib `5.5.1`, and `ebb 0.1.0-dev`; `EbbMod` initialized. The dev server stopped at the normal EULA gate, not due to an Ebb mod crash.
- Jar inspection confirmed packaged sync/entity/dev/NPC/routine classes and assets.
- Final refresh: `git diff --check` returned no output; `runServer` smoke was re-run after the final code hardening and again reached the EULA gate with status 0.

Current generated jars:

- `build/libs/ebb-0.1.0-dev.jar` SHA-256: `9f6a09ceb005aea6148aaa3d8e1545212c9ca6aaca9305aba2a1b8ca3a85624e`
- `build/libs/ebb-0.1.0-dev-sources.jar` SHA-256: `c5e3857cf88f4262890b2195b7e67822329dc0ae11dca19ad3a12cfaef101c96`

## Remaining non-code note

Full interactive client UX (actual visual model/animation, keypress, HUD and GUI interaction) still requires the separately documented Fabric 26.1.2 client profile workflow. The implementation, build, parser smoke, jar packaging, and server-initialization smoke now cover the requested code objectives without modifying the vanilla profile.
