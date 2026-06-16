# Progress Log

## Session: 2026-05-30

### Phase 0: Project Initialization & Environment Readiness
- **Status:** complete
- **Started:** 2026-05-30 Asia/Shanghai
- Actions taken:
  - Read the project plan and summarized the intended mod architecture.
  - Inspected the 26.1.2 test client directory and PCL metadata.
  - Created `CRPG_MOD/docs/assistant_preflight_notes.md` with project background and environment findings.
  - Initialized `planning-with-files` under `CRPG_MOD/.kiro`.
  - Replaced generic planning templates with project-specific task plan, findings, and progress log.
  - Installed project-local Eclipse Temurin JDK 25.0.3 and Gradle 9.5.1 under `.tools/`.
  - Created environment helper scripts under `scripts/`.
  - Created a Fabric Loom 26.1.2 Gradle project skeleton with Fabric API and GeckoLib dependencies.
  - Generated Gradle wrapper files and adjusted wrapper timeout/retry settings.
  - Verified the project builds successfully via `scripts/gradle-local.sh --no-daemon build`.
- Files created/modified:
  - `docs/assistant_preflight_notes.md` (created during preflight)
  - `.kiro/plan/task_plan.md` (created/initialized)
  - `.kiro/plan/findings.md` (created/initialized)
  - `.kiro/plan/progress.md` (created/initialized)
  - `.kiro/steering/planning-context.md` (created by bootstrap)
  - `.tools/jdk-25/` (installed)
  - `.tools/gradle-9.5.1/` (installed)
  - `.tools/downloads/` (downloaded verified tool archives)
  - `scripts/install_env.sh` (created)
  - `scripts/env.sh` (created)
  - `scripts/env.ps1` (created)
  - `scripts/gradle-local.sh` (created)
  - `settings.gradle` (created)
  - `build.gradle` (created)
  - `gradle.properties` (created)
  - `gradlew`, `gradlew.bat`, `gradle/wrapper/*` (created)
  - `src/main/java/com/crpg/ebb/EbbMod.java` (created)
  - `src/client/java/com/crpg/ebb/client/EbbClient.java` (created)
  - `src/main/resources/fabric.mod.json` (created)
  - `src/main/resources/assets/ebb/lang/en_us.json` and `zh_cn.json` (created)
  - `docs/environment_setup.md` (created)
  - `build/libs/ebb-0.1.0-dev.jar` and sources jar (created by build)

## Test Results
| Test | Input | Expected | Actual | Status |
|------|-------|----------|--------|--------|
| Planning bootstrap | Run bootstrap script from `CRPG_MOD` | `.kiro/plan` and `.kiro/steering` are created under `CRPG_MOD` | Files created successfully | ✓ |
| JDK install | `.tools/jdk-25/bin/java -version` and `javac -version` | Java/Javac 25 | Temurin 25.0.3 and javac 25.0.3 | ✓ |
| Gradle install | `source scripts/env.sh && gradle -v` | Gradle runs on JDK 25 | Gradle 9.5.1, Launcher JVM 25.0.3 | ✓ |
| Fabric build | `scripts/gradle-local.sh --no-daemon build` | Build succeeds | BUILD SUCCESSFUL; jar produced in `build/libs/` | ✓ |
| Dependency check | `dependencies --configuration runtimeClasspath` | Fabric Loader/API and GeckoLib present | Loader 0.19.2, Fabric API 0.150.0+26.1.2, GeckoLib 5.5.1 present | ✓ |

## Error Log
| Timestamp | Error | Attempt | Resolution |
|-----------|-------|---------|------------|
| 2026-05-30 | Adoptium checksum URL content was not formatted for `sha256sum -c` due wrong URL handling. | 1 | Installer switched to Adoptium assets JSON checksum. |
| 2026-05-30 | Loom plugin failed with `RepositoriesMode.FAIL_ON_PROJECT_REPOS`. | 1 | Changed settings repository mode to `PREFER_PROJECT`. |
| 2026-05-30 | Gradle wrapper validation timed out against distribution URL. | 1 | Re-ran wrapper generation with `--no-validate-url` and increased wrapper timeout/retries. |
| 2026-05-30 | `scripts/gradle-local.sh --version` initially attempted wrapper download and timed out. | 1 | Helper now invokes `.tools/gradle-9.5.1/bin/gradle` directly. |

## 5-Question Reboot Check
| Question | Answer |
|----------|--------|
| Where am I? | Phase 1: Fabric Skeleton & Data Reload Baseline is now in progress after completing Phase 0. |
| Where am I going? | Next: add `/ebb` command and JSON reload registries, then start interaction target MVP. |
| What's the goal? | Build a Fabric 26.1.2 Disco-like CRPG mod prototype under `CRPG_MOD`. |
| What have I learned? | Local WSL toolchain is now installed under `CRPG_MOD`; test client remains vanilla and untouched; see `findings.md`. |
| What have I done? | Installed local JDK/Gradle, created Fabric Loom skeleton, resolved dependencies, and verified a successful build. |

---
*Update after completing each phase or encountering errors.*

### Phase 1: Fabric Skeleton & Data Reload Baseline
- **Status:** in_progress
- **Started:** 2026-05-30 Asia/Shanghai
- Actions taken:
  - Created Gradle/Fabric Loom project structure and minimal common/client entrypoints.
  - Verified compile/build path with project-local JDK 25 and Gradle 9.5.1.
- Remaining:
  - Register `/ebb` base command.
  - Add empty JSON reload registries for dialogues, block groups, entity bindings, attributes, and routines.
- Files created/modified:
  - `settings.gradle`, `build.gradle`, `gradle.properties`, `src/**`, `scripts/**`, `docs/environment_setup.md`.


### Confirmations Recorded
- **Status:** complete
- **Time:** 2026-05-30 Asia/Shanghai
- Actions taken:
  - Recorded six user confirmations into `task_plan.md` and `findings.md`.
  - Updated key questions from open items into resolved decisions.
- Files created/modified:
  - `.kiro/plan/task_plan.md`
  - `.kiro/plan/findings.md`
  - `.kiro/plan/progress.md`


### Phase 1 Completion Update
- **Status:** complete
- **Time:** 2026-05-30 Asia/Shanghai
- Actions taken:
  - Added `/ebb`, `/ebb status`, and `/ebb data` command registration.
  - Added server-data JSON reload listeners for dialogues, block groups, entity bindings, attributes, and NPC routines.
  - Verified build success with `scripts/gradle-local.sh --no-daemon build`.
  - Inspected jar contents for expected project classes and resources.
- Files created/modified:
  - `src/main/java/com/crpg/ebb/EbbMod.java`
  - `src/main/java/com/crpg/ebb/registry/ModCommands.java`
  - `src/main/java/com/crpg/ebb/data/JsonDataRegistry.java`
  - `src/main/java/com/crpg/ebb/data/NarrativeDataRegistries.java`
  - `.kiro/plan/task_plan.md`
  - `.kiro/plan/findings.md`
  - `.kiro/plan/progress.md`
- Verification:
  - `scripts/gradle-local.sh --no-daemon build` → BUILD SUCCESSFUL.
  - `jar tf build/libs/ebb-0.1.0-dev.jar` includes `ModCommands.class`, `JsonDataRegistry.class`, `NarrativeDataRegistries.class`, `EbbMod.class`, and `fabric.mod.json`.

### Phase 2: Interaction Target MVP
- **Status:** in_progress
- **Started:** 2026-05-30 Asia/Shanghai
- Next actions:
  - Add target records/types for entity and block-group targets.
  - Add typed block-group definition loading and index.
  - Then add client detection and server validation.


### Phase 2 Foundation Update
- **Status:** in_progress
- **Time:** 2026-05-30 Asia/Shanghai
- Actions taken:
  - Added target model records/types for vanilla entity and block-group targets.
  - Added block-group JSON parser and spatial indexes by id, by block, and by chunk.
  - Wired block-group index rebuilding to the block-group reload registry.
  - Verified build success with `scripts/gradle-local.sh --no-daemon build`.
- Files created/modified:
  - `src/main/java/com/crpg/ebb/interaction/InteractionTargetType.java`
  - `src/main/java/com/crpg/ebb/interaction/InteractionTarget.java`
  - `src/main/java/com/crpg/ebb/interaction/EntityTarget.java`
  - `src/main/java/com/crpg/ebb/interaction/BlockGroupTarget.java`
  - `src/main/java/com/crpg/ebb/interaction/BlockGroupDefinition.java`
  - `src/main/java/com/crpg/ebb/interaction/BlockGroupIndex.java`
  - `src/main/java/com/crpg/ebb/data/JsonDataRegistry.java`
  - `src/main/java/com/crpg/ebb/data/NarrativeDataRegistries.java`
- Verification:
  - First build found a `ChunkPos` constructor mismatch; fixed after API inspection.
  - Second `scripts/gradle-local.sh --no-daemon build` → BUILD SUCCESSFUL.


### Phase 2 Completion Update
- **Status:** complete
- **Time:** 2026-05-30 Asia/Shanghai
- Actions taken:
  - Added client-side crosshair detection state and detector, registered from `EbbClient`.
  - Added server-side interaction validation for block-group and entity targets.
  - Verified build success with `scripts/gradle-local.sh --no-daemon build`.
  - Inspected jar contents for Phase 2 classes.
- Files created/modified:
  - `src/client/java/com/crpg/ebb/client/EbbClient.java`
  - `src/client/java/com/crpg/ebb/client/interaction/ClientInteractionState.java`
  - `src/client/java/com/crpg/ebb/client/interaction/ClientTargetDetector.java`
  - `src/main/java/com/crpg/ebb/interaction/InteractionValidationResult.java`
  - `src/main/java/com/crpg/ebb/interaction/InteractionService.java`
  - `.kiro/plan/task_plan.md`
  - `.kiro/plan/findings.md`
  - `.kiro/plan/progress.md`
- Verification:
  - `scripts/gradle-local.sh --no-daemon build` → BUILD SUCCESSFUL.
  - `jar tf build/libs/ebb-0.1.0-dev.jar` includes `ClientTargetDetector.class`, `ClientInteractionState.class`, `InteractionService.class`, `InteractionValidationResult.class`, and interaction target/index classes.

### Phase 3: Highlight, HUD Prompt, and Interaction Key
- **Status:** in_progress
- **Started:** 2026-05-30 Asia/Shanghai
- Next actions:
  - Add `X` key binding.
  - Add HUD prompt rendering from `ClientInteractionState`.
  - Add highlight renderer for current entity/block-group target.
  - Add networking payload for C2S interaction request and S2C denial/open-dialogue placeholder.


### Phase 3 Completion Update
- **Status:** complete
- **Time:** 2026-05-30 Asia/Shanghai
- Actions taken:
  - Added default `X` client key binding under a custom Esoteric Ebb controls category.
  - Added HUD prompt rendering via Fabric HUD extraction API; prompt uses `按 [%s] 互动` / `Press [%s] to interact` and reads the actual keybind label.
  - Added target highlight renderer via Fabric level render events, drawing translucent outlines for entity AABBs and block-group blocks/bounds.
  - Added Fabric play networking payloads for C2S interaction requests and S2C denial/open-dialogue responses.
  - Server handler re-validates requests through `InteractionService` before accepting and opening a dialogue placeholder.
  - Client denial/open-dialogue receivers currently show overlay/system messages; full dialogue UI is next phase.
- Files created/modified:
  - `src/main/java/com/crpg/ebb/EbbMod.java`
  - `src/main/java/com/crpg/ebb/network/InteractionRequestPayload.java`
  - `src/main/java/com/crpg/ebb/network/InteractionDeniedPayload.java`
  - `src/main/java/com/crpg/ebb/network/OpenDialoguePayload.java`
  - `src/main/java/com/crpg/ebb/network/ModPackets.java`
  - `src/client/java/com/crpg/ebb/client/EbbClient.java`
  - `src/client/java/com/crpg/ebb/client/input/ClientKeyMappings.java`
  - `src/client/java/com/crpg/ebb/client/network/ClientInteractionNetworking.java`
  - `src/client/java/com/crpg/ebb/client/render/InteractionPromptHud.java`
  - `src/client/java/com/crpg/ebb/client/render/TargetHighlightRenderer.java`
  - `src/main/resources/assets/ebb/lang/en_us.json`
  - `src/main/resources/assets/ebb/lang/zh_cn.json`
  - `.kiro/plan/task_plan.md`, `.kiro/plan/findings.md`, `.kiro/plan/progress.md`
- Verification:
  - First build found `GameProfile.getName()` unavailable; fixed by using `player.getName().getString()`.
  - Second build found the key mapping helper package was `keymapping`, not `keybinding`; fixed after jar inspection.
  - Final `scripts/gradle-local.sh --no-daemon build` → BUILD SUCCESSFUL.
  - Jar inspection shows Phase 3 classes: `ClientKeyMappings`, `ClientInteractionNetworking`, `InteractionPromptHud`, `TargetHighlightRenderer`, `InteractionRequestPayload`, `InteractionDeniedPayload`, `OpenDialoguePayload`, and `ModPackets`.
  - Generated jar: `build/libs/ebb-0.1.0-dev.jar`, SHA-256 `f074fcb109ad436c0c9213f782b344b46dabf87f046d9f58865fcb1e6905649c`; sources jar SHA-256 `c89832eac516794094e5f8e36c3bfb4df9f12c96883457ffb9aecc5ea3651833`.
  - No `.minecraft` files or vanilla `26.1.2` profile files were modified.

### Phase 4: Dialogue Runtime MVP
- **Status:** in_progress
- **Started:** 2026-05-30 Asia/Shanghai
- Next actions:
  - Define dialogue JSON v0 parser/validator.
  - Add server runtime sessions and choice request payloads.
  - Replace the open-dialogue placeholder with a client dialogue/action/thought choice screen.


### Phase 4 Completion Update
- **Status:** complete
- **Time:** 2026-05-30 Asia/Shanghai
- Actions taken:
  - Added dialogue JSON v0 parser/validator: definitions, nodes, choices, choice types, and check metadata placeholders.
  - Added typed `DialogueRegistry` rebuilt from the existing raw `dialogues` reload listener; invalid dialogue JSON records validation messages instead of crashing.
  - Added server-side `DialogueService` sessions, one active conversation per player, basic branch advancement, terminal choice close, and client close cleanup.
  - Expanded play networking with open/update/close/choose/close-request payloads and visible choice DTOs.
  - Replaced the open-dialogue placeholder with `DialogueScreen`, showing speaker/text and styled dialogue/action/thought choices.
  - Added sample dialogue resources for `ebb:debug/entity` and `ebb:demo/locked_door_dialogue`.
- Files created/modified:
  - `src/main/java/com/crpg/ebb/dialogue/*`
  - `src/main/java/com/crpg/ebb/network/OpenDialoguePayload.java`
  - `src/main/java/com/crpg/ebb/network/ModPackets.java`
  - `src/main/java/com/crpg/ebb/network/dialogue/*`
  - `src/main/java/com/crpg/ebb/data/NarrativeDataRegistries.java`
  - `src/client/java/com/crpg/ebb/client/network/ClientInteractionNetworking.java`
  - `src/client/java/com/crpg/ebb/client/gui/dialogue/DialogueScreen.java`
  - `src/main/resources/data/ebb/dialogues/debug/entity.json`
  - `src/main/resources/data/ebb/dialogues/demo/locked_door_dialogue.json`
  - `src/main/resources/assets/ebb/lang/en_us.json` and `zh_cn.json`
  - `.kiro/plan/task_plan.md`, `.kiro/plan/findings.md`, `.kiro/plan/progress.md`
- Verification:
  - `scripts/gradle-local.sh --no-daemon build` → BUILD SUCCESSFUL after Phase 4 implementation.
  - Dialogue parser smoke test compiled and ran from `build/tmp/verify`: `Dialogue parser smoke test passed; valid_messages=0, invalid_messages=1`.
  - Jar inspection shows dialogue runtime/screen/payload classes and bundled sample dialogue JSON files.
  - Generated jar: `build/libs/ebb-0.1.0-dev.jar`, SHA-256 `9e49849a5c940e5af89640ef86ad4bd1a029d3c952df5ca8dde94eb864db553b`; sources jar SHA-256 `fe8c1645a61a5dbc489f0cbafd7b3e1fd4e6d0a9bb034bdc5bb59c4f1f5448d0`.
  - No `.minecraft` files or vanilla `26.1.2` profile files were modified.

### Phase 5: Checks, Effects, and Persistence
- **Status:** in_progress
- **Started:** 2026-05-30 Asia/Shanghai
- Next actions:
  - Parse typed attribute JSON and expose default player attributes.
  - Implement server-side d20 roll resolution for dialogue choices with `check`.
  - Add roll result display in the dialogue UI and narrative flags/effects foundation.
  - Add saved-data persistence for per-player/world narrative state.


### Phase 5 Completion Update
- **Status:** complete
- **Time:** 2026-05-30 Asia/Shanghai
- Actions taken:
  - Added typed attribute definitions and `AttributeRegistry`, rebuilt from `data/*/attributes/*.json`.
  - Added sample attributes `force`, `logic`, and `empathy`.
  - Added authoritative server-side d20 resolution for dialogue choices with `check`, including natural 20/1 critical branches and success/failure branches.
  - Added `RollResultPayload` and dialogue screen roll-result display.
  - Added dialogue conditions and effects for player/world flags, attribute setting, item placeholders, and routine placeholders.
  - Added `NarrativeSavedData`/`PlayerNarrativeState` codec-based saved data for player flags/attributes and world flags.
  - Updated the locked-door sample dialogue with a force check, flag effects, and a conditionally visible thought choice.
- Files created/modified:
  - `src/main/java/com/crpg/ebb/attribute/*`
  - `src/main/java/com/crpg/ebb/state/*`
  - `src/main/java/com/crpg/ebb/dialogue/DialogueCondition.java`, `DialogueEffect.java`, `DialogueScope.java`, and updates to dialogue runtime classes
  - `src/main/java/com/crpg/ebb/network/dialogue/RollResultPayload.java` and `DialogueUpdatePayload.java`
  - `src/client/java/com/crpg/ebb/client/gui/dialogue/DialogueScreen.java`
  - `src/main/resources/data/ebb/attributes/*.json`
  - `src/main/resources/data/ebb/dialogues/demo/locked_door_dialogue.json`
  - `.kiro/plan/task_plan.md`, `.kiro/plan/findings.md`, `.kiro/plan/progress.md`
- Verification:
  - `scripts/gradle-local.sh --no-daemon build` → BUILD SUCCESSFUL.
  - Phase 5 smoke test compiled and ran from `build/tmp/verify`: attribute registry rebuilt with 3 attributes, dialogue conditions/effects parsed, and `NarrativeSavedData` codec round-tripped player flags/attributes.
  - Jar inspection shows attribute/state/effect/condition/roll classes and bundled attribute JSON resources.
  - Generated jar: `build/libs/ebb-0.1.0-dev.jar`, SHA-256 `6075b78a472cd7fd89d5cc5131307a13eb90ff6f3de2a145129f59a493eefa95`; sources jar SHA-256 `dda76e8d100ed05ec4e906d0bcc5c61b1a42c1547e135e2021c2aee9757c2f47`.
  - No `.minecraft` files or vanilla `26.1.2` profile files were modified.

### Phase 6: Developer Mode
- **Status:** in_progress
- **Started:** 2026-05-30 Asia/Shanghai
- Next actions:
  - Add `/ebb dev` command group with OP permission checks.
  - Add dev snapshot payloads and a simple client browser screen.
  - Expose loaded data counts, validation messages, active sessions/state counts, and current target debug info.


