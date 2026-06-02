#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import os
import shutil
import subprocess
import sys
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
NODE_DIR = PROJECT_ROOT / "tools" / "gui_automation" / "node"
WINDOWS_REQUIREMENTS = [
    "pyautogui>=0.9.54",
    "pillow>=10.0.0",
    "mss>=9.0.0",
    "numpy>=1.26.0",
    "opencv-python>=4.9.0.80",
    "pywin32>=306",
    "psutil>=5.9.0",
]


def run(cmd: list[str], *, cwd: Path | None = None, check: bool = True) -> subprocess.CompletedProcess:
    print("+", " ".join(cmd))
    return subprocess.run(cmd, cwd=cwd, text=True, check=check)


def install_node_deps() -> None:
    if shutil.which("npm") is None:
        raise SystemExit("npm is required for mineflayer automation")
    run(["npm", "install"], cwd=NODE_DIR)
    run(["npm", "run", "self-test"], cwd=NODE_DIR)


def powershell_available() -> bool:
    return shutil.which("powershell.exe") is not None


def install_windows_python_deps(skip: bool = False) -> None:
    if skip:
        print("Skipping Windows Python dependency install by request.")
        return
    if not powershell_available():
        print("powershell.exe is not visible from this shell; skipping Windows Python dependency install.", file=sys.stderr)
        return
    requirement_literal = " ".join(json.dumps(req) for req in WINDOWS_REQUIREMENTS)
    command = (
        "$ErrorActionPreference='Stop'; "
        "py -3.12 -m pip install --user --upgrade pip; "
        f"py -3.12 -m pip install --user {requirement_literal}"
    )
    run(["powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", command])


def main() -> int:
    parser = argparse.ArgumentParser(description="Install Ebb GUI automation dependencies.")
    parser.add_argument("--skip-node", action="store_true")
    parser.add_argument("--skip-windows-python", action="store_true")
    args = parser.parse_args()
    if not args.skip_node:
        install_node_deps()
    install_windows_python_deps(args.skip_windows_python)
    print("GUI automation dependency installation complete.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
