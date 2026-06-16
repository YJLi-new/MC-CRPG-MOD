# Task Plan: Minecraft Disco-like CRPG Mod

## Goal
Build a Fabric-based Minecraft Java Edition 26.1.2 CRPG mod prototype under `CRPG_MOD` that supports interactable targets, highlights, dialogue/action/thought UI, server-authoritative checks, narrative state, developer tooling, and later NPC routines.

## Current Phase
Phase 42 is complete with code, automated validation, and actual Windows GUI `llm_chat` evidence; next work should proceed to PLAN.md P43 testing, evaluation, schemas, audits, and documentation.

## Phases

### Phase 0: Project Initialization & Environment Readiness
- [x] Read project plan and current background docs.
- [x] Inspect test client `.minecraft/versions/26.1.2` structure.
- [x] Initialize file-based planning under `CRPG_MOD/.kiro/plan`.
- [x] Prepare Fabric 26.1.2 project skeleton under `CRPG_MOD`.
- [x] Resolve local build toolchain needs: JDK 25 and Gradle wrapper.
- **Status:** complete

### Phase 1: Fabric Skeleton & Data Reload Baseline
- [x] Create Gradle/Fabric Loom project structure.
- [x] Add `fabric.mod.json`, common/client entrypoints, package layout.
- [x] Register `/ebb` base command.
- [x] Add empty JSON reload registries for dialogues/block groups/entity bindings/attributes/routines.
- [x] Verify best available compile path with `scripts/gradle-local.sh --no-daemon build`.
- **Status:** complete

### Phase 2: Interaction Target MVP
- [x] Implement target records/types for entity and block-group targets.
- [x] Implement block group loading and spatial lookup/index.
- [x] Implement client crosshair target detection within 10m.
- [x] Implement server-side interaction validation: distance, target existence, line of sight.
- **Status:** complete

### Phase 3: Highlight, HUD Prompt, and Interaction Key
- [x] Add key binding, default `X` unless changed.
- [x] Render target highlight for entity bounding boxes and small block groups.
- [x] Render `按 [X] 互动` HUD prompt when within 2m and valid.
- [x] Send C2S interaction request and handle denial/open-dialogue response.
- **Status:** complete

### Phase 4: Dialogue Runtime MVP
- [x] Define dialogue JSON v0 and parser/validator.
- [x] Implement dialogue registry, runtime session, and basic branching.
- [x] Implement dialogue screen with dialogue/action/thought choice styling.
- [x] Support `/reload` updating content without crashing on invalid JSON.
- **Status:** complete

### Phase 5: Checks, Effects, and Persistence
- [x] Implement attributes and d20 skill checks server-side.
- [x] Implement roll result display, success/failure/critical branches.
- [x] Implement effects and conditions for flags/items/routine placeholders.
- [x] Persist player narrative state and world state.
- **Status:** complete

### Phase 6: Developer Mode
- [x] Add `/ebb dev` command group with OP permission checks.
- [x] Add dev snapshot payloads and basic browser screen.
- [x] Show loaded dialogues/block groups/NPC bindings/routines/validation errors/current target debug info.
- **Status:** complete

### Phase 7: NPC Entity and Routine
- [x] Add narrative NPC entity and renderer.
- [x] Integrate GeckoLib after the core interaction-dialogue loop works.
- [x] Implement basic routine schedule and look-at-player behavior.
- [x] Link dialogue effects to NPC routine changes.
- **Status:** complete for review-remediation MVP skeleton. The implementation is intentionally basic: `ebb:npc`, GeckoLib idle/walk controller/assets, `/ebb summon_npc <routine>`, typed routine JSON, stand/walk destination updates, look-at-player, and routine effect wiring for interacted Ebb NPCs.

### Phase 8: Review Remediation and Hardening
- [x] Add dedicated-server-safe block-group sync and client block-group index.
- [x] Add typed vanilla/custom entity binding parser, registry, resolver, and sample bindings.
- [x] Expand `/ebb dev` into a full dialogue tree/entity binding/NPC routine browser.
- [x] Add explicit check outcome effects and node enter effects.
- [x] Add dialogue session lifecycle cleanup, timeout, and action revalidation.
- [x] Harden packet count decoding, missing-reference validation, attribute defaults, and optional block predicates.
- [x] Improve dialogue UI paging/wait-state/end behavior and text-key support.
- [x] Verify with build, smoke test, jar hashes, and jar inspection.
- **Status:** complete

