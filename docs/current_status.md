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

- Dialogues: 19 bundled demo/debug dialogues.
- Block groups: 12 interactable investigation points.
- Entity bindings: 14 bindings including role tags and role custom-name fallbacks.
- NPC routines: 7 routines.
- Role NPCs: innkeeper, witness, suspicious tenant, guard/fixer, cook, and courier.
- Quest branches: 4 major routes and 8 minor branches, with Take Root and ending placeholders.
- Feats: 12 demo feats with active/passive/source metadata.
- Chimes: Force, Finesse, Endurance, Logic, Empathy, Rhetoric, Instinct, Dread, with 40 total passive lines.
- Journal/clue content: 20 journal entries and 21 clue definitions.
- Investigation/conflict: locked-room clue scene plus hallway confrontation, kitchen bargain, and courtyard standoff set-pieces.

## Next roadmap focus

The project has reached the technical MVP/P8 vertical slice. The active plan now starts from `GOAL.md` P20/P21 and then proceeds through P22–P31 toward Alpha 0.1.
