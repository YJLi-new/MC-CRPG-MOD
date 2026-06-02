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
