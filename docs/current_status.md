# Current Project Status — 2026-06-03

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

After the 2026-06-03 Phase 33 codebase-review remediation build, the current build artifact hashes are:

```text
7da6e7148c5cabba5b357ee183fddbfc293a227dd4b7c8520491ad85d15df576  build/libs/ebb-0.1.0-dev.jar
7848b75ada4f5ff3a53922328a86ad8345bbb51e2bdadc0a6da1117a9bce761b  build/libs/ebb-0.1.0-dev-sources.jar
```

The separate `26.1.2-Fabric-Ebb-Test` profile was last GUI-refreshed for the earlier P32 jar until the next explicit client-test/profile-refresh step. If Java sources or resources change after this status file, rebuild, refresh the separate test profile, and update these hashes.

## 2026-06-03 Phase 33 codebase review remediation snapshot

Implemented from `ebb_codebase_review_report_2026-06-03.md`:

- Closed the nested `/ebb dialogue vars <player>` privacy hole by routing command permissions through `EbbCommandPermissionGuards` and applying the dialogue gamemaster guard to the nested player argument.
- Added active feat conditions (`has_active_feat` / `feat_active`) that check `NarrativeSavedData.isFeatActive(...)` instead of collapsing to unlocked-feat semantics.
- Added `disadvantage` checks, advantage/disadvantage cancel-out semantics, raw-roll capture, and roll modifier breakdowns for attribute/static/feat/clue contributions.
- Clarified checked-choice semantics with `pre_effects`, legacy pre-roll `effects`, `success_effects` / `failure_effects`, and explicit `end_on_success` linting.
- Implemented retryable check locks: failed retryable/white checks set `check_locked:<dialogue_id>:<choice_id>` and require an `unlock_retry` / `unlock` flag before retry.
- Centralized collider-only prediction/authority/dev-inspect raycasts in `InteractionRaycastPolicy`; server block-group LOS now tests the nearest authored block center as well as the interaction point.
- Made duplicate block-group membership invalid instead of silently overriding later targets.
- Hardened NPC routine parsing for nonempty steps, non-overlapping time windows, positive teleport fallback distances, and cook/courier role inference while preserving visible GeckoLib animation metadata.
- Reconfirmed placeholder item semantics: `give_item` / `take_item` remain narrative item flags/status echoes, not vanilla inventory manipulation.

Phase 33 validation evidence:

```text
scripts/gradle-local.sh --no-daemon test --tests com.crpg.ebb.DeepResearchDataTest -> BUILD SUCCESSFUL
scripts/gradle-local.sh --no-daemon validateEbbData                         -> BUILD SUCCESSFUL
python3 -m py_compile scripts/goal_static_audit.py                           -> passed
python3 scripts/goal_static_audit.py                                         -> passed including P33 guardrails
scripts/run_smoke_checks.sh                                                  -> passed including static/deep/third/gui audits and authoring validation
scripts/gradle-local.sh --no-daemon runGametestServer --args nogui           -> BUILD SUCCESSFUL, 6 required GameTests passed
scripts/gradle-local.sh --no-daemon build                                    -> BUILD SUCCESSFUL
git diff --check                                                             -> passed
```


## 2026-06-03 K-key Ebb menu / live dialogue background snapshot

Implemented after the dialogue wait-state hotfix:

- Added a K-key Ebb menu (`key.ebb.menu`) for the mod-specific menu. It opens from normal play, closes with K/Done, and exposes player-safe shortcuts for Journal, Quest Tree, Attributes, Dialogue Vars, plus dialogue font-scale and text-speed controls.
- `DialogueScreen` now leaves the live player view visible behind the panel: the previous full-screen `extractTransparentBackground(graphics)` darkening call was removed, while the dialogue panel itself remains translucent. In short, the dialogue screen no longer darkens the non-panel gameplay view.
- GUI automation now captures `build/gui-e2e/k_menu_open.png` and records `top_band_luminance` for dialogue screenshots so a black full-screen overlay regression is detected.
- The separate `26.1.2-Fabric-Ebb-Test` profile was refreshed with jar `25e896a4bc2847d3c941287c356f44eb650bc33c423c317f63567a0f164d2757` after closing the running Minecraft JVM.
- Windows GUI retest passed with `282` steps and `0` failures. Evidence includes `build/gui-e2e/k_menu_open.png`, `build/gui-e2e/role_innkeeper_dialogue.png`, and `build/gui-e2e/gui-retest-report.json`. The K menu screenshot recorded cyan border pixels `4096`, gray button ratio `0.1985`, and top-band luminance `90.52`; dialogue screenshots recorded live-background top-band luminance around `105–107`, proving the non-panel area is not a black overlay.


P32 validation evidence:

```text
python3 scripts/goal_static_audit.py                         -> passed including P32 guardrails
python3 scripts/gui_retest_issue_audit.py --skip-profile      -> passed
scripts/gradle-local.sh --no-daemon build                    -> BUILD SUCCESSFUL
scripts/gradle-local.sh --no-daemon validateEbbData          -> BUILD SUCCESSFUL
scripts/run_smoke_checks.sh                                  -> passed
scripts/gradle-local.sh --no-daemon runGametestServer --args nogui -> BUILD SUCCESSFUL, 6 required GameTests passed
git diff --check                                             -> passed
python3 scripts/gui_e2e_run.py --scenario dry_run             -> passed
scripts/configure_pcl_test_client.sh                         -> refreshed separate Fabric profile
scripts/check_pcl_runtime_loaded.py                          -> passed, jar 25e896a4bc2847d3c941287c356f44eb650bc33c423c317f63567a0f164d2757, counts 19/12/14/7
python3 scripts/gui_e2e_run.py --scenario gui_retest --gui --gui-wait 1.4 -> 282 steps, 0 failures
python3 scripts/gui_retest_issue_audit.py --require-save      -> passed with matching profile jar
```

## 2026-06-03 dialogue wait-state / GUI hotfix snapshot

A Windows GUI screenshot showed a dialogue choice stuck on `等待服务器……`. The stale runtime log contained `ZipFile invalid LOC header`, `Failed to load class file for DialogueService$ChoiceResolution`, and server task errors. The on-disk jars were valid, so the root cause was refreshing the profile-local mod jar while the Minecraft JVM was still running, leaving the active classloader with stale ZIP offsets.

Hotfixes implemented:

- Server dialogue-choice receiver catches unexpected failures and sends `DialogueClosePayload(..., server_error)` instead of leaving the client waiting forever.
- Client `DialogueScreen` re-enables choices after a 10 second response timeout and shows a localized recovery message.
- `scripts/configure_pcl_test_client.sh` now refuses to refresh `26.1.2-Fabric-Ebb-Test` while the matching Java process is alive; this guard was verified with exit code `2`.
- Runtime checks now fail if `latest.log` contains stale ZIP/classloader failure signatures.
- GUI automation setup no longer uses invalid `minecraft:oak_sign[facing=south]`; guestbook setup uses a lectern placeholder, stable/guestbook viewpoints now actually hit their targets, and GUI assertions require a real cyan-bordered dialogue screen.

Latest validation evidence:

```text
scripts/gradle-local.sh --no-daemon build                    -> BUILD SUCCESSFUL
scripts/gradle-local.sh --no-daemon validateEbbData          -> BUILD SUCCESSFUL
scripts/run_smoke_checks.sh                                  -> passed
scripts/gradle-local.sh --no-daemon runGametestServer --args nogui -> BUILD SUCCESSFUL, 6 required GameTests passed
scripts/gui_retest_issue_audit.py --require-save             -> passed
scripts/check_pcl_runtime_loaded.py                          -> passed, jar 954262dea77f6507cffa82ae088e47d69f82979ba9a94917a01d4f61b185723d, counts 19/12/14/7
python3 scripts/gui_e2e_run.py --scenario gui_retest --gui --gui-wait 1.4 -> 270 steps, 0 failures
```

GUI evidence files include:

```text
build/gui-e2e/role_innkeeper_after_choice_1.png
build/gui-e2e/block_guestbook_torn_page_dialogue.png
build/gui-e2e/block_stable_mud_dialogue.png
build/gui-e2e/gui-retest-report.json
```

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
- The separate PCL test profile was refreshed with jar `0d66613edc03b54684e1cca4a7c38d36829899cab06b37e6ff48874cb08ca15e`. A fresh `26.1.2-Fabric-Ebb-Test` relaunch loaded that jar for the P22 GUI proof. Later P23 work refreshed the profile again; see latest artifact hashes above.

P22 code/build validation so far:

```text
scripts/gradle-local.sh --no-daemon build                    -> BUILD SUCCESSFUL
scripts/gradle-local.sh --no-daemon validateEbbData          -> BUILD SUCCESSFUL
scripts/run_smoke_checks.sh                                  -> passed, including P22 GoalStaticAudit and GuiRetestIssueAudit
scripts/gradle-local.sh --no-daemon runGametestServer --args nogui -> BUILD SUCCESSFUL, 6 required GameTests passed
git diff --check                                             -> passed
scripts/configure_pcl_test_client.sh                         -> refreshed separate Fabric test profile
```

Final P22 GUI/runtime proof after relaunch:

```text
scripts/check_pcl_runtime_loaded.py -> passed with jar 0d66613edc03b54684e1cca4a7c38d36829899cab06b37e6ff48874cb08ca15e and runtime counts dialogues=13, block_groups=8, entity_bindings=10, npc_routines=5
build/gui-e2e/p22_locked_door_prompt_proof.png      -> merged cyan block-group outline and `按 [X] 互动` prompt
build/gui-e2e/p22_locked_door_f3_debug_proof.png    -> F3 Ebb diagnostics show `style=merged` with id/dialogue details near the hotbar, away from vanilla debug text
build/gui-e2e/p22_innkeeper_prompt_proof.png        -> bound NPC outline and `按 [X] 互动` prompt
build/gui-e2e/p22_innkeeper_f3_debug_proof.png      -> F3 Ebb diagnostics show `style=outline` with entity/dialogue details
```

P22 is now complete; continue with P23 dialogue UI and reading rhythm.

## P23 dialogue UI and reading rhythm snapshot

Implemented after the final P22 proof:

- `DialogueScreen` now uses `text_key` fallback so missing translations display literal author text rather than raw keys.
- Dialogue history highlights the current node entry with a `▶` marker and distinct styling.
- Keyboard navigation supports number keys 1-5, Enter, Page/Arrow up/down, Home, and End for choice/history rhythm.
- Dialogue check authoring supports hidden DC / hidden roll display controls: `hidden_dc`, `hidden_roll`, `show_dc`, `show_roll`, `display_dc`, and `display_roll`.
- Choice labels and roll-result echoes honor those display controls while server-side d20 resolution remains authoritative.
- Client-local player reading settings are available from the dialogue panel: `A-`, `A+`, and text-speed cycling. They persist in the separate profile as `config/ebb-client.json` with `dialogue_font_scale` and `dialogue_text_speed`.
- Body/history/status text is scaled and wrapped against the selected font scale; the roll/status/chime/clue/quest echo area is scissored and reserved above choices so long echoes clip instead of drawing through buttons.
- Dialogue/action/thought choices, Chime status inserts, roll results, clue/journal/quest/feat/take-root echoes, relationship/routine/conflict echoes, and current-history focus have distinct colors or typography.
- The header title is clipped to a title lane beside the settings buttons so settings no longer overlap the dialogue id/node label.

P23 verification evidence:

```text
scripts/gradle-local.sh --no-daemon test                     -> BUILD SUCCESSFUL, including hidden DC/roll JUnit coverage
scripts/gradle-local.sh --no-daemon build                    -> BUILD SUCCESSFUL
scripts/configure_pcl_test_client.sh                         -> refreshed separate Fabric test profile
scripts/check_pcl_runtime_loaded.py                          -> passed after relaunch with jar 89c54c83518a07493ed21a377a0a4bae3ba4fd4ccfd7c7ba7c86e28c0671a6f9 and counts 13/8/10/5
```

GUI proof screenshots captured from `26.1.2-Fabric-Ebb-Test` / `新的世界 (1)`:

- `build/gui-e2e/p23_scale3_dialogue_open.png` — scale 3, settings visible, five-choice page, no status/choice overlap.
- `build/gui-e2e/p23_scale3_roll_status.png` — scale 3, visible d20 roll/status echoes above choices.
- `build/gui-e2e/p23_scale3_settings_changed.png` — scale 3, A+/speed settings persisted (`dialogue_font_scale=1.1`, `dialogue_text_speed=fast`).
- `build/gui-e2e/p23_scale2_dialogue_open.png` — scale 2, font-speed controls and choice area remain readable.
- `build/gui-e2e/p23_scale2_roll_status.png` — scale 2, quest/take-root/journal status echoes clip above the single visible choice.
- `build/gui-e2e/p23_headerfix_scale2_dialogue_open.png` — refreshed jar after the header-lane fix; settings buttons no longer overlap the dialogue id/node label.

Chime visual treatment is additionally covered by static audit/code paths (`CHIME_STATUS_COLOR`, `[Chime:]` status detection) because the mutable GUI save had already consumed the one-shot chime triggers during earlier playtests.

Final P23 validation after documentation/profile refresh:

```text
python3 scripts/goal_static_audit.py                         -> passed P20/P21/P22/P23 guardrails
python3 scripts/deep_research_static_audit.py                -> passed
python3 scripts/third_review_static_audit.py                 -> passed
scripts/compile_authoring_sources.py --clean                 -> compiled dialogues=1, interactables=1, npc=2, errors=0
scripts/gradle-local.sh --no-daemon test                     -> BUILD SUCCESSFUL
scripts/gradle-local.sh --no-daemon build                    -> BUILD SUCCESSFUL
scripts/gradle-local.sh --no-daemon validateEbbData          -> BUILD SUCCESSFUL
scripts/run_smoke_checks.sh                                  -> passed, including GoalStaticAudit and GuiRetestIssueAudit
scripts/gradle-local.sh --no-daemon runGametestServer --args nogui -> BUILD SUCCESSFUL, 6 required GameTests passed
scripts/run_gui_automation_smoke.sh                          -> passed
python3 scripts/gui_e2e_run.py --scenario runtime_check       -> report build/gui-e2e/runtime-check-report.json
scripts/check_pcl_runtime_loaded.py                          -> passed with runtime counts 13/8/10/5
git diff --check                                             -> passed
```

P23 is complete. Next: P24 authoring and validation hardening.


## P24 authoring and validation hardening snapshot

Implemented after P23 closure:

- Expanded `docs/json_authoring_guide.md` with condition/effect reference tables covering all current parser-supported types and aliases.
- Added starter JSON Schemas under `docs/schemas/` for dialogues, block groups, entity bindings, and chimes.
- Hardened `scripts/compile_authoring_sources.py` so malformed YAML/JSON reports file:line:column diagnostics instead of an unwrapped traceback.
- Added `scripts/p24_authoring_validation.py` for cross-registry authoring validation: dialogue references, block/entity dialogue links, quest/feat/chime/journal/clue/routine/relationship/scene/conflict ids, chime trigger tags, and high-stakes failure-forward lint.
- Added `authoring/examples/tavern_case/` as a compact example authoring pack; it compiles and passes P24 reference validation.
- Wired P24 checks and a malformed-YAML diagnostic regression into `scripts/run_smoke_checks.sh`.
- Fixed a real authoring bug found by the new validator: `witness_intro` used `witness.read` chime tags but bundled chimes only advertised `innkeeper.read`; `instinct` and `empathy` now include `witness.read`.

P24 validation:

```text
python3 scripts/goal_static_audit.py                         -> passed P20/P21/P22/P23/P24 guardrails
scripts/compile_authoring_sources.py --clean                 -> compiled dialogues=1, interactables=1, npc=2, errors=0
scripts/compile_authoring_sources.py --source authoring/examples/tavern_case --out build/generated/ebb_authoring_examples/tavern_case/data/ebb --clean -> compiled cleanly
scripts/p24_authoring_validation.py                          -> passed bundled + generated authoring roots
scripts/p24_authoring_validation.py build/generated/ebb_authoring_examples/tavern_case/data/ebb -> passed example pack
scripts/gradle-local.sh --no-daemon test                     -> BUILD SUCCESSFUL
scripts/gradle-local.sh --no-daemon build                    -> BUILD SUCCESSFUL
scripts/gradle-local.sh --no-daemon validateEbbData          -> BUILD SUCCESSFUL
scripts/run_smoke_checks.sh                                  -> passed with P24 validator, example-pack compile, and malformed-YAML diagnostic regression
scripts/gradle-local.sh --no-daemon runGametestServer --args nogui -> BUILD SUCCESSFUL, 6 required GameTests passed
scripts/configure_pcl_test_client.sh                         -> refreshed separate Fabric test profile with jar 2a3cf218c500fc58a2e9c987cd0f423da6c438cbe4d045c79e466d976f43eca9
git diff --check                                             -> passed before and after P24 implementation
```

P24 is complete. Next: P25 Quest Tree / Take Root / Feat maturation.


## P25 Quest Tree / Take Root / Feat maturation snapshot

Implemented after P24 closure:

- `QuestTreeService` now emits a clearer branch map with `◆ MAJOR`, `◇ MINOR`, state, descriptions, `★ TAKE ROOT` text, grants, and a feat loadout section showing active/unlocked/locked status, passive/slot flags, source quest branches, and check modifiers.
- `QuestTreeScreen` now has filter tabs for All / Major / Minor / Feats / Take Root and colors major branches, minor branches, Take Root moments, feat loadout lines, and active feats differently.
- `JournalService` now includes category tags in entry lines; `JournalScreen` has All / Clues / Leads / Quests / Scenes filters and category-aware colors.
- `DialogueScreen` now gives `take_root:` status echoes their own special color instead of reusing generic quest color.
- Added JUnit coverage `majorQuestCannotTakeRootTwice`, proving repeated major branch completion is idempotent and does not duplicate feat unlocks or active slots.

P25 validation:

```text
python3 scripts/goal_static_audit.py                         -> passed P25 guardrails
scripts/gradle-local.sh --no-daemon test                     -> BUILD SUCCESSFUL
scripts/gradle-local.sh --no-daemon build                    -> BUILD SUCCESSFUL
scripts/gradle-local.sh --no-daemon validateEbbData          -> BUILD SUCCESSFUL
scripts/run_smoke_checks.sh                                  -> passed with P24/P25 static guardrails and smoke checks
scripts/gradle-local.sh --no-daemon runGametestServer --args nogui -> BUILD SUCCESSFUL, 6 required GameTests passed
scripts/configure_pcl_test_client.sh                         -> refreshed separate Fabric test profile with jar 12e2019548e6ffb50db6c5f2d661d49fe8264f2ecc2efc858a0ee86f6d46fb5f
git diff --check                                             -> passed during P25 validation
```

P25 is complete. Next: P26 Chime / inner voice expansion.


## P26 Chime / inner voice expansion snapshot

Implemented after P25 closure:

- Expanded bundled Chimes from four voices to eight DND-8 attribute voices: Force/strength, Finesse/dexterity, Endurance/constitution, Logic/intelligence, Empathy/wisdom, Rhetoric/charisma, Instinct/perception, and Dread/luck.
- `ChimeDefinition` now parses `tone_guide`, `active_thoughts`, `one_shot_per_node`, and `one_shot_global` in addition to trigger tags, speaker style, cooldown, and passive lines.
- `ChimeResolver` now uses server day time for per-player `cooldown_ticks` and keeps explicit per-node/global one-shot flags so repeated nodes do not spam passive inserts; it also exposes `explain(...)` for developer reason traces.
- Demo innkeeper content now includes one active thought route per Chime, each gated by `chime:ebb:demo/<voice>` and adding the matching `ebb:demo/thought_<voice>` thought.
- Chime authoring docs/schema now document tone guides, active thought route metadata, and anti-spam tuning. `/ebb dev` snapshots include a Chime trigger debug section for active dialogue nodes, listing ready/skip reasons such as missing tags, low score, one-shot seen, and cooldown remaining.
- Added JUnit and smoke coverage for DND-8 Chime coverage, active routes, metadata, and cooldown behavior.

P26 validation:

```text
python3 scripts/goal_static_audit.py                         -> passed P26 guardrails
scripts/gradle-local.sh --no-daemon test --tests com.crpg.ebb.DeepResearchDataTest -> BUILD SUCCESSFUL
scripts/gradle-local.sh --no-daemon build                    -> BUILD SUCCESSFUL
scripts/gradle-local.sh --no-daemon validateEbbData          -> BUILD SUCCESSFUL
scripts/run_smoke_checks.sh                                  -> passed with P26 static/smoke coverage and GuiRetestIssueAudit
scripts/gradle-local.sh --no-daemon runGametestServer --args nogui -> BUILD SUCCESSFUL, 6 required GameTests passed
scripts/configure_pcl_test_client.sh                         -> refreshed separate Fabric test profile with jar cd3239b53a576cf68599b6886eb32df0af079f3859f74c06bd6b4c32047596a6
git diff --check                                             -> passed
```

P26 is complete. Next: P27 NPC art, animation, and routine production.


## P27 NPC art, animation, and routine production snapshot

Implemented after P26 closure:

- Added role-specific visual skins for the existing temporary humanoid GeckoLib NPC model: innkeeper, witness, tenant, and guard now resolve to distinct placeholder textures from the NPC routine/narrative key.
- Added conversation animation hooks for `talk`, `think`, `dismiss`, and `nervous_idle`, plus a `fidget` routine animation, while preserving idle/walk behavior.
- Added routine action validation for invalid actions, invalid/underspecified paths, invalid pose names, and invalid animation names.
- Added routine debug command output showing visual role, current routine debug action, target, step key, path index, and conversation-focus state.
- Active dialogues now explicitly pause movement, set conversation pose/animation, look at the player, and restore the previous routine pose/animation after focus ends.

P27 validation:

```text
python3 scripts/goal_static_audit.py                         -> passed P27 guardrails
scripts/gradle-local.sh --no-daemon test --tests com.crpg.ebb.DeepResearchDataTest -> BUILD SUCCESSFUL
scripts/gradle-local.sh --no-daemon build                    -> BUILD SUCCESSFUL
scripts/gradle-local.sh --no-daemon validateEbbData          -> BUILD SUCCESSFUL
scripts/run_smoke_checks.sh                                  -> passed with P27 static/smoke checks and GuiRetestIssueAudit
scripts/gradle-local.sh --no-daemon runGametestServer --args nogui -> BUILD SUCCESSFUL, 6 required GameTests passed
scripts/configure_pcl_test_client.sh                         -> refreshed separate Fabric test profile with jar 5caede905791192f8987eb97927ad5e05d51b3bd74204333e92584864543a225
git diff --check                                             -> passed
```

P27 is complete. Next: P28 Investigation and set-piece conflict expansion.

## P28 Investigation and set-piece conflict expansion snapshot

Implemented after P27 closure:

- Formalized set-piece conflict data with phases `setup`, `pressure`, `turn`, `consequence`, and `resolution`, plus phase descriptions, leverage clue ids, and declared outcomes.
- Added `ConflictOutcomeDefinition` and `apply_conflict_outcome` so dialogue nodes can apply named nonviolent, messy, and failure-forward results instead of directly hard-coding every state.
- Added persisted conflict phase helpers (`getConflictPhase` / `setConflictPhase`) while preserving legacy conflict states such as `active`, `resolved`, and `failed_forward`.
- Expanded conflict status echoes and dialogue UI labels to show phase, state, stress, resolve, known leverage clues, and outcome count. `/ebb dev` snapshots now include a conflict phase/status catalog.
- Expanded `ebb:demo/hallway_confrontation` and `guard_intro` with clue-gated approaches, clue-modified checks, a quiet nonviolent resolution, a messy resolution, and two fail-forward outcomes (`failed_forward_public` and `failed_forward`).
- Added conflict schema/docs, authoring validator outcome-reference checks, JUnit coverage, smoke coverage, and static audit guardrails for P28.

