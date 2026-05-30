# Review Remediation Report — 2026-05-30

Source review: `C:\Users\lanla\Downloads\ebb_project_review.md`.

## Completed fixes

- Dedicated-server block-group visibility:
  - Added `BlockGroupSyncPayload` and server lifecycle sync on join/data-pack sync/reload.
  - Added client-only `ClientBlockGroupIndex` and changed crosshair block-group detection to use the synced client index rather than server-data reload state.
- Entity bindings:
  - Added typed `EntityBindingDefinition` / `EntityBindingRegistry` with matching by UUID, scoreboard tag, custom/name, entity type, priority, and fallback debug binding.
  - Server entity validation now resolves dialogue/range from entity bindings instead of always opening `ebb:debug/entity`.
  - Added sample bindings for tagged villagers and `ebb:npc`.
- Developer tree browser:
  - Extended `/ebb dev` output with complete dialogue trees: dialogue id, start node, node text/text_key, node enter effects, choices, conditions, effects, checks, and check outcome effects.
  - Dev output also lists entity bindings and NPC routines.
- Check/effect semantics:
  - `check` now supports `success_effects`, `failure_effects`, `critical_success_effects`, and `critical_failure_effects`.
  - Nodes now support `enter_effects`.
  - Existing choice-level `effects` remain explicit pre/outcome-independent effects.
- Dialogue session lifecycle:
  - Sessions close on disconnect/leave/respawn/level change/server stopping.
  - Sessions timeout after 5 minutes of inactivity.
  - `ACTION` choices revalidate target range/LOS/server existence before effects or branch resolution.
- Validation/codec hardening:
  - Dialogue missing references are hard-invalid instead of only warnings.
  - Attribute default scores are clamped into `[min,max]` with a validation message.
  - Packet list decoders reject invalid counts instead of truncating and leaving unread bytes.
  - Block groups support optional block predicates via `{"pos":[x,y,z], "block":"minecraft:id"}` while preserving legacy `[x,y,z]` entries.
- Dialogue UI improvements:
  - Dialogue screen no longer pauses the world.
  - Added visible close/end button, scrollable long text, paged choice list, pending-choice button disable, server-waiting status, and `text_key`/choice `text_key` support.
- NPC/routine/look-at-player:
  - Added `ebb:npc` custom entity skeleton registered with attributes.
  - Added GeckoLib `GeoEntity` integration, renderer registration, model/animation/texture assets, and generic idle/walk animation controller.
  - Added `/ebb summon_npc <routine>` OP command.
  - Invalid routine identifiers fail gracefully instead of throwing a command exception.
  - Added typed NPC routine parser/registry and basic routine controller for stand/walk destination and `look_at_player` behavior.
  - Routine dialogue effect can set an interacted `EbbNpcEntity` routine id.

## Verification performed

```bash
scripts/gradle-local.sh --no-daemon build
```

Result: `BUILD SUCCESSFUL`.

Additional parser/registry smoke test compiled and ran from `build/tmp/verify-src/ReviewSmoke.java`:

```text
ReviewSmoke passed: dialogues=3, attributes=3, block_groups=1, entity_bindings=2, npc_routines=1, messages=1
```

The single smoke warning/message is expected in the standalone smoke process because it does not initialize the mod entity registry before parsing `ebb:npc`; the actual mod initializer registers `ebb:npc` before data reload listeners.

Jar evidence:

- `build/libs/ebb-0.1.0-dev.jar` SHA-256: `9f6a09ceb005aea6148aaa3d8e1545212c9ca6aaca9305aba2a1b8ca3a85624e`
- `build/libs/ebb-0.1.0-dev-sources.jar` SHA-256: `c5e3857cf88f4262890b2195b7e67822329dc0ae11dca19ad3a12cfaef101c96`

Jar inspection confirmed packaged classes/resources for block-group sync, client index, entity bindings, dev tree dumper, `EbbNpcEntity`, NPC routines, sample entity bindings, sample routine, and GeckoLib assets.

`runServer --args nogui` smoke loaded Fabric/Minecraft/GeckoLib/Ebb and reached the expected EULA gate without an Ebb mod initialization crash.
