# Changelog

## 0.1.0-dev — Alpha vertical-slice work in progress (2026-06-03)

### Added

- Fabric 26.1.2 / Java 25 / GeckoLib 5.5.1 CRPG framework baseline.
- Explicit block-group and entity-binding interaction targets with server validation and client highlights/prompts.
- Dialogue/action/thought UI with paging, keyboard navigation, history, roll/status echoes, font scale, and text speed settings.
- Server-authoritative d20 checks with criticals, hidden DC/roll display controls, success/failure effects, and failure-forward validation.
- Narrative persistence for player/world state, story variables, quests, feats, chimes, journal entries, clues, relationships, routines, investigation scenes, and conflicts.
- Quest Branch / Take Root / Feat systems and UI.
- Eight DND-8 Chime inner voices with active thought routes, cooldowns, and one-shot controls.
- Journal and Quest Tree UI filters.
- Custom `ebb:npc` entity with GeckoLib placeholder humanoid skins, routines, conversation focus pause/restore, and routine debug output.
- Investigation clues and dialogue set-piece conflict phases/outcomes.
- P30 tavern vertical slice: 3 acts, 12 block groups, 6 role NPCs, 4 major branches, 8 minor branches, 12 feats, 8 Chimes / 40 lines, 20 journal entries, 21 clues, 3 set-piece conflicts, and ending placeholders.
- Authoring compiler, JSON schemas, cross-registry validator, smoke runner, static audits, JUnit tests, Fabric GameTests, and GUI automation scaffolding.
- Installation guide, story-pack tutorial, release metadata draft, and mixed license clarity.

### Fixed

- Disabled release-default debug entity fallback so arbitrary entities are not interactable.
- Added dedicated-server-safe sync for block groups, entity bindings, and registered entity targets.
- Fixed roll-result visibility for checked dialogue choices.
- Fixed dialogue UI status/choice overlap at multiple GUI scales.
- Fixed existing-save role NPC binding compatibility and player-facing self-inspection commands.
- Added relationship definitions for new cook/courier support NPCs after P30 validation caught missing references.

### Known alpha limitations

- NPC art and animation are placeholder production assets, not final character art.
- The tavern endings are placeholders for route proof, not final story conclusions.
- Public distribution metadata is a draft and should be reviewed before uploading to Modrinth/CurseForge.
