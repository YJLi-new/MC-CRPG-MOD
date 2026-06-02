# GUI E2E Automation — Mineflayer, MineDojo-compatible Env, and Screenshots

Date: 2026-06-01 Asia/Shanghai

## What was added

- Local Node automation under `tools/gui_automation/node` with `mineflayer`, `minecraft-protocol`, and `minecraft-data`.
- A high-version adapter for Minecraft/Fabric `26.1.2`: protocol metadata resolves to protocol `775` / data version `4790`, while missing 26.1 data tables are aliased to the newest compatible 1.21.x data tables.
- Python orchestration under `tools/gui_automation/python` with:
  - `EbbGuiEnv`: MineDojo-style reset/step/report surface.
  - Windows window focus/input/screenshot helper.
  - runtime log parsing and screenshot color-signal assertions.
  - local Fabric server controller with explicit Minecraft EULA gate.
- New entrypoints:
  - `scripts/install_gui_automation_deps.py`
  - `scripts/gui_e2e_run.py`
  - `scripts/run_gui_automation_smoke.sh`

## Main commands

```bash
scripts/install_gui_automation_deps.py
scripts/run_gui_automation_smoke.sh
scripts/gui_e2e_run.py --scenario dry_run
scripts/gui_e2e_run.py --scenario runtime_check
scripts/gui_e2e_run.py --scenario bot_probe --port 25565
scripts/gui_e2e_run.py --scenario gui_retest --allow-stale-runtime --bot-probe
```

After the Windows client is relaunched into the refreshed jar:

```bash
scripts/gui_e2e_run.py --scenario gui_retest --gui --bot-probe
```

If a local dedicated server is desired, accept the Minecraft EULA yourself first and then use:

```bash
EBB_ACCEPT_MINECRAFT_EULA=true scripts/gui_e2e_run.py --scenario gui_retest --start-server --prepare-world-copy --bot-probe --gui
```

## Current verification

- `scripts/install_gui_automation_deps.py` installed local Node dependencies and Windows Python GUI dependencies.
- `npm --prefix tools/gui_automation/node run self-test` passes and reports selected version `26.1.2`, protocol `775`, data version `4790`.
- `scripts/gui_e2e_run.py --scenario dry_run` passes.
- `scripts/gui_e2e_run.py --scenario gui_retest --allow-stale-runtime --bot-probe` generates `build/gui-e2e/gui-retest-report.json` and the expected command/NPC/block manifest.
- `scripts/gui_e2e_run.py --scenario runtime_check` still reports the known external blocker: the latest Windows runtime log is from the old client session with counts `3/1/2/1`.

## Notes and limits

- The automation does not modify the vanilla `26.1.2` profile.
- The original `新的世界 (1)` save is only copied for local server testing; direct destructive edits are not applied to it.
- The full GUI visual pass still requires the Windows Minecraft client to be fully closed and relaunched so the latest log loads the refreshed `da8da3...` jar.
