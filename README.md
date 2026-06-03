# Esoteric Ebb CRPG

Esoteric Ebb CRPG (`ebb`) is a Fabric Minecraft Java Edition `26.1.2` mod prototype for a Disco-like CRPG conversation/investigation framework. It supports crosshair-based interaction with explicit block groups and bound entities, server-authoritative dialogue/check resolution, d20 rolls, layered narrative state, Quest Branch / Take Root / Feat growth, Chime inner voices, journal/clues, relationships, NPC routines, investigation scenes, and dialogue set-piece conflicts.

## Stack

- Minecraft Java Edition `26.1.2`
- Fabric Loader `0.19.2`
- Fabric API `0.150.0+26.1.2`
- Fabric Loom `1.17.0-alpha.13`
- Java `25`
- GeckoLib `5.5.1`

See [`GOAL.md`](GOAL.md) for the product roadmap, [`docs/architecture.md`](docs/architecture.md) for runtime architecture, and [`docs/installation.md`](docs/installation.md) for client/dedicated-server setup.

## Repository layout

```text
src/main/java/com/crpg/ebb/        common/server-authoritative runtime
src/client/java/com/crpg/ebb/      client-only detection, rendering, GUI, networking receivers
src/main/resources/data/ebb/       bundled story/runtime JSON data
authoring/                         YAML/JSON authoring sources and examples
scripts/                           build, validation, smoke, GUI automation helpers
docs/                              architecture, authoring, verification, and status docs
tools/gui_automation/              mineflayer/MineDojo-compatible GUI automation helpers
```

## Build and validate

Use the project-local JDK/Gradle helper:

```bash
scripts/gradle-local.sh --no-daemon build
scripts/gradle-local.sh --no-daemon validateEbbData
```

Strong verification before claiming completion:

```bash
python3 scripts/goal_static_audit.py
python3 scripts/deep_research_static_audit.py
python3 scripts/third_review_static_audit.py
scripts/compile_authoring_sources.py --clean
scripts/gradle-local.sh --no-daemon test
scripts/gradle-local.sh --no-daemon validateEbbData
scripts/gradle-local.sh --no-daemon runGametestServer --args nogui
scripts/run_smoke_checks.sh
git diff --check
```

## Play in the local test profile

Do not modify the vanilla `26.1.2` profile in place. Refresh the separate Fabric/PCL test profile instead:

```bash
scripts/configure_pcl_test_client.sh
```

Then launch `26.1.2-Fabric-Ebb-Test` from Plain Craft Launcher and open the test world `新的世界 (1)`. Useful checks:

```bash
scripts/check_pcl_runtime_loaded.py
scripts/run_gui_automation_smoke.sh
python3 scripts/gui_e2e_run.py --scenario runtime_check
python3 scripts/gui_e2e_run.py --scenario gui_retest --gui --gui-wait 1.4
```

Only claim GUI verification when the GUI automation or a human run has actually run against the refreshed jar.

## Player/dev commands

Player-facing/self-inspection:

```text
/ebb
/ebb status
/ebb vars
/ebb dialogue vars
/ebb journal
/ebb quest
/ebb quest tree
/ebb attributes
/ebb attributes spend <attribute> <points>
```

OP/dev:

```text
/ebb dev
/ebb dev on
/ebb dev off
/ebb data
/ebb dialogue list
/ebb dialogue inspect <id>
/ebb dialogue tree <id>
/ebb dialogue reload
/ebb routine inspect <npc_or_id>
/ebb summon_npc <routine_or_role>
/ebb save_debug
```

## Authoring

Read [`docs/json_authoring_guide.md`](docs/json_authoring_guide.md) for the full contract and [`docs/story_pack_tutorial.md`](docs/story_pack_tutorial.md) for a step-by-step block-group + dialogue + check tutorial. Runtime data lives under `src/main/resources/data/ebb/**`; authoring examples compile with:

```bash
scripts/compile_authoring_sources.py --clean
```

The current bundled vertical slice is a compact tavern/side-door case with six role NPCs, 12 interactable block groups, four major quest routes, eight minor branches, 12 feats, eight Chimes / 40 passive lines, 20 journal entries, 21 clues, three set-piece conflicts, and ending placeholders.

## Current status

See [`docs/current_status.md`](docs/current_status.md). Historical docs that mention pending GUI retests are preserved as audit history and superseded by the final GUI automation evidence listed there.

## Release packaging

P31 alpha release preparation docs:

- [`docs/installation.md`](docs/installation.md) — client, PCL test profile, and dedicated server installation.
- [`docs/release_metadata_draft.md`](docs/release_metadata_draft.md) — Modrinth/CurseForge metadata draft.
- [`docs/story_pack_tutorial.md`](docs/story_pack_tutorial.md) — custom story-pack tutorial.
- [`CHANGELOG.md`](CHANGELOG.md) — alpha changelog.
- [`LICENSE.md`](LICENSE.md) — code/data/assets license split.
