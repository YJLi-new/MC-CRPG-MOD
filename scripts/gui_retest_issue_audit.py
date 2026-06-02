#!/usr/bin/env python3
"""Regression audit for the 2026-06-01 GUI retest issues.

Checks the exact failure modes reported from the Windows client:
1) player-facing commands must exist and not be hidden behind OP-only dialogue tooling;
2) role NPC bindings must resolve to distinct dialogues and cover legacy tags/names used by
   saves and GUI automation;
3) all P8 block groups must be packaged, and the refreshed test profile must carry the same jar.

When --save-path is supplied, it also inspects the external Minecraft save for the authored
P8 block objects and verifies any Ebb NPCs currently present use tags/names supported by the
source bindings. It no longer requires every role NPC to exist in the mutable user save; the
source/JUnit/GameTest checks cover all four role bindings. The save audit uses nbtlib if
available and is intentionally not required for CI.
"""

from __future__ import annotations

import argparse
import gzip
import hashlib
import io
import json
import sys
import zipfile
import zlib
from pathlib import Path
from typing import Iterable

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_PROFILE_JAR = Path("/mnt/e/MC/PCL/.minecraft/versions/26.1.2-Fabric-Ebb-Test/mods/ebb-0.1.0-dev.jar")
DEFAULT_SAVE = Path("/mnt/e/MC/PCL/.minecraft/versions/26.1.2-Fabric-Ebb-Test/saves/新的世界 (1)")
EXPECTED_BLOCK_GROUPS = {
    "locked_door.json",
    "counter_ledger.json",
    "notice_board.json",
    "washroom_mirror.json",
    "windowsill_ash.json",
    "tenant_luggage.json",
    "cellar_hatch.json",
    "back_door.json",
}
ROLE_EXPECTATIONS = {
    "innkeeper": ("ebb:demo/innkeeper_intro", {"ebb.npc.demo.innkeeper", "ebb.npc.demo.innkeeper_day", "ebb.npc.innkeeper", "ebb.npc.innkeeper_day"}),
    "witness": ("ebb:demo/witness_intro", {"ebb.npc.demo.witness", "ebb.npc.demo.witness_day", "ebb.npc.witness", "ebb.npc.witness_day"}),
    "tenant": ("ebb:demo/tenant_intro", {"ebb.npc.demo.tenant", "ebb.npc.demo.tenant_day", "ebb.npc.tenant", "ebb.npc.tenant_day"}),
    "guard": ("ebb:demo/guard_intro", {"ebb.npc.demo.guard", "ebb.npc.demo.guard_day", "ebb.npc.guard", "ebb.npc.guard_day"}),
}
SAVE_BLOCKS = {
    (0, 64, 4): "minecraft:oak_planks",
    (0, 65, 4): "minecraft:oak_planks",
    (2, 64, 1): "minecraft:lectern",
    (1, 65, 0): "minecraft:oak_sign",
    (-2, 64, 6): "minecraft:glass",
    (4, 65, 6): "minecraft:gray_wool",
    (8, 64, 3): "minecraft:chest",
    (6, 63, 7): "minecraft:acacia_fence",
    (10, 64, 5): "minecraft:oak_planks",
    (10, 65, 5): "minecraft:oak_planks",
}
LEGACY_NPC_TAGS = {"ebb.npc.innkeeper_day", "ebb.npc.witness_day", "ebb.npc.tenant_day", "ebb.npc.guard_day"}
ROLE_CUSTOM_NAMES = {f"Ebb NPC: {role}_day": role for role in ROLE_EXPECTATIONS}
SUPPORTED_ROLE_TAGS = {tag for _, tags in ROLE_EXPECTATIONS.values() for tag in tags}


def fail(message: str) -> None:
    raise AssertionError(message)


