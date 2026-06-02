# Ebb GUI Automation

This toolchain combines:

- PrismarineJS/mineflayer for server probing and bot-side chat/state automation.
- A MineDojo-compatible Python environment (`EbbGuiEnv`) for reset/step/report orchestration.
- Windows Python GUI control and screenshots as the visual authority for Minecraft client behavior.

The implementation targets Minecraft/Fabric 26.1.2. Prismarine upstream may not expose exact 26.1.2 data; the adapter probes the server and reports whether it used exact support, a configured high-version fallback, or failed negotiation.

## Common commands

```bash
python3 scripts/install_gui_automation_deps.py
scripts/gui_e2e_run.py --scenario dry_run
scripts/gui_e2e_run.py --scenario runtime_check
scripts/gui_e2e_run.py --scenario bot_probe --port 25565
scripts/gui_e2e_run.py --scenario gui_retest --no-start-server
```

For a local dedicated server run, accept the Minecraft EULA yourself first, then pass `--accept-minecraft-eula` or set `EBB_ACCEPT_MINECRAFT_EULA=true`.
