#!/usr/bin/env python3
"""P24 authoring/reference validation for bundled and generated Ebb story data."""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any, Iterable

CATEGORIES = {
    "dialogues": "dialogue",
    "quest_branches": "quest",
    "feats": "feat",
    "chimes": "chime",
    "journal_entries": "journal",
    "clues": "clue",
    "npc_routines": "routine",
    "relationships": "relationship",
    "investigation_scenes": "scene",
    "conflicts": "conflict",
    "interactions/block_groups": "block_group",
    "interactions/entity_bindings": "entity_binding",
}

EFFECT_TARGETS = {
    "start_quest_branch": "quest_branches",
    "start_quest": "quest_branches",
    "complete_quest_branch": "quest_branches",
    "complete_quest": "quest_branches",
    "take_root": "quest_branches",
    "unlock_feat": "feats",
    "grant_feat": "feats",
    "activate_feat": "feats",
    "equip_feat": "feats",
    "add_journal_entry": "journal_entries",
    "add_journal": "journal_entries",
    "journal": "journal_entries",
    "reveal_clue": "clues",
    "add_clue": "clues",
    "clue": "clues",
    "set_npc_routine": "npc_routines",
    "npc_routine": "npc_routines",
    "routine": "npc_routines",
    "set_relation": "relationships",
    "add_relation": "relationships",
    "relation": "relationships",
    "relationship": "relationships",
    "start_conflict": "conflicts",
    "conflict": "conflicts",
    "add_conflict_stress": "conflicts",
    "add_conflict_resolve": "conflicts",
    "set_conflict_state": "conflicts",
    "apply_conflict_outcome": "conflicts",
    "conflict_outcome": "conflicts",
    "resolve_conflict": "conflicts",
    "set_scene_phase": "investigation_scenes",
    "scene_phase": "investigation_scenes",
}

CONDITION_TARGETS = {
    "quest_state": "quest_branches",
    "quest": "quest_branches",
    "quest_branch": "quest_branches",
    "has_feat": "feats",
    "feat": "feats",
    "has_active_feat": "feats",
    "has_journal_entry": "journal_entries",
    "journal": "journal_entries",
    "journal_entry": "journal_entries",
    "clue_found": "clues",
    "has_clue": "clues",
    "clue": "clues",
    "relation_at_least": "relationships",
    "relationship_at_least": "relationships",
    "relation": "relationships",
    "relationship": "relationships",
    "conflict_state": "conflicts",
    "conflict": "conflicts",
    "scene_phase": "investigation_scenes",
    "scene": "investigation_scenes",
}

ID_KEYS = (
    "id", "key", "quest", "quest_branch", "questBranch", "feat", "journal", "journal_entry", "journalEntry",
    "clue", "routine", "relation", "relationship", "conflict", "scene",
)


def normalize_id(raw: Any, namespace: str = "ebb") -> str:
    value = str(raw or "").strip()
    if not value:
        return ""
    if value.startswith("chime:"):
        value = value[len("chime:"):]
    return value if ":" in value else f"{namespace}:{value}"


def resource_id(path: Path, root: Path, category: str, data: dict[str, Any]) -> str:
    declared = data.get("id")
    if isinstance(declared, str) and declared.strip():
        return normalize_id(declared)
    rel = path.relative_to(root / category).with_suffix("")
    return "ebb:" + rel.as_posix()


def load_json(path: Path) -> tuple[dict[str, Any] | None, str | None]:
    try:
        return json.loads(path.read_text(encoding="utf-8")), None
    except json.JSONDecodeError as exc:
        return None, f"{path}:{exc.lineno}:{exc.colno}: invalid JSON: {exc.msg}"
    except OSError as exc:
        return None, f"{path}: could not read: {exc}"


def iter_json(root: Path, category: str) -> Iterable[Path]:
    base = root / category
    if not base.exists():
        return []
    return sorted(path for path in base.rglob("*.json") if path.is_file())


def collect(root: Path) -> tuple[dict[str, dict[str, dict[str, Any]]], list[str], dict[str, list[str]]]:
    ids: dict[str, dict[str, dict[str, Any]]] = {category: {} for category in CATEGORIES}
    errors: list[str] = []
    chime_tags: dict[str, list[str]] = {}
    for category in CATEGORIES:
        for path in iter_json(root, category):
            data, error = load_json(path)
            if error:
                errors.append(error)
                continue
            if not isinstance(data, dict):
                errors.append(f"{path}: top-level JSON must be an object")
                continue
            identifier = resource_id(path, root, category, data)
            ids[category][identifier] = {"path": str(path), "data": data}
            if category == "chimes":
                tags = data.get("trigger_tags") or data.get("tags") or []
                if isinstance(tags, list):
                    for tag in tags:
                        if isinstance(tag, str):
                            chime_tags.setdefault(tag, []).append(identifier)
    return ids, errors, chime_tags


