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
  "priority": 100,
  "highlight": {
    "color": "#76F2B2",
    "opacity": 1.0,
    "far_opacity": 0.6,
    "render_mode": "outline",
    "priority": 40
  }
}
```

Supported match fields:

- `uuid`: exact entity UUID.
- `tag` or `tags`: scoreboard tag(s); at least one must match.
- `name`: custom/display name string.
- `entity_type` or `entity_types`: entity type id(s).

Higher specificity/priority wins. Entity bindings are synced to modded clients with `EntityBindingSyncPayload` for dedicated-server-safe highlight/prompt prediction.

Optional `highlight` fields are synced to clients:

- `color`: base `#RRGGBB` or `#AARRGGBB`.
- `opacity`: near/in-range alpha, 0.0–1.0.
- `far_opacity`: far/highlight-only alpha, 0.0–1.0.
- `close_color` / `far_color`: explicit colors when one base color is not enough.
- `render_mode`: `outline`, `merged`, or `bounds`; entity bindings normally use `outline`.
- `priority`: visual priority reserved for future overlapping-target polish.

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
  "interaction_point": [0.5, 64.5, 4.5],
  "highlight": {
    "color": "#64E6FF",
    "opacity": 1.0,
    "far_opacity": 0.55,
    "render_mode": "merged",
    "priority": 20
  }
}
```

Notes:

- Each group may contain at most `max_blocks_per_group` blocks, default/hard limit 512.
- Object block entries with `block` add server-side block predicate validation.
- Large structures should be split into multiple logical groups or represented by smaller interaction hotspots.
- A block may belong to only one block group in a dimension. duplicate block membership is a validation error; the first group remains indexed and later overlapping groups are skipped instead of silently overriding the target.
- Server validation raycasts to the nearest authored block center and the declared `interaction_point` using the shared collider-only `InteractionRaycastPolicy`, so clients and dedicated servers agree on solid-block line-of-sight semantics.
- Block-group `highlight.render_mode=merged` merges adjacent authored blocks into fewer outline boxes for cleaner visuals. Use `outline` for per-block outlines or `bounds` for a single bounding box.

## Dialogues

Path: `data/<namespace>/dialogues/<id>.json`

Core concepts:

- `start`: start node id.
- `nodes`: map of node id to speaker/text/choices.
- Node `enter_effects`: applied when entering a node.
- Choice `type`: `dialogue`, `action`, or `thought`.
- Choice `conditions`: controls visibility.
- Choice `pre_effects`: pre-roll / outcome-independent effects.
- Choice `effects`: legacy alias for `pre_effects`; prefer `pre_effects` in new content so success/failure mutations are not confused with branch outcome effects.
- Choice `check`: server-side d20 check with outcome branches and outcome effects.
- Choice `end_on_success: true`: explicitly says a successful checked choice may end the conversation when it has no `success`, `critical_success`, or fallback `next` node.
- `text_key` may be used instead of, or alongside, literal `text` for localization.
- When `text_key` is missing from the active language file, the client falls back to literal `text` instead of showing the raw translation key.
- Check display controls:
  - `hidden_dc: true` or `show_dc: false` hides the numeric DC from choice labels and roll-result echoes.
  - `hidden_roll: true` or `show_roll: false` hides the d20/total arithmetic while still reporting the resolved outcome.
  - `display_dc` and `display_roll` are accepted aliases for `show_dc` / `show_roll`.
  - `advantage: true` rolls two d20 and keeps the higher die. `disadvantage: true` rolls two d20 and keeps the lower die. If both are true they cancel out to a normal single d20 roll.
  - Roll echoes include the chosen die plus attribute/static/feat/clue modifier breakdown when `show_roll` is true.

Player-facing reading settings:

- The dialogue screen exposes A-/A+ controls for local dialogue font scale.
- The dialogue screen exposes a speed button cycling Slow / Normal / Fast / Instant text reveal.
- These are client-only readability preferences saved in `config/ebb-client.json`; they do not affect server-authoritative checks, choices, conditions, or effects.
- The dialogue layout computes body, status, choice, and done-button regions from one panel model so scaled text/status echoes are clipped rather than overlapping choices.

Check outcome effect keys:

- `success_effects`
- `failure_effects`
- `critical_success_effects`
- `critical_failure_effects`

Authoring cautions:

- Checked choices with branch-specific pre-roll effects (`complete_quest_branch`, `unlock_feat`, `start_conflict`, relation/NPC-state changes, clue/journal reveals, etc.) emit validation warnings because those effects run before the d20 is rolled. Put success/failure-specific mutations into `success_effects` / `failure_effects` instead.
- Retryable checks are “white checks”: a failed `mode: "retryable"` choice records `check_locked:<dialogue_id>:<choice_id>` and cannot be clicked again until an `unlock_retry` / `unlock` effect sets `unlock:<choice_id>` or `unlock:<dialogue_id>:<choice_id>`.
- `give_item` / `take_item` currently produce `item_placeholder_give` / `item_placeholder_take` status echoes and player flags (`item:<id>`). They do not manipulate vanilla inventory yet.

## P24 validation, schemas, and reference tables

Machine-readable starter schemas live under `docs/schemas/`:

- `ebb.dialogue.schema.json`
- `ebb.block_group.schema.json`
- `ebb.entity_binding.schema.json`
- `ebb.chime.schema.json`
- `ebb.conflict.schema.json`

They are intentionally conservative authoring aids; the Java parsers and `scripts/p24_authoring_validation.py` remain the authoritative validation path for cross-file references and failure-forward rules.

## P29 save/load, multiplayer, and permission hardening contract

Narrative state is server-owned and saved through `NarrativeSavedData`. P29 sets `CURRENT_SCHEMA_VERSION = 2`; older v1 saves are migrated on load and re-saved at the current version. The v1→v2 migration infers missing `conflict_phase:<conflict_id>` states from legacy `conflict:<conflict_id>` state plus persisted stress/resolve scores, so existing worlds keep their hallway-confrontation progress while gaining P28 phases.

Multiplayer dialogue sessions are also server-authoritative:

- One player may have only one active session; opening a new dialogue closes that player's older one.
- Two players may talk to different NPCs concurrently.
- The same entity is reserved while another player's dialogue session targets it; a second opener receives `entity_dialogue_busy`.
- Choice packets are preflighted by conversation UUID, player UUID, and timeout. Spoofed, stale, expired, invalid-choice, unavailable-choice, and stale-target/action packets are denied before effects run.
- Disconnect, respawn, leave, dimension change, timeout, and server stopping clean up sessions.

Command permission rules:

- OP/GAMEMASTER-gated: `/ebb dev`, `/ebb dialogue inspect|tree|reload`, `/ebb routine`, `/ebb export save-debug`, `/ebb summon_npc`, `/ebb attributes grant|set|reset`.
- Player-safe self-inspection: `/ebb status`, `/ebb data`, `/ebb vars`, `/ebb dialogue vars`, `/ebb journal`, `/ebb quest`, `/ebb attributes`, `/ebb attributes spend`.

Dedicated-server client diagnostics are surfaced in `/ebb dev`: if a player does not advertise Ebb sync payloads (`BlockGroupSyncPayload`, `EntityBindingSyncPayload`, or `EntityTargetSyncPayload`), the server records missing-client-mod diagnostics rather than silently assuming client prediction is available.

## P30 Vertical slice content expansion map

P30 expands the tavern slice into a larger 3-act proof-of-design:

1. **Discovery:** the original locked door, ledger, mirror, window, luggage, notice board, cellar hatch, and back door introduce the room.
2. **Pressure / investigation:** new investigation points (`stairwell_dust`, `kitchen_manifest`, `guestbook_torn_page`, `stable_mud`) plus cook/courier NPCs add service-route, trade, and mercy clues.
3. **Confrontation / ending:** hallway, kitchen, and courtyard set-piece conflicts feed public, quiet, messy, trade, and mercy ending placeholders.

P30 content minimums now tracked by static/JUnit/smoke checks:

- 12 block-group investigation points.
- At least 6 NPC roles or equivalent depth; current data has innkeeper, witness, tenant, guard, cook, and courier coverage through role bindings/routines/dialogues.
- 4 major branches and 8 minor branches.
- 12 feats.
- 8 Chimes with at least 40 Chime lines.
- At least 20 journal entries and 20 clues.
- 3 set-piece conflicts.
- Ending placeholders for each major route (`public_end`, `quiet_end`, `trade_end`, `mercy_end`) plus the messy fail-forward route.

Run authoring checks from the repository root:

```bash
scripts/compile_authoring_sources.py --clean
scripts/compile_authoring_sources.py --source authoring/examples/tavern_case --out build/generated/ebb_authoring_examples/tavern_case/data/ebb --clean
scripts/p24_authoring_validation.py
scripts/p24_authoring_validation.py build/generated/ebb_authoring_examples/tavern_case/data/ebb
```

Compiler diagnostics include file names and, for malformed YAML/JSON, parser line/column positions. Cross-reference validation checks dialogue ids, node-local branch refs, quest ids, feat ids, chime trigger tags, journal/clue ids, routine ids, relationship ids, investigation scene ids, and conflict ids.

### Condition reference

| Type / aliases | Required fields | Optional fields | Meaning | Cross-reference target |
|---|---|---|---|---|
| `flag`, `has_flag`, `not_flag`, `trait`, `thought` | `id` / `key` / `flag` / `trait` / `thought` | `scope`, `expected`, `value` | Player/world boolean flag, trait, or thought gate. | Local narrative state |
| `variable_equals`, `var_equals`, `variable` | `id` / `key`, `equals` / `string_value` / `value` | `scope`, `expected` | Player/world string variable equality. | Local narrative state |
| `attribute_at_least`, `skill_at_least`, `attribute` | `attribute` / `id` / `key`, `min` / `at_least` | `expected` | DND-8 attribute threshold gate. | Attribute key/alias |
| `story_var`, `story_variable`, `story_var_equals`, `story_var_at_least` | `id` / `story_var`, plus `value`/`equals` or `min` | `scope`, `layer`, `expected` | Branch/Major/Minor story variable equality or integer threshold. | Story variable layer/key |
| `quest_state`, `quest`, `quest_branch` | `id` / `quest` / `quest_branch` | `state` / `value`, `expected` | Quest branch state, defaulting to `take_rooted`. | `quest_branches` |
| `has_feat`, `feat` | `id` / `feat` | `expected` | Unlocked/known feat gate. | `feats` |
| `has_active_feat`, `active_feat`, `feat_active`, `slotted_feat`, `equipped_feat` | `id` / `feat` | `expected` | Active feat-slot gate; requires the feat to be currently active, not merely unlocked. | `feats` |
| `has_journal_entry`, `journal`, `journal_entry` | `id` / `journal` / `journal_entry` | `expected` | Journal entry gate. | `journal_entries` |
| `clue_found`, `has_clue`, `clue` | `id` / `clue` | `expected` | Clue or matching journal-entry gate. | `clues` |
| `relation_at_least`, `relationship_at_least`, `relation`, `relationship` | `id` / `relation` / `relationship`, `min` | `expected` | NPC relationship threshold gate. | `relationships` |
| `npc_state`, `npc_tag`, `has_npc_state`, `has_npc_tag` | `npc` / `npc_id` / `id`, `tag` / `state` / `state_tag` | `scope`, `expected` | NPC memory/state tag gate. | NPC state key |
| `time_window`, `time`, `time_of_day`, `day_time` | `start` / `min`, `end` / `max` | `expected` | Minecraft day-time window; wrap-around windows are supported. | Day time |
| `conflict_state`, `conflict`, `conflict_status` | `id` / `conflict` | `state` / `value`, `expected` | Set-piece conflict state gate. | `conflicts` |
| `scene_phase`, `scene`, `scene_status` | `id` / `scene` | `phase` / `value`, `expected` | Investigation scene phase gate. | `investigation_scenes` |

### Effect reference

| Type / aliases | Required fields | Value fields | Status echo / behavior | Cross-reference target |
|---|---|---|---|---|
| `set_flag`, `setFlag` | `id` / `key` / `flag` | `scope` | Sets a player/world flag. | Local narrative state |
| `clear_flag`, `clearFlag` | `id` / `key` / `flag` | `scope` | Clears a player/world flag. | Local narrative state |
| `set_attribute` | `attribute` / `id`, integer `value` | — | Sets player attribute score. | Attribute key/alias |
| `set_variable`, `set_var`, `setVar` | `id` / `key` / `var` | `string_value` / `text` / `value`, `scope` | Sets a string variable. | Local narrative state |
| `clear_variable`, `clear_var` | `id` / `key` / `var` | `scope` | Clears a string variable. | Local narrative state |
| `set_story_var`, `story_var`, `setStoryVar` | `id` / `story_var` | `value`, `scope`, `layer` | Emits `story_var_set`. | Story variable layer/key |
| `clear_story_var`, `clearStoryVar` | `id` / `story_var` | `scope`, `layer` | Emits `story_var_clear`. | Story variable layer/key |
| `add_story_int`, `addStoryInt`, `increment_story_var` | `id` / `story_var` | `amount` / `delta` / `value`, `scope`, `layer` | Emits `story_var_add`. | Story variable layer/key |
| `start_quest_branch`, `start_quest` | `id` / `quest` / `quest_branch` | — | Starts quest branch. | `quest_branches` |
| `complete_quest_branch`, `complete_quest`, `take_root` | `id` / `quest` / `quest_branch` | — | Completes branch and may Take Root. | `quest_branches` |
| `unlock_feat`, `grant_feat` | `id` / `feat` | — | Unlocks a feat. | `feats` |
| `activate_feat`, `equip_feat`, `slot_feat` | `id` / `feat` | — | Activates feat slot. | `feats` |
| `add_journal_entry`, `journal`, `add_journal` | `id` / `journal` / `journal_entry` | — | Emits journal/clue status. | `journal_entries` |
| `reveal_clue`, `add_clue`, `clue` | `id` / `clue` | — | Reveals clue and linked journal behavior. | `clues` |
| `set_relation`, `relation`, `set_relationship` | `id` / `relation` / `relationship` | integer `value` / `amount` / `delta` | Emits `relation_changed`. | `relationships` |
| `add_relation`, `modify_relation` | `id` / `relation` / `relationship` | integer `value` / `amount` / `delta` | Adds to relation and emits `relation_changed`. | `relationships` |
| `set_npc_state`, `npc_state`, `add_npc_tag` | `id` / `npc` / `npc_id` | `tag` / `state` / `state_tag`, `scope` | Emits `npc_state_set`. | NPC state key |
| `clear_npc_state`, `remove_npc_tag` | `id` / `npc` / `npc_id` | `tag` / `state` / `state_tag`, `scope` | Emits `npc_state_clear`. | NPC state key |
| `set_npc_routine`, `routine`, `set_routine` | `id` / `routine` | — | Requests routine change for interacted Ebb NPC. | `npc_routines` |
| `start_conflict`, `conflict` | `id` / `conflict` | — | Starts set-piece conflict. | `conflicts` |
| `add_conflict_stress`, `add_stress` | `id` / `conflict` | integer `amount` / `delta` / `value` | Adds stress. | `conflicts` |
| `add_conflict_resolve`, `add_resolve` | `id` / `conflict` | integer `amount` / `delta` / `value` | Adds resolve. | `conflicts` |
| `apply_conflict_outcome`, `conflict_outcome`, `resolve_conflict` | `id` / `conflict` | `outcome` / `outcome_id` / `value` | Applies a declared conflict outcome and sets phase. | `conflicts.outcomes` |
| `set_conflict_state` | `id` / `conflict` | `state` / `value` | Emits `conflict_state`. | `conflicts` |
| `set_scene_phase`, `scene_phase` | `id` / `scene` | `phase` / `value` | Emits `scene_phase`. | `investigation_scenes` |
| `add_trait`, `remove_trait` | `id` / `trait` | — | Sets/clears trait flag. | Local narrative state |
| `add_thought`, `remove_thought` | `id` / `thought` | — | Sets/clears thought flag. | Local narrative state |
| `unlock_retry`, `unlock` | `id` / `key` / `unlock` | — | Sets retry unlock flag. | Local narrative state |
| `give_item`, `take_item` | `id` / `key` | — | Placeholder item flag; not real inventory yet. | Placeholder state |
| `routine_placeholder` | `id` / `routine` | — | Legacy placeholder, prefer `set_npc_routine`. | Placeholder state |

High-stakes checks (`dc >= 10` unless explicitly `optional: true`) should include a `failure` branch or `failure_effects`; validation reports them as authoring errors.

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
  "trigger_tags": ["innkeeper.read", "witness.read"],
  "tone_guide": "Argument architecture. Finds leverage in wording, audience, framing, and who loses narrative control.",
  "speaker_style": "argument",
  "cooldown_ticks": 200,
  "one_shot_per_node": true,
  "one_shot_global": false,
  "active_thoughts": ["ebb:demo/thought_rhetoric"],
  "lines": ["别问钥匙。问谁会因为钥匙被交出来而失去叙事控制。"]
}
```

