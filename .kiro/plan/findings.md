# Findings & Decisions

## Requirements
- Use persistent file-based planning via `planning-with-files`.
- Keep generated/output project files under `E:\MC\SIMMC2_1-21-8\CRPG_MOD` unless doing actual client testing.
- Use the project plan at `CRPG_MOD/docs/minecraft_disco_like_crpg_mod_plan.md` as the architectural baseline.
- Use `.minecraft/versions/26.1.2` as the test client to inspect/target.
- Project target: Minecraft Java Edition 26.1.2, Fabric, Java, JDK 25.

## Research Findings
- `CRPG_MOD` initially contained documentation only; no Fabric/Gradle project skeleton was present.
- Test version directory `.minecraft/versions/26.1.2` contains `26.1.2.jar`, `26.1.2.json`, `PCL/Setup.ini`, and `resourcepacks/`.
- PCL setup marks `26.1.2` as vanilla/original, with empty Fabric/Forge/NeoForge fields.
- `26.1.2.json` reports `javaVersion.majorVersion: 25`, `mainClass: net.minecraft.client.main.Main`, `assets: 30`, and 107 libraries.
- The `26.1.2.jar` is vanilla; no `fabric.mod.json` was found inside it.
- Windows-applicable libraries for 26.1.2 are present; Linux-native libraries are not complete, so direct WSL client launching would need extra native libraries.
- `.minecraft/mods` existed and was empty at preflight.
- Fabric Loader jars exist in `.minecraft/libraries` for `0.16.10`, `0.17.2`, and `0.18.4`, but the 26.1.2 profile itself is not Fabric-enabled.
- Current WSL shell had OpenJDK 8 runtime only; `javac` and `gradle` were not found.

## Technical Decisions
| Decision | Rationale |
|----------|-----------|
| Use `CRPG_MOD/.kiro/plan` for planning files. | Satisfies both the skill workflow and the user's output-location constraint. |
| Treat the current 26.1.2 PCL profile as a vanilla test baseline, not yet a modded Fabric runtime. | Inspected `Setup.ini` and jar/profile contents show no active Fabric profile. |
| Start with Phase 0/1 before copying anything into `.minecraft`. | Avoids modifying the test client before an actual client test is requested/needed. |

## Issues Encountered
| Issue | Resolution |
|-------|------------|
| Local shell lacks JDK 25/Javac/Gradle. | Record as environment readiness task; install/provision official JDK/Gradle wrapper when implementation begins. |
| Test client is vanilla, not Fabric-enabled. | Record as future testing setup task; use Fabric profile/loader only when ready for actual client testing. |

## Resources
- Project plan: `CRPG_MOD/docs/minecraft_disco_like_crpg_mod_plan.md`
- Preflight notes: `CRPG_MOD/docs/assistant_preflight_notes.md`
- Planning files: `CRPG_MOD/.kiro/plan/task_plan.md`, `findings.md`, `progress.md`
- Test client profile: `.minecraft/versions/26.1.2/26.1.2.json`
- Test client setup: `.minecraft/versions/26.1.2/PCL/Setup.ini`

## Visual/Browser Findings
- No visual/browser findings yet.

---
*Update this file after every 2 view/browser/search operations or important discoveries.*

## Research Findings Update — Environment Sources (2026-05-30)
- Official Adoptium docs provide a stable API URL pattern for latest GA Temurin binaries, including Java major version `25`, OS `linux`, arch `x64`, image type `jdk`, and recommend SHA-256 verification.
- Official Gradle releases page is the canonical source for current/past Gradle binary ZIPs.
- Fabric Loom documentation for Minecraft 26.1.2 states Loom is the Gradle plugin for Fabric mod development and handles downloading/processing Minecraft jars, mappings, dependencies, and assets.
- Fabric 26.1 porting docs say 26.1 uses unobfuscated/Mojang official names, requires Java compatibility 25, uses plugin id `net.fabricmc.fabric-loom`, removes the mappings dependency line, and replaces `modImplementation` with standard `implementation` for 26.1+.
- Modrinth Fabric API page confirms Fabric API is a normal Fabric mod that requires Fabric Loader and goes in the `mods` folder for playing.

## Research Findings Update — Version Pins (2026-05-30)
- Fabric meta API for Minecraft `26.1.2` lists Fabric Loader `0.19.2` as stable, with `0.19.1` and earlier returned as non-stable in the first results.
- Fabric API Maven metadata latest matching `26.1.2` is `0.150.0+26.1.2`; overall latest at query time was `0.150.1+26.2`, so pin to the `26.1.2` line.
- Fabric Loom Maven metadata latest/release at query time was `1.17.0-alpha.13`; 26.1+ docs require plugin id `net.fabricmc.fabric-loom`.
- GeckoLib for Fabric `26.1.2` exists as `5.5.1`; Cloudsmith Maven coordinates are `com.geckolib:geckolib-fabric-26.1.2:5.5.1` from `https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/`.


## Installation Results — 2026-05-30
- Installed Eclipse Temurin JDK `25.0.3+9-LTS` under `.tools/jdk-25` and verified `java`/`javac` report version 25.
- Installed Gradle `9.5.1` under `.tools/gradle-9.5.1` and verified it runs with the local JDK 25 when `scripts/env.sh` is sourced.
- Created `scripts/install_env.sh`, `scripts/env.sh`, `scripts/env.ps1`, and `scripts/gradle-local.sh` for reproducible local environment setup.
- Created Fabric Loom Gradle project files: `settings.gradle`, `build.gradle`, `gradle.properties`, `gradlew`, `gradlew.bat`, and `gradle/wrapper/*`.
- Pinned dependencies in `gradle.properties`: Minecraft `26.1.2`, Fabric Loader `0.19.2`, Fabric API `0.150.0+26.1.2`, Loom `1.17.0-alpha.13`, GeckoLib `5.5.1`.
- Created minimal common/client Fabric entrypoints: `com.crpg.ebb.EbbMod` and `com.crpg.ebb.client.EbbClient`.
- `scripts/gradle-local.sh --no-daemon build` completed successfully and produced `build/libs/ebb-0.1.0-dev.jar` plus sources jar.
- Dependency resolution confirmed runtime classpath contains Fabric Loader, Fabric API `0.150.0+26.1.2`, and GeckoLib `com.geckolib:geckolib-fabric-26.1.2:5.5.1`.
- The test client directory was not modified and no jar was copied to `.minecraft/mods`, because this was environment setup rather than an actual client test.

## Pending Confirmations Review — 2026-05-30
Resolved from original key questions:
- Version pins are now selected and build-verified: Minecraft `26.1.2`, Fabric Loader `0.19.2`, Fabric API `0.150.0+26.1.2`, Loom `1.17.0-alpha.13`, GeckoLib `5.5.1`.
- WSL build toolchain is now project-local under `CRPG_MOD/.tools`.

Still needs explicit or implicit confirmation:
- Whether to keep the initial MVP limited to vanilla entity/block targets before adding custom GeckoLib NPCs. Current recommended default: yes.
- How to Fabric-enable the PCL test client when actual client testing begins. Current recommended default: create a separate Fabric 26.1.2 profile rather than modifying the vanilla `26.1.2` profile in place.
- Whether `mod_id = ebb`, package `com.crpg.ebb`, and display name `Esoteric Ebb CRPG` are final or placeholders.
- Whether the interaction key default should be `X`. Current recommended default: `X`.
- Whether GeckoLib should remain a hard dependency from the beginning, or be kept as a declared dependency but unused until Phase 7. Current implemented state: hard dependency declared.
- First playable MVP content target: the plan suggests the “旅馆走廊” scene with boss/door/ledger/cat; this still needs confirmation before authoring sample JSON/content.
- How aggressive client testing should be: build-only for now, or soon copy built jars into `.minecraft/mods` and create/use a Fabric client profile.


## User Confirmations — 2026-05-30
- Confirmed: when actual client testing begins, create/use a separate Fabric 26.1.2 profile instead of modifying the vanilla `26.1.2` profile in place.
- Confirmed: initial MVP should use vanilla entity/block targets first; custom GeckoLib NPCs/routines are later-phase work.
- Confirmed: current naming is temporary: `mod_id=ebb`, package `com.crpg.ebb`, display name `Esoteric Ebb CRPG`.
- Confirmed: default interaction key is `X`.
- Confirmed: GeckoLib is accepted as a hard dependency from the beginning.
- Confirmed: first playable sample content should use the plan's “旅馆走廊” scene.