### Phase 6 Completion Update
- **Status:** complete
- **Time:** 2026-05-30 Asia/Shanghai
- Actions taken:
  - Added OP-gated `/ebb dev` command using Fabric permission predicates with `PermissionLevel.GAMEMASTERS`.
  - Added `/ebb dev summary` text fallback for console/non-modded-client command sources.
  - Added `DevSnapshotPayload` and `DevSnapshotService` to collect registry summaries, typed validation messages, active dialogue sessions, narrative saved-data counts, and player count.
  - Added client `DevSnapshotScreen` with paged snapshot browsing and dynamic current-target debug info from `ClientInteractionState`.
  - Registered the dev snapshot clientbound payload and client receiver.
  - Added a bundled sample block group `ebb:demo/locked_door` pointing at `ebb:demo/locked_door_dialogue` for block-group target testing.
  - Added `docs/mvp_verification_steps.md` with build, smoke-test, jar, and separate-Fabric-profile manual verification steps.
- Files created/modified:
  - `src/main/java/com/crpg/ebb/dev/DevSnapshotService.java`
  - `src/main/java/com/crpg/ebb/network/dev/DevSnapshotPayload.java`
  - `src/client/java/com/crpg/ebb/client/gui/dev/DevSnapshotScreen.java`
  - `src/main/java/com/crpg/ebb/registry/ModCommands.java`
  - `src/main/java/com/crpg/ebb/network/ModPackets.java`
  - `src/client/java/com/crpg/ebb/client/network/ClientInteractionNetworking.java`
  - `src/main/resources/data/ebb/interactions/block_groups/demo/locked_door.json`
  - `src/main/resources/assets/ebb/lang/en_us.json` and `zh_cn.json`
  - `docs/mvp_verification_steps.md`
  - `.kiro/plan/task_plan.md`, `.kiro/plan/findings.md`, `.kiro/plan/progress.md`
- Verification:
  - Final `scripts/gradle-local.sh --no-daemon build` → BUILD SUCCESSFUL.
  - Final smoke checks: `DialogueParserSmoke` passed; `Phase5Smoke` passed.
  - Final jar inspection confirms dev snapshot classes, client key/dialogue classes, sample dialogues, sample attributes, and sample block group JSON are packaged.
  - Final generated jar: `build/libs/ebb-0.1.0-dev.jar`, SHA-256 `b17d34f7b3e215e6bc26e727327531f3d8fc6765d6be3f375f5ed054a3337086`; sources jar SHA-256 `cde33765724d8f5ecc5724e0eea936df36f1c896fd3e0f92ad0bb9b9aa62da04`.
  - No `.minecraft` files or vanilla `26.1.2` profile files were modified.

### MVP Goal Completion Evidence
- **Status:** achieved for the requested vanilla entity/block-group CRPG interaction-dialogue MVP.
- Implemented: `/ebb` base/data/status/dev commands; JSON reload registries; typed block groups/dialogues/attributes; client crosshair target detection; highlights; default `X` prompt/key; server-authoritative interaction validation; dialogue UI with dialogue/action/thought styling; JSON branching; d20 checks; effects/conditions; narrative saved-data persistence; developer inspection basics.
- Deferred by design: custom GeckoLib NPC entity/routine/animation work remains Phase 7+; GeckoLib is still declared as a hard dependency.
- Manual client testing was not performed because it would require setting up/running a separate Fabric client profile; documented steps are in `docs/mvp_verification_steps.md`.


### Review Remediation Completion Update
- **Status:** complete
- **Time:** 2026-05-30 Asia/Shanghai
- Actions taken:
  - Implemented `BlockGroupSyncPayload`, server lifecycle block-group sync, and client `ClientBlockGroupIndex`; client block-group detection no longer depends on server-data reload state.
  - Implemented typed entity bindings with UUID/tag/name/entity-type matching and sample bindings for tagged villagers and `ebb:npc`; server entity validation now opens bound dialogues instead of always using debug.
  - Expanded `/ebb dev` into a line-based full tree browser for dialogue ids, start nodes, nodes, choices, checks, conditions, effects, entity bindings, validation, and NPC routines.
  - Added check outcome effects (`success_effects`, `failure_effects`, `critical_success_effects`, `critical_failure_effects`) and node `enter_effects`; existing choice effects are now documented/treated as pre-roll attempt effects.
  - Added dialogue session lifecycle cleanup for disconnect/leave/respawn/level change/server stop, timeout pruning, and server-side ACTION target revalidation.
  - Hardened packet count decoding, dialogue missing-reference validation, attribute default clamping, and optional block predicates.
  - Improved dialogue UI with non-pausing screen, scrollable text, paged choices, pending-choice disable/wait state, explicit end/close button, and `text_key` support.
  - Implemented `ebb:npc` GeckoLib MVP skeleton, renderer/assets, `/ebb summon_npc <routine>`, typed routine JSON, basic stand/walk/look-at-player controller, and routine effect wiring.
  - Added `docs/review_remediation_2026-05-30.md` and updated `docs/mvp_verification_steps.md`.
- Verification:
  - `scripts/gradle-local.sh --no-daemon build` → BUILD SUCCESSFUL.
  - `ReviewSmoke` → passed: dialogues=3, attributes=3, block_groups=1, entity_bindings=2, npc_routines=1; also verified check outcome effects and missing-reference hard invalidation.
  - Jar SHA-256: `build/libs/ebb-0.1.0-dev.jar` = `9f6a09ceb005aea6148aaa3d8e1545212c9ca6aaca9305aba2a1b8ca3a85624e`; sources jar = `c5e3857cf88f4262890b2195b7e67822329dc0ae11dca19ad3a12cfaef101c96`.
  - Jar inspection confirmed block-group sync/client index, entity binding registry, dev tree dumper, `EbbNpcEntity`, `ModEntityTypes`, NPC routine registry, sample entity bindings, sample routine, and GeckoLib assets.
  - `scripts/gradle-local.sh --no-daemon runServer --args nogui` smoke loaded Fabric/Minecraft/GeckoLib/Ebb and reached the normal EULA gate without an Ebb mod crash.
- Notes:
  - Manual client testing still requires a separate Fabric 26.1.2 profile; the vanilla profile was not modified.
  - Standalone smoke logged one expected unknown `ebb:npc` entity type warning because it did not run the full mod initializer before registry parsing; runtime initializer registers the entity before reload listeners.

### Final Review-Remediation Verification Refresh
- **Status:** complete
- **Time:** 2026-05-30 Asia/Shanghai
- Actions taken:
  - Re-ran `scripts/gradle-local.sh --no-daemon build` after final robustness cleanup.
  - Recompiled/re-ran `ReviewSmoke` from `build/tmp/verify-src/ReviewSmoke.java` against the current runtime classpath.
  - Added minor hardening: invalid `/ebb summon_npc <routine>` identifiers now fail gracefully, and invalid/unavailable/action-target-invalid dialogue choice attempts update session activity before returning a server-authoritative status update.
- Verification:
  - `scripts/gradle-local.sh --no-daemon build` → `BUILD SUCCESSFUL in 41s`.
  - `ReviewSmoke` → `ReviewSmoke passed: dialogues=3, attributes=3, block_groups=1, entity_bindings=2, npc_routines=1, messages=1`.
  - Current jar SHA-256: `build/libs/ebb-0.1.0-dev.jar` = `9f6a09ceb005aea6148aaa3d8e1545212c9ca6aaca9305aba2a1b8ca3a85624e`; sources jar = `c5e3857cf88f4262890b2195b7e67822329dc0ae11dca19ad3a12cfaef101c96`.
  - Final `runServer --args nogui` smoke exited with status 0; Fabric loaded Minecraft `26.1.2`, Fabric Loader `0.19.2`, Fabric API `0.150.0+26.1.2`, GeckoLib `5.5.1`, and `ebb 0.1.0-dev`; `EbbMod` initialized and the dev server stopped at the normal EULA gate.
  - `git diff --check` returned no whitespace/error output.

### Playable PCL Test Client Configuration
- **Status:** complete
- **Time:** 2026-05-30 Asia/Shanghai
- Actions taken:
  - Created a separate PCL/Fabric test profile at `/mnt/e/MC/PCL/.minecraft/versions/26.1.2-Fabric-Ebb-Test`.
  - Preserved the vanilla `/mnt/e/MC/PCL/.minecraft/versions/26.1.2` profile; no in-place vanilla profile modification was made.
  - Added `scripts/configure_pcl_test_client.sh` to regenerate/update the dedicated test profile after future builds.
  - Generated a full PCL-style Fabric version JSON using Fabric Loader `0.19.2` for Minecraft `26.1.2` with main class `net.fabricmc.loader.impl.launch.knot.KnotClient`.
  - Installed profile-local mods: `ebb-0.1.0-dev.jar`, `fabric-api-0.150.0+26.1.2.jar`, and `geckolib-fabric-26.1.2-5.5.1.jar`.
  - Copied required Fabric loader libraries into the PCL `.minecraft/libraries` cache.
  - Added profile command-history helper commands for `/ebb`, NPC summon, locked-door block setup, and villager tagging.
  - Documented the setup in `docs/client_test_profile_setup_2026-05-30.md`.
- Verification:
  - Profile JSON exists and reports `id=26.1.2-Fabric-Ebb-Test`, `clientVersion=26.1.2`, `mainClass=net.fabricmc.loader.impl.launch.knot.KnotClient`, and 114 libraries.
  - Windows-applicable missing library count is `0`.
  - Profile-local mods are present with hashes: Ebb `9f6a09ceb005aea6148aaa3d8e1545212c9ca6aaca9305aba2a1b8ca3a85624e`, Fabric API `43bdfc59a21ace202345bc4c42c751fa36b80617a61cf7b2f8c3698b806305d8`, GeckoLib `63d2519dc13e302da52911727f11ecb7b6bbecc79968751a90bf607273d5f8bc`.

### Dice Roll Interaction Fix
- **Status:** complete
- **Time:** 2026-05-30 Asia/Shanghai
- Trigger:
  - User reported that clicking the visible `[empathy] DC 12 d20` dialogue button did not trigger a random dice roll.
- Root cause / implementation notes:
  - Server-side d20 rolling existed, but all `ACTION` choices were revalidating the original target before resolving checks. For the innkeeper social check, the NPC routine/position could invalidate the target before the social roll, so the choice returned a target-invalid status instead of rolling.
  - Added choice-level `revalidate_target` support. It defaults to true for material action choices, but the innkeeper social pressure check now sets `"revalidate_target": false`, so clicking it proceeds to the authoritative server-side d20 roll.
  - Added immediate client feedback `掷骰中……<check>` while waiting for the server response, plus explicit network-unavailable feedback if the choice packet cannot be sent.
  - Added server log evidence for every dialogue roll: attribute, choice id, player, d20, modifier, total, DC, and outcome.
  - Added GeckoLib 5 resource-path copies under `assets/ebb/geckolib/models/...` and `assets/ebb/geckolib/animations/...` to address the test-client log spam about missing `ebb:entity/npc` model/animation resources.
- Verification:
  - `scripts/gradle-local.sh --no-daemon build` → `BUILD SUCCESSFUL in 50s`.
  - Refreshed playable PCL test profile via `scripts/configure_pcl_test_client.sh`; installed test-client Ebb jar hash now matches build jar: `2894be7aecc5689df6ee78b8d1c240153659652b8b5e4c3f331d38b42af72ad2`.
  - Jar inspection confirms `data/ebb/dialogues/demo/innkeeper_intro.json` has `push.revalidate_target=false` and bundled GeckoLib resource paths exist.
  - `ReviewSmoke` still passes: `dialogues=3, attributes=3, block_groups=1, entity_bindings=2, npc_routines=1, messages=1`.
  - `git diff --check` returned no whitespace/error output.

### DND-8 Player Attribute Points
- **Status:** complete
- **Time:** 2026-05-30 Asia/Shanghai
- Actions taken:
  - Replaced the previous three sample attributes with DND-like eight dimensions: `strength`, `dexterity`, `constitution`, `intelligence`, `wisdom`, `charisma`, `perception`, and `luck`.
  - Added attribute aliases so older content keys remain compatible: `force -> strength`, `logic -> intelligence`, `empathy -> charisma`.
  - Added persistent per-player unspent attribute points to `PlayerNarrativeState`; new/old player states default to `8` unspent points.
  - Added `/ebb attributes` and `/ebb attr` commands to display scores and points.
  - Added `/ebb attributes spend <attribute> <points>` for player point spending.
  - Added OP/debug commands `/ebb attributes grant <points>`, `/ebb attributes set <attribute> <score>`, and `/ebb attributes reset` for the invoking player.
  - Updated sample checks to use DND-8 keys: locked door uses `strength`; innkeeper pressure uses `charisma`.
  - Added `docs/player_attributes_dnd8.md` and updated manual verification docs/command history.
- Verification:
  - `scripts/gradle-local.sh --no-daemon build` → `BUILD SUCCESSFUL in 51s`.
  - `ReviewSmoke` → `dialogues=3, attributes=8, block_groups=1, entity_bindings=2, npc_routines=1, messages=1`.
  - `AttributePointsSmoke` → `AttributePointsSmoke passed: attributes=8, points=8`, verifying aliases, default points, spend, unknown rejection, and reset.
  - Refreshed playable PCL profile; build jar and installed test-client jar both hash to `823c52f5dc521a78b7c8d155420315b3a12fab5700d178d0631a4cc4c7ac895e`.

### Dialogue UI Roll/Status Layout Fix
- **Status:** complete
- **Time:** 2026-05-30 Asia/Shanghai
- Trigger:
  - User screenshot showed the roll/result/status line rendered through the choice-row area after clicking a dialogue check, visually colliding with arrow and choice buttons.
- Root cause / implementation notes:
  - `DialogueScreen` rendered roll/result/status text at a fixed `bottom - 120` Y coordinate while choice buttons used a separate bottom-anchored layout, so a one-choice post-roll node could draw status text in the same vertical band as the visible choice buttons.
  - Added shared panel layout helpers and a dedicated, scissored status area immediately above the current page of choice buttons. The dialogue body now shrinks to the area above status text, and long status/roll text is clipped instead of being drawn underneath buttons.
- Verification:
  - `scripts/gradle-local.sh --no-daemon build` → `BUILD SUCCESSFUL in 37s`.
  - `ReviewSmoke` → `ReviewSmoke passed: dialogues=3, attributes=8, block_groups=1, entity_bindings=2, npc_routines=1, messages=1`.
  - `AttributePointsSmoke` → `AttributePointsSmoke passed: attributes=8, points=8`.
  - Refreshed playable PCL profile via `scripts/configure_pcl_test_client.sh`; build jar and installed test-client jar both hash to `6ffae3ba4a80e4a1ce20dfbc0f26963ff570b41871f7b8a8cd576f1f04963000`.
  - `git diff --check` returned no whitespace/error output.

### Registered Entity Target Filtering
- **Status:** complete
- **Time:** 2026-05-30 Asia/Shanghai
- Trigger:
  - User requested that entity outline highlighting appears only for entities players/content have actively registered, not for every entity looked at.
- Root cause / implementation notes:
  - `EntityBindingRegistry.resolve(entity)` previously returned a fallback debug binding for any unmatched entity, and `ClientTargetDetector` considered every pickable non-spectator entity as a candidate. This made ordinary animals/villagers/etc. eligible for Ebb highlight/prompt.
  - Removed the implicit fallback behavior from entity binding resolution. The client raycast predicate now filters entity candidates through `EntityBindingRegistry.isRegisteredTarget(entity)` before creating an `EntityTarget`, and the server denies stale/malicious unbound entity requests with `unbound_entity`.
  - Updated manual verification docs: unregistered entities should now remain inert; tagged/bound entities such as villagers tagged `ebb.npc.innkeeper` or `/ebb summon_npc ebb:demo/innkeeper_day` should still highlight and interact.
  - Client prompt/near-highlight now uses the matched entity binding's `interaction_range` instead of the global default for registered entity targets.
- Verification:
  - Initial compile after code change: `scripts/gradle-local.sh --no-daemon build` → `BUILD SUCCESSFUL in 2m 6s`.
  - Final rebuild after preserving entity/UUID target ids and using binding-specific interaction range: `scripts/gradle-local.sh --no-daemon build` → `BUILD SUCCESSFUL in 37s`.
  - `ReviewSmoke` → `ReviewSmoke passed: dialogues=3, attributes=8, block_groups=1, entity_bindings=2, npc_routines=1, messages=1`.
  - `AttributePointsSmoke` → `AttributePointsSmoke passed: attributes=8, points=8`.
  - Refreshed playable PCL profile via `scripts/configure_pcl_test_client.sh`; build jar and installed test-client jar both hash to `88f63c0377880a70d2ab349c7ce46d77340b5e1954af0ee620a27df49d045249`.
  - `git diff --check` returned no whitespace/error output.

### Second Review Remediation
- **Status:** code/docs complete; manual Windows GUI client test pending human operation
- **Time:** 2026-05-30 Asia/Shanghai
- Source:
  - Read `C:\Users\lanla\Downloads\ebb_project_review_2026-05-30_second.md` (225 lines).
- Actions taken:
  - Added data-driven `InteractionSettings` under `data/*/interactions/settings/*.json`; bundled demo sets `enable_debug_entity_fallback=false`, while datapacks can enable it for development.
  - Restored debug fallback as an explicit setting instead of an implicit always-on fallback; unbound entities are denied as `unbound_entity` when fallback is disabled.
  - Added `EntityBindingSyncPayload` and sync lifecycle wiring so dedicated-server clients receive entity bindings and interaction settings for highlight/prompt prediction.
  - Added shared sync limits and made oversized block groups hard-invalid before sync; `BlockGroupSyncPayload` now throws instead of silently truncating oversized groups/blocks.
  - Added routine waypoint progression: multi-point routine `path` steps now advance sequentially and loop while active.
  - Updated `/ebb dev`/data summaries to include interaction settings and debug fallback status.
  - Added `docs/second_review_remediation_2026-05-30.md`, `docs/json_authoring_guide.md`, and `docs/manual_client_test_result_2026-05-30_second.md`; updated MVP/review/audit docs.
- Verification:
  - `scripts/gradle-local.sh --no-daemon build` → `BUILD SUCCESSFUL in 51s` after settings/sync/limit/waypoint implementation.
  - `ReviewSmoke` → `ReviewSmoke passed: dialogues=3, attributes=8, block_groups=1, entity_bindings=2, npc_routines=1, messages=1`.
  - `AttributePointsSmoke` → `AttributePointsSmoke passed: attributes=8, points=8`.
  - `SecondReviewSmoke` → passed; verified fallback off by default, fallback can be enabled by data config, entity binding sync payload construction, and oversized block-group invalidation.
  - Refreshed playable PCL profile after second-review build; build jar and installed test-client jar both hash to `510cb69490d4b855a084ab433d0d0db06b150ad8aa253b76dd2fa94fdf9d432b`.
  - Final `git diff --check` returned no whitespace/error output.
  - `scripts/gradle-local.sh --no-daemon runServer --args nogui` → `BUILD SUCCESSFUL in 1m 29s`; Fabric loaded Minecraft/Fabric API/GeckoLib/Ebb and stopped at the normal EULA gate, not an Ebb initialization crash.
- Pending:
  - Full GUI hand-test requires launching and operating the Windows `26.1.2-Fabric-Ebb-Test` Minecraft client. A checklist/status file was added at `docs/manual_client_test_result_2026-05-30_second.md`.

