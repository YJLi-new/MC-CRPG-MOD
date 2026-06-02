from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

try:
    from PIL import Image
except Exception:  # pragma: no cover - dependency is installed on Windows for real GUI runs
    Image = None

@dataclass(frozen=True)
class ImageSignal:
    name: str
    pixels: int
    ratio: float
    passed: bool


def _load(path: Path):
    if Image is None:
        raise RuntimeError("Pillow is required for image assertions")
    return Image.open(path).convert("RGB")


def count_pixels_near(path: Path, target: tuple[int, int, int], tolerance: int = 45) -> ImageSignal:
    img = _load(path)
    width, height = img.size
    total = width * height
    tr, tg, tb = target
    count = 0
    for r, g, b in img.getdata():
        if abs(r - tr) <= tolerance and abs(g - tg) <= tolerance and abs(b - tb) <= tolerance:
            count += 1
    return ImageSignal(name=f"near_rgb_{target}", pixels=count, ratio=count / total if total else 0.0, passed=count > 0)


def has_cyan_highlight_or_prompt(path: Path) -> ImageSignal:
    signal = count_pixels_near(path, (0, 255, 255), 70)
    return ImageSignal("cyan_highlight_or_prompt", signal.pixels, signal.ratio, signal.ratio > 0.00005)


def has_red_command_error(path: Path) -> ImageSignal:
    signal = count_pixels_near(path, (255, 80, 80), 70)
    return ImageSignal("red_command_error", signal.pixels, signal.ratio, signal.ratio > 0.00005)


def summarize_signals(path: Path) -> dict[str, object]:
    signals: Iterable[ImageSignal] = [has_cyan_highlight_or_prompt(path), has_red_command_error(path)]
    return {signal.name: {"pixels": signal.pixels, "ratio": signal.ratio, "passed": signal.passed} for signal in signals}
