#!/usr/bin/env python3
"""Compile author-friendly Ebb YAML/JSON sources into runtime datapack JSON.

The runtime mod still reads Minecraft-style JSON under data/ebb.  This helper lets
writers keep higher-level YAML examples from the research plan while producing the
current registry layout used by the Fabric 26.1.2 MVP.
"""
from __future__ import annotations

import argparse
import json
import shutil
import sys
from pathlib import Path
from typing import Any
from json import JSONDecodeError

try:
    import yaml  # type: ignore
except ImportError as exc:  # pragma: no cover - environment guard
    print("PyYAML is required for authoring YAML. Install with: python3 -m pip install --user PyYAML", file=sys.stderr)
    raise SystemExit(2) from exc

MOD_ID = "ebb"
DEFAULT_SOURCE = Path("authoring")
DEFAULT_OUT = Path("build/generated/ebb_authoring/data/ebb")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, default=DEFAULT_SOURCE, help="authoring source root")
    parser.add_argument("--out", type=Path, default=DEFAULT_OUT, help="output data/ebb root")
    parser.add_argument("--clean", action="store_true", help="delete output root before writing")
    parser.add_argument("--apply", action="store_true", help="write into src/main/resources/data/ebb instead of --out")
    return parser.parse_args()


def read_doc(path: Path) -> Any:
    text = path.read_text(encoding="utf-8")
    if path.suffix.lower() in {".yaml", ".yml"}:
        data = yaml.safe_load(text)
    else:
        data = json.loads(text)
    return data or {}


def format_read_error(path: Path, exc: BaseException) -> str:
    if isinstance(exc, JSONDecodeError):
        return f"{path}:{exc.lineno}:{exc.colno}: invalid JSON: {exc.msg}"
    mark = getattr(exc, "problem_mark", None)
    problem = getattr(exc, "problem", None) or str(exc)
    if mark is not None:
        return f"{path}:{mark.line + 1}:{mark.column + 1}: invalid YAML: {problem}"
    return f"{path}: could not parse authoring document: {exc}"


def read_doc_or_error(path: Path, errors: list[str]) -> Any | None:
    try:
        data = read_doc(path)
    except Exception as exc:
        errors.append(format_read_error(path, exc))
        return None
    if not isinstance(data, dict):
        errors.append(f"{path}:1:1: authoring document must be a mapping/object")
        return None
    return data


def normalize_identifier(value: str, *, default_namespace: str = MOD_ID) -> str:
    if not value or not str(value).strip():
        raise ValueError("identifier must not be blank")
    value = str(value).strip()
    return value if ":" in value else f"{default_namespace}:{value}"


def identifier_path(identifier: str) -> Path:
    namespace, path = identifier.split(":", 1)
    if namespace != MOD_ID:
        # Keep non-ebb namespaces visible in generated paths instead of silently
        # overwriting local resources.
        return Path(namespace) / f"{path}.json"
    return Path(f"{path}.json")


def write_json(root: Path, category: str, identifier: str, data: dict[str, Any]) -> Path:
    path = root / category / identifier_path(identifier)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return path


def convert_effect(effect: dict[str, Any]) -> dict[str, Any]:
    result = dict(effect)
    # Preserve the report's shortcut fields but normalize the most common `key`
    # shape so current DialogueEffect can consume it directly.
    if "setFlag" in result and "type" not in result:
        result["type"] = "set_flag"
    if "clearFlag" in result and "type" not in result:
        result["type"] = "clear_flag"
    if "setVar" in result and "type" not in result:
        result["type"] = "set_variable"
    if "setStoryVar" in result and "type" not in result:
        result["type"] = "set_story_var"
    if "clearStoryVar" in result and "type" not in result:
        result["type"] = "clear_story_var"
    if "addStoryInt" in result and "type" not in result:
        result["type"] = "add_story_int"
    if "addTrait" in result and "type" not in result:
        result["type"] = "add_trait"
    if "unlock" in result and "type" not in result:
        result["type"] = "unlock_retry"
    return result


