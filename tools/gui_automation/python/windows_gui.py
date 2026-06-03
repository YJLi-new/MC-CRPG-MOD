from __future__ import annotations

import argparse
import json
import os
import re
import subprocess
import sys
import time
from pathlib import Path


def _require_windows():
    if os.name != "nt":
        raise RuntimeError("windows_gui.py must run under Windows Python, not WSL Python")


def _import_gui_deps():
    _require_windows()
    import pyautogui  # type: ignore
    import win32con  # type: ignore
    import win32gui  # type: ignore
    import win32process  # type: ignore
    return pyautogui, win32con, win32gui, win32process


def find_windows(title_regex: str) -> list[dict[str, object]]:
    pyautogui, win32con, win32gui, win32process = _import_gui_deps()
    pattern = re.compile(title_regex, re.I)
    found: list[dict[str, object]] = []

    def enum_cb(hwnd, _):
        if not win32gui.IsWindowVisible(hwnd):
            return
        title = win32gui.GetWindowText(hwnd)
        if title and pattern.search(title):
            rect = win32gui.GetWindowRect(hwnd)
            _, pid = win32process.GetWindowThreadProcessId(hwnd)
            found.append({"hwnd": hwnd, "title": title, "rect": rect, "pid": pid})

    win32gui.EnumWindows(enum_cb, None)
    return found