## API Inspection Findings — 2026-05-30 Phase 1
- Command API class is `net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback`; callback signature is `register(CommandDispatcher<CommandSourceStack>, CommandBuildContext, Commands.CommandSelection)`.
- Minecraft 26.1.2 mapped identifier class is `net.minecraft.resources.Identifier` rather than older `ResourceLocation`; use `Identifier.fromNamespaceAndPath(namespace, path)`.
- Fabric resource loader v1 package is now under `net.fabricmc.fabric.api.resource.v1`; `ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(Identifier, PreparableReloadListener)` is available.
- Vanilla JSON reload helper exists as `net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener<T>` with constructor `(Codec<T>, FileToIdConverter)`; `FileToIdConverter.json("dialogues")` maps `data/<namespace>/dialogues/*.json` style files.
- `CommandSourceStack.sendSuccess(Supplier<Component>, boolean)` and `Component.literal(String)` are available for base command feedback.


## Phase 1 Completion Evidence — 2026-05-30
- Implemented `/ebb`, `/ebb status`, and `/ebb data` server commands in `src/main/java/com/crpg/ebb/registry/ModCommands.java`.
- Implemented generic JSON data reload registry/listener in `JsonDataRegistry` and registered five server-data reload listeners via `NarrativeDataRegistries`: dialogues, interactions/block_groups, interactions/entity_bindings, npc_routines, attributes.
- Hooked command and reload listener registration from `EbbMod.onInitialize()`.
- Verification command `scripts/gradle-local.sh --no-daemon build` completed successfully after the changes.
- Jar inspection shows the generated jar contains `ModCommands`, `JsonDataRegistry`, `NarrativeDataRegistries`, `EbbMod`, `EbbClient`, lang files, and `fabric.mod.json`.

## Phase 2 Foundation Findings — 2026-05-30
- Added interaction target model classes: `InteractionTarget`, `InteractionTargetType`, `EntityTarget`, and `BlockGroupTarget`.
- Added typed block group parsing/index foundation: `BlockGroupDefinition` and `BlockGroupIndex`.
- `NarrativeDataRegistries.BLOCK_GROUPS` now observes raw JSON reloads and rebuilds `BlockGroupIndex` after block-group JSON is loaded.
- Build issue encountered: `ChunkPos` in 26.1.2 is a record with constructor `(int x, int z)` and static `containing(BlockPos)`; direct `new ChunkPos(BlockPos)` is invalid.

## API Inspection Findings — 2026-05-30 Phase 2 Detection/Validation
- Client `Minecraft` exposes `player`, `level`, `hitResult`, `setScreen`, and private `pick`; custom detector should do its own ray/selection logic rather than relying on private pick.
- `Entity`/`Player` expose `getEyePosition()`, `getViewVector(float)`, `pick(double,float,boolean)`, `getBoundingBox()`, `getUUID()`, `isSpectator()`, `level()`, and distance helpers.
- `Level`/`BlockGetter` expose `clip(ClipContext)` and entity queries `getEntities(Entity, AABB, Predicate)`; `ProjectileUtil.getEntityHitResult(...)` is available for vanilla entity ray hits.
- Hit-result classes expose `HitResult.getLocation()`, `BlockHitResult.getBlockPos()`, and `EntityHitResult.getEntity()`.
- Server validation can access dimensions via `Level.dimension()`, levels via `MinecraftServer.getLevel(ResourceKey<Level>)`, and entities by UUID via `ServerLevel.getEntityInAnyDimension(UUID)`.


## Phase 2 Completion Evidence — 2026-05-30
- Implemented client-side crosshair target detection foundation in `ClientTargetDetector`; it runs every 2 ticks via `ClientTickEvents.END_CLIENT_TICK`, pauses when a screen is open, raycasts up to 10m, checks block-group hits through `BlockGroupIndex`, and checks vanilla entity hits through `ProjectileUtil.getEntityHitResult`.
- Implemented `ClientInteractionState` to retain the current predicted target, distance, interaction-range state, line-of-sight flag, and reason for later HUD/highlight use.
- Implemented server-authoritative validation foundation in `InteractionService`: validates block groups and entities for spectator status, dimension, 2m distance, target existence/usability, and block line-of-sight via `ClipContext`.
- Verification command `scripts/gradle-local.sh --no-daemon build` completed successfully after Phase 2 additions.
- Jar inspection shows generated jar contains client detector/state classes and common interaction validation/model/index classes.
- No `.minecraft` files or vanilla `26.1.2` profile files were modified.

## API Inspection Findings — 2026-05-30 Phase 3 Client/Networking
- Key binding API in 26.1.2 uses `net.minecraft.client.KeyMapping` with `KeyMapping.Category`; custom categories can be registered by `KeyMapping.Category.register(Identifier)`. `KeyMapping.consumeClick()` is available for edge-triggered interaction input.
- Fabric HUD API uses `net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry` and `HudElement.extractRenderState(GuiGraphicsExtractor, DeltaTracker)`. `GuiGraphicsExtractor` supports `guiWidth()`, `guiHeight()`, `fill`, `outline`, `text`, and `centeredText` for prompt rendering.
- Fabric level rendering events are under `net.fabricmc.fabric.api.client.rendering.v1.level`. `LevelRenderEvents.BEFORE_GIZMOS`/`AFTER_TRANSLUCENT_TERRAIN` provide a `LevelRenderContext` with `poseStack()` and `bufferSource()`.
- Minecraft 26.1.2 render types are under `net.minecraft.client.renderer.rendertype`; line rendering can use `RenderTypes.linesTranslucent()` or `RenderTypes.lines()`. `ShapeRenderer.renderShape(...)` and `Shapes.block()`/`Shapes.create(AABB)` are available for outline-style highlight rendering.
- `GameRenderer.getMainCamera()` returns the active camera; `Camera.position()` provides world-space camera position for translating target shapes into render coordinates.
- Fabric networking uses `PayloadTypeRegistry.serverboundPlay()/clientboundPlay()`, `ServerPlayNetworking.registerGlobalReceiver`, `ClientPlayNetworking.send/registerGlobalReceiver`, and `CustomPacketPayload.Type<T>`. `FriendlyByteBuf`/`RegistryFriendlyByteBuf` expose `read/writeIdentifier`, `read/writeUUID`, `read/writeBlockPos`, `read/writeUtf`, `read/writeEnum`, and booleans for compact custom codecs.


## Phase 3 Completion Evidence — 2026-05-30
- Implemented default `X` key binding with `KeyMapping.Category.register(EbbMod.id("controls"))` and `KeyMappingHelper.registerKeyMapping`.
- Implemented HUD prompt rendering through `HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, ...)`; prompt is only drawn when `ClientInteractionState` reports a target within the 2m interaction range.
- Implemented level highlight rendering through `LevelRenderEvents.BEFORE_GIZMOS`, `RenderTypes.linesTranslucent()`, and `ShapeRenderer.renderShape` for entity AABBs and block-group block outlines/bounds.
- Implemented server-authoritative play networking: client sends `InteractionRequestPayload`, server validates with `InteractionService`, then replies with `InteractionDeniedPayload` or `OpenDialoguePayload`.
- Client-side open-dialogue response is a placeholder system message until Phase 4 screen/runtime is implemented.
- Build evidence: `scripts/gradle-local.sh --no-daemon build` completed successfully after fixes.
- Jar evidence: `build/libs/ebb-0.1.0-dev.jar` contains Phase 3 key/render/network classes and updated lang files.
- No client profile or `.minecraft` file was changed during Phase 3 verification.

## API Inspection Findings — 2026-05-30 Phase 4 Dialogue UI
- Minecraft 26.1.2 `Screen` rendering uses `extractRenderState(GuiGraphicsExtractor, int mouseX, int mouseY, float tickDelta)` instead of older `render(DrawContext, ...)` style methods.
- `Screen` still provides protected widget helpers: `addRenderableWidget`, `clearWidgets`, and `init()` for building button-based UI.
- `Button.builder(Component, Button.OnPress)` is available; `Button.OnPress` receives the pressed `Button` instance.
- `GuiGraphicsExtractor.textWithWordWrap`, `fill`, `outline`, `text`, and `centeredText` are sufficient for a simple dialogue panel and choice list.


## Phase 4 Completion Evidence — 2026-05-30
- Implemented dialogue JSON v0 parser/validator for `id`, `start`, `nodes`, `speaker`, `text`, `choices`, choice `type`, `next`, and check metadata fields.
- `DialogueRegistry` is rebuilt from `NarrativeDataRegistries.DIALOGUES` during `/reload`; parser exceptions/invalid content become validation messages.
- `DialogueService` creates UUID conversation sessions server-side, ensures one active session per player, advances branches by choice id, and closes terminal conversations.
- Network payloads now cover open, update, close, choose option, and close request. `OpenDialoguePayload` carries initial node text and visible choices rather than only a placeholder id.
- `DialogueScreen` uses the 26.1.2 `Screen.extractRenderState` API and styled choice buttons: dialogue white quoted text, action gold parenthesized text, thought aqua italic bracketed text.
- Added bundled sample dialogues: `ebb:debug/entity` for vanilla entity interaction testing and `ebb:demo/locked_door_dialogue` for the confirmed inn-corridor scene.
- Verification command `scripts/gradle-local.sh --no-daemon build` completed successfully after Phase 4 changes.
- Parser smoke test validates the sample debug entity dialogue and confirms invalid empty-node JSON returns a validation message instead of being accepted silently.
- Jar inspection shows dialogue runtime, payload, screen classes, and sample dialogue JSON resources in `build/libs/ebb-0.1.0-dev.jar`.