### Phase 9: Playable Client Test Profile
- [x] Create a separate Fabric 26.1.2 profile instead of modifying the vanilla `26.1.2` profile in place.
- [x] Install current Ebb mod jar plus Fabric API and GeckoLib in the profile-local `mods/` directory.
- [x] Install/verify Fabric Loader libraries and PCL metadata needed for actual launch.
- [x] Document launch and manual verification steps.
- **Status:** complete

### Phase 10: Dice Roll Playtest Fix
- [x] Make checked dialogue choices produce visible roll feedback when clicked.
- [x] Allow social checked actions to skip target revalidation while preserving default revalidation for material action choices.
- [x] Refresh the playable PCL test profile with the rebuilt jar.
- [x] Verify build and smoke checks.
- **Status:** complete

### Phase 11: DND-8 Player Attribute Points
- [x] Replace sample attributes with DND-like eight dimensions.
- [x] Preserve legacy attribute aliases for existing content.
- [x] Persist per-player unspent attribute points.
- [x] Add player commands for viewing/spending points and OP debug commands for grant/set/reset.
- [x] Update sample dialogue checks and verification docs.
- **Status:** complete

### Phase 12: Dialogue UI Roll Status Layout Fix
- [x] Move roll-result/status rendering into a bounded area above the visible choice buttons.
- [x] Derive choice buttons, status area, body scissor, and end button from one panel layout to avoid overlap at different GUI scales.
- [x] Clip long roll/status text instead of drawing underneath buttons.
- [x] Rebuild and refresh the playable PCL/Fabric test profile.
- **Status:** complete

### Phase 13: Registered Entity Target Filtering
- [x] Remove implicit fallback that treated every pickable entity as an Ebb debug interaction target.
- [x] Filter client entity crosshair detection through explicit entity bindings before creating highlights/prompts.
- [x] Deny unbound entity interaction requests server-side with `unbound_entity`.
- [x] Update verification docs to require tagged/bound entities for entity highlights.
- **Status:** complete

### Phase 14: Second Review Remediation
- [x] Read `ebb_project_review_2026-05-30_second.md` and map required fixes.
- [x] Add data-driven interaction settings with configurable debug entity fallback, disabled for bundled demo.
- [x] Add `EntityBindingSyncPayload` and sync entity bindings/settings to dedicated-server clients.
- [x] Add `EntityTargetSyncPayload` so tag-matched server entities are explicitly synced to clients for highlight/prompt prediction.
- [x] Make block-group sync limits explicit and invalidate oversized groups before sync.
- [x] Upgrade routine path handling from first-point-only to sequential waypoint progression.
- [x] Update verification docs and add JSON authoring guide / second review remediation report.
- [x] Re-run build and smoke checks.
- [ ] Execute full Windows GUI manual client retest from `26.1.2-Fabric-Ebb-Test`; pending human GUI operation.
- **Status:** code/docs complete; manual GUI retest pending

### Phase 15: Third Review Runtime Wiring Reconciliation
- [x] Read `ebb_project_review_2026-05-31_third.md` and map stale/wiring concerns.
- [x] Re-audit P0 runtime wiring for client target prediction, sync payload registration, receivers, server sync, and entrypoints.
- [x] Tighten entity prediction to respect matched highlight range and clear client sync state on join/disconnect.
- [x] Add tracked static audit script for third-review wiring requirements.
- [x] Add requirement-by-requirement completion audit for third-review items.
- [x] Verify Google Drive mirrored source tree against third-review static audit and critical-file hashes.
- [x] Re-run static audit, Gradle build, smoke checks, server smoke, jar inspection, and test-profile refresh.
- [ ] Execute full Windows GUI manual retest from `26.1.2-Fabric-Ebb-Test`; pending human GUI operation.
- **Status:** code/docs/build/source-audit complete; manual GUI retest pending