### Second Review Completion Audit
- **Status:** complete as an audit; overall goal remains active due missing Windows GUI hand-test evidence.
- **Time:** 2026-05-30 Asia/Shanghai
- Actions taken:
  - Added `docs/second_review_completion_audit_2026-05-30.md` mapping every explicit second-review/user requirement to current worktree evidence.
  - Confirmed code/data/docs/build/smoke/server-smoke/test-client-refresh requirements have evidence.
  - Confirmed the only unproven item is the review P0 full player-driven Windows GUI client test; result remains pending in `docs/manual_client_test_result_2026-05-30_second.md`.


### Registered Entity Target UUID Sync Hotfix
- **Status:** code/build/profile complete; Windows GUI retest pending
- **Time:** 2026-05-31 Asia/Shanghai
- Trigger:
  - User's GUI screenshot showed `/ebb data` loaded `interaction_settings(debug_entity_fallback=false)` and `entity_bindings(valid=2)`. The nearest `ebb:npc` had tags (`ebb.npc`, `ebb.npc.ebb`, `ebb_npc`) after `/tag`/`/reload`, but there was still no cyan outline or interaction prompt.
- Root cause / implementation notes:
  - `EntityBindingSyncPayload` synced binding definitions/settings, but tag-based client prediction still depended on the client being able to read server-side entity tags. That is unreliable for dedicated-server-style clients and reproduced in the GUI test.
  - Added `SyncedEntityTarget`, `EntityTargetSyncPayload`, and `ClientEntityTargetIndex`.
  - The server now sends per-player snapshots of nearby server-matched registered entity UUIDs every 20 ticks and on data sync/reload. Debug fallback mode intentionally sends an empty target list because fallback prediction already permits any pickable entity when explicitly enabled.
  - `ClientTargetDetector` now allows entity raycast targets when either the UUID is in the server-synced registered target index or the entity can be resolved locally by synced binding definitions/fallback settings.
  - Server-side `InteractionService.validateEntity` remains authoritative for UUID existence, binding match, range, and line of sight.
- Verification:
  - `scripts/gradle-local.sh --no-daemon build` → `BUILD SUCCESSFUL in 48s`.
  - `ReviewSmoke` → `ReviewSmoke passed: dialogues=3, attributes=8, block_groups=1, entity_bindings=2, npc_routines=1, messages=1`.
  - `AttributePointsSmoke` → `AttributePointsSmoke passed: attributes=8, points=8`.
  - `SecondReviewSmoke` → passed and now covers `EntityTargetSyncPayload` construction.
  - Refreshed playable PCL profile via `scripts/configure_pcl_test_client.sh`; build jar and installed test-client jar both hash to `22a1a4ad8de44f8dc673c3f65474c456f27480f0ecbbb38fb6d7cbbfb438d1ae`.
  - `git diff --check` returned no whitespace/error output.
  - `scripts/gradle-local.sh --no-daemon runServer --args nogui` → `BUILD SUCCESSFUL in 1m 21s`; Ebb initialized and the server stopped at the normal EULA gate.
- Pending:
  - Human must restart/relaunch `26.1.2-Fabric-Ebb-Test` to load the refreshed jar, then retest the tagged/summoned NPC highlight and `X` interaction.


### Third Review Runtime Wiring Reconciliation
- **Status:** code/docs/build/source-audit complete; Windows GUI retest pending
- **Time:** 2026-05-31 Asia/Shanghai
- Source:
  - Read `C:\Users\lanla\Downloads\ebb_project_review_2026-05-31_third.md`. The review flagged that a Drive source sample appeared to lack runtime wiring for entity/block sync, payload receivers, entrypoints, commands, and dialogue effects/lifecycle despite documents claiming completion.
- Actions taken:
  - Re-audited the active checkout and confirmed the P0/P1 wiring is present in current source: `ClientBlockGroupIndex`, `ClientEntityTargetIndex`, sync payload registration/receivers, `InteractionSyncService`, `EbbMod`/`EbbClient` entrypoints, attributes/summon commands, dialogue enter/outcome effects, and session lifecycle.
  - Tightened `ClientTargetDetector` to respect matched `highlightRange` for synced/bound entity predictions.
  - Updated `ClientInteractionNetworking` to clear synced block/entity/binding/settings state on client JOIN as well as DISCONNECT.
  - Added tracked `scripts/third_review_static_audit.py` to fail on the stale patterns described by the review and assert all required wiring points.
  - Added `docs/third_review_reconciliation_2026-05-31.md` and `docs/third_review_completion_audit_2026-05-31.md`; updated manual/MVP/audit docs to separate source/build completion from pending GUI retest.
- Verification:
  - `scripts/third_review_static_audit.py` → `ThirdReviewStaticAudit passed: runtime wiring and documented command/effect surfaces are present.`
  - `scripts/gradle-local.sh --no-daemon build` → `BUILD SUCCESSFUL in 46s`.
  - `ReviewSmoke` → `ReviewSmoke passed: dialogues=3, attributes=8, block_groups=1, entity_bindings=2, npc_routines=1, messages=1`.
  - `AttributePointsSmoke` → `AttributePointsSmoke passed: attributes=8, points=8`.
  - `SecondReviewSmoke` → passed, including entity target sync payload construction.
  - Refreshed playable PCL profile; build jar and installed test-client jar both hash to `01d880f0e424e0b6ff3592e9bf89189199254167ebb891baffa18330231758b0`.
  - Jar inspection confirmed `EntityTargetSyncPayload`, `ClientTargetDetector`, and `ClientInteractionNetworking` are packaged.
  - `scripts/gradle-local.sh --no-daemon runServer --args nogui` → `BUILD SUCCESSFUL in 1m 44s`; Ebb initialized and stopped at the normal EULA gate.
  - `git diff --check` returned no whitespace/error output.
- Pending:
  - Human must fully relaunch `26.1.2-Fabric-Ebb-Test` and retest the entity highlight/prompt/X-dialogue path.


### Third Review Drive Mirror Verification
- **Status:** complete for source-tree consistency; Windows GUI retest still pending
- **Time:** 2026-05-31 Asia/Shanghai
- Actions taken:
  - Pulled the current Google Drive repo folder `1cGZxWHdCeYYI3ttzL6ilXEGjkOlCz2nt` back into `build/tmp/drive-third-audit` after pushing/syncing commit `4085b5fbe5f515fa1966d583e8c5ce66298f7aed`.
  - Ran the third-review static audit from the Drive mirror, not only from the local checkout.
  - Compared SHA-256 hashes for critical runtime wiring files between local checkout and Drive mirror.
- Verification:
  - Drive pull contained `129` files.
  - `python3 build/tmp/drive-third-audit/scripts/third_review_static_audit.py` → `ThirdReviewStaticAudit passed: runtime wiring and documented command/effect surfaces are present.`
  - Local-vs-Drive hashes matched for `ClientTargetDetector.java`, `ClientInteractionNetworking.java`, `ModPackets.java`, `InteractionSyncService.java`, `EbbMod.java`, `EbbClient.java`, `ModCommands.java`, `third_review_static_audit.py`, `third_review_completion_audit_2026-05-31.md`, and `third_review_reconciliation_2026-05-31.md`.
  - The stale `EbbMod.id("debug/entity")` hardcoded entity target pattern is absent from the Drive mirror detector.
- Pending:
  - Human GUI retest remains the only unproven item.

### Deep Research Report Phase 1 Contract/Dev Command Foundation
- **Status:** in_progress
- **Time:** 2026-05-31 Asia/Shanghai
- Actions taken:
  - Restored planning context and re-read `deep-research-report (2).md` from `C:\Users\lanla\Downloads`.
  - Verified existing in-progress architecture/schema changes compile with `scripts/gradle-local.sh --no-daemon build`.
  - Added public dev-dump helpers for single dialogue/routine inspection.
  - Added current dialogue session lookup and NPC routine path getters for OP tooling.
  - Expanded `/ebb dev` with `on/off`, and added OP command surfaces for dialogue inspect/tree/vars/reload, routine inspect, and save-debug export.
- Verification so far:
  - `scripts/gradle-local.sh --no-daemon build` → BUILD SUCCESSFUL after command expansion.

### Phase 16: Deep Research Report Implementation
- **Status:** complete
- **Time:** 2026-05-31 Asia/Shanghai
- Actions taken:
  - Implemented report-driven API contracts, dialogue schema/runtime extensions, narrative variables, richer conditions/effects, and failure-forward validation.
  - Implemented OP command surfaces for dev mode, dialogue inspection/tree/variables/reload, routine inspection, and save-debug export.
  - Added author-friendly examples under `authoring/`, compiler `scripts/compile_authoring_sources.py`, Gradle tasks `compileEbbAuthoring`/`validateEbbData`, tracked smoke runner, and CI workflow.
  - Re-reviewed changes and fixed bugs found by smoke/static checks: string `value` parsing for `set_var`, clearer attribute condition matching, shortcut effect id parsing, no-namespace entity type defaults, and known custom `ebb:npc` validation.
- Verification:
  - `scripts/gradle-local.sh --no-daemon validateEbbData` → BUILD SUCCESSFUL.
  - `scripts/run_smoke_checks.sh` → BUILD SUCCESSFUL + DeepResearchSmoke/ReviewSmoke/AttributePointsSmoke/SecondReviewSmoke + ThirdReviewStaticAudit all passed.
  - Final rerun `scripts/run_smoke_checks.sh` → Gradle build successful; DeepResearchSmoke, AttributePointsSmoke, ReviewSmoke, SecondReviewSmoke, and ThirdReviewStaticAudit all passed with zero registry validation messages.
  - Final jar hashes: `build/libs/ebb-0.1.0-dev.jar` SHA-256 `42fe00c49b6a7f5dde80521075354c7a56c05196f11b9af10b7f138d865efadb`; sources jar SHA-256 `e6fc48d6edbeb164bd02e6c5bde7cd471f786a7d22869dbc41b8f88759450bf5`.
  - Jar inspection via `.tools/jdk-25/bin/jar tf` confirmed packaged `com/crpg/ebb/api/*`, `DialogueNodeType`, `RollMode`, expanded dialogue/interaction/dev command classes, and bundled runtime data resources.
  - `git diff --check` and `scripts/third_review_static_audit.py` passed after final edits.


### Phase 16 Deep Research Final Test/Audit Hardening
- **Status:** complete
- **Time:** 2026-06-01 Asia/Shanghai
- Actions taken:
  - Added formal JUnit coverage in `src/test/java/com/crpg/ebb/DeepResearchDataTest.java` for bundled registry cleanliness, fail-forward validation, narrative effect persistence, saved-data codec round-trip for variables/flags, and generated authoring output loading.
  - Added Fabric GameTest coverage in `src/main/java/com/crpg/ebb/test/EbbGameTests.java` and registered it through the `fabric-gametest` entrypoint.
  - Fixed the GameTest run configuration by removing the over-narrow `fabric-api.gametest.filter=ebb`; the unfiltered headless server discovers and runs the Ebb tests reliably.
  - Extended `scripts/third_review_static_audit.py` and `.github/workflows/build.yml` to assert/run JUnit, authoring validation, Fabric GameTest, and jar build coverage.
  - Added `docs/deep_research_report_completion_audit_2026-06-01.md` and updated authoring/implementation docs with final automated-test evidence.
- Verification:
  - `scripts/run_smoke_checks.sh` → BUILD SUCCESSFUL; DeepResearchSmoke, AttributePointsSmoke, ReviewSmoke, SecondReviewSmoke, and ThirdReviewStaticAudit all passed.
  - `scripts/gradle-local.sh --no-daemon validateEbbData` → BUILD SUCCESSFUL in 54s.
  - JUnit XML `build/test-results/test/TEST-com.crpg.ebb.DeepResearchDataTest.xml` → tests=5, failures=0, errors=0, skipped=0.
  - `scripts/gradle-local.sh --no-daemon runGametestServer --args nogui` → 4 required game tests passed.
  - Final jar SHA-256 `98130df556aa021dc1928736943e839d7dc049e6a83d1deb918b0d697b6e2cf3`; sources jar SHA-256 `9d4c2108c6821f86174429161dbeb54e006045add300d3bd272474b540bc22bb`.


### Phase 16 Continuation Audit Hardening
- **Status:** complete
- **Time:** 2026-06-01 Asia/Shanghai
- Actions taken:
  - Treated completion as unproven and re-read the deep research report against current source evidence.
  - Found and closed weak report-alignment points: `DialogueScreen` now keeps a scrollable dialogue history log; `NpcRoutineController` pauses movement and focuses look-at during active NPC conversations; routine look policies now carry `requires_line_of_sight`; `NarrativeSavedData` now persists a schema version.
  - Extended the tracked static audit to assert these newly closed requirements.
- Verification:
  - `scripts/gradle-local.sh --no-daemon test` → BUILD SUCCESSFUL; JUnit tests passed.
  - `scripts/run_smoke_checks.sh` → BUILD SUCCESSFUL; DeepResearchSmoke, AttributePointsSmoke, ReviewSmoke, SecondReviewSmoke, ThirdReviewStaticAudit, and DeepResearchStaticAudit passed.
  - `scripts/gradle-local.sh --no-daemon validateEbbData` → BUILD SUCCESSFUL in 49s.
  - `scripts/gradle-local.sh --no-daemon runGametestServer --args nogui` → 4 required game tests passed.
  - `git diff --check`, `python3 scripts/third_review_static_audit.py`, and `python3 scripts/deep_research_static_audit.py` passed.
  - Final jar SHA-256 `98130df556aa021dc1928736943e839d7dc049e6a83d1deb918b0d697b6e2cf3`; sources jar SHA-256 `9d4c2108c6821f86174429161dbeb54e006045add300d3bd272474b540bc22bb`.

### Phase 17 / GOAL.md P2 Story Variables
- **Status:** complete for code/data/docs/tests; Windows GUI retest remains human-operated.
- **Time:** 2026-06-01 Asia/Shanghai
- Source:
  - Read `C:\Users\lanla\Downloads\GOAL.md` and treated it as the active execution target over older generic planning, while preserving the existing Fabric 26.1.2 / Java 25 / GeckoLib 5.5.1 stack.
- Actions taken:
  - Added `com.crpg.ebb.story.StoryVarLayer` and `StoryVarValue`.
  - Extended `PlayerNarrativeState` and `NarrativeSavedData` codecs with Branch/Major/Minor story-variable maps for player and world scope.
  - Added story-variable dialogue effects: `set_story_var`, `clear_story_var` / `remove_story_var`, and `add_story_int` / `increment_story_var`.
  - Added `story_var` dialogue conditions with scalar equality and integer `min` / `at_least` checks.
  - Exposed story vars in `/ebb dialogue vars`, `/ebb dev`, saved-data debug snapshots, and authoring docs.
  - Extended the authoring compiler shortcuts for `setStoryVar`, `clearStoryVar`, and `addStoryInt`.
  - Updated `ebb:demo/innkeeper_intro` so it writes/reads all three layers: Minor `met_innkeeper` / `innkeeper_annoyance`, Major `innkeeper_trust`, and Branch `tavern_route=public`.
  - Added `scripts/goal_static_audit.py` and wired it into `scripts/run_smoke_checks.sh`.
  - Added JUnit/smoke coverage for Branch/Major/Minor story-var effects, conditions, and saved-data codec round trips.
  - Added `docs/goal_p2_story_variables_2026-06-01.md` and updated `docs/json_authoring_guide.md` / `docs/mvp_verification_steps.md`.
- Verification:
  - `scripts/gradle-local.sh --no-daemon build` → `BUILD SUCCESSFUL in 3m 39s`.
  - `scripts/run_smoke_checks.sh` → build successful; DeepResearchSmoke, AttributePointsSmoke, ReviewSmoke, SecondReviewSmoke, ThirdReviewStaticAudit, DeepResearchStaticAudit, and GoalStaticAudit all passed.
  - `scripts/goal_static_audit.py` → passed.
  - `scripts/deep_research_static_audit.py` → passed.
  - `scripts/third_review_static_audit.py` → passed.
  - `scripts/gradle-local.sh --no-daemon validateEbbData` → `BUILD SUCCESSFUL in 55s`.
  - `scripts/gradle-local.sh --no-daemon runGametestServer --args nogui` → 4 required GameTests passed.
  - `git diff --check` → no whitespace errors.
- Next:
  - Implement GOAL.md P3 Quest Branch / Take Root / Feat definitions, state, dialogue effects/conditions, dev views, docs, demo content, and tests.

### Phase 17 / GOAL.md P3 Quest Branch, Take Root, and Feat MVP
- **Status:** complete for code/data/docs/tests; Windows GUI retest remains human-operated.
- **Time:** 2026-06-01 Asia/Shanghai
- Actions taken:
  - Added `quest/` package with `QuestBranchDefinition`, `QuestBranchRegistry`, `QuestBranchKind`, `TakeRootService`, and `QuestTreeService`.
  - Added `feat/` package with `FeatDefinition` and `FeatRegistry`.
  - Added raw JSON registries for `quest_branches` and `feats` through `NarrativeDataRegistries`.
  - Extended `PlayerNarrativeState` / `NarrativeSavedData` with quest states, unlocked feats, active feats, and 4 active feat slots.
  - Added dialogue effects for `start_quest_branch`, `complete_quest_branch`, `unlock_feat`, and `activate_feat`.
  - Added dialogue conditions for `quest_state` and `has_feat`.
  - Added feat check modifiers to server-authoritative d20 rolls through `FeatRegistry.totalCheckModifier`.
  - Added basic Quest Tree UX: `QuestTreePayload`, `QuestTreeScreen`, and `/ebb quest` / `/ebb quest tree`.
  - Extended `/ebb dev` with quest/feat registry summaries and per-player quest/feat state.
  - Added bundled quest branches `ebb:demo/tavern_public` and `ebb:demo/tavern_quiet` plus 4 feats: `Tavern Authority`, `Paranoid Pattern Reader`, `Cheap Empathy`, and `Door Theology`.
  - Updated `ebb:demo/innkeeper_intro` so public/quiet choices complete major branches and a feat-gated authority follow-up appears after `Tavern Authority`.
  - Extended `scripts/goal_static_audit.py`, JUnit, smoke tests, authoring guide, and MVP verification docs for P3.
  - Added `docs/goal_p3_quest_feat_2026-06-01.md`.
- Verification:
  - `scripts/gradle-local.sh --no-daemon build` → `BUILD SUCCESSFUL in 3m 28s`.
  - `scripts/run_smoke_checks.sh` → build successful; DeepResearchSmoke, AttributePointsSmoke, ReviewSmoke, SecondReviewSmoke, ThirdReviewStaticAudit, DeepResearchStaticAudit, and GoalStaticAudit all passed.
  - `scripts/goal_static_audit.py` → passed for P2 and P3.
  - `scripts/gradle-local.sh --no-daemon validateEbbData` → `BUILD SUCCESSFUL in 54s`.
  - `scripts/gradle-local.sh --no-daemon runGametestServer --args nogui` → 4 required GameTests passed.
  - `git diff --check` → no whitespace errors.
  - Jar SHA-256 `643740159d9f61a2e7f699d898edbdfdcfdea9d01d6e7b78e46b780064f4932b`; sources jar SHA-256 `70ba1356a89e0fb9e56196a5f8d2a1b3a551fc8baedf804beec2de327b397232`.
- Next:
  - Implement GOAL.md P4 Chime / Inner Voice / Passive Inserts MVP.

