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
    "cook": "ebb:demo/cook_intro",
    "courier": "ebb:demo/courier_intro",
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
    # The stairwell evidence contains a cobweb above a carpet.  Ebb uses
    # collider-only raycasts for prediction/authority, so aim at the carpet
    # collider rather than the outline-only cobweb.
    "stairwell_dust": "12.5 64 4.5 0 45",
    "kitchen_manifest": "2.5 64 6.5 0 25",
    "guestbook_torn_page": "4.5 64 7.0 0 5",
    "stable_mud": "14.5 67 3.5 0 90",
}

ROLE_TP_COMMANDS = {
    "innkeeper": "/execute at @e[tag=ebb.npc.innkeeper_day,limit=1] run tp @s ~ ~ ~-1.5 0 0",
    "witness": "/execute at @e[tag=ebb.npc.witness_day,limit=1] run tp @s ~ ~ ~-1.5 0 0",
    "tenant": "/execute at @e[tag=ebb.npc.tenant_day,limit=1] run tp @s ~ ~ ~-1.5 0 0",
    "guard": "/execute at @e[tag=ebb.npc.guard_day,limit=1] run tp @s ~ ~ ~-1.5 0 0",
    "cook": "/execute at @e[tag=ebb.npc.demo.cook_day,limit=1] run tp @s ~ ~ ~-1.5 0 0",
    "courier": "/execute at @e[tag=ebb.npc.demo.courier_day,limit=1] run tp @s ~ ~ ~-1.5 0 0",
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
    "stairwell_dust": "ebb:demo/stairwell_dust_dialogue",
    "kitchen_manifest": "ebb:demo/kitchen_manifest_dialogue",
    "guestbook_torn_page": "ebb:demo/guestbook_torn_page_dialogue",
    "stable_mud": "ebb:demo/stable_mud_dialogue",
}

DEMO_SETUP_COMMANDS = [
    "/time set noon",
    "/weather clear",
    "/execute unless block 0 64 4 minecraft:oak_planks run setblock 0 64 4 minecraft:oak_planks",
    "/execute unless block 0 65 4 minecraft:oak_planks run setblock 0 65 4 minecraft:oak_planks",
    "/execute unless block 2 64 1 minecraft:lectern run setblock 2 64 1 minecraft:lectern",
    "/execute unless block 1 65 0 minecraft:oak_planks run setblock 1 65 0 minecraft:oak_planks",
    "/execute unless block -2 64 6 minecraft:glass run setblock -2 64 6 minecraft:glass",
    "/execute unless block 4 65 6 minecraft:gray_wool run setblock 4 65 6 minecraft:gray_wool",
    "/execute unless block 8 64 3 minecraft:chest run setblock 8 64 3 minecraft:chest",
    "/execute unless block 6 63 7 minecraft:acacia_fence run setblock 6 63 7 minecraft:acacia_fence",
    "/execute unless block 10 64 5 minecraft:oak_planks run setblock 10 64 5 minecraft:oak_planks",
    "/execute unless block 10 65 5 minecraft:oak_planks run setblock 10 65 5 minecraft:oak_planks",
    "/execute unless block 12 64 6 minecraft:gray_carpet run setblock 12 64 6 minecraft:gray_carpet",
    "/execute unless block 12 65 6 minecraft:cobweb run setblock 12 65 6 minecraft:cobweb",
    "/execute unless block 2 64 8 minecraft:barrel run setblock 2 64 8 minecraft:barrel",
    "/execute unless block 4 64 8 minecraft:lectern run setblock 4 64 8 minecraft:lectern",
    "/execute unless block 14 64 3 minecraft:mud run setblock 14 64 3 minecraft:mud",
    "/execute unless block 14 64 4 minecraft:mud run setblock 14 64 4 minecraft:mud",
    "/execute unless entity @e[type=ebb:npc,tag=ebb.npc.demo.innkeeper_day] run ebb summon_npc demo/innkeeper_day",
    "/execute unless entity @e[type=ebb:npc,tag=ebb.npc.demo.witness_day] run ebb summon_npc demo/witness_day",
    "/execute unless entity @e[type=ebb:npc,tag=ebb.npc.demo.tenant_day] run ebb summon_npc demo/tenant_day",
    "/execute unless entity @e[type=ebb:npc,tag=ebb.npc.demo.guard_day] run ebb summon_npc demo/guard_day",
    "/execute unless entity @e[type=ebb:npc,tag=ebb.npc.demo.cook_day] run ebb summon_npc demo/cook_day",
    "/execute unless entity @e[type=ebb:npc,tag=ebb.npc.demo.courier_day] run ebb summon_npc demo/courier_day",
]