### Phase 16: Deep Research Report Implementation
- [x] Read `C:\Users\lanla\Downloads\deep-research-report (2).md` and map actionable items to the existing Fabric 26.1.2/JDK25 project instead of migrating to the report's suggested 1.21.1/JDK21 mainline.
- [x] Add stable architecture contracts under `com.crpg.ebb.api` for interactable targets, dialogue runtime/repository, roll service, validation, and reload reports.
- [x] Extend dialogue schema/runtime for node types, node `next`, authoring aliases, retryable/one-shot roll modes, advantage/static modifiers, failure-forward validation, single-use checks, richer conditions/effects, variables, traits, thoughts, and unlock tags.
- [x] Extend block/entity authoring compatibility with `boxes`, `anchor`, namespace defaults, and Minecraft-default entity type parsing.
- [x] Implement OP developer commands for dev on/off, dialogue inspect/tree/vars/reload, routine inspect, and save-debug export.
- [x] Add author-friendly YAML/JSON examples, compiler script, Gradle validation entrypoint, smoke runner, and GitHub Actions build baseline.
- [x] Add tracked JUnit and Fabric GameTest coverage for the report's automated-test strategy, including bundled data, failure-forward validation, narrative effects, generated authoring data, NPC spawn/routine state, and tagged NPC binding resolution.
- [x] Close continuation-audit gaps: scrollable dialogue history log, conversation-focus routine pause, line-of-sight-aware NPC look policy, and saved-data schema versioning.
- [x] Repeatedly review changed code, fix discovered parsing/validation bugs, and verify with Gradle build, authoring compile, smoke checks, static audits, JUnit, Fabric GameTest, and jar inspection.
- **Status:** complete

### Phase 17: GOAL.md Playable Vertical Slice Expansion
- [x] Read `C:\Users\lanla\Downloads\GOAL.md` and adopt it as the active execution target while preserving Fabric 26.1.2 / Java 25 / GeckoLib 5.5.1 and existing data-driven architecture.
- [x] Re-establish P0/P1 baseline with build, smoke, data validation, static audits, and GameTest verification.
- [x] Implement P2 Story Variables: Branch/Major/Minor layered persistence, dialogue effects, dialogue conditions, developer inspection, JSON authoring docs, demo innkeeper content, smoke/JUnit/static audit coverage.
- [x] Implement P3 Quest Branch / Take Root / Feat definitions, state, dialogue effects/conditions, basic Quest Tree UI, dev views, docs, demo content, and tests.
- [x] Implement P4 Chime / Inner Voice / Passive Inserts registry, resolver, dialogue passive insert hook, UI styling, demo content, docs, and tests.
- [x] Implement P5 Journal / Quest Tree UX.
- [x] Implement P6 relationship/memory/routine expansion.
- [x] Implement P7 investigation and dialogue-set-piece conflict systems.
- [x] Implement P8 playable tavern vertical slice with 4 NPCs, 8 interactable points, 2 major branches, 4 feats, 4 chimes, 1 conflict scene, and ending placeholder evidence.
- **Status:** code/data/docs/automated verification complete for P2-P8; full Windows GUI retest pending human operation


### Phase 18: GUI Retest Issue Hotfix
- [x] Diagnose screenshots from `新的世界 (1)`: stale installed jar, legacy role NPC tags, and player-facing command permission/suggestion issue.
- [x] Make `/ebb dialogue vars` accessible for player self-inspection and add `/ebb vars` alias.
- [x] Make `/ebb summon_npc <routine>` resolve bundled bare routine names to `ebb:demo/<routine>` when present.
- [x] Extend role NPC bindings to match both new demo tags and legacy `ebb.npc.<role>_day` tags already in the save.
- [x] Rebuild, run smoke/data/GameTest verification, inspect packaged jar resources, and refresh the separate Fabric test profile.
- [x] Add and run a GUI retest issue audit that checks command accessibility, INIT sync clear, role bindings, installed jar, and `新的世界 (1)` save evidence.
- [x] Add JUnit/GameTest regressions for command nodes, distinct legacy role-NPC bindings, and all eight block-group targets.
- [x] Add a profile runtime-log checker to prove whether the Windows client has relaunched into the refreshed jar/data.
- **Status:** code/profile/audit/test complete; latest runtime log still stale until Windows client relaunches