### Phase 17 / GOAL.md P4 Chime and Inner Voice MVP
- **Status:** complete for code/data/docs/tests; Windows GUI retest remains human-operated.
- **Time:** 2026-06-01 Asia/Shanghai
- Actions taken:
  - Added `chime/` package with `ChimeDefinition`, `ChimeRegistry`, and `ChimeResolver`.
  - Added raw JSON registry for `chimes` through `NarrativeDataRegistries`.
  - Extended `DialogueNode` with `tags` / `chime_tags` parsing.
  - Hooked `ChimeResolver` into `DialogueService` open/update payload creation so passive inserts are still server-authoritative.
  - Added cyan chime status styling in `DialogueScreen` for `[Chime: ...]` passive inserts.
  - Added four bundled chimes: Instinct, Rhetoric, Dread, and Empathy.
  - Tagged the innkeeper start node with `innkeeper.read`; different attribute builds can now trigger different chime inserts on the same scene.
  - Added the `rhetoric_insight` thought path gated by the passive `chime:ebb:demo/rhetoric` flag.
  - Extended `scripts/goal_static_audit.py`, JUnit, smoke tests, authoring docs, MVP verification docs, and dev snapshot validation with chime coverage.
  - Added `docs/goal_p4_chime_2026-06-01.md`.
- Verification:
  - `scripts/gradle-local.sh --no-daemon build` → `BUILD SUCCESSFUL in 3m 13s`.
  - `scripts/run_smoke_checks.sh` → build successful; DeepResearchSmoke, AttributePointsSmoke, ReviewSmoke, SecondReviewSmoke, ThirdReviewStaticAudit, DeepResearchStaticAudit, and GoalStaticAudit all passed.
  - `scripts/goal_static_audit.py` → passed for P2, P3, and P4.
  - `scripts/gradle-local.sh --no-daemon validateEbbData` → `BUILD SUCCESSFUL in 57s`.
  - `scripts/gradle-local.sh --no-daemon runGametestServer --args nogui` → 4 required GameTests passed.
  - `git diff --check` → no whitespace errors.
  - Jar SHA-256 `cd2cd304d9e3f12da49ab0b79ac90c7ce656e013898d73cfb459b96f919f36d7`; sources jar SHA-256 `2586f2849bdfa16bfd05dc0ed09c4a1b7e6cb598c1375843ff5e183261a5365b`.
- Next:
  - Implement GOAL.md P5 UI rhythm and Journal / Quest Tree maturation.


### Phase 17 / GOAL.md P5 Journal and UI Rhythm MVP
- **Status:** complete for code/data/docs/tests; Windows GUI retest remains human-operated.
- **Time:** 2026-06-01 Asia/Shanghai
- Actions taken:
  - Added journal entries registry/service, persisted player journal IDs, JournalPayload/JournalScreen, and `/ebb journal`.
  - Added `add_journal_entry` / `reveal_clue` effects and `has_journal_entry` / `clue_found` conditions.
  - Added four bundled clue/lead entries and wired locked-door / innkeeper demo routes to produce visible journal consequences.
  - Strengthened DialogueScreen status echoes with typed labels/colors for clue/journal, quest/take-root, feat, chime, story-var, and future relation feedback.
  - Updated authoring docs, MVP verification docs, smoke/JUnit coverage, and `scripts/goal_static_audit.py`; added `docs/goal_p5_journal_ui_2026-06-01.md`.
- Verification:
  - `scripts/run_smoke_checks.sh` passed; DeepResearchSmoke, AttributePointsSmoke, ReviewSmoke, SecondReviewSmoke, ThirdReviewStaticAudit, DeepResearchStaticAudit, and GoalStaticAudit all passed.
  - `scripts/goal_static_audit.py` passed for P2, P3, P4, and P5.
  - `scripts/gradle-local.sh --no-daemon validateEbbData` → `BUILD SUCCESSFUL in 1m 11s`.
  - `scripts/gradle-local.sh --no-daemon runGametestServer --args nogui` → 4 required GameTests passed.
  - `git diff --check` → no whitespace errors.
  - Jar SHA-256 `6f3e3b345b8d5c457dbc291fa927fbf32d0c7793eb30911d028b696bef2b25fa`; sources jar SHA-256 `cfb59f1f4b1f228d850e9bf6dd77d43996d158e36453421556e779edc5f64bd2`.
- Next:
  - Implement GOAL.md P6 relationship, NPC memory/state tags, time/state-dependent dialogue, and routine expansion.


### Phase 17 / GOAL.md P6 Relationship, NPC Memory, and Routine Expansion
- **Status:** complete for code/data/docs/tests; Windows GUI retest remains human-operated.
- **Time:** 2026-06-01 Asia/Shanghai
- Actions taken:
  - Added relationship registry/data, relationship score persistence, player/world NPC state tags, debug snapshots, and dev browser lines.
  - Added dialogue effects/conditions for relation score changes, NPC memory tags, time-window visibility, and real NPC routine switching.
  - Expanded Ebb NPC routine skeleton with narrative key, pose, animation, wait/walk_path/look_at/play_animation/set_pose/teleport_fallback action handling.
  - Added witness/tenant/guard role bindings, dialogues, routines, and innkeeper relationship/routine consequences.
  - Updated JSON authoring docs, MVP verification docs, P6 completion audit, smoke/JUnit coverage, and GoalStaticAudit.
- Verification:
  - `scripts/gradle-local.sh --no-daemon build` → `BUILD SUCCESSFUL in 2m 39s`.
  - `scripts/run_smoke_checks.sh` passed; DeepResearchSmoke, AttributePointsSmoke, ReviewSmoke, SecondReviewSmoke, ThirdReviewStaticAudit, DeepResearchStaticAudit, and GoalStaticAudit all passed.
  - `scripts/goal_static_audit.py` passed for P2-P6.
  - `scripts/gradle-local.sh --no-daemon validateEbbData` → `BUILD SUCCESSFUL in 54s`.
  - `scripts/gradle-local.sh --no-daemon runGametestServer --args nogui` → 4 required GameTests passed.
  - `git diff --check` → no whitespace errors.
  - Jar SHA-256 `b64e744f4a6c115757ecefee2098fb22061f9a44c1438ae80877e96d87f96c92`; sources jar SHA-256 `b08b1fdd6abee405fe901d7e0f98619d813143f9bf369ca4f30b86c9068e7698`.
- Next:
  - Implement GOAL.md P7 investigation scenes, clue-to-DC hooks, and one set-piece conflict prototype.


### Phase 17 / GOAL.md P7 Investigation and Set-piece Conflict
- **Status:** complete for code/data/docs/tests; Windows GUI retest remains human-operated.
- **Time:** 2026-06-01 Asia/Shanghai
- Actions taken:
  - Added investigation and conflict packages, registries, clue definitions, investigation scene definitions, conflict definitions, and services.
  - Added discovered clue persistence, scene/conflict narrative state, conflict stress/resolve scores, dev snapshot lines, and saved-data debug output.
  - Added `reveal_clue`, conflict, and scene-phase dialogue effects/conditions; hooked discovered clue modifiers into server-authoritative d20 rolls.
  - Added 1 investigation scene, 5 clues, 1 hallway confrontation set-piece, and demo wiring in locked-door/witness/tenant/guard dialogue.
  - Updated docs, smoke/JUnit coverage, and `scripts/goal_static_audit.py`.
- Verification:
  - `scripts/gradle-local.sh --no-daemon build` → `BUILD SUCCESSFUL in 2m 56s`.
  - `scripts/run_smoke_checks.sh` passed; DeepResearchSmoke, AttributePointsSmoke, ReviewSmoke, SecondReviewSmoke, ThirdReviewStaticAudit, DeepResearchStaticAudit, and GoalStaticAudit all passed.
  - `scripts/goal_static_audit.py` passed for P2-P7.
  - `scripts/gradle-local.sh --no-daemon validateEbbData` → `BUILD SUCCESSFUL in 1m 11s`.
  - `scripts/gradle-local.sh --no-daemon runGametestServer --args nogui` → 4 required GameTests passed.
  - `git diff --check` → no whitespace errors.
  - Jar SHA-256 `d511e37ae5451cf5797ebd22ccf8aca12c11976091b15625275ce3c161d2d14c`; sources jar SHA-256 `f9de175da9917b8abf790da05e27e88e59f0b96a7d66c9469e9e928e44ca306d`.
- Next:
  - Implement GOAL.md P8 playable tavern vertical slice completion and final evidence.

### Phase 17 / GOAL.md P8 Playable Tavern Vertical Slice
- **Status:** complete for code/data/docs/tests and automated verification; Windows GUI retest remains human-operated.
- **Time:** 2026-06-01 Asia/Shanghai
- Actions taken:
  - Completed the P8 tavern/side-door vertical slice content inventory: 4 role NPCs (innkeeper, witness, tenant, guard), 8 block-group interactable points, 2 major quest branches, 4 feats, 4 chimes, 5 clues, 1 investigation scene, 1 set-piece conflict, and public/quiet/messy ending placeholders.
  - Added seven new block-group targets and seven matching block dialogues around the existing locked-door scene: counter ledger, washroom mirror, windowsill ash, tenant luggage, notice board, cellar hatch, and back door.
  - Extended role NPC content and route consequences so public, quiet, and fail-forward paths all write server-authoritative story/quest/clue/conflict/journal state and surface visible UI feedback.
  - Updated `docs/goal_p8_vertical_slice_2026-06-01.md`, `docs/json_authoring_guide.md`, `docs/mvp_verification_steps.md`, JUnit coverage, smoke checks, and `scripts/goal_static_audit.py` for P8.
  - Re-reviewed the changed runtime/data surface after P8 and confirmed the new content stays data-driven instead of adding one-off Java scene code.
- Verification:
  - `scripts/gradle-local.sh --no-daemon build` → `BUILD SUCCESSFUL in 2m 48s`.
  - `scripts/run_smoke_checks.sh` → build successful; `DeepResearchSmoke`, `AttributePointsSmoke`, `ReviewSmoke`, `SecondReviewSmoke`, `ThirdReviewStaticAudit`, `DeepResearchStaticAudit`, and `GoalStaticAudit` all passed.
  - `python3 scripts/goal_static_audit.py` → passed for P2-P8.
  - `scripts/gradle-local.sh --no-daemon validateEbbData` → `BUILD SUCCESSFUL in 59s`.
  - `scripts/gradle-local.sh --no-daemon runGametestServer --args nogui` → 4 required GameTests passed; `BUILD SUCCESSFUL in 3m 30s`.
  - `git diff --check` → no whitespace errors.
  - Jar SHA-256 `47d3e8ac42c28c9e0d6dc437e90656c03404694677677d4e0212220c28c29d59`; sources jar SHA-256 `a4c89f3076519c8570f092c0b6f5e0006c53cac52cb64b6acde1f95310977bd5`.
  - Jar inspection confirmed P7/P8 packages and data resources: conflict, investigation, relationship classes plus clues, conflict, investigation scene, and all eight P8 block-group JSON files.
  - Post-bookkeeping recheck: fixed trailing-blank markdown whitespace, then `git diff --check`, `python3 scripts/goal_static_audit.py`, `python3 scripts/deep_research_static_audit.py`, `python3 scripts/third_review_static_audit.py`, and `scripts/gradle-local.sh --no-daemon validateEbbData` all passed; latest `validateEbbData` finished `BUILD SUCCESSFUL in 57s`.
- Remaining manual step:
  - Optional/full GUI retest from the separate `26.1.2-Fabric-Ebb-Test` Windows profile: spawn role NPCs, visit all eight interactable points, play public/quiet/fail-forward routes, and verify the back-door ending placeholder changes.

### Phase 18 / GUI Retest Issue Hotfix
- **Status:** complete for code/profile/audit refresh; human must relaunch client for final visual retest.
- **Time:** 2026-06-01 Asia/Shanghai
- Actions taken:
  - Investigated screenshots and the save `新的世界 (1)`.
  - Found the test profile was still running an older Ebb jar (`01d880...`) that packaged only 1 block group and 2 entity bindings; the rebuilt project jar packages 8 block groups and 6 bindings.
  - Inspected `新的世界 (1)` block data and confirmed the P8 target objects exist at the authored coordinates.
  - Inspected existing Ebb NPC entities and found legacy tags such as `ebb.npc.tenant_day`; updated role bindings to match both demo and legacy role tags.
  - Made `/ebb dialogue vars` player-self accessible, added `/ebb vars`, and made `/ebb summon_npc tenant_day` resolve to bundled `ebb:demo/tenant_day` where available.
  - Refreshed `26.1.2-Fabric-Ebb-Test` via `scripts/configure_pcl_test_client.sh`; installed jar hash now matches build jar.
  - Moved client synced-interaction cleanup from `ClientPlayConnectionEvents.JOIN` to `INIT` so block-group/entity-binding sync is not cleared after initial join.
  - Added `scripts/gui_retest_issue_audit.py` and wired it into `scripts/run_smoke_checks.sh` as a regression check for these exact screenshots/symptoms.
- Verification:
  - `scripts/gradle-local.sh --no-daemon build` → `BUILD SUCCESSFUL in 2m 56s` after the final client sync-order fix.
  - `scripts/run_smoke_checks.sh` → passed; loaded 13 dialogues, 8 block groups, 6 entity bindings, 5 routines, 0 validation messages; includes `GuiRetestIssueAudit`.
  - `scripts/gradle-local.sh --no-daemon validateEbbData` → `BUILD SUCCESSFUL in 1m 11s`.
  - `git diff --check` → no whitespace errors.
  - `scripts/gradle-local.sh --no-daemon runGametestServer --args nogui` → all 4 required GameTests passed; `BUILD SUCCESSFUL in 4m 34s`.
  - Build jar and installed test-profile jar both SHA-256 `2f1817dcf9c3511eb02588c9d1ae3f4f73e61473744f273a1499741e275dea65`.
  - Installed jar inspection confirmed 6 entity bindings, all 8 P8 block-group JSON files, and legacy tenant tags in packaged binding data.
  - Full GUI issue audit with save path passed: `GuiRetestIssueAudit passed: commands, INIT sync clear, role bindings, 8 block groups, build jar, profile=2f1817dcf9c3511eb02588c9d1ae3f4f73e61473744f273a1499741e275dea65, save=...新的世界 (1)`.
- Manual retest requirement:
  - Fully close/relaunch Minecraft so the new jar is loaded; then run `/ebb data` and retest commands, role NPC dialogue, and all block targets.

### Phase 18 / GUI Retest Issue Hotfix — strengthened regression evidence
- **Status:** complete for code/profile/audit/test refresh; human must relaunch client for final visual retest.
- **Time:** 2026-06-01 Asia/Shanghai
- Actions taken:
  - Added JUnit regression coverage in `DeepResearchDataTest.guiRetestCommandsRoleBindingsAndBlockGroupsAreRegistered` for `/ebb journal`, `/ebb quest`, `/ebb dialogue vars`, `/ebb vars`, four distinct role-specific NPC bindings, legacy `ebb.npc.<role>_day` tags, and all eight GUI retest block groups.
  - Added Fabric GameTests for runtime legacy role-tag resolution across innkeeper/witness/tenant/guard and all eight vertical-slice block-group targets.
  - Rebuilt the jar, refreshed `26.1.2-Fabric-Ebb-Test`, and re-ran the GUI issue audit against `新的世界 (1)`.
- Verification:
  - `scripts/gradle-local.sh --no-daemon test` → `BUILD SUCCESSFUL in 3m 47s`.
  - `scripts/gradle-local.sh --no-daemon runGametestServer --args nogui` → 6 required GameTests passed; `BUILD SUCCESSFUL in 4m 30s`.
  - `scripts/run_smoke_checks.sh` → passed, including `GuiRetestIssueAudit`.
  - `scripts/configure_pcl_test_client.sh` → refreshed separate Fabric test profile.
  - `scripts/gui_retest_issue_audit.py --save-path '.../新的世界 (1)' --require-save` → passed; profile jar hash `da8da3aaa3769bbbb8b1324fe4bedf56ddc497ef26fb355766df40316285cf94`.
  - `sha256sum build/libs/ebb-0.1.0-dev.jar .../mods/ebb-0.1.0-dev.jar` → both `da8da3aaa3769bbbb8b1324fe4bedf56ddc497ef26fb355766df40316285cf94`.
  - `git diff --check` → no whitespace errors.
- Manual retest requirement:
  - Fully close/relaunch Minecraft so the new jar is loaded, then retest the screenshots' three visible symptom classes in `新的世界 (1)`.

### Phase 18 / Runtime relaunch check helper
- **Status:** complete for helper/evidence; GUI visual retest remains pending.
- **Time:** 2026-06-01 Asia/Shanghai
- Actions taken:
  - Added `scripts/check_pcl_runtime_loaded.py` to distinguish a refreshed profile jar on disk from an already-running/stale Minecraft client process.
  - The helper compares build/profile jar hashes, parses `26.1.2-Fabric-Ebb-Test/logs/latest.log`, and requires at least 13 dialogues, 8 block groups, 6 entity bindings, and 5 routines.
- Verification:
  - `python3 -m py_compile scripts/check_pcl_runtime_loaded.py` → passed.
  - `scripts/check_pcl_runtime_loaded.py` currently reports stale runtime counts from the old visible session: dialogues=3, block_groups=1, entity_bindings=2, npc_routines=1, and notes that `latest.log` is older than the installed refreshed jar.
- Manual retest requirement:
  - Fully close/relaunch Minecraft, enter `新的世界 (1)`, then re-run `scripts/check_pcl_runtime_loaded.py`; it should pass before final GUI symptom confirmation.

### Phase 18 / Runtime relaunch blocker recheck
- **Status:** blocked on external Windows GUI relaunch.
- **Time:** 2026-06-01 Asia/Shanghai
- Evidence:
  - Re-ran `scripts/check_pcl_runtime_loaded.py`.
  - The profile jar on disk is still the refreshed jar `da8da3aaa3769bbbb8b1324fe4bedf56ddc497ef26fb355766df40316285cf94`.
  - The latest runtime log still reports the old loaded data counts: dialogues=3, block_groups=1, entity_bindings=2, npc_routines=1.
  - The latest runtime log is older than the installed refreshed jar, proving Minecraft has not been relaunched into the refreshed mod build yet.
- Blocker:
  - No further source/profile changes can prove the requested GUI symptoms are fixed until the Windows client is fully closed and relaunched into `26.1.2-Fabric-Ebb-Test`, then `新的世界 (1)` is opened and rechecked.

### Phase 19 / Mineflayer + MineDojo-compatible Windows GUI automation
- **Status:** implementation complete; final visual pass still blocked by stale Windows client runtime until relaunch.
- **Time:** 2026-06-01 Asia/Shanghai
- Actions taken:
  - Added local Node automation package under `tools/gui_automation/node` with `mineflayer`, `minecraft-protocol`, and `minecraft-data`.
  - Added a 26.1.2 adapter that installs local minecraft-data aliases for missing 26.1 data tables while preserving protocol `775` and dataVersion `4790`.
  - Added Python MineDojo-compatible `EbbGuiEnv`, runtime log parser, server controller, Windows GUI focus/input/screenshot helper, and screenshot color-signal assertions.
  - Added `scripts/gui_e2e_run.py` scenarios: `dry_run`, `runtime_check`, `bot_probe`, and `gui_retest`.
  - Added `scripts/install_gui_automation_deps.py`, `scripts/run_gui_automation_smoke.sh`, and `docs/gui_e2e_automation_2026-06-01.md`.
  - Installed local Node dependencies and Windows Python GUI dependencies using allowed direct installation.