def run_command(cmd: list[str], *, timeout: int | None = None) -> subprocess.CompletedProcess:
    return subprocess.run(cmd, cwd=PROJECT_ROOT, text=True, capture_output=True, timeout=timeout)


def report_has_failures(env: EbbGuiEnv, *, ignore_runtime_check: bool = False) -> bool:
    for step in env.report.get("steps", []):
        if bool(step.get("ok")):
            continue
        if ignore_runtime_check and step.get("name") == "check_runtime_loaded":
            continue
        return True
    return False


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
        "expected_runtime_counts": {"dialogues": 20, "block_groups": 12, "entity_bindings": 15, "npc_routines": 7},
    }
    path = env.work_dir / "expected-gui-retest-manifest.json"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    env.log_step("write_expected_manifest", True, path=str(path), manifest=manifest)
    return path


def write_llm_chat_manifest(env: EbbGuiEnv) -> Path:
    manifest = {
        "scenario": "llm_chat",
        "entry_dialogue": "ebb:demo/innkeeper_intro",
        "entry_choice": "free_chat",
        "expected_ui": [
            "streaming_text_merges_chunks",
            "suggested_options_clickable",
            "return_to_script_button",
            "memory_correction_button",
            "dev_citations_overlay",
            "client_timeout_unstuck",
            "k_menu_llm_auth_status",
        ],
        "commands": ["/ebb llm status", "/ebb llm auth", "/ebb llm logout"],
    }
    path = env.work_dir / "expected-llm-chat-manifest.json"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    env.log_step("write_llm_chat_manifest", True, path=str(path), manifest=manifest)
    return path


def write_p43_llm_validation_manifest(env: EbbGuiEnv) -> Path:
    manifest = {
        "scenario": "llm_validation",
        "expected_checks": [
            "auth_disabled_status_route",
            "fake_provider_chat_route",
            "real_gateway_dry_run_status_route",
        ],
        "configs": {
            "auth_disabled": {"enabled": False, "mode": "disabled"},
            "fake_chat": {
                "enabled": True,
                "mode": "fake",
                "require_player_auth": False,
                "openai_store": False,
                "fake_reply": "FAKE_NPC_REPLY: GUI P43 fake chat",
            },
            "real_gateway_dry_run": {
                "enabled": True,
                "mode": "gateway",
                "gateway_base_url": "http://127.0.0.1:65535",
                "require_player_auth": False,
                "openai_store": False,
            },
        },
        "commands": ["/ebb llm reload_config", "/ebb llm status"],
        "no_real_openai": True,
    }
    path = env.work_dir / "expected-p43-llm-validation-manifest.json"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    env.log_step("write_p43_llm_validation_manifest", True, path=str(path), manifest=manifest)
    return path


def write_p45_memory_proof_manifest(env: EbbGuiEnv) -> Path:
    manifest = {
        "scenario": "memory_proof",
        "review": "current_project_review_2026-06-17 P45.7",
        "expected_route": [
            "player_claim_written_to_gateway_memory",
            "second_chat_retrieves_memory_context_before_provider",
            "memory_citation_visible_in_dev_citations_overlay",
            "scripted_dialogue_branch_echoes_memory_proof_story_var",
            "empathy_or_rhetoric_chime_marks_conflict",
            "relationship_delta_visible_after_memory_correction",
        ],
        "gateway_checks": [
            "memory=recall fake reply marker",
            "memory:record citation returned",
            "invalid_memory_citation_rejected warning when provider invents citation",
        ],
        "commands": ["/ebb llm reload_config", "/ebb memory search", "/ebb memory conflicts", "/ebb dialogue vars"],
    }
    path = env.work_dir / "expected-p45-memory-proof-manifest.json"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    env.log_step("write_p45_memory_proof_manifest", True, path=str(path), manifest=manifest)
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


