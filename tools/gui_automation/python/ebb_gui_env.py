from __future__ import annotations

import json
import subprocess
import time
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

from runtime_log import RuntimeCounts, parse_runtime_counts
from server_controller import ServerController

EXPECTED_COUNTS = RuntimeCounts(dialogues=20, block_groups=12, entity_bindings=15, npc_routines=7)

@dataclass
class EbbGuiEnv:
    project_root: Path
    profile_id: str = "26.1.2-Fabric-Ebb-Test"
    mc_dir: Path = Path("/mnt/e/MC/PCL/.minecraft")
    save_name: str = "新的世界 (1)"
    work_dir: Path = Path("build/gui-e2e")
    report: dict[str, Any] = field(default_factory=lambda: {"steps": []})

    @property
    def profile_dir(self) -> Path:
        return self.mc_dir / "versions" / self.profile_id

    @property
    def source_world(self) -> Path:
        return self.profile_dir / "saves" / self.save_name

    @property
    def latest_log(self) -> Path:
        return self.profile_dir / "logs" / "latest.log"

    @property
    def node_bot(self) -> Path:
        return self.project_root / "tools" / "gui_automation" / "node" / "bot.js"

    def log_step(self, name: str, ok: bool, **payload):
        step = {"name": name, "ok": ok, "ts": time.strftime("%Y-%m-%dT%H:%M:%S%z"), **payload}
        self.report.setdefault("steps", []).append(step)
        return step

    def write_report(self, name: str = "report.json") -> Path:
        self.work_dir.mkdir(parents=True, exist_ok=True)
        path = self.work_dir / name
        path.write_text(json.dumps(self.report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        return path

    def refresh_profile(self) -> subprocess.CompletedProcess:
        cp = subprocess.run([str(self.project_root / "scripts" / "configure_pcl_test_client.sh")], cwd=self.project_root, text=True, capture_output=True)
        self.log_step("refresh_profile", cp.returncode == 0, stdout=cp.stdout[-4000:], stderr=cp.stderr[-4000:])
        return cp

    def runtime_counts(self) -> RuntimeCounts:
        return parse_runtime_counts(self.latest_log)

    def check_runtime_loaded(self) -> bool:
        cp = subprocess.run([str(self.project_root / "scripts" / "check_pcl_runtime_loaded.py")], cwd=self.project_root, text=True, capture_output=True)
        self.log_step("check_runtime_loaded", cp.returncode == 0, stdout=cp.stdout, stderr=cp.stderr)
        return cp.returncode == 0

    def prepare_world_copy(self) -> Path:
        controller = ServerController(self.project_root, self.work_dir, self.source_world)
        target = controller.prepare_world_copy()
        self.log_step("prepare_world_copy", True, source=str(self.source_world), target=str(target))
        return target

    def probe_bot(self, host: str = "127.0.0.1", port: int = 25565) -> subprocess.CompletedProcess:
        try:
            cp = subprocess.run(["node", str(self.node_bot), "probe", "--host", host, "--port", str(port), "--version", "26.1.2", "--timeout-ms", "5000"], cwd=self.project_root, text=True, capture_output=True, timeout=120)
        except subprocess.TimeoutExpired as exc:
            cp = subprocess.CompletedProcess(exc.cmd, 124, stdout=exc.stdout or "", stderr=(exc.stderr or "") + "\nprobe subprocess timeout")
        self.log_step("mineflayer_probe", cp.returncode == 0, stdout=cp.stdout, stderr=cp.stderr)
        return cp

    def windows_gui(self, action: str, *args: str, timeout: int = 30) -> subprocess.CompletedProcess:
        script = self.project_root / "tools" / "gui_automation" / "python" / "windows_gui.py"
        win_script = self.wsl_to_windows(script)
        # Invoke Windows Python directly through WSL interop.  A previous
        # PowerShell-command-string wrapper over-escaped regex backslashes (for
        # example `26\\.1` became a literal double-backslash pattern), which made
        # GUI tests unable to find the Minecraft window.
        cmd = ["py.exe", "-3.12", win_script, action, *[str(value) for value in args]]
        cp = subprocess.run(cmd, cwd=self.project_root, text=True, capture_output=True, timeout=timeout, errors="replace")
        self.log_step(f"windows_gui_{action}", cp.returncode == 0, stdout=cp.stdout, stderr=cp.stderr)
        return cp

    def launch_pcl(self, exe: Path = Path("/mnt/e/MC/PCL/Plain Craft Launcher.exe")) -> subprocess.CompletedProcess:
        return self.windows_gui("launch", "--exe", self.wsl_to_windows(exe), timeout=10)

    def close_client(self, window_title: str, force: bool = True) -> subprocess.CompletedProcess:
        args = ["close", "--title", window_title]
        if force:
            args.append("--force")
        return self.windows_gui(*args, timeout=20)

    def gui_chat_command(self, command: str, window_title: str, screenshot_name: str, wait_seconds: float = 1.0) -> Path:
        text = command
        if command.startswith("/"):
            # Opening with "/" is more reliable for commands than T on some
            # localized/keymap setups.  Minecraft pre-fills the slash, so paste
            # the command body only.
            self.windows_gui("press", "--title", window_title, "--key", "/")
            text = command[1:]
        else:
            self.windows_gui("press", "--title", window_title, "--key", "t")
        # Give Minecraft one client tick+ for the chat/command edit box to appear
        # before pasting.  Without this, fast automation may paste into gameplay
        # and trigger keybinds instead of commands.
        time.sleep(0.35)
        self.windows_gui("send-text", "--title", window_title, "--text", text)
        time.sleep(wait_seconds)
        out = self.work_dir / screenshot_name
        self.windows_gui("screenshot", "--title", window_title, "--out", self.wsl_to_windows(out))
        return out

    @staticmethod
    def wsl_to_windows(path: Path) -> str:
        text = str(path.resolve())
        if text.startswith("/mnt/") and len(text) > 6:
            drive = text[5].upper()
            rest = text[7:].replace("/", "\\")
            return f"{drive}:\\{rest}"
        return text