- Verification:
  - `scripts/install_gui_automation_deps.py` → installed Node deps and Windows Python deps; Node self-test passed.
  - `npm --prefix tools/gui_automation/node run self-test` → selected `26.1.2`, protocol `775`, dataVersion `4790`.
  - `scripts/gui_e2e_run.py --scenario dry_run` → passed.
  - `scripts/gui_e2e_run.py --scenario gui_retest --allow-stale-runtime --bot-probe` → generated `build/gui-e2e/gui-retest-report.json`; bot probe correctly reported no server on localhost:25565.
  - `scripts/run_gui_automation_smoke.sh` → passed.
  - `scripts/gui_retest_issue_audit.py --save-path '.../新的世界 (1)' --require-save` → passed.
  - `git diff --check` → no whitespace errors.
  - `scripts/check_pcl_runtime_loaded.py` still reports the known stale runtime log counts `3/1/2/1`, confirming the remaining external relaunch blocker.
  - `scripts/run_smoke_checks.sh` → passed after GUI automation additions; build, DeepResearchSmoke, AttributePointsSmoke, ReviewSmoke, SecondReviewSmoke, ThirdReviewStaticAudit, DeepResearchStaticAudit, GoalStaticAudit, and GuiRetestIssueAudit all passed.
  - Windows dependency sanity check: `windows_gui.py find` via Windows Python succeeded and found the Plain Craft Launcher window.

### Phase 19 GUI Automation Relaunch Pass / Name-Binding Hotfix
- **Status:** in_progress
- **Time:** 2026-06-02 Asia/Shanghai
- Actions taken:
  - User relaunched the separate `26.1.2-Fabric-Ebb-Test` client and entered `新的世界 (1)` via GUI automation.
  - Confirmed the previously refreshed runtime loaded the correct jar/data counts: `dialogues=13`, `block_groups=8`, `entity_bindings=6`, `npc_routines=5`.
  - Fixed GUI automation reliability: direct `py.exe` Windows interop instead of PowerShell command-string regex escaping; clipboard paste instead of per-character typing; `/` command entry for commands; command-screen close logic; better retest viewpoints.
  - Ran GUI retest and verified `/ebb journal`, `/ebb quest`, `/ebb dialogue vars`, and `/ebb vars` no longer show invalid-command errors.
  - Observed role NPC prompt/dialogue prediction still unreliable in the running client when relying only on tag sync; added four role-specific custom-name entity bindings (`*_ebb_npc_name.json`) so dedicated-style clients can predict from synced custom names while server validation remains authoritative.
  - Rebuilt and refreshed the PCL test profile jar with the name-binding hotfix.
- Verification:
  - `scripts/gradle-local.sh --no-daemon test` → BUILD SUCCESSFUL.
  - `scripts/gradle-local.sh --no-daemon build` → BUILD SUCCESSFUL.
  - `scripts/run_smoke_checks.sh` → smoke checks passed and now reports `entity_bindings=10`.
  - `scripts/gui_retest_issue_audit.py --save-path '/mnt/e/MC/PCL/.minecraft/versions/26.1.2-Fabric-Ebb-Test/saves/新的世界 (1)' --require-save` → passed with profile jar hash `03639e9834306a475487dbd32ba7f0079838de3bd2f70bf3a94cf37f08934133`.
  - `scripts/check_pcl_runtime_loaded.py` correctly reports the current running Minecraft process is stale: latest log still has `entity_bindings=6`, expected `>=10`, and latest.log is older than the installed jar.
- Blocker / next action:
  - Full final GUI retest now requires fully closing the current Minecraft process and relaunching `26.1.2-Fabric-Ebb-Test` so the installed jar hash `03639e...` and 10 entity bindings load into memory.

### Phase 19 final GUI visual pass and integrated-server sync fix
- **Status:** complete
- **Time:** 2026-06-02 Asia/Shanghai
- Actions taken:
  - Fully closed the running Minecraft client via Windows GUI automation and relaunched `26.1.2-Fabric-Ebb-Test` from Plain Craft Launcher without touching the vanilla `26.1.2` profile.
  - Entered `新的世界 (1)` via GUI automation and confirmed the refreshed profile jar was loaded in memory.
  - Fixed an integrated-server-only entity-binding sync regression: client INIT clearing of the common `EntityBindingRegistry` wiped the in-process server registry before `EntityBindingSyncPayload` serialization, causing `Entity binding registry synced: 0 binding(s)` in singleplayer. The client now preserves shared integrated-server registries during INIT while still clearing stale synced registries for dedicated clients.
  - Hardened GUI retest automation to use tag-relative role teleports (`execute at @e[tag=ebb.npc.<role>_day]`) instead of stale fixed NPC coordinates, and to close only actual cyan-bordered Ebb screens so normal gameplay does not accidentally open the pause menu.
  - Rebuilt/refreshed the playable PCL profile jar and reran final GUI visual retest.
- Verification:
  - `scripts/gradle-local.sh --no-daemon build` → BUILD SUCCESSFUL after the integrated-server sync fix.
  - `scripts/configure_pcl_test_client.sh` → refreshed separate Fabric test profile.
  - `scripts/gui_retest_issue_audit.py --save-path '/mnt/e/MC/PCL/.minecraft/versions/26.1.2-Fabric-Ebb-Test/saves/新的世界 (1)' --require-save` → passed; profile/build jar hash `23540536daaeda7e054813ee10fa4ce653fca1085876fce058f8a2819e3e3ec3`.
  - `scripts/check_pcl_runtime_loaded.py` → passed with runtime `dialogues=13`, `block_groups=8`, `entity_bindings=10`, `npc_routines=5`.
  - `scripts/gui_e2e_run.py --scenario gui_retest --gui --gui-wait 1.4` → report `build/gui-e2e/gui-retest-report.json`, 127 steps, 0 failures.
  - Final visual contact sheet `build/gui-e2e/contact_dialogues_final_verified.png` shows `/ebb journal`, `/ebb quest`, `/ebb dialogue vars`, `/ebb vars`, four distinct role NPC dialogues (`innkeeper`, `witness`, `tenant`, `guard`), and all eight block-group dialogues (`locked_door`, `counter_ledger`, `notice_board`, `washroom_mirror`, `windowsill_ash`, `tenant_luggage`, `cellar_hatch`, `back_door`).
  - `scripts/run_gui_automation_smoke.sh` → passed.
  - `scripts/run_smoke_checks.sh` → passed, including build, authoring compile, DeepResearchSmoke, AttributePointsSmoke, ReviewSmoke (`entity_bindings=10`), SecondReviewSmoke, ThirdReviewStaticAudit, DeepResearchStaticAudit, GoalStaticAudit, and GuiRetestIssueAudit.
  - `git diff --check` → no whitespace errors.

### Phase 20 / Architecture Plan P20-P21 documentation baseline
- **Status:** complete.
- **Time:** 2026-06-02 Asia/Shanghai
- Actions taken:
  - Read `C:\Users\lanla\Downloads\minecraft_disco_crpg_mod_goal_architecture_plan.md` and identified the new active roadmap P20-P31 plus Alpha 0.1 target.
  - Copied the uploaded plan to root `GOAL.md`.
  - Added `README.md`, `AGENTS.md`, `docs/architecture.md`, `docs/current_status.md`, and `docs/status_reconciliation_2026-06-02.md`.
  - Added supersession notes to historical P2-P8 completion docs so their original GUI-pending text is preserved but reconciled with the final GUI automation evidence.
  - Extended `scripts/goal_static_audit.py` with P20/P21 checks for authoritative docs, version pins, required data directories, artifact status hashes, failure-forward checked choices, and major quest Take-Root consequences.
- Verification so far:
  - `scripts/gradle-local.sh --no-daemon build` -> BUILD SUCCESSFUL.
  - `scripts/gradle-local.sh --no-daemon validateEbbData` -> BUILD SUCCESSFUL.
  - `python3 scripts/goal_static_audit.py` -> passed after adding P20/P21 guardrails.
  - `python3 scripts/deep_research_static_audit.py` -> passed.
  - `python3 scripts/third_review_static_audit.py` -> passed.
  - `scripts/compile_authoring_sources.py --clean` -> compiled generated authoring data with 0 errors.
  - `scripts/gradle-local.sh --no-daemon test` -> BUILD SUCCESSFUL.
  - `scripts/gradle-local.sh --no-daemon validateEbbData` -> BUILD SUCCESSFUL.
  - `scripts/gradle-local.sh --no-daemon runGametestServer --args nogui` -> 6 required GameTests passed.
  - `scripts/run_smoke_checks.sh` -> passed, including updated `GuiRetestIssueAudit`.
  - `scripts/run_gui_automation_smoke.sh` -> passed.
  - `python3 scripts/gui_e2e_run.py --scenario runtime_check` -> passed.
  - `python3 scripts/gui_e2e_run.py --scenario gui_retest --gui --gui-wait 1.4` -> `build/gui-e2e/gui-retest-report.json`, 127 steps, 0 failures.
  - `git diff --check` -> passed.
- Next:
  - Continue with P22 interaction/highlight polish from `GOAL.md`.

### Phase 21 / Architecture Plan P22 interaction-highlight polish
- **Status:** in_progress; code/data/docs/tests complete, GUI relaunch visual proof pending.
- **Time:** 2026-06-02 Asia/Shanghai
- Actions taken:
  - Added `HighlightStyle` with close/far ARGB colors, opacity parsing, `outline` / `merged` / `bounds` render modes, and visual priority.
  - Added highlight style parsing to `BlockGroupDefinition` and `EntityBindingDefinition`.
  - Synced highlight style through `BlockGroupSyncPayload`, `EntityBindingSyncPayload`, and `EntityTargetSyncPayload`; `SyncedEntityTarget` now carries the binding style.
  - Extended `ClientInteractionState` and `ClientTargetDetector` to preserve target prediction reasons and the selected highlight style while still filtering unbound entities and respecting per-binding highlight ranges.
  - Updated `TargetHighlightRenderer` to use synced styles and merge adjacent block-group blocks into cleaner cuboid outlines.
  - Added F3/debug-screen Ebb target reason text in `InteractionPromptHud`, keeping normal play quiet.
  - Added demo highlight style data to `locked_door` and innkeeper bindings and documented highlight authoring fields.
  - Updated `scripts/goal_static_audit.py` with P22 guardrails and made `scripts/run_smoke_checks.sh` ignore stale legacy generated `build/tmp/verify-src` unless explicitly requested.
  - Refreshed the separate PCL test profile with the rebuilt P22 jar.
- Verification:
  - `scripts/gradle-local.sh --no-daemon build` -> BUILD SUCCESSFUL.
  - `scripts/gradle-local.sh --no-daemon validateEbbData` -> BUILD SUCCESSFUL.
  - `scripts/run_smoke_checks.sh` -> passed, including P22 `GoalStaticAudit` and updated `GuiRetestIssueAudit`.
  - `scripts/gradle-local.sh --no-daemon runGametestServer --args nogui` -> 6 required GameTests passed.
  - `git diff --check` -> passed.
  - P22 jar/profile hash: `3c8a3b636e1fb1dcbc9d668b12d446cf002355123b6eac095b3701f4abaae35c`; sources jar hash `321b18fa91be822655bf5d6d340d13652dda311afac590372ef3bee7adb57967`.
- Next:
  - Close/relaunch `26.1.2-Fabric-Ebb-Test` and run GUI visual proof that the refreshed P22 jar is loaded and styled highlights/debug reasons render as expected.

### Phase 21 / P22 runtime relaunch check
- **Status:** pending external GUI relaunch for visual proof only; source/profile verification is complete.
- **Time:** 2026-06-02 Asia/Shanghai
- Evidence:
  - `scripts/check_pcl_runtime_loaded.py` after refreshing the P22 profile reports the new profile jar hash `3c8a3b636e1fb1dcbc9d668b12d446cf002355123b6eac095b3701f4abaae35c`, but `latest.log` is older than the installed jar.
  - Observed data counts are still sufficient (`dialogues=13`, `block_groups=8`, `entity_bindings=10`, `npc_routines=5`), but the runtime log cannot prove the new highlight-style code is loaded until Minecraft is relaunched.
- Next:
  - Close/relaunch `26.1.2-Fabric-Ebb-Test`, enter `新的世界 (1)`, and run GUI/runtime checks for P22 styling.


### Phase 21 / P22 post-HUD readability build and push preparation
- **Status:** source/profile ready for push; GUI visual proof remains pending.
- **Time:** 2026-06-02 Asia/Shanghai
- Actions taken:
  - Split the F3 Ebb target debug overlay into concise reason/style and id/dialogue/match lines to avoid the earlier overlong overlay crossing other F3 text.
  - Rebuilt the mod and refreshed the separate PCL/Fabric test profile.
  - Updated `docs/current_status.md` with the latest P22 build/profile hashes before push.
- Evidence:
  - `build/libs/ebb-0.1.0-dev.jar` SHA-256: `37188b09af534900f2024bd32cc6795e059c5cf319f7edd032dfd1d54a7b2c8d`.
  - `26.1.2-Fabric-Ebb-Test/mods/ebb-0.1.0-dev.jar` SHA-256: `37188b09af534900f2024bd32cc6795e059c5cf319f7edd032dfd1d54a7b2c8d`.
  - `build/libs/ebb-0.1.0-dev-sources.jar` SHA-256: `42473d388a1b577652ee656da6abac5e9f61f2612c4c314953911aa3c034d351`.
- Remaining:
  - Runtime latest.log still needs a fresh close/relaunch into this exact post-HUD P22 jar before final GUI visual proof can close Phase 21.

### Phase 21 / P22 pre-push verification refresh
- **Status:** passed for source/profile push.
- **Time:** 2026-06-02 Asia/Shanghai
- Verification:
  - `python3 scripts/goal_static_audit.py` -> passed with P20/P21/P22 guardrails.
  - `scripts/gradle-local.sh --no-daemon validateEbbData` -> BUILD SUCCESSFUL.
  - `scripts/run_smoke_checks.sh` -> passed, including build, authoring compile, DeepResearchSmoke, ThirdReviewStaticAudit, DeepResearchStaticAudit, GoalStaticAudit, and GuiRetestIssueAudit.
  - `git diff --check` -> passed.
  - Hashes rechecked: build/profile jar `37188b09af534900f2024bd32cc6795e059c5cf319f7edd032dfd1d54a7b2c8d`; sources jar `42473d388a1b577652ee656da6abac5e9f61f2612c4c314953911aa3c034d351`.
- Remaining:
  - This does not close the P22 GUI proof checkbox; a fresh Windows client close/relaunch and visual check are still needed for that item.


### Phase 21 / P22 final GUI proof and overlay placement fix
- **Status:** complete.
- **Time:** 2026-06-02 Asia/Shanghai
- Actions taken:
  - Reviewed the first P22 F3 proof screenshots and found the centered Ebb debug lines still visually collided with vanilla debug text on small-window/auto-GUI-scale clients.
  - Moved the F3-only Ebb diagnostics near the hotbar and clipped lines by pixel width, preserving the normal center interaction prompt.
  - Rebuilt, refreshed the separate Fabric test profile, fully closed/relaunched `26.1.2-Fabric-Ebb-Test`, and entered `新的世界 (1)` for proof against the new jar.
- Verification:
  - `scripts/gradle-local.sh --no-daemon build` -> BUILD SUCCESSFUL.
  - `scripts/configure_pcl_test_client.sh` -> refreshed separate Fabric test profile.
  - `scripts/check_pcl_runtime_loaded.py` -> passed with jar `0d66613edc03b54684e1cca4a7c38d36829899cab06b37e6ff48874cb08ca15e`, `dialogues=13`, `block_groups=8`, `entity_bindings=10`, `npc_routines=5`.
  - Screenshots captured: `build/gui-e2e/p22_locked_door_prompt_proof.png`, `p22_locked_door_f3_debug_proof.png`, `p22_innkeeper_prompt_proof.png`, `p22_innkeeper_f3_debug_proof.png`.
  - Visual review: locked-door block group shows merged cyan outline and `style=merged`; innkeeper bound entity shows outline prompt and `style=outline`; F3 diagnostics are readable near the hotbar and no longer cross the vanilla debug-chart text.
- Next:
  - Start P23 dialogue UI and reading rhythm upgrade.


### Phase 22 / P23 dialogue UI foundation
- **Status:** in_progress; first code/docs/tests slice complete.
- **Time:** 2026-06-02 Asia/Shanghai
- Actions taken:
  - Audited `DialogueScreen`, dialogue payloads, check parsing, roll summaries, and authoring docs against GOAL P23.
  - Added `text_key` fallback via `Component.translatableWithFallback` so missing translations fall back to literal author text.
  - Added current-history focus marker/styling in the scrollable dialogue log.
  - Added keyboard navigation for visible choices: number keys 1-5, Enter, Page/Arrow up/down for pages, and Home/End for history scroll.
  - Added hidden DC / hidden roll display controls to `DialogueCheck`, `VisibleDialogueChoice`, and `RollResultPayload` without changing server-authoritative resolution.
  - Updated authoring docs and `goal_static_audit.py`, and added JUnit coverage for hidden DC/roll player-facing summaries.
- Verification:
  - First compile attempt found Minecraft 26.1.2 `Screen.keyPressed` uses `KeyEvent` instead of the older int/signature; fixed against local deobf API.
  - `python3 scripts/goal_static_audit.py` -> passed, including P23 foundation guardrails.
  - `scripts/gradle-local.sh --no-daemon test` -> BUILD SUCCESSFUL.
  - `scripts/gradle-local.sh --no-daemon build` -> BUILD SUCCESSFUL.
  - `scripts/configure_pcl_test_client.sh` -> refreshed separate Fabric test profile.
  - P23 profile/build jar hash: `42640fc8f6c2fa202b02eb91ae8c11a2cd8b997b757b99b3ec32d868259a0538`; sources jar hash `7b3d802e4bff9bb6fc9718bd9b70deef538e3d21000a0fcd2f4f4f1e96662e61`.
- Remaining:
  - P23 still needs final status/choice overlap proof at multiple GUI scales, a decision/implementation for font scale and text speed settings, and GUI route comfort proof before it can be marked complete.

### Phase 22 / P23 foundation smoke verification
- **Status:** passed; P23 remains in progress.
- **Time:** 2026-06-02 Asia/Shanghai
- Verification:
  - `python3 scripts/goal_static_audit.py` -> passed, including P23 foundation guardrails.
  - `scripts/run_smoke_checks.sh` -> passed, including build, authoring compile, DeepResearchSmoke, ThirdReviewStaticAudit, DeepResearchStaticAudit, GoalStaticAudit, and GuiRetestIssueAudit.
  - `git diff --check` -> passed.
- Note:
  - The separate test profile has the P23 jar on disk, but the currently open Minecraft process was last proven for the P22 jar. Relaunch is required before judging P23 GUI changes in-game.

### Phase 22 / P23 dialogue UI settings and multi-scale proof
- **Status:** code/profile/GUI proof complete; final validation suite pending in current turn.
- **Time:** 2026-06-02 Asia/Shanghai
- Actions taken:
  - Added `ClientDialogueSettings` with profile-local `config/ebb-client.json` persistence for `dialogue_font_scale` and `dialogue_text_speed`.
  - Initialized client settings from `EbbClient` and added dialogue-panel controls: `A-`, `A+`, and text-speed cycling.
  - Scaled/wrapped body/history/status text using the selected font scale, added typed reveal based on text speed, and kept status echoes scissored above the choice buttons.
  - Preserved distinct dialogue/action/thought styling and status colors for roll results, Chime inserts, clue/journal/quest/feat/take-root, and relationship/routine/conflict echoes.
  - Reviewed GUI screenshots and found the settings buttons could visually overlap the dialogue id/node title; fixed the header by clipping the title to a lane beside the settings controls.
  - Rebuilt and refreshed the separate PCL/Fabric test profile with jar `89c54c83518a07493ed21a377a0a4bae3ba4fd4ccfd7c7ba7c86e28c0671a6f9`.
