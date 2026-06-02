# GOAL.md P7 Investigation / Set-piece Conflict Completion Audit

> Status reconciliation 2026-06-02: this document is historical. Its original GUI-pending note is superseded by `docs/current_status.md` and `docs/status_reconciliation_2026-06-02.md`, which record the final automated GUI visual pass against the refreshed `26.1.2-Fabric-Ebb-Test` jar.


Date: 2026-06-01 Asia/Shanghai

## Scope

Implemented the GOAL.md P7 MVP: investigation scene/clue data, discovered clue persistence, clue-to-check modifier hooks, scene phase state, conflict state/stress/resolve state, and one hallway confrontation set-piece that fails forward instead of invoking a traditional combat system.

## Code implementation

- Added `investigation/` package:
  - `ClueDefinition`
  - `InvestigationSceneDefinition`
  - `InvestigationRegistry`
  - `InvestigationService`
- Added `conflict/` package:
  - `ConflictDefinition`
  - `ConflictRegistry`
  - `ConflictService`
- Added reload registries:
  - `data/*/clues`
  - `data/*/investigation_scenes`
  - `data/*/conflicts`
- Extended `PlayerNarrativeState` / `NarrativeSavedData` with discovered clues, scene/conflict narrative states, and conflict stress/resolve scores.
- Extended dialogue effects with `reveal_clue`, `start_conflict`, `add_conflict_stress`, `add_conflict_resolve`, `set_conflict_state`, and `set_scene_phase`.
- Extended dialogue conditions with `clue_found`, `conflict_state`, and `scene_phase`.
- Hooked clue modifiers into server-authoritative d20 checks through `InvestigationRegistry.totalCheckModifier(...)` inside `DialogueService`.
- Extended `/ebb dev` and saved-data debug snapshots to expose investigation/conflict state.
- Extended DialogueScreen status echoes for clue, scene, and conflict consequences.

## Bundled demo content

- Investigation scene:
  - `ebb:demo/locked_room`
- Five clues:
  - `ebb:demo/door_scratches`
  - `ebb:demo/bruised_shoulder`
  - `ebb:demo/witness_knock_pattern`
  - `ebb:demo/tenant_false_window`
  - `ebb:demo/guard_denial`
- Conflict:
  - `ebb:demo/hallway_confrontation`
- Demo dialogue wiring:
  - locked door reveals door/latch clues;
  - witness reveals knock-pattern clue;
  - tenant thought path reveals false-window clue;
  - guard dialogue reveals denial clue and starts the hallway confrontation set-piece.

## P7 acceptance mapping

- At least 1 investigation scene: `ebb:demo/locked_room`.
- At least 5 clues: all five listed above.
- At least 2 clues affect later checks: clue `check_modifiers` feed into d20 rolls for charisma/perception/intelligence/strength/luck/wisdom checks.
- At least 1 set-piece conflict: `ebb:demo/hallway_confrontation`.
- Pre-investigation changes conflict process: guard confrontation choices are clue-gated and clue modifiers affect its checks.
- Failure still advances: conflict failure writes `failed_forward`, scene phase `messy`, NPC/relationship consequences, and visible text.

## Verification

- `scripts/gradle-local.sh --no-daemon build` → `BUILD SUCCESSFUL in 2m 56s` after P7 implementation.
- `scripts/run_smoke_checks.sh` → build successful; DeepResearchSmoke, AttributePointsSmoke, ReviewSmoke, SecondReviewSmoke, ThirdReviewStaticAudit, DeepResearchStaticAudit, and P7-extended GoalStaticAudit passed.
- `scripts/goal_static_audit.py` → passed for P2-P7.
- `scripts/gradle-local.sh --no-daemon validateEbbData` → `BUILD SUCCESSFUL in 58s`.
- `scripts/gradle-local.sh --no-daemon runGametestServer --args nogui` → 4 required GameTests passed.
- `git diff --check` → no whitespace errors.
- Jar SHA-256: `d511e37ae5451cf5797ebd22ccf8aca12c11976091b15625275ce3c161d2d14c`.
- Sources jar SHA-256: `f9de175da9917b8abf790da05e27e88e59f0b96a7d66c9469e9e928e44ca306d`.

## Manual GUI status

Code/data/docs/tests are complete. Full Windows GUI retest remains human-operated: spawn/approach the guard and related NPCs, reveal clues, start the hallway confrontation, and verify fail-forward conflict feedback in the dialogue UI.

## Next GOAL phase

Proceed to P8: vertical slice content completion and final evidence.
