# GOAL.md P5 Journal / UI Rhythm Completion Audit

> Status reconciliation 2026-06-02: this document is historical. Its original GUI-pending note is superseded by `docs/current_status.md` and `docs/status_reconciliation_2026-06-02.md`, which record the final automated GUI visual pass against the refreshed `26.1.2-Fabric-Ebb-Test` jar.


Date: 2026-06-01 Asia/Shanghai

## Scope

Implemented the GOAL.md P5 MVP: player-visible journal/clue state, a Journal screen, Quest Tree access, and typed DialogueScreen status echoes so quest/feat/chime/journal consequences are understandable without reading command output.

## Code implementation

- Added `journal/` package:
  - `JournalEntryCategory`
  - `JournalEntryDefinition`
  - `JournalEntryRegistry`
  - `JournalService`
- Added `JournalPayload` plus client-side `JournalScreen`.
- Added `/ebb journal` as a player-facing command with networked GUI and text fallback.
- Registered `data/*/journal_entries` through `NarrativeDataRegistries` and rebuilt it into `JournalEntryRegistry` on reload.
- Extended `PlayerNarrativeState` / `NarrativeSavedData` with persisted unlocked journal-entry IDs.
- Added dialogue effect aliases for `add_journal_entry`, `reveal_clue`, and `add_clue`.
- Added dialogue condition aliases for `has_journal_entry`, `clue_found`, and `has_clue`.
- Extended `/ebb dev` snapshots and summaries with journal registry/player-state visibility.
- Strengthened `DialogueScreen` status echoes with typed labels/colors for clue/journal, quest/take-root, feat, chime, story-var, and future relation messages.

## Bundled demo content

Four journal entries were added under `data/ebb/journal_entries/demo`:

- `door_scratches` — clue gained by knocking on the locked door.
- `bruised_shoulder` — fail-forward clue gained by failing the force-door check.
- `public_pressure` — lead gained by taking the public innkeeper branch.
- `quiet_compromise` — lead gained by taking the quiet innkeeper branch.

`ebb:demo/locked_door_dialogue` and `ebb:demo/innkeeper_intro` now write these entries through dialogue effects, so the player receives visible non-command feedback during ordinary play.

## Verification

- `scripts/run_smoke_checks.sh` → build successful; DeepResearchSmoke, AttributePointsSmoke, ReviewSmoke, SecondReviewSmoke, ThirdReviewStaticAudit, DeepResearchStaticAudit, and GoalStaticAudit passed.
- `scripts/goal_static_audit.py` → `GoalStaticAudit passed: P2 Story Variables, P3 Quest/Take-Root/Feat, P4 Chime, and P5 Journal/UI rhythm are wired through persistence, dialogue, dev/UI, docs, demo data, smoke, and JUnit.`
- `scripts/gradle-local.sh --no-daemon validateEbbData` → `BUILD SUCCESSFUL in 58s`.
- `scripts/gradle-local.sh --no-daemon runGametestServer --args nogui` → 4 required GameTests passed.
- `git diff --check` → no whitespace errors.
- Jar SHA-256: `6f3e3b345b8d5c457dbc291fa927fbf32d0c7793eb30911d028b696bef2b25fa`.
- Sources jar SHA-256: `cfb59f1f4b1f228d850e9bf6dd77d43996d158e36453421556e779edc5f64bd2`.

## Manual GUI status

Code/data/docs/tests are complete. Full Windows GUI retest remains human-operated: launch `26.1.2-Fabric-Ebb-Test`, interact with the locked door and innkeeper, and verify the dialogue status area plus `/ebb journal` show clue/lead consequences without needing command-line debug output.

## Next GOAL phase

Proceed to P6: NPC relationships, memory, state tags, time/state-dependent dialogue, and routine expansion.
