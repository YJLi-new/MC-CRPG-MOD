# Esoteric Ebb CRPG JSON Authoring Guide

This guide documents the current MVP data files under `data/<namespace>/...`.

## Interaction settings

Path: `data/<namespace>/interactions/settings/<id>.json`

Example:

```json
{
  "enable_debug_entity_fallback": false,
  "debug_entity_fallback_dialogue": "ebb:debug/entity",
  "debug_entity_fallback_interaction_range": 2.0,
  "debug_entity_fallback_highlight_range": 10.0,
  "max_blocks_per_group": 512
}
```

Notes:

- Demo default is `enable_debug_entity_fallback=false`, so only bound/tagged entities highlight and interact.
- For development-only testing, a datapack may set `enable_debug_entity_fallback=true` to make ordinary pickable entities open the debug dialogue.
- `max_blocks_per_group` cannot exceed the network hard limit of 512. Oversized groups are invalid; split them into smaller block groups.

## Entity bindings

Path: `data/<namespace>/interactions/entity_bindings/<id>.json`

Example:

```json
{
  "match": {
    "entity_type": "minecraft:villager",
    "tag": "ebb.npc.innkeeper"
  },
  "dialogue": "ebb:demo/innkeeper_intro",
  "interaction_range": 2.25,
  "highlight_range": 10.0,
  "priority": 100
}
```

Supported match fields:

- `uuid`: exact entity UUID.
- `tag` or `tags`: scoreboard tag(s); at least one must match.
- `name`: custom/display name string.
- `entity_type` or `entity_types`: entity type id(s).

Higher specificity/priority wins. Entity bindings are synced to modded clients with `EntityBindingSyncPayload` for dedicated-server-safe highlight/prompt prediction.

## Block groups

Path: `data/<namespace>/interactions/block_groups/<id>.json`

Example:

```json
{
  "dimension": "minecraft:overworld",
  "dialogue": "ebb:demo/locked_door_dialogue",
  "blocks": [
    { "pos": [0, 64, 4], "block": "minecraft:oak_door" },
    [0, 65, 4]
  ],
  "interaction_point": [0.5, 64.5, 4.5]
}
```

Notes:

- Each group may contain at most `max_blocks_per_group` blocks, default/hard limit 512.
- Object block entries with `block` add server-side block predicate validation.
- Large structures should be split into multiple logical groups or represented by smaller interaction hotspots.

## Dialogues

Path: `data/<namespace>/dialogues/<id>.json`

Core concepts:

- `start`: start node id.
- `nodes`: map of node id to speaker/text/choices.
- Node `enter_effects`: applied when entering a node.
- Choice `type`: `dialogue`, `action`, or `thought`.
- Choice `conditions`: controls visibility.
- Choice `effects`: pre-roll / outcome-independent effects.
- Choice `check`: server-side d20 check with outcome branches and outcome effects.
- `text_key` may be used instead of, or alongside, literal `text` for localization.

Check outcome effect keys:

- `success_effects`
- `failure_effects`
- `critical_success_effects`
- `critical_failure_effects`

## Story Variables

GOAL.md requires narrative variables to be layered instead of a flat bag of flags:

- **Branch**: major route / ending commitments, e.g. `tavern_route=public`.
- **Major**: quest/NPC/world-state pivots, e.g. `innkeeper_trust=2`.
- **Minor**: local scene beats, e.g. `met_innkeeper=true`.

Story vars are server-authoritative and persist in `NarrativeSavedData` for both player and world scope.

Effects:

```json
{ "type": "set_story_var", "scope": "player", "layer": "branch", "id": "tavern_route", "value": "public" }
{ "type": "add_story_int", "scope": "player", "layer": "major", "id": "innkeeper_trust", "amount": 1 }
{ "type": "clear_story_var", "scope": "world", "layer": "minor", "id": "ash_smell" }
```

Conditions:

```json
{ "type": "story_var", "scope": "player", "layer": "branch", "id": "tavern_route", "value": "public" }
{ "type": "story_var", "scope": "player", "layer": "major", "id": "innkeeper_trust", "min": 1 }
{ "type": "story_var", "scope": "player", "layer": "minor", "id": "met_innkeeper", "value": true }
```

Use `/ebb dialogue vars` or `/ebb dev` to inspect the current player's Branch/Major/Minor story vars.

## Chime / Inner Voice / Passive Inserts

