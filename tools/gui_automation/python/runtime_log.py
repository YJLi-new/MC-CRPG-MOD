from __future__ import annotations

import re
from dataclasses import dataclass
from pathlib import Path

PATTERNS = {
    "dialogues": re.compile(r"Loaded (\d+) dialogue JSON definition\(s\)"),
    "block_groups": re.compile(r"Loaded (\d+) block group JSON definition\(s\)"),
    "entity_bindings": re.compile(r"Loaded (\d+) entity binding JSON definition\(s\)"),
    "npc_routines": re.compile(r"Loaded (\d+) npc routine JSON definition\(s\)"),
}

@dataclass(frozen=True)
class RuntimeCounts:
    dialogues: int | None = None
    block_groups: int | None = None
    entity_bindings: int | None = None
    npc_routines: int | None = None

    def as_dict(self) -> dict[str, int | None]:
        return {
            "dialogues": self.dialogues,
            "block_groups": self.block_groups,
            "entity_bindings": self.entity_bindings,
            "npc_routines": self.npc_routines,
        }

    def meets(self, expected: "RuntimeCounts") -> bool:
        for key, required in expected.as_dict().items():
            actual = self.as_dict()[key]
            if actual is None or required is None or actual < required:
                return False
        return True


def _last_int(pattern: re.Pattern[str], text: str) -> int | None:
    matches = pattern.findall(text)
    return int(matches[-1]) if matches else None


def parse_runtime_counts(log_path: Path) -> RuntimeCounts:
    text = log_path.read_bytes().decode("utf-8", "replace")
    return RuntimeCounts(
        dialogues=_last_int(PATTERNS["dialogues"], text),
        block_groups=_last_int(PATTERNS["block_groups"], text),
        entity_bindings=_last_int(PATTERNS["entity_bindings"], text),
        npc_routines=_last_int(PATTERNS["npc_routines"], text),
    )