Fields:

| Field | Meaning |
|---|---|
| `source_attribute` / `attribute` | DND-8 attribute key or alias that powers this voice. |
| `min_score` | Minimum player score required before the voice can speak. |
| `trigger_tags` / `tags` | Dialogue-node `chime_tags` that this Chime can answer. |
| `tone_guide` | Author-facing tone contract; keep it specific enough that future lines stay consistent. |
| `speaker_style` | Short rendering/dev label such as `argument`, `soft`, `gut`, or `warning`. |
| `cooldown_ticks` / `cooldown` | Per-player anti-spam cooldown between passive inserts from the same Chime. |
| `one_shot_per_node` / `one_shot` | Default `true`; prevents repeating the same Chime on the same dialogue node. |
| `one_shot_global` | Optional stronger lock that lets this Chime speak only once per player. |
| `active_thoughts` / `active_thought` | Thought IDs unlocked by active follow-up routes associated with this Chime. |
| `lines` / `text` | Passive insert text candidates shown in the dialogue status area. |

Dialogue nodes opt into passive insert resolution with `chime_tags`:

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
- Triggering a chime writes `chime:<id>` as a player flag, so a normal condition can unlock a follow-up thought path.
- `cooldown_ticks`, `one_shot_per_node`, and `one_shot_global` keep Chimes from spamming repeated nodes.
- Active thought routes should usually add the matching thought flag with `add_thought`, allowing the Journal/Quest/Dev views to show that the player internalized the insight.

