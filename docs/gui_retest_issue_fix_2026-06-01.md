# GUI Retest Issue Fix — Commands, Role NPC Bindings, and Block Groups

Date: 2026-06-01 Asia/Shanghai

## Reported symptoms

1. `/ebb journal`, `/ebb quest`, and `/ebb dialogue vars` appeared unusable in the command UI.
2. Tagged role NPCs all opened the same innkeeper dialogue.
3. In save `新的世界 (1)`, only the locked-door block target showed the Ebb interaction prompt/highlight.

## Root causes found

- The dedicated `26.1.2-Fabric-Ebb-Test` profile was still carrying an older Ebb jar hash `01d880f0e424e0b6ff3592e9bf89189199254167ebb891baffa18330231758b0`. That jar contained only 3 dialogues, 1 block group, 2 entity bindings, and 1 routine, so only `locked_door` could be detected.
- The existing NPCs in `新的世界 (1)` were spawned by that older jar with legacy routine ids/tags such as `minecraft:tenant_day` and `ebb.npc.tenant_day`. The newer role bindings only matched `ebb.npc.demo.tenant`, so the generic `ebb.npc` binding won and opened `ebb:demo/innkeeper_intro`.
- `/ebb dialogue vars` was inside the OP-style `/ebb dialogue` command branch. It should be a player-facing self-inspection command, unlike OP-only inspect/tree/reload tools.

## Fixes applied

- Made `/ebb dialogue vars` player-self accessible by moving the OP permission check down to only `inspect`, `tree`, and `reload` subcommands.
- Added `/ebb vars` as a direct player-facing alias for the same variable/state summary.
- Updated `/ebb summon_npc` routine parsing so bare routine names like `tenant_day` resolve to bundled `ebb:demo/tenant_day` when present, while still accepting explicit ids like `demo/tenant_day` and `ebb:demo/tenant_day`.
- Extended role NPC entity bindings to match both new demo tags and legacy tags already present in `新的世界 (1)`:
  - innkeeper: `ebb.npc.demo.innkeeper`, `ebb.npc.demo.innkeeper_day`, `ebb.npc.innkeeper`, `ebb.npc.innkeeper_day`
  - witness: `ebb.npc.demo.witness`, `ebb.npc.demo.witness_day`, `ebb.npc.witness`, `ebb.npc.witness_day`
  - tenant: `ebb.npc.demo.tenant`, `ebb.npc.demo.tenant_day`, `ebb.npc.tenant`, `ebb.npc.tenant_day`
  - guard: `ebb.npc.demo.guard`, `ebb.npc.demo.guard_day`, `ebb.npc.guard`, `ebb.npc.guard_day`
- Refreshed `26.1.2-Fabric-Ebb-Test` with the rebuilt jar and updated command history/examples.
- Moved client synced-interaction cleanup from play `JOIN` to play `INIT`, so stale data is cleared before login sync payloads instead of risking clearing freshly synced block-group/entity-binding data after join.
- Added `scripts/gui_retest_issue_audit.py` and wired it into `scripts/run_smoke_checks.sh` so command accessibility, role binding distinctness, packaged block groups, refreshed profile jar, and `新的世界 (1)` save evidence are machine-checkable.

## Save inspection evidence

The save `新的世界 (1)` contains the expected P8 block objects at the bundled block-group coordinates:

- `locked_door`: `(0,64,4)` and `(0,65,4)` → `minecraft:oak_planks`
- `counter_ledger`: `(2,64,1)` → `minecraft:lectern`
- `notice_board`: `(1,65,0)` → `minecraft:oak_sign`
- `washroom_mirror`: `(-2,64,6)` → `minecraft:glass`
- `windowsill_ash`: `(4,65,6)` → `minecraft:gray_wool`
- `tenant_luggage`: `(8,64,3)` → `minecraft:chest`
- `cellar_hatch`: `(6,63,7)` → `minecraft:acacia_fence`
- `back_door`: `(10,64,5)` and `(10,65,5)` → `minecraft:oak_planks`

The same save contains old spawned role NPCs with legacy tags such as `ebb.npc.tenant_day`, now covered by the role bindings.

## Verification

- `scripts/gradle-local.sh --no-daemon test` → `BUILD SUCCESSFUL in 3m 47s`; includes `guiRetestCommandsRoleBindingsAndBlockGroupsAreRegistered`, which verifies `/ebb journal`, `/ebb quest`, `/ebb dialogue vars`, `/ebb vars`, distinct role bindings, legacy role tags, and all 8 block groups from bundled data.
- `scripts/gradle-local.sh --no-daemon runGametestServer --args nogui` → all 6 required GameTests passed; includes runtime role-tag binding and all-8-block-group checks.
- `scripts/run_smoke_checks.sh` → passed; loaded 13 dialogues, 8 block groups, 6 entity bindings, 5 routines, 0 validation messages; includes `GuiRetestIssueAudit`.
- `git diff --check` → no whitespace errors.
- Rebuilt jar SHA-256: `da8da3aaa3769bbbb8b1324fe4bedf56ddc497ef26fb355766df40316285cf94`.
- Installed test-profile jar SHA-256: `da8da3aaa3769bbbb8b1324fe4bedf56ddc497ef26fb355766df40316285cf94`.
- Installed jar inspection: 6 entity bindings and all 8 P8 block-group JSON resources are packaged.
- `scripts/gui_retest_issue_audit.py --save-path ... --require-save` → passed with profile jar hash `da8da3aaa3769bbbb8b1324fe4bedf56ddc497ef26fb355766df40316285cf94` and save path `新的世界 (1)`.