- GUI/runtime evidence:
  - Scale 3 screenshots: `build/gui-e2e/p23_scale3_dialogue_open.png`, `p23_scale3_roll_status.png`, `p23_scale3_settings_changed.png`.
  - Scale 2 screenshots: `build/gui-e2e/p23_scale2_dialogue_open.png`, `p23_scale2_roll_status.png`.
  - Header-fix proof after the refreshed jar: `build/gui-e2e/p23_headerfix_scale2_dialogue_open.png`.
  - `scripts/check_pcl_runtime_loaded.py` passed after relaunch with runtime counts `dialogues=13`, `block_groups=8`, `entity_bindings=10`, `npc_routines=5`.
  - Profile settings proof: `config/ebb-client.json` contains `dialogue_font_scale=1.1` and `dialogue_text_speed=fast` after GUI control use.
- Verification already run before final docs update:
  - `scripts/gradle-local.sh --no-daemon test` -> BUILD SUCCESSFUL.
  - `scripts/gradle-local.sh --no-daemon build` -> BUILD SUCCESSFUL.
  - `scripts/configure_pcl_test_client.sh` -> refreshed separate Fabric test profile.
- Remaining:
  - Re-run post-documentation `goal_static_audit`, data validation, smoke checks, GameTest, and `git diff --check`; then mark P23 complete if all pass.

### Phase 22 / P23 final validation closure
- **Status:** complete.
- **Time:** 2026-06-02 Asia/Shanghai
- Verification:
  - `python3 scripts/goal_static_audit.py` -> passed with P20/P21/P22/P23 guardrails, including font-scale/text-speed settings and scissored status rendering.
  - `python3 scripts/deep_research_static_audit.py` -> passed.
  - `python3 scripts/third_review_static_audit.py` -> passed.
  - `scripts/compile_authoring_sources.py --clean` -> compiled generated authoring data with 0 errors.
  - `scripts/gradle-local.sh --no-daemon test` -> BUILD SUCCESSFUL.
  - `scripts/gradle-local.sh --no-daemon build` -> BUILD SUCCESSFUL.
  - `scripts/gradle-local.sh --no-daemon validateEbbData` -> BUILD SUCCESSFUL.
  - `scripts/run_smoke_checks.sh` -> passed, including GoalStaticAudit and GuiRetestIssueAudit.
  - `scripts/gradle-local.sh --no-daemon runGametestServer --args nogui` -> 6 required GameTests passed; BUILD SUCCESSFUL.
  - `scripts/run_gui_automation_smoke.sh` -> passed.
  - `python3 scripts/gui_e2e_run.py --scenario runtime_check` -> wrote `build/gui-e2e/runtime-check-report.json`.
  - `scripts/check_pcl_runtime_loaded.py` -> passed with jar `89c54c83518a07493ed21a377a0a4bae3ba4fd4ccfd7c7ba7c86e28c0671a6f9` and runtime counts `13/8/10/5`.
  - `git diff --check` -> passed.
- Artifact hashes:
  - build/profile jar: `89c54c83518a07493ed21a377a0a4bae3ba4fd4ccfd7c7ba7c86e28c0671a6f9`.
  - sources jar: `6b9fb41c0052116400a4c1dfee438cfb7904f87d6961bed506714381f4519322`.
- Next:
  - Start P24 authoring and validation hardening.

### Phase 23 / P24 authoring and validation hardening
- **Status:** complete.
- **Time:** 2026-06-02 Asia/Shanghai
- Actions taken:
  - Expanded `docs/json_authoring_guide.md` with complete condition/effect reference tables and P24 validation workflow notes.
  - Added starter JSON Schema files under `docs/schemas/` for dialogues, block groups, entity bindings, and chimes.
  - Hardened `scripts/compile_authoring_sources.py` so bad YAML/JSON emits file:line:column diagnostics.
  - Added `scripts/p24_authoring_validation.py` for cross-registry dialogue/interaction/quest/feat/chime/journal/clue/routine/relationship/scene/conflict reference checks plus high-stakes failure-forward lint.
  - Added `authoring/examples/tavern_case/` with dialogue, block-group interactable, NPC binding, and routine examples; it compiles cleanly.
  - Wired P24 validation, example compilation, and malformed-YAML diagnostic regression into `scripts/run_smoke_checks.sh`.
  - Fixed a real content bug found by the new validator: `witness_intro` had `witness.read` chime tags with no matching chime trigger; `instinct` and `empathy` now include `witness.read`.
- Verification:
  - `python3 scripts/goal_static_audit.py` -> passed with P24 guardrails.
  - `scripts/compile_authoring_sources.py --clean` -> compiled generated authoring data with 0 errors.
  - `scripts/compile_authoring_sources.py --source authoring/examples/tavern_case --out build/generated/ebb_authoring_examples/tavern_case/data/ebb --clean` -> compiled example pack with 0 errors.
  - `scripts/p24_authoring_validation.py` -> passed bundled + generated roots.
  - `scripts/p24_authoring_validation.py build/generated/ebb_authoring_examples/tavern_case/data/ebb` -> passed example pack.
  - `scripts/gradle-local.sh --no-daemon test` -> BUILD SUCCESSFUL.
  - `scripts/gradle-local.sh --no-daemon build` -> BUILD SUCCESSFUL.
  - `scripts/gradle-local.sh --no-daemon validateEbbData` -> BUILD SUCCESSFUL.
  - `scripts/run_smoke_checks.sh` -> passed, including P24 validator/example/diagnostic checks.
  - `scripts/gradle-local.sh --no-daemon runGametestServer --args nogui` -> 6 required GameTests passed; BUILD SUCCESSFUL.
  - `git diff --check` -> passed.
- Artifact hashes:
  - build/profile jar: `2a3cf218c500fc58a2e9c987cd0f423da6c438cbe4d045c79e466d976f43eca9`.
  - sources jar: `6125d476c2b3867630045dee169144702fbb8ebb800d9271ae83a9b970253179`.
- Next:
  - Start P25 Quest Tree / Take Root / Feat maturation.

### Phase 24 / P25 Quest Tree, Take Root, and Feat maturation
- **Status:** complete.
- **Time:** 2026-06-03 Asia/Shanghai
- Actions taken:
  - Upgraded `QuestTreeService` output from a flat list into a branch map with `◆ MAJOR`, `◇ MINOR`, quest state, descriptions, `★ TAKE ROOT` text, grants, and a feat loadout section.
  - Upgraded `QuestTreeScreen` with All/Major/Minor/Feats/Take Root filters and distinct colors for major, minor, Take Root, feat loadout, and active feat lines.
  - Added feat source-quest and check-modifier visibility in the quest/feat view while keeping the existing line-list packet shape.
  - Added category tags to `JournalService` output and All/Clues/Leads/Quests/Scenes filters plus category colors to `JournalScreen`.
  - Added a special `TAKE_ROOT_STATUS_COLOR` for dialogue status echoes beginning with `take_root:`.
  - Added `majorQuestCannotTakeRootTwice` JUnit coverage for major-branch Take Root idempotency and duplicate feat/slot prevention.
- Verification:
  - `python3 scripts/goal_static_audit.py` -> passed with P25 guardrails.
  - `scripts/gradle-local.sh --no-daemon test` -> BUILD SUCCESSFUL.
  - `scripts/gradle-local.sh --no-daemon build` -> BUILD SUCCESSFUL.
  - `scripts/gradle-local.sh --no-daemon validateEbbData` -> BUILD SUCCESSFUL.
  - `scripts/run_smoke_checks.sh` -> passed, including P24/P25 static guardrails and GUI retest issue audit.
  - `scripts/gradle-local.sh --no-daemon runGametestServer --args nogui` -> 6 required GameTests passed; BUILD SUCCESSFUL.
  - `git diff --check` -> passed.
- Artifact hashes:
  - build/profile jar: `12e2019548e6ffb50db6c5f2d661d49fe8264f2ecc2efc858a0ee86f6d46fb5f`.
  - sources jar: `5815ee98f9ac0d26afc9c8e34b73796b17dd18e7c776122a2320d08af8b896f3`.
- Next:
  - Start P26 Chime / inner voice expansion.


### Phase 25 / Architecture P26 Completion Update
- **Status:** complete
- **Time:** 2026-06-03 Asia/Shanghai
- Actions taken:
  - Expanded bundled demo Chimes to eight DND-8 attribute voices: Force, Finesse, Endurance, Logic, Empathy, Rhetoric, Instinct, and Dread.
  - Added `tone_guide`, `active_thoughts`, `one_shot_per_node`, and `one_shot_global` parsing to `ChimeDefinition`.
  - Added server-time cooldown and explicit node/global one-shot anti-spam to `ChimeResolver`, while preserving player `chime:<id>` flags for dialogue conditions.
  - Added `ChimeResolver.explain(...)` and `/ebb dev` snapshot Chime trigger debug lines so OP/dev view lists ready/skip reasons for active dialogue nodes.
  - Added one active thought route per Chime to `ebb:demo/innkeeper_intro`, each gated by the matching chime flag and adding the matching thought id.
  - Updated chime JSON Schema, authoring guide, static audit, smoke checks, JUnit coverage, and current status docs.
  - Rebuilt and refreshed the separate PCL/Fabric test profile with jar `cd3239b53a576cf68599b6886eb32df0af079f3859f74c06bd6b4c32047596a6`.
- Verification:
  - `python3 scripts/goal_static_audit.py` → passed P26 guardrails.
  - `scripts/gradle-local.sh --no-daemon test --tests com.crpg.ebb.DeepResearchDataTest` → BUILD SUCCESSFUL.
  - `scripts/gradle-local.sh --no-daemon build` → BUILD SUCCESSFUL.
  - `scripts/gradle-local.sh --no-daemon validateEbbData` → BUILD SUCCESSFUL.
  - `scripts/run_smoke_checks.sh` → passed including P26 smoke/static checks and GuiRetestIssueAudit.
  - `scripts/gradle-local.sh --no-daemon runGametestServer --args nogui` → BUILD SUCCESSFUL; 6 required GameTests passed.
  - `git diff --check` → passed.
- Next:
  - Start Architecture P27: NPC art, animation, and routine production.


### Phase 26 / Architecture P27 Completion Update
- **Status:** complete
- **Time:** 2026-06-03 Asia/Shanghai
- Actions taken:
  - Added role-aware GeckoLib NPC renderer/model data with temporary humanoid textures for innkeeper, witness, tenant, and guard.
  - Added NPC visual-role persistence/inference from routine/narrative key.
  - Added conversation animation hooks (`talk`, `think`, `dismiss`, `nervous_idle`) and routine `fidget` animation while preserving idle/walk.
  - Added routine validation for allowed actions, path shape, pose names, and animation names; invalid steps are rejected with messages.
  - Added routine debug state and `/ebb routine inspect` output for visual role, action, target, step key, path index, and conversation focus.
  - Hardened dialogue focus so NPCs pause movement, look at the player, set conversation pose/animation, and restore previous routine pose/animation when focus ends.
  - Updated authoring docs, current status, static audits, smoke checks, JUnit coverage, and the separate Fabric test profile.
- Verification:
  - `python3 scripts/goal_static_audit.py` → passed P27 guardrails.
  - `scripts/gradle-local.sh --no-daemon test --tests com.crpg.ebb.DeepResearchDataTest` → BUILD SUCCESSFUL.
  - `scripts/gradle-local.sh --no-daemon build` → BUILD SUCCESSFUL.
  - `scripts/gradle-local.sh --no-daemon validateEbbData` → BUILD SUCCESSFUL.
  - `scripts/run_smoke_checks.sh` → passed including P27 static/smoke checks and GuiRetestIssueAudit.
  - `scripts/gradle-local.sh --no-daemon runGametestServer --args nogui` → BUILD SUCCESSFUL; 6 required GameTests passed.
  - `git diff --check` → passed.
- Artifact hashes:
  - build/profile jar: `5caede905791192f8987eb97927ad5e05d51b3bd74204333e92584864543a225`.
  - sources jar: `23c1578907b68174de0b62db07a9529d29eeebd47028ddd45a310075a43601d7`.
- Next:
  - Start Architecture P28: Investigation and set-piece conflict expansion.

### P28 Validation Note
- **Time:** 2026-06-03 Asia/Shanghai
- Error: Ran `scripts/gradle-local.sh --no-daemon test --tests com.crpg.ebb.DeepResearchDataTest` and `scripts/run_smoke_checks.sh` in parallel. Both tried to own Gradle `:test` binary result files and failed with `NoSuchFileException ... build/test-results/test/binary/*`.
- Resolution plan: do not repeat in parallel; rerun Gradle/JUnit and smoke sequentially after cleaning test result artifacts if needed.

### Phase 27 / Architecture P28 Completion Update
- **Status:** complete
- **Time:** 2026-06-03 Asia/Shanghai
- Actions taken:
  - Formalized set-piece conflicts with phases `setup`, `pressure`, `turn`, `consequence`, `resolution`, phase descriptions, leverage clue ids, and named outcomes.
  - Added `ConflictOutcomeDefinition`, persisted conflict phase helpers, `ConflictService.applyOutcome`, status-line helpers, and known-leverage summaries.
  - Added `apply_conflict_outcome` dialogue effect and client dialogue labels for conflict status/stress/resolve/outcome echoes.
  - Expanded `/ebb dev` snapshots with a conflict phase/status catalog including player status lines, leverage clues, phases, and outcomes.
  - Expanded the hallway confrontation demo with clue-gated routes, clue-modified checks, quiet nonviolent resolution, messy resolution, and two failure-forward outcomes.
  - Added conflict JSON schema, authoring docs, authoring validator outcome-reference checks, static audit guardrails, JUnit, and smoke coverage.
  - Refreshed the separate `26.1.2-Fabric-Ebb-Test` profile with the P28 jar.
- Validation:
  - `python3 scripts/goal_static_audit.py` → passed P28 guardrails.
  - `scripts/gradle-local.sh --no-daemon validateEbbData` → BUILD SUCCESSFUL.
  - `rm -rf build/test-results/test build/reports/tests/test && scripts/gradle-local.sh --no-daemon test --tests com.crpg.ebb.DeepResearchDataTest` → BUILD SUCCESSFUL.
  - `scripts/run_smoke_checks.sh` → passed, including P28 smoke/static checks and GuiRetestIssueAudit.
  - `scripts/gradle-local.sh --no-daemon build` → BUILD SUCCESSFUL.
  - `scripts/gradle-local.sh --no-daemon runGametestServer --args nogui` → BUILD SUCCESSFUL; 6 required GameTests passed.
  - `scripts/configure_pcl_test_client.sh` → profile refreshed; jar SHA-256 `01f617f760d35081afbd9872c3bbecffed71264384c169ddc9dd55474837ac24`.
- Notes:
  - A failed validation attempt came from running Gradle `:test` via JUnit and smoke scripts in parallel, causing both to contend over `build/test-results/test/binary/*`; sequential reruns passed. Avoid parallel Gradle test/smoke invocations.
- Next:
  - Architecture P29 Save/load, multiplayer, and permissions hardening.

### Phase 28 / Architecture P29 Completion Update
- **Status:** complete
- **Time:** 2026-06-03 Asia/Shanghai
- Actions taken:
  - Incremented narrative saved-data schema to v2 and added v1 migration for missing conflict phase states inferred from legacy conflict state and stress/resolve scores.
  - Added JUnit coverage proving legacy saves preserve conflict state/score/scene data, gain conflict phase, retain defaults, and re-encode with the current version.
  - Added dialogue choice packet preflight for conversation UUID, player UUID, and timeout before effects can run.
  - Added same-entity dialogue contention protection (`entity_dialogue_busy`) and diagnostics for spoofed/stale/missing/invalid/unavailable/action-invalid packets.
  - Added dedicated-server missing-client-mod diagnostics for Ebb sync payloads and surfaced them in `/ebb dev` snapshots with dialogue security counters.
  - Added command-permission regression checks preserving OP-only admin commands and player-safe self-inspection commands.
  - Refreshed the separate `26.1.2-Fabric-Ebb-Test` profile with the P29 jar.
- Validation:
  - `python3 scripts/goal_static_audit.py` → passed P29 guardrails.
  - `scripts/gradle-local.sh --no-daemon test --tests com.crpg.ebb.DeepResearchDataTest` → BUILD SUCCESSFUL.
  - `scripts/run_smoke_checks.sh` → passed with P29 static/smoke coverage and GuiRetestIssueAudit.
  - `scripts/gradle-local.sh --no-daemon validateEbbData` → BUILD SUCCESSFUL.
  - `scripts/gradle-local.sh --no-daemon runGametestServer --args nogui` → BUILD SUCCESSFUL; 6 required GameTests passed.
  - `scripts/configure_pcl_test_client.sh` → profile refreshed; jar SHA-256 `9bba5b7f63f2ad696cd8204b5ec6940b4268a92be99f994c903e3792f384da5c`.
- Next:
  - Architecture P30 Vertical slice content expansion.


### Phase 29 / Architecture P30 Completion Update
- **Status:** complete
- **Time:** 2026-06-03 Asia/Shanghai
- Actions taken:
  - Expanded the tavern case into a 3-act vertical slice covering discovery, pressure/investigation, and confrontation/ending.
  - Added four block-group investigation points for 12 total, plus cook/courier NPC routines, role bindings, custom-name fallbacks, dialogues, placeholder textures, and relationship definitions for six role NPCs total.
  - Added two major quest branches, eight minor branches, eight new feats, 16 journal entries, 16 clue definitions, and two additional set-piece conflicts.
  - Expanded all eight Chimes to five passive lines each, reaching 40 total Chime lines.
  - Added trade/mercy ending placeholders so every major route has a concrete placeholder route endpoint.
  - Added docs/current-status authoring notes, static audit guardrails, JUnit content-count coverage, and smoke coverage.
  - Fixed a real P30 authoring-validation bug discovered by smoke: cook/courier dialogue failure effects referenced missing relationship ids, so `ebb:demo/cook` and `ebb:demo/courier` relationships were added.
  - Refreshed the separate `26.1.2-Fabric-Ebb-Test` profile with the P30 jar.
- Validation:
  - `python3 scripts/goal_static_audit.py` → passed P30 guardrails.
  - `scripts/gradle-local.sh --no-daemon test --tests com.crpg.ebb.DeepResearchDataTest` → BUILD SUCCESSFUL.
  - `scripts/run_smoke_checks.sh` → passed with P30 static/smoke coverage and GuiRetestIssueAudit.
  - `scripts/gradle-local.sh --no-daemon validateEbbData` → BUILD SUCCESSFUL.
  - `scripts/gradle-local.sh --no-daemon runGametestServer --args nogui` → BUILD SUCCESSFUL; 6 required GameTests passed.
  - `scripts/configure_pcl_test_client.sh` → profile refreshed; jar SHA-256 `9e0a016757b91f0636814723c9ab0be3b21c71cae5bafac042fab1b074aaa2ed`.
