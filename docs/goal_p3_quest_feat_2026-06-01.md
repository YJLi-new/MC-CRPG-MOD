# GOAL.md P3 Quest Branch / Take Root / Feat Completion Audit

> Status reconciliation 2026-06-02: this document is historical. Its original GUI-pending note is superseded by `docs/current_status.md` and `docs/status_reconciliation_2026-06-02.md`, which record the final automated GUI visual pass against the refreshed `26.1.2-Fabric-Ebb-Test` jar.


Date: 2026-06-01 Asia/Shanghai

## Scope

Implemented the GOAL.md P3 MVP: quest branch definitions, take-root resolution, feat definitions/loadout state, dialogue effects/conditions, feat check modifiers, a basic Quest Tree UI, bundled demo content, docs, smoke/JUnit/static-audit coverage.

## Code implementation

- Added `quest/` package:
  - `QuestBranchKind`
  - `QuestBranchDefinition`
  - `QuestBranchRegistry`
  - `TakeRootService`
  - `QuestTreeService`
- Added `feat/` package:
  - `FeatDefinition`
  - `FeatRegistry`
- Added raw data registries:
  - `data/*/quest_branches`
  - `data/*/feats`
- Extended `PlayerNarrativeState` / `NarrativeSavedData` with:
  - `quest_states`
  - `unlocked_feats`
  - `active_feats`
  - 4 active feat slots
- Extended dialogue effects:
  - `start_quest_branch`
  - `complete_quest_branch`
  - `unlock_feat`
  - `activate_feat`
- Extended dialogue conditions:
  - `quest_state`
  - `has_feat`
- Added feat check modifiers to server-side d20 resolution through `FeatRegistry.totalCheckModifier`.
- Added basic Quest Tree UX:
  - `QuestTreePayload`
  - `QuestTreeScreen`
  - `/ebb quest` and `/ebb quest tree`
- Extended `/ebb dev` to show quest/feat registry summaries and player quest/feat state.

## Bundled demo content

Quest branches:

- `ebb:demo/tavern_public` — major public-reveal branch; take-root grants `Tavern Authority` and `Cheap Empathy`.
- `ebb:demo/tavern_quiet` — major quiet-resolution branch; take-root grants `Paranoid Pattern Reader` and `Door Theology`.

Feats:

- `ebb:demo/tavern_authority` — charisma modifier.
- `ebb:demo/paranoid_pattern_reader` — perception modifier.
- `ebb:demo/cheap_empathy` — wisdom/charisma modifier when active.
- `ebb:demo/door_theology` — intelligence modifier when active.

`ebb:demo/innkeeper_intro` now completes the public or quiet branch through dialogue choices and exposes a feat-gated authority follow-up after `Tavern Authority` is granted.

## Verification

- `scripts/gradle-local.sh --no-daemon build` → `BUILD SUCCESSFUL in 3m 28s`.
- `scripts/run_smoke_checks.sh` → build successful; DeepResearchSmoke, AttributePointsSmoke, ReviewSmoke, SecondReviewSmoke, ThirdReviewStaticAudit, DeepResearchStaticAudit, and GoalStaticAudit passed.
- `scripts/goal_static_audit.py` → passed for P2 + P3.
- `scripts/gradle-local.sh --no-daemon validateEbbData` → `BUILD SUCCESSFUL in 54s`.
- `scripts/gradle-local.sh --no-daemon runGametestServer --args nogui` → 4 required GameTests passed.
- `git diff --check` → no whitespace errors.
- Jar SHA-256: `643740159d9f61a2e7f699d898edbdfdcfdea9d01d6e7b78e46b780064f4932b`.
- Sources jar SHA-256: `70ba1356a89e0fb9e56196a5f8d2a1b3a551fc8baedf804beec2de327b397232`.
- Jar inspection confirmed packaged quest/feat/story classes, `QuestTreeScreen`, `QuestTreePayload`, and bundled quest/feat data JSON.

## Manual GUI status

Code/data/docs/tests are complete. Full Windows GUI retest remains human-operated: launch `26.1.2-Fabric-Ebb-Test`, interact with the innkeeper, choose public/quiet routes, inspect `/ebb quest`, `/ebb dialogue vars`, and `/ebb dev`.

## Next GOAL phase

Proceed to P4: Chime / Inner Voice / Passive Inserts MVP.
