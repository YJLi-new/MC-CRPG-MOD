# Release Metadata Draft — Esoteric Ebb CRPG Alpha

This file is a draft for public release pages. Do not submit it blindly; update screenshots, final version numbers, and license links at release time.

## Shared metadata

- **Project name:** Esoteric Ebb CRPG
- **Mod id:** `ebb`
- **Current version:** `0.1.0-dev`
- **Loader:** Fabric only
- **Minecraft version:** `26.1.2`
- **Java version:** `25`
- **Required dependencies:** Fabric API `0.150.0+26.1.2`, GeckoLib `5.5.1`
- **Environment:** client and server required for multiplayer; client required for UI/highlights/NPC rendering
- **Categories:** Adventure, Game Mechanics, Utility, Worldgen/Structures optional only if future maps add them
- **Tags:** CRPG, dialogue, investigation, quests, NPC, d20, data-driven, Fabric
- **License summary:** see `LICENSE.md` for code/data/assets split

## Short description

A Fabric 26.1.2 Disco-like CRPG framework for Minecraft: explicit interactable targets, dialogue/action/thought choices, server-authoritative d20 checks, failure-forward story state, Chime inner voices, journals, quest branches, feats, NPC routines, and set-piece conflicts.

## Long description draft

**Esoteric Ebb CRPG** turns small Minecraft spaces into dense CRPG scenes. Look at authored block groups or bound NPCs, press **X**, and enter a dialogue/investigation UI with d20 checks, failure-forward consequences, journal/clue updates, quest branches, Take Root moments, feats, relationships, and inner-voice Chime inserts.

The current alpha includes a tavern vertical slice with:

- 3-act discovery → pressure/investigation → confrontation/ending structure;
- 12 interactable block-group investigation points;
- 6 role NPCs with explicit bindings and routines;
- 4 major and 8 minor quest branches;
- 12 feats;
- 8 DND-8 Chimes with 40 passive lines;
- 20 journal entries and 21 clue definitions;
- 3 dialogue set-piece conflicts;
- public, quiet, messy, trade, and mercy ending placeholders.

The mod is intended for authored story packs. Content is data-driven through JSON/YAML-like authoring sources and validation scripts.

## Installation text

Install Fabric Loader `0.19.2` for Minecraft `26.1.2`, then place these jars in `mods/`:

1. `ebb-0.1.0-dev.jar`
2. `fabric-api-0.150.0+26.1.2.jar`
3. `geckolib-fabric-26.1.2-5.5.1.jar`

Dedicated servers and multiplayer clients should all install the same Ebb/Fabric API/GeckoLib stack.

## Modrinth draft fields

```yaml
project_type: mod
title: Esoteric Ebb CRPG
slug: esoteric-ebb-crpg
client_side: required
server_side: required
loaders:
  - fabric
game_versions:
  - 26.1.2
license: mixed-see-LICENSE.md
categories:
  - adventure
  - game-mechanics
  - utility
required_dependencies:
  - Fabric API 0.150.0+26.1.2
  - GeckoLib 5.5.1
```

## CurseForge draft fields

```yaml
name: Esoteric Ebb CRPG
class_id: mc-mods
game_versions:
  - Minecraft 26.1.2
  - Fabric
  - Java 25
release_type: alpha
dependencies:
  required:
    - Fabric API
    - GeckoLib
```

## Release checklist

- [ ] Confirm jar hash against `docs/current_status.md`.
- [ ] Run `scripts/run_smoke_checks.sh` and `scripts/gradle-local.sh --no-daemon runGametestServer --args nogui`.
- [ ] Relaunch the client and capture current screenshots or GUI automation report.
- [ ] Update `CHANGELOG.md` with the final release date.
- [ ] Confirm `LICENSE.md` is acceptable for public distribution.
- [ ] Upload only the mod jar; do not bundle Minecraft, Fabric API, GeckoLib, or user saves.