def convert_check(raw: dict[str, Any]) -> dict[str, Any]:
    check = dict(raw)
    if "ability" in check and "attribute" not in check:
        check["attribute"] = check["ability"]
    for key in ("success_effects", "failure_effects", "critical_success_effects", "critical_failure_effects"):
        if key in check:
            check[key] = [convert_effect(effect) for effect in check[key]]
    return check


def convert_choice(raw: dict[str, Any]) -> dict[str, Any]:
    choice: dict[str, Any] = {}
    for key, value in raw.items():
        if key == "kind":
            choice["type"] = value
        elif key == "label":
            choice["text"] = value
        elif key == "roll":
            choice["check"] = convert_check(value)
        elif key == "check":
            choice["check"] = convert_check(value)
        elif key == "effects":
            choice[key] = [convert_effect(effect) for effect in value]
        else:
            choice[key] = value
    return choice


def convert_node(raw: dict[str, Any]) -> dict[str, Any]:
    node: dict[str, Any] = {}
    for key, value in raw.items():
        if key == "choices":
            node[key] = [convert_choice(choice) for choice in value]
        elif key in {"effects", "enter_effects"}:
            node[key] = [convert_effect(effect) for effect in value]
        else:
            node[key] = value
    return node


def validate_dialogue(identifier: str, data: dict[str, Any], source: Path) -> list[str]:
    errors: list[str] = []
    nodes = data.get("nodes", {})
    if not isinstance(nodes, dict) or not nodes:
        return [f"{source}: dialogue {identifier} has no nodes"]
    start = data.get("start")
    if start not in nodes:
        errors.append(f"{source}: start node {start!r} is missing")
    for node_id, node in nodes.items():
        if not isinstance(node, dict):
            errors.append(f"{source}: node {node_id} must be an object")
            continue
        next_node = node.get("next")
        if next_node and next_node not in nodes:
            errors.append(f"{source}: node {node_id} next={next_node!r} is missing")
        for choice in node.get("choices", []) or []:
            choice_id = choice.get("id", "<missing>")
            choice_next = choice.get("next")
            if choice_next and choice_next not in nodes:
                errors.append(f"{source}: node {node_id} choice {choice_id} next={choice_next!r} is missing")
            check = choice.get("check")
            if check:
                if not check.get("failure") and not choice.get("next"):
                    errors.append(f"{source}: node {node_id} choice {choice_id} roll/check must fail forward explicitly")
                for field in ("success", "failure", "critical_success", "critical_failure"):
                    ref = check.get(field)
                    if ref and ref not in nodes:
                        errors.append(f"{source}: node {node_id} choice {choice_id} {field}={ref!r} is missing")
    return errors


def compile_dialogues(source_root: Path, out_root: Path) -> tuple[int, list[str]]:
    count = 0
    errors: list[str] = []
    for path in sorted((source_root / "dialogues").glob("**/*")):
        if path.suffix.lower() not in {".yaml", ".yml", ".json"}:
            continue
        raw = read_doc_or_error(path, errors)
        if raw is None:
            continue
        identifier = normalize_identifier(raw.get("id") or path.stem)
        start = raw.get("entry") or raw.get("start")
        runtime = {
            "id": identifier,
            "version": raw.get("version", 1),
            "start": start,
            "nodes": {node_id: convert_node(node) for node_id, node in (raw.get("nodes") or {}).items()},
        }
        if raw.get("tags"):
            runtime["tags"] = raw["tags"]
        errors.extend(validate_dialogue(identifier, runtime, path))
        write_json(out_root, "dialogues", identifier, runtime)
        count += 1
    return count, errors


def compile_interactables(source_root: Path, out_root: Path) -> tuple[int, list[str]]:
    count = 0
    errors: list[str] = []
    for path in sorted((source_root / "interactables").glob("**/*")):
        if path.suffix.lower() not in {".json", ".yaml", ".yml"}:
            continue
        raw_doc = read_doc_or_error(path, errors)
        if raw_doc is None:
            continue
        raw = dict(raw_doc)
        identifier = normalize_identifier(raw.get("id") or path.stem)
        target_type = str(raw.pop("targetType", raw.pop("target_type", "block_group"))).lower()
        if target_type != "block_group":
            errors.append(f"{path}: only targetType=block_group is supported by the current MVP compiler")
            continue
        raw["id"] = identifier
        if "dialogue" in raw:
            raw["dialogue"] = normalize_identifier(raw["dialogue"])
        write_json(out_root, "interactions/block_groups", identifier, raw)
        count += 1
    return count, errors