def focus_window(title_regex: str) -> dict[str, object]:
    pyautogui, win32con, win32gui, win32process = _import_gui_deps()
    windows = find_windows(title_regex)
    if not windows:
        raise RuntimeError(f"no window matched {title_regex!r}")
    hwnd = int(windows[0]["hwnd"])
    win32gui.ShowWindow(hwnd, win32con.SW_RESTORE)
    try:
        win32gui.SetForegroundWindow(hwnd)
    except Exception:
        # Windows foreground-lock rules can reject SetForegroundWindow even for
        # a visible launcher window.  A harmless center click is sufficient for
        # GUI testing and avoids falling back to shell-specific tricks.
        left, top, right, bottom = windows[0]["rect"]
        win32gui.SetWindowPos(hwnd, win32con.HWND_TOPMOST, left, top, right - left, bottom - top, 0)
        win32gui.SetWindowPos(hwnd, win32con.HWND_NOTOPMOST, left, top, right - left, bottom - top, 0)
        pyautogui.click(left + max(1, (right - left) // 2), top + max(1, (bottom - top) // 2))
        try:
            win32gui.SetForegroundWindow(hwnd)
        except Exception:
            pass
    time.sleep(0.2)
    return windows[0]


def screenshot(title_regex: str, out: Path) -> dict[str, object]:
    pyautogui, *_ = _import_gui_deps()
    window = focus_window(title_regex)
    left, top, right, bottom = window["rect"]
    img = pyautogui.screenshot(region=(left, top, right - left, bottom - top))
    out.parent.mkdir(parents=True, exist_ok=True)
    img.save(out)
    return {"window": window, "screenshot": str(out)}


def send_text(title_regex: str, text: str, press_enter: bool = True) -> dict[str, object]:
    pyautogui, *_ = _import_gui_deps()
    import win32clipboard  # type: ignore
    window = focus_window(title_regex)
    # Use the Windows clipboard instead of per-character typing.  Minecraft can
    # take a tick to create the chat edit box after T or /; per-character typing
    # is both race-prone and dangerous because letters like "e" can trigger
    # gameplay keybinds if chat was not focused yet.
    win32clipboard.OpenClipboard()
    try:
        win32clipboard.EmptyClipboard()
        win32clipboard.SetClipboardData(win32clipboard.CF_UNICODETEXT, text)
    finally:
        win32clipboard.CloseClipboard()
    pyautogui.hotkey("ctrl", "v")
    if press_enter:
        pyautogui.press("enter")
    return {"window": window, "sent": text, "enter": press_enter}


def press_key(title_regex: str, key: str) -> dict[str, object]:
    pyautogui, *_ = _import_gui_deps()
    window = focus_window(title_regex)
    pyautogui.press(key)
    return {"window": window, "key": key}


def hotkey(title_regex: str, keys: list[str]) -> dict[str, object]:
    pyautogui, *_ = _import_gui_deps()
    if not keys:
        raise ValueError("--keys is required")
    window = focus_window(title_regex)
    pyautogui.hotkey(*keys)
    return {"window": window, "keys": keys}


def click_relative(title_regex: str, x: int, y: int, clicks: int = 1) -> dict[str, object]:
    pyautogui, *_ = _import_gui_deps()
    window = focus_window(title_regex)
    left, top, _right, _bottom = window["rect"]
    pyautogui.click(left + x, top + y, clicks=clicks, interval=0.1)
    return {"window": window, "relative": [x, y], "clicks": clicks}



def close_window(title_regex: str, force: bool = False) -> dict[str, object]:
    pyautogui, win32con, win32gui, win32process = _import_gui_deps()
    windows = find_windows(title_regex)
    closed: list[dict[str, object]] = []
    for window in windows:
        hwnd = int(window["hwnd"])
        _thread_id, pid = win32process.GetWindowThreadProcessId(hwnd)
        win32gui.PostMessage(hwnd, win32con.WM_CLOSE, 0, 0)
        closed.append({"hwnd": hwnd, "pid": pid, "title": window.get("title")})
    time.sleep(1.5)
    if force and closed:
        try:
            import psutil  # type: ignore
            live_hwnd_pids = {int(w["pid"]) for w in find_windows(title_regex)}
            for item in closed:
                pid = int(item["pid"])
                if pid in live_hwnd_pids and psutil.pid_exists(pid):
                    psutil.Process(pid).kill()
                    item["forced"] = True
        except Exception as exc:
            closed.append({"force_error": str(exc)})
    return {"matched": len(windows), "closed": closed}

def launch(exe: Path) -> dict[str, object]:
    _require_windows()
    proc = subprocess.Popen([str(exe)], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    return {"exe": str(exe), "pid": proc.pid}


def main() -> int:
    for stream in (sys.stdout, sys.stderr):
        try:
            stream.reconfigure(encoding="utf-8", errors="replace")
        except Exception:
            pass
    parser = argparse.ArgumentParser()
    parser.add_argument("action", choices=["find", "focus", "screenshot", "send-text", "press", "hotkey", "click", "close", "launch"])
    parser.add_argument("--title", default=r"26\.1\.2-Fabric-Ebb-Test|Minecraft")
    parser.add_argument("--out", type=Path)
    parser.add_argument("--text")
    parser.add_argument("--key")
    parser.add_argument("--keys", nargs="+")
    parser.add_argument("--x", type=int)
    parser.add_argument("--y", type=int)
    parser.add_argument("--clicks", type=int, default=1)
    parser.add_argument("--force", action="store_true")
    parser.add_argument("--exe", type=Path)
    args = parser.parse_args()
    try:
        if args.action == "launch":
            if args.exe is None: raise ValueError("--exe is required")
            result = launch(args.exe)
        elif args.action == "find":
            result = {"windows": find_windows(args.title)}
        elif args.action == "focus":
            result = focus_window(args.title)
        elif args.action == "screenshot":
            if args.out is None: raise ValueError("--out is required")
            result = screenshot(args.title, args.out)
        elif args.action == "send-text":
            if args.text is None: raise ValueError("--text is required")
            result = send_text(args.title, args.text)
        elif args.action == "click":
            if args.x is None or args.y is None: raise ValueError("--x and --y are required")
            result = click_relative(args.title, args.x, args.y, args.clicks)
        elif args.action == "close":
            result = close_window(args.title, force=args.force)
        elif args.action == "hotkey":
            result = hotkey(args.title, args.keys or [])
        else:
            if args.key is None: raise ValueError("--key is required")
            result = press_key(args.title, args.key)
        print(json.dumps({"ok": True, "result": result}, ensure_ascii=False))
        return 0
    except Exception as exc:
        print(json.dumps({"ok": False, "error": str(exc)}, ensure_ascii=False), file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