- Artifact hashes:
  - build/profile jar: `9e0a016757b91f0636814723c9ab0be3b21c71cae5bafac042fab1b074aaa2ed`.
  - sources jar: `b39391ab8820808ecf95598752d654dc275ab3d98408fcf92fbdfb3b1c3b9618`.
- Next:
  - Start Architecture P31: release packaging and player documentation.


### Phase 30 / Architecture P31 Completion Update
- **Status:** complete
- **Time:** 2026-06-03 Asia/Shanghai
- Actions taken:
  - Added `docs/installation.md` covering client setup, dedicated server setup, Java 25, Fabric Loader/API, GeckoLib requirements, missing-client diagnostics, troubleshooting, and the separate `26.1.2-Fabric-Ebb-Test` profile workflow.
  - Added `docs/release_metadata_draft.md` with shared alpha release copy and Modrinth/CurseForge draft fields.
  - Added `docs/story_pack_tutorial.md` showing a minimal custom block-group + dialogue + d20 check + failure-forward + journal/clue pack.
  - Added `CHANGELOG.md` for the `0.1.0-dev` alpha vertical-slice state.
  - Added `LICENSE.md` clarifying code/scripts under MIT and bundled story data/placeholder Ebb assets under CC BY-NC-SA 4.0, with third-party/platform content excluded.
  - Updated `README.md`, `docs/current_status.md`, and `scripts/goal_static_audit.py` with P31 guardrails and links.
- Validation:
  - `python3 scripts/goal_static_audit.py` → passed P31 guardrails.
  - `scripts/run_smoke_checks.sh` → passed with P31 static/smoke coverage and GuiRetestIssueAudit.
  - `scripts/gradle-local.sh --no-daemon validateEbbData` → BUILD SUCCESSFUL.
  - `scripts/gradle-local.sh --no-daemon runGametestServer --args nogui` → BUILD SUCCESSFUL; 6 required GameTests passed.
- Notes:
  - P31 only changed docs/static-audit/release packaging files, so the built/profile jar hash remains `9e0a016757b91f0636814723c9ab0be3b21c71cae5bafac042fab1b074aaa2ed`.
- Next:
  - Run final diff/status checks and decide whether to commit/push if requested.


### Phase 31 / Dialogue wait-state and GUI automation hotfix
- **Status:** complete
- **Time:** 2026-06-03 Asia/Shanghai
- Actions taken:
  - Diagnosed the persistent `等待服务器……` screen to a stale in-memory mod jar/classloader state after the test-profile jar was overwritten while Minecraft was still running.
  - Added `ModPackets.handleDialogueChoice(...)` so any unhandled server choice error logs and sends `DialogueClosePayload(..., server_error)` rather than leaving the client waiting.
  - Added `DialogueScreen` waiting timeout recovery and localized timeout text.
  - Added a running-profile guard to `scripts/configure_pcl_test_client.sh`; verified it exits `2` while the matching Minecraft process is open.
  - Updated `scripts/check_pcl_runtime_loaded.py` to fail on stale ZIP/classloader errors and updated runtime-count expectations to `dialogues=19, block_groups=12, entity_bindings=14, npc_routines=7`.
  - Fixed GUI runner setup commands to avoid invalid `oak_sign[facing=south]`, ensure the guestbook block exists as a lectern placeholder, use stable target viewpoints for guestbook/stable mud, and mark block/NPC interactions failed unless a cyan dialogue screen appears.
  - Closed/relaunched the Windows client from the separate `26.1.2-Fabric-Ebb-Test` profile, entered `新的世界 (1)`, and reran full GUI automation.
- Validation:
  - `scripts/configure_pcl_test_client.sh` while Minecraft PID was running → refused with exit code `2`.
  - `scripts/gradle-local.sh --no-daemon build` → BUILD SUCCESSFUL.
  - `scripts/gradle-local.sh --no-daemon validateEbbData` → BUILD SUCCESSFUL.
  - `scripts/run_smoke_checks.sh` → passed after updated audit/docs.
  - `scripts/gradle-local.sh --no-daemon runGametestServer --args nogui` → BUILD SUCCESSFUL; 6 required GameTests passed.
  - `scripts/gui_retest_issue_audit.py --save-path '/mnt/e/MC/PCL/.minecraft/versions/26.1.2-Fabric-Ebb-Test/saves/新的世界 (1)' --require-save` → passed.
  - `scripts/check_pcl_runtime_loaded.py` → passed with jar `954262dea77f6507cffa82ae088e47d69f82979ba9a94917a01d4f61b185723d` and runtime counts `19/12/14/7`; no stale ZIP/class/server task fatal hits.
  - `python3 scripts/gui_e2e_run.py --scenario gui_retest --gui --gui-wait 1.4` → `build/gui-e2e/gui-retest-report.json`, 270 steps, 0 failures.
  - Key GUI evidence: `role_innkeeper_after_choice_1.png`, `block_guestbook_torn_page_dialogue.png`, `block_stable_mud_dialogue.png`.
  - `git diff --check` → passed.
- Artifact hashes:
  - build/profile jar: `954262dea77f6507cffa82ae088e47d69f82979ba9a94917a01d4f61b185723d`.
  - sources jar: `d9c3b9031495b242e0baff913bcd8371bf8773cfc7b38cb645408f0d4e2af83f`.

### PostToolUse hook cwd compatibility fix
- **Status:** complete.
- **Time:** 2026-06-03 Asia/Shanghai.
- Confirmed `/mnt/e/MC/SIMMC2_1-21-8` was missing and likely caused `No such file or directory` hook/tool startup failures.
- Created `/mnt/e/MC/SIMMC2_1-21-8` and symlinked `CRPG_MOD` to `/mnt/e/MC/PCL/CRPG_MOD`. Verified `CRPG_MOD/.kiro/plan` is reachable from the compatibility cwd.

### Phase 32 / K-key Ebb menu and live dialogue background implementation
- **Status:** in progress.
- **Time:** 2026-06-03 Asia/Shanghai.
- Added `EbbMenuScreen` under client GUI menu, opened with default `K`, with Journal/Quest/Attributes/Vars shortcuts and dialogue reading setting controls.
- Added `key.ebb.menu` binding in `ClientKeyMappings` and English/Chinese translations.
- Removed `DialogueScreen.extractTransparentBackground(graphics)` so interaction dialogue leaves the live player view visible outside the panel.
- Added static audit guardrails in `goal_static_audit.py` and `gui_retest_issue_audit.py`; GUI runner now captures `k_menu_open.png` and checks top-band luminance for black-overlay regressions.
- Preliminary build before audit-script changes: `scripts/gradle-local.sh --no-daemon build` succeeded; build jar SHA-256 `25e896a4bc2847d3c941287c356f44eb650bc33c423c317f63567a0f164d2757`.
- Next: rerun build/validate/smoke/GameTest after audit/doc changes, refresh the separate test profile, and run Windows GUI proof.

### Phase 32 / automated validation checkpoint
- **Status:** passed.
- **Time:** 2026-06-03 Asia/Shanghai.
- `python3 -m py_compile scripts/gui_e2e_run.py scripts/gui_retest_issue_audit.py scripts/goal_static_audit.py` → passed.
- `python3 scripts/goal_static_audit.py` → passed including P32 K-menu/live-background guardrails.
- `python3 scripts/gui_retest_issue_audit.py --skip-profile` → passed.
- `scripts/gradle-local.sh --no-daemon build` → BUILD SUCCESSFUL.
- `scripts/gradle-local.sh --no-daemon validateEbbData` → BUILD SUCCESSFUL.
- `scripts/run_smoke_checks.sh` → passed including P32 static/gui guardrails.
- `scripts/gradle-local.sh --no-daemon runGametestServer --args nogui` → BUILD SUCCESSFUL; 6 required GameTests passed.
- `git diff --check` → passed.
- `python3 scripts/gui_e2e_run.py --scenario dry_run` → passed; Node high-version adapter self-test OK.
- Artifact hashes: build jar `25e896a4bc2847d3c941287c356f44eb650bc33c423c317f63567a0f164d2757`; sources jar `7ac5bfe332c460160430979da80afd15d4e9bb692ee7f7afbe266502f245b2eb`.
- Next: close/relaunch or refresh Windows `26.1.2-Fabric-Ebb-Test`, then run GUI proof for K menu and dialogue live background.

### Phase 32 / K-key Ebb menu and live dialogue background completion
- **Status:** complete.
- **Time:** 2026-06-03 Asia/Shanghai.
- Closed the running Windows Minecraft client PID 3432, refreshed the separate `26.1.2-Fabric-Ebb-Test` profile, and relaunched via the captured Java command line.
- Profile jar and build jar now match: `25e896a4bc2847d3c941287c356f44eb650bc33c423c317f63567a0f164d2757`.
- `scripts/check_pcl_runtime_loaded.py` → passed with runtime counts dialogues=19, block_groups=12, entity_bindings=14, npc_routines=7.
- `python3 scripts/gui_e2e_run.py --scenario gui_retest --gui --gui-wait 1.4` → `build/gui-e2e/gui-retest-report.json`, 282 steps, 0 failures.
- P32 GUI evidence: `build/gui-e2e/k_menu_open.png` shows K-menu; `build/gui-e2e/role_innkeeper_dialogue.png` and block dialogue screenshots show visible live-world background outside the dialogue panel.
- `python3 scripts/gui_retest_issue_audit.py --save-path ... --require-save` → passed with matching profile jar.

### Phase 33 / review report intake
- **Status:** started.
- **Time:** 2026-06-03 Asia/Shanghai.
- Read `C:\Users\lanla\Downloads\ebb_codebase_review_report_2026-06-03.md`.
- Mapped actionable H/M/L findings to Phase 33: permissions, active feats, raycast policy, disadvantage/roll breakdowns, checked-choice validation, block-group duplicate/LOS hardening, retryable checks, item/routine semantics, and command architecture split.

### Phase 33 / codebase review remediation completion
- **Status:** complete.
- **Time:** 2026-06-03 Asia/Shanghai.
- Implemented review report remediation items:
  - Added `EbbCommandPermissionGuards` and fixed nested `/ebb dialogue vars <player>` permission guard while preserving player self-inspection commands.
  - Added `FEAT_ACTIVE` / `has_active_feat` semantics backed by `NarrativeSavedData.isFeatActive`.
  - Added `disadvantage`, advantage/disadvantage cancel-out, raw roll fields, and attribute/static/feat/clue roll breakdowns in `RollResultPayload`.
  - Added `pre_effects`, legacy pre-roll `effects` compatibility, `end_on_success` linting, branch-specific pre-effect warnings, and retryable/white-check lock/unlock handling.
  - Added `InteractionRaycastPolicy`, switched client/dev raycasts to the shared collider policy, and hardened server block-group LOS to test nearest block center plus interaction point.
  - Made duplicate block-group membership skip later overlapping groups with validation messages.
  - Hardened NPC routine parsing for nonempty steps, overlapping time windows, and positive teleport distance; added cook/courier role inference and cleaned routine controller indentation.
  - Updated authoring docs, dialogue schema, current status, and P33 static audit/JUnit coverage.
- Validation:
  - `scripts/gradle-local.sh --no-daemon test --tests com.crpg.ebb.DeepResearchDataTest` → BUILD SUCCESSFUL.
  - `scripts/gradle-local.sh --no-daemon validateEbbData` → BUILD SUCCESSFUL.
  - `python3 -m py_compile scripts/goal_static_audit.py` → passed.
  - `python3 scripts/goal_static_audit.py` → passed including P33 guardrails.
  - `scripts/run_smoke_checks.sh` → passed including authoring validation, deep/third/static audits, and GUI issue audit.
  - `scripts/gradle-local.sh --no-daemon runGametestServer --args nogui` → BUILD SUCCESSFUL; 6 required GameTests passed.
  - `scripts/gradle-local.sh --no-daemon build` → BUILD SUCCESSFUL.
  - `git diff --check` → passed.
- Artifact hashes:
  - build jar: `7da6e7148c5cabba5b357ee183fddbfc293a227dd4b7c8520491ad85d15df576`.
  - sources jar: `7848b75ada4f5ff3a53922328a86ad8345bbb51e2bdadc0a6da1117a9bce761b`.
- Note: the separate Windows GUI test profile has not yet been refreshed for this Phase 33 jar because this review remediation did not explicitly request actual client testing; refresh it before GUI/manual testing.

### Phase 34 / PLAN.md LLM NPC + Memory Foundation intake
- **Status:** started.
- **Time:** 2026-06-15 Asia/Shanghai.
- Read `E:\MC\PCL\PLAN.md` completely.
- PLAN defines P34-P43 for server-authoritative LLM NPC free chat, major/minor NPC tiers, long-term memory/facts/conflicts, NPC profiles/knowledge, gateway auth/OpenAI integration, memory safety, dev tools, docs, schemas, static audits, and GUI E2E.
- Implementation strategy for the first incremental phase: do P34 only first, with disabled/fake provider, no OpenAI/network calls, no secrets, payload/UI skeleton, `/ebb llm status`, and automated guardrails.

### Phase 34 / PLAN.md P34 LLM fake-provider foundation completion
- **Status:** complete.
- **Time:** 2026-06-15 Asia/Shanghai.
- Implemented disabled/fake server-side LLM config (`config/ebb-llm-server.json` safe fields only), `LlmGatewayClient`, deterministic `FakeLlmGatewayClient`, `DisabledLlmGatewayClient`, chat request/response/session records, and `LlmChatService` with owner/nonce/session-timeout validation.
- Added LLM chat payloads (`opened`, `message`, `chunk`, `options`, `close`, `cancel`, `error`) and registered server/client networking without OpenAI/API secret fields.
- Added client `NpcChatScreen` skeleton with live in-world background, input box, suggested options, status/error handling, cancel/close behavior, and translations.
- Added `ChoiceType.LLM_CHAT` with `free_chat` alias, `llm` choice settings, dialogue-service LLM chat interception before d20 resolution, and a sample innkeeper free-chat choice.
- Added `/ebb llm status` plus OP `/ebb llm reload_config`.
- Added P34 authoring docs, dialogue schema entries, current-status notes, static audit guardrails, JUnit coverage, and GameTest coverage.
- Validation:
  - `scripts/gradle-local.sh --no-daemon build` → BUILD SUCCESSFUL.
  - `scripts/gradle-local.sh --no-daemon test --tests com.crpg.ebb.DeepResearchDataTest` → BUILD SUCCESSFUL.
  - `scripts/gradle-local.sh --no-daemon validateEbbData` → BUILD SUCCESSFUL.
  - `python3 -m py_compile scripts/goal_static_audit.py` → passed.
  - `python3 scripts/goal_static_audit.py` → passed including P34 guardrails.
  - `scripts/run_smoke_checks.sh` → passed including P34 static/smoke checks.
  - `scripts/gradle-local.sh --no-daemon runGametestServer --args nogui` → BUILD SUCCESSFUL; 7 required GameTests passed.
  - `git diff --check` → passed.
- Artifact hashes:
  - build jar: `1f502a7eadb65cfe2520c7e857b4ee29b842dd786a48d4956c9697b1ac5f157d`.
  - sources jar: `87cadd087bb5403b25b2bc35cc30ce1077cd75daf0c2ec289f5c10ce5564a3c1`.
- Review notes:
  - P34 intentionally does not make real OpenAI/gateway calls; gateway/OAuth/OpenAI remain P36/P37.
  - Debug entity fallback remains disabled; free chat is only reachable through scripted `llm_chat` choices on registered targets.
- Next:
  - Start PLAN.md P35: NPC Profile / Tier / Promotion data layer.


### Phase 35 / PLAN.md P35 NPC Profile, Tier, and Promotion data layer completion
- **Status:** complete.
- **Time:** 2026-06-17 Asia/Shanghai.
- Actions taken:
  - Added `NpcTier`, `NpcProfileDefinition`, `NpcProfileRegistry`, and `NpcPromotionService` under `com.crpg.ebb.npc.profile`.
  - Registered `npc_profiles` as a server-data reload registry and surfaced profile counts/validation in `/ebb data` and dev snapshots.
  - Added six scripted major P30 role profiles for innkeeper, witness, tenant, guard, cook, and courier.
  - Extended entity bindings with `npc_tier`, `npc_profile`, `llm.promote_on_first_chat`, and `llm.profile_seed_archetypes`, including dedicated-server sync payload support.
  - Added explicit opt-in minor villager binding with tag `ebb.npc.minor` plus `ebb:llm/minor_intro` fake/LLM chat entry. Debug entity fallback remains disabled.
  - Added promoted profile persistence in `NarrativeSavedData.promoted_npc_profiles`, bumped schema to v3, added debug/export lines, and wired minor first-chat promotion into `LlmChatService`.
  - Added `/ebb npc profile target`, `/ebb npc profile <npc_key>`, `/ebb npc minorize <entity>`, `/ebb npc promote <entity>`, and `/ebb npc regenerate_profile <npc_key>` dev commands.
  - Added NPC profile schema/docs, P35 static-audit guardrails, JUnit coverage, and GameTest coverage.
- Validation:
  - `scripts/gradle-local.sh --no-daemon compileJava` → BUILD SUCCESSFUL.
  - `scripts/gradle-local.sh --no-daemon test --tests com.crpg.ebb.DeepResearchDataTest` → BUILD SUCCESSFUL.
  - `scripts/gradle-local.sh --no-daemon validateEbbData` → BUILD SUCCESSFUL.
  - `python3 scripts/goal_static_audit.py` → passed including P35 guardrails.
  - `scripts/run_smoke_checks.sh` → passed; smoke registry logs show dialogues=20, block_groups=12, entity_bindings=15, npc_routines=7.
  - `scripts/gradle-local.sh --no-daemon runGametestServer --args nogui` → BUILD SUCCESSFUL; 8 required GameTests passed.
  - `scripts/gradle-local.sh --no-daemon build` → BUILD SUCCESSFUL.
  - `git diff --check` → passed.
- Artifact hashes:
  - build jar: `fdab4338525cb74c42f65485fb305b0712bea467aaea18bc986f721d20ee76e1`.
  - sources jar: `f0aaada7ead57b647afb33b49e4d7cf87bb2b87b278da919caaa78deadc26a0d`.
- Review notes:
  - P35 intentionally keeps real gateway/OAuth work out of the mod jar; P36 begins the separate gateway/auth service.
  - Minor NPC promotion remains opt-in through bindings/tags/dev commands and does not reintroduce “all entities are interactable.”
- Next:
  - Start PLAN.md P36 gateway minimal service + OAuth/OIDC authentication.

### Phase 35 / publish request
- **Status:** publishing.
- **Time:** 2026-06-17 Asia/Shanghai.
- User requested a brief progress report and push to both GitHub and Google Drive.
- Current implementation state remains: P35 complete/validated; next planned phase is P36 gateway minimal service + OAuth/OIDC authentication.

### Phase 36 / PLAN.md gateway auth implementation start
- **Status:** started.
- **Time:** 2026-06-17 Asia/Shanghai.
- Re-read `.kiro/plan/task_plan.md`, recent progress, and `/mnt/e/MC/PCL/PLAN.md` P36.
- Requirement map: add `ebb-llm-gateway/`, `/v1/health`, `/v1/auth/device/start`, `/v1/auth/device/status`, dev local auth, production OAuth/OIDC abstraction, Minecraft `/ebb llm auth/status/logout`, server-only token storage, auth-required chat gating, fake-provider success after login, logout invalidation, and token redaction/no client leakage.