def compile_npc(source_root: Path, out_root: Path) -> tuple[int, list[str]]:
    count = 0
    errors: list[str] = []
    for path in sorted((source_root / "npc").glob("**/*")):
        if path.suffix.lower() not in {".yaml", ".yml", ".json"}:
            continue
        raw_doc = read_doc_or_error(path, errors)
        if raw_doc is None:
            continue
        raw = dict(raw_doc)
        npc_id = str(raw.get("id") or path.stem)
        bindings = raw.get("dialogueBindings") or raw.get("dialogue_bindings") or {}
        default_dialogue = bindings.get("default") if isinstance(bindings, dict) else None
        base_entity = raw.get("baseEntity") or raw.get("base_entity") or "ebb:npc"
        tags = bindings.get("tags", []) if isinstance(bindings, dict) else []
        if default_dialogue:
            binding_id = normalize_identifier(npc_id)
            binding = {
                "match": {
                    "entity_type": normalize_identifier(base_entity, default_namespace="minecraft") if ":" not in str(base_entity) else str(base_entity),
                    "tags": tags or [f"ebb.npc.{npc_id}"],
                },
                "dialogue": normalize_identifier(default_dialogue),
                "interaction_range": 2.5,
                "highlight_range": 10.0,
                "priority": 160,
            }
            write_json(out_root, "interactions/entity_bindings", binding_id, binding)
            count += 1
        elif bindings:
            errors.append(f"{path}: dialogueBindings exists but has no default dialogue")

        routine = raw.get("routine")
        if isinstance(routine, dict):
            routine_identifier = normalize_identifier(routine.get("id") or f"{npc_id}_routine")
            steps = []
            for item in routine.get("schedule", []) or []:
                action = str(item.get("action") or ("walk" if "WALK" in str(item.get("state", "")).upper() else "stand")).lower()
                step: dict[str, Any] = {
                    "time": [int(item.get("from", 0)), int(item.get("to", 24000))],
                    "action": action,
                }
                if "pos" in item:
                    step["pos"] = item["pos"]
                if "path" in item:
                    step["path"] = item["path"]
                if "look" in item:
                    step["look"] = item["look"]
                steps.append(step)
            track = (((raw.get("lookPolicy") or {}).get("trackPlayerWhenNear") or {})
                     if isinstance(raw.get("lookPolicy"), dict) else {})
            runtime_routine = {
                "steps": steps,
                "look_at_player": {
                    "enabled": bool(track.get("enabled", False)),
                    "range": float(track.get("radius", 4.0)),
                    "max_yaw_speed": float(track.get("maxYawDelta", 8.0)),
                    "requires_line_of_sight": bool(track.get("requiresLineOfSight", track.get("requires_line_of_sight", True))),
                },
            }
            write_json(out_root, "npc_routines", routine_identifier, runtime_routine)
            count += 1
    return count, errors


def main() -> int:
    args = parse_args()
    out_root = Path("src/main/resources/data/ebb") if args.apply else args.out
    if args.clean and out_root.exists():
        shutil.rmtree(out_root)
    out_root.mkdir(parents=True, exist_ok=True)

    results = []
    errors: list[str] = []
    for label, compiler in (
        ("dialogues", compile_dialogues),
        ("interactables", compile_interactables),
        ("npc", compile_npc),
    ):
        count, section_errors = compiler(args.source, out_root)
        results.append(f"{label}={count}")
        errors.extend(section_errors)

    print("Compiled Ebb authoring sources to", out_root)
    print("Summary:", ", ".join(results), f"errors={len(errors)}")
    for error in errors:
        print("ERROR:", error, file=sys.stderr)
    return 1 if errors else 0


if __name__ == "__main__":
    raise SystemExit(main())