P28 validation:

```text
python3 scripts/goal_static_audit.py                         -> passed P28 guardrails
scripts/gradle-local.sh --no-daemon validateEbbData          -> BUILD SUCCESSFUL
scripts/gradle-local.sh --no-daemon test --tests com.crpg.ebb.DeepResearchDataTest -> BUILD SUCCESSFUL
scripts/run_smoke_checks.sh                                  -> passed with P28 static/smoke coverage and GuiRetestIssueAudit
scripts/gradle-local.sh --no-daemon build                    -> BUILD SUCCESSFUL
scripts/gradle-local.sh --no-daemon runGametestServer --args nogui -> BUILD SUCCESSFUL, 6 required GameTests passed
scripts/configure_pcl_test_client.sh                         -> refreshed separate Fabric test profile with jar 01f617f760d35081afbd9872c3bbecffed71264384c169ddc9dd55474837ac24
```

P28 is complete. Next: P29 Branch consequences and world reactivity.

## P29 Save/load, multiplayer, and permissions hardening snapshot

Implemented after P28 closure:

- Incremented `NarrativeSavedData.CURRENT_SCHEMA_VERSION` to 2 and added v1→v2 migration that infers persisted `conflict_phase:<id>` values from legacy conflict state plus stress/resolve scores.
- Added saved-data migration tests that prove legacy worlds keep conflict state/score/scene data, gain conflict phase, retain safe defaults, and re-encode at the current schema version.
- Added dialogue session preflight validation for conversation UUID, player UUID, and timeout before any choice effects run; spoofed, stale, expired, invalid-choice, unavailable-choice, and stale action-target attempts record security diagnostics.
- Added same-NPC contention protection: another player cannot open a dialogue against an entity already reserved by an active session (`entity_dialogue_busy`). Different NPCs remain independently sessionable.
- Added missing-client-mod diagnostics for dedicated-server sync payloads; `/ebb dev` now shows dialogue security counters and missing Ebb client payload diagnostics.
- Added command-permission regression coverage preserving OP-only admin commands while keeping player-safe self-inspection commands available.

P29 validation:

```text
python3 scripts/goal_static_audit.py                         -> passed P29 guardrails
scripts/gradle-local.sh --no-daemon test --tests com.crpg.ebb.DeepResearchDataTest -> BUILD SUCCESSFUL
scripts/run_smoke_checks.sh                                  -> passed with P29 static/smoke coverage and GuiRetestIssueAudit
scripts/gradle-local.sh --no-daemon validateEbbData          -> BUILD SUCCESSFUL
scripts/gradle-local.sh --no-daemon runGametestServer --args nogui -> BUILD SUCCESSFUL, 6 required GameTests passed
scripts/configure_pcl_test_client.sh                         -> refreshed separate Fabric test profile with jar 9bba5b7f63f2ad696cd8204b5ec6940b4268a92be99f994c903e3792f384da5c
```

P29 is complete. Next: P30 Vertical slice content expansion.

## P30 Vertical slice content expansion snapshot

Implemented after P29 closure:

- Expanded the tavern demo into a 3-act content map: discovery, pressure/investigation, and confrontation/ending.
- Added four additional block-group investigation points for 12 total: stairwell dust, kitchen manifest, guestbook torn page, and stable mud.
- Added cook and courier NPC coverage with role bindings, name fallback bindings, routines, dialogues, and placeholder textures, bringing the slice to six role NPCs.
- Added two major branches (`tavern_trade`, `tavern_mercy`) and eight minor branches.
- Expanded feats from 4 to 12.
- Kept 8 DND-8 Chimes and expanded them to at least 40 total Chime lines.
- Expanded journal/clue content to at least 20 journal entries and 20 clues.
- Added two set-piece conflicts (`kitchen_bargain`, `courtyard_standoff`) for 3 total conflicts.
- Added `trade_end` and `mercy_end` ending placeholders alongside public/quiet/messy endings.
- Added P30 docs, static audit, JUnit, and smoke count coverage.

P30 validation:

```text
python3 scripts/goal_static_audit.py                         -> passed P30 guardrails
scripts/gradle-local.sh --no-daemon test --tests com.crpg.ebb.DeepResearchDataTest -> BUILD SUCCESSFUL
scripts/run_smoke_checks.sh                                  -> passed with P30 static/smoke coverage and GuiRetestIssueAudit
scripts/gradle-local.sh --no-daemon validateEbbData          -> BUILD SUCCESSFUL
scripts/gradle-local.sh --no-daemon runGametestServer --args nogui -> BUILD SUCCESSFUL, 6 required GameTests passed
scripts/configure_pcl_test_client.sh                         -> refreshed separate Fabric test profile with jar 9e0a016757b91f0636814723c9ab0be3b21c71cae5bafac042fab1b074aaa2ed
```

P30 is complete. Next: P31 packaging, installation, release metadata, authoring tutorial, changelog, and license clarity.

## P31 Release packaging and player documentation snapshot

Implemented after P30 closure:

- Added `docs/installation.md` with client install, dedicated server install, Java/Fabric API/GeckoLib dependency requirements, missing-client diagnostics, and the known-compatible `26.1.2-Fabric-Ebb-Test` PCL profile refresh workflow.
- Added `docs/release_metadata_draft.md` with shared alpha release copy plus Modrinth and CurseForge field drafts.
- Added `docs/story_pack_tutorial.md` showing how to create a custom block-group investigation point, dialogue, d20 check, failure-forward branch, journal entry, and clue.
- Added `CHANGELOG.md` for the current `0.1.0-dev` alpha vertical-slice state.
- Added `LICENSE.md` clarifying the mixed license split: code/scripts under MIT, bundled story data and placeholder Ebb assets under CC BY-NC-SA 4.0, and third-party/platform content under their own licenses.
- Updated `README.md` with release packaging links and the current P30 vertical-slice counts.

P31 validation:

```text
python3 scripts/goal_static_audit.py                         -> passed P31 release-packaging guardrails
scripts/run_smoke_checks.sh                                  -> passed with P31 static/smoke coverage and GuiRetestIssueAudit
scripts/gradle-local.sh --no-daemon validateEbbData          -> BUILD SUCCESSFUL
scripts/gradle-local.sh --no-daemon runGametestServer --args nogui -> BUILD SUCCESSFUL, 6 required GameTests passed
```

P31 is complete; the architecture-plan P20-P31 track is fully implemented to the current automated-verification standard. The game jar hash did not change during P31 because the phase only added docs/static-audit/release packaging files.

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

- Dialogues: 20 bundled demo/debug/LLM dialogues.
- Block groups: 12 interactable investigation points.
- Entity bindings: 15 bindings including role tags, role custom-name fallbacks, and an opt-in minor villager LLM candidate.
- NPC routines: 7 routines.
- Role NPCs: innkeeper, witness, suspicious tenant, guard/fixer, cook, and courier.
- Quest branches: 4 major routes and 8 minor branches, with Take Root and ending placeholders.
- Feats: 12 demo feats with active/passive/source metadata.
- Chimes: Force, Finesse, Endurance, Logic, Empathy, Rhetoric, Instinct, Dread, with 40 total passive lines.
- Journal/clue content: 20 journal entries and 21 clue definitions.
- Investigation/conflict: locked-room clue scene plus hallway confrontation, kitchen bargain, and courtyard standoff set-pieces.

## Next roadmap focus

The project has reached the technical MVP/P8 vertical slice. The active plan now starts from `GOAL.md` P20/P21 and then proceeds through P22–P31 toward Alpha 0.1.

## Phase 34 LLM fake-provider foundation snapshot

