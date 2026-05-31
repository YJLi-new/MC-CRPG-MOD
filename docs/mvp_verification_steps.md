# Esoteric Ebb CRPG MVP Verification Steps

Generated: 2026-05-30 Asia/Shanghai

## Automated/build verification already run

From the active checkout (`/mnt/e/MC/PCL/CRPG_MOD`; original project path was `E:\MC\SIMMC2_1-21-8\CRPG_MOD`):

```bash
scripts/gradle-local.sh --no-daemon build
```

Expected/current result: `BUILD SUCCESSFUL` and jar output in `build/libs/`.

Additional smoke checks run from `build/tmp/verify` / `build/tmp/verify-src`:

- `DialogueParserSmoke`: validates sample dialogue parsing and invalid-dialogue validation messages.
- `Phase5Smoke`: validates attribute registry, locked-door check/effect/condition parsing, condition state changes, and `NarrativeSavedData` codec round trip.
- `ReviewSmoke`: validates review-remediation registries: dialogues, attributes, block groups, entity bindings, NPC routines, check outcome effects, and hard-invalid missing dialogue references.

## Generated jars

- `build/libs/ebb-0.1.0-dev.jar`
- `build/libs/ebb-0.1.0-dev-sources.jar`

## Manual client verification plan

Do **not** modify `.minecraft/versions/26.1.2` in place. For manual testing, create/use a separate Fabric 26.1.2 profile and install the generated jar plus required dependencies (Fabric API and GeckoLib) in that separate profile's mods folder.

Suggested checks in a single-player test world or LAN/server with the mod installed on both sides:

1. Run `/ebb status` and `/ebb data`; expect `interaction_settings(...debug_entity_fallback=false...)` for the bundled demo config.
2. Run `/ebb dev` as an OP/cheat-enabled player; expect the developer snapshot screen, including interaction settings and entity bindings.
3. Run `/ebb attributes`; expect DND-8 scores and 8 starting unspent attribute points. Use `/ebb attributes spend charisma 1` to verify point spending changes dialogue roll modifiers.
4. Look at an unregistered vanilla pickable entity within 10m; with default demo settings, expect **no** Ebb cyan highlight and no Ebb interaction prompt.
5. Tag/register a test villager, for example `/tag @e[type=minecraft:villager,limit=1,sort=nearest] add ebb.npc.innkeeper`, then wait up to 1 second for server target sync and look at that registered entity within 10m; expect cyan highlight based on synced entity bindings/registered entity target sync.
6. Move within that registered entity's configured `interaction_range`; expect `按 [X] 互动` / `Press [X] to interact`, and pressing `X` should open the configured entity-binding dialogue.
7. Choose dialogue/action/thought options; expect branching and terminal close behavior.
8. Build or place a two-block locked-door test target at overworld blocks `[0,64,4]` and `[0,65,4]`; look at it within range and press `X`; expect `ebb:demo/locked_door_dialogue`.
9. Choose the force action; expect a server-side d20 roll summary and success/failure branch.
10. Choose the knock action, return to the start node, and verify the conditional thought choice becomes visible after the player flag is set.
11. Run `/reload`; invalid JSON should produce validation messages rather than crash. On a dedicated server, block-group definitions should resync to connected modded clients through `BlockGroupSyncPayload`, entity binding definitions/settings through `EntityBindingSyncPayload`, and currently registered tag-matched entities through `EntityTargetSyncPayload`. Then re-run `/ebb data` or `/ebb dev`.
12. Tag a villager with `ebb.npc.innkeeper` or run `/ebb summon_npc ebb:demo/innkeeper_day`; interact to verify `ebb:demo/innkeeper_intro`. Untagged/unbound entities should remain inert for Ebb targeting.
13. In `/ebb dev`, verify full tree lines for dialogue ids, nodes, choices, checks, conditions, effects, entity bindings, and NPC routines.

## Notes

- Current interaction key is configurable in Controls but defaults to `X`.
- GeckoLib remains a hard dependency and now has an MVP `ebb:npc` skeleton with idle/walk controller, waypoint path progression, and routine/look-at-player logic.
- The sample block group is deliberately small and coordinate-based for deterministic testing; content authors can replace it with real inn-corridor coordinates later. Optional block predicates are supported with `{ "pos": [x,y,z], "block": "minecraft:block_id" }`. Groups over `max_blocks_per_group` are invalid and should be split.
- Debug entity fallback can be re-enabled for development by a datapack under `data/<namespace>/interactions/settings/*.json` with `"enable_debug_entity_fallback": true`; keep it disabled for formal demo content.
- Tag-based entity bindings do not depend on clients seeing scoreboard/entity tags directly: the server periodically syncs matched nearby registered entity UUIDs to modded clients, while interaction validation remains server-authoritative.
- See `docs/manual_client_test_result_2026-05-30_second.md` for the second-review GUI hand-test checklist/status.