```json
{ "type": "flag", "scope": "player", "id": "chime:ebb:demo/rhetoric", "value": true }
{ "type": "add_thought", "id": "ebb:demo/thought_rhetoric" }
```

The bundled P26 demo set now provides eight attribute voices: `Force`/strength, `Finesse`/dexterity, `Endurance`/constitution, `Logic`/intelligence, `Empathy`/wisdom, `Rhetoric`/charisma, `Instinct`/perception, and `Dread`/luck. Each has a passive insert plus one active thought route in `ebb:demo/innkeeper_intro`.

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
- Active dialogue sessions with an Ebb NPC pause its movement routine, force a conversation-focus look target, and restore the previous pose/animation when the session closes or times out.
- `/ebb routine inspect <entity>` shows routine id, visual role, pose/animation, current step, current action/target, and whether conversation focus is active.

P27/P33 add role-specific temporary skins and conversation-focus animations. The bundled `ebb:npc` still uses a simple humanoid GeckoLib model, but the renderer chooses role textures such as `npc_innkeeper.png`, `npc_witness.png`, `npc_tenant.png`, `npc_guard.png`, `npc_cook.png`, or `npc_courier.png` from the NPC's routine/narrative key. This is intentionally placeholder art, not final production character art.

Allowed routine actions: `stand`, `wait`, `walk`, `walk_path`, `look_at`, `play_animation`, `set_pose`, `teleport_fallback`.