## API Inspection Findings — 2026-05-30 Phase 5 Saved Data
- Minecraft 26.1.2 saved data uses `net.minecraft.world.level.saveddata.SavedDataType<T>` with a `Codec<T>` rather than older `SavedData.Factory` save/load methods.
- `ServerLevel.getDataStorage()` returns `net.minecraft.world.level.storage.SavedDataStorage`, which exposes `computeIfAbsent(SavedDataType<T>)`.
- `MinecraftServer.overworld()` is available; the CRPG narrative saved data uses overworld data storage as the current global world-state location.
- `CompoundTag` accessors now commonly return `Optional` variants, but the implemented saved data path uses codecs instead of manual NBT methods.
- `Entity.getRandom()` exposes `RandomSource`, used for authoritative server-side d20 rolls.


## Phase 5 Completion Evidence — 2026-05-30
- Implemented typed attribute registry with JSON-backed definitions and sample `force`, `logic`, and `empathy` attributes.
- Dialogue checks are now resolved on the server using `d20 + attributeScore` and support configured success/failure plus critical success/failure branches.
- `DialogueUpdatePayload` carries optional `RollResultPayload`; `DialogueScreen` displays roll summaries after checked choices.
- Added dialogue conditions/effects: player/world flags, attribute setting, item placeholders (`give_item`/`take_item` aliases), and routine placeholders.
- Implemented codec-based `NarrativeSavedData` in overworld data storage for player attributes/flags and world flags.
- Smoke test confirms sample attribute loading, locked-door check/effect/condition parsing, condition evaluation before/after a flag, in-memory attribute override, and saved-data codec round trip.
- Verification command `scripts/gradle-local.sh --no-daemon build` completed successfully; jar inspection confirms Phase 5 classes/resources are packaged.

## API Inspection Findings — 2026-05-30 Phase 6 Developer Commands
- `CommandSourceStack` in 26.1.2 exposes `permissions()` rather than the older `hasPermission(int)` helper.
- Fabric permission API v1 provides `PermissionPredicates.require(Identifier, PermissionLevel)`; use this in Brigadier `.requires(...)` for OP-style command gating.
- Minecraft permission levels include `GAMEMASTERS`, `ADMINS`, and `OWNERS`; Phase 6 dev commands use `GAMEMASTERS` as the current OP-compatible default.


## Phase 6 Completion Evidence — 2026-05-30
- `/ebb dev` is registered under an OP-style permission predicate (`ebb:command.dev`, defaulting to `PermissionLevel.GAMEMASTERS`).
- `DevSnapshotPayload` and `DevSnapshotScreen` provide a client-side developer browser screen with server snapshot lines and dynamic current-target debug info.
- Dev snapshot content includes raw registry counts, typed dialogue/attribute/block-group summaries, validation messages, active dialogue session count, narrative saved-data summary, and online player count.
- `/ebb dev summary` provides a text fallback for console or clients that cannot receive the dev snapshot payload.
- Added sample block-group content `data/ebb/interactions/block_groups/demo/locked_door.json` for the locked-door dialogue.
- `docs/mvp_verification_steps.md` documents build/smoke/manual verification without modifying the vanilla `26.1.2` profile.
- Final verification: `scripts/gradle-local.sh --no-daemon build` succeeded; both smoke checks passed; jar inspection confirms dev snapshot classes/resources and sample content are packaged.


## Review Remediation Findings — 2026-05-30
- Read `C:\Users\lanla\Downloads\ebb_project_review.md` and implemented the requested hardening/remediation set in the active checkout `/mnt/e/MC/PCL/CRPG_MOD`.
- Dedicated-server block groups now sync with `BlockGroupSyncPayload`; client detection uses `ClientBlockGroupIndex`, not the server-data `BlockGroupIndex`.
- Entity binding registry now parses `interactions/entity_bindings` and resolves server-side entity dialogue/range by UUID/tag/name/entity type/priority; debug fallback is data-configurable through interaction settings and disabled for bundled demo content.
- `/ebb dev` now includes complete dialogue tree dumps with nodes, choices, conditions, effects, checks, and outcome effects, plus entity bindings and NPC routines.
- Dialogue checks now support `success_effects`, `failure_effects`, `critical_success_effects`, and `critical_failure_effects`; nodes support `enter_effects`.
- Dialogue sessions now close on disconnect/leave/respawn/level change/server stop, expire after inactivity, and revalidate ACTION choice target LOS/range before applying effects.
- Packet list decoders now reject bad counts; missing dialogue references are hard-invalid; attribute defaults clamp to min/max; block groups support optional block predicates.
- Added `ebb:npc` GeckoLib MVP skeleton with renderer/assets, `/ebb summon_npc <routine>`, typed routine registry, basic stand/walk/look-at-player controller, and routine effect wiring for interacted Ebb NPCs.
- Verification: `scripts/gradle-local.sh --no-daemon build` succeeded; `ReviewSmoke` passed for registries/effects/hard-invalid refs; jar inspection confirmed new classes/resources.

- Additional review-remediation verification: `runServer --args nogui` loaded Minecraft 26.1.2, Fabric Loader 0.19.2, Fabric API 0.150.0+26.1.2, GeckoLib 5.5.1, and `ebb`; the dev server stopped at the normal EULA gate rather than an Ebb initialization crash.

## Playable Client Setup Findings — 2026-05-30
- The actual PCL Minecraft directory in this environment is `/mnt/e/MC/PCL/.minecraft`; `/mnt/e/MC/SIMMC2_1-21-8/.minecraft` is absent.
- The vanilla `26.1.2` profile is at `/mnt/e/MC/PCL/.minecraft/versions/26.1.2` and remains preserved.
- A new independent PCL profile `26.1.2-Fabric-Ebb-Test` was created with Fabric Loader `0.19.2`, Fabric API `0.150.0+26.1.2`, GeckoLib `5.5.1`, and the current Ebb mod jar.
- PCL Fabric profiles in this install use full version JSON plus `PCL/Setup.ini`; the test profile follows that pattern rather than modifying the vanilla profile.

## DND-8 Attribute Points Findings — 2026-05-30
- Implemented the project's first player-facing point allocation feature as eight DND-like dimensions: strength, dexterity, constitution, intelligence, wisdom, charisma, perception, and luck.
- The implementation intentionally stores scores as direct d20 modifiers in the existing CRPG check model rather than D&D ability scores; one unspent point raises one modifier by +1.
- Legacy dialogue/content keys remain compatible through aliases: force, logic, and empathy map to strength, intelligence, and charisma respectively.
- Current debug/admin commands operate on the invoking player only; target-player administration can be added later if needed.

## Finding: Dialogue roll/status text must share layout with choice buttons
- **Date:** 2026-05-30
- **Context:** Screenshot after a successful checked dialogue choice showed status text bleeding through the choice button row.
- **Decision:** Avoid fixed Y coordinates for dynamic dialogue UI sections. Derive body, status, choices, and done button Y positions from one panel layout; scissor status text above choices.

## Finding: Entity fallback binding made every pickable entity look interactable
- **Date:** 2026-05-30
- **Context:** Client entity raycast highlighted all pickable entities because unmatched entities resolved to a debug fallback binding.
- **Decision:** Explicit entity bindings are now required for Ebb entity highlight/prompt. Unbound entities return no binding client-side and are denied as `unbound_entity` server-side.

## Finding: Second review release semantics and dedicated-client prediction
- **Date:** 2026-05-30
- **Context:** `ebb_project_review_2026-05-30_second.md` flagged debug fallback release semantics, entity binding client prediction, silent block-group sync truncation, and routine skeleton limits.
- **Decision:** Add `InteractionSettings`, default demo fallback off, sync entity bindings/settings to clients, hard-invalidate oversized block groups, and support sequential routine waypoints while documenting remaining full-NPC work as future iteration.


## Finding: Tag-based entity bindings need matched-entity UUID sync
- **Date:** 2026-05-31
- **Context:** Human GUI testing showed an `ebb:npc` with server-side tags (`ebb.npc`, `ebb.npc.ebb`, `ebb_npc`) and loaded bindings/settings, but the client still displayed no Ebb highlight/prompt.
- **Decision:** Do not rely on remote clients seeing scoreboard/entity tags for prediction. Keep server validation authoritative, and add periodic `EntityTargetSyncPayload` snapshots that sync nearby entity UUIDs already matched by the server-side entity binding registry. Client raycast prediction now accepts either a synced registered entity UUID or a locally resolvable binding/debug fallback.