def read_text(relative: str) -> str:
    return (ROOT / relative).read_text(encoding="utf-8")


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def audit_sources() -> None:
    commands = read_text("src/main/java/com/crpg/ebb/registry/ModCommands.java")
    required = [
        'Commands.literal("journal")',
        'Commands.literal("quest")',
        'Commands.literal("dialogue")',
        'Commands.literal("vars")',
        "sendDialogueVars(context.getSource(), context.getSource().getPlayerOrException())",
        "private static Identifier parseRoutineIdentifier(String raw)",
        'EbbMod.id("demo/" + raw)',
    ]
    missing = [needle for needle in required if needle not in commands]
    if missing:
        fail(f"ModCommands missing command/routine evidence: {missing}")
    dialogue_line = next(i for i, line in enumerate(commands.splitlines()) if 'Commands.literal("dialogue")' in line)
    following = [line.strip() for line in commands.splitlines()[dialogue_line + 1:dialogue_line + 4] if line.strip()]
    if following and following[0].startswith(".requires(PermissionPredicates.require"):
        fail("/ebb dialogue root is still OP-gated; /ebb dialogue vars must be player-accessible")

    client_net = read_text("src/client/java/com/crpg/ebb/client/network/ClientInteractionNetworking.java")
    if "ClientPlayConnectionEvents.INIT.register" not in client_net:
        fail("Client synced interaction data is not cleared at INIT")
    if "ClientPlayConnectionEvents.JOIN.register" in client_net:
        fail("Client synced interaction data is still cleared at JOIN, which can clear login sync payloads")
    for needle in [
        "ClientBlockGroupIndex.rebuild(payload.definitions())",
        "EntityBindingRegistry.syncFromServer(payload.definitions(), payload.settings())",
        "ClientEntityTargetIndex.rebuild(payload.targets())",
    ]:
        if needle not in client_net:
            fail(f"Client sync receiver missing: {needle}")

    binding_dir = ROOT / "src/main/resources/data/ebb/interactions/entity_bindings/demo"
    for role, (dialogue, tags) in ROLE_EXPECTATIONS.items():
        data = json.loads((binding_dir / f"{role}_ebb_npc.json").read_text(encoding="utf-8"))
        if data.get("dialogue") != dialogue:
            fail(f"{role} binding dialogue {data.get('dialogue')} != {dialogue}")
        actual_tags = set(data.get("match", {}).get("tags", []))
        if not tags.issubset(actual_tags):
            fail(f"{role} binding missing legacy/demo tags: {sorted(tags - actual_tags)}")
        name_data = json.loads((binding_dir / f"{role}_ebb_npc_name.json").read_text(encoding="utf-8"))
        if name_data.get("dialogue") != dialogue:
            fail(f"{role} name binding dialogue {name_data.get('dialogue')} != {dialogue}")
        expected_name = f"Ebb NPC: {role}_day"
        if name_data.get("match", {}).get("name") != expected_name:
            fail(f"{role} name binding should match custom name {expected_name!r}")

    block_dir = ROOT / "src/main/resources/data/ebb/interactions/block_groups/demo"
    actual_groups = {path.name for path in block_dir.glob("*.json")}
    if not EXPECTED_BLOCK_GROUPS.issubset(actual_groups):
        fail(f"missing source block groups: {sorted(EXPECTED_BLOCK_GROUPS - actual_groups)}")


def audit_jar(jar_path: Path, label: str) -> None:
    if not jar_path.exists():
        fail(f"{label} jar missing: {jar_path}")
    with zipfile.ZipFile(jar_path) as zf:
        names = set(zf.namelist())
        block_groups = {Path(name).name for name in names if name.startswith("data/ebb/interactions/block_groups/demo/") and name.endswith(".json")}
        if not EXPECTED_BLOCK_GROUPS.issubset(block_groups):
            fail(f"{label} jar missing block groups: {sorted(EXPECTED_BLOCK_GROUPS - block_groups)}")
        for role, (dialogue, tags) in ROLE_EXPECTATIONS.items():
            name = f"data/ebb/interactions/entity_bindings/demo/{role}_ebb_npc.json"
            if name not in names:
                fail(f"{label} jar missing {name}")
            data = json.loads(zf.read(name).decode("utf-8"))
            if data.get("dialogue") != dialogue:
                fail(f"{label} jar {role} dialogue {data.get('dialogue')} != {dialogue}")
            actual_tags = set(data.get("match", {}).get("tags", []))
            if not tags.issubset(actual_tags):
                fail(f"{label} jar {role} missing tags: {sorted(tags - actual_tags)}")
            name_binding = f"data/ebb/interactions/entity_bindings/demo/{role}_ebb_npc_name.json"
            if name_binding not in names:
                fail(f"{label} jar missing {name_binding}")
            name_data = json.loads(zf.read(name_binding).decode("utf-8"))
            if name_data.get("dialogue") != dialogue:
                fail(f"{label} jar {role} name dialogue {name_data.get('dialogue')} != {dialogue}")
            expected_name = f"Ebb NPC: {role}_day"
            if name_data.get("match", {}).get("name") != expected_name:
                fail(f"{label} jar {role} name binding should match {expected_name!r}")


def region_chunks(region_path: Path):
    import nbtlib  # type: ignore
    data = region_path.read_bytes()
    if len(data) < 4096:
        return
    for idx in range(1024):
        sector = int.from_bytes(data[idx * 4:idx * 4 + 3], "big")
        count = data[idx * 4 + 3]
        if not sector or not count:
            continue
        pos = sector * 4096
        if pos + 5 > len(data):
            continue
        length = int.from_bytes(data[pos:pos + 4], "big")
        comp = data[pos + 4]
        payload = data[pos + 5:pos + 4 + length]
        try:
            raw = gzip.decompress(payload) if comp == 1 else zlib.decompress(payload) if comp == 2 else payload
            yield nbtlib.File.parse(io.BytesIO(raw))
        except Exception:
            continue


