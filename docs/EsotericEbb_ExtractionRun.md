# Esoteric Ebb Extraction Run

This run implements steps 1-3 only and pauses before full export.

- Output directory: `/mnt/e/SteamLibrary/steamapps/common/Esoteric Ebb/mc_mod/extraction_20260529_180228`
- Scanner: `scripts/scan_and_sample.py`
- Parser mode: fallback binary scanner; UnityPy unavailable in this run.
- Files seen: `154`
- Files scanned: `109`
- Ink JSON parsed: `572` (source groups: `{'backup': 286, 'current': 286}`)
- CSV dialogue-like rows indexed: `149330` (source groups: `{'backup': 74665, 'current': 74665}`)
- Quest-related candidates indexed: `68343`
- Character candidates indexed: `182`

## Important files

- `reports/sample_review.md` — pause-point review and recommendation.
- `reports/scan_summary.md` — scan summary.
- `indexes/resource_index.jsonl` — file-level resource index.
- `indexes/ink_stories.jsonl` — parsed Ink JSON object index.
- `indexes/text_assets.jsonl` — CSV/dialogue-like row index with previews.
- `indexes/quest_candidates.jsonl` — Quest/Feat/Glossary/etc. keyword candidates.
- `indexes/characters_candidates.jsonl` — speaker/name candidates.
- `samples/` — small sample exports only, not full corpus.

## Status

Paused after sample extraction as requested. Full export has not been run.

## Dependency update

UnityPy was installed successfully after the initial fallback scan. See:

- `reports/dependency_install.md`
- `reports/unitypy_probe.md`
- `reports/next_steps_after_dependency_install.md`
