#!/usr/bin/env python3
"""Static completion audit for deep-research-report (2).md implementation.

This is intentionally report-facing rather than review-facing: it checks the
architecture, schema, UI, NPC/routine, authoring, developer tooling, persistence,
and automated-test surfaces that the deep research report made actionable for
this Fabric 26.1.2 project.
"""
from __future__ import annotations

from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]


def read(relative: str) -> str:
    path = ROOT / relative
    if not path.exists():
        raise AssertionError(f"missing file: {relative}")
    return path.read_text(encoding="utf-8")


def exists(relative: str) -> None:
    if not (ROOT / relative).exists():
        raise AssertionError(f"missing path: {relative}")


def require(name: str, haystack: str, *needles: str) -> None:
    missing = [needle for needle in needles if needle not in haystack]
    if missing:
        raise AssertionError(f"{name}: missing {missing}")


def main() -> int:
    for api in [
        "TargetRef", "HitContext", "InteractableTarget", "InteractionOpenResult",
        "DialogueRepository", "DialogueRuntime", "DialogueStepResult",
        "RollRule", "RollContext", "RollOutcome", "RollService",
        "ValidationReport", "ReloadReport",
    ]:
        exists(f"src/main/java/com/crpg/ebb/api/{api}.java")

    definition = read("src/main/java/com/crpg/ebb/dialogue/DialogueDefinition.java")
    choice = read("src/main/java/com/crpg/ebb/dialogue/DialogueChoice.java")
    check = read("src/main/java/com/crpg/ebb/dialogue/DialogueCheck.java")
    condition = read("src/main/java/com/crpg/ebb/dialogue/DialogueCondition.java")
    effect = read("src/main/java/com/crpg/ebb/dialogue/DialogueEffect.java")
    node = read("src/main/java/com/crpg/ebb/dialogue/DialogueNode.java")
    require("Dialogue report schema", read("src/main/java/com/crpg/ebb/dialogue/DialogueNodeType.java"), "LINE", "CHOICE", "ROLL", "EFFECT", "JUMP", "END")
    require("Roll modes", read("src/main/java/com/crpg/ebb/dialogue/RollMode.java"), "RETRYABLE", "ONE_SHOT", "WHITE", "RED")
    require("Dialogue validation", definition, "failing forward", "that node is missing")
    require("Dialogue aliases", choice, '"kind"', '"label"', '"roll"')
    require("Choice semantics", choice, "singleUse", "revalidateTarget", "conditions", "effects")
    require("Check semantics", check, "advantage", "staticModifier", "successEffects", "failureEffects", "criticalSuccessEffects", "criticalFailureEffects", "effectsForOutcome")
    require("Node enter effects", node, "enterEffects", '"enter_effects"', "DialogueNodeType")
    require("Report conditions", condition, "VARIABLE_EQUALS", "ATTRIBUTE_AT_LEAST", "has_trait", "has_thought", "not_flag")
    require("Report effects", effect, "SET_VARIABLE", "ADD_TRAIT", "ADD_THOUGHT", "UNLOCK_RETRY", "setVar", "addTrait", "addThought", "unlock")

    screen = read("src/client/java/com/crpg/ebb/client/gui/dialogue/DialogueScreen.java")
    require("Dialogue Screen UI", screen, "extends Screen", "HistoryEntry", "renderScrollableBody", "renderStatusArea", "choiceLabel", "RollResultPayload", "VISIBLE_CHOICES")

    target_detector = read("src/client/java/com/crpg/ebb/client/interaction/ClientTargetDetector.java")
    require("Interaction focus prediction", target_detector, "ClientBlockGroupIndex", "ClientEntityTargetIndex", "highlightRange", "interactionRange")
    require("Prompt/highlight", read("src/client/java/com/crpg/ebb/client/render/InteractionPromptHud.java"), "ClientInteractionState.snapshot", "interact")
    require("Highlight renderer", read("src/client/java/com/crpg/ebb/client/render/TargetHighlightRenderer.java"), "LevelRenderEvents", "BlockGroupTarget", "renderAabb")

    saved = read("src/main/java/com/crpg/ebb/state/NarrativeSavedData.java")
    require("Saved data", saved, "SavedDataType", "CURRENT_SCHEMA_VERSION", "world_variables", "setPlayerVariable", "debugSnapshot", "schemaVersion")

    commands = read("src/main/java/com/crpg/ebb/registry/ModCommands.java")
    require("Developer commands", commands, "dev", "dialogue", "inspect", "tree", "vars", "reload", "routine", "save-debug", "summon_npc", "createAttributesCommand")

    routine_controller = read("src/main/java/com/crpg/ebb/routine/NpcRoutineController.java")
    routine_definition = read("src/main/java/com/crpg/ebb/routine/NpcRoutineDefinition.java")
    require("NPC routine controller", routine_controller, "applyConversationFocus", "applyMovement", "applyLookAtPlayer", "hasLineOfSight")
    require("NPC routine schema", routine_definition, "steps", "look_at_player", "requires_line_of_sight", "path", "debugSummary")
    require("NPC entity", read("src/main/java/com/crpg/ebb/npc/EbbNpcEntity.java"), "GeoEntity", "routineId", "routinePathIndex", "setPersistenceRequired")

    compiler = read("scripts/compile_authoring_sources.py")
    require("Authoring compiler", compiler, "compile_dialogues", "compile_interactables", "compile_npc", "fail forward", "requires_line_of_sight", "build/generated/ebb_authoring/data/ebb")
    for path in [
        "authoring/dialogues/harbor_clerk_intro.yaml",
        "authoring/interactables/city_office_counter.json",
        "authoring/npc/harbor_clerk_day_cycle.yaml",
    ]:
        exists(path)

    build = read("build.gradle")
    require("Gradle verification", build, "compileEbbAuthoring", "validateEbbData", "useJUnitPlatform", "gametestServer")
    workflow = read(".github/workflows/build.yml")
    require("CI", workflow, "java-version: '25'", "Validate data and run unit tests", "Run Fabric game tests", "Build mod jar")
    mod_json = read("src/main/resources/fabric.mod.json")
    require("Mod metadata", mod_json, "fabric-gametest", "geckolib", "\"java\"")
    exists("src/test/java/com/crpg/ebb/DeepResearchDataTest.java")
    exists("src/main/java/com/crpg/ebb/test/EbbGameTests.java")
    exists("docs/deep_research_report_completion_audit_2026-06-01.md")

    print("DeepResearchStaticAudit passed: report-facing architecture, schema, UI, NPC, authoring, persistence, devtool, and test surfaces are present.")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except AssertionError as exc:
        print(f"DeepResearchStaticAudit failed: {exc}", file=sys.stderr)
        raise SystemExit(1)
