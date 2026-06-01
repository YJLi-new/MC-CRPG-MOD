# Deep Research Report Implementation Audit — 2026-05-31

Source request: implement all actionable content from `C:\Users\lanla\Downloads\deep-research-report (2).md` in the existing Fabric Minecraft Java Edition `26.1.2` project.

## Version decision

The report recommends Minecraft `1.21.1` + Fabric + JDK 21 as a stable mainline and treats `26.1.2` + JDK 25 as an experimental line. This project is already constrained by the user goal and working test profile to Fabric Minecraft `26.1.2`, Fabric Loader `0.19.2`, Fabric API `0.150.0+26.1.2`, Loom `1.17.0-alpha.13`, GeckoLib `5.5.1`, and local JDK 25. Therefore the implementation keeps the current 26.1.2 stack and adapts the report's architecture/data/tooling recommendations without migrating versions.

## Implemented architecture/schema items

- Added explicit domain contracts under `com.crpg.ebb.api`:
  - `TargetRef`, `HitContext`, `InteractableTarget`, `InteractionOpenResult`
  - `DialogueRepository`, `DialogueRuntime`, `DialogueStepResult`
  - `RollRule`, `RollContext`, `RollOutcome`, `RollService`
  - `ValidationReport`, `ReloadReport`
- Added dialogue node and roll taxonomy:
  - `DialogueNodeType`: `line`, `choice`, `roll`, `effect`, `jump`, `end`
  - `RollMode`: `retryable` / `one_shot`, with `white` / `red` aliases
- Extended dialogue parsing to support report-style author fields:
  - top-level node `type`, node `next`, node `effects` / `enter_effects`
  - choice `kind`, `label`, `roll`, `once`
  - check `ability`, `mode`, `advantage`, `modifier`, `modifiers`
  - outcome-specific `success_effects`, `failure_effects`, `critical_success_effects`, `critical_failure_effects`
- Enforced failure-forward semantics: any checked/rolled choice must provide an explicit failure target or fallback `next`.
- Added narrative variables and richer condition/effect vocabulary:
  - player/world variables in `NarrativeSavedData`
  - conditions: `has_flag`, `not_flag`, `has_trait`, `has_thought`, `variable_equals`, `attribute_at_least`
  - effects: `set_var`, `clear_var`, `add_trait`, `remove_trait`, `add_thought`, `remove_thought`, `unlock_retry`
- Extended block/entity authoring aliases:
  - block groups support `id`, `anchor`, and `boxes`
  - block group `dialogue` values default to the `ebb` namespace when omitted
  - entity binding dialogue IDs default to `ebb`, while un-namespaced entity type IDs default to `minecraft`

## Implemented developer tooling

Existing OP-only `/ebb dev` snapshot/browser remains available. The report's command list is now represented by server commands:

- `/ebb dev on`
- `/ebb dev off`
- `/ebb dev summary`
- `/ebb dialogue inspect current`
- `/ebb dialogue inspect entity <entity>`
- `/ebb dialogue inspect dialogue <dialogue_id>`
- `/ebb dialogue tree <dialogue_id>`
- `/ebb dialogue vars`
- `/ebb dialogue vars <player>`
- `/ebb dialogue reload`
- `/ebb routine inspect <entity>`
- `/ebb export save-debug`

Notes:

- Commands are gated with Fabric permission predicates at gamemaster level.
- `dialogue inspect current` performs a server-side 10-block view ray against registered entity bindings and block groups, then prints the target's dialogue tree.
- `dialogue vars` reads server saved narrative state and reports variables, flags/tags, attribute points, and active dialogue session.
- `export save-debug` writes a pretty JSON snapshot into the current world under `ebb-debug-exports/` when invoked in-game.

## Implemented authoring pipeline

Author-friendly examples now live under `authoring/`:

- `authoring/dialogues/harbor_clerk_intro.yaml`
- `authoring/interactables/city_office_counter.json`
- `authoring/npc/harbor_clerk_day_cycle.yaml`

Compiler:

```bash
scripts/compile_authoring_sources.py --clean
```

Output root by default:

```text
build/generated/ebb_authoring/data/ebb/
```

Generated runtime JSON categories:

- `dialogues/`
- `interactions/block_groups/`
- `interactions/entity_bindings/`
- `npc_routines/`

Gradle entrypoints:

```bash
scripts/gradle-local.sh --no-daemon compileEbbAuthoring
scripts/gradle-local.sh --no-daemon validateEbbData
```

Tracked smoke runner:

```bash
scripts/run_smoke_checks.sh
```

Tracked CI baseline:

- `.github/workflows/build.yml`
- Sets up Temurin JDK 25.
- Installs `PyYAML` for authoring compilation.
- Compiles authoring sources, runs `test validateEbbData`, runs the Fabric game-test server, and builds the mod jar.

## Scope notes

- The report recommends optional YACL/owo/SmartBrainLib and later common/platform split. MVP does not add these dependencies; it keeps the current single Fabric project with strong package boundaries, which matches the report's recommendation to avoid cross-loader and dependency over-expansion on Day 1.
- NPC/routine remains an MVP skeleton: `ebb:npc`, GeckoLib idle/walk renderer, sequential waypoints, look-at-player, summon/inspect commands. More complex animation/behavior stacks are future iteration work.
- Existing dedicated-server-safe interaction sync, entity target sync, block group sync, server-authoritative dialogue/dice/effects/persistence, UI, and registered-target filtering remain intact.

## Verification commands used

```bash
scripts/gradle-local.sh --no-daemon build
scripts/compile_authoring_sources.py --clean
scripts/run_smoke_checks.sh
scripts/gradle-local.sh --no-daemon validateEbbData
scripts/gradle-local.sh --no-daemon runGametestServer --args nogui
python3 scripts/deep_research_static_audit.py
```

`DeepResearchSmoke` verifies the new report-driven items: authoring compilation, node type parsing, ONE_SHOT/single-use checks, modifiers, failure effects, variable/trait/unlock effects, flag-gated thoughts, generated block-group/entity-binding/routine loading, and failure-forward validation.

Additional automated coverage added on 2026-06-01:

- JUnit: `src/test/java/com/crpg/ebb/DeepResearchDataTest.java`
  - 5 tests, 0 failures/errors in `build/test-results/test/TEST-com.crpg.ebb.DeepResearchDataTest.xml`.
  - Covers bundled registry cleanliness, fail-forward validation, variable/trait/thought/unlock effects, saved-data codec round-trip for variables/flags, and compiled authoring output.
- Fabric GameTest: `src/main/java/com/crpg/ebb/test/EbbGameTests.java`
  - Registered through the `fabric-gametest` entrypoint in `fabric.mod.json`.
  - `runGametestServer --args nogui` produced `All 4 required tests passed :)`.
  - Covers bundled runtime registries, NPC spawn/routine state, and tagged Ebb NPC entity-binding resolution.

Final artifact evidence after the added tests:

- `build/libs/ebb-0.1.0-dev.jar` SHA-256 `4edd480212dae954c67cd9185cbdc98b4d836a3868bcbd3b86e7afc0174a1c48`
- `build/libs/ebb-0.1.0-dev-sources.jar` SHA-256 `862c5ef166d5502e011b784c165070093758dba7e9313fd99044c75e67ee33fe`


## Continuation hardening — 2026-06-01

A second completion audit treated the earlier status as unproven and tightened implementation around report details that were previously only weakly evidenced:

- `DialogueScreen` now stores a scrollable session history log, so the dialogue body is a log/rhythm surface rather than only the latest line.
- `NpcRoutineController` now pauses movement and looks at the conversation player while an Ebb NPC is in an active dialogue session.
- `NpcRoutineDefinition.LookAtPlayer` and the authoring compiler now preserve `requires_line_of_sight` / `requiresLineOfSight`.
- `NarrativeSavedData` now includes a persisted schema version for migration readiness.

- `scripts/deep_research_static_audit.py` is now included in `scripts/run_smoke_checks.sh` to verify report-facing architecture/schema/UI/NPC/persistence/devtool/test surfaces.