## Finding: Third review Drive-source/runtime-wiring divergence
- **Date:** 2026-05-31
- **Context:** `ebb_project_review_2026-05-31_third.md` reported that a Drive source sample appeared stale: docs claimed sync/runtime fixes but sampled files looked like pre-wiring code.
- **Decision:** Treat this as a governance/source-audit risk, not just a code feature request. Re-audit the active checkout, add `scripts/third_review_static_audit.py` to make the critical wiring machine-checkable, keep docs explicit that GUI retest is pending, and refresh the PCL profile after each runtime wiring patch.

### 2026-05-31 Deep Research Report Implementation Mapping
- The requested deep research report is broader than the current 26.1.2 codebase and recommends a stable 1.21.1 mainline, but project/user constraints require keeping the existing Fabric 26.1.2/JDK25 stack. Implementation therefore adapts architecture/schema/tooling recommendations without migrating Minecraft versions.
- Existing in-progress changes already cover several report items: API contracts under `com.crpg.ebb.api`, dialogue node/check/condition/effect schema extensions, player/world variables, failure-forward validation, and block/entity JSON authoring aliases.
- Remaining report-derived gaps to close in this pass: verified smoke/static audit after schema changes, author-friendly source examples/compiler pipeline, OP developer command tree (`dev on/off`, dialogue tree/vars/reload/inspect, routine inspect, save-debug export), and updated planning/docs with concrete evidence.

### 2026-05-31 Deep Research Implementation Review Findings
- The report's stable 1.21.1/JDK21 recommendation conflicts with this session's standing 26.1.2/JDK25 constraint, so the safe implementation path is additive adaptation, not version migration.
- Smoke testing exposed a real parser bug: `DialogueEffect.optionalInt("value")` attempted to parse string variable values as integers before `SET_VARIABLE` could consume them. Fixed by making integer option parsing type-safe across effect/check/condition helpers.
- Review also found authoring shortcut gaps (`addTrait`, `removeTrait`, `addThought`, `removeThought`) and entity-type namespace ambiguity. Fixed by accepting shortcut ids and defaulting un-namespaced entity types to `minecraft` while preserving `ebb` defaults for dialogue ids.
- The new authoring compiler intentionally writes generated data under `build/generated/ebb_authoring/data/ebb` by default; it does not mutate bundled runtime resources unless `--apply` is explicitly used.


## Finding: Deep research automated tests need both JVM and real server coverage
- **Date:** 2026-06-01
- **Context:** The deep research report explicitly separates unit tests from Minecraft GameTest coverage. The initial implementation had smoke/static checks but lacked a tracked GameTest/JUnit baseline.
- **Decision:** Add JUnit tests for schema/runtime/authoring invariants and Fabric GameTests for bundled data, NPC spawn/routine state, and tagged NPC binding resolution. Use the unfiltered `runGametestServer --args nogui` run because the Fabric GameTest filter is exact/narrow enough that `fabric-api.gametest.filter=ebb` matched no tests.


## Finding: Deep research second audit revealed weak UI/routine evidence
- **Date:** 2026-06-01
- **Context:** Goal continuation required treating prior completion as unproven. Re-reading the report showed that the current code proved current-node dialogue rendering, basic look-at-player, and saved-data persistence, but did not strongly prove a dialogue log/rhythm surface, conversation-focus routine pause/resume, look-at line-of-sight policy, or saved-state schema versioning.
- **Decision:** Implement lightweight but real code paths for these: client-side dialogue session history, active conversation focus in `NpcRoutineController`, `requires_line_of_sight` parsing/authoring, and a persisted `NarrativeSavedData` version field.

## Finding: GOAL.md Story Variables should be layered state, not flags with prefixes
- **Date:** 2026-06-01
- **Context:** GOAL.md explicitly requires Branch / Major / Minor variables as a core product-model layer for route commitments, quest/NPC pivots, and local beats.
- **Decision:** Add a dedicated `story/` package and persist separate player/world maps for Branch, Major, and Minor variables in `NarrativeSavedData`, instead of encoding the layer into flat flag names. Dialogue JSON now reads/writes these variables through explicit `set_story_var`, `add_story_int`, `clear_story_var`, and `story_var` condition syntax. `/ebb dialogue vars`, `/ebb dev`, saved debug export, static audit, JUnit, and smoke checks expose/validate the same layer.

## Finding: Take-root should be a service boundary between quest completion and growth rewards
- **Date:** 2026-06-01
- **Context:** GOAL.md P3 requires major quest completion to produce take-root settlement text and grant growth outcomes, rather than merely setting a flag.
- **Decision:** Add `TakeRootService` as the server-authoritative transition point. Dialogue effects complete a quest branch; major branches then apply take-root effects, unlock/activate up to four feat slots, and record `take_rooted` state. Feat modifiers are resolved server-side during d20 checks, and `/ebb quest` / `/ebb dev` expose the state.

## Finding: Passive chimes need server-side state writes to change paths safely
- **Date:** 2026-06-01
- **Context:** GOAL.md P4 requires passive inner-voice inserts to do more than display flavor; at least one passive insight should alter the player's judgment path.
- **Decision:** Resolve chimes on the server when opening/updating dialogue nodes. A triggered chime writes `chime:<id>` and one-shot seen flags into `NarrativeSavedData`, then the normal dialogue condition system can reveal follow-up thought choices such as `rhetoric_insight`. The client only renders the `[Chime:]` status with distinct cyan styling.


## 2026-06-01 — GOAL P5 Journal/UI rhythm review
- Finding: P5 should not rely on command/debug text for player comprehension; the visible DialogueScreen status area and Journal screen are now the primary feedback loop for clues/leads and fail-forward consequences.
- Finding: P5 smoke coverage must include journal persistence/effects/conditions because adding clue effects to existing checks can legitimately change old fixture counts. `ReviewSmoke` now accepts fail-forward multi-effect checks.


## 2026-06-01 — GOAL P6 relationship/routine review
- Finding: role-specific Ebb NPC bindings must outrank the generic `ebb.npc` binding; P6 uses higher-priority tags such as `ebb.npc.demo.witness` generated by `/ebb summon_npc demo/witness_day`.
- Finding: routine expansion can stay MVP-safe by persisting requested pose/animation metadata on `EbbNpcEntity` while retaining GeckoLib idle/walk rendering until bespoke animations are authored.


## 2026-06-01 — GOAL P7 investigation/conflict review
- Finding: clues should remain server-authoritative gameplay objects, not only journal text; P7 stores discovered clue IDs and feeds clue modifiers into d20 resolution.
- Finding: the first conflict should be a dialogue set-piece with stress/resolve/fail-forward state, not a generalized combat loop. Guard dialogue now embodies that constraint.

## 2026-06-01 — GOAL P8 playable vertical slice review
- Finding: P8 is best completed as a content/data vertical slice on top of the existing server-authoritative systems, not by hard-coding a tavern controller. The final slice uses block groups, entity bindings, dialogue JSON, chimes, feats, journal/clue effects, relationship/routine effects, and conflict state as the integration surface.
- Finding: The minimum playable evidence should count concrete authoring assets, not just code features. Final automated checks assert 13 dialogues, 8 block groups, 6 entity bindings, 5 routines, 4 feats, 4 chimes, 5 clues, one conflict, and public/quiet/messy ending placeholders.
- Finding: Remaining uncertainty is only experiential/GUI pacing, so it is explicitly separated as a human Windows client retest item; automated build/data/GameTest/static/jar evidence is complete.

## 2026-06-01 — GUI retest issue root cause
- Finding: The reported missing commands, repeated NPC dialogue, and single block target were primarily caused by the Windows test profile still carrying an older Ebb jar (`01d880...`) that had only 1 block group and 2 bindings. The profile now has the rebuilt jar `f11d67...` with 8 block groups and 6 bindings.
- Finding: Existing NPCs in `新的世界 (1)` were spawned before routine-id normalization and carry legacy tags like `ebb.npc.tenant_day`; role bindings now include both `ebb.npc.demo.<role>` and legacy `ebb.npc.<role>_day` tags so the save does not need entity replacement.
- Finding: Player-facing state inspection should not be hidden behind OP-only dialogue tooling. `/ebb dialogue vars` is now accessible for the invoking player, and `/ebb vars` is available as a short alias.
- Finding: Client-side stale-sync cleanup belongs at play connection init rather than join-ready; clearing at join risks removing block-group/entity-binding payloads delivered during login.
- Finding: A dedicated `GuiRetestIssueAudit` is now tracked because these GUI bugs crossed source, packaged jar, profile jar, and existing-save state; future smoke checks should keep proving all four layers.

