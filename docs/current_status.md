# Current Project Status — 2026-06-02

This is the authoritative status snapshot for the active `minecraft_disco_crpg_mod_goal_architecture_plan.md` / `GOAL.md` track. Historical phase docs are preserved, but older notes that say GUI retest was pending are superseded by the final GUI evidence listed here.

## Repository and stack

- Repo root: `/mnt/e/MC/PCL/CRPG_MOD`
- GitHub repo: `https://github.com/YJLi-new/MC-CRPG-MOD`
- Current branch at last status update: `main`
- Minecraft: `26.1.2`
- Fabric Loader: `0.19.2`
- Fabric API: `0.150.0+26.1.2`
- Fabric Loom: `1.17.0-alpha.13`
- Java: `25`
- GeckoLib: `5.5.1`
- Mod metadata: `ebb`, `com.crpg.ebb`, `Esoteric Ebb CRPG`, version `0.1.0-dev`

## Latest built artifacts

After the latest P22 HUD/debug-overlay readability build, the built and test-profile jars matched:

```text
37188b09af534900f2024bd32cc6795e059c5cf319f7edd032dfd1d54a7b2c8d  build/libs/ebb-0.1.0-dev.jar
37188b09af534900f2024bd32cc6795e059c5cf319f7edd032dfd1d54a7b2c8d  /mnt/e/MC/PCL/.minecraft/versions/26.1.2-Fabric-Ebb-Test/mods/ebb-0.1.0-dev.jar
42473d388a1b577652ee656da6abac5e9f61f2612c4c314953911aa3c034d351  build/libs/ebb-0.1.0-dev-sources.jar
```

If Java sources or resources change after this status file, rebuild and update these hashes.

## P20/P21 verification refreshed on 2026-06-02

After adding `GOAL.md`, repository onboarding docs, architecture/current-status docs, historical GUI status reconciliation, and P20/P21 static-audit guardrails, the following checks passed from `/mnt/e/MC/PCL/CRPG_MOD`:

```text
python3 scripts/goal_static_audit.py                         -> passed P20/P21 + P2-P8 guardrails
python3 scripts/deep_research_static_audit.py                -> passed
python3 scripts/third_review_static_audit.py                 -> passed
scripts/compile_authoring_sources.py --clean                 -> compiled dialogues=1, interactables=1, npc=2, errors=0
scripts/gradle-local.sh --no-daemon test                     -> BUILD SUCCESSFUL
scripts/gradle-local.sh --no-daemon validateEbbData          -> BUILD SUCCESSFUL
scripts/gradle-local.sh --no-daemon runGametestServer --args nogui -> BUILD SUCCESSFUL, 6 required GameTests passed
scripts/run_smoke_checks.sh                                  -> passed, including GuiRetestIssueAudit
scripts/run_gui_automation_smoke.sh                          -> passed
python3 scripts/gui_e2e_run.py --scenario runtime_check       -> report build/gui-e2e/runtime-check-report.json
python3 scripts/gui_e2e_run.py --scenario gui_retest --gui --gui-wait 1.4 -> 127 steps, 0 failures
git diff --check                                             -> passed
```

Re-run this suite after Java/resource changes, and update artifact hashes here if they change.


## P22 interaction/highlight polish snapshot

Implemented after the P20/P21 verification snapshot:

- Data-driven `HighlightStyle` with close/far colors, opacity, render mode, and visual priority for block groups and entity bindings.
- Highlight style sync through `BlockGroupSyncPayload`, `EntityBindingSyncPayload`, and `EntityTargetSyncPayload`.
- Client block-group rendering now supports merged adjacent block outlines, per-block outlines, and bounds mode.
- F3/debug overlay now shows concise Ebb target-prediction reason/style lines (`no_target`, `unbound_entity`, `outside_binding_highlight_range`, `style=merged`, etc.) without showing normal denial spam during play.
- Demo data exercises `highlight.render_mode=merged` on `locked_door` and `highlight.render_mode=outline` on the innkeeper binding/name binding.
- The separate PCL test profile was refreshed with jar `37188b09af534900f2024bd32cc6795e059c5cf319f7edd032dfd1d54a7b2c8d`. If a Minecraft process was already running, close/relaunch `26.1.2-Fabric-Ebb-Test` before judging the new P22 visuals in-game.

P22 code/build validation so far:

```text
scripts/gradle-local.sh --no-daemon build                    -> BUILD SUCCESSFUL
scripts/gradle-local.sh --no-daemon validateEbbData          -> BUILD SUCCESSFUL
scripts/run_smoke_checks.sh                                  -> passed, including P22 GoalStaticAudit and GuiRetestIssueAudit
scripts/gradle-local.sh --no-daemon runGametestServer --args nogui -> BUILD SUCCESSFUL, 6 required GameTests passed
git diff --check                                             -> passed
scripts/configure_pcl_test_client.sh                         -> refreshed separate Fabric test profile
```

The P22 jar has been installed to the test profile. Latest P22 runtime relaunch check: `scripts/check_pcl_runtime_loaded.py` reports the profile jar hash `37188b09af534900f2024bd32cc6795e059c5cf319f7edd032dfd1d54a7b2c8d`; the latest runtime log does not yet include registry-load evidence for this post-HUD build, so close/relaunch `26.1.2-Fabric-Ebb-Test`, enter `新的世界 (1)`, and rerun GUI visual proof before marking P22 fully complete.

## Final GUI evidence superseding older pending notes

Final GUI visual pass evidence from the separate Fabric/PCL test profile:

- Test profile: `/mnt/e/MC/PCL/.minecraft/versions/26.1.2-Fabric-Ebb-Test`
- World: `新的世界 (1)`
- Runtime check: `scripts/check_pcl_runtime_loaded.py` passed with `dialogues=13`, `block_groups=8`, `entity_bindings=10`, `npc_routines=5`.
- GUI run: `scripts/gui_e2e_run.py --scenario gui_retest --gui --gui-wait 1.4`
- Report: `build/gui-e2e/gui-retest-report.json`, 127 steps, 0 failures.
- Contact sheet: `build/gui-e2e/contact_dialogues_final_verified.png`.
- Visual coverage: `/ebb journal`, `/ebb quest`, `/ebb dialogue vars`, `/ebb vars`, four distinct role NPC dialogues, and all eight block-group dialogues.
- Latest P20/P21 rerun: `build/gui-e2e/gui-retest-report.json`, 127 steps, 0 failures, 2026-06-02T17:29:14+0800 to 2026-06-02T17:31:46+0800.

Older files such as `goal_p2_*` through `goal_p8_*`, `second_review_completion_audit_*`, and `third_review_reconciliation_*` may record the state that existed before automation and relaunch fixes. Treat them as historical audit records; use this status file plus the final GUI report for current truth.

## Current playable slice inventory

- Dialogues: 13 bundled demo/debug dialogues.
- Block groups: 8 interactable investigation points.
- Entity bindings: 10 bindings including role tags and role custom-name fallbacks.
- NPC routines: 5 routines.
- Role NPCs: innkeeper, witness, suspicious tenant, guard/fixer.
- Quest branches: public and quiet major routes with Take Root.
- Feats: Tavern Authority, Paranoid Pattern Reader, Cheap Empathy, Door Theology.
- Chimes: Instinct, Rhetoric, Dread, Empathy.
- Investigation/conflict: locked-room clue scene and hallway confrontation set-piece.

## Next roadmap focus

The project has reached the technical MVP/P8 vertical slice. The active plan now starts from `GOAL.md` P20/P21 and then proceeds through P22–P31 toward Alpha 0.1.
