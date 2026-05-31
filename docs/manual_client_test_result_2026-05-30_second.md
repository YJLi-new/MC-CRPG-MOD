# Manual Client Test Result — 2026-05-30 Second Review

Profile: `26.1.2-Fabric-Ebb-Test`  
Client directory: `/mnt/e/MC/PCL/.minecraft/versions/26.1.2-Fabric-Ebb-Test`

## Status

Command-line build/smoke verification is complete for the second-review fixes. Full player-driven Windows GUI testing is still pending because it requires a human/GUI operator to launch Minecraft, enter a world, aim, press `X`, and visually confirm rendering/UI behavior.

## Required manual checks

1. Launch `26.1.2-Fabric-Ebb-Test` from PCL.
2. Create/open a cheat-enabled test world.
3. Run `/ebb data`; verify `interaction_settings(...debug_entity_fallback=false...)` and entity bindings are listed.
4. Look at an untagged ordinary pickable entity; verify no Ebb highlight/prompt.
5. Tag a villager: `/tag @e[type=minecraft:villager,limit=1,sort=nearest] add ebb.npc.innkeeper`.
6. Wait up to 1 second for registered entity target sync, then look at the tagged villager within 10m; verify highlight.
7. Move within binding `interaction_range`; verify `按 [X] 互动` prompt.
8. Press `X`; verify `ebb:demo/innkeeper_intro` opens.
9. Click a checked option; verify d20 roll result/status appears without overlapping buttons.
10. Run `/ebb summon_npc ebb:demo/innkeeper_day`; verify NPC renders and looks toward the player in range.
11. Wait during the walk routine window or set time to verify routine path waypoint movement.
12. Build/place the locked-door sample blocks and verify block-group highlight/dialogue still works.
13. Run `/reload`; verify block-group, entity-binding, and registered entity target sync still leave client prediction correct.

Record screenshots/log snippets below after human execution.

## Result log

- 2026-05-31: Human GUI test showed `/ebb data` loaded `debug_entity_fallback=false`, `entity_bindings(valid=2)`, and the nearest `ebb:npc` had tags `ebb.npc`, `ebb.npc.ebb`, and `ebb_npc`, but no entity highlight/prompt appeared. Root cause: client-side tag matching could not be trusted for dedicated-style clients because entity tags are not available for local prediction. Fixed by adding server-authoritative registered entity target UUID sync (`EntityTargetSyncPayload`) and refreshing the test profile. Retest pending.