### Phase 19: Mineflayer/MineDojo Windows GUI Automation
- [x] Add local Node mineflayer/minecraft-protocol/minecraft-data automation package.
- [x] Add 26.1.2 high-version protocol/data alias adapter preserving protocol 775 and dataVersion 4790.
- [x] Add MineDojo-compatible Python `EbbGuiEnv` orchestration layer.
- [x] Add Windows Python focus/input/screenshot helper and image signal assertions.
- [x] Add GUI E2E runner with runtime_check, bot_probe, dry_run, and gui_retest scenarios.
- [x] Add dependency installer, GUI automation smoke runner, and documentation.
- [x] Verify dry-run, Node self-test, GUI automation smoke, existing GUI issue audit, and runtime stale-log detection.
- [x] Execute full Windows GUI visual pass after client relaunch.
- **Status:** complete

### Phase 20: Architecture Plan P20/P21 Documentation and Baseline Health
- [x] Read `C:\Users\lanla\Downloads\minecraft_disco_crpg_mod_goal_architecture_plan.md` and copy it into repository root as `GOAL.md`.
- [x] Add repository-level onboarding docs: `README.md`, `AGENTS.md`, `docs/architecture.md`, `docs/current_status.md`, and `docs/status_reconciliation_2026-06-02.md`.
- [x] Reconcile historical GUI-pending docs with the final automated GUI evidence without deleting old audit context.
- [x] Extend `scripts/goal_static_audit.py` with P20/P21 guardrails: docs existence, version pins, required data directories, current artifact hashes, failure-forward checked-choice lint, and major Take-Root consequence checks.
- [x] Run initial baseline `scripts/gradle-local.sh --no-daemon build` and `validateEbbData` after reading the new GOAL track.
- [x] Run full P21 verification suite after documentation/audit changes.
- **Status:** complete

### Phase 21: Architecture Plan P22 Interaction and Highlight Polish
- [x] Add data-driven `HighlightStyle` for block groups and entity bindings with colors, opacity, render mode, and visual priority.
- [x] Sync highlight styles through block-group, entity-binding, and registered entity-target payloads.
- [x] Render merged adjacent block-group outlines and support `outline` / `merged` / `bounds` render modes.
- [x] Add F3-only Ebb target prediction reason/style overlay for no-target/unbound/too-far/binding-hit diagnostics.
- [x] Add demo authoring data and docs for `highlight` style fields.
- [x] Extend `scripts/goal_static_audit.py` with P22 guardrails.
- [x] Build, validate data, smoke check, and GameTest after P22 code changes.
- [x] Close/relaunch the Windows test client and run GUI visual proof against the refreshed P22 jar.
- **Status:** complete


### Phase 22: Architecture Plan P23 Dialogue UI and Reading Rhythm Upgrade
- [x] Re-audit current `DialogueScreen` history, status, choice layout, style rendering, localization fallback, and keyboard input surfaces.
- [x] Improve scrollable history log with clear current node focus.
- [x] Ensure status/roll/chime/clue/quest echo area never overlaps choices at multiple GUI scales.
- [x] Add/verify distinct visual treatment for spoken dialogue, action, thought, chime passive inserts, roll results, and take-root moments.
- [x] Add optional hidden-DC / hidden-roll display modes in data if missing.
- [x] Add feasible player-facing font scale/text speed controls or document deferral with settings contract.
- [x] Add keyboard navigation for choices.
- [x] Add localization fallback for missing `text_key` translations.
- [x] Verify with build/data/static/JUnit/GameTest/smoke and GUI route proof.
- **Status:** complete


### Phase 23: Architecture Plan P24 Authoring and Validation Hardening
- [x] Expand `docs/json_authoring_guide.md` with a complete reference table for all conditions/effects.
- [x] Add JSON Schema files if not present, or generate schema docs from parsers.
- [x] Extend `compile_authoring_sources.py` to emit line/file diagnostics for bad YAML/JSON.
- [x] Add a failure-forward lint rule: high-stakes checked choices require failure branch/effects.
- [x] Add reference validation for dialogue IDs, node IDs, quest IDs, feat IDs, chime IDs, journal/clue IDs, routine IDs, and relationship IDs.
- [x] Add example authoring pack under `authoring/examples/tavern_case/`.
- **Status:** complete


### Phase 24: Architecture Plan P25 Quest Tree / Take Root / Feat Maturation
- [x] Upgrade Quest Tree UI from basic list/tree into a more legible branch map.
- [x] Make Major vs Minor branch distinction visible.
- [x] Show Take Root as a special moment with text, color, and granted feat summary.
- [x] Improve feat loadout UI: unlocked, active, passive, source quest, modifiers.
- [x] Add conflict/quest/history filters to Journal and Quest screens.
- [x] Add tests ensuring major branches cannot Take Root twice.
- **Status:** complete


