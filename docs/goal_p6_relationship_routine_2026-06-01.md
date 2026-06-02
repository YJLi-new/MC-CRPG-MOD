# GOAL.md P6 Relationship / NPC Memory / Routine Completion Audit

> Status reconciliation 2026-06-02: this document is historical. Its original GUI-pending note is superseded by `docs/current_status.md` and `docs/status_reconciliation_2026-06-02.md`, which record the final automated GUI visual pass against the refreshed `26.1.2-Fabric-Ebb-Test` jar.


Date: 2026-06-01 Asia/Shanghai

## Scope

Implemented the GOAL.md P6 MVP: persisted NPC relationship scores, NPC state/memory tags, relation/state/time dialogue conditions, dialogue-driven routine switching, expanded routine actions, role-specific Ebb NPC bindings, and demo content showing at least two NPCs with memory/time-dependent responses.

## Code implementation

- Added `relationship/` package:
  - `RelationshipDefinition`
  - `RelationshipRegistry`
- Added `data/*/relationships` reload registry through `NarrativeDataRegistries` and exposed it in `/ebb data` / `/ebb dev`.
- Extended `PlayerNarrativeState` and `NarrativeSavedData` with:
  - per-player relationship scores;
  - per-player NPC state tags;
  - world NPC state tags;
  - debug snapshot and developer tree lines for relationship/NPC memory state.
- Extended dialogue effects:
  - `set_relation`
  - `add_relation`
  - `set_npc_state`
  - `clear_npc_state`
  - `set_npc_routine`
- Extended dialogue conditions:
  - `relation_at_least`
  - `npc_state`
  - `time_window`
- Extended `DialogueService` to evaluate time-window conditions using the server's authoritative day time.
- Extended `EbbNpcEntity` with persisted `narrative_key`, `pose`, and `animation` strings.
- Extended `NpcRoutineController` / `NpcRoutineDefinition` with P6 action vocabulary: `wait`, `walk_path`, `look_at`, `play_animation`, `set_pose`, and `teleport_fallback` in addition to existing stand/walk behavior.
- Extended `/ebb routine inspect` and `/ebb summon_npc` so spawned Ebb NPCs carry role-specific narrative keys and tags.
- Extended `DialogueScreen` status labeling/coloring for relationship, NPC-state, and routine status echoes.

## Bundled demo content

- Added relationships:
  - `ebb:demo/innkeeper`
  - `ebb:demo/witness`
- Added role-specific Ebb NPC bindings:
  - innkeeper
  - witness
  - tenant
  - guard
- Added routines:
  - `ebb:demo/innkeeper_backroom`
  - `ebb:demo/witness_day`
  - `ebb:demo/tenant_day`
  - `ebb:demo/guard_day`
- Added dialogues:
  - `ebb:demo/witness_intro`
  - `ebb:demo/tenant_intro`
  - `ebb:demo/guard_intro`
- Extended `ebb:demo/innkeeper_intro` so success/failure/major-branch choices write relationship and memory state, and major route choices can switch the interacted Ebb NPC to `ebb:demo/innkeeper_backroom`.

## P6 acceptance mapping

- At least 2 NPCs have long-term memory differences:
  - Innkeeper remembers `guarded`, `helpful`, `publicly_exposed`, and `private_bargain` tags.
  - Witness remembers `asked_about_door`, `publicly_committed`, and `shut_down` tags.
- At least 1 NPC changes routine due to story:
  - Innkeeper public/quiet branch choices apply `set_npc_routine` to `ebb:demo/innkeeper_backroom` for Ebb NPC targets.
- Time/state-dependent dialogue:
  - Innkeeper and witness both expose `time_window` night lines.
  - Witness exposes NPC-memory and relation-gated follow-ups.

## Verification

- `scripts/gradle-local.sh --no-daemon build` → `BUILD SUCCESSFUL in 2m 39s` after P6 implementation.
- `scripts/run_smoke_checks.sh` → build successful; DeepResearchSmoke, AttributePointsSmoke, ReviewSmoke, SecondReviewSmoke, ThirdReviewStaticAudit, DeepResearchStaticAudit, and P6-extended GoalStaticAudit passed.
- `scripts/goal_static_audit.py` → passed for P2, P3, P4, P5, and P6.
- `scripts/gradle-local.sh --no-daemon validateEbbData` → `BUILD SUCCESSFUL in 54s`.
- `scripts/gradle-local.sh --no-daemon runGametestServer --args nogui` → 4 required GameTests passed.
- `git diff --check` → no whitespace errors.
- Jar SHA-256: `b64e744f4a6c115757ecefee2098fb22061f9a44c1438ae80877e96d87f96c92`.
- Sources jar SHA-256: `b08b1fdd6abee405fe901d7e0f98619d813143f9bf369ca4f30b86c9068e7698`.

## Manual GUI status

Code/data/docs/tests are complete. Full Windows GUI retest remains human-operated: launch `26.1.2-Fabric-Ebb-Test`, spawn role NPCs with `/ebb summon_npc`, interact through relation/memory-gated paths, and inspect the changed routine with `/ebb routine inspect`.

## Next GOAL phase

Proceed to P7: investigation scenes, clue-to-DC hooks, and one set-piece conflict prototype.