Allowed routine animations: `idle`, `walk`, `fidget`, `talk`, `think`, `dismiss`, `nervous_idle`, `scripted`. Dialogue focus can select `talk`, `think`, `dismiss`, or `nervous_idle` based on the active dialogue node.

Allowed routine poses: `standing`, `blocking`, `suspicious`, `guarded`, `listening`, `composed`, `restless`, `leaning`, `pacing`, `conversation`, `talking`, `thinking`, `dismissing`, `nervous`, `scripted`. Invalid action, path, pose, or animation names are validation messages and invalid steps are not used.

Routine hardening:

- `steps` must contain at least one valid step.
- Step time windows may wrap around midnight, but two steps in the same routine may not overlap after wrap expansion.
- `teleport_distance` / `teleportFallbackDistance` must be positive.
- `play_animation` and `set_pose` are visible metadata actions through the GeckoLib controller; complex animation queues and authored gesture timelines remain future work.

Known future NPC work: behavior stacks, richer schedules/transitions, patrol pause points, authored gestures, and final humanoid art assets.

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

`EbbNpcEntity` persists a `narrative_key`, `visual_role`, `pose`, and `animation` string. `/ebb routine inspect <entity>` shows these values plus the current routine debug action/target. Dialogue can switch the currently interacted Ebb NPC's routine with:

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

Set-piece conflicts live under `data/<namespace>/conflicts/<id>.json`. P28 formalizes them as a small conversation-combat state machine with phases `setup`, `pressure`, `turn`, `consequence`, and `resolution`. `leverage_clues` names clue ids that should be surfaced in the conflict status UI when known, and `outcomes` declares the named nonviolent, messy, and failure-forward endings that dialogue effects may apply:

```json
{
  "title": "Hallway Confrontation",
  "scene": "ebb:demo/locked_room",
  "phases": [
    { "id": "setup", "text": "Define the stakes." },
    { "id": "pressure", "text": "Stress rises." },
    { "id": "turn", "text": "Leverage converts into resolve." },
    { "id": "consequence", "text": "Failure advances messily." },
    { "id": "resolution", "text": "The scene resolves." }
  ],
  "leverage_clues": [
    "ebb:demo/door_scratches",
    "ebb:demo/witness_knock_pattern"
  ],
  "stress_limit": 3,
  "resolve_goal": 2,
  "failure_state": "failed_forward",
  "success_state": "resolved",
  "outcomes": [
    { "id": "quiet_resolve", "kind": "nonviolent", "state": "resolved_nonviolent" },
    { "id": "messy_resolve", "kind": "messy", "state": "resolved_messy" },
    { "id": "public_pressure_fail", "kind": "failure_forward", "state": "failed_forward_public", "failure_forward": true },
    { "id": "guard_standoff_fail", "kind": "failure_forward", "state": "failed_forward", "failure_forward": true }
  ]
}
```