## 2026-06-01 — GUI retest regressions should be runtime-covered, not only statically audited
- Finding: Static/source/profile checks prove packaging and registration shape, but the NPC role problem was specifically a runtime priority/matching issue. Added Fabric GameTests that spawn `ebb:npc` entities with the same legacy `ebb.npc.<role>_day` tags seen in `新的世界 (1)` and assert they resolve to distinct role dialogues.
- Finding: The single-block-target symptom needs exact content inventory checks. Added JUnit/GameTest assertions for all eight authored block-group targets so future regressions cannot pass with only `locked_door` packaged.

## 2026-06-01 — Latest client log confirms the visible test window was still old-code
- Finding: `/mnt/e/MC/PCL/.minecraft/versions/26.1.2-Fabric-Ebb-Test/logs/latest.log` still records the user-visible problematic session loading only 3 dialogues, 1 block group, and 2 entity bindings at 16:37. That matches the screenshots and proves the window was launched before the refreshed jar/profile state. The installed profile jar now hashes to `da8da3...`, but a running Minecraft process cannot hot-swap its already-loaded mod classes/resources.

## 2026-06-01 — Separate on-disk profile correctness from runtime-log correctness
- Finding: The refreshed profile jar and source/build audits can be correct while `latest.log` still proves the last visible client session was old. Added `scripts/check_pcl_runtime_loaded.py` so the next retest can confirm the Windows client actually relaunched into the refreshed jar before judging GUI behavior.

## 2026-06-01 — Runtime relaunch remains the only unresolved blocker
- Finding: A fresh recheck still shows `check_pcl_runtime_loaded.py` observing stale runtime counts from the old client session: 3 dialogues, 1 block group, 2 entity bindings, and 1 routine. The profile jar on disk is correct (`da8da3...`), so further code edits are not the limiting factor; final verification requires closing and relaunching the Windows Minecraft client.

## 2026-06-01 — Mineflayer 26.1.2 needs data-table aliasing, not only version metadata
- Finding: `minecraft-data` 3.110.2 contains 26.1.2 protocol metadata (`protocolVersion=775`, `dataVersion=4790`) but not full `26.1` data tables, so `minecraft-data('26.1.2')` returns null by default.
- Decision: Install a local adapter that aliases missing 26.1 data tables to the newest compatible 1.21.x tables while retaining the 26.1.2 protocol/dataVersion metadata. This lets mineflayer/minecraft-protocol attempt 26.1.2 negotiation and report remaining protocol mismatches explicitly.

## 2026-06-01 — MineDojo compatibility should wrap current tooling rather than revive old Malmo
- Finding: PyPI `minedojo` is only version 0.1 and is not installed; the old MineDojo/Malmo backend is not a viable direct 26.1.2 runtime.
- Decision: Provide a MineDojo-compatible `EbbGuiEnv` API backed by the actual Fabric 26.1.2 profile/server, mineflayer probe/chat layer, and Windows screenshot/input automation.

## 2026-06-02 — Architecture plan P20/P21 first-gap analysis
- Finding: The uploaded architecture/product plan is not just a recap of P2-P8; its active roadmap starts at P20/P21. The earliest concrete gaps in the current repo were missing root `GOAL.md`, missing root `README.md`/`AGENTS.md`, no `docs/architecture.md`, no authoritative `docs/current_status.md`, and old historical docs that still said GUI retest was pending without a current supersession note.
- Decision: Implement P20/P21 first before feature expansion. This makes the repo self-explanatory and adds machine guardrails for version pins, required data directories, current artifact hashes, failure-forward checked choices, and major Take-Root consequences.


## 2026-06-02 — P22 F3 overlay must respect vanilla debug layout at scaled resolutions
- Finding: Centered F3-only Ebb target diagnostics were technically present but collided with vanilla `Debug Charts` text in the 26.1.2 test client's small-window/auto-GUI-scale mode.
- Decision: Keep the normal interaction prompt centered, but render F3 diagnostics close to the hotbar with pixel-width clipping. This keeps reason/style/id/dialogue evidence readable without turning regular play into denial/debug spam.

## 2026-06-02 — P23 reading settings should stay client-local and non-authoritative
- Finding: Font scale and text speed are player comfort preferences, not narrative state. Persisting them as profile-local `config/ebb-client.json` keeps server-authoritative dialogue checks/effects untouched and works for both singleplayer and dedicated clients.
- Decision: Add `ClientDialogueSettings` under client sources, initialize it from `EbbClient`, and expose only dialogue-panel controls (`A-`, `A+`, speed cycle). Body/history/status text scales and wraps; vanilla choice buttons remain normal widgets for stable click/keyboard behavior.

## 2026-06-02 — Mutable GUI save makes one-shot Chime screenshot proof unreliable
- Finding: The current `新的世界 (1)` GUI save has already consumed several one-shot Chime triggers during earlier route testing, so repeated attempts to capture a fresh Chime insert screenshot opened later route nodes instead.
- Decision: Treat Chime visual handling as a static/code-audited P23 item for this phase (`CHIME_STATUS_COLOR` and `[Chime:]` status routing) while using multi-scale GUI screenshots to prove the shared status/roll/clue/quest/take-root echo area clips above choices.

## 2026-06-02 — Settings controls must not compete with the dialogue heading
- Finding: The first P23 GUI screenshots proved settings persistence and status layout, but the top-left settings buttons could visually collide with the centered dialogue id/node heading in small-window GUI-scale modes.
- Decision: Render the dialogue id/node heading in a clipped lane to the right of the settings buttons. This keeps author/debug context visible without sacrificing the new player-facing controls.

## 2026-06-02 — P24 first audit: validation exists but is split across parser/runtime layers
- Finding: Dialogue node-local references and failure-forward checks already exist in `DialogueDefinition`, and runtime registries expose validation messages, but P24 cross-registry references (quest/feat/chime/journal/clue/routine/relationship/conflict IDs mentioned from dialogue effects/conditions) are not yet centralized in a single validation pass.
- Decision: Add a P24 authoring validation layer rather than scattering all cross-registry checks into individual parsers. It should run from data validation/smoke/static audit and produce author-facing messages without changing demo runtime data unless intentional.

## 2026-06-02 — P24 compiler diagnostics need source context, not just semantic messages
- Finding: `compile_authoring_sources.py` currently reports semantic errors with source filenames and node/choice ids, but YAML/JSON parse exceptions are not wrapped with line/column context and compiler output does not consistently classify file/line diagnostics.
- Decision: Extend the compiler with structured diagnostic helpers that include source file and parser line/column when PyYAML/JSON exposes them, then add a regression fixture/check for bad authoring input.

## 2026-06-02 — P25 first audit: payloads are line-based, so first maturation should preserve network shape
- Finding: `QuestTreePayload` and `JournalPayload` are already stable line-list packets; changing them into rich DTOs would create more network/code churn than needed for the first P25 pass.
- Decision: Keep line-list payload compatibility and improve server-generated semantic prefixes plus client-side filters/colors/cards. This upgrades legibility and feat/Take-Root visibility without destabilizing packet codecs.

### 2026-06-03 P26 Chime Audit
- Existing Chime parser supports only name/source attribute/min score/trigger tags/speaker style/cooldown/lines; no tone guide, explicit one-shot fields, or active thought metadata yet.
- Existing resolver has per-dialogue-node one-shot flags but does not use `cooldown_ticks`; `DialogueService` already computes `dayTime` and can pass it to an overloaded resolver.
- Bundled data currently has 4 Chimes (`dread`, `empathy`, `instinct`, `rhetoric`) over luck/wisdom/perception/charisma; P26 needs 8 voices covering all DND-8 attributes.
- Dialogue open/update resolves Chime before computing visible choices, so chime-triggered active thought choices can appear on the same node once the resolver sets `chime:<id>`.
- NarrativeSavedData already supports player variables and flags, so cooldown can be stored as player variables like `chime_last_tick:<id>` while one-shot remains flag-based.
- Attribute data confirms the DND-8 keys used by this project: strength, dexterity, constitution, intelligence, wisdom, charisma, perception, luck.
- ChimeRegistry keeps ordered resolution by identifier string; P26 tests should isolate a single attribute at a time when asserting a specific Chime fires.
- The innkeeper start node can safely host all 8 active Chime thought choices; visible choices are paged by the dialogue UI and conditions keep inactive routes hidden until the resolver sets the corresponding chime flag.
- First P26 static audit run failed because the new smoke guardrail looked at `scripts/run_smoke_checks.sh` instead of the Java smoke source where the P26 assertions live. Adjusting audit source target rather than duplicating smoke strings in the shell wrapper.
- P26 dev chime-reason build attempt failed because `ServerLevel` in Minecraft 26.1.2 exposes `getOverworldClockTime()` for this project's day-time checks, not `getDayTime()`. Fixed DevSnapshotService to use the same clock source as DialogueService.

