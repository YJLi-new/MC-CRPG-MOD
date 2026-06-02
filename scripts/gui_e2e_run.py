#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import subprocess
import sys
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(PROJECT_ROOT / "tools" / "gui_automation" / "python"))

from ebb_gui_env import EbbGuiEnv  # noqa: E402
from image_assertions import summarize_signals  # noqa: E402
from server_controller import ServerController  # noqa: E402

ROLE_DIALOGUES = {
    "innkeeper": "ebb:demo/innkeeper_intro",
    "witness": "ebb:demo/witness_intro",
    "tenant": "ebb:demo/tenant_intro",
    "guard": "ebb:demo/guard_intro",
}

VIEWPOINTS = {
    "locked_door": "0.5 64 2.5 0 0",
    "counter_ledger": "2.5 64 3.0 180 35",
    "notice_board": "1.5 64 2.0 180 0",
    "washroom_mirror": "-1.5 64 8.5 180 30",
    "windowsill_ash": "4.5 64 4.7 0 3",
    "tenant_luggage": "8.5 64 1.5 0 30",
    "cellar_hatch": "6.5 64 5.5 0 47",
    "back_door": "10.5 64 3.5 0 20",
}

ROLE_TP_COMMANDS = {
    "innkeeper": "/execute at @e[tag=ebb.npc.innkeeper_day,limit=1] run tp @s ~ ~ ~-1.5 0 0",
    "witness": "/execute at @e[tag=ebb.npc.witness_day,limit=1] run tp @s ~ ~ ~-1.5 0 0",
    "tenant": "/execute at @e[tag=ebb.npc.tenant_day,limit=1] run tp @s ~ ~ ~-1.5 0 0",
    "guard": "/execute at @e[tag=ebb.npc.guard_day,limit=1] run tp @s ~ ~ ~-1.5 0 0",
}

BLOCK_DIALOGUES = {
    "locked_door": "ebb:demo/locked_door_dialogue",
    "counter_ledger": "ebb:demo/counter_ledger_dialogue",
    "notice_board": "ebb:demo/notice_board_dialogue",
    "washroom_mirror": "ebb:demo/washroom_mirror_dialogue",
    "windowsill_ash": "ebb:demo/windowsill_ash_dialogue",
    "tenant_luggage": "ebb:demo/tenant_luggage_dialogue",
    "cellar_hatch": "ebb:demo/cellar_hatch_dialogue",
    "back_door": "ebb:demo/back_door_dialogue",
}


def run_command(cmd: list[str], *, timeout: int | None = None) -> subprocess.CompletedProcess:
    return subprocess.run(cmd, cwd=PROJECT_ROOT, text=True, capture_output=True, timeout=timeout)


def scenario_dry_run(env: EbbGuiEnv, args: argparse.Namespace) -> int:
    env.report["scenario"] = "dry_run"
    checks = {
        "node_package": (PROJECT_ROOT / "tools/gui_automation/node/package.json").exists(),
        "node_modules": (PROJECT_ROOT / "tools/gui_automation/node/node_modules").exists(),
        "windows_gui_script": (PROJECT_ROOT / "tools/gui_automation/python/windows_gui.py").exists(),
        "source_world": env.source_world.exists(),
        "profile_dir": env.profile_dir.exists(),
    }
    for name, ok in checks.items():
        env.log_step(f"check_{name}", bool(ok))
    cp = run_command(["node", str(PROJECT_ROOT / "tools/gui_automation/node/bot.js"), "--self-test"], timeout=120)
    env.log_step("node_self_test", cp.returncode == 0, stdout=cp.stdout, stderr=cp.stderr)
    env.write_report()
    print(json.dumps(env.report, ensure_ascii=False, indent=2))
    return 0 if all(checks.values()) and cp.returncode == 0 else 1


def scenario_runtime_check(env: EbbGuiEnv, args: argparse.Namespace) -> int:
    env.report["scenario"] = "runtime_check"
    ok = env.check_runtime_loaded()
    path = env.write_report("runtime-check-report.json")
    print(f"report={path}")
    return 0 if ok else 1


def scenario_bot_probe(env: EbbGuiEnv, args: argparse.Namespace) -> int:
    env.report["scenario"] = "bot_probe"
    cp = env.probe_bot(host=args.host, port=args.port)
    path = env.write_report("bot-probe-report.json")
    print(cp.stdout, end="")
    if cp.stderr:
        print(cp.stderr, file=sys.stderr, end="")
    print(f"report={path}")
    return cp.returncode