Conflict effects. Starting or changing stress/resolve returns a `conflict_*` status echo with `state`, `phase`, `stress`, `resolve`, known `leverage`, and outcome count. This is displayed in the dialogue status strip and listed in `/ebb dev` snapshots:

```json
{ "type": "start_conflict", "id": "ebb:demo/hallway_confrontation" }
{ "type": "add_conflict_stress", "id": "ebb:demo/hallway_confrontation", "amount": 1 }
{ "type": "add_conflict_resolve", "id": "ebb:demo/hallway_confrontation", "amount": 2 }
{ "type": "apply_conflict_outcome", "id": "ebb:demo/hallway_confrontation", "outcome": "quiet_resolve" }
{ "type": "set_conflict_state", "id": "ebb:demo/hallway_confrontation", "state": "resolved" }
{ "type": "set_scene_phase", "scene": "ebb:demo/locked_room", "phase": "confrontation" }
```

Conflict/scene conditions:

```json
{ "type": "conflict_state", "id": "ebb:demo/hallway_confrontation", "state": "active" }
{ "type": "scene_phase", "id": "ebb:demo/locked_room", "phase": "messy" }
```

The bundled guard dialogue contains the first expanded set-piece. It uses clue-gated lines, DND-8 d20 checks, stress/resolve effects, and `apply_conflict_outcome` nodes. Known clues both unlock options and modify DCs through the existing clue check modifiers. The demo includes a nonviolent quiet resolution, a messy resolution, and two separate failure-forward outcomes; failure changes the scene into a messier state instead of hard-stopping the vertical slice.

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

## P34 LLM / Free Chat Foundation

P34 adds a disabled-by-default, server-authoritative LLM chat path. The mod jar does **not** contain OpenAI/API secrets and the default/fake providers do not access the network. Server operators may place non-sensitive settings in `config/ebb-llm-server.json`; by default `/ebb llm status` reports `enabled=false mode=disabled` and `llm_disabled` is shown when content tries to open free chat without enabling a provider.

Scripted dialogue remains primary. Add an optional free-chat choice with `type: "llm_chat"` or the alias `type: "free_chat"`:

```json
{
  "id": "free_chat",
  "type": "llm_chat",
  "text": "我们随便聊聊。",
  "llm": {
    "npc": "ebb:demo/innkeeper",
    "topic_hint": "tavern rumors, the locked door, and recent guests",
    "return_node": "start",
    "allow_memory_write": true
  }
}
```

Runtime behavior in P34:

- `DialogueService` intercepts `LLM_CHAT` choices before d20 resolution; no check is rolled for free chat.
- The server validates the existing dialogue target/range/LOS before opening `NpcChatScreen`.
- The client sends only player text, conversation UUID, and a nonce. It never sends API keys or hidden knowledge.
- Fake mode returns deterministic `FAKE_NPC_REPLY` text plus fake citation ids so UI/dev plumbing can be tested without OpenAI.
- Disabled mode surfaces the explicit status `llm_disabled` and leaves the scripted dialogue usable.

Minimal dev config for fake-mode local testing:

```json
{
  "enabled": true,
  "mode": "fake",
  "max_input_chars": 512,
  "session_timeout_ticks": 1200,
  "fake_reply": "FAKE_NPC_REPLY"
}
```

Commands:

- `/ebb llm status` — player-safe status with mode, active session count, provider, and network policy.
- `/ebb llm reload_config` — OP/dev reload of `config/ebb-llm-server.json`.

## P35 NPC Profiles, Tiers, and Minor Promotion

P35 adds `data/<namespace>/npc_profiles/<path>.json` for stable major NPC identity. A profile has a `tier`, a display name, an optional `entity_binding`, LLM policy fields, character voice rules, stance/faction values, and initial knowledge references. The current bundled schema is `docs/schemas/ebb.npc_profile.schema.json`.

Supported `npc_tier` / profile `tier` values are:

- `major_scripted`: data-pack authored major NPC, such as the P30 tavern roles.
- `minor_generatable`: a candidate entity that may become important after first free chat.
- `major_promoted`: a persisted profile generated from a minor NPC.
- `static_non_llm`: an interactable entity that should not use LLM profile/promotion.
- `disabled`: explicitly disabled.

Bundled role profiles now exist for `ebb:demo/innkeeper`, `ebb:demo/witness`, `ebb:demo/tenant`, `ebb:demo/guard`, `ebb:demo/cook`, and `ebb:demo/courier`. Their entity bindings declare `npc_tier: "major_scripted"` and `npc_profile: "ebb:demo/<role>"`, so `/ebb npc profile target` can inspect the character profile when looking at a registered role NPC.

Minor NPCs remain opt-in. Do **not** re-enable debug entity fallback just to make minor NPCs work. A minor candidate should use an explicit entity binding, for example:

```json
{
  "match": {"entity_type": "minecraft:villager", "tag": "ebb.npc.minor"},
  "dialogue": "ebb:llm/minor_intro",
  "npc_tier": "minor_generatable",
  "interaction_range": 2.25,
  "highlight_range": 10.0,
  "priority": 30,
  "llm": {
    "enabled": true,
    "promote_on_first_chat": true,
    "profile_seed_archetypes": ["townsperson", "tavern regular", "worker", "witness"]
  }
}
```

On first eligible fake/LLM chat, `NpcPromotionService` creates a deterministic-enough `major_promoted` profile from world/entity context and stores it in `NarrativeSavedData.promoted_npc_profiles`. Reloading resources does not erase promoted profiles because they live in saved data, not in the datapack registry. Developer commands:

- `/ebb npc profile target`: inspect the looked-at NPC profile or promotion candidate.
- `/ebb npc profile <npc_key>`: inspect a static or promoted profile by id.
- `/ebb npc minorize <entity>`: OP/dev helper that adds the `ebb.npc.minor` tag.
- `/ebb npc promote <entity>`: OP/dev helper that immediately creates/loads the promoted profile.
- `/ebb npc regenerate_profile <npc_key>`: OP/dev helper that removes a saved promoted profile so it can be regenerated.

