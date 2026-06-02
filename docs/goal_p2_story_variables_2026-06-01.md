# GOAL.md P2 Story Variables Completion Audit

> Status reconciliation 2026-06-02: this document is historical. Its original GUI-pending note is superseded by `docs/current_status.md` and `docs/status_reconciliation_2026-06-02.md`, which record the final automated GUI visual pass against the refreshed `26.1.2-Fabric-Ebb-Test` jar.


Date: 2026-06-01 Asia/Shanghai

## Scope

Implemented the GOAL.md P2 layer: server-authoritative Story Variables split into **Branch / Major / Minor** layers while preserving the existing Fabric 26.1.2 + Java 25 + GeckoLib 5.5.1 stack and existing JSON directories.

## Code implementation

- Added `com.crpg.ebb.story.StoryVarLayer` with Branch/Major/Minor parsing and aliases.
- Added `com.crpg.ebb.story.StoryVarValue` for stable string persistence plus int/bool/string comparison helpers.
- Extended `PlayerNarrativeState` codec with per-player `story_branch_vars`, `story_major_vars`, and `story_minor_vars`.
- Extended `NarrativeSavedData` codec/debug snapshot with world `world_story_branch_vars`, `world_story_major_vars`, and `world_story_minor_vars`, plus player/world get/set/add helpers.
- Extended dialogue effects:
  - `set_story_var`
  - `clear_story_var` / `remove_story_var`
  - `add_story_int` / `increment_story_var`
- Extended dialogue conditions with `story_var` supporting scalar equality and integer `min`/`at_least` checks.
- Extended `/ebb dialogue vars` and `/ebb dev` snapshot output to show story-variable state.
- Extended authoring compiler shortcuts for `setStoryVar`, `clearStoryVar`, and `addStoryInt`.

## Bundled demo content

`data/ebb/dialogues/demo/innkeeper_intro.json` now demonstrates all three layers:

- Minor: `met_innkeeper=true` on entry; `innkeeper_annoyance` increments on failed pressure checks.
- Major: `innkeeper_trust` increments on successful pressure checks; a trust-gated follow-up branch appears when it reaches at least 1.
- Branch: `tavern_route=public` records a public-route commitment and unlocks a public-route ending placeholder/echo.

## Tests and audits run

- `scripts/gradle-local.sh --no-daemon build` → BUILD SUCCESSFUL.
- `scripts/run_smoke_checks.sh` → BUILD SUCCESSFUL; DeepResearchSmoke, AttributePointsSmoke, ReviewSmoke, SecondReviewSmoke, ThirdReviewStaticAudit, DeepResearchStaticAudit, and GoalStaticAudit passed.
- `scripts/goal_static_audit.py` → passed.
- `scripts/deep_research_static_audit.py` → passed.
- `scripts/third_review_static_audit.py` → passed.
- `scripts/gradle-local.sh --no-daemon validateEbbData` → BUILD SUCCESSFUL.
- `scripts/gradle-local.sh --no-daemon runGametestServer --args nogui` → 4 required GameTests passed.
- `git diff --check` → no whitespace errors.

## Manual GUI status

P2 code/data/docs/tests are complete. Full Windows GUI retest remains a human-operated step: use `26.1.2-Fabric-Ebb-Test`, interact with the tagged/summoned innkeeper, then inspect `/ebb dialogue vars` and `/ebb dev` for Branch/Major/Minor state.

## Next GOAL phase

Proceed to P3: Quest Branch / Take Root / Feat definitions and runtime state, building on the now-layered Story Variables.