Implemented the first PLAN.md P34 slice:

- Added server-only `LlmConfig` reading `config/ebb-llm-server.json`; default mode is disabled and fake/disabled modes report `network=blocked`.
- Added `LlmGatewayClient`, deterministic `FakeLlmGatewayClient`, `DisabledLlmGatewayClient`, `LlmChatService`, LLM chat sessions, timeout cleanup, replay/ownership preflight, and explicit `llm_disabled` errors.
- Added LLM chat network payloads for opened/message/chunk/options/close/cancel/error and registered server/client receivers without any secret/token fields.
- Added `NpcChatScreen` skeleton with live in-game background, input box, suggested option buttons, status/error rendering, and cancel/close handling.
- Added dialogue choice type `llm_chat` plus `free_chat` alias and a sample innkeeper free-chat choice; scripted dialogue remains primary and d20 rolls are not run for LLM chat choices.
- Added `/ebb llm status` and OP `/ebb llm reload_config`.
- Added P34 JUnit/GameTest/static-audit coverage for fake reply determinism, disabled `llm_disabled`, session timeout, payload wiring, command availability, and no API-key/network usage in disabled/fake paths.

Phase 34 artifact hashes after the first successful P34 build/JUnit pass:

```text
build/libs/ebb-0.1.0-dev.jar         c944272a3dec18e5e89463d457a272857b0763686e1c56d3f122ee6d64edbe03
build/libs/ebb-0.1.0-dev-sources.jar f9d5ac6e056829f018c1d1da31f5cbb4b004d9bed21ad4a5ad2ea4f35d822345
```

Phase 34 artifact hashes after the post-review rebuild:

```text
build/libs/ebb-0.1.0-dev.jar         1f502a7eadb65cfe2520c7e857b4ee29b842dd786a48d4956c9697b1ac5f157d
build/libs/ebb-0.1.0-dev-sources.jar 87cadd087bb5403b25b2bc35cc30ce1077cd75daf0c2ec289f5c10ce5564a3c1
```

## Phase 35 NPC Profile / Tier / Promotion data-layer snapshot

Implemented the PLAN.md P35 data-layer slice:

- Added `NpcTier` with `major_scripted`, `minor_generatable`, `major_promoted`, `static_non_llm`, and `disabled`.
- Added `NpcProfileDefinition` and `NpcProfileRegistry` under `com.crpg.ebb.npc.profile`, plus the `npc_profiles` data reload registry and developer snapshot/status surfaces.
- Added six P30 role profiles: `ebb:demo/innkeeper`, `ebb:demo/witness`, `ebb:demo/tenant`, `ebb:demo/guard`, `ebb:demo/cook`, and `ebb:demo/courier`.
- Extended entity bindings with `npc_tier`, `npc_profile`, `llm.promote_on_first_chat`, and `llm.profile_seed_archetypes`; these fields sync through `EntityBindingSyncPayload`.
- Added an explicit opt-in minor candidate binding for `minecraft:villager` with tag `ebb.npc.minor`, without re-enabling debug entity fallback.
- Added `NpcPromotionService`, which creates deterministic-enough `major_promoted` profile JSON and stores it in `NarrativeSavedData.promoted_npc_profiles`.
- Bumped narrative saved-data schema to v3 and added promoted profile debug/export surfaces.
- Added `/ebb npc profile target`, `/ebb npc profile <npc_key>`, `/ebb npc minorize <entity>`, `/ebb npc promote <entity>`, and `/ebb npc regenerate_profile <npc_key>` as OP/dev inspection helpers.
- Added authoring docs and `docs/schemas/ebb.npc_profile.schema.json`.

Phase 35 validation checkpoint:

```text
scripts/gradle-local.sh --no-daemon compileJava                  -> BUILD SUCCESSFUL
scripts/gradle-local.sh --no-daemon test --tests com.crpg.ebb.DeepResearchDataTest -> BUILD SUCCESSFUL
scripts/gradle-local.sh --no-daemon validateEbbData              -> BUILD SUCCESSFUL
scripts/gradle-local.sh --no-daemon build                        -> BUILD SUCCESSFUL
python3 scripts/goal_static_audit.py                              -> passed including P35 guardrails
scripts/run_smoke_checks.sh                                       -> passed with runtime counts 20/12/15/7
scripts/gradle-local.sh --no-daemon runGametestServer --args nogui -> BUILD SUCCESSFUL, 8 required GameTests passed
```

Phase 35 artifact hashes after the first successful P35 build/validation checkpoint:

```text
build/libs/ebb-0.1.0-dev.jar         fdab4338525cb74c42f65485fb305b0712bea467aaea18bc986f721d20ee76e1
build/libs/ebb-0.1.0-dev-sources.jar f0aaada7ead57b647afb33b49e4d7cf87bb2b87b278da919caaa78deadc26a0d
```

## Phase 36 Gateway / OAuth-OIDC auth snapshot

Implemented the PLAN.md P36 gateway-auth slice:

- Added independent `ebb-llm-gateway/` Java 25 service with dependency-free HTTP handlers for `/v1/health`, `/v1/auth/device/start`, `/v1/auth/device/status`, and `/v1/auth/logout`.
- Added gateway auth providers: `dev_local` for local testing and a generic OIDC device-flow adapter configurable for production providers such as Keycloak/Auth0/Stytch.
- Extended `LlmConfig` with `gateway_base_url`, `gateway_timeout_ms`, and `require_player_auth`; safe config JSON still does not expose secrets or player tokens.
- Added Minecraft server-side auth classes under `com.crpg.ebb.llm.auth`, including server-only token storage, redacted status summaries, dev-local auth client, and HTTP gateway auth client.
- Added `/ebb llm auth`, `/ebb llm status`, and `/ebb llm logout`; raw opaque player tokens are never sent to the client UI.
- `LlmChatService` now gates free-chat opening/message sends with `auth_required` when `require_player_auth=true`; after login fake-provider chat remains deterministic, and logout invalidates access.
- Added P36 JUnit/GameTest/static-audit coverage plus `scripts/p36_gateway_smoke.sh`.

Phase 36 validation checkpoint:

```text
scripts/p36_gateway_smoke.sh                                  -> BUILD SUCCESSFUL, P36 gateway smoke passed
scripts/gradle-local.sh --no-daemon test --tests com.crpg.ebb.DeepResearchDataTest -> BUILD SUCCESSFUL
scripts/gradle-local.sh --no-daemon validateEbbData           -> BUILD SUCCESSFUL
python3 scripts/goal_static_audit.py                          -> passed including P36 guardrails
scripts/run_smoke_checks.sh                                   -> passed including P36 gateway smoke/static guardrails
scripts/gradle-local.sh --no-daemon runGametestServer --args nogui -> BUILD SUCCESSFUL, 9 required GameTests passed
scripts/gradle-local.sh --no-daemon build                     -> BUILD SUCCESSFUL
git diff --check                                               -> passed
```

Current proofs: gateway smoke passes, unauthenticated chat gate returns `auth_required`, dev-local login enables fake chat, logout restores `auth_required`, and client LLM UI/networking contains no `opaque_player_token` surface.

Phase 36 artifact hashes after the first successful P36 build checkpoint:

```text
build/libs/ebb-0.1.0-dev.jar         0e22ea2023c8159b6263bd4a6d0e9b9aa474a065d22ece951b25e5e8ddf1eca7
build/libs/ebb-0.1.0-dev-sources.jar 8bbc5b0c42ce6f59a67c4ad9b1556f0affcdde6ee79e7e7ab32bade68536ccc1
```

## Phase 37 OpenAI Responses Gateway snapshot

Implemented the PLAN.md P37 gateway-chat slice:

- Added `/v1/chat/message` to `ebb-llm-gateway/` with request/response records covering NPC/player ids, conversation id, scene context, structured output, chunked response evidence, model config, and `store:false` privacy state.
- Added gateway chat providers: deterministic `fake`, `mock_openai_responses` for default tests without API consumption, and `openai_responses` using the official `com.openai:openai-java` SDK and Responses API streaming path.
- Added structured JSON schema configuration through `ResponseFormatTextJsonSchemaConfig`, plus streaming/chunk assembly with `ResponseAccumulator` and `createStreaming`.
- Added `SimpleCircuitBreaker` plus gateway timeout handling; real provider failures return JSON errors instead of blocking callers indefinitely.
- Extended Minecraft `LlmConfig` with `default_chat_model`, `llm_chat_streaming`, `structured_output`, and `openai_store` safe config fields. Default privacy remains `store:false`.
- Added `HttpLlmGatewayClient` and wired `LlmChatService` gateway mode to server-to-gateway `/v1/chat/message`; server-only auth tokens are attached only by the Minecraft server and never by the client UI.
- Added P37 JUnit/GameTest/static-audit coverage and `scripts/p37_gateway_chat_smoke.sh`. Tests default to fake/mock provider paths and do not consume OpenAI API quota.

Phase 37 validation checkpoint is complete: gateway smoke, DeepResearchDataTest, validateEbbData, static audit, full smoke runner, GameTest server, build, and git diff checks all passed.

Phase 37 validation checkpoint:

```text
scripts/p37_gateway_chat_smoke.sh                         -> BUILD SUCCESSFUL, P37 gateway chat smoke passed
scripts/gradle-local.sh --no-daemon compileJava           -> BUILD SUCCESSFUL
scripts/gradle-local.sh --no-daemon test --tests com.crpg.ebb.DeepResearchDataTest -> BUILD SUCCESSFUL
scripts/gradle-local.sh --no-daemon validateEbbData       -> BUILD SUCCESSFUL
python3 scripts/goal_static_audit.py                      -> passed including P37 guardrails
scripts/run_smoke_checks.sh                               -> passed after current artifact hash update
scripts/gradle-local.sh --no-daemon runGametestServer --args nogui -> BUILD SUCCESSFUL, 10 required GameTests passed
scripts/gradle-local.sh --no-daemon build                 -> BUILD SUCCESSFUL
```

Phase 37 artifact hashes after the first successful P37 build checkpoint:

```text
build/libs/ebb-0.1.0-dev.jar         a7cf0b8e09a7f48b34d01c7415fc74248fd17bb557ca61f5ac048a4c0c9c875e
build/libs/ebb-0.1.0-dev-sources.jar 582f62c33b48624b834985a98ff07a4b9b1578569b0bdf5022652364bce8495f
```

## Phase 38 MemoryStore MVP snapshot

Implemented the PLAN.md P38 memory-store slice:

- Added H2-backed gateway migration `V001__memory_store.sql` for `schema_migrations`, append-only `memory_records`, `memory_facts`, and `memory_conflicts`.
- Added gateway memory model/classes: `MemoryRecord`, `MemoryFact`, `MemoryConflict`, deterministic `MemoryEmbeddingService`, `MemoryFactExtractor`, `MemorySearchRequest`, `ScoredMemory`, and `MemoryStore`.
- Gateway chat now appends player/NPC turns into memory after successful `/v1/chat/message` responses.
- Added deterministic fact extraction for P38 tests/dev authoring (`fact:subject.predicate=value`, `remember:...`, and first-person self-description). Changed facts create superseded old facts plus open `MemoryConflict` rows.
- Added hybrid retrieval using keyword overlap, deterministic embedding cosine similarity, entity/NPC match, and recency weighting. Retrieval returns citation ids.
- Added gateway endpoints `/v1/memory/search`, `/v1/memory/inspect`, and `/v1/memory/conflicts`.
- Added Minecraft server-side `MemoryGatewayClient` and OP/dev commands `/ebb memory search <query>`, `/ebb memory inspect <id>`, and `/ebb memory conflicts`.
- Added P38 smoke, JUnit, GameTest, docs, and static-audit coverage. Gateway memory tests run with H2 in-memory DB URLs and do not consume OpenAI API quota.

Phase 38 validation checkpoint is complete: gateway memory smoke, DeepResearchDataTest, validateEbbData, static audit, full smoke runner, GameTest server, build, and git diff checks all passed.


Phase 38 validation checkpoint:

```text
scripts/p38_memory_smoke.sh                                -> BUILD SUCCESSFUL, P38 memory smoke passed
scripts/gradle-local.sh --no-daemon compileJava            -> BUILD SUCCESSFUL
scripts/gradle-local.sh --no-daemon test --tests com.crpg.ebb.DeepResearchDataTest -> BUILD SUCCESSFUL
scripts/gradle-local.sh --no-daemon validateEbbData        -> BUILD SUCCESSFUL
python3 scripts/goal_static_audit.py                       -> passed including P38 guardrails
scripts/run_smoke_checks.sh                                -> passed including P38 memory smoke/static guardrails
scripts/gradle-local.sh --no-daemon runGametestServer --args nogui -> BUILD SUCCESSFUL, 11 required GameTests passed
scripts/gradle-local.sh --no-daemon build                  -> BUILD SUCCESSFUL
```

Phase 38 artifact hashes after final P38 validation:

```text
build/libs/ebb-0.1.0-dev.jar         71ed88bf53c0d3cb3d3eab4b4ac1fe6663114e3e55a9e98cd21a364043d78877
build/libs/ebb-0.1.0-dev-sources.jar bde884ef67db57f6642e916fe57cfaf0bf3236cec42fdc6fd30d5d5e895df929
```

## Phase 39 Memory extraction / consolidation snapshot

Implemented the PLAN.md P39 memory extraction and consolidation slice on top of the P38 gateway MemoryStore:

- Added `LlmMemoryOperationExtractor`: LLM `memory_writes` and structured JSON now produce proposed `MemoryOperation` rows rather than direct writes.
- Added `DeterministicMemoryValidator`: proposed ops must pass deterministic validation before they mutate facts/summaries/lessons.
- Added canonical tavern ownership protection. A player self-claim like `我是旅馆老板` rejects the proposed `tavern.owner=player:<uuid>` operation and records an A-MemGuard-style safety lesson; canonical ownership remains with the innkeeper.
- Added `MemoryConsolidator`: background-style episodic summaries, related-memory links, and A-Mem-like evolution update summaries for superseded facts while preserving raw episode text.
- Extended gateway storage with `memory_operations`, `memory_summaries`, `memory_links`, and `memory_safety_lessons`.
- Extended dev visibility: inspectable records include `raw_episode`, `extracted_facts`, memory operations, summaries, related links, and safety lessons; conflicts remain queryable.
- Added `/v1/memory/episodes`, `/v1/memory/lessons`, `/ebb memory episodes`, and `/ebb memory lessons`.
- Acceptance fixture: ledger questioning extracts/searches `questioned_ledger`, so an NPC can retrieve that the player previously questioned the ledger.

Phase 39 validation checkpoint is in progress. Passing checkpoint so far:

```text
scripts/gradle-local.sh --no-daemon compileJava      -> BUILD SUCCESSFUL
scripts/p39_memory_consolidation_smoke.sh            -> BUILD SUCCESSFUL, P39 memory consolidation smoke passed
```

Phase 39 artifact hashes after first successful P39 build checkpoint:

```text
build/libs/ebb-0.1.0-dev.jar         a6066bb5c44d9b4ea2a05961898dda848a8f82923a4df757caed62a9213002fa
build/libs/ebb-0.1.0-dev-sources.jar 7536682d771743c89a1a246ef5dce8fea0df2a4687c88a083534b71a0fab99c0
```

Phase 39 final validation checkpoint:

```text
scripts/gradle-local.sh --no-daemon compileJava            -> BUILD SUCCESSFUL
scripts/p39_memory_consolidation_smoke.sh                  -> BUILD SUCCESSFUL, P39 memory consolidation smoke passed
scripts/gradle-local.sh --no-daemon test --tests com.crpg.ebb.DeepResearchDataTest -> BUILD SUCCESSFUL
python3 scripts/goal_static_audit.py                       -> passed including P39 guardrails
scripts/gradle-local.sh --no-daemon validateEbbData        -> BUILD SUCCESSFUL
scripts/run_smoke_checks.sh                                -> passed including P39 memory consolidation smoke/static guardrails
scripts/gradle-local.sh --no-daemon runGametestServer --args nogui -> BUILD SUCCESSFUL, 11 required GameTests passed
scripts/gradle-local.sh --no-daemon build                  -> BUILD SUCCESSFUL
git diff --check                                           -> passed
```