## P36 Gateway Auth

P36 introduces a separate `ebb-llm-gateway/` service for browser/device authentication. The Minecraft mod still owns gameplay authority, but LLM authentication state is **server-side only**: the client UI never receives `opaque_player_token`, and `/ebb llm status` prints only redacted token fingerprints.

Server config additions in `config/ebb-llm-server.json`:

```json
{
  "enabled": true,
  "mode": "fake",
  "gateway_base_url": "http://127.0.0.1:8787",
  "gateway_timeout_ms": 30000,
  "require_player_auth": true
}
```

Commands:

- `/ebb llm auth` starts device/browser login and prints a verification URL plus user code.
- `/ebb llm status` reports provider/auth state and completes a pending login when the gateway returns authenticated.
- `/ebb llm logout` removes the server-only token and asks the gateway to invalidate it.

Local development may use the gateway's `dev_local` auth provider or the mod-side `DevLocalLlmAuthClient`; production gateway auth supports OIDC device-flow configuration for providers such as Keycloak/Auth0/Stytch through environment variables on the gateway process. Keep provider secrets outside datapacks, resource packs, and the mod jar.

Runtime gating:

- If `require_player_auth=true`, choosing an `llm_chat`/`free_chat` option before login returns `auth_required`.
- After `/ebb llm auth` + `/ebb llm status`, fake-provider chat works without network/OpenAI usage.
- After `/ebb llm logout`, the next chat attempt is again `auth_required`.

## P37 OpenAI Responses Gateway

P37 adds the gateway chat endpoint used by Minecraft gateway mode. The Minecraft client still never sees OpenAI keys or `opaque_player_token`; the dedicated/server side sends player chat to the configured gateway.

Gateway endpoint contract:

```text
POST /v1/chat/message
```

Request fields include `server_id`, `world_id`, `minecraft_player_uuid`, `npc_key`, `npc_display_name`, optional `entity_uuid`, `conversation_id`, `dialogue_id`, `source_node_id`, `topic_hint`, `scene_context`, `message`, `model`, `stream`, `structured`, `store`, and `max_output_tokens`. If player auth is enabled, the Minecraft server adds `opaque_player_token` from server-only auth storage; this token is never present in client payloads or UI.

Response fields include `conversation_id`, `npc_reply`, `mood`, `suggested_options`, `memory_writes`, `citations`, `proposed_effects`, `warnings`, `chunks`, `structured_json`, `provider`, `model`, `store`, `chunked_response`, `status`, and optional `error`.

Gateway provider selection is controlled by gateway environment variables, not by datapack content:

```text
EBB_GATEWAY_CHAT_PROVIDER=fake | mock_openai_responses | openai_responses
EBB_OPENAI_MODEL=gpt-5.2
EBB_OPENAI_STORE=false
EBB_LLM_CHAT_STREAMING=true
EBB_LLM_STRUCTURED_OUTPUT=true
EBB_GATEWAY_CIRCUIT_FAILURE_THRESHOLD=3
EBB_GATEWAY_CIRCUIT_COOLDOWN_MS=30000
```

Privacy defaults: `store:false` is the default. OpenAI conversation state is only requested when the server/gateway config explicitly enables it. Tests and smoke checks use fake or `mock_openai_responses` providers by default, so they do not consume OpenAI API quota.

Minecraft server config additions for gateway chat mode:

```json
{
  "enabled": true,
  "mode": "gateway",
  "gateway_base_url": "http://127.0.0.1:8787",
  "gateway_timeout_ms": 30000,
  "require_player_auth": true,
  "default_chat_model": "gpt-5.2",
  "llm_chat_streaming": true,
  "structured_output": true,
  "openai_store": false
}
```

Failure handling: gateway HTTP errors, timeouts, and open circuit-breaker responses are converted to explicit LLM error statuses such as `llm_gateway_error` or `llm_circuit_open`; the Minecraft UI should not hang waiting for a real provider failure.

## P38 MemoryStore MVP

P38 introduces the first gateway-owned memory store. It is intentionally server/gateway side: Minecraft clients never connect to the DB and never receive raw gateway tokens. The gateway applies `db/migration/V001__memory_store.sql` on startup and stores:

- `MemoryRecord`: append-only player/NPC conversation turns with `memory:record:<id>` citation ids.
- `MemoryFact`: extracted deterministic facts with `memory:fact:<id>` citation ids.
- `MemoryConflict`: open conflicts when a new fact supersedes or contradicts an existing current fact.

Gateway endpoints:

```text
POST /v1/memory/search
GET  /v1/memory/inspect?id=<memory_id>
GET  /v1/memory/conflicts?server_id=<id>&world_id=<id>&limit=25
```

The search endpoint performs hybrid retrieval using recent records, keyword overlap, entity/NPC context, and deterministic local embeddings. This deterministic embedding path is the P38 write/retrieval baseline used by tests; later phases can replace or augment it with OpenAI Embeddings without changing Minecraft client payloads.

Current deterministic fact authoring/testing syntax in player text:

```text
fact:player.favorite=blue
remember:innkeeper.mood=guarded
I am the night clerk
```

When the same `subject.predicate` receives a new value, the old fact is marked `superseded`, the new fact remains `current`, and a `MemoryConflict` is created with both citation ids.

Minecraft developer commands query the gateway from the server:

```text
/ebb memory search <query>
/ebb memory inspect <id>
/ebb memory conflicts
```

These commands require dev permission and gateway mode (`mode: "gateway"`, `gateway_base_url` configured). They are intended for validation and author debugging; normal gameplay retrieval is consumed by gateway chat/retrieval logic.