Path: `data/<namespace>/chimes/<id>.json`

```json
{
  "name": "Rhetoric",
  "source_attribute": "charisma",
  "min_score": 1,
  "trigger_tags": ["innkeeper.read"],
  "speaker_style": "argument",
  "cooldown_ticks": 200,
  "lines": ["别问钥匙。问谁会因为钥匙被交出来而失去叙事控制。"]
}
```

Dialogue nodes can opt into passive insert resolution with `chime_tags`:

```json
{
  "speaker": "innkeeper",
  "text": "旅馆老板抬起眼……",
  "chime_tags": ["innkeeper.read"]
}
```

Runtime behavior:

- The server checks the current node's `chime_tags` against loaded chimes.
- A chime triggers when the player has the required source attribute score.
- The passive insert appears in the dialogue status area with cyan Chime styling.
- Triggering a chime writes `chime:<id>` as a player flag, so a normal condition can unlock a follow-up thought path:

```json
{ "type": "flag", "scope": "player", "id": "chime:ebb:demo/rhetoric", "value": true }
```

Current bundled chimes are `Instinct`, `Rhetoric`, `Dread`, and `Empathy`.

## Journal / Clues / Leads

Path: `data/<namespace>/journal_entries/<id>.json`

```json
{
  "title": "Fresh Scratches Inside the Doorframe",
  "category": "clue",
  "quest": "ebb:demo/tavern_quiet",
  "text": "The marks are on the inside edge, as if the door was tested from within before anyone outside noticed.",
  "tags": ["locked_door", "inn_corridor"]
}
```

Supported categories:

- `clue`
- `scene_note`
- `lead`
- `quest_note`

Dialogue effect:

```json
{ "type": "add_journal_entry", "id": "ebb:demo/door_scratches" }
```

Aliases include `reveal_clue`, `add_clue`, `journal`, and `add_journal`.

Dialogue condition:

```json
{ "type": "has_clue", "id": "ebb:demo/door_scratches" }
```

Use `/ebb journal` to open the current player's Journal screen. Dialogue status echoes display clue/journal gains in green, quest/take-root in gold, feats in purple, chimes in cyan, and relation changes in red once relation effects are added.

## Quest Branch / Take Root / Feat

Path: `data/<namespace>/quest_branches/<id>.json`

```json
{
  "title": "公开揭露",
  "kind": "major",
  "description": "把隐藏事实推到公共视野。",
  "take_root_text": "公开路线扎根：你更擅长把房间变成证词现场。",
  "grant_feats": ["ebb:demo/tavern_authority"],
  "take_root_effects": [
    { "type": "set_story_var", "scope": "player", "layer": "major", "id": "public_pressure", "value": 1 }
  ]
}
```

Path: `data/<namespace>/feats/<id>.json`

```json
{
  "display_name": "Tavern Authority",
  "description": "你在拥挤房间里说话时，别人更容易把它当成事实。",
  "permanent_passive": true,
  "active_slot_candidate": true,
  "check_modifiers": { "charisma": 1 }
}
```

Dialogue effects:

```json
{ "type": "start_quest_branch", "id": "ebb:demo/tavern_public" }
{ "type": "complete_quest_branch", "id": "ebb:demo/tavern_public" }
{ "type": "unlock_feat", "id": "ebb:demo/tavern_authority" }
{ "type": "activate_feat", "id": "ebb:demo/tavern_authority" }
```

Dialogue conditions:

```json
{ "type": "quest_state", "id": "ebb:demo/tavern_public", "value": "take_rooted" }
{ "type": "has_feat", "id": "ebb:demo/tavern_authority" }
```

Current MVP limits:

- Quest branch `kind` may be `minor` or `major`.
- Completing a **major** branch runs take-root once, applies `take_root_effects`, grants feats, and records `take_rooted` state.
- Player feat state supports 4 active feat slots. Permanent passive feats apply when unlocked; non-permanent feat modifiers apply when active.
- Use `/ebb quest` or `/ebb quest tree` for the basic Quest Tree UI; `/ebb dev` also shows quest/feat state.

## Attributes

Path: `data/<namespace>/attributes/<id>.json`

Current bundled DND-like dimensions:

- `strength`, `dexterity`, `constitution`, `intelligence`, `wisdom`, `charisma`, `perception`, `luck`