Phase 39 artifact hashes after final P39 validation:

```text
build/libs/ebb-0.1.0-dev.jar         a6066bb5c44d9b4ea2a05961898dda848a8f82923a4df757caed62a9213002fa
build/libs/ebb-0.1.0-dev-sources.jar 7536682d771743c89a1a246ef5dce8fea0df2a4687c88a083534b71a0fab99c0
```

## Phase 40 NPC Knowledge Base first-code checkpoint

Phase 40 is in progress, not complete. Current checked-in draft adds the core code surfaces for PLAN.md P40:

- `NpcKnowledgePackDefinition` with chunks, tags, `secret`, and `reveal_conditions` evaluated through existing dialogue conditions.
- `NpcKnowledgeRegistry` reload integration through `npc_knowledge_packs`.
- Deterministic local `NpcKnowledgeIndex` scoring for visible chunk retrieval.
- `NpcKnowledgeService` for visible/hidden inspection, prompt-context assembly, dynamic NPC facts, dynamic pack grants, and stance tags.
- Dialogue effects: `npc_kb_add_fact`, `npc_kb_add_pack`, and `npc_stance_shift`.
- LLM chat prompt assembly now includes only currently visible NPC knowledge context.

P40 remains pending for demo knowledge-pack data, `/ebb kb inspect <npc>`, acceptance tests proving hidden secret non-leakage before clue and changed answers after clue, and the final P40 static/GameTest/full-smoke checkpoint.

Phase 40 first-code checkpoint validation:

```text
scripts/gradle-local.sh --no-daemon compileJava            -> BUILD SUCCESSFUL
scripts/p39_memory_consolidation_smoke.sh                  -> BUILD SUCCESSFUL, P39 memory consolidation smoke passed
python3 scripts/goal_static_audit.py                       -> passed including P39 guardrails and current artifact hashes
scripts/gradle-local.sh --no-daemon validateEbbData        -> BUILD SUCCESSFUL
scripts/gradle-local.sh --no-daemon build                  -> BUILD SUCCESSFUL
git diff --check                                           -> passed
```

Phase 40 first-code checkpoint artifact hashes:

```text
build/libs/ebb-0.1.0-dev.jar         82d4a6a8af5356238d899b5037a2f81775a77cd2e264cc0908c24c44b463b2e9
build/libs/ebb-0.1.0-dev-sources.jar 2766691671b1c13772ed1759b8ed437e3839e504be1e5dcae4cdeb07e8f087ee
```


## Phase 40 NPC Knowledge Base final snapshot

Implemented and validated the PLAN.md P40 NPC Knowledge Base and story-effects slice:

- Added seven bundled demo `npc_knowledge_packs` matching the scripted NPC profile `initial_packs`.
- `NpcKnowledgePackDefinition` parses chunk ids, text/content, tags, `secret`, and `reveal_conditions` through existing `DialogueCondition` semantics.
- `NpcKnowledgeRegistry` reloads `data/*/npc_knowledge_packs`, contributes to `/ebb data` summary, and reports validation messages.
- `NpcKnowledgeIndex` provides deterministic local embedding-style scoring for query-ranked chunk retrieval.
- `NpcKnowledgeService` assembles prompt context from visible chunks only, tracks hidden chunks for dev inspection, includes dynamic player-added NPC facts, and stores stance shifts in player/NPC state.
- Dialogue story effects are now available: `npc_kb_add_fact`, `npc_kb_add_pack`, and `npc_stance_shift`.
- `/ebb kb inspect <npc>` and `/ebb kb inspect <npc> <query>` show current visible/hidden chunks for the command player.
- Acceptance fixture: before `ebb:demo/guestbook_gap`, the innkeeper KB prompt does not include `tenant paid cash`; after the clue, the same question includes the secret chunk and the deterministic fake reply changes from `kb=public_only` to `kb=secret_visible`.

Phase 40 final validation checkpoint:

```text
scripts/gradle-local.sh --no-daemon test --tests com.crpg.ebb.DeepResearchDataTest -> BUILD SUCCESSFUL
scripts/gradle-local.sh --no-daemon validateEbbData        -> BUILD SUCCESSFUL
scripts/gradle-local.sh --no-daemon runGametestServer --args nogui -> BUILD SUCCESSFUL, 12 required GameTests passed
python3 scripts/goal_static_audit.py                       -> passed including P40 guardrails
scripts/run_smoke_checks.sh                                -> passed including P40 static/JUnit smoke coverage
scripts/gradle-local.sh --no-daemon build                  -> BUILD SUCCESSFUL
git diff --check                                           -> passed
```

Phase 40 artifact hashes after validation build checkpoint:

```text
build/libs/ebb-0.1.0-dev.jar         bcc8b8eb179cd3683c33ef70af704a56c482b4f29ae3bdbcde493129b04d3e63
build/libs/ebb-0.1.0-dev-sources.jar 4b09c667ac467dce360fd7cebe8a5545fd69988ea6306287c826b8110990d13c
```


## Phase 41 Minor NPC instant generation snapshot

Implemented PLAN.md P41 on top of the earlier P35 promotion scaffold:

- Added `NpcProfileGenerator` with an auditable prompt/schema contract (`ebb.npc_profile_generator.v1`).
- Generated promoted profiles now include `character`, `stance`, `knowledge`, `knowledge_seed`, `suggested_options`, and `profile_generation` metadata.
- First eligible chat for an `ebb.npc.minor` / `minor_generatable` entity still persists a promoted major profile in `NarrativeSavedData.promoted_npc_profiles`; repeated/re-entry promotion attempts return `existing_promoted_major` and preserve the original profile JSON.
- Added a per-world-hour promotion rate limit (`MAX_PROMOTIONS_PER_WORLD_HOUR`) with `rate_limited` status instead of blindly promoting every tagged background NPC.
- Added OP/dev review and rejection surfaces: `/ebb npc review <npc_key>`, `/ebb npc reject_profile <npc_key>`, plus existing minorize/promote/regenerate/profile commands.

Phase 41 validation checkpoint so far:

```text
scripts/gradle-local.sh --no-daemon test --tests com.crpg.ebb.DeepResearchDataTest -> BUILD SUCCESSFUL
scripts/gradle-local.sh --no-daemon runGametestServer --args nogui -> BUILD SUCCESSFUL, 12 required GameTests passed
```


Phase 41 validation update after artifact hash refresh:

```text
scripts/gradle-local.sh --no-daemon validateEbbData        -> BUILD SUCCESSFUL
scripts/run_smoke_checks.sh                                -> first run reached GoalStaticAudit and correctly failed on stale artifact hash after P41 jar changed; hash updated below before rerun
```

Phase 41 artifact hashes after P41 code build:

```text
build/libs/ebb-0.1.0-dev.jar         416f0fb73ca92a1b3dc6861d96797a055eae4a254d8be670e8e35f66ca39cd5e
build/libs/ebb-0.1.0-dev-sources.jar 2491d34a2fab835948d46e4f7f127dfc763dcfd675aa1e169e085e40da1185c2
```


Phase 41 final validation checkpoint:

```text
scripts/gradle-local.sh --no-daemon test --tests com.crpg.ebb.DeepResearchDataTest -> BUILD SUCCESSFUL
scripts/gradle-local.sh --no-daemon runGametestServer --args nogui -> BUILD SUCCESSFUL, 12 required GameTests passed
scripts/gradle-local.sh --no-daemon validateEbbData        -> BUILD SUCCESSFUL
python3 scripts/goal_static_audit.py                       -> passed including P41 guardrails
scripts/run_smoke_checks.sh                                -> passed including P41 static/JUnit smoke coverage
scripts/gradle-local.sh --no-daemon build                  -> BUILD SUCCESSFUL
git diff --check                                           -> passed
```

