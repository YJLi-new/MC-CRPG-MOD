# Second Review Remediation — 2026-05-30

Source review: `C:\Users\lanla\Downloads\ebb_project_review_2026-05-30_second.md`.

## Completed code changes

### Debug entity fallback is now configurable

- Added `InteractionSettings` loaded from `data/*/interactions/settings/*.json`.
- Bundled demo setting is `data/ebb/interactions/settings/default.json` with `enable_debug_entity_fallback=false`.
- `EntityBindingRegistry.resolve(entity)` now returns a fallback `ebb:debug/entity` binding only when that setting is enabled.
- Server validation denies unbound entity interaction as `unbound_entity` when fallback is disabled.
- `/ebb dev` and `/ebb data` summaries now report whether debug fallback is enabled.

### Dedicated-server entity binding client prediction

- Added `EntityBindingSyncPayload` and registered it as a clientbound play payload.
- `InteractionSyncService` now syncs both block groups and entity bindings/settings to modded clients on join/data-pack sync and successful reload.
- Client receivers rebuild the client-side entity binding registry from the server payload and clear synced state on disconnect.
- Client crosshair detection uses the synced binding registry to decide entity highlight/prompt eligibility and matched binding interaction range.

### Block-group sync limits are explicit

- Added shared `InteractionSyncLimits`.
- `BlockGroupDefinition.parse` hard-invalidates groups whose block count exceeds `InteractionSettings.max_blocks_per_group` instead of relying on payload truncation.
- `BlockGroupIndex` skips groups beyond the sync group limit with an explicit validation message.
- `BlockGroupSyncPayload` no longer silently truncates group or block lists; attempting to send oversized data throws immediately.

### NPC routine waypoint skeleton improvement

- Routine `path` steps now progress through multiple waypoints in sequence instead of always targeting only the first point.
- `EbbNpcEntity` tracks transient routine path index and resets it when the routine changes or the entity is loaded.
- This remains an MVP routine skeleton: behavior stacks, animation performances, transition blending, and complex schedules remain later work.

## Documentation updates

- Updated `docs/mvp_verification_steps.md` to reflect registered-entity-only default behavior and fallback config.
- Updated `docs/review_remediation_2026-05-30.md` and `docs/completion_audit_2026-05-30.md` to remove the old unconditional fallback statement.
- Added `docs/json_authoring_guide.md` with schemas/examples for interaction settings, entity bindings, block groups, routines, attributes, and dialogue effects.
- Added `docs/manual_client_test_result_2026-05-30_second.md` as the hand-test record template/status note.

## Verification

Commands run from `/mnt/e/MC/PCL/CRPG_MOD`:

```bash
scripts/gradle-local.sh --no-daemon build
CP="$(cat build/tmp/runtime-classpath.txt)"
.tools/jdk-25/bin/javac -cp "$CP" -d build/tmp/verify \
  build/tmp/verify-src/ReviewSmoke.java \
  build/tmp/verify-src/AttributePointsSmoke.java \
  build/tmp/verify-src/SecondReviewSmoke.java
.tools/jdk-25/bin/java -cp "build/tmp/verify:$CP" ReviewSmoke
.tools/jdk-25/bin/java -cp "build/tmp/verify:$CP" AttributePointsSmoke
.tools/jdk-25/bin/java -cp "build/tmp/verify:$CP" SecondReviewSmoke
```

Observed results:

- Build: `BUILD SUCCESSFUL in 51s` after interaction settings/sync/limit/waypoint changes.
- `ReviewSmoke`: passed with dialogues=3, attributes=8, block_groups=1, entity_bindings=2, npc_routines=1.
- `AttributePointsSmoke`: passed with attributes=8, points=8.
- `SecondReviewSmoke`: passed; verified fallback can be disabled/enabled by config, entity binding sync payload construction, and oversized block-group invalidation.
- `scripts/gradle-local.sh --no-daemon runServer --args nogui`: `BUILD SUCCESSFUL`; loaded Ebb and reached the expected EULA gate.
- Refreshed `26.1.2-Fabric-Ebb-Test`; build jar and installed test-client jar SHA-256: `510cb69490d4b855a084ab433d0d0db06b150ad8aa253b76dd2fa94fdf9d432b`.
- `git diff --check`: no whitespace/error output.

## Manual client test status

The second review correctly identifies full Windows GUI playtesting as P0. Code/build-level work is complete, but an actual player-driven run of `26.1.2-Fabric-Ebb-Test` still requires operating the Windows Minecraft client window: entering a world, looking at entities/blocks, pressing `X`, clicking dialogue choices, and visually confirming NPC render/look-at behavior. This cannot be honestly marked as completed by command-line-only automation.