### 2026-06-03 P27 NPC/Routine Audit
- Current `EbbNpcEntity` persists routine id, narrative key, pose, and animation, but renderer uses one default GeckoLib model/texture and no role-specific skin selection.
- Current `NpcRoutineDefinition` accepts free-form action/pose/animation strings and does not validate invalid routine actions or animation names beyond vector parsing.
- Current `NpcRoutineController` already pauses movement/look routine during active dialogue focus, but it does not store/restore previous routine animation after focus, nor expose current step/action/target debug state.
- Current GeckoLib assets are placeholder one-texture NPC assets with idle/walk only; P27 needs documented/temporary humanoid role skins and conversation animation hooks.
- First P27 custom animation controller compile failed because Java inferred GeckoLib's animation test animatable as `GeoAnimatable`; fixed by explicitly parameterizing `AnimationController<EbbNpcEntity>`.
- First P27 JUnit compile failed because the test referenced `NpcRoutineDefinition` without importing it; added the missing import.
- P27 smoke initially failed legacy ThirdReviewStaticAudit because routine focus now uses `DialogueService.activeConversationSessionForEntity` to select conversation animations, while the audit still required the older player-only method string. Updated the audit to the richer session-based focus contract.

## P28 audit start — 2026-06-03
- Current conflict runtime is still minimal: `ConflictDefinition` only has title/scene/stressLimit/resolveGoal/failureState/successState; `ConflictService` only supports start/addStress/addResolve and returns terse `conflict_*` echoes.
- Existing demo `hallway_confrontation.json` has no formal phases, leverage clues, or outcome catalog; `guard_intro.json` does route success/failure via conflict stress/resolve but does not expose a full status/leverage model yet.
- Existing smoke/JUnit assertions expect legacy terminal states `failed_forward` and `resolved`, so P28 should preserve those compatibility states while adding richer phase/status/outcome semantics around them.
- Dialogue runtime already supports `clue_found`, `conflict_state`, `scene_phase`, and success/failure check effects, so P28 can extend `ConflictService` and demo data without inventing a parallel dialogue path.
- `NarrativeSavedData` persists conflict state in narrative states and conflict stress/resolve scores in `conflict_scores`; this is sufficient for P28 score persistence if the service exposes phase/status helpers and outcome application.
- Existing investigation clues already carry check modifiers by DND-8 attribute; P28 should explicitly surface those known clues as leverage and include demo choices whose visibility/DC behavior depends on them.
- Dev snapshot currently only lists conflict registry summary/messages and saved-state debug lines; P28 can add a dedicated conflict catalog/status section so OPs can inspect phases, leverage, and outcomes without a separate UI rewrite.
- There is no conflict JSON schema yet; adding one under `docs/schemas` will make the new phase/leverage/outcome contract explicit alongside existing dialogue/chime/entity/block schemas.

## P29 audit start — 2026-06-03
- `NarrativeSavedData` already stores a schema `version` and optional codec defaults, but `CURRENT_SCHEMA_VERSION` is still 1 and there are no explicit migration/default tests for older saved data missing newer fields such as story vars, relationship/clue/conflict maps, or conflict phase states.
- Dialogue networking already validates player/session ownership, timeouts, invalid choices, conditions, and action target revalidation. It closes sessions on disconnect/respawn/leave/level change, but same-NPC contention is not explicit and stale/spoofed packet denials are not currently exposed as testable counters/reasons.
- Commands use Fabric permission predicates for OP-level dev/dialogue/routine/export/summon/attribute admin commands, while player-facing self-inspection commands (`/ebb vars`, `/ebb journal`, `/ebb quest`, `/ebb attributes`) remain accessible; P29 should add regression tests rather than loosening this.

## P30 content-count audit — 2026-06-03
- Current bundled demo counts before P30 expansion: block_groups=8, entity_bindings=10, npc_routines=5, quest_branches=2, feats=4, chimes=8, journal_entries=4, clues=5, conflicts=1, dialogues=13.
- P30 can be satisfied by data expansion without new engine features: add 4 block groups, 2 role NPC bindings/routines/dialogues, 2 major + 8 minor quest branches, 8 feats, expand existing 8 chimes to 5 lines each, add at least 15 journal/clue pairs, 2 conflicts, and ending placeholders for the new major branches.
- Existing parsers tolerate simple JSON: quest branches need title/kind and major take_root_text; feats need display/description/modifiers; journal entries need text; clues can link journal entries; block groups require dialogue/blocks/interaction_point; entity bindings can match `ebb:npc` tags; routines can use simple stand steps.


## P30 validation finding — 2026-06-03
- Finding: P30 smoke/reference validation caught a real cross-registry content bug introduced by new support NPCs: `cook_intro` and `courier_intro` used `add_relation` effects for `ebb:demo/cook` and `ebb:demo/courier` before matching relationship definitions existed. Added relationship JSON files instead of removing the effects so NPC reactivity remains represented in the data model.


## P31 release-packaging audit — 2026-06-03
- Finding: P31 can be completed as documentation/static-guardrail work without changing the game jar. The jar/profile hash remains the P30 hash while install docs, release metadata, story-pack tutorial, changelog, and license clarity become the release-facing contract.
- Decision: Keep Modrinth/CurseForge metadata as a tracked draft rather than an upload artifact, because screenshots, final version naming, and legal license review should happen immediately before public submission.


### 2026-06-03 — Dialogue wait-state root cause and GUI automation correction
- Screenshot symptom: after choosing a dialogue option, the UI displayed `等待服务器……` indefinitely.
- Runtime evidence: the old `latest.log` contained `ZipFile invalid LOC header`, `Failed to load class file for DialogueService$ChoiceResolution`, and `[Server thread/ERROR]: Error executing task on Server`.
- Root cause: the profile-local Ebb jar had been overwritten while the Minecraft JVM was still running; the artifact on disk was valid afterwards, but the already-running classloader kept stale ZIP offsets.
- Fixes: server choice receiver now catches failures and sends `DialogueClosePayload(..., server_error)`, client dialogue choices time out/re-enable after 10s, `configure_pcl_test_client.sh` refuses profile refresh while the matching Java process is alive, and runtime log checks flag stale classloader ZIP/class errors.
- GUI automation issue from follow-up screenshot: setup used invalid `minecraft:oak_sign[facing=south]` and viewpoints for `guestbook_torn_page`/`stable_mud` missed the actual raycast target. The runner now uses valid/idempotent setup commands, a lectern placeholder for the guestbook block, closer guestbook view, top-down stable-mud view, and dialogue-screen cyan-border assertions.

### 2026-06-03 PostToolUse hook cwd failure
- Symptom: tool/hook startup could fail with `No such file or directory (os error 2)` when using the session default cwd.
- Finding: `/mnt/e/MC/SIMMC2_1-21-8` from the turn environment did not exist, while the active project is `/mnt/e/MC/PCL/CRPG_MOD`.
- Fix: created compatibility root `/mnt/e/MC/SIMMC2_1-21-8` and symlinked `CRPG_MOD -> /mnt/e/MC/PCL/CRPG_MOD`, so hooks/tools launched from the old cwd can resolve the project plan.

### Phase 33 intake findings (2026-06-03)
- Review report confirms the actionable set already mapped into Phase 33: H1 nested dialogue-vars permission, H2 active feat condition, H3 centralized raycast policy, H4 disadvantage and roll breakdowns, H5 checked-choice success end semantics, M1 pre-effects authoring semantics, M2 large block-group LOS, M3 duplicate block-group invalidation, M4 retryable check locking/unlock, M5 item placeholder semantics, M6 NPC animation/role visibility, M7 routine validation, M8 dev inspect authority detail, M9 command architecture cleanup, M10 roll UX breakdown.
- Initial code scan confirmed nested `/ebb dialogue vars <player>` lacks a permission guard, `HAS_ACTIVE_FEAT` is aliased to `HAS_FEAT`, client/dev raycasts still use OUTLINE in spots, and server block group LOS currently checks a single interaction point.

### Phase 33 code-audit findings before edits
- `DialogueChoice` currently treats `effects` as pre-roll effects; adding a `pre_effects` alias can be backwards-compatible by merging `pre_effects` before existing `effects` and documenting `effects` as legacy pre-effects.
- `RollResultPayload` lives in `network/dialogue` and has only selected die + aggregate modifier. To keep test/source compatibility, add an overloaded old-signature constructor while extending packet serialization with raw rolls and modifier breakdown.
- Block group indexing currently keeps both groups and lets later duplicates overwrite `byBlock`; the safer deterministic behavior is to skip the later overlapping group and retain the first owner.
- NPC routines already validate allowed action/pose/animation and apply visible GeckoLib animation strings. Remaining review gaps are empty routine rejection, time overlap detection, positive teleport fallback distance, and role inference for the newer cook/courier demo NPCs.