Legacy aliases are preserved where relevant, e.g. `force -> strength`, `logic -> intelligence`, `empathy -> charisma`.

## NPC routines

Path: `data/<namespace>/npc_routines/<id>.json`

Example:

```json
{
  "steps": [
    { "time": [0, 6000], "action": "stand", "pos": [2.5, 64.0, 2.5] },
    { "time": [6000, 12000], "action": "walk", "path": [[2.5, 64.0, 2.5], [5.5, 64.0, 2.5]] }
  ],
  "look_at_player": {
    "enabled": true,
    "range": 4.0,
    "max_yaw_speed": 8.0,
    "requires_line_of_sight": true
  }
}
```

MVP behavior:

- `stand` moves to/holds a single `pos`.
- `walk` follows `path` waypoints sequentially and loops while the step is active.
- `look_at_player` turns the NPC's head/look controller toward the nearest non-spectator player in range, respecting `requires_line_of_sight` when enabled.
- Active dialogue sessions with an Ebb NPC pause its movement routine and force a conversation-focus look target until the session ends.

Known future NPC work: behavior stacks, richer schedules/transitions, patrol pause points, animation performances, gestures, and custom humanoid art assets.

## Author-friendly YAML/JSON source compiler

The deep-research authoring layer is available under `authoring/` and compiles into the current runtime JSON layout without changing the bundled demo data by default:

```bash
scripts/compile_authoring_sources.py --clean
```

Default generated root:

```text
build/generated/ebb_authoring/data/ebb/
```

Supported source directories:

- `authoring/dialogues/*.yaml|*.json` → `dialogues/*.json`
- `authoring/interactables/*.json|*.yaml` → `interactions/block_groups/*.json`
- `authoring/npc/*.yaml|*.json` → `interactions/entity_bindings/*.json` and `npc_routines/*.json`

The compiler accepts report-style aliases such as dialogue `entry`, choice `kind`/`label`/`roll`, block-group `targetType`/`anchor`/`boxes`, and NPC `dialogueBindings`/`lookPolicy`/`routine.schedule`. It also performs basic failure-forward validation for rolled choices before writing JSON.

Use `scripts/run_smoke_checks.sh` for the full local build + authoring + smoke validation suite.

## Validation and regression tests

Recommended local verification after editing authoring data or schemas:

```bash
scripts/compile_authoring_sources.py --clean
scripts/gradle-local.sh --no-daemon test validateEbbData
scripts/gradle-local.sh --no-daemon runGametestServer --args nogui
```

The tracked JUnit suite checks bundled registry cleanliness, fail-forward validation, narrative effect persistence, and generated authoring output. The tracked Fabric GameTest suite checks bundled runtime registries, Ebb NPC spawn/routine state, and tagged Ebb NPC binding resolution in a real headless Minecraft server.

## Relationship / NPC Memory / Routine Expansion

Relationships are data-driven under `data/<namespace>/relationships/<id>.json`:

```json
{
  "display_name": "Innkeeper",
  "narrative_key": "ebb:demo/innkeeper",
  "default_score": 0,
  "tags": ["core_npc", "tavern"]
}
```

Dialogue effects can write relationship scores and NPC memory tags:

```json
{ "type": "add_relation", "relation": "ebb:demo/innkeeper", "amount": 2 }
{ "type": "set_relation", "relation": "ebb:demo/witness", "value": 1 }
{ "type": "set_npc_state", "npc": "ebb:demo/innkeeper", "tag": "guarded" }
{ "type": "clear_npc_state", "npc": "ebb:demo/innkeeper", "tag": "guarded" }
```

Dialogue conditions can read those values, plus the current world time:

```json
{ "type": "relation_at_least", "relation": "ebb:demo/innkeeper", "min": 2 }
{ "type": "npc_state", "npc": "ebb:demo/witness", "tag": "asked_about_door" }
{ "type": "time_window", "start": 12000, "end": 24000 }
```

NPC routine JSON now supports the MVP action vocabulary requested for GOAL.md P6:

- `stand`
- `wait`
- `walk`
- `walk_path`
- `look_at`
- `play_animation` (records the requested narrative animation on the NPC skeleton)
- `set_pose` (records the requested narrative pose on the NPC skeleton)
- `teleport_fallback`

Example:

```json
{
  "time": [12000, 24000],
  "action": "look_at",
  "pos": [6.5, 64.0, 6.5],
  "pose": "listening",
  "animation": "idle",
  "teleport_distance": 24.0
}
```