### Phase 25: Architecture Plan P26 Chime / Inner Voice Expansion
- [x] Expand current four Chimes into a clearer initial set of 8 attribute voices.
- [x] Give each Chime a tone guide and trigger tags.
- [x] Add one passive and one active thought route per Chime in demo content.
- [x] Add cooldown/one-shot tuning so Chimes do not spam repeated nodes.
- [x] Add dev view listing why a Chime did or did not trigger.
- **Status:** complete


### Phase 26: Architecture Plan P27 NPC Art, Animation, and Routine Production
- [x] Create or document temporary humanoid GeckoLib model/texture assets.
- [x] Add role-specific visual skins for innkeeper, witness, tenant, and guard.
- [x] Add routine action validation for invalid path/pose/animation names.
- [x] Add routine debug overlay or command output showing current step/action/target.
- [x] Add conversation animation hooks: talk, think, dismiss, nervous idle.
- [x] Ensure active dialogues pause routine and restore it cleanly after close/timeout.
- **Status:** complete


### Phase 27: Architecture Plan P28 Investigation and Set-piece Conflict Expansion
- [x] Formalize conflict phases: setup, pressure, turn, consequence, resolution.
- [x] Add conflict UI status: stress, resolve, known leverage/clues.
- [x] Let clues unlock options and modify DCs in conflict.
- [x] Add at least two failure-forward conflict outcomes.
- [x] Add one non-violent and one messy resolution path.
- [x] Add tests for conflict score persistence and fail-forward paths.
- **Status:** complete


### Phase 28: Architecture Plan P29 Save/load, Multiplayer, and Permissions Hardening
- [x] Add explicit saved-data migration tests for schema version increments.
- [x] Verify new worlds and old worlds load without data loss.
- [x] Add multiplayer session handling tests: two players talking to different NPCs, same NPC contention, disconnect mid-dialogue.
- [x] Ensure OP-only commands are permission-gated; player self-inspection commands remain player-safe.
- [x] Audit all server receivers for spoofing and stale target/session IDs.
- [x] Add diagnostics for missing client mod on dedicated server if applicable.
- **Status:** complete


### Phase 29: Architecture Plan P30 Vertical Slice Content Expansion
- [x] Expand the tavern case with 3 acts: discovery, pressure/investigation, confrontation/ending.
- [x] Add at least 12 block-group investigation points.
- [x] Add at least 6 NPCs or 4 NPCs with much deeper reactivity.
- [x] Add at least 4 major branches and 8 minor branches.
- [x] Add at least 12 feats.
- [x] Add at least 8 Chimes and 40 Chime lines.
- [x] Add at least 20 journal/clue entries.
- [x] Add at least 3 set-piece conflicts.
- [x] Ensure every major route has an ending placeholder or concrete ending.
- **Status:** complete


### Phase 30: Architecture Plan P31 Release Packaging and Player Documentation
- [x] Create installation docs for client and dedicated server.
- [x] Document Fabric API and GeckoLib dependency requirements.
- [x] Create known-compatible test profile instructions.
- [x] Add Modrinth/CurseForge metadata draft if releasing publicly.
- [x] Add data authoring tutorial for custom story packs.
- [x] Add a changelog.
- [x] Add license clarity for code, data, and assets.
- **Status:** complete


### Phase 31: Dialogue Wait-State and GUI Automation Hotfix
- [x] Diagnose the screenshot where DialogueScreen stayed on `等待服务器……` after choosing an option.
- [x] Add server-side dialogue choice exception handling so failures close/ack the client instead of leaving it waiting forever.
- [x] Add client-side dialogue choice timeout recovery and translations.
- [x] Prevent `configure_pcl_test_client.sh` from overwriting the profile-local mod jar while the matching Minecraft JVM is running.
- [x] Harden runtime log checks for stale classloader/ZIP errors.
- [x] Fix GUI automation setup/viewpoints for guestbook and stable mud interactables, and make visual checks require an actual dialogue screen.
- [x] Run Windows GUI retest against `新的世界 (1)` with 0 failures.
- **Status:** complete


