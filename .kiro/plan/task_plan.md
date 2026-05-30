# Task Plan: Minecraft Disco-like CRPG Mod

## Goal
Build a Fabric-based Minecraft Java Edition 26.1.2 CRPG mod prototype under `CRPG_MOD` that supports interactable targets, highlights, dialogue/action/thought UI, server-authoritative checks, narrative state, developer tooling, and later NPC routines.

## Current Phase
MVP verification complete; Phase 7 custom NPC/routine work deferred

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
- [ ] Add narrative NPC entity and renderer.
- [ ] Integrate GeckoLib after the core interaction-dialogue loop works.
- [ ] Implement basic routine schedule and look-at-player behavior.
- [ ] Link dialogue effects to NPC routine changes.
- **Status:** deferred for current MVP (GeckoLib remains a hard dependency; custom NPC/routine work is intentionally postponed beyond the verified vanilla entity/block-group interaction-dialogue loop).

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
