# Esoteric Ebb CRPG MVP Verification Steps

Generated: 2026-05-30 Asia/Shanghai

## Automated/build verification already run

From `E:\MC\SIMMC2_1-21-8\CRPG_MOD` / `/mnt/e/MC/SIMMC2_1-21-8/CRPG_MOD`:

```bash
scripts/gradle-local.sh --no-daemon build
```

Expected/current result: `BUILD SUCCESSFUL` and jar output in `build/libs/`.

Additional smoke checks run from `build/tmp/verify`:

- `DialogueParserSmoke`: validates sample dialogue parsing and invalid-dialogue validation messages.
- `Phase5Smoke`: validates attribute registry, locked-door check/effect/condition parsing, condition state changes, and `NarrativeSavedData` codec round trip.

## Generated jars

- `build/libs/ebb-0.1.0-dev.jar`
- `build/libs/ebb-0.1.0-dev-sources.jar`

## Manual client verification plan

Do **not** modify `.minecraft/versions/26.1.2` in place. For manual testing, create/use a separate Fabric 26.1.2 profile and install the generated jar plus required dependencies (Fabric API and GeckoLib) in that separate profile's mods folder.

Suggested checks in a single-player test world or LAN/server with the mod installed on both sides:

1. Run `/ebb status` and `/ebb data`.
2. Run `/ebb dev` as an OP/cheat-enabled player; expect the developer snapshot screen.
3. Look at a vanilla pickable entity within 10m; expect cyan highlight.
4. Move within 2m; expect `按 [X] 互动` / `Press [X] to interact` prompt.
5. Press `X`; expect the `ebb:debug/entity` dialogue screen.
6. Choose dialogue/action/thought options; expect branching and terminal close behavior.
7. Build or place a two-block locked-door test target at overworld blocks `[0,64,4]` and `[0,65,4]`; look at it within range and press `X`; expect `ebb:demo/locked_door_dialogue`.
8. Choose the force action; expect a server-side d20 roll summary and success/failure branch.
9. Choose the knock action, return to the start node, and verify the conditional thought choice becomes visible after the player flag is set.
10. Run `/reload`; invalid JSON should produce validation messages rather than crash. Then re-run `/ebb data` or `/ebb dev`.

## Notes

- Current interaction key is configurable in Controls but defaults to `X`.
- GeckoLib remains a hard dependency, but custom GeckoLib NPC/routine work is intentionally deferred.
- The sample block group is deliberately small and coordinate-based for deterministic testing; content authors can replace it with real inn-corridor coordinates later.