### Phase 32: K-key Ebb Menu and Live Dialogue Background
- [x] Add a mod-specific Ebb menu screen opened with the default `K` key.
- [x] Provide player-safe menu actions for Journal, Quest Tree, Attributes, Dialogue Vars, and dialogue reading settings.
- [x] Remove the full-screen dark background from interaction dialogues so the live player view remains visible outside the translucent dialogue panel.
- [x] Add static audit and GUI automation guardrails for the K menu and live-background regression.
- [x] Rebuild, refresh the separate Fabric test profile, and capture Windows GUI proof.
- **Status:** complete


### Phase 33: Codebase Review Remediation — Security, Rules Semantics, Raycast Consistency, and Authoring Hardening
- [x] Fix `/ebb dialogue vars <player>` permission leakage and add permission regression coverage.
- [x] Add active-feat condition semantics and tests.
- [x] Unify client/server/dev raycast policy and add interaction consistency coverage.
- [x] Add disadvantage and roll breakdown support to d20 checks.
- [x] Harden checked-choice success-forward / pre-effect authoring validation.
- [x] Harden block-group targeting and duplicate membership behavior.
- [x] Integrate retryable / white-check unlock semantics.
- [x] Clarify or implement item/routine animation semantics and validation.
- [x] Split large command registration into command-group classes without changing command surface.
- [x] Repeatedly review code after each phase and complete full validation suite.
- **Status:** complete

## Key Questions
1. Version pins? Resolved: Minecraft `26.1.2`, Fabric Loader `0.19.2`, Fabric API `0.150.0+26.1.2`, Loom `1.17.0-alpha.13`, GeckoLib `5.5.1`; no Yarn dependency line for 26.1+.
2. Initial MVP target scope? Resolved: use vanilla entity/block targets first; custom GeckoLib NPCs/routines come later.
3. Test client Fabric setup? Resolved: when actual client testing begins, create/use a separate Fabric 26.1.2 profile rather than modifying the vanilla `26.1.2` profile in place.
4. Build toolchain? Resolved: use project-local JDK 25 and Gradle under `CRPG_MOD/.tools`.
5. Naming? Resolved for now as temporary: `mod_id=ebb`, package `com.crpg.ebb`, display name `Esoteric Ebb CRPG`.
6. Default interaction key? Resolved: `X`.
7. GeckoLib dependency policy? Resolved: GeckoLib is an accepted hard dependency from the beginning.
8. First playable content sample? Resolved: use the plan's “旅馆走廊” MVP scene.

## Decisions Made
| Decision | Rationale |
|----------|-----------|
| Keep all generated project files under `CRPG_MOD` unless performing actual client tests. | Explicit user constraint; avoids modifying the broader client unexpectedly. |
| Initialize planning under `CRPG_MOD/.kiro/plan` rather than repository root `.kiro`. | Keeps planning output inside the required output directory. |
| Main implementation route is Fabric + Minecraft Java Edition 26.1.2 + Java + JDK 25. | Matches the project plan and inspected test client version. |
| Delay human NPC/GeckoLib work until the interaction-dialogue loop is functional. | Reduces risk; aligns with project plan's recommended development order. |
| Use vanilla entity/block targets for initial MVP before custom GeckoLib NPCs. | User confirmed; keeps the first playable loop focused on interaction/dialogue/checks before animation/routine complexity. |
| Fabric-enable testing with a separate Fabric 26.1.2 profile. | User confirmed; preserves the vanilla `26.1.2` profile as a clean baseline. |
| Treat `mod_id=ebb`, package `com.crpg.ebb`, and display name `Esoteric Ebb CRPG` as temporary names. | User confirmed naming is temporary, so code should avoid unnecessary hard-coding outside normal mod-id constants. |
| Use `X` as the default interaction key. | User confirmed. |
| Keep GeckoLib as a hard dependency from the start. | User confirmed; aligns with later human NPC animation goals. |
| Use the “旅馆走廊” scene as the first playable MVP content sample. | User confirmed; provides concrete targets: innkeeper, locked door, ledger, silent cat. |