def write_expected_manifest(env: EbbGuiEnv) -> Path:
    manifest = {
        "commands": ["/ebb journal", "/ebb quest", "/ebb dialogue vars", "/ebb vars"],
        "role_dialogues": ROLE_DIALOGUES,
        "block_dialogues": BLOCK_DIALOGUES,
        "expected_runtime_counts": {"dialogues": 13, "block_groups": 8, "entity_bindings": 10, "npc_routines": 5},
    }
    path = env.work_dir / "expected-gui-retest-manifest.json"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    env.log_step("write_expected_manifest", True, path=str(path), manifest=manifest)
    return path


def _cyan_screen_pixels(path: Path) -> int:
    import warnings
    from PIL import Image
    img = Image.open(path).convert("RGB")
    with warnings.catch_warnings():
        warnings.simplefilter("ignore", DeprecationWarning)
        pixels = img.getdata()
        return sum(1 for r, g, b in pixels if g > 120 and b > 120 and r < 80 and abs(g - b) < 80)


def _gray_button_ratio(path: Path) -> float:
    import warnings
    from PIL import Image
    img = Image.open(path).convert("RGB")
    total = img.width * img.height
    if total == 0:
        return 0.0
    with warnings.catch_warnings():
        warnings.simplefilter("ignore", DeprecationWarning)
        pixels = img.getdata()
        count = sum(1 for r, g, b in pixels if abs(r - g) < 20 and abs(g - b) < 20 and 80 < r < 190)
        return count / total


def close_interaction_screen_if_open(env: EbbGuiEnv, args: argparse.Namespace, screenshot: Path) -> None:
    if not screenshot.exists():
        return
    cyan_pixels = _cyan_screen_pixels(screenshot)
    gray_ratio = _gray_button_ratio(screenshot)
    env.log_step("analyze_interaction_screen", True, screenshot=str(screenshot), cyan_pixels=cyan_pixels, gray_button_ratio=gray_ratio)
    # Dialogue/dev screens have a cyan border; normal gameplay can contain lots
    # of gray pixels (planks, stone, rain, entities), so do not use gray alone as
    # a close signal or automation will accidentally open the pause menu.
    if cyan_pixels > 500:
        env.windows_gui("press", "--title", args.window_title, "--key", "escape")