## Latest client log note

`26.1.2-Fabric-Ebb-Test/logs/latest.log` still shows the problematic 16:37 client session loading only 3 dialogues, 1 block group, 2 entity bindings, and 1 routine. That is the old jar already loaded in memory. The profile-local jar on disk has since been replaced with hash `da8da3aaa3769bbbb8b1324fe4bedf56ddc497ef26fb355766df40316285cf94`; Minecraft must be fully closed and relaunched before the fixes can appear in-game.

A helper check is now available from the project root:

```bash
scripts/check_pcl_runtime_loaded.py
```

Before relaunch, it should report the stale counts above and say `latest.log is older than the installed Ebb jar`. After relaunching `26.1.2-Fabric-Ebb-Test` and entering `新的世界 (1)`, it should pass with at least `dialogues=13`, `block_groups=8`, `entity_bindings=6`, and `npc_routines=5`.

## Manual retest notes

Fully close and relaunch Minecraft before retesting; a running client will keep using the old classes already loaded in memory.

In `新的世界 (1)`, after relaunch:

1. Run `/ebb data`; expected block groups = 8, entity bindings = 6, routines = 5.
2. Try `/ebb journal`, `/ebb quest`, `/ebb dialogue vars`, and `/ebb vars`.
3. Aim at each existing role NPC; tenant/witness/guard should no longer open innkeeper dialogue.
4. Aim at each listed block object within 2 blocks; each should show the `按 [X] 互动` prompt/highlight.

## 2026-06-02 GUI automation follow-up

After the client was relaunched and `新的世界 (1)` was entered, runtime loading of the previous refresh was confirmed with `dialogues=13`, `block_groups=8`, `entity_bindings=6`, and `npc_routines=5`. Automated command entry was then hardened to use `/` command-line entry plus clipboard paste; this avoids localized-keyboard races where per-character typing could trigger gameplay keybinds.

The command part of the GUI retest now shows `/ebb journal`, `/ebb quest`, `/ebb dialogue vars`, and `/ebb vars` executing without the earlier invalid-command suggestions. The remaining entity-prediction risk was narrowed to role NPC client prediction when relying only on tags. To make the role NPCs predictable for dedicated-style clients that can see custom names but not necessarily server tags, four extra role-specific name bindings were added:

- `demo/innkeeper_ebb_npc_name.json` → `Ebb NPC: innkeeper_day` → `ebb:demo/innkeeper_intro`
- `demo/witness_ebb_npc_name.json` → `Ebb NPC: witness_day` → `ebb:demo/witness_intro`
- `demo/tenant_ebb_npc_name.json` → `Ebb NPC: tenant_day` → `ebb:demo/tenant_intro`
- `demo/guard_ebb_npc_name.json` → `Ebb NPC: guard_day` → `ebb:demo/guard_intro`

The profile-local jar was refreshed to SHA-256 `03639e9834306a475487dbd32ba7f0079838de3bd2f70bf3a94cf37f08934133`. Because the Minecraft process keeps the old jar in memory, the current `latest.log` is expected to remain stale until the process is fully closed and relaunched. After relaunch, `scripts/check_pcl_runtime_loaded.py` should pass with at least `entity_bindings=10`.

## 2026-06-02 final GUI pass

The final pass found one more singleplayer/integrated-server-only sync bug: client INIT cleared the shared `EntityBindingRegistry`, so the in-process server serialized `EntityBindingSyncPayload` with 0 definitions. Dedicated servers would not share that static registry, but singleplayer testing exposed it. The client clear path now preserves the shared integrated-server entity binding registry at INIT while still clearing stale synced registries for dedicated clients.

Final runtime and visual checks after closing/relaunching `26.1.2-Fabric-Ebb-Test`:

- Runtime jar hash: `23540536daaeda7e054813ee10fa4ce653fca1085876fce058f8a2819e3e3ec3`
- Runtime counts: `dialogues=13`, `block_groups=8`, `entity_bindings=10`, `npc_routines=5`
- GUI E2E report: `build/gui-e2e/gui-retest-report.json`, 127 steps, 0 failures
- Visual contact sheet: `build/gui-e2e/contact_dialogues_final_verified.png`

The contact sheet shows the four player-facing command surfaces, four distinct role NPC dialogues, and all eight interactable block-group dialogues opening from the actual Windows client.