## Errors Encountered
| Error | Attempt | Resolution |
|-------|---------|------------|
| Adoptium `.sha256.txt` URL downloaded the tarball content rather than a formatted checksum file. | 1 | Switched installer to Adoptium assets JSON and verified against `binary.package.checksum`. |
| Fabric Loom failed with `RepositoriesMode.FAIL_ON_PROJECT_REPOS` because Loom adds local repositories. | 1 | Changed settings repository mode to `RepositoriesMode.PREFER_PROJECT`. |
| Gradle wrapper URL validation timed out against `services.gradle.org`. | 1 | Generated wrapper with `--no-validate-url`, then set wrapper timeout/retries higher. |
| `scripts/gradle-local.sh` initially delegated to `./gradlew`, causing a wrapper distribution download timeout. | 1 | Changed helper to invoke the installed local Gradle distribution directly. |
| `ChunkPos` constructor mismatch: `new ChunkPos(BlockPos)` is invalid in 26.1.2. | 1 | Inspected `ChunkPos`; changed chunk key construction to `(block.getX() >> 4, block.getZ() >> 4)`. |
| `GameProfile.getName()` is absent in the 26.1.2 dependency set. | 1 | Switched debug logging to `player.getName().getString()`. |
| Fabric key mapping API package is `net.fabricmc.fabric.api.client.keymapping.v1`, not `keybinding.v1`. | 1 | Corrected the `KeyMappingHelper` import after inspecting the local Fabric API jar. |
| Dialogue parser smoke test initially accepted an invalid empty `nodes` object without a validation message. | 1 | Added explicit `nodes must contain at least one valid node` validation and re-ran the smoke test successfully. |

## Notes
- Re-read this plan and `progress.md` before major implementation decisions.
- Log discoveries in `findings.md`, especially after file searches or external documentation checks.
- User's path convention: Windows `E:\MC\SIMMC2_1-21-8\CRPG_MOD`, WSL `/mnt/e/MC/SIMMC2_1-21-8/CRPG_MOD`.

### Phase 34: PLAN.md P34 — LLM / Memory Specs and Fake Provider Foundation
- [x] Read `E:\MC\PCL\PLAN.md` end-to-end and map P34-P43 requirements without narrowing scope.
- [x] Add server-side LLM config with disabled/fake/gateway modes and no API secrets in jar/resources.
- [x] Add fake LLM gateway client, chat request/response/session/service, timeout/cancel/error-safe lifecycle.
- [x] Add LLM chat network payloads and client `NpcChatScreen` skeleton.
- [x] Add `/ebb llm status` and static audit proving disabled/fake mode does not access network.
- [x] Integrate scripted `llm_chat` / `free_chat` choice type without weakening deterministic dialogue authority.
- [x] Add tests/GameTest/smoke/static/docs for fake provider, disabled mode, timeout close, and API-key guardrails.
- [x] Run build, validateEbbData, smoke checks, GameTest, and diff checks.
- **Status:** complete


### Phase 35: PLAN.md P35 — NPC Profile / Tier / Promotion Data Layer
- [x] Add `NpcTier`, `NpcProfileDefinition`, parser/reload registry, and safe dev status surfaces.
- [x] Add six P30 role NPC profile JSON definitions and profile authoring docs/schema.
- [x] Extend entity binding schema for minor-generatable NPC candidates without re-enabling debug fallback.
- [x] Add promotion service and persisted promoted profile state.
- [x] Add tests/GameTest/static/smoke coverage proving scripted profiles load and promoted profiles persist.
- **Status:** complete


### Phase 36: PLAN.md P36 — Gateway Minimal Service + OAuth/OIDC Authentication
- [x] Add `ebb-llm-gateway/` service skeleton.
- [x] Implement `/v1/health`, `/v1/auth/device/start`, and `/v1/auth/device/status`.
- [x] Implement dev-only local auth provider and production OAuth/OIDC abstraction without storing secrets in the mod jar.
- [x] Add Minecraft `/ebb llm auth/status/logout` flow with server-only token storage.
- [x] Add tests/docs/static audits proving unauthenticated chat returns auth-required, login enables fake-provider chat, logout invalidates token, and profile refresh does not leak tokens.
- **Status:** complete


### Phase 37: PLAN.md P37 — OpenAI Responses API Integration
- [x] Add gateway `/v1/chat/message`.
- [x] Use an official OpenAI SDK path in the gateway while keeping tests mocked by default.
- [x] Support structured JSON output, streaming/chunked response, timeout/circuit breaker, model config, and `store:false` default privacy.
- [x] Connect Minecraft gateway mode to the real/fake gateway chat endpoint without blocking UI or leaking secrets.
- [x] Add tests/docs/static audits proving fake/real provider switching and graceful failure.
- **Status:** complete