## Phase 42 LLM Chat UI snapshot

Implemented the PLAN.md P42 LLM Chat UI completion slice:

- Server replies now use `sendStreamingNpcResponse` and `streamingChunks` so `LlmChatChunkPayload` can stream multi-packet NPC text when `llm_chat_streaming=true`.
- `NpcChatScreen` merges streaming NPC chunks into one visible line until `done=true`, and only then releases `waitingForReply`.
- Suggested options remain clickable and are now included in the P42 GUI E2E route.
- Added Return to Script. The button sends `return_to_script`; the server calls `DialogueService.reopenFromLlmChat` to reopen a normal scripted `DialogueSession` at the stored `returnNodeId`.
- Added a Memory Correction button. The next player message is sent as `memory_correction: ...` for gateway-side memory validation/correction handling.
- Added a Dev Citations overlay. Citation ids are hidden by default and rendered in an explicit overlay instead of inline in normal NPC text.
- Added client-side timeout handling (`CLIENT_REPLY_TIMEOUT_MS`) and network-unavailable feedback for LLM cancel sends so timeout/cancel/server-error cases do not leave the UI stuck.
- Expanded the `K` menu with visible LLM auth status guidance plus `/ebb llm status`, `/ebb llm auth`, and `/ebb llm logout` buttons. Tokens remain server-side and status output is redacted.
- Added `scripts/gui_e2e_run.py --scenario llm_chat` plus smoke-manifest coverage for opening NPC chat, sending text, receiving a reply, toggling citations, clicking a suggested option, and returning to scripted dialogue.

Phase 42 validation is complete, including automated validation and an actual Windows GUI `llm_chat` pass against the separate `26.1.2-Fabric-Ebb-Test` profile.

Phase 42 artifact hashes after the final P42 Java/resource build checkpoint:

```text
build/libs/ebb-0.1.0-dev.jar         fddca1051023d9a5d21a57b5da0b1002fc5715c7f8970c45deb925ed398c892a
build/libs/ebb-0.1.0-dev-sources.jar 0d21e3b2ebeb9e93cd8c4edad27fb01a6e8ffde48ed16dcf48cae93214eb8efc
```

P42 docs note: memory correction is player-authored and server/gateway validated; citations overlay remains dev-only; K menu shows LLM status commands.

Phase 42 automated validation checkpoint:

```text
python3 -m py_compile scripts/gui_e2e_run.py scripts/goal_static_audit.py -> pass
scripts/gradle-local.sh --no-daemon test --tests com.crpg.ebb.DeepResearchDataTest -> BUILD SUCCESSFUL
scripts/gradle-local.sh --no-daemon build                  -> BUILD SUCCESSFUL
python3 scripts/goal_static_audit.py                       -> passed including P42 guardrails
scripts/run_gui_automation_smoke.sh                        -> passed, including llm_chat scenario manifest/report generation
scripts/run_smoke_checks.sh                                -> passed
scripts/gradle-local.sh --no-daemon runGametestServer --args nogui -> BUILD SUCCESSFUL, 13 required GameTests passed
scripts/gradle-local.sh --no-daemon validateEbbData        -> BUILD SUCCESSFUL
git diff --check                                           -> passed
```

P42 actual Windows GUI visual run status: complete.

```text
scripts/gui_e2e_run.py --scenario llm_chat --gui --skip-demo-setup --window-title 'Minecraft\*? 26\.1\.2' --gui-wait 1.1 --allow-stale-runtime -> report build/gui-e2e/llm-chat-report.json, failed=[]
```

Actual GUI screenshot evidence:

- `build/gui-e2e/llm_k_menu_status.png` — K-menu LLM status/actions visible with live background.
- `build/gui-e2e/llm_chat_reply.png` — fake-provider LLM chat reply, suggested options, Return to Script / Memory Correction / Citations buttons, and live player view outside the panel.
- `build/gui-e2e/llm_citations_overlay.png` — citations shown only in the explicit dev overlay.
- `build/gui-e2e/llm_suggested_option_reply.png` — suggested option click generated a second fake reply.
- `build/gui-e2e/llm_returned_to_script.png` — Return to Script reopened the scripted dialogue at `ebb:demo/innkeeper_intro / start` with `returned_from_llm_chat` status.

Automation hygiene note: `scenario_llm_chat` now writes its fake LLM server config only when `--gui` is used, so dry-run smoke/report generation does not modify the external PCL test profile.

## Phase 43 Testing / Evaluation / Documentation snapshot

Implemented PLAN.md P43 validation and documentation layer:

- Expanded `docs/json_authoring_guide.md` with a P43 reference for NPC profiles, NPC knowledge packs / KB, LLM server config, and memory effects / proposed LLM memory writes.
- Added `docs/schemas/ebb.npc_knowledge.schema.json`; retained and audited `docs/schemas/ebb.npc_profile.schema.json`.
- Added `scripts/p43_llm_safety_audit.py` and wired it into `scripts/run_smoke_checks.sh`.  The audit checks no secret-like API key literals, fake/mock LLM providers in tests, no hidden KB text in client/sync payloads, and rejection/ignoring of high-risk direct LLM `proposed_effects`.
- Added gateway-side `GatewayChatResponse.sanitizeProposedEffects` so direct model output cannot smuggle high-authority game mutations such as quest completion, item grants, flags, clue reveals, relation changes, routines, or commands through `proposed_effects`.
- Added JUnit coverage through `p43TestingEvaluationAndSafetyGatesAreAuditable`: memory conflict evidence, promotion persistence, prompt pack assembly, docs/schemas, static audit markers, and GUI route markers.
- Added GameTest coverage through `p43FakeChatMinorPromotionAndRelationshipDeltaAreDeterministic`: fake-provider chat, minor promotion persistence, and relationship delta mutation in a real headless Minecraft server.
- Added GUI E2E manifest/route `scripts/gui_e2e_run.py --scenario llm_validation` for auth-disabled status, fake chat, and real-gateway dry-run status without using real OpenAI.

Phase 43 artifact hashes after the P43 build checkpoint:

```text
build/libs/ebb-0.1.0-dev.jar         1d2cfee8a3b323151c7eeda5c0c4c2201f0d97a67f1f8b7c4199d77aedafc1d8
build/libs/ebb-0.1.0-dev-sources.jar 6d867b388a9e16ce2cbc1c228de2c39576936b90ad044918544a714727590ee4
```

Phase 43 validation checkpoint so far:

```text
python3 -m py_compile scripts/gui_e2e_run.py scripts/goal_static_audit.py scripts/p43_llm_safety_audit.py -> pass
python3 scripts/p43_llm_safety_audit.py                    -> pass
python3 scripts/goal_static_audit.py                       -> passed including P43 guardrails
scripts/gradle-local.sh --no-daemon test --tests com.crpg.ebb.DeepResearchDataTest -> BUILD SUCCESSFUL
scripts/p36_gateway_smoke.sh                               -> BUILD SUCCESSFUL
scripts/run_gui_automation_smoke.sh                        -> pass, including llm_validation dry-run manifest/report
scripts/gradle-local.sh --no-daemon build                  -> BUILD SUCCESSFUL
```

Phase 43 final validation update:

```text
scripts/gradle-local.sh --no-daemon validateEbbData        -> BUILD SUCCESSFUL
scripts/gradle-local.sh --no-daemon runGametestServer --args nogui -> BUILD SUCCESSFUL, 14 required GameTests passed
scripts/run_smoke_checks.sh                                -> passed including P43LlmSafetyAudit
python3 scripts/goal_static_audit.py                       -> passed including P43 guardrails
git diff --check                                           -> passed
```

P43 status: complete. Next repository task is a final requirement-by-requirement audit of `E:\MC\PCL\PLAN.md` before declaring the overall persistent goal complete.
