# Esoteric Ebb CRPG MVP Verification Steps

Generated: 2026-05-30 Asia/Shanghai

## Automated/build verification already run

From the active checkout (`/mnt/e/MC/PCL/CRPG_MOD`; original project path was `E:\MC\SIMMC2_1-21-8\CRPG_MOD`):

```bash
scripts/gradle-local.sh --no-daemon build
```

Expected/current result: `BUILD SUCCESSFUL` and jar output in `build/libs/`.

Additional smoke checks run from `build/tmp/verify` / `build/tmp/verify-src` and scripts:

- `DialogueParserSmoke`: validates sample dialogue parsing and invalid-dialogue validation messages.
- `Phase5Smoke`: validates attribute registry, locked-door check/effect/condition parsing, condition state changes, and `NarrativeSavedData` codec round trip.
- `ReviewSmoke`: validates review-remediation registries: dialogues, attributes, block groups, entity bindings, NPC routines, check outcome effects, and hard-invalid missing dialogue references.
- `scripts/third_review_static_audit.py`: checks third-review P0/P1 runtime wiring in source: sync payload registration/receivers, client prediction indexes, entrypoints, commands, dialogue lifecycle, and effects.
- `scripts/deep_research_static_audit.py`: checks report-facing API/schema/UI/NPC/authoring/devtool/test surfaces.
- `scripts/goal_static_audit.py`: checks GOAL.md P2 Story Variables wiring through persistence, dialogue effects/conditions, dev views, docs, bundled demo data, smoke, and JUnit.
- GOAL.md P3 checks now include Quest Branch / Take Root / Feat wiring: `quest_branches`, `feats`, `complete_quest_branch`, `has_feat`, feat check modifiers, `/ebb quest`, `QuestTreeScreen`, and dev snapshot state.
- GOAL.md P4 checks now include Chime / Inner Voice wiring: `chimes`, dialogue node `chime_tags`, server-side `ChimeResolver`, cyan `[Chime:]` passive insert rendering, and chime-unlocked thought paths.
- GOAL.md P5 checks now include Journal/UI rhythm wiring: `journal_entries`, `/ebb journal`, `JournalScreen`, `add_journal_entry`, `has_clue`, and typed status echoes for clue/journal/quest/feat/chime/relation messages.
- `scripts/gradle-local.sh --no-daemon runGametestServer --args nogui`: runs 4 required Fabric GameTests on a headless Minecraft server.

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
11. Interact with `ebb:demo/innkeeper_intro` and verify GOAL.md Story Variables:
    - On entry, `/ebb dialogue vars` should eventually show `story.minor.met_innkeeper = true`.
    - A successful “逼问他交出钥匙” check adds `story.major.innkeeper_trust`; the trust-gated follow-up choice should become visible.
    - Choosing “我会把这扇门的事公开” sets `story.branch.tavern_route = public`; the public-route placeholder/echo should become visible.
12. Verify GOAL.md Quest/Feat take-root:
    - Choosing the public route completes major branch `ebb:demo/tavern_public`, grants feats `ebb:demo/tavern_authority` and `ebb:demo/cheap_empathy`, and displays take-root status text.
    - Choosing the quiet route completes major branch `ebb:demo/tavern_quiet`, grants feats `ebb:demo/paranoid_pattern_reader` and `ebb:demo/door_theology`, and displays take-root status text.
    - Run `/ebb quest` or `/ebb quest tree`; expect the Quest Tree screen showing branch state, take-root text, active feat slots, and feat modifiers.
    - After gaining `Tavern Authority`, return to the innkeeper start node and expect the feat-gated authority follow-up choice.
13. Verify GOAL.md Chime/Inner Voice:
    - Spend an attribute point before talking to the innkeeper, e.g. `/ebb attributes spend charisma 1` for `Rhetoric`, `/ebb attributes spend wisdom 1` for `Empathy`, `/ebb attributes spend perception 1` for `Instinct`, or `/ebb attributes spend luck 1` for `Dread`.
    - Interact with the innkeeper start node; expect a cyan `[Chime: ...]` passive insert in the status area.
    - With `Rhetoric`, expect the passive insert to unlock the `rhetoric_insight` thought path.
14. Verify GOAL.md Journal/UI rhythm:
    - Knock on the locked door; expect a green clue/journal status echo and `ebb:demo/door_scratches` in `/ebb journal`.
    - Fail the force-door check; expect a green scene-note status echo for `ebb:demo/bruised_shoulder`, proving failure still creates content visible without commands.
    - Choose public/quiet innkeeper routes; expect journal lead entries in `/ebb journal`.
    - Verify the DialogueScreen status area uses distinct colors/labels for clue/journal, quest/take-root, feat, chime, and future relation echoes.
15. Run `/reload`; invalid JSON should produce validation messages rather than crash. On a dedicated server, block-group definitions should resync to connected modded clients through `BlockGroupSyncPayload`, entity binding definitions/settings through `EntityBindingSyncPayload`, and currently registered tag-matched entities through `EntityTargetSyncPayload`. Then re-run `/ebb data` or `/ebb dev`.
16. Tag a villager with `ebb.npc.innkeeper` or run `/ebb summon_npc ebb:demo/innkeeper_day`; interact to verify `ebb:demo/innkeeper_intro`. Untagged/unbound entities should remain inert for Ebb targeting.

