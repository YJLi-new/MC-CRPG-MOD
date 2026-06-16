#!/usr/bin/env python3
"""Check whether the PCL Fabric test profile's latest runtime log loaded the refreshed Ebb data.

This is intentionally separate from smoke checks: before the user relaunches Minecraft, the
installed profile jar can be correct while `logs/latest.log` still belongs to an old running
client session. In that case this script exits non-zero and prints the last observed registry
counts so the stale-window condition is explicit.
"""

from __future__ import annotations

import argparse
import hashlib
import re
import sys
from dataclasses import dataclass
from pathlib import Path


DEFAULT_PROFILE = Path("/mnt/e/MC/PCL/.minecraft/versions/26.1.2-Fabric-Ebb-Test")
DEFAULT_BUILD_JAR = Path("build/libs/ebb-0.1.0-dev.jar")


@dataclass(frozen=True)
class RuntimeCounts:
    dialogues: int | None
    block_groups: int | None
    entity_bindings: int | None
    npc_routines: int | None

    def summary(self) -> str:
        return (
            f"dialogues={self.dialogues}, block_groups={self.block_groups}, "
            f"entity_bindings={self.entity_bindings}, npc_routines={self.npc_routines}"
        )


PATTERNS = {
    "dialogues": re.compile(r"Loaded (\d+) dialogue JSON definition\(s\)"),
    "block_groups": re.compile(r"Loaded (\d+) block group JSON definition\(s\)"),
    "entity_bindings": re.compile(r"Loaded (\d+) entity binding JSON definition\(s\)"),
    "npc_routines": re.compile(r"Loaded (\d+) npc routine JSON definition\(s\)"),
}

FATAL_LOG_PATTERNS = {
    "bad_zip": re.compile(r"ZipFile invalid LOC header", re.IGNORECASE),
    "class_load_failure": re.compile(r"Failed to load class file", re.IGNORECASE),
    "server_task_error": re.compile(r"\[Server thread/ERROR\]: Error executing task on Server"),
}


def fatal_log_hits(text: str) -> list[str]:
    return [name for name, pattern in FATAL_LOG_PATTERNS.items() if pattern.search(text)]


def sha256(path: Path) -> str:
    hasher = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            hasher.update(chunk)
    return hasher.hexdigest()


def last_int(pattern: re.Pattern[str], text: str) -> int | None:
    matches = pattern.findall(text)
    return int(matches[-1]) if matches else None


def parse_counts(log_path: Path) -> RuntimeCounts:
    text = log_path.read_bytes().decode("utf-8", "replace")
    return RuntimeCounts(
        dialogues=last_int(PATTERNS["dialogues"], text),
        block_groups=last_int(PATTERNS["block_groups"], text),
        entity_bindings=last_int(PATTERNS["entity_bindings"], text),
        npc_routines=last_int(PATTERNS["npc_routines"], text),
    )


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--profile", type=Path, default=DEFAULT_PROFILE,
                        help="PCL test profile directory")
    parser.add_argument("--build-jar", type=Path, default=DEFAULT_BUILD_JAR,
                        help="Current project build jar to compare with the profile-local installed jar")
    parser.add_argument("--expected-dialogues", type=int, default=20)
    parser.add_argument("--expected-block-groups", type=int, default=12)
    parser.add_argument("--expected-entity-bindings", type=int, default=15)
    parser.add_argument("--expected-npc-routines", type=int, default=7)
    args = parser.parse_args()

    profile = args.profile
    installed_jar = profile / "mods" / "ebb-0.1.0-dev.jar"
    latest_log = profile / "logs" / "latest.log"

    if not profile.exists():
        print(f"RuntimeLoadCheck failed: profile directory does not exist: {profile}", file=sys.stderr)
        return 2
    if not args.build_jar.exists():
        print(f"RuntimeLoadCheck failed: build jar does not exist: {args.build_jar}", file=sys.stderr)
        return 2
    if not installed_jar.exists():
        print(f"RuntimeLoadCheck failed: installed profile jar does not exist: {installed_jar}", file=sys.stderr)
        return 2

    build_hash = sha256(args.build_jar)
    installed_hash = sha256(installed_jar)
    if build_hash != installed_hash:
        print(
            "RuntimeLoadCheck failed: profile jar is stale relative to build jar\n"
            f"  build={build_hash} {args.build_jar}\n"
            f"  profile={installed_hash} {installed_jar}",
            file=sys.stderr,
        )
        return 1

    if not latest_log.exists():
        print(
            "RuntimeLoadCheck inconclusive: latest.log does not exist yet. "
            "Launch the PCL profile once, then re-run this script.",
            file=sys.stderr,
        )
        return 2

    log_text = latest_log.read_bytes().decode("utf-8", "replace")
    fatal_hits = fatal_log_hits(log_text)
    if fatal_hits:
        print("RuntimeLoadCheck failed: latest.log contains fatal Ebb/Minecraft runtime errors:", file=sys.stderr)
        print(f"  profile={profile}", file=sys.stderr)
        print(f"  latest_log={latest_log}", file=sys.stderr)
        print(f"  hits={', '.join(fatal_hits)}", file=sys.stderr)
        print(
            "Action: fully close Minecraft, rebuild/refresh the profile only while closed, "
            "relaunch 26.1.2-Fabric-Ebb-Test, enter 新的世界 (1), then re-run this check.",
            file=sys.stderr,
        )
        return 1

    counts = parse_counts(latest_log)
    expected = RuntimeCounts(
        dialogues=args.expected_dialogues,
        block_groups=args.expected_block_groups,
        entity_bindings=args.expected_entity_bindings,
        npc_routines=args.expected_npc_routines,
    )
    log_mtime = latest_log.stat().st_mtime
    jar_mtime = installed_jar.stat().st_mtime

    problems: list[str] = []
    for field in ("dialogues", "block_groups", "entity_bindings", "npc_routines"):
        actual = getattr(counts, field)
        required = getattr(expected, field)
        if actual is None:
            problems.append(f"{field}: no registry-load line found")
        elif actual < required:
            problems.append(f"{field}: last log count {actual} < expected {required}")
    if log_mtime < jar_mtime:
        problems.append("latest.log is older than the installed Ebb jar; Minecraft has not been relaunched since refresh")

    if problems:
        print("RuntimeLoadCheck stale/incomplete:")
        print(f"  profile={profile}")
        print(f"  jar_hash={installed_hash}")
        print(f"  latest_log={latest_log}")
        print(f"  observed={counts.summary()}")
        print(f"  expected>={expected.summary()}")
        for problem in problems:
            print(f"  - {problem}")
        print("Action: fully close Minecraft, relaunch 26.1.2-Fabric-Ebb-Test, enter 新的世界 (1), then run this again.")
        return 1

    print("RuntimeLoadCheck passed:")
    print(f"  profile={profile}")
    print(f"  jar_hash={installed_hash}")
    print(f"  latest_log={latest_log}")
    print(f"  observed={counts.summary()}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
