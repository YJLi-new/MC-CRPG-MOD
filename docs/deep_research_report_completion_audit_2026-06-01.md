# Deep Research Report Completion Audit — 2026-06-01

Source: `C:\Users\lanla\Downloads\deep-research-report (2).md`.

Scope decision: the report recommends a stable Minecraft `1.21.1`/JDK21 mainline, but this project is already constrained to Fabric Minecraft Java Edition `26.1.2`, Fabric Loader `0.19.2`, Fabric API `0.150.0+26.1.2`, Loom `1.17.0-alpha.13`, GeckoLib `5.5.1`, and JDK25. This audit therefore treats “complete” as: all actionable architecture, schema, tooling, test, and MVP gameplay recommendations were adapted to the current 26.1.2 codebase without migrating or weakening prior project constraints.

## Requirement-by-requirement status

| Report area | Status | Evidence |
|---|---:|---|
| Keep MVP stack small, single Gradle project, strong package boundaries | Complete | Existing single Fabric project; new domain-facing contracts under `src/main/java/com/crpg/ebb/api/`; no optional YACL/owo/SmartBrainLib dependency bloat. |
| Core interaction remains client-predicted but server-authoritative | Complete | Existing `interaction`, `network`, `InteractionService`, sync payloads, registered entity filtering, block-group sync, and server dialogue open path remain wired; `scripts/third_review_static_audit.py` asserts this. |
| Dialogue graph schema: nodes/options/conditions/effects/jumps | Complete | `DialogueDefinition`, `DialogueNode`, `DialogueChoice`, `DialogueNodeType`, `DialogueCondition`, `DialogueEffect`; parser accepts report-style aliases `kind`, `label`, `roll`, `next`, `effects`, `enter_effects`. |
| Failure-forward checked choices | Complete | `DialogueDefinition` validation rejects rolled choices without explicit failure/fallback; covered by JUnit `checkedChoicesMustFailForward` and smoke checks. |
| d20 roll model with retryable/one-shot, advantage, modifiers, crit routes | Complete | `RollMode`, `DialogueCheck`, `DialogueService`, `RollRule` contract; supports success/failure/critical outcome routes and outcome effects. |
| Rich narrative state: flags, variables, traits, thoughts, unlock tags | Complete | `NarrativeSavedData`, `PlayerNarrativeState`, richer `DialogueCondition`/`DialogueEffect`; covered by JUnit and `DeepResearchSmoke`. |
| Saved-data versioning for migration readiness | Complete | `NarrativeSavedData.CURRENT_SCHEMA_VERSION`, codec field `version`, debug snapshot version, and JUnit codec round-trip assertion. |
| OP-gated developer commands and tree/vars/routine/export tooling | Complete | `ModCommands` adds `/ebb dev on/off/summary`, `/ebb dialogue inspect/tree/vars/reload`, `/ebb routine inspect`, `/ebb export save-debug`; `DialogueDebugDumper` formats dialogue and routine trees. |
| Author-friendly YAML/JSON source pipeline | Complete | `authoring/` examples, `scripts/compile_authoring_sources.py`, generated output under `build/generated/ebb_authoring/data/ebb`, `compileEbbAuthoring` and `validateEbbData` Gradle tasks. |
| NPC/routine MVP baseline | Complete for MVP skeleton | Existing `ebb:npc`, GeckoLib controller/renderer integration, routine registry/controller, sequential waypoint handling, line-of-sight-aware look-at-player policy, conversation-focus routine pause, summon/inspect commands; GameTest covers spawn/routine state and binding resolution. |
| Java interface drafts | Complete | Implemented as actual contracts in `com.crpg.ebb.api`: target, dialogue repository/runtime, roll service, validation/reload reports. |
| Dialogue UI as narrative rhythm/log surface | Complete | `DialogueScreen` now keeps a scrollable session history log, separate roll/status area, paged choices, and text scroll. |
| Automated test strategy | Complete | JUnit data/runtime tests, smoke checks, static wiring audit, and Fabric GameTest server run are all tracked and passing. |
| GitHub Actions baseline | Complete | `.github/workflows/build.yml` installs JDK25/PyYAML, compiles authoring data, runs `test validateEbbData`, runs `runGametestServer --args nogui`, then builds the jar. |
| Compatibility/migration guidance | Complete | Documented in `docs/deep_research_report_implementation_2026-05-31.md`, `docs/json_authoring_guide.md`, and this audit: current 26.1.2 adaptation, pure-Java domain contracts, optional dependencies deferred. |

## Verification evidence

Commands run successfully in `/mnt/e/MC/PCL/CRPG_MOD`:

```bash
scripts/run_smoke_checks.sh
scripts/gradle-local.sh --no-daemon validateEbbData
scripts/gradle-local.sh --no-daemon runGametestServer --args nogui
python3 scripts/third_review_static_audit.py
python3 scripts/deep_research_static_audit.py
```

Observed results:

- `scripts/run_smoke_checks.sh`: Gradle build successful; `DeepResearchSmoke`, `AttributePointsSmoke`, `ReviewSmoke`, `SecondReviewSmoke`, `ThirdReviewStaticAudit`, and `DeepResearchStaticAudit` all passed.
- `scripts/gradle-local.sh --no-daemon validateEbbData`: `BUILD SUCCESSFUL in 54s`.
- JUnit XML: `build/test-results/test/TEST-com.crpg.ebb.DeepResearchDataTest.xml` reports `tests=5`, `failures=0`, `errors=0`, `skipped=0`.
- Fabric GameTest XML: `build/test-results/gametest/TEST-ebb-gametest.xml` contains:
  - `ebb:ebb_game_tests_bundled_data_registries_are_valid`
  - `ebb:ebb_game_tests_ebb_npc_spawns_with_routine_state`
  - `ebb:ebb_game_tests_tagged_ebb_npc_resolves_configured_binding`
  - plus Minecraft's `minecraft:always_pass`
  - server log result: `All 4 required tests passed :)`.
- Jar inspection confirms packaged `com/crpg/ebb/api/*`, `DialogueNodeType`, `RollMode`, `DialogueScreen` history class, `NpcRoutineController`, `NarrativeSavedData`, `EbbGameTests`, `fabric.mod.json`, and bundled runtime data resources.

Artifact hashes:

```text
98130df556aa021dc1928736943e839d7dc049e6a83d1deb918b0d697b6e2cf3  build/libs/ebb-0.1.0-dev.jar
9d4c2108c6821f86174429161dbeb54e006045add300d3bd272474b540bc22bb  build/libs/ebb-0.1.0-dev-sources.jar
```

## Known non-blocking scope boundaries

- The report's 1.21.1/JDK21 mainline was intentionally not adopted because it conflicts with this session's active 26.1.2/JDK25 project goal and existing playable test profile.
- Optional configuration/UI/AI libraries from the report remain deferred; the MVP keeps dependencies minimal and avoids multi-loader abstraction until after the core loop is stable.
- Full human Windows GUI retesting remains a separate manual activity when requested; it is not required to prove the headless source/tooling implementation in this audit.


## Continuation audit addendum — 2026-06-01

After treating completion as unproven again, a stricter reading of the report found three weak spots and they were closed:

- Dialogue UI body now functions as a scrollable conversation log rather than only showing the current node.
- NPC routines now pause movement and use a conversation-focus look target while an Ebb NPC is in an active dialogue session, then resume automatically when the session closes.
- Routine look-at-player policy now supports and documents `requires_line_of_sight`; the authoring compiler preserves `requiresLineOfSight` from YAML into runtime JSON.
- `NarrativeSavedData` now carries a persisted schema `version` field to support future migrations.