### PLAN.md P34 intake — 2026-06-15
- New active objective references `E:\MC\PCL\PLAN.md`, which defines P34 LLM NPC + Memory Foundation on top of the current Fabric 26.1.2 Ebb mod.
- Initial read confirms scope: server-authoritative, async, disable-able/mockable LLM NPC free chat; NPC tiers with scripted major, minor generatable, promoted major, static non-LLM, disabled; gateway-first auth/LLM architecture; no API secrets in mod jar; memory/knowledge/provenance/conflict systems; OP dev inspection.
- Need finish reading PLAN.md, map requirements into Phase 34+, and implement incrementally with deterministic fake provider first before any external OpenAI/gateway dependency.

### PLAN.md P34 implementation audit start — 2026-06-15
- P34 is the earliest incomplete PLAN.md stage and should be implemented before NPC profile/promotion, gateway auth, OpenAI, or real memory backends.
- Current repository is clean at `2029001` and contains only planning updates from the prior turn; authoritative implementation work for P34 has not begun yet.
- P34 must preserve the hard PLAN constraints: server-authoritative chat, async fake/disabled provider first, no OpenAI/API secrets in the mod jar, no real network access in disabled/fake modes, and existing deterministic dialogue/quest/conflict systems must remain primary.
- P34 code audit found clean insertion points: `ModPackets` for new payload registration/receivers, `ClientInteractionNetworking` for client receivers/send helpers, `DialogueService.choose` before d20 resolution for `ChoiceType.LLM_CHAT`, and `ModCommands.registerEbbCommand` for `/ebb llm status`.
- `DialogueScreen.choiceLabel` uses an exhaustive `switch` on `ChoiceType`, so adding `LLM_CHAT` requires an explicit client style case to keep compilation and player-facing labels correct.
- Existing tests are centralized in `DeepResearchDataTest` plus `EbbGameTests`; P34 can add deterministic fake/disabled/session tests without requiring real OpenAI/gateway/network dependencies.
- First P34 build attempt failed on two concrete Java issues in `LlmChatService`: passing `ServerPlayer` where UUID was expected and accessing private `ServerPlayer.server`; fixed by using `player.getUUID()` and `((ServerLevel) player.level()).getServer()`.

### PLAN.md P35 implementation audit start — 2026-06-15
- P34 is complete and verified with build/JUnit/validate/smoke/GameTest/static/diff evidence; next earliest incomplete PLAN.md item is P35 NPC Profile / Tier / Promotion data layer.
- P35 must not make all entities interactable: minor NPC promotion may only apply to explicitly configured entity bindings/tags/custom `ebb:npc`/OP commands, preserving debug fallback disabled.
- P35 acceptance needs both data-layer scripted profiles and persisted promoted profiles: `/ebb npc profile target` should inspect scripted profile data, and a minor fake-chat promotion path should write stable profile data to saved state.

### Phase 35 / intake and code audit start
- **Status:** started.
- **Time:** 2026-06-15 Asia/Shanghai.
- Read current plan/progress after P34 push; starting PLAN.md P35 NPC Profile / Tier / Promotion data layer.
- Initial focus: registry/reload hook, entity binding extension, promoted-profile persistence, and dev command surfaces.

### Phase 35 / initial code audit findings
- PLAN P35 requires NpcTier enum, NpcProfileDefinition/parser/registry, six P30 role profile JSON files, minor-generatable entity binding schema, NpcPromotionService, and promoted profile persistence.
- Current reload hub is com.crpg.ebb.data.NarrativeDataRegistries with no npc_profiles registry; entity bindings are in com.crpg.ebb.interaction.entity and currently lack npc_tier/llm promotion fields.
- NarrativeSavedData schema is v2 and has world/player vars plus NPC state tags, but no promotedNpcProfiles map yet; this is the main persistence insertion point.
- Command surface has /ebb llm and /ebb dialogue/routine/dev, but no /ebb npc profile target/key/minorize/regenerate_profile yet.

### Phase 35 / compile fix checkpoint
- First P35 compile attempt failed on ResourceKey<Level>.location() and missing UUID import in ModCommands. Fixed to use dimension().identifier() and imported java.util.UUID.

### Phase 35 / data and compile checkpoint
- Added six static role NPC profile JSON files under data/ebb/npc_profiles/demo.
- Added npc_tier/npc_profile metadata to role entity bindings and innkeeper villager alias.
- Added minor villager binding at data/ebb/interactions/entity_bindings/llm/minor_villager.json and LLM intro dialogue at data/ebb/dialogues/llm/minor_intro.json.
- Updated GUI/runtime count expectations from 19/12/14/7 to 20/12/15/7.
- `scripts/gradle-local.sh --no-daemon compileJava` passed after fixes.


### Phase 35 / completion review findings
- Repeated code review after P35 found and fixed two compile issues: ResourceKey dimension id API mismatch and missing UUID import.
- Static audit initially failed because P29 expected saved-data schema v2; audit was updated to reflect the intentional P35 schema bump to v3 while preserving migration checks.
- Runtime/test counts are now 20 dialogues, 12 block groups, 15 entity bindings, 7 routines, and 6 npc_profiles. GUI/runtime count tools were updated to use the new minimums.
- Full smoke and GameTest validation passed after P35, including promoted profile persistence and minor NPC promotion coverage.
P36 implementation started: requirements from /mnt/e/MC/PCL/PLAN.md P36 are independent gateway skeleton/endpoints, dev local auth, production OIDC abstraction, Minecraft /ebb llm auth/status/logout, server-only token storage, auth-required gating, fake chat after login, logout invalidation, and token redaction/no client leakage.

### P37 requirements and SDK source notes
- PLAN.md P37 requires gateway `/v1/chat/message`, official OpenAI Java/Node SDK path, Responses API, structured JSON output, streaming/chunked response, timeout/circuit breaker, model config, and default `store:false` unless explicitly enabled.
- Official OpenAI Java SDK README shows Gradle dependency `implementation("com.openai:openai-java:4.39.1")`, Responses API usage via `OpenAIClient`, `OpenAIOkHttpClient.fromEnv()`, `ResponseCreateParams`, and streaming Responses helpers with `ResponseAccumulator` and `client.responses().createStreaming(...)`.
- Current mod `LlmChatService.clientFor` still returns fake or disabled only; P37 must add a gateway chat client for `mode=gateway` and make provider failures return an error payload rather than hanging the UI.

### P37 continuation audit - 2026-06-17T03:03:28+08:00
- Current P37 partial state: gateway chat provider/request/response classes exist but `GatewayServer` is not wired to `/v1/chat/message`; Minecraft `LlmChatService.clientFor` still needs a gateway HTTP chat client; P37 docs/tests/static audit are not yet complete.

### P37 completion review notes
- `HttpLlmGatewayClient` is server-side only and attaches `opaque_player_token` from `LlmAuthService.validToken`; client UI/networking still contains no token field.
- Gateway `openai_responses` provider uses official Java SDK and structured JSON schema; `fake` and `mock_openai_responses` remain default validation paths to avoid API usage.
- Artifact hash static audit initially failed after P37 jar rebuild and was fixed by updating `docs/current_status.md` with current P37 jar/source hashes.

### P38 implementation approach - 2026-06-17T03:36:02+08:00
- P38 will live primarily in `ebb-llm-gateway`: H2-backed migration, append-only `MemoryRecord`, current/superseded `MemoryFact`, `MemoryConflict`, deterministic hash embeddings for fake/mock tests, and hybrid retrieval endpoints. Minecraft `/ebb memory search/inspect/conflicts` will query gateway via server-side HTTP using `LlmConfig.gateway_base_url`, with no client secret exposure.

### P38 completion review notes
- H2 treats `VALUE` as reserved; migration/code use `fact_value`, `old_fact_value`, and `new_fact_value` columns.
- P38 does not call OpenAI embeddings yet; it adds a deterministic gateway embedding write path so tests do not consume API and later OpenAI embedding replacement can keep the same retrieval contract.
- GameTest avoids live gateway calls; HTTP behavior is covered by GatewaySmoke and JUnit local HttpServer tests.

### Phase 39 intake / design notes — 2026-06-17 Asia/Shanghai
- PLAN.md P39 requires LLM-proposed memory operations, deterministic validation, episodic summary consolidation, related-memory links, A-Mem-like summary evolution that preserves raw episodes, A-MemGuard-like safety lessons, and dev visibility for raw episodes/facts/conflicts.
- Existing P38 `MemoryStore` already had records/facts/conflicts/search; the P39 implementation should extend this store instead of creating a second persistence path, so chat appends continue to be atomic and server/gateway authoritative.
- Chosen acceptance fixtures: reject a proposed `tavern.owner=player:<uuid>` operation from “我是旅馆老板” with a safety lesson preserving canonical innkeeper ownership; extract `player:<uuid>.questioned_ledger=true` and a bilingual summary when the player questions the ledger; expose raw record text, extracted facts, operations, summaries, links, conflicts, and safety lessons via gateway/dev commands.