def has_id(ids: dict[str, dict[str, dict[str, Any]]], category: str, raw: Any) -> bool:
    identifier = normalize_id(raw)
    return bool(identifier) and identifier in ids.get(category, {})


def effect_id(effect: dict[str, Any]) -> str:
    for key in ID_KEYS:
        value = effect.get(key)
        if isinstance(value, (str, int)):
            return str(value)
    for shortcut in ("setStoryVar", "clearStoryVar", "addStoryInt", "setFlag", "clearFlag", "setVar", "unlock"):
        value = effect.get(shortcut)
        if isinstance(value, (str, int)):
            return str(value)
    return ""


def condition_id(condition: dict[str, Any]) -> str:
    for key in ID_KEYS + ("flag", "story_var", "storyVar", "attribute", "npc", "npc_id", "npcId"):
        value = condition.get(key)
        if isinstance(value, (str, int)):
            return str(value)
    return ""


def type_name(raw: Any, fallback: str = "") -> str:
    return str(raw or fallback).strip().lower().replace("-", "_")


def validate_effect(effect: dict[str, Any], where: str, ids: dict[str, dict[str, dict[str, Any]]], errors: list[str]) -> None:
    raw_type = type_name(effect.get("type"))
    if not raw_type:
        if "conflict" in effect and any(key in effect for key in ("outcome", "outcome_id", "outcomeId")):
            raw_type = "apply_conflict_outcome"
        # Shortcuts consumed by DialogueEffect.parse.
        else:
            for shortcut, effect_type in (
                ("quest", "complete_quest_branch"), ("feat", "unlock_feat"), ("journal", "add_journal_entry"),
                ("clue", "reveal_clue"), ("routine", "set_npc_routine"), ("relationship", "set_relation"),
                ("conflict", "start_conflict"), ("scene", "set_scene_phase"),
            ):
                if shortcut in effect:
                    raw_type = effect_type
                    break
    target_category = EFFECT_TARGETS.get(raw_type)
    if not target_category:
        return
    raw_id = effect_id(effect)
    if not has_id(ids, target_category, raw_id):
        errors.append(f"{where}: effect type={raw_type} references missing {target_category} id {raw_id!r}")
        return
    if raw_type in {"apply_conflict_outcome", "conflict_outcome", "resolve_conflict"}:
        conflict_id = normalize_id(raw_id)
        outcome_id = str(effect.get("outcome") or effect.get("outcome_id") or effect.get("outcomeId") or effect.get("value") or "").strip().lower().replace("-", "_")
        outcomes = ids["conflicts"][conflict_id]["data"].get("outcomes") or []
        known = {
            str(outcome.get("id", "")).strip().lower().replace("-", "_")
            for outcome in outcomes
            if isinstance(outcome, dict)
        }
        if not outcome_id or outcome_id not in known:
            errors.append(f"{where}: conflict outcome {outcome_id!r} is not declared by {conflict_id}")


def validate_condition(condition: dict[str, Any], where: str, ids: dict[str, dict[str, dict[str, Any]]], errors: list[str]) -> None:
    raw_type = type_name(condition.get("type"), "flag")
    target_category = CONDITION_TARGETS.get(raw_type)
    if not target_category:
        return
    raw_id = condition_id(condition)
    if not has_id(ids, target_category, raw_id):
        errors.append(f"{where}: condition type={raw_type} references missing {target_category} id {raw_id!r}")


def validate_effect_list(effects: Any, where: str, ids: dict[str, dict[str, dict[str, Any]]], errors: list[str]) -> None:
    if effects is None:
        return
    if not isinstance(effects, list):
        errors.append(f"{where}: effects must be an array")
        return
    for index, effect in enumerate(effects):
        if isinstance(effect, dict):
            validate_effect(effect, f"{where}[{index}]", ids, errors)
        else:
            errors.append(f"{where}[{index}]: effect must be an object")


def validate_conditions(conditions: Any, where: str, ids: dict[str, dict[str, dict[str, Any]]], errors: list[str]) -> None:
    if conditions is None:
        return
    if not isinstance(conditions, list):
        errors.append(f"{where}: conditions must be an array")
        return
    for index, condition in enumerate(conditions):
        if isinstance(condition, dict):
            validate_condition(condition, f"{where}[{index}]", ids, errors)
        else:
            errors.append(f"{where}[{index}]: condition must be an object")


