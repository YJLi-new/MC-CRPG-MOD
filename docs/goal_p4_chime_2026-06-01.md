# GOAL.md P4 Chime / Inner Voice Completion Audit

Date: 2026-06-01 Asia/Shanghai

## Scope

Implemented the GOAL.md P4 MVP: chime registry/resolver, passive insert hooks on dialogue nodes, distinct dialogue UI styling, four bundled chimes, and a passive insight that unlocks a new thought path.

## Code implementation

- Added `chime/` package:
  - `ChimeDefinition`
  - `ChimeRegistry`
  - `ChimeResolver`
- Added raw JSON registry `data/*/chimes` through `NarrativeDataRegistries`.
- Extended `DialogueNode` with `tags` / `chime_tags` parsing.
- Hooked `ChimeResolver.resolve(...)` into `DialogueService` before open/update payloads are sent, so the server remains authoritative.
- Triggered chimes write player flags of the form `chime:<id>` and one-shot seen flags for the current dialogue node.
- Added cyan `CHIME_STATUS_COLOR` rendering in `DialogueScreen` for passive insert status lines containing `[Chime:]`.
- Extended `/ebb dev` data summaries/validation output with chime registry status.

## Bundled demo content

Four chimes were added under `data/ebb/chimes/demo`:

- `Instinct` — perception build voice.
- `Rhetoric` — charisma build voice.
- `Dread` — luck build voice.
- `Empathy` — wisdom build voice.

`ebb:demo/innkeeper_intro` now tags its start node with `innkeeper.read`. Different builds can trigger different chime lines on the same innkeeper scene. The `Rhetoric` chime writes `chime:ebb:demo/rhetoric`, which unlocks the `rhetoric_insight` thought path.

## Verification

- `scripts/gradle-local.sh --no-daemon build` → `BUILD SUCCESSFUL in 3m 13s`.
- `scripts/run_smoke_checks.sh` → build successful; DeepResearchSmoke, AttributePointsSmoke, ReviewSmoke, SecondReviewSmoke, ThirdReviewStaticAudit, DeepResearchStaticAudit, and GoalStaticAudit passed.
- `scripts/goal_static_audit.py` → passed for P2 + P3 + P4.
- `scripts/gradle-local.sh --no-daemon validateEbbData` → `BUILD SUCCESSFUL in 57s`.
- `scripts/gradle-local.sh --no-daemon runGametestServer --args nogui` → 4 required GameTests passed.
- `git diff --check` → no whitespace errors.
- Jar SHA-256: `cd2cd304d9e3f12da49ab0b79ac90c7ce656e013898d73cfb459b96f919f36d7`.
- Sources jar SHA-256: `2586f2849bdfa16bfd05dc0ed09c4a1b7e6cb598c1375843ff5e183261a5365b`.
- Jar inspection confirmed packaged chime classes and bundled chime data JSON.

## Manual GUI status

Code/data/docs/tests are complete. Full Windows GUI retest remains human-operated: launch `26.1.2-Fabric-Ebb-Test`, spend a relevant attribute point such as `/ebb attributes spend charisma 1`, interact with the innkeeper, and verify a cyan `[Chime: Rhetoric]` passive insert plus the unlocked `rhetoric_insight` thought choice.

## Next GOAL phase

Proceed to P5: UI rhythm and Journal/Quest Tree maturation.