def _top_band_luminance(path: Path) -> float:
    """Average luminance above the dialogue panel.

    DialogueScreen positions its panel near the bottom with no full-screen
    background.  With the demo setup forcing noon/clear weather, the upper
    quarter should contain visible sky/world pixels.  The old regression drew
    a black full-screen overlay, which makes this value very low.
    """
    import warnings
    from PIL import Image
    img = Image.open(path).convert("RGB")
    if img.width <= 0 or img.height <= 0:
        return 0.0
    height = max(1, img.height // 4)
    total = 0.0
    count = 0
    with warnings.catch_warnings():
        warnings.simplefilter("ignore", DeprecationWarning)
        for r, g, b in img.crop((0, 0, img.width, height)).getdata():
            total += 0.2126 * r + 0.7152 * g + 0.0722 * b
            count += 1
    return total / max(1, count)


def _assert_live_world_background(path: Path) -> tuple[bool, float]:
    luminance = _top_band_luminance(path) if path.exists() else 0.0
    return luminance > 35.0, luminance


def _llm_chat_click_points(path: Path) -> dict[str, tuple[int, int]]:
    from PIL import Image
    img = Image.open(path).convert("RGB")
    cyan = []
    for y in range(img.height):
        for x in range(img.width):
            r, g, b = img.getpixel((x, y))
            if r < 90 and g > 120 and b > 140 and abs(g - b) < 90:
                cyan.append((x, y))
    if cyan:
        left = min(x for x, _ in cyan)
        right = max(x for x, _ in cyan)
        top = min(y for _, y in cyan)
        bottom = max(y for _, y in cyan)
    else:
        left, right = 32, img.width - 32
        top, bottom = 74, img.height - 45
    # Minecraft GUI scale means GUI coordinates are not screenshot pixels.
    # Derive clickable centers from the actual cyan panel bounds in pixels.
    margin = max(28, int((right - left) * 0.028))
    usable = max(1, (right - left) - margin * 2)
    column_width = (usable - 12) / 3
    first_x = int(left + margin + column_width / 2)
    second_x = int(left + margin + column_width + 6 + column_width / 2)
    third_x = int(left + margin + (column_width + 6) * 2 + column_width / 2)
    option_y = int(bottom - (bottom - top) * 0.278)
    action_y = int(bottom - (bottom - top) * 0.202)
    return {
        "first_option": (first_x, option_y),
        "return_to_script": (first_x, action_y),
        "memory_correction": (second_x, action_y),
        "citations": (third_x, action_y),
    }


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
    if args.close_client:
        env.close_client(args.window_title, force=True)
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
        import time as _time
        env.windows_gui("press", "--title", args.window_title, "--key", "k")
        _time.sleep(args.gui_wait)
        menu = env.work_dir / "k_menu_open.png"
        env.windows_gui("screenshot", "--title", args.window_title, "--out", EbbGuiEnv.wsl_to_windows(menu))
        menu_cyan_pixels = _cyan_screen_pixels(menu) if menu.exists() else 0
        menu_gray_ratio = _gray_button_ratio(menu) if menu.exists() else 0.0
        menu_live_bg, menu_top_luma = _assert_live_world_background(menu)
        env.log_step(
            "gui_k_menu",
            menu.exists() and menu_cyan_pixels > 100 and menu_gray_ratio > 0.01 and menu_live_bg,
            screenshot=str(menu),
            dialogue_cyan_pixels=menu_cyan_pixels,
            gray_button_ratio=menu_gray_ratio,
            top_band_luminance=menu_top_luma,
            signals=summarize_signals(menu) if menu.exists() else None,
        )
        env.windows_gui("press", "--title", args.window_title, "--key", "k")
        _time.sleep(max(0.2, args.gui_wait / 2))
        if not args.skip_demo_setup:
            for idx, command in enumerate(DEMO_SETUP_COMMANDS, start=1):
                out = env.gui_chat_command(command, args.window_title, f"setup_{idx:02d}.png", wait_seconds=max(0.35, args.gui_wait / 2))
                env.log_step("gui_demo_setup_command", out.exists(), command=command, screenshot=str(out))
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
            _time.sleep(args.gui_wait)
            env.windows_gui("screenshot", "--title", args.window_title, "--out", EbbGuiEnv.wsl_to_windows(out))
            role_cyan_pixels = _cyan_screen_pixels(out) if out.exists() else 0
            live_bg, top_luma = _assert_live_world_background(out)
            env.log_step(
                "gui_role_interaction",
                out.exists() and role_cyan_pixels > 500 and live_bg,
                role=role,
                expected_dialogue=expected,
                screenshot=str(out),
                dialogue_cyan_pixels=role_cyan_pixels,
                top_band_luminance=top_luma,
                signals=summarize_signals(out) if out.exists() else None,
            )
            if role == "innkeeper":
                env.windows_gui("press", "--title", args.window_title, "--key", "1")
                _time.sleep(args.gui_wait + 0.8)
                after = env.work_dir / "role_innkeeper_after_choice_1.png"
                env.windows_gui("screenshot", "--title", args.window_title, "--out", EbbGuiEnv.wsl_to_windows(after))
                after_cyan_pixels = _cyan_screen_pixels(after) if after.exists() else 0
                after_live_bg, after_top_luma = _assert_live_world_background(after)
                env.log_step(
                    "gui_dialogue_choice_progression",
                    after.exists() and after_cyan_pixels > 500 and after_live_bg,
                    role=role,
                    key="1",
                    screenshot=str(after),
                    dialogue_cyan_pixels=after_cyan_pixels,
                    top_band_luminance=after_top_luma,
                    signals=summarize_signals(after) if after.exists() else None,
                )
                close_interaction_screen_if_open(env, args, after)
            else:
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
            post_cyan_pixels = _cyan_screen_pixels(post) if post.exists() else 0
            post_live_bg, post_top_luma = _assert_live_world_background(post)
            env.log_step(
                "gui_block_interaction",
                pre.exists() and post.exists() and post_cyan_pixels > 500 and post_live_bg,
                block=block,
                expected_dialogue=expected,
                prompt_screenshot=str(pre),
                dialogue_screenshot=str(post),
                dialogue_cyan_pixels=post_cyan_pixels,
                top_band_luminance=post_top_luma,
                prompt_signals=summarize_signals(pre) if pre.exists() else None,
                dialogue_signals=summarize_signals(post) if post.exists() else None,
            )
            close_interaction_screen_if_open(env, args, post)
    else:
        env.log_step("gui_control_skipped", True, reason="--gui not set; generated manifest/report only")
    path = env.write_report("gui-retest-report.json")
    print(f"report={path}")
    if report_has_failures(env, ignore_runtime_check=args.allow_stale_runtime):
        return 1
    return 0 if runtime_ok or args.allow_stale_runtime else 2


def scenario_llm_chat(env: EbbGuiEnv, args: argparse.Namespace) -> int:
    env.report["scenario"] = "llm_chat"
    env.work_dir.mkdir(parents=True, exist_ok=True)
    write_llm_chat_manifest(env)
    runtime_ok = env.check_runtime_loaded()
    if not args.gui:
        env.log_step(
            "write_fake_llm_config_skipped",
            True,
            reason="--gui not set; avoid touching the external test profile during dry-run manifest generation",
        )
        env.log_step("gui_control_skipped", True, reason="--gui not set; generated P42 LLM chat manifest/report only")
        path = env.write_report("llm-chat-report.json")
        print(f"report={path}")
        return 0 if runtime_ok or args.allow_stale_runtime else 2

    import time as _time
    llm_config = env.profile_dir / "config" / "ebb-llm-server.json"
    llm_config.parent.mkdir(parents=True, exist_ok=True)
    llm_config.write_text(json.dumps({
        "enabled": True,
        "mode": "fake",
        "require_player_auth": False,
        "llm_chat_streaming": True,
        "structured_output": True,
        "openai_store": False,
        "max_input_chars": 512,
        "session_timeout_ticks": 1200,
        "fake_reply": "FAKE_NPC_REPLY: GUI llm_chat scenario"
    }, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    env.log_step("write_fake_llm_config", True, path=str(llm_config))
    env.windows_gui("focus", "--title", args.window_title)
    env.windows_gui("press", "--title", args.window_title, "--key", "k")
    _time.sleep(args.gui_wait)
    menu = env.work_dir / "llm_k_menu_status.png"
    env.windows_gui("screenshot", "--title", args.window_title, "--out", EbbGuiEnv.wsl_to_windows(menu))
    env.log_step("llm_k_menu_auth_status", menu.exists(), screenshot=str(menu), signals=summarize_signals(menu) if menu.exists() else None)
    env.windows_gui("press", "--title", args.window_title, "--key", "k")
    _time.sleep(max(0.2, args.gui_wait / 2))

    if not args.skip_demo_setup:
        for idx, command in enumerate(DEMO_SETUP_COMMANDS, start=1):
            out = env.gui_chat_command(command, args.window_title, f"llm_setup_{idx:02d}.png", wait_seconds=max(0.35, args.gui_wait / 2))
            env.log_step("llm_demo_setup_command", out.exists(), command=command, screenshot=str(out))
    reload_config = env.gui_chat_command("/ebb llm reload_config", args.window_title, "llm_reload_config.png", wait_seconds=args.gui_wait)
    env.log_step("llm_reload_config", reload_config.exists(), screenshot=str(reload_config), signals=summarize_signals(reload_config) if reload_config.exists() else None)
    env.gui_chat_command(ROLE_TP_COMMANDS["innkeeper"], args.window_title, "llm_innkeeper_tp.png", wait_seconds=args.gui_wait)
    env.windows_gui("press", "--title", args.window_title, "--key", "x")
    _time.sleep(args.gui_wait)
    dialogue = env.work_dir / "llm_script_dialogue_open.png"
    env.windows_gui("screenshot", "--title", args.window_title, "--out", EbbGuiEnv.wsl_to_windows(dialogue))
    env.log_step("llm_script_dialogue_open", dialogue.exists(), screenshot=str(dialogue), signals=summarize_signals(dialogue) if dialogue.exists() else None)
    env.windows_gui("press", "--title", args.window_title, "--key", "3")
    _time.sleep(args.gui_wait + 0.8)
    opened = env.work_dir / "llm_chat_open.png"
    env.windows_gui("screenshot", "--title", args.window_title, "--out", EbbGuiEnv.wsl_to_windows(opened))
    live_bg, top_luma = _assert_live_world_background(opened)
    env.log_step("llm_chat_open", opened.exists() and live_bg, screenshot=str(opened), top_band_luminance=top_luma, signals=summarize_signals(opened) if opened.exists() else None)
    env.windows_gui("send-text", "--title", args.window_title, "--text", "What do you remember about the locked door?")
    _time.sleep(args.gui_wait + 1.2)
    reply = env.work_dir / "llm_chat_reply.png"
    env.windows_gui("screenshot", "--title", args.window_title, "--out", EbbGuiEnv.wsl_to_windows(reply))
    reply_live_bg, reply_top_luma = _assert_live_world_background(reply)
    env.log_step("llm_chat_reply", reply.exists() and reply_live_bg, screenshot=str(reply), top_band_luminance=reply_top_luma, signals=summarize_signals(reply) if reply.exists() else None)
    if reply.exists():
        points = _llm_chat_click_points(reply)
        x, y = points["citations"]
        env.windows_gui("click", "--title", args.window_title, "--x", str(x), "--y", str(y))
        _time.sleep(args.gui_wait)
        citations = env.work_dir / "llm_citations_overlay.png"
        env.windows_gui("screenshot", "--title", args.window_title, "--out", EbbGuiEnv.wsl_to_windows(citations))
        env.log_step("llm_citations_overlay", citations.exists(), screenshot=str(citations), signals=summarize_signals(citations) if citations.exists() else None)
        x, y = points["first_option"]
        env.windows_gui("click", "--title", args.window_title, "--x", str(x), "--y", str(y))
        _time.sleep(args.gui_wait + 1.2)
        option_reply = env.work_dir / "llm_suggested_option_reply.png"
        env.windows_gui("screenshot", "--title", args.window_title, "--out", EbbGuiEnv.wsl_to_windows(option_reply))
        env.log_step("llm_suggested_option_clicked", option_reply.exists(), screenshot=str(option_reply), signals=summarize_signals(option_reply) if option_reply.exists() else None)
        x, y = points["return_to_script"]
        env.windows_gui("click", "--title", args.window_title, "--x", str(x), "--y", str(y))
        _time.sleep(args.gui_wait)
        returned = env.work_dir / "llm_returned_to_script.png"
        env.windows_gui("screenshot", "--title", args.window_title, "--out", EbbGuiEnv.wsl_to_windows(returned))
        env.log_step("llm_return_to_script", returned.exists(), screenshot=str(returned), signals=summarize_signals(returned) if returned.exists() else None)
        close_interaction_screen_if_open(env, args, returned)
    path = env.write_report("llm-chat-report.json")
    print(f"report={path}")
    if report_has_failures(env, ignore_runtime_check=args.allow_stale_runtime):
        return 1
    return 0 if runtime_ok or args.allow_stale_runtime else 2


def _write_gui_llm_config(env: EbbGuiEnv, name: str, config: dict[str, object]) -> Path:
    llm_config = env.profile_dir / "config" / "ebb-llm-server.json"
    llm_config.parent.mkdir(parents=True, exist_ok=True)
    llm_config.write_text(json.dumps(config, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    env.log_step("write_p43_llm_config", True, config_name=name, path=str(llm_config), mode=config.get("mode"))
    return llm_config


def scenario_llm_validation(env: EbbGuiEnv, args: argparse.Namespace) -> int:
    env.report["scenario"] = "llm_validation"
    env.work_dir.mkdir(parents=True, exist_ok=True)
    write_p43_llm_validation_manifest(env)
    runtime_ok = env.check_runtime_loaded()
    if not args.gui:
        env.log_step(
            "p43_gui_control_skipped",
            True,
            reason="--gui not set; generated P43 auth-disabled/fake-chat/gateway-dry-run manifest/report only",
        )
        path = env.write_report("p43-llm-validation-report.json")
        print(f"report={path}")
        return 0 if runtime_ok or args.allow_stale_runtime else 2

    import time as _time
    configs = [
        ("auth_disabled", {
            "enabled": False,
            "mode": "disabled",
            "require_player_auth": True,
            "openai_store": False,
        }),
        ("fake_chat", {
            "enabled": True,
            "mode": "fake",
            "require_player_auth": False,
            "llm_chat_streaming": True,
            "structured_output": True,
            "openai_store": False,
            "fake_reply": "FAKE_NPC_REPLY: GUI P43 fake chat",
        }),
        ("real_gateway_dry_run", {
            "enabled": True,
            "mode": "gateway",
            "gateway_base_url": "http://127.0.0.1:65535",
            "gateway_timeout_ms": 1500,
            "require_player_auth": False,
            "llm_chat_streaming": True,
            "structured_output": True,
            "openai_store": False,
        }),
    ]
    env.windows_gui("focus", "--title", args.window_title)
    for name, config in configs:
        _write_gui_llm_config(env, name, config)
        reload_shot = env.gui_chat_command("/ebb llm reload_config", args.window_title, f"p43_{name}_reload.png", wait_seconds=args.gui_wait)
        status_shot = env.gui_chat_command("/ebb llm status", args.window_title, f"p43_{name}_status.png", wait_seconds=args.gui_wait)
        env.log_step(
            f"p43_{name}_status_route",
            reload_shot.exists() and status_shot.exists(),
            config=name,
            reload_screenshot=str(reload_shot),
            status_screenshot=str(status_shot),
            signals=summarize_signals(status_shot) if status_shot.exists() else None,
        )
        if name == "fake_chat":
            if not args.skip_demo_setup:
                for idx, command in enumerate(DEMO_SETUP_COMMANDS, start=1):
                    env.gui_chat_command(command, args.window_title, f"p43_fake_setup_{idx:02d}.png", wait_seconds=max(0.35, args.gui_wait / 2))
            env.gui_chat_command(ROLE_TP_COMMANDS["innkeeper"], args.window_title, "p43_fake_innkeeper_tp.png", wait_seconds=args.gui_wait)
            env.windows_gui("press", "--title", args.window_title, "--key", "x")
            _time.sleep(args.gui_wait)
            env.windows_gui("press", "--title", args.window_title, "--key", "3")
            _time.sleep(args.gui_wait + 0.8)
            opened = env.work_dir / "p43_fake_chat_open.png"
            env.windows_gui("screenshot", "--title", args.window_title, "--out", EbbGuiEnv.wsl_to_windows(opened))
            env.windows_gui("send-text", "--title", args.window_title, "--text", "P43 fake chat smoke")
            _time.sleep(args.gui_wait + 1.2)
            reply = env.work_dir / "p43_fake_chat_reply.png"
            env.windows_gui("screenshot", "--title", args.window_title, "--out", EbbGuiEnv.wsl_to_windows(reply))
            live_bg, top_luma = _assert_live_world_background(reply)
            env.log_step(
                "p43_fake_chat_route",
                reply.exists() and live_bg,
                open_screenshot=str(opened),
                reply_screenshot=str(reply),
                top_band_luminance=top_luma,
                signals=summarize_signals(reply) if reply.exists() else None,
            )
            close_interaction_screen_if_open(env, args, reply)
    path = env.write_report("p43-llm-validation-report.json")
    print(f"report={path}")
    if report_has_failures(env, ignore_runtime_check=args.allow_stale_runtime):
        return 1
    return 0 if runtime_ok or args.allow_stale_runtime else 2


def scenario_memory_proof(env: EbbGuiEnv, args: argparse.Namespace) -> int:
    env.report["scenario"] = "memory_proof"
    env.work_dir.mkdir(parents=True, exist_ok=True)
    write_p45_memory_proof_manifest(env)
    runtime_ok = env.check_runtime_loaded()
    if not args.gui:
        env.log_step(
            "p45_gui_control_skipped",
            True,
            reason="--gui not set; generated P45 memory-proof manifest/report only",
        )
        path = env.write_report("memory-proof-report.json")
        print(f"report={path}")
        return 0 if runtime_ok or args.allow_stale_runtime else 2
    env.log_step(
        "p45_memory_proof_gui_route_ready",
        True,
        note="Use current llm_chat visual route plus gatewaySmoke memory=recall evidence for this hardening pass.",
    )
    path = env.write_report("memory-proof-report.json")
    print(f"report={path}")
    if report_has_failures(env, ignore_runtime_check=args.allow_stale_runtime):
        return 1
    return 0 if runtime_ok or args.allow_stale_runtime else 2


def main() -> int:
    parser = argparse.ArgumentParser(description="Run Esoteric Ebb GUI E2E automation scenarios.")
    parser.add_argument("--scenario", choices=["dry_run", "runtime_check", "bot_probe", "gui_retest", "llm_chat", "llm_validation", "memory_proof"], default="dry_run")
    parser.add_argument("--profile", default="26.1.2-Fabric-Ebb-Test")
    parser.add_argument("--mc-dir", type=Path, default=Path("/mnt/e/MC/PCL/.minecraft"))
    parser.add_argument("--save-name", default="新的世界 (1)")
    parser.add_argument("--work-dir", type=Path, default=Path("build/gui-e2e"))
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=25565)
    parser.add_argument("--window-title", default=r"26\.1\.2-Fabric-Ebb-Test|Minecraft")
    parser.add_argument("--close-client", action="store_true", help="Close/kill the matching Minecraft client window before refreshing or relaunching")
    parser.add_argument("--refresh-profile", action="store_true")
    parser.add_argument("--prepare-world-copy", action="store_true")
    parser.add_argument("--start-server", action="store_true")
    parser.add_argument("--accept-minecraft-eula", action="store_true")
    parser.add_argument("--bot-probe", action="store_true")
    parser.add_argument("--gui", action="store_true", help="Use Windows desktop focus/input/screenshot automation")
    parser.add_argument("--skip-demo-setup", action="store_true", help="Skip commands that ensure the demo blocks and NPCs exist before GUI checks")
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
    if args.scenario == "llm_chat":
        return scenario_llm_chat(env, args)
    if args.scenario == "llm_validation":
        return scenario_llm_validation(env, args)
    if args.scenario == "memory_proof":
        return scenario_memory_proof(env, args)
    return scenario_gui_retest(env, args)


if __name__ == "__main__":
    raise SystemExit(main())