def scenario_gui_retest(env: EbbGuiEnv, args: argparse.Namespace) -> int:
    env.report["scenario"] = "gui_retest"
    env.work_dir.mkdir(parents=True, exist_ok=True)
    write_expected_manifest(env)
    if args.refresh_profile:
        cp = env.refresh_profile()
        if cp.returncode != 0:
            env.write_report("gui-retest-report.json")
            return cp.returncode
    if args.prepare_world_copy:
        env.prepare_world_copy()
    if args.start_server:
        controller = ServerController(PROJECT_ROOT, env.work_dir, env.source_world)
        if args.prepare_world_copy:
            controller.prepare_world_copy()
        controller.start(port=args.port, accept_eula=args.accept_minecraft_eula)
        env.log_step("start_server", True, port=args.port)
    runtime_ok = env.check_runtime_loaded()
    if not runtime_ok and not args.allow_stale_runtime:
        env.log_step("stop_before_gui", False, reason="runtime_check_failed")
        path = env.write_report("gui-retest-report.json")
        print(f"Runtime check failed; report={path}", file=sys.stderr)
        return 2
    if args.bot_probe:
        env.probe_bot(host=args.host, port=args.port)
    if args.launch_pcl:
        env.launch_pcl(Path(args.pcl_exe))
    if args.gui:
        env.windows_gui("focus", "--title", args.window_title)
        shot = env.work_dir / "initial-client.png"
        env.windows_gui("screenshot", "--title", args.window_title, "--out", EbbGuiEnv.wsl_to_windows(shot))
        if shot.exists():
            env.log_step("analyze_initial_screenshot", True, signals=summarize_signals(shot))
        for idx, command in enumerate(["/ebb journal", "/ebb quest", "/ebb dialogue vars", "/ebb vars"], start=1):
            out = env.gui_chat_command(command, args.window_title, f"command_{idx}.png", wait_seconds=args.gui_wait)
            env.log_step("gui_command", out.exists(), command=command, screenshot=str(out), signals=summarize_signals(out) if out.exists() else None)
            # /ebb journal and /ebb quest deliberately open GUI screens; close
            # those before the next automated command.  The vars commands print
            # to chat only, so pressing Escape there would open the pause menu.
            if command in {"/ebb journal", "/ebb quest"}:
                env.windows_gui("press", "--title", args.window_title, "--key", "escape")
        for role, expected in ROLE_DIALOGUES.items():
            env.gui_chat_command(ROLE_TP_COMMANDS[role], args.window_title, f"role_{role}_tp.png", wait_seconds=args.gui_wait)
            out = env.work_dir / f"role_{role}_dialogue.png"
            env.windows_gui("press", "--title", args.window_title, "--key", "x")
            import time as _time
            _time.sleep(args.gui_wait)
            env.windows_gui("screenshot", "--title", args.window_title, "--out", EbbGuiEnv.wsl_to_windows(out))
            env.log_step("gui_role_interaction", out.exists(), role=role, expected_dialogue=expected, screenshot=str(out), signals=summarize_signals(out) if out.exists() else None)
            close_interaction_screen_if_open(env, args, out)
        for block, expected in BLOCK_DIALOGUES.items():
            tp = VIEWPOINTS[block]
            env.gui_chat_command(f"/tp @s {tp}", args.window_title, f"block_{block}_tp.png", wait_seconds=args.gui_wait)
            pre = env.work_dir / f"block_{block}_prompt.png"
            env.windows_gui("screenshot", "--title", args.window_title, "--out", EbbGuiEnv.wsl_to_windows(pre))
            env.windows_gui("press", "--title", args.window_title, "--key", "x")
            import time as _time
            _time.sleep(args.gui_wait)
            post = env.work_dir / f"block_{block}_dialogue.png"
            env.windows_gui("screenshot", "--title", args.window_title, "--out", EbbGuiEnv.wsl_to_windows(post))
            env.log_step("gui_block_interaction", pre.exists() and post.exists(), block=block, expected_dialogue=expected, prompt_screenshot=str(pre), dialogue_screenshot=str(post), prompt_signals=summarize_signals(pre) if pre.exists() else None, dialogue_signals=summarize_signals(post) if post.exists() else None)
            close_interaction_screen_if_open(env, args, post)
    else:
        env.log_step("gui_control_skipped", True, reason="--gui not set; generated manifest/report only")
    path = env.write_report("gui-retest-report.json")
    print(f"report={path}")
    return 0 if runtime_ok or args.allow_stale_runtime else 2


def main() -> int:
    parser = argparse.ArgumentParser(description="Run Esoteric Ebb GUI E2E automation scenarios.")
    parser.add_argument("--scenario", choices=["dry_run", "runtime_check", "bot_probe", "gui_retest"], default="dry_run")
    parser.add_argument("--profile", default="26.1.2-Fabric-Ebb-Test")
    parser.add_argument("--mc-dir", type=Path, default=Path("/mnt/e/MC/PCL/.minecraft"))
    parser.add_argument("--save-name", default="新的世界 (1)")
    parser.add_argument("--work-dir", type=Path, default=Path("build/gui-e2e"))
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=25565)
    parser.add_argument("--window-title", default=r"26\.1\.2-Fabric-Ebb-Test|Minecraft")
    parser.add_argument("--refresh-profile", action="store_true")
    parser.add_argument("--prepare-world-copy", action="store_true")
    parser.add_argument("--start-server", action="store_true")
    parser.add_argument("--accept-minecraft-eula", action="store_true")
    parser.add_argument("--bot-probe", action="store_true")
    parser.add_argument("--gui", action="store_true", help="Use Windows desktop focus/input/screenshot automation")
    parser.add_argument("--gui-wait", type=float, default=1.0)
    parser.add_argument("--launch-pcl", action="store_true")
    parser.add_argument("--pcl-exe", default="/mnt/e/MC/PCL/Plain Craft Launcher.exe")
    parser.add_argument("--allow-stale-runtime", action="store_true", help="Continue report generation even if latest client log still shows old jar data")
    args = parser.parse_args()
    env = EbbGuiEnv(PROJECT_ROOT, profile_id=args.profile, mc_dir=args.mc_dir, save_name=args.save_name, work_dir=args.work_dir)
    if args.scenario == "dry_run":
        return scenario_dry_run(env, args)
    if args.scenario == "runtime_check":
        return scenario_runtime_check(env, args)
    if args.scenario == "bot_probe":
        return scenario_bot_probe(env, args)
    return scenario_gui_retest(env, args)


if __name__ == "__main__":
    raise SystemExit(main())