def validate_dialogues(ids: dict[str, dict[str, dict[str, Any]]], chime_tags: dict[str, list[str]], errors: list[str]) -> None:
    for dialogue_id, payload in ids["dialogues"].items():
        path = payload["path"]
        data = payload["data"]
        nodes = data.get("nodes") or {}
        if not isinstance(nodes, dict):
            continue
        for node_id, node in nodes.items():
            if not isinstance(node, dict):
                continue
            where = f"{path}:dialogue {dialogue_id}.nodes.{node_id}"
            validate_effect_list(node.get("enter_effects"), f"{where}.enter_effects", ids, errors)
            validate_effect_list(node.get("effects"), f"{where}.effects", ids, errors)
            tags = node.get("chime_tags") or []
            if isinstance(tags, list):
                for tag in tags:
                    if isinstance(tag, str) and tag not in chime_tags:
                        errors.append(f"{where}: chime_tags entry {tag!r} has no matching chime trigger_tags")
            choices = node.get("choices") or []
            if not isinstance(choices, list):
                errors.append(f"{where}.choices: choices must be an array")
                continue
            for choice_index, choice in enumerate(choices):
                if not isinstance(choice, dict):
                    errors.append(f"{where}.choices[{choice_index}]: choice must be an object")
                    continue
                choice_id = choice.get("id", f"#{choice_index}")
                cwhere = f"{where}.choices[{choice_id}]"
                validate_conditions(choice.get("conditions"), f"{cwhere}.conditions", ids, errors)
                validate_effect_list(choice.get("effects"), f"{cwhere}.effects", ids, errors)
                check = choice.get("check") or choice.get("roll")
                if isinstance(check, dict):
                    for key in ("success_effects", "failure_effects", "critical_success_effects", "critical_failure_effects"):
                        validate_effect_list(check.get(key), f"{cwhere}.check.{key}", ids, errors)
                    dc = check.get("dc", 0)
                    high_stakes = not bool(check.get("optional", False)) and isinstance(dc, int | float) and dc >= 10
                    has_failure_path = bool(check.get("failure") or choice.get("next"))
                    has_failure_effects = bool(check.get("failure_effects"))
                    if high_stakes and not (has_failure_path or has_failure_effects):
                        errors.append(f"{cwhere}: high-stakes check dc={dc} must fail forward with failure branch or failure_effects")


def validate_interactions(ids: dict[str, dict[str, dict[str, Any]]], errors: list[str]) -> None:
    for category in ("interactions/block_groups", "interactions/entity_bindings"):
        for identifier, payload in ids[category].items():
            path = payload["path"]
            data = payload["data"]
            dialogue = data.get("dialogue")
            if dialogue and not has_id(ids, "dialogues", dialogue):
                errors.append(f"{path}:{identifier}: missing dialogue id {dialogue!r}")


def validate_secondary_registries(ids: dict[str, dict[str, dict[str, Any]]], errors: list[str]) -> None:
    for quest_id, payload in ids["quest_branches"].items():
        for feat in payload["data"].get("grant_feats", []) or []:
            if isinstance(feat, str) and not has_id(ids, "feats", feat):
                errors.append(f"{payload['path']}:{quest_id}: grant_feats references missing feat {feat!r}")
        validate_effect_list(payload["data"].get("take_root_effects"), f"{payload['path']}:{quest_id}.take_root_effects", ids, errors)
    for scene_id, payload in ids["investigation_scenes"].items():
        for clue in payload["data"].get("clues", []) or []:
            if isinstance(clue, str) and not has_id(ids, "clues", clue):
                errors.append(f"{payload['path']}:{scene_id}: clues references missing clue {clue!r}")
    for clue_id, payload in ids["clues"].items():
        data = payload["data"]
        if data.get("scene") and not has_id(ids, "investigation_scenes", data["scene"]):
            errors.append(f"{payload['path']}:{clue_id}: scene references missing investigation scene {data['scene']!r}")
        if data.get("journal_entry") and not has_id(ids, "journal_entries", data["journal_entry"]):
            errors.append(f"{payload['path']}:{clue_id}: journal_entry references missing journal entry {data['journal_entry']!r}")
    for journal_id, payload in ids["journal_entries"].items():
        quest = payload["data"].get("quest")
        if quest and not has_id(ids, "quest_branches", quest):
            errors.append(f"{payload['path']}:{journal_id}: quest references missing quest branch {quest!r}")
    for conflict_id, payload in ids["conflicts"].items():
        scene = payload["data"].get("scene")
        if scene and not has_id(ids, "investigation_scenes", scene):
            errors.append(f"{payload['path']}:{conflict_id}: scene references missing investigation scene {scene!r}")


def validate_root(root: Path) -> list[str]:
    ids, errors, chime_tags = collect(root)
    validate_interactions(ids, errors)
    validate_dialogues(ids, chime_tags, errors)
    validate_secondary_registries(ids, errors)
    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("roots", nargs="*", type=Path, default=[Path("src/main/resources/data/ebb"), Path("build/generated/ebb_authoring/data/ebb")])
    args = parser.parse_args()
    all_errors: list[str] = []
    for root in args.roots:
        if not root.exists():
            print(f"P24AuthoringValidation skipped missing root: {root}")
            continue
        errors = validate_root(root)
        print(f"P24AuthoringValidation root={root} errors={len(errors)}")
        all_errors.extend(errors)
    for error in all_errors:
        print("ERROR:", error, file=sys.stderr)
    if all_errors:
        return 1
    print("P24AuthoringValidation passed: dialogue, interactable, quest, feat, chime, journal/clue, routine, relationship, scene, and conflict references are consistent.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
