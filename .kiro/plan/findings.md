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