18. Verify GOAL.md P6 relationship / NPC memory / routine expansion:
    - Spawn role NPCs with `/ebb summon_npc demo/witness_day`, `/ebb summon_npc demo/tenant_day`, and `/ebb summon_npc demo/guard_day`; each should use its role-specific dialogue binding, not the generic innkeeper fallback.
    - Interact with the innkeeper and choose a route that applies `set_npc_routine`; then run `/ebb routine inspect <entity>` and verify `routine_id`, `narrative_key`, `pose`, and `animation` reflect the switched routine.
    - Interact with the witness twice: after asking about the door, the memory-gated follow-up should appear; after improving relation, the relation-gated line should appear.
    - At night (`/time set night`), verify at least one `time_window` thought line appears for innkeeper or witness; at day, it should be hidden.
    - Confirm DialogueScreen shows relation/NPC-state/routine status echoes in the status area, not only in `/ebb dev`.


19. Verify GOAL.md P7 investigation / set-piece conflict:
    - Use the locked door, witness, tenant, and guard interactions to reveal at least five clues: `door_scratches`, `bruised_shoulder`, `witness_knock_pattern`, `tenant_false_window`, and `guard_denial`.
    - Verify `/ebb journal` receives linked clue journal entries where configured, while `/ebb dev` shows `investigation(clues=5, scenes=1)` and player discovered clue state after interactions.
    - Confirm clue-gated choices appear in the guard confrontation only after the relevant clues are found.
    - Start the guard hallway confrontation and choose both a prepared clue path and an unprepared pressure path; verify success adds conflict resolve and failure adds stress plus fail-forward scene/relationship consequences.
    - Verify the dialogue status area shows `clue_found`, `conflict_started`, `conflict_stress`/`conflict_resolve`, and `scene_phase` echoes.


20. Verify GOAL.md P8 playable tavern vertical slice content minimums:
    - Run `/ebb data`; expect at least `dialogues=13`, `interactions/block_groups=8`, role-specific `entity_bindings>=6`, `npc_routines>=5`, `quest_branches=2`, `feats=4`, `chimes=4`, `clues=5`, `conflicts=1`.
    - Visit all eight block investigation points: locked door, counter ledger, washroom mirror, windowsill ash, tenant luggage, notice board, cellar hatch, and back door.
    - Spawn/interact with the four role NPCs: innkeeper, witness, tenant, and guard.
    - Complete one public route and one quiet route in separate passes; confirm back-door ending placeholders differ.
    - Trigger at least one failed check and confirm the resulting fail-forward text/status/scene state opens a different messy ending placeholder rather than stopping play.

17. In `/ebb dev`, verify full tree lines for dialogue ids, nodes, choices, checks, conditions, effects, Story Variables, quest/feat/chime/journal state, entity bindings, and NPC routines.

## Notes

- Current interaction key is configurable in Controls but defaults to `X`.
- GeckoLib remains a hard dependency and now has an MVP `ebb:npc` skeleton with idle/walk controller, waypoint path progression, and routine/look-at-player logic.
- The sample block group is deliberately small and coordinate-based for deterministic testing; content authors can replace it with real inn-corridor coordinates later. Optional block predicates are supported with `{ "pos": [x,y,z], "block": "minecraft:block_id" }`. Groups over `max_blocks_per_group` are invalid and should be split.
- Debug entity fallback can be re-enabled for development by a datapack under `data/<namespace>/interactions/settings/*.json` with `"enable_debug_entity_fallback": true`; keep it disabled for formal demo content.
- Tag-based entity bindings do not depend on clients seeing scoreboard/entity tags directly: the server periodically syncs matched nearby registered entity UUIDs to modded clients, while interaction validation remains server-authoritative.
- Story Variables are layered as Branch/Major/Minor and are stored server-side in `NarrativeSavedData`; dialogue JSON can read/write them via `set_story_var`, `add_story_int`, `clear_story_var`, and `story_var` conditions.
- Quest Branch / Take Root / Feat data is under `data/ebb/quest_branches` and `data/ebb/feats`. Major branch completion grants feats and records `take_rooted`; feat modifiers currently apply to server-side checks through `FeatRegistry.totalCheckModifier`.
- Chime data is under `data/ebb/chimes`. Dialogue nodes opt in with `chime_tags`; triggered chimes write `chime:<id>` flags that can unlock later thought paths.
- Journal data is under `data/ebb/journal_entries`. Journal gains are player-visible through `/ebb journal` and DialogueScreen status echoes, not only through command/debug output.
- See `docs/manual_client_test_result_2026-05-30_second.md` for the GUI hand-test checklist/status.
- See `docs/third_review_reconciliation_2026-05-31.md` and `docs/third_review_completion_audit_2026-05-31.md` for the third-review runtime wiring reconciliation and requirement audit.