## P39 Memory extraction / consolidation

Gateway chat responses may include `memory_writes` as **proposals**, not direct writes. Supported proposal strings are intentionally small and auditable:

```json
{
  "npc_reply": "...",
  "memory_writes": [
    "fact:player.questioned_ledger=true",
    "summary:Player previously questioned the ledger. 玩家之前质问过账本。",
    "lesson:Do not accept a player self-claim as canonical ownership."
  ]
}
```

The gateway applies a deterministic validator before anything mutates the memory DB:

- `fact:subject.predicate=value` and `remember:subject.predicate=value` can become `MemoryFact` rows.
- `summary:...` can enrich the raw episode summary.
- `lesson:...` can become a safety lesson.
- Canonical facts are protected. For the demo, a player claim such as `我是旅馆老板` can create a rejected `tavern.owner=player:<uuid>` proposal, but it must not overwrite the canonical innkeeper ownership; the gateway records an A-MemGuard-style lesson instead.

P39 also adds consolidation records:

- `memory_operations` stores proposed/accepted/rejected ops.
- `memory_summaries` stores episodic summaries while `memory_records.text` remains the raw episode.
- `memory_links` connects related episodes.
- `memory_safety_lessons` stores guardrails learned from rejected unsafe/canonical-conflicting proposals.

Developer commands:

```text
/ebb memory search questioned_ledger
/ebb memory inspect <memory_id>     # raw_episode, extracted_facts, operations, summaries, links, lessons
/ebb memory conflicts
/ebb memory episodes
/ebb memory lessons
```

Acceptance examples:

- Player: `我是旅馆老板` → rejected canonical ownership proposal; `/ebb memory lessons` contains `canonical owner remains innkeeper`.
- Player previously questioned the ledger → `/ebb memory search questioned_ledger` returns a citation id and inspectable raw episode/summary.

## P40 NPC Knowledge Base

P40 adds data-driven NPC knowledge packs. The data folder is:

```text
src/main/resources/data/ebb/npc_knowledge_packs/<namespace path>.json
```

Draft schema shape:

```json
{
  "chunks": [
    {
      "id": "public_lore",
      "text": "The innkeeper publicly admits the guestbook is incomplete.",
      "tags": ["ledger", "inn"],
      "secret": false
    },
    {
      "id": "secret_ledger_cash",
      "text": "Secret ledger detail: the tenant paid cash.",
      "tags": ["ledger", "tenant"],
      "secret": true,
      "reveal_conditions": [
        { "type": "clue_found", "id": "ebb:demo/guestbook_gap" }
      ]
    }
  ]
}
```

Runtime/story effects added by the draft:

```json
{ "type": "npc_kb_add_fact", "npc": "ebb:demo/innkeeper", "fact": "knows_player_checked_ledger" }
{ "type": "npc_kb_add_pack", "npc": "ebb:demo/innkeeper", "pack": "ebb:demo/innkeeper_private_ledger" }
{ "type": "npc_stance_shift", "npc": "ebb:demo/innkeeper", "stance": "defensive" }
```

The LLM prompt assembler retrieves only chunks whose `reveal_conditions` pass for the current player. Use `/ebb kb inspect <npc>` or `/ebb kb inspect <npc> <query>` as an OP/dev command to show the current player-specific visible and hidden chunks. Hidden chunks are listed by id for developer review but are never included in LLM prompt context until their conditions pass.


## P41 Minor NPC instant generation

Minor NPCs are opt-in. A normal villager becomes eligible only when a binding/tag marks it as a minor candidate:

```json
{
  "match": {"entity_type": "minecraft:villager", "tag": "ebb.npc.minor"},
  "dialogue": "ebb:llm/minor_intro",
  "npc_tier": "minor_generatable",
  "llm": {
    "enabled": true,
    "promote_on_first_chat": true,
    "profile_seed_archetypes": ["townsperson", "tavern regular", "worker", "witness"]
  }
}
```

On the first eligible fake/LLM chat, `NpcProfileGenerator` creates a promoted major profile with `character`, `stance`, `knowledge`, `knowledge_seed`, `suggested_options`, and `profile_generation` metadata. The generator has an auditable prompt/schema contract (`ebb.npc_profile_generator.v1`) while the MVP remains deterministic and network-free in tests.

Developer commands:

```text
/ebb npc minorize <entity>
/ebb npc promote <entity>
/ebb npc profile target
/ebb npc profile <npc_key>
/ebb npc review <npc_key>
/ebb npc reject_profile <npc_key>
/ebb npc regenerate_profile <npc_key>
```

Promotion is rate limited per world hour (`MAX_PROMOTIONS_PER_WORLD_HOUR`) to avoid turning every tagged background entity into a major NPC at once. Existing promoted profiles are reused, so leaving and re-entering the world keeps the same generated profile.

## P42 LLM Chat UI

The `llm_chat` / `free_chat` choice opens `NpcChatScreen` instead of rolling a check. The UI now treats LLM chat as a temporary branch away from scripted dialogue, not a replacement for the authored scene.

Authoring knobs remain on the choice:

```json
{
  "id": "free_chat",
  "type": "llm_chat",
  "text": "我们随便聊聊。",
  "llm": {
    "npc": "ebb:demo/innkeeper",
    "topic_hint": "tavern rumors, the locked door, and recent guests",
    "return_node": "start",
    "allow_memory_write": true
  }
}
```

Player-facing UI behavior:

- Streaming text: server replies are sent as `LlmChatChunkPayload` chunks when `llm_chat_streaming` is enabled. The client merges chunks into one NPC line until `done=true`.
- Suggested options: gateway/fake replies may include `suggested_options`; selecting one sends it as the next player message.
- `return_to_script`: the Return to Script button sends a server-bound cancel reason. The server reopens a normal scripted `DialogueSession` at the choice's `return_node` using authoritative dialogue data.
- Memory correction: the Correct Memory button prefixes the player's next message as `memory_correction: ...`, so gateway memory extraction can treat it as a player-authored correction rather than hidden game state.
- Citations overlay: citations are hidden by default and visible only through the dev citations overlay. They are not appended inline to ordinary NPC text.
- Timeout/error/cancel behavior: local timeout, provider errors, network-unavailable sends, and explicit cancel all release input/buttons instead of leaving the screen stuck in a waiting state.
- K menu: the mod menu opened with `K` includes LLM auth status/login/logout actions; `/ebb llm status` remains the authoritative, token-redacted server status output.

GUI automation includes a `llm_chat` scenario that opens the innkeeper free-chat choice, sends text, waits for a fake/gateway reply, toggles citations, clicks a suggested option, and returns to scripted dialogue.

## P43 Testing / Evaluation Authoring Reference

P43 freezes the LLM/NPC-memory authoring contract into docs, schemas, audits, and tests.  Treat this section as the checklist for story-pack authors and server operators before using real gateway mode.

### NPC profile data

NPC profiles live in `data/<namespace>/npc_profiles/<path>.json` and are validated against `docs/schemas/ebb.npc_profile.schema.json`.  Required author-facing fields are:

- `tier`: one of `major_scripted`, `minor_generatable`, `major_promoted`, `static_non_llm`, or `disabled`.
- `display_name`: player-facing name.
- `character`: at least `archetype` and `voice`, usually with `values`, `fears`, and `speech_rules`.
- `stance`: faction and attitude scores such as `trust`, `fear`, and `resentment`.
- `knowledge.initial_packs`: public/hidden KB packs initially available to the NPC.
- `llm`: optional chat policy.  `provider` is a policy hint only; server config still controls real provider mode.

Promoted minor NPC profiles are saved in world data (`promoted_npc_profiles`) and must remain deterministic once created.  Do not author ordinary datapack entities as `major_promoted`; use `minor_generatable` bindings plus promotion instead.

### NPC knowledge packs / KB

NPC knowledge packs live in `data/<namespace>/npc_knowledge_packs/<path>.json` and are validated against `docs/schemas/ebb.npc_knowledge.schema.json`.

```json
{
  "chunks": [
    {
      "id": "public_guestbook_gap",
      "text": "The innkeeper admits the guestbook has a missing page.",
      "tags": ["guestbook", "ledger"],
      "secret": false
    },
    {
      "id": "secret_cash_payment",
      "text": "Secret ledger detail: the tenant paid cash.",
      "tags": ["ledger", "tenant"],
      "secret": true,
      "reveal_conditions": [
        {"type": "clue_found", "id": "ebb:demo/guestbook_gap"}
      ]
    }
  ]
}
```

Secret chunks must have `reveal_conditions`.  Prompt assembly includes only visible chunks; `/ebb kb inspect <npc> [query]` can show hidden chunk ids for OP/dev review, but client sync payloads must not include hidden KB text.

### LLM server config

`config/ebb-llm-server.json` is a server-side file, not datapack content.  The mod jar must not contain API keys.  Fake mode should be used in tests and GUI automation:

```json
{
  "enabled": true,
  "mode": "fake",
  "require_player_auth": false,
  "llm_chat_streaming": true,
  "structured_output": true,
  "openai_store": false,
  "fake_reply": "FAKE_NPC_REPLY"
}
```

Gateway mode is a server-to-gateway dry-run or real-provider path.  It still sends only server-owned data and optional server-only opaque player tokens to the gateway; the Minecraft client never receives tokens or OpenAI secrets:

```json
{
  "enabled": true,
  "mode": "gateway",
  "gateway_base_url": "http://127.0.0.1:8787",
  "require_player_auth": true,
  "default_chat_model": "gpt-5.2",
  "llm_chat_streaming": true,
  "structured_output": true,
  "openai_store": false
}
```

### Memory effects and LLM memory writes

Scripted dialogue can update memory/KB through ordinary server-authoritative effects:

```json
{"type":"npc_kb_add_fact","npc":"ebb:demo/innkeeper","fact":"player_checked_back_door"}
{"type":"npc_kb_add_pack","npc":"ebb:demo/innkeeper","pack":"ebb:demo/kitchen_manifest"}
{"type":"npc_stance_shift","npc":"ebb:demo/innkeeper","stance":"defensive"}
{"type":"add_relation","relation":"ebb:demo/innkeeper","amount":1}
{"type":"set_npc_state","npc":"ebb:demo/innkeeper","tag":"player_apologized"}
```

LLM/gateway `memory_writes` are proposals.  They pass through deterministic validation, can create `MemoryConflict` rows, and may create safety lessons when they contradict canonical facts.  `proposed_effects` from the LLM are **not** applied directly to Minecraft state; high-risk effects such as quest completion, item grants, flags, routine changes, clue reveals, relation mutation, or commands are rejected/sanitized and must be implemented as scripted dialogue effects instead.

### P43 verification matrix

- Static audit: `scripts/p43_llm_safety_audit.py` plus `scripts/goal_static_audit.py` check no secret-like API key literals, fake/mock providers in tests, no hidden KB text in client sync payloads, and no high-risk direct LLM effects.
- JUnit: `p43TestingEvaluationAndSafetyGatesAreAuditable` covers memory conflict evidence, promotion persistence, prompt/KB pack assembly, docs/schemas, and GUI route markers.
- GameTest: `p43FakeChatMinorPromotionAndRelationshipDeltaAreDeterministic` covers fake-provider chat, minor promotion persistence, and relationship delta state in a headless Minecraft server.
- GUI E2E: `scripts/gui_e2e_run.py --scenario llm_validation` documents auth-disabled/status, fake chat, and gateway dry-run routes; use `--gui` for a live client run.