def palette_index(section, x: int, y: int, z: int) -> int:
    bs = section.get("block_states")
    if not bs:
        return 0
    palette = bs.get("palette")
    if not palette:
        return 0
    if len(palette) == 1 or "data" not in bs:
        return 0
    bits = max(4, (len(palette) - 1).bit_length())
    idx = ((y & 15) * 16 + (z & 15)) * 16 + (x & 15)
    bit_index = idx * bits
    long_index = bit_index // 64
    start = bit_index % 64
    data = bs["data"]
    value = int(data[long_index]) & ((1 << 64) - 1)
    if start + bits <= 64:
        return (value >> start) & ((1 << bits) - 1)
    value2 = int(data[long_index + 1]) & ((1 << 64) - 1)
    return ((value >> start) | (value2 << (64 - start))) & ((1 << bits) - 1)


def block_name(root, x: int, y: int, z: int) -> str:
    sec_y = y // 16 if y >= 0 else -((-y + 15) // 16)
    for section in root.get("sections", []):
        if int(section.get("Y")) == sec_y:
            bs = section.get("block_states")
            if not bs or not bs.get("palette"):
                return "minecraft:air"
            palette = bs["palette"]
            idx = palette_index(section, x, y, z)
            if idx < 0 or idx >= len(palette):
                return f"BADIDX:{idx}/{len(palette)}"
            return str(palette[idx]["Name"])
    return "minecraft:air"


def audit_save(save_path: Path) -> None:
    try:
        import nbtlib  # noqa: F401  # type: ignore
    except Exception as exc:
        fail(f"save audit requires nbtlib: {exc}")
    overworld = save_path / "dimensions/minecraft/overworld"
    if not overworld.exists():
        fail(f"save overworld path missing: {overworld}")

    chunks: dict[tuple[int, int], object] = {}
    for region_path in (overworld / "region").glob("*.mca"):
        for root in region_chunks(region_path) or []:
            chunks[(int(root["xPos"]), int(root["zPos"]))] = root
    if not chunks:
        fail("save audit found no region chunks")
    for pos, expected in SAVE_BLOCKS.items():
        x, y, z = pos
        root = chunks.get((x >> 4, z >> 4))
        if root is None:
            fail(f"save missing chunk for block {pos}")
        actual = block_name(root, x, y, z)
        if actual != expected:
            fail(f"save block {pos}: {actual} != {expected}")

    found_role_markers: set[str] = set()
    for region_path in (overworld / "entities").glob("*.mca"):
        for root in region_chunks(region_path) or []:
            for entity in root.get("Entities", root.get("entities", [])):
                tags = {str(tag) for tag in entity.get("Tags", [])}
                custom_name = str(entity.get("CustomName", ""))
                entity_id = str(entity.get("id", ""))
                if entity_id != "ebb:npc" and "ebb.npc" not in tags and "Ebb NPC:" not in custom_name:
                    continue
                unsupported_tags = sorted(tag for tag in tags if tag.startswith("ebb.npc.") and tag not in SUPPORTED_ROLE_TAGS and tag != "ebb.npc")
                if unsupported_tags:
                    fail(f"save has Ebb NPC tags not covered by source bindings: {unsupported_tags}")
                if custom_name.startswith("Ebb NPC:") and custom_name not in ROLE_CUSTOM_NAMES:
                    fail(f"save has Ebb NPC custom name not covered by name bindings: {custom_name}")
                found_role_markers |= tags & SUPPORTED_ROLE_TAGS
                if custom_name in ROLE_CUSTOM_NAMES:
                    found_role_markers.add(custom_name)
    if not found_role_markers:
        fail("save audit found no Ebb NPC role tags or role custom names to validate")


def main(argv: Iterable[str]) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--profile-jar", type=Path, default=DEFAULT_PROFILE_JAR)
    parser.add_argument("--skip-profile", action="store_true")
    parser.add_argument("--save-path", type=Path, default=None)
    parser.add_argument("--require-save", action="store_true")
    args = parser.parse_args(list(argv))

    audit_sources()
    build_jar = ROOT / "build/libs/ebb-0.1.0-dev.jar"
    audit_jar(build_jar, "build")
    profile_status = "skipped"
    if not args.skip_profile:
        audit_jar(args.profile_jar, "profile")
        if sha256(build_jar) != sha256(args.profile_jar):
            fail("profile jar hash does not match build jar hash")
        profile_status = sha256(args.profile_jar)

    save_status = "skipped"
    save_path = args.save_path or (DEFAULT_SAVE if DEFAULT_SAVE.exists() else None)
    if save_path is not None:
        audit_save(save_path)
        save_status = str(save_path)
    elif args.require_save:
        fail("--require-save was set but no save path exists/supplied")

    print(
        "GuiRetestIssueAudit passed: commands, INIT sync clear, role bindings, "
        f"8 block groups, build jar, profile={profile_status}, save={save_status}"
    )
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main(sys.argv[1:]))
    except AssertionError as exc:
        print(f"GuiRetestIssueAudit failed: {exc}", file=sys.stderr)
        raise SystemExit(1)
