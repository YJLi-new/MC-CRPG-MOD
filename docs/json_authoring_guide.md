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
    "max_yaw_speed": 8.0
  }
}
```

MVP behavior:

- `stand` moves to/holds a single `pos`.
- `walk` follows `path` waypoints sequentially and loops while the step is active.
- `look_at_player` turns the NPC's head/look controller toward the nearest non-spectator player in range.

Known future NPC work: behavior stacks, richer schedules/transitions, patrol pause points, animation performances, gestures, and custom humanoid art assets.
