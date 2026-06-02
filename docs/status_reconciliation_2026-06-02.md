# Historical Status Reconciliation — 2026-06-02

Several audit documents from 2026-05-30 through early 2026-06-01 correctly state that Windows/PCL GUI retesting was pending or externally blocked. Those statements were true at the time and should not be deleted, because they explain the sequence of stale-jar, sync, and role-binding fixes.

Current authoritative status is now different:

1. The separate `26.1.2-Fabric-Ebb-Test` profile was fully closed and relaunched from Plain Craft Launcher.
2. The refreshed jar hash `23540536daaeda7e054813ee10fa4ce653fca1085876fce058f8a2819e3e3ec3` loaded in memory.
3. `scripts/check_pcl_runtime_loaded.py` passed with `dialogues=13`, `block_groups=8`, `entity_bindings=10`, `npc_routines=5`.
4. `scripts/gui_e2e_run.py --scenario gui_retest --gui --gui-wait 1.4` produced `build/gui-e2e/gui-retest-report.json` with 127 steps and 0 failures.
5. `build/gui-e2e/contact_dialogues_final_verified.png` visually shows command checks, four distinct NPC dialogues, and all eight block-group dialogues.

Therefore:

- Historical docs remain valid as a timeline of what was blocked then.
- `docs/current_status.md`, `.kiro/plan/task_plan.md` current phase, and the final GUI report are the current status authorities.
- Future edits should add new status snapshots rather than rewriting old audit conclusions in place.