## Phase 40 continuation findings — 2026-06-17
- PLAN.md P40 acceptance requires three concrete proofs: hidden KB secret is not in prompt before clue, same question changes after clue, and `/ebb kb inspect <npc>` shows visible/hidden chunks.
- Current P40 draft already compiles and has parser/registry/index/service/effects/prompt context, but lacks demo `npc_knowledge_packs` JSON for all profile `initial_packs`, lacks `/ebb kb inspect`, and lacks tests/audit that assert non-leakage and post-clue reveal.

## Phase 41 continuation findings — 2026-06-17
- Existing P35 already covers much of P41: minor candidate detection via `ebb.npc.minor` tag/entity binding, deterministic promoted profile JSON generation, persistence in `NarrativeSavedData`, first-chat promotion in `LlmChatService`, and dev commands for minorize/promote/regenerate.
- Remaining P41 gaps versus PLAN.md are: explicit `NpcProfileGenerator` class/prompt/schema surface, generated profile `knowledge_seed` and generated `suggested_options`, dev review surface beyond raw profile display/reject, and world-hour promotion rate limiting.

## Phase 42 intake findings — 2026-06-17
- PLAN.md P42 requires: streaming text, suggested-option selection, return-to-script button, memory correction button, dev citations overlay, GUI E2E scenario, error/timeout/cancel non-stuck behavior, and K-menu LLM auth status visibility.
- Existing `NpcChatScreen` already has basic input, send, suggested option buttons, error handling, cancel/close, and live background behavior, but it appends every NPC chunk as a new line, has no explicit return-to-script button, no memory-correction action, no citation overlay toggle, and no K-menu LLM auth status surface yet verified.

## Phase 42 implementation audit — 2026-06-17
- Existing `NpcChatScreen` opens over a live world background (`isPauseScreen=false`) and has basic input, send, suggested options, cancel, and error handling, but each NPC chunk is appended as a separate line and `waitingForReply` is cleared for every chunk regardless of `done`.
- Server `LlmChatService.completeResponse` currently emits the whole NPC response in one `LlmChatChunkPayload`; `LlmConfig.llmChatStreaming()` exists and should drive chunked sends without adding client secrets or bypassing gateway authority.
- Dialogue sessions are removed when `llm_chat` opens, while `LlmChatSession` stores dialogue id/source/return node/target metadata. P42's “返回脚本对话” should resume a server-authoritative `DialogueSession` at `returnNodeId` using that stored metadata instead of only closing the UI locally.
- K menu exists on `K` and renders only a centered panel, but it has no visible LLM auth status line/button yet; `/ebb llm status/auth/logout` commands already provide server-owned safe auth state and should be exposed through the menu without syncing tokens to the client.
- Existing GUI automation runner covers K menu, scripted role/block interactions, and live-background checks. P42 needs a new LLM chat scenario/dry-run manifest proving NPC chat open/send/reply/suggested-option path plus timeout/error/cancel non-stuck guardrails.

## Phase 42 validation findings — 2026-06-17
- P42 code/static/JUnit/GameTest/smoke validation is now green, including a generated `llm_chat` GUI automation scenario and manifest.
- Actual Windows GUI execution remains the only P42 evidence gap: `windows_gui.py find --title '26\\.1\\.2-Fabric-Ebb-Test|Minecraft'` returned an empty window list, so there was no active test client window to drive.

## Phase 42 actual GUI findings — 2026-06-17
- Direct Windows GUI validation succeeded against the separate `26.1.2-Fabric-Ebb-Test` profile after launching Minecraft directly with the profile-local jar and using the existing `新的世界 (1)` save.
- The first automation pass showed click-coordinate drift at Minecraft GUI scale. The runner now derives LLM-chat button click points from the cyan panel bounds in the screenshot instead of fixed normalized screen positions.
- Actual `llm_chat` report `build/gui-e2e/llm-chat-report.json` had no failed steps and proved K-menu LLM status, fake-provider free chat, live-background chat panel, citations overlay, suggested-option reply, and return to scripted dialogue.
- `scenario_llm_chat` now writes the fake server config only for actual `--gui` client testing, preventing dry-run smoke checks from modifying files outside `CRPG_MOD`.

## Phase 43 implementation findings — 2026-06-17
- P43's “high-risk effects not allowed from LLM direct output” is best enforced in two layers: gateway `GatewayChatResponse.sanitizeProposedEffects` rejects high-authority proposal strings, and Minecraft `HttpLlmGatewayClient` still ignores `proposed_effects` entirely so no direct LLM output mutates server state.
- `scripts/p43_llm_safety_audit.py` intentionally scans tracked and currently untracked non-ignored files so it remains useful before commit, not only after `git add`.
- GUI E2E P43 uses `llm_validation` as a dry-run-capable route. In non-GUI mode it writes only manifests/reports inside `CRPG_MOD`; profile-local config writes happen only with explicit `--gui` client testing.

### Phase 44 / PLAN.md final audit extraction started
- **Time:** 2026-06-17 Asia/Shanghai.
- Re-read planning context and current git state; repository is clean at `e350d5d` before Phase 44 edits.
- Began deriving explicit requirements from `/mnt/e/MC/PCL/PLAN.md` headings and sections 0-2.3. Key requirement families: server-authoritative/free LLM chat, major/minor NPC tiering and deterministic promotion, no client/API secret exposure, OAuth/OIDC gateway auth, chat UI streaming/cancel/citations/corrections, six-layer append-only memory, fact conflict/supersede rules, and NPC profile/knowledge authoring.

### Phase 44 / PLAN.md requirement families extracted
- **Time:** 2026-06-17 Asia/Shanghai.
- Finished reading `/mnt/e/MC/PCL/PLAN.md` sections 2.4-16.
- Additional requirement families: NPC profile/knowledge JSON and story effects, standalone gateway with auth/device and OpenAI Responses path, mod-side LLM packages/payloads/configs, prompt structured-output and post-validation, memory-store DB/migration with append-only records/facts/conflicts and hybrid retrieval, no hidden KB/API keys in client sync, dialogue/relationship/quest integration, OP dev memory/profile/KB tooling, privacy/consent/cost controls, phase P34-P43 acceptance gates, authoring examples, prompt templates, and final mandatory build/validate/smoke/GameTest checks.

### Phase 44 / evidence scan initial result
- **Time:** 2026-06-17 Asia/Shanghai.
- Current code contains the major PLAN.md implementation families: `llm`, `network/llm`, `npc/profile`, `npc/knowledge`, gateway auth/chat/memory packages, six demo NPC profiles, seven KB packs, minor villager binding, LLM dialogue choice, and P43 schemas/audits.
- Initial gap candidates from comparing PLAN.md wording with runtime surfaces: `/ebb llm quota` and consent commands are not visible in `ModCommands`; gateway routes currently show health/auth/logout/chat/message/memory search/inspect/conflicts/episodes/lessons but not the full optional route list (`/v1/player/quota`, npc profile endpoints, chat start/cancel/session, memory correct/delete, knowledge update). Need decide which are required for final PLAN completion and implement or document verified deferral only if PLAN treats them as non-MVP.

### Phase 44 / first gap remediation checkpoint
- **Time:** 2026-06-17 Asia/Shanghai.
- Implemented additional PLAN.md API/command contract surfaces found during audit: gateway profile/quota/chat-session/memory-correct/delete/knowledge endpoints, append-only memory correction audit, player-memory delete endpoint, `/ebb llm quota`, `/ebb llm consent view|revoke`, `/ebb llm auth_debug <player>`, `/ebb memory correct`, `/ebb memory export`, and `/ebb memory delete_player`.
- Validation checkpoint passed: `scripts/gradle-local.sh --no-daemon compileJava` and `scripts/p36_gateway_smoke.sh`.

### Phase 44 / final audit strict-surface cleanup — 2026-06-17
- PLAN.md explicitly listed separate LLM chat UI helper classes (`NpcChatHistoryWidget`, `NpcChatInputWidget`, `LlmAuthStatusWidget`); the previous implementation kept this logic inside `NpcChatScreen`/K-menu. Split the rendering/input/status helper surfaces into named classes and updated the P42 static guardrail.
- PLAN.md used `data/*/npc_knowledge/<path>.json` while the implemented registry used `npc_knowledge_packs`. Added a backward-compatible alias directory to `JsonDataRegistry`/`NarrativeDataRegistries` so both names are accepted and duplicate ids are reported instead of silently overridden.
- Validation after cleanup: `scripts/gradle-local.sh --no-daemon compileJava compileClientJava` and `python3 scripts/goal_static_audit.py` passed.
