from __future__ import annotations

import os
import shutil
import subprocess
import time
from dataclasses import dataclass, field
from pathlib import Path

@dataclass
class ServerController:
    project_root: Path
    work_dir: Path
    source_world: Path | None = None
    process: subprocess.Popen | None = field(default=None, init=False)

    @property
    def run_dir(self) -> Path:
        return self.project_root / "run"

    def prepare_world_copy(self) -> Path:
        if self.source_world is None:
            raise ValueError("source_world is required to prepare a world copy")
        target = self.run_dir / "world"
        if target.exists():
            backup = self.work_dir / f"previous_run_world_{int(time.time())}"
            backup.parent.mkdir(parents=True, exist_ok=True)
            shutil.move(str(target), str(backup))
        shutil.copytree(self.source_world, target)
        return target

    def write_server_properties(self, port: int = 25565, offline: bool = True):
        self.run_dir.mkdir(parents=True, exist_ok=True)
        props = self.run_dir / "server.properties"
        existing: dict[str, str] = {}
        if props.exists():
            for line in props.read_text(encoding="utf-8", errors="ignore").splitlines():
                if "=" in line and not line.startswith("#"):
                    k, v = line.split("=", 1)
                    existing[k] = v
        existing.update({
            "server-port": str(port),
            "online-mode": "false" if offline else "true",
            "enforce-secure-profile": "false" if offline else existing.get("enforce-secure-profile", "true"),
            "gamemode": "creative",
            "allow-flight": "true",
            "enable-command-block": "true",
            "level-name": "world",
            "motd": "Ebb GUI automation server",
        })
        props.write_text("\n".join(f"{k}={v}" for k, v in sorted(existing.items())) + "\n", encoding="utf-8")

    def ensure_eula(self, accept: bool = False):
        eula = self.run_dir / "eula.txt"
        if accept or os.environ.get("EBB_ACCEPT_MINECRAFT_EULA", "").lower() in {"1", "true", "yes"}:
            eula.write_text("eula=true\n", encoding="utf-8")
            return
        if not eula.exists() or "eula=true" not in eula.read_text(encoding="utf-8", errors="ignore"):
            raise RuntimeError("Minecraft EULA is not accepted. Re-run with --accept-minecraft-eula or set EBB_ACCEPT_MINECRAFT_EULA=true after you accept https://aka.ms/MinecraftEULA")

    def start(self, port: int = 25565, accept_eula: bool = False) -> subprocess.Popen:
        self.write_server_properties(port=port, offline=True)
        self.ensure_eula(accept=accept_eula)
        cmd = [str(self.project_root / "scripts" / "gradle-local.sh"), "--no-daemon", "runServer", "--args", "nogui"]
        log_path = self.work_dir / "server-process.log"
        self.work_dir.mkdir(parents=True, exist_ok=True)
        log = log_path.open("ab")
        self.process = subprocess.Popen(cmd, cwd=self.project_root, stdin=subprocess.PIPE, stdout=log, stderr=subprocess.STDOUT, text=True)
        return self.process

    def send_console(self, command: str):
        if self.process is None or self.process.stdin is None:
            raise RuntimeError("server is not running")
        self.process.stdin.write(command + "\n")
        self.process.stdin.flush()

    def stop(self):
        if self.process is None:
            return
        try:
            self.send_console("stop")
            self.process.wait(timeout=30)
        except Exception:
            self.process.terminate()
        finally:
            self.process = None
