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