`EbbNpcEntity` persists a `narrative_key`, `pose`, and `animation` string. `/ebb routine inspect <entity>` shows these values, and dialogue can switch the currently interacted Ebb NPC's routine with:

```json
{ "type": "set_npc_routine", "id": "ebb:demo/innkeeper_backroom" }
```

The bundled demo includes innkeeper/witness/tenant/guard bindings and routines. Spawn test NPCs with `/ebb summon_npc demo/witness_day`, `/ebb summon_npc demo/tenant_day`, or `/ebb summon_npc demo/guard_day`; the command adds role-specific tags such as `ebb.npc.demo.witness` so entity bindings select the correct dialogue.

## Investigation / Clues / Set-piece Conflict

P7 adds an investigation layer on top of journal entries. Clues live under `data/<namespace>/clues/<id>.json` and can link back to a journal entry:

```json
{
  "title": "Fresh Scratches Inside the Doorframe",
  "scene": "ebb:demo/locked_room",
  "journal_entry": "ebb:demo/door_scratches",
  "text": "The marks are on the inside edge.",
  "check_modifiers": { "perception": 1, "intelligence": 1 }
}
```

Investigation scenes live under `data/<namespace>/investigation_scenes/<id>.json`:

```json
{
  "title": "The Locked Room in the Upper Hall",
  "clues": ["ebb:demo/door_scratches", "ebb:demo/witness_knock_pattern"],
  "completion_threshold": 3
}
```

Reveal clues from dialogue with:

```json
{ "type": "reveal_clue", "id": "ebb:demo/door_scratches" }
```

If the clue has a `journal_entry`, the linked journal entry is also unlocked. Discovered clues can gate later choices and add server-authoritative check modifiers through `InvestigationRegistry.totalCheckModifier`:

```json
{ "type": "clue_found", "id": "ebb:demo/witness_knock_pattern" }
```

Set-piece conflicts live under `data/<namespace>/conflicts/<id>.json`:

```json
{
  "title": "Hallway Confrontation",
  "scene": "ebb:demo/locked_room",
  "stress_limit": 3,
  "resolve_goal": 2,
  "failure_state": "failed_forward",
  "success_state": "resolved"
}
```

Conflict effects:

```json
{ "type": "start_conflict", "id": "ebb:demo/hallway_confrontation" }
{ "type": "add_conflict_stress", "id": "ebb:demo/hallway_confrontation", "amount": 1 }
{ "type": "add_conflict_resolve", "id": "ebb:demo/hallway_confrontation", "amount": 2 }
{ "type": "set_conflict_state", "id": "ebb:demo/hallway_confrontation", "state": "resolved" }
{ "type": "set_scene_phase", "scene": "ebb:demo/locked_room", "phase": "confrontation" }
```

Conflict/scene conditions:

```json
{ "type": "conflict_state", "id": "ebb:demo/hallway_confrontation", "state": "active" }
{ "type": "scene_phase", "id": "ebb:demo/locked_room", "phase": "messy" }
```

The bundled guard dialogue contains the first set-piece prototype. It uses clue-gated lines, d20 checks, stress/resolve effects, and fail-forward text rather than a generic combat system.

## Playable Tavern Vertical Slice Content Map

The bundled P8 tavern slice is intentionally small and dense. It uses eight block-group investigation points plus four role NPCs:

Block investigation points:

1. locked door — `ebb:demo/locked_door_dialogue`
2. counter ledger — `ebb:demo/counter_ledger_dialogue`
3. washroom mirror — `ebb:demo/washroom_mirror_dialogue`
4. windowsill ash — `ebb:demo/windowsill_ash_dialogue`
5. tenant luggage — `ebb:demo/tenant_luggage_dialogue`
6. notice board — `ebb:demo/notice_board_dialogue`
7. cellar hatch — `ebb:demo/cellar_hatch_dialogue`
8. back door / ending placeholder — `ebb:demo/back_door_dialogue`

Role NPCs:

- innkeeper — `ebb:demo/innkeeper_intro`
- witness — `ebb:demo/witness_intro`
- suspicious tenant — `ebb:demo/tenant_intro`
- guard/fixer — `ebb:demo/guard_intro`

The back door dialogue is the current ending placeholder. It reads Branch/scene state and exposes public, quiet, and messy fail-forward endings.