### Phase 38: PLAN.md P38 — MemoryStore MVP
- [x] Add gateway DB migration.
- [x] Add append-only `MemoryRecord`, `MemoryFact`, and `MemoryConflict`.
- [x] Add embeddings write path and hybrid retrieval: recent + vector + keyword + entity + time.
- [x] Add Minecraft dev commands `/ebb memory search/inspect/conflicts`.
- [x] Add tests proving two-turn memory retrieval, supersede/conflict behavior, and citation ids.
- **Status:** complete


### Phase 39: PLAN.md P39 — Memory extraction / consolidation
- [x] Add LLM extractor that proposes memory operations.
- [x] Add deterministic validator applying memory operations.
- [x] Add background summarizer for episodic summaries and related-memory links.
- [x] Add A-Mem-like evolution while preserving raw episodes.
- [x] Add A-MemGuard-like safety lessons and dev UI for raw episodes/facts/conflicts.
- [x] Run final build/static/smoke/GameTest validation and artifact hash update.
- **Status:** complete


### Phase 40: PLAN.md P40 — NPC Knowledge Base and story effects
- [x] Add `NpcKnowledgePackDefinition` parser.
- [x] Add `NpcKnowledgeRegistry` reload.
- [x] Add KB chunk embedding/indexing.
- [x] Support `reveal_conditions` using existing `DialogueCondition`.
- [x] Add effects: `npc_kb_add_fact`, `npc_kb_add_pack`, and `npc_stance_shift`.
- [x] Make prompt assembler retrieve only visible KB.
- [x] Add acceptance tests: hidden secret before clue, changed answer after clue, `/ebb kb inspect <npc>` visibility.
- **Status:** complete


### Phase 41: PLAN.md P41 — Minor NPC instant generation
- [x] Add minor candidate detector.
- [x] Add `NpcProfileGenerator` prompt/schema.
- [x] Generate character, stance, knowledge seed, and suggested options.
- [x] Persist promoted major profile.
- [x] Add generated profile dev review.
- [x] Add per-world-hour promotion rate limit.
- [x] Add acceptance tests for tagged minor chat/profile persistence/review/regenerate surfaces.
- [x] Run final static/smoke/build validation and artifact hash update.
- **Status:** complete


### Phase 42: PLAN.md P42 — LLM Chat UI Completion
- [x] Add streaming text packet emission and client-side chunk merging.
- [x] Keep suggested options clickable and cover them in GUI E2E.
- [x] Add server-authoritative Return to Script from LLM chat back to a scripted dialogue node.
- [x] Add Memory Correction button/path for player-authored memory corrections.
- [x] Add dev citations overlay instead of inline citations.
- [x] Add client timeout/cancel/error non-stuck behavior.
- [x] Expose LLM auth status/login/logout actions through the K menu without syncing tokens.
- [x] Add P42 GUI E2E scenario manifest/route and automated audit coverage.
- [x] Run final JUnit/GameTest/static/smoke/build validation and artifact hash update.
- [x] Execute actual Windows GUI `llm_chat` scenario against `26.1.2-Fabric-Ebb-Test`; report `build/gui-e2e/llm-chat-report.json` has no failed steps and screenshots prove live-background LLM chat, citations overlay, suggested-option click, and return-to-script.
- **Status:** complete



### Phase 43: PLAN.md P43 — Testing, Evaluation, and Documentation
- [ ] Expand JSON authoring guide for NPC profiles, NPC knowledge/Kb, LLM config, and memory effects.
- [ ] Add/verify schemas `ebb.npc_profile.schema.json` and `ebb.npc_knowledge.schema.json`.
- [ ] Add static audits for API-key literals, fake LLM provider in tests, hidden knowledge not in client sync payloads, and high-risk effects not accepted directly from LLM output.
- [ ] Add JUnit coverage for memory conflict, promotion persistence, and prompt pack assembly.
- [ ] Add GameTest coverage for fake provider chat, minor promotion, and relationship delta.
- [ ] Add GUI E2E coverage for auth disabled, fake chat, and real gateway dry-run.
- **Status:** pending