### Phase 36 / implementation checkpoint
- **Status:** implementation complete; validation in progress.
- **Time:** 2026-06-17 Asia/Shanghai.
- Added independent `ebb-llm-gateway/` Java 25 service with `/v1/health`, `/v1/auth/device/start`, `/v1/auth/device/status`, and `/v1/auth/logout`.
- Added gateway `dev_local` auth provider plus generic OIDC device-flow adapter configurable for Keycloak/Auth0/Stytch-style providers.
- Added mod-side server-only auth token storage, dev-local and HTTP gateway auth clients, `/ebb llm auth/status/logout`, and `auth_required` gating in `LlmChatService`.
- Added P36 docs, static-audit guardrails, JUnit/GameTest coverage, and `scripts/p36_gateway_smoke.sh`.
- Checkpoint validation: `scripts/p36_gateway_smoke.sh`, `scripts/gradle-local.sh --no-daemon compileJava`, P36 static audit, and `DeepResearchDataTest` passed before full-suite rerun.


### Phase 36 / PLAN.md Gateway auth completion
- **Status:** complete.
- **Time:** 2026-06-17 Asia/Shanghai.
- Implemented and reviewed the P36 gateway/auth slice.
- Final validation:
  - `scripts/p36_gateway_smoke.sh` → BUILD SUCCESSFUL; `P36 gateway smoke passed`.
  - `scripts/gradle-local.sh --no-daemon test --tests com.crpg.ebb.DeepResearchDataTest` → BUILD SUCCESSFUL.
  - `scripts/gradle-local.sh --no-daemon validateEbbData` → BUILD SUCCESSFUL.
  - `python3 scripts/goal_static_audit.py` → passed including P36 guardrails.
  - `scripts/run_smoke_checks.sh` → passed including P36 gateway smoke/static checks.
  - `scripts/gradle-local.sh --no-daemon runGametestServer --args nogui` → BUILD SUCCESSFUL; 9 required GameTests passed.
  - `scripts/gradle-local.sh --no-daemon build` → BUILD SUCCESSFUL.
  - `git diff --check` → passed.
- Artifact hashes:
  - build jar: `0e22ea2023c8159b6263bd4a6d0e9b9aa474a065d22ece951b25e5e8ddf1eca7`.
  - sources jar: `8bbc5b0c42ce6f59a67c4ad9b1556f0affcdde6ee79e7e7ab32bade68536ccc1`.
- Review notes: auth tokens are held only in server-side maps, `/ebb llm status` uses redacted fingerprints, client LLM UI/networking has no `opaque_player_token`, and fake mode remains network-free after auth.
- Next: PLAN.md P37 OpenAI Responses API integration in the gateway with tests mocked by default.


### Phase 37 / PLAN.md OpenAI Responses gateway chat completion
- **Status:** complete.
- **Time:** 2026-06-17 Asia/Shanghai.
- Implemented P37 gateway chat integration:
  - Added `/v1/chat/message` in `GatewayServer` with `GatewayChatRequest`, `GatewayChatResponse`, server-token validation, timeout handling, and `SimpleCircuitBreaker` failure protection.
  - Added fake, mock OpenAI, and real OpenAI Responses providers. The real provider uses official `com.openai:openai-java:4.39.1`, `ResponseCreateParams`, `ResponseFormatTextJsonSchemaConfig`, `ResponseAccumulator`, and `createStreaming`; tests default to fake/mock and do not consume API quota.
  - Added structured JSON output schema, chunked/streaming response evidence, model config, max output token config, and explicit `store:false` privacy default unless gateway/server config enables storing.
  - Added Minecraft `HttpLlmGatewayClient` and wired `LlmChatService` gateway mode to server-to-gateway `/v1/chat/message`; player auth tokens remain server-only and are never surfaced to client UI.
  - Extended `LlmConfig` with `default_chat_model`, `llm_chat_streaming`, `structured_output`, and `openai_store`.
  - Added P37 docs, static audit guardrails, JUnit coverage, GameTest coverage, and `scripts/p37_gateway_chat_smoke.sh`.
- Validation:
  - `scripts/p37_gateway_chat_smoke.sh` → BUILD SUCCESSFUL; `P37 gateway chat smoke passed`.
  - `scripts/gradle-local.sh --no-daemon compileJava` → BUILD SUCCESSFUL.
  - `scripts/gradle-local.sh --no-daemon test --tests com.crpg.ebb.DeepResearchDataTest` → BUILD SUCCESSFUL.
  - `scripts/gradle-local.sh --no-daemon validateEbbData` → BUILD SUCCESSFUL.
  - `python3 scripts/goal_static_audit.py` → passed including P37 guardrails.
  - `scripts/run_smoke_checks.sh` → passed including P37 gateway smoke/static guardrails.
  - `scripts/gradle-local.sh --no-daemon runGametestServer --args nogui` → BUILD SUCCESSFUL; 10 required GameTests passed.
  - `scripts/gradle-local.sh --no-daemon build` → BUILD SUCCESSFUL.
  - `git diff --check` → passed.
- Artifact hashes:
  - build jar: `a7cf0b8e09a7f48b34d01c7415fc74248fd17bb557ca61f5ac048a4c0c9c875e`.
  - sources jar: `582f62c33b48624b834985a98ff07a4b9b1578569b0bdf5022652364bce8495f`.
- Review notes: P37 is intentionally gateway-only for real OpenAI access; no API key or real provider dependency is added to the Fabric mod jar/client path. Real provider failures resolve to explicit error payloads such as `llm_gateway_error` instead of hanging the Minecraft UI.
- Next: PLAN.md P38 MemoryStore MVP.


### Phase 38 / PLAN.md MemoryStore MVP completion
- **Status:** complete.
- **Time:** 2026-06-17 Asia/Shanghai.
- Implemented P38 memory-store slice:
  - Added H2 gateway DB migration `V001__memory_store.sql` with `schema_migrations`, append-only `memory_records`, `memory_facts`, and `memory_conflicts`.
  - Added gateway memory classes: `MemoryStore`, `MemoryRecord`, `MemoryFact`, `MemoryConflict`, `MemoryEmbeddingService`, `MemoryFactExtractor`, `MemorySearchRequest`, `ScoredMemory`, and append/search/inspect/conflict APIs.
  - Wired `/v1/chat/message` to append player/NPC turns after successful provider responses.
  - Added deterministic embedding write path and hybrid retrieval scoring across keyword, vector, entity/NPC context, and recency.
  - Added deterministic P38 fact extraction (`fact:` / `remember:` / first-person self-description), superseded old facts, and open conflict creation when values change.
  - Added gateway `/v1/memory/search`, `/v1/memory/inspect`, and `/v1/memory/conflicts`.
  - Added Minecraft server-side `MemoryGatewayClient` and OP/dev `/ebb memory search`, `/ebb memory inspect`, and `/ebb memory conflicts` commands.
  - Added P38 docs, static audit guardrails, JUnit coverage, GameTest coverage, and `scripts/p38_memory_smoke.sh`.
- Validation:
  - `scripts/p38_memory_smoke.sh` → BUILD SUCCESSFUL; `P38 memory smoke passed`.
  - `scripts/gradle-local.sh --no-daemon compileJava` → BUILD SUCCESSFUL.
  - `scripts/gradle-local.sh --no-daemon test --tests com.crpg.ebb.DeepResearchDataTest` → BUILD SUCCESSFUL.
  - `scripts/gradle-local.sh --no-daemon validateEbbData` → BUILD SUCCESSFUL.
  - `python3 scripts/goal_static_audit.py` → passed including P38 guardrails.
  - `scripts/run_smoke_checks.sh` → passed including P38 memory smoke/static guardrails.
  - `scripts/gradle-local.sh --no-daemon runGametestServer --args nogui` → BUILD SUCCESSFUL; 11 required GameTests passed.
  - `scripts/gradle-local.sh --no-daemon build` → BUILD SUCCESSFUL.
  - `git diff --check` → passed.
- Artifact hashes after final P38 validation:
  - build jar: `71ed88bf53c0d3cb3d3eab4b4ac1fe6663114e3e55a9e98cd21a364043d78877`.
  - sources jar: `bde884ef67db57f6642e916fe57cfaf0bf3236cec42fdc6fd30d5d5e895df929`.
- Review notes: Memory DB/API lives in the gateway; Minecraft receives only developer command summaries and citation ids. Tests use H2 in-memory databases and fake/mock gateway providers.
- Next: PLAN.md P39 Memory extraction / consolidation.


### Phase 39 / PLAN.md Memory extraction-consolidation implementation checkpoint
- **Status:** implementation complete; validation in progress.
- **Time:** 2026-06-17 Asia/Shanghai.
- Implemented P39 gateway memory consolidation layer:
  - Added `LlmMemoryOperationExtractor` to treat LLM `memory_writes` / structured JSON as proposed memory ops, with deterministic ledger-question and ownership-claim fallback extraction.
  - Added `DeterministicMemoryValidator` with canonical tavern ownership protection. Player claims like “我是旅馆老板” reject `tavern.owner=player:<uuid>` instead of overwriting innkeeper ownership.
  - Added `MemoryOperation`, `MemorySummary`, `MemoryLink`, and `MemorySafetyLesson` persistence via the H2 migration.
  - Added `MemoryConsolidator` background-style summaries, related memory links, A-Mem-like summary evolution on superseded facts while preserving raw episode text, and A-MemGuard-like safety lessons.
  - Extended inspect/search/dev surfaces with raw episodes, extracted facts, operations, summaries, related links, conflicts, `/v1/memory/episodes`, `/v1/memory/lessons`, `/ebb memory episodes`, and `/ebb memory lessons`.
  - Updated gateway smoke to assert canonical owner protection, ledger-question retrieval, raw episode/fact/summary inspectability, and P39 completion output.
- Checkpoint validation:
  - `scripts/gradle-local.sh --no-daemon compileJava` → BUILD SUCCESSFUL for the mod.
  - `scripts/p39_memory_consolidation_smoke.sh` → BUILD SUCCESSFUL; `P39 memory consolidation smoke passed`.
- Next: run full static/JUnit/data/smoke/GameTest/build/diff validation, update docs/current_status hashes, then mark P39 complete if evidence holds.


### Phase 39 / PLAN.md Memory extraction-consolidation completion
- **Status:** complete.
- **Time:** 2026-06-17 Asia/Shanghai.
- Final validation after implementation/review:
  - `scripts/gradle-local.sh --no-daemon compileJava` → BUILD SUCCESSFUL.
  - `scripts/p39_memory_consolidation_smoke.sh` → BUILD SUCCESSFUL; `P39 memory consolidation smoke passed`.
  - `scripts/gradle-local.sh --no-daemon test --tests com.crpg.ebb.DeepResearchDataTest` → BUILD SUCCESSFUL.
  - `python3 scripts/goal_static_audit.py` → passed including P39 guardrails.
  - `scripts/gradle-local.sh --no-daemon validateEbbData` → BUILD SUCCESSFUL.
  - `scripts/run_smoke_checks.sh` → passed including P39 gateway smoke/static guardrails.
  - `scripts/gradle-local.sh --no-daemon runGametestServer --args nogui` → BUILD SUCCESSFUL; 11 required GameTests passed.
  - `scripts/gradle-local.sh --no-daemon build` → BUILD SUCCESSFUL.
  - `git diff --check` → passed.
- Artifact hashes after final P39 validation:
  - build jar: `a6066bb5c44d9b4ea2a05961898dda848a8f82923a4df757caed62a9213002fa`.
  - sources jar: `7536682d771743c89a1a246ef5dce8fea0df2a4687c88a083534b71a0fab99c0`.
- Review notes: P39 keeps raw `memory_records.text` immutable, writes LLM-derived content through explicit operation rows plus deterministic validation, and stores safety lessons instead of trusting canonical-conflicting player claims. The implementation is gateway-owned; Minecraft receives only dev command summaries and raw JSON from gateway endpoints.
- Next: PLAN.md P40 NPC Knowledge Base and story effects.

### Phase 40 / PLAN.md NPC Knowledge Base first-code checkpoint
- **Status:** in progress.
- **Time:** 2026-06-17 Asia/Shanghai.
- Added first draft classes for `NpcKnowledgePackDefinition`, `NpcKnowledgeRegistry`, deterministic `NpcKnowledgeIndex`, and `NpcKnowledgeService`.
- Wired `npc_knowledge_packs` into data reload registries, made `DialogueCondition.parse` reusable for `reveal_conditions`, added story effects `npc_kb_add_fact`, `npc_kb_add_pack`, and `npc_stance_shift`, and passed visible KB context into LLM prompt assembly.
- Fixed a compile issue by using `getOverworldClockTime()` for day-time condition evaluation.
- Checkpoint validation before publishing request:
  - `scripts/gradle-local.sh --no-daemon compileJava` → BUILD SUCCESSFUL.
  - `scripts/p39_memory_consolidation_smoke.sh` → BUILD SUCCESSFUL.
  - `python3 scripts/goal_static_audit.py` → passed including P39 and artifact checks after docs hash update.
  - `scripts/gradle-local.sh --no-daemon validateEbbData` → BUILD SUCCESSFUL.
  - `scripts/gradle-local.sh --no-daemon build` → BUILD SUCCESSFUL.
  - `git diff --check` → passed.
- Remaining P40 work: add demo knowledge pack JSON, `/ebb kb inspect <npc>`, hidden/visible acceptance tests, P40 static audit/docs finalization, GameTest/full smoke rerun.

### Phase 40 / test compile fix
- **Status:** fixing.
- **Time:** 2026-06-17 Asia/Shanghai.
- First P40 JUnit compile attempt failed because the new test used `Locale.ROOT` without importing `java.util.Locale`; added the import and will rerun the test.

### Phase 40 / reveal condition authoring fix
- **Status:** fixing.
- **Time:** 2026-06-17 Asia/Shanghai.
- P40 JUnit exposed that `relationship_min` is not a supported `DialogueCondition` alias; changed witness KB reveal condition to supported `relationship_at_least` with `min`.

### Phase 40 / fake-client answer-change fix
- **Status:** fixing.
- **Time:** 2026-06-17 Asia/Shanghai.
- P40 acceptance test showed mod-side `FakeLlmGatewayClient` still returned the legacy reply without KB visibility signal. Patched it to include `kb=public_only` or `kb=secret_visible` from `knowledgeContext`.


### Phase 40 / PLAN.md NPC Knowledge Base completion
- **Status:** complete.
- **Time:** 2026-06-17 Asia/Shanghai.
- Implemented seven demo NPC knowledge packs, `/ebb kb inspect <npc> [query]`, visible-only prompt assembly, deterministic KB ranking, reveal-conditioned hidden chunks, and story effects `npc_kb_add_fact`, `npc_kb_add_pack`, `npc_stance_shift`.
- Acceptance evidence:
  - Before `ebb:demo/guestbook_gap`, innkeeper prompt context does not include `tenant paid cash`.
  - After the clue, the same question includes the secret chunk and fake reply changes from `kb=public_only` to `kb=secret_visible`.
  - Dev inspection lines list visible and hidden chunks.
- Validation completed so far:
  - `scripts/gradle-local.sh --no-daemon test --tests com.crpg.ebb.DeepResearchDataTest` → BUILD SUCCESSFUL.
  - `scripts/gradle-local.sh --no-daemon validateEbbData` → BUILD SUCCESSFUL.
  - `scripts/gradle-local.sh --no-daemon runGametestServer --args nogui` → BUILD SUCCESSFUL; 12 required GameTests passed.
  - `python3 scripts/goal_static_audit.py` → passed including P40 guardrails.
  - `scripts/run_smoke_checks.sh` → passed.
- Next: final build/diff confirmation and then proceed to PLAN.md P41.

### Phase 40 / final build confirmation
- **Status:** complete.
- **Time:** 2026-06-17 Asia/Shanghai.
- Final confirmation after status updates:
  - `python3 scripts/goal_static_audit.py` → passed including P40 guardrails.
  - `scripts/gradle-local.sh --no-daemon build` → BUILD SUCCESSFUL.
  - `git diff --check` → passed.
  - Artifact hashes: jar `bcc8b8eb179cd3683c33ef70af704a56c482b4f29ae3bdbcde493129b04d3e63`, sources `4b09c667ac467dce360fd7cebe8a5545fd69988ea6306287c826b8110990d13c`.
- Next phase: PLAN.md P41 minor NPC instant generation.


### Phase 41 / PLAN.md minor NPC instant generation checkpoint
- **Status:** implementation complete; validation in progress.
- **Time:** 2026-06-17 Asia/Shanghai.
- Added `NpcProfileGenerator` prompt/schema and deterministic generation of character, stance, knowledge seed, suggested options, and profile generation metadata.
- Added world-hour promotion rate limiting, `/ebb npc review <npc_key>`, `/ebb npc reject_profile <npc_key>`, and expanded generated-profile dev review output.
- Added JUnit and GameTest coverage for rate limit helpers, dev review/reject/regenerate command paths, generated profile fields, and stable existing promoted profiles on repeated promotion attempts.
- Validation so far:
  - `scripts/gradle-local.sh --no-daemon compileJava` → BUILD SUCCESSFUL.
  - `scripts/gradle-local.sh --no-daemon test --tests com.crpg.ebb.DeepResearchDataTest` → BUILD SUCCESSFUL.
  - `scripts/gradle-local.sh --no-daemon runGametestServer --args nogui` → BUILD SUCCESSFUL; 12 required GameTests passed.
- Next: validate data, static audit, smoke runner, build, hash update, and mark Phase 41 complete if evidence holds.


### Phase 41 / stale artifact hash guardrail
- **Status:** fixing.
- **Time:** 2026-06-17 Asia/Shanghai.
- `scripts/run_smoke_checks.sh` reached `GoalStaticAudit` and failed because P41 code changed the jar hash but `docs/current_status.md` still listed the P40 hash. This is the intended artifact-hash guardrail; updated the P41 artifact hashes and will rerun static/smoke.


### Phase 41 / PLAN.md minor NPC instant generation completion
- **Status:** complete.
- **Time:** 2026-06-17 Asia/Shanghai.
- Final validation after hash update:
  - `scripts/gradle-local.sh --no-daemon validateEbbData` → BUILD SUCCESSFUL.
  - `python3 scripts/goal_static_audit.py` → passed including P41 guardrails.
  - `scripts/run_smoke_checks.sh` → passed.
  - `scripts/gradle-local.sh --no-daemon build` → BUILD SUCCESSFUL.
  - `git diff --check` → passed.
  - Artifact hashes: jar `416f0fb73ca92a1b3dc6861d96797a055eae4a254d8be670e8e35f66ca39cd5e`, sources `2491d34a2fab835948d46e4f7f127dfc763dcfd675aa1e169e085e40da1185c2`.
- Next phase: PLAN.md P42 LLM Chat UI completion.

### Push Snapshot: P40/P41 LLM NPC Knowledge and Minor NPC Generation
- **Status:** prepared for GitHub + Google Drive push
- **Time:** 2026-06-17 Asia/Shanghai
- Actions taken:
  - Re-read planning files and verified the current active state: Phase 41 complete; Phase 42 LLM Chat UI completion is next.
  - Verified local prerequisites: GitHub CLI authenticated, `origin` points to `YJLi-new/MC-CRPG-MOD`, Drive push sync mapping is active for this repository.
  - Re-ran `git diff --check` and `scripts/run_smoke_checks.sh` before commit/push.
- Verification:
  - `git diff --check` → pass.
  - `scripts/run_smoke_checks.sh` → pass; includes Gradle build, gateway smoke checks, authoring validation, static audits, deep-research smoke, goal static audit through P41, and GUI retest issue audit.
