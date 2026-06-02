# GOAL.md P8 Playable Tavern Vertical Slice Completion Audit

> Status reconciliation 2026-06-02: this document is historical. Its original GUI-pending note is superseded by `docs/current_status.md` and `docs/status_reconciliation_2026-06-02.md`, which record the final automated GUI visual pass against the refreshed `26.1.2-Fabric-Ebb-Test` jar.


Date: 2026-06-01 Asia/Shanghai

## Scope

Implemented the content minimums for the GOAL.md P8 vertical slice: one compact tavern/side-door area, four role NPCs, eight interactable investigation points, two major branches, four feats, four chimes, one set-piece conflict, and visible ending placeholders.

## Content inventory

### Area

The current bundled slice represents a dense tavern / upper-hall / side-door area through data-driven interaction points:

- lobby/counter ledger
- upper hallway locked door
- washroom mirror
- windowsill ash
- tenant luggage
- notice board
- cellar hatch
- back door / ending placeholder

### NPCs

Four role NPCs have bindings, routines, and dialogue:

1. Innkeeper — `ebb:demo/innkeeper_intro`
2. Witness — `ebb:demo/witness_intro`
3. Suspicious tenant — `ebb:demo/tenant_intro`
4. Guard/fixer — `ebb:demo/guard_intro`

### Interactable investigation points

Eight block-group targets are bundled under `data/ebb/interactions/block_groups/demo`:

1. `locked_door`
2. `counter_ledger`
3. `washroom_mirror`
4. `windowsill_ash`
5. `tenant_luggage`
6. `notice_board`
7. `cellar_hatch`
8. `back_door`

### Branches / growth / voice / conflict

- Major branches: `ebb:demo/tavern_public`, `ebb:demo/tavern_quiet`.
- Feats: `Tavern Authority`, `Paranoid Pattern Reader`, `Cheap Empathy`, `Door Theology`.
- Chimes: `Instinct`, `Rhetoric`, `Dread`, `Empathy`.
- Set-piece conflict: `ebb:demo/hallway_confrontation`.
- Ending placeholders: public, quiet, and messy fail-forward endings in `ebb:demo/back_door_dialogue`.

## Replayability evidence

- Public route can be entered through the innkeeper or notice board and resolves toward a public ending placeholder.
- Quiet route can be entered through the innkeeper and resolves toward a quiet ending placeholder.
- Failed confrontation/cellar/luggage paths write messy/fail-forward state and expose a different back-door ending placeholder.
- Chimes, feats, clues, relationship state, and scene phase affect visible choices/check outcomes.

## Verification

Final automated verification for this P8 pass completed on 2026-06-01 Asia/Shanghai:

- `scripts/gradle-local.sh --no-daemon build` → `BUILD SUCCESSFUL in 2m 48s`.
- `scripts/run_smoke_checks.sh` → build successful; `DeepResearchSmoke`, `AttributePointsSmoke`, `ReviewSmoke`, `SecondReviewSmoke`, `ThirdReviewStaticAudit`, `DeepResearchStaticAudit`, and `GoalStaticAudit` all passed.
- `python3 scripts/goal_static_audit.py` → passed for P2 Story Variables through P8 Playable Vertical Slice content.
- `scripts/gradle-local.sh --no-daemon validateEbbData` → `BUILD SUCCESSFUL in 59s`.
- `scripts/gradle-local.sh --no-daemon runGametestServer --args nogui` → 4 required GameTests passed; `BUILD SUCCESSFUL in 3m 30s`.
- `git diff --check` → no whitespace errors.
- Jar hashes:
  - `build/libs/ebb-0.1.0-dev.jar` SHA-256 `47d3e8ac42c28c9e0d6dc437e90656c03404694677677d4e0212220c28c29d59`
  - `build/libs/ebb-0.1.0-dev-sources.jar` SHA-256 `a4c89f3076519c8570f092c0b6f5e0006c53cac52cb64b6acde1f95310977bd5`
- Jar inspection confirmed packaged `com/crpg/ebb/conflict/*`, `com/crpg/ebb/investigation/*`, `com/crpg/ebb/relationship/*`, `data/ebb/clues/*`, `data/ebb/conflicts/*`, `data/ebb/investigation_scenes/*`, and all eight P8 block-group JSON resources.
- Post-bookkeeping recheck: `git diff --check`, `python3 scripts/goal_static_audit.py`, `python3 scripts/deep_research_static_audit.py`, `python3 scripts/third_review_static_audit.py`, and `scripts/gradle-local.sh --no-daemon validateEbbData` all passed; latest `validateEbbData` finished `BUILD SUCCESSFUL in 57s`.

## Manual GUI status

Code/data/docs/tests are complete. Full Windows GUI retest remains human-operated: launch `26.1.2-Fabric-Ebb-Test`, spawn role NPCs as needed, visit all eight interactable points, play at least one public route, one quiet route, and one fail-forward route, then verify the back-door ending placeholder changes.
