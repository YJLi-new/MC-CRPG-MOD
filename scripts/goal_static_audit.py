#!/usr/bin/env python3
"""Static audit for the active GOAL.md implementation track.

The GOAL document is larger than the previous review reports. This audit grows
phase-by-phase; it currently gates P20/P21 documentation/baseline guardrails,
P2 Story Variables, P3 Quest Branch /
Take Root / Feat MVP, P4 Chime / Inner Voice MVP, P5 Journal/UI rhythm,
P6 relationship/NPC-memory/routine wiring, P7 investigation/conflict, and
P8 playable vertical-slice content minimums.
"""
from __future__ import annotations

import hashlib
import json
from pathlib import Path
import re
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


def parse_properties(relative: str) -> dict[str, str]:
    props: dict[str, str] = {}
    for raw in read(relative).splitlines():
        line = raw.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        props[key.strip()] = value.strip()
    return props


def json_files(relative: str) -> list[Path]:
    root = ROOT / relative
    if not root.exists():
        raise AssertionError(f"missing path: {relative}")
    return sorted(root.rglob("*.json"))


def load_json(path: Path) -> dict:
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        raise AssertionError(f"{path.relative_to(ROOT)}: invalid JSON: {exc}") from exc
    if not isinstance(data, dict):
        raise AssertionError(f"{path.relative_to(ROOT)}: expected JSON object")
    return data


def sha256(relative: str) -> str:
    path = ROOT / relative
    if not path.exists():
        raise AssertionError(f"missing file for hash: {relative}")
    digest = hashlib.sha256()
    with path.open("rb") as fh:
        for chunk in iter(lambda: fh.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def audit_p20_p21_documentation_and_baseline() -> None:
    for path in [
        "GOAL.md",
        "README.md",
        "AGENTS.md",
        "docs/architecture.md",
        "docs/current_status.md",
        "docs/status_reconciliation_2026-06-02.md",
    ]:
        exists(path)

    goal = read("GOAL.md")
    require("GOAL product plan", goal, "Minecraft Disco-like CRPG Mod", "P20", "P21", "P31", "Final Target for Alpha 0.1")
    require("GOAL operating contract", goal, "migrate this project", "Mandatory verification", "Every new dialogue check must fail forward")

    readme = read("README.md")
    require("README onboarding", readme, "Esoteric Ebb CRPG", "Build and validate", "Play in the local test profile", "Authoring")
    require("README verification", readme, "scripts/run_smoke_checks.sh", "scripts/gradle-local.sh --no-daemon build", "GOAL.md", "docs/json_authoring_guide.md")

    agents = read("AGENTS.md")
    require("AGENTS guardrails", agents, "server-authoritative", "data-driven", "Do not make all entities interactable", "26.1.2-Fabric-Ebb-Test")

    architecture = read("docs/architecture.md")
    require("Architecture doc", architecture, "Runtime flow", "Data layer", "Interaction layer", "Networking layer", "Narrative state", "Verification contract")

    status = read("docs/current_status.md")
    require("Current status doc", status, "dialogues=13", "block_groups=8", "entity_bindings=10", "127 steps, 0 failures")
    require("Status reconciliation", read("docs/status_reconciliation_2026-06-02.md"), "Historical docs remain valid", "final GUI report", "current status authorities")

    props = parse_properties("gradle.properties")
    expected = {
        "minecraft_version": "26.1.2",
        "loader_version": "0.19.2",
        "fabric_api_version": "0.150.0+26.1.2",
        "loom_version": "1.17.0-alpha.13",
        "geckolib_version": "5.5.1",
        "mod_version": "0.1.0-dev",
        "maven_group": "com.crpg",
        "archives_base_name": "ebb",
        "mod_id": "ebb",
        "mod_name": "Esoteric Ebb CRPG",
    }
    mismatches = {key: (props.get(key), value) for key, value in expected.items() if props.get(key) != value}
    if mismatches:
        raise AssertionError(f"gradle.properties pin mismatch: {mismatches}")
    for value in expected.values():
        if value not in goal and value not in status and value not in readme:
            raise AssertionError(f"version/metadata value is undocumented: {value}")

    fabric_mod = read("src/main/resources/fabric.mod.json")
    require("fabric.mod.json entrypoints", fabric_mod, "com.crpg.ebb.EbbMod", "com.crpg.ebb.client.EbbClient", "com.crpg.ebb.test.EbbGameTests")

    for data_dir in [
        "src/main/resources/data/ebb/interactions/settings",
        "src/main/resources/data/ebb/interactions/entity_bindings",
        "src/main/resources/data/ebb/interactions/block_groups",
        "src/main/resources/data/ebb/dialogues",
        "src/main/resources/data/ebb/attributes",
        "src/main/resources/data/ebb/chimes",
        "src/main/resources/data/ebb/journal_entries",
        "src/main/resources/data/ebb/quest_branches",
        "src/main/resources/data/ebb/feats",
        "src/main/resources/data/ebb/relationships",
        "src/main/resources/data/ebb/npc_routines",
        "src/main/resources/data/ebb/clues",
        "src/main/resources/data/ebb/investigation_scenes",
        "src/main/resources/data/ebb/conflicts",
        "authoring/dialogues",
        "authoring/interactables",
        "authoring/npc",
    ]:
        exists(data_dir)

    # P21: built jars must be documented when they exist. This keeps status docs
    # honest after Java/resource changes that affect artifacts.
    if (ROOT / "build/libs/ebb-0.1.0-dev.jar").exists():
        jar_hash = sha256("build/libs/ebb-0.1.0-dev.jar")
        if jar_hash not in status:
            raise AssertionError(f"docs/current_status.md missing current jar hash {jar_hash}")
    if (ROOT / "build/libs/ebb-0.1.0-dev-sources.jar").exists():
        sources_hash = sha256("build/libs/ebb-0.1.0-dev-sources.jar")
        if sources_hash not in status:
            raise AssertionError(f"docs/current_status.md missing current sources jar hash {sources_hash}")

    # Failure-forward lint: every bundled checked choice must have a failure
    # branch and/or failure effects, and any failure branch must resolve to a
    # non-empty node in the same dialogue.
    for path in json_files("src/main/resources/data/ebb/dialogues"):
        dialogue = load_json(path)
        nodes = dialogue.get("nodes", {})
        if not isinstance(nodes, dict):
            continue
        for node_id, node in nodes.items():
            if not isinstance(node, dict):
                continue
            choices = node.get("choices", [])
            if not isinstance(choices, list):
                continue
            for choice in choices:
                if not isinstance(choice, dict) or "check" not in choice:
                    continue
                check = choice.get("check")
                if not isinstance(check, dict):
                    raise AssertionError(f"{path.relative_to(ROOT)}:{node_id}:{choice.get('id')}: check must be an object")
                failure = check.get("failure") or check.get("fail")
                failure_effects = check.get("failure_effects") or check.get("fail_effects")
                if not failure and not failure_effects:
                    raise AssertionError(f"{path.relative_to(ROOT)}:{node_id}:{choice.get('id')}: checked choice lacks failure-forward branch/effects")
                if failure:
                    failure_node = nodes.get(str(failure))
                    if not isinstance(failure_node, dict):
                        raise AssertionError(f"{path.relative_to(ROOT)}:{node_id}:{choice.get('id')}: missing failure node {failure}")
                    if not (failure_node.get("text") or failure_node.get("text_key") or failure_node.get("choices") or failure_node.get("enter_effects")):
                        raise AssertionError(f"{path.relative_to(ROOT)}:{failure}: failure node is empty")

    # Major branches must take root into visible text plus a build/state
    # consequence.
    for path in json_files("src/main/resources/data/ebb/quest_branches"):
        quest = load_json(path)
        if str(quest.get("kind", "")).lower() != "major":
            continue
        if not str(quest.get("take_root_text", "")).strip():
            raise AssertionError(f"{path.relative_to(ROOT)}: major quest branch missing take_root_text")
        if not quest.get("grant_feats") and not quest.get("take_root_effects"):
            raise AssertionError(f"{path.relative_to(ROOT)}: major quest branch lacks feat/effect consequence")


def audit_p22_interaction_highlight_polish() -> None:
    exists("src/main/java/com/crpg/ebb/interaction/HighlightStyle.java")
    highlight_style = read("src/main/java/com/crpg/ebb/interaction/HighlightStyle.java")
    require("HighlightStyle", highlight_style, "closeColor", "farColor", "RenderMode", "OUTLINE", "MERGED", "BOUNDS", "opacity")

    block_group = read("src/main/java/com/crpg/ebb/interaction/BlockGroupDefinition.java")
    require("Block group highlight style", block_group, "HighlightStyle", "parseOptional", "highlightStyle", "blockDefault")
    entity_binding = read("src/main/java/com/crpg/ebb/interaction/entity/EntityBindingDefinition.java")
    require("Entity binding highlight style", entity_binding, "HighlightStyle", "parseOptional", "highlightStyle", "entityDefault", "debugSummary")

    require("Block group style sync", read("src/main/java/com/crpg/ebb/network/sync/BlockGroupSyncPayload.java"), "writeHighlightStyle", "readHighlightStyle", "definition.highlightStyle()")
    require("Entity binding style sync", read("src/main/java/com/crpg/ebb/network/sync/EntityBindingSyncPayload.java"), "writeHighlightStyle", "readHighlightStyle", "definition.highlightStyle()")
    require("Entity target style sync", read("src/main/java/com/crpg/ebb/network/sync/EntityTargetSyncPayload.java"), "writeHighlightStyle", "readHighlightStyle", "target.highlightStyle()")
    require("Synced entity style", read("src/main/java/com/crpg/ebb/interaction/entity/SyncedEntityTarget.java"), "HighlightStyle", "binding.highlightStyle()")

    detector = read("src/client/java/com/crpg/ebb/client/interaction/ClientTargetDetector.java")
    require("Client target prediction reasons/styles", detector, "highlightStyle", "outside_binding_highlight_range", "unbound_entity", "targetData.highlightStyle()", "binding.highlightStyle()")
    require("Entity highlight respects binding range", detector, "distance > highlightRange", "ClientEntityTargetIndex.contains", "EntityBindingRegistry.isRegisteredTarget")

    state = read("src/client/java/com/crpg/ebb/client/interaction/ClientInteractionState.java")
    require("Client interaction state debug", state, "reason", "highlightStyle", "empty(String reason)")

    renderer = read("src/client/java/com/crpg/ebb/client/render/TargetHighlightRenderer.java")
    require("Target renderer style/merge", renderer, "snapshot.highlightStyle()", "mergeAdjacentBlockBoxes", "RenderMode.MERGED", "RenderMode.BOUNDS", "RenderMode.OUTLINE")

    hud = read("src/client/java/com/crpg/ebb/client/render/InteractionPromptHud.java")
    require("Target debug overlay", hud, "showDebugScreen", "Ebb target:", "snapshot.reason()", "style=")

    service = read("src/main/java/com/crpg/ebb/interaction/InteractionService.java")
    require("Server LOS collider policy", service, "InteractionRaycastPolicy.blockModeForAuthority()", "blocked_line_of_sight")

    locked_door = load_json(ROOT / "src/main/resources/data/ebb/interactions/block_groups/demo/locked_door.json")
    if "highlight" not in locked_door or locked_door["highlight"].get("render_mode") != "merged":
        raise AssertionError("locked_door block group should exercise highlight.render_mode=merged")
    innkeeper_binding = load_json(ROOT / "src/main/resources/data/ebb/interactions/entity_bindings/demo/innkeeper_ebb_npc_name.json")
    if "highlight" not in innkeeper_binding or innkeeper_binding["highlight"].get("render_mode") != "outline":
        raise AssertionError("innkeeper name binding should exercise highlight.render_mode=outline")

    docs = read("docs/json_authoring_guide.md")
    require("Authoring docs highlight style", docs, "Optional `highlight` fields", "render_mode", "merged", "bounds", "far_opacity")


def audit_p23_dialogue_ui_reading_rhythm_foundation() -> None:
    exists("src/client/java/com/crpg/ebb/client/gui/dialogue/ClientDialogueSettings.java")
    settings = read("src/client/java/com/crpg/ebb/client/gui/dialogue/ClientDialogueSettings.java")
    require(
        "P23 dialogue client settings",
        settings,
        "dialogue_font_scale",
        "dialogue_text_speed",
        "getConfigDir",
        "ebb-client.json",
        "increaseFontScale",
        "decreaseFontScale",
        "cycleTextSpeed",
        "TextSpeed",
        "INSTANT",
    )
    require("P23 settings initialization", read("src/client/java/com/crpg/ebb/client/EbbClient.java"), "ClientDialogueSettings.load()")
    screen = read("src/client/java/com/crpg/ebb/client/gui/dialogue/DialogueScreen.java")
    require(
        "P23 dialogue screen rhythm foundation",
        screen,
        "ClientDialogueSettings",
        "translatableWithFallback",
        "keyPressed",
        "GLFW_KEY_1",
        "GLFW_KEY_ENTER",
        "▶ ",
        "addSettingsWidgets",
        "drawScaledText",
        "visibleCharacters",
        "renderStatusArea",
        "enableScissor",
    )
    require(
        "P23 distinct dialogue/status styling",
        screen,
        "case DIALOGUE",
        "case ACTION",
        "case THOUGHT",
        "ChatFormatting.GOLD",
        "ChatFormatting.AQUA",
        "CHIME_STATUS_COLOR",
        "STATUS_COLOR",
        "take_root:",
        "QUEST_STATUS_COLOR",
    )
    check = read("src/main/java/com/crpg/ebb/dialogue/DialogueCheck.java")
    require("P23 check display controls", check, "hidden_dc", "hidden_roll", "showDc", "showRoll", "display_dc", "display_roll")
    visible = read("src/main/java/com/crpg/ebb/network/dialogue/VisibleDialogueChoice.java")
    require("P23 choice check summary controls", visible, "showDc()", "showRoll()", "DC ?", "hidden roll")
    roll = read("src/main/java/com/crpg/ebb/network/dialogue/RollResultPayload.java")
    require("P23 roll-result display controls", roll, "showDc", "showRoll", "hidden DC", "hidden roll")
    test = read("src/test/java/com/crpg/ebb/DeepResearchDataTest.java")
    require("P23 JUnit hidden roll/DC coverage", test, "hiddenDcAndHiddenRollAffectPlayerFacingSummaries", "hidden_dc", "hidden_roll", "VisibleDialogueChoice.fromChoice")
    docs = read("docs/json_authoring_guide.md")
    require("P23 authoring docs hidden roll/DC/settings", docs, "hidden_dc", "hidden_roll", "show_dc", "show_roll", "falls back to literal", "A-/A+", "config/ebb-client.json")
    lang = read("src/main/resources/assets/ebb/lang/en_us.json") + read("src/main/resources/assets/ebb/lang/zh_cn.json")
    require("P23 translation keys", lang, "screen.ebb.dialogue.font_scale", "screen.ebb.dialogue.text_speed", "screen.ebb.dialogue.text_speed.instant")


def audit_p24_authoring_validation_hardening() -> None:
    exists("scripts/p24_authoring_validation.py")
    validator = read("scripts/p24_authoring_validation.py")
    require(
        "P24 cross-reference validator",
        validator,
        "EFFECT_TARGETS",
        "CONDITION_TARGETS",
        "validate_dialogues",
        "high-stakes check",
        "chime_tags entry",
        "P24AuthoringValidation passed",
    )
    compiler = read("scripts/compile_authoring_sources.py")
    require(
        "P24 compiler diagnostics",
        compiler,
        "format_read_error",
        "JSONDecodeError",
        "problem_mark",
        "invalid YAML",
        "invalid JSON",
        "read_doc_or_error",
    )
    smoke = read("scripts/run_smoke_checks.sh")
    require(
        "P24 smoke wiring",
        smoke,
        "scripts/p24_authoring_validation.py",
        "authoring/examples/tavern_case",
        "bad.yaml",
        "invalid YAML",
    )
    docs = read("docs/json_authoring_guide.md")
    require(
        "P24 authoring reference docs",
        docs,
        "Condition reference",
        "Effect reference",
        "docs/schemas/",
        "High-stakes checks",
        "quest_branches",
        "npc_routines",
        "relationships",
        "investigation_scenes",
    )
    for schema in [
        "docs/schemas/ebb.dialogue.schema.json",
        "docs/schemas/ebb.block_group.schema.json",
        "docs/schemas/ebb.entity_binding.schema.json",
        "docs/schemas/ebb.chime.schema.json",
    ]:
        exists(schema)
        data = load_json(ROOT / schema)
        if "$schema" not in data or "properties" not in data:
            raise AssertionError(f"{schema} should be a JSON Schema with properties")
    for example in [
        "authoring/examples/tavern_case/dialogues/locked_pantries.yaml",
        "authoring/examples/tavern_case/interactables/pantry_doors.json",
        "authoring/examples/tavern_case/npc/pantry_keeper.yaml",
    ]:
        exists(example)
    chimes = read("src/main/resources/data/ebb/chimes/demo/instinct.json") + read("src/main/resources/data/ebb/chimes/demo/empathy.json")
    require("P24 chime trigger reference fix", chimes, "witness.read")


def audit_p25_quest_feat_maturation() -> None:
    quest_service = read("src/main/java/com/crpg/ebb/quest/QuestTreeService.java")
    require(
        "P25 quest tree service lines",
        quest_service,
        "Branch Map:",
        "◆ MAJOR",
        "◇ MINOR",
        "★ TAKE ROOT:",
        "Feat Loadout:",
        "sourceQuests",
        "modifiers=",
    )
    quest_screen = read("src/client/java/com/crpg/ebb/client/gui/quest/QuestTreeScreen.java")
    require(
        "P25 quest screen filters/colors",
        quest_screen,
        "filter=",
        "TAKE_ROOT",
        "MAJOR",
        "MINOR",
        "FEATS",
        "lineColor",
        "visibleLines",
    )
    journal_service = read("src/main/java/com/crpg/ebb/journal/JournalService.java")
    require("P25 journal category payload", journal_service, "[category=", "entry.category().serializedName()")
    journal_screen = read("src/client/java/com/crpg/ebb/client/gui/journal/JournalScreen.java")
    require("P25 journal filters", journal_screen, "Filter", "CLUES", "LEADS", "QUESTS", "SCENES", "category=clue", "category=lead", "visibleLines")
    dialogue_screen = read("src/client/java/com/crpg/ebb/client/gui/dialogue/DialogueScreen.java")
    require("P25 take-root special color", dialogue_screen, "TAKE_ROOT_STATUS_COLOR", "status.startsWith(\"take_root:\")")
    test = read("src/test/java/com/crpg/ebb/DeepResearchDataTest.java")
    require("P25 take-root idempotency test", test, "majorQuestCannotTakeRootTwice", "quest_already_take_rooted", "unlockedFeatIds", "activeFeatIds")


def audit_p26_chime_inner_voice_expansion() -> None:
    expected = {
        "dread": "luck",
        "empathy": "wisdom",
        "endurance": "constitution",
        "finesse": "dexterity",
        "force": "strength",
        "instinct": "perception",
        "logic": "intelligence",
        "rhetoric": "charisma",
    }
    actual_attributes: dict[str, str] = {}
    for name, attribute in expected.items():
        relative = f"src/main/resources/data/ebb/chimes/demo/{name}.json"
        exists(relative)
        data = load_json(ROOT / relative)
        actual_attributes[name] = str(data.get("source_attribute", ""))
        if actual_attributes[name] != attribute:
            raise AssertionError(f"{relative}: expected source_attribute={attribute}, got {actual_attributes[name]}")
        if not str(data.get("tone_guide", "")).strip():
            raise AssertionError(f"{relative}: missing tone_guide")
        if not data.get("trigger_tags"):
            raise AssertionError(f"{relative}: missing trigger_tags")
        if int(data.get("cooldown_ticks", 0)) <= 0:
            raise AssertionError(f"{relative}: cooldown_ticks must be positive")
        if data.get("one_shot_per_node") is not True:
            raise AssertionError(f"{relative}: one_shot_per_node must be true")
        if not data.get("active_thoughts"):
            raise AssertionError(f"{relative}: missing active_thoughts")
    if set(actual_attributes.values()) != set(expected.values()):
        raise AssertionError(f"P26 chime attributes should cover DND-8 exactly: {actual_attributes}")

    definition = read("src/main/java/com/crpg/ebb/chime/ChimeDefinition.java")
    require(
        "P26 ChimeDefinition metadata",
        definition,
        "toneGuide",
        "activeThoughtIds",
        "oneShotPerNode",
        "oneShotGlobal",
        '"tone_guide"',
        '"active_thoughts"',
        '"one_shot_per_node"',
    )
    resolver = read("src/main/java/com/crpg/ebb/chime/ChimeResolver.java")
    require(
        "P26 ChimeResolver anti-spam",
        resolver,
        "dayTime",
        "cooldownTicks",
        "chime_last_tick:",
        "setPlayerVariable",
        "oneShotPerNode",
        "oneShotGlobal",
        "cooldownRemainingTicks",
    )
    require("P26 DialogueService passes time to chimes", read("src/main/java/com/crpg/ebb/dialogue/DialogueService.java"), "ChimeResolver.resolve(definition, node, state, session.playerUuid(), dayTime)")
    require("P26 dev chime trigger reasons", read("src/main/java/com/crpg/ebb/dev/DevSnapshotService.java"), "Chime trigger debug", "ChimeResolver.explain", "no active dialogue", "chime_tags")

    innkeeper = read("src/main/resources/data/ebb/dialogues/demo/innkeeper_intro.json")
    for name in expected:
        require(
            f"P26 innkeeper active route for {name}",
            innkeeper,
            f"chime:ebb:demo/{name}",
            f"ebb:demo/thought_{name}",
        )
        if name != "rhetoric":
            require(f"P26 innkeeper node for {name}", innkeeper, f"{name}_chime_thought", f"{name}_chime_line")
    require("P26 rhetoric backwards-compatible active route", innkeeper, "rhetoric_insight", "rhetoric_line")

    test = read("src/test/java/com/crpg/ebb/DeepResearchDataTest.java")
    require(
        "P26 JUnit coverage",
        test,
        "p26ChimeSetCoversEightAttributesAndActiveThoughtRoutes",
        "chimeCooldownPreventsRepeatedPassiveInsertsAcrossNodes",
        "ChimeResolver.explain",
        "ChimeRegistry.size() >= 8",
        "toneGuide()",
        "activeThoughtIds()",
    )
    smoke = read("scripts/smoke/DeepResearchSmoke.java")
    require("P26 smoke coverage", smoke, "P26 chime coverage", "expectedChimeAttributes", "toneGuide", "cooldownTicks")
    docs = read("docs/json_authoring_guide.md")
    require("P26 authoring docs", docs, "tone_guide", "active_thoughts", "one_shot_per_node", "cooldown_ticks", "eight attribute voices")
    schema = read("docs/schemas/ebb.chime.schema.json")
    require("P26 chime schema", schema, "tone_guide", "active_thoughts", "one_shot_per_node", "one_shot_global")


def audit_p27_npc_animation_routine_production() -> None:
    for role in ["innkeeper", "witness", "tenant", "guard"]:
        exists(f"src/main/resources/assets/ebb/textures/entity/npc_{role}.png")
    for animation_path in [
        "src/main/resources/assets/ebb/animations/entity/npc.animation.json",
        "src/main/resources/assets/ebb/geckolib/animations/entity/npc.animation.json",
    ]:
        animation = read(animation_path)
        require("P27 NPC conversation animations", animation, "dialogue.talk", "dialogue.think", "dialogue.dismiss", "dialogue.nervous_idle", "misc.fidget")

    entity = read("src/main/java/com/crpg/ebb/npc/EbbNpcEntity.java")
    require(
        "P27 NPC entity state",
        entity,
        "visualRole",
        "beginConversationFocus",
        "endConversationFocus",
        "routineDebugSummary",
        "AnimationController<EbbNpcEntity>",
        "dialogue.talk",
        "dialogue.nervous_idle",
    )
    for client_file in [
        "src/client/java/com/crpg/ebb/client/npc/EbbNpcRenderData.java",
        "src/client/java/com/crpg/ebb/client/npc/EbbNpcModel.java",
        "src/client/java/com/crpg/ebb/client/npc/EbbNpcRenderer.java",
    ]:
        exists(client_file)
    require("P27 role-aware model", read("src/client/java/com/crpg/ebb/client/npc/EbbNpcModel.java"), "VISUAL_ROLE", "npc_", "textures/entity")
    require("P27 role-aware renderer", read("src/client/java/com/crpg/ebb/client/npc/EbbNpcRenderer.java"), "captureDefaultRenderState", "addGeckolibData", "animatable.visualRole()")
    require("P27 renderer registration", read("src/client/java/com/crpg/ebb/client/npc/ModEntityRenderers.java"), "EbbNpcRenderer::new", "role-aware")

    routine_definition = read("src/main/java/com/crpg/ebb/routine/NpcRoutineDefinition.java")
    require(
        "P27 routine validation",
        routine_definition,
        "ALLOWED_ACTIONS",
        "ALLOWED_ANIMATIONS",
        "ALLOWED_POSES",
        "walk_path",
        "talk",
        "nervous_idle",
        "path must contain at least two waypoints",
    )
    controller = read("src/main/java/com/crpg/ebb/routine/NpcRoutineController.java")
    require(
        "P27 routine controller focus/debug",
        controller,
        "activeConversationSessionForEntity",
        "beginConversationFocus",
        "endConversationFocus",
        "conversationAnimationFor",
        "setRoutineDebug",
        "targetSummary",
        "requiresLineOfSight",
    )
    commands = read("src/main/java/com/crpg/ebb/registry/ModCommands.java")
    require("P27 routine inspect command", commands, "visual_role=", "routineDebugSummary", "current_step@time", "setVisualRole")
    docs = read("docs/json_authoring_guide.md")
    require("P27 authoring docs", docs, "Allowed routine actions", "Allowed routine animations", "Allowed routine poses", "role-specific temporary skins", "conversation-focus animations")
    status = read("docs/current_status.md")
    require("P27 status docs", status, "P27 NPC art, animation, and routine production", "role-specific visual skins", "conversation animation hooks")
    test = read("src/test/java/com/crpg/ebb/DeepResearchDataTest.java")
    require("P27 JUnit coverage", test, "p27RoutineValidationAndConversationAnimationNamesAreExplicit", "ALLOWED_ANIMATIONS", "moonwalk", "impossible_pose", "spin_forever")


def audit_p28_investigation_conflict_expansion() -> None:
    conflict_definition = read("src/main/java/com/crpg/ebb/conflict/ConflictDefinition.java")
    require(
        "P28 conflict definition contract",
        conflict_definition,
        "REQUIRED_PHASES",
        "leverageClues",
        "ConflictOutcomeDefinition",
        "phaseDescriptions",
        "parseOutcomes",
        "setup",
        "pressure",
        "turn",
        "consequence",
        "resolution",
    )
    outcome_definition = read("src/main/java/com/crpg/ebb/conflict/ConflictOutcomeDefinition.java")
    require("P28 conflict outcome model", outcome_definition, "failureForward", "isFailureForwardKind", "isMessyKind", "isNonViolentKind")
    conflict_service = read("src/main/java/com/crpg/ebb/conflict/ConflictService.java")
    require(
        "P28 conflict service status",
        conflict_service,
        "setConflictPhase",
        "applyOutcome",
        "statusLine",
        "knownLeverageClues",
        "stress=",
        "resolve=",
        "leverage=",
    )
    saved_data = read("src/main/java/com/crpg/ebb/state/NarrativeSavedData.java")
    require("P28 conflict phase persistence", saved_data, "getConflictPhase", "setConflictPhase", "conflict_phase")
    effect = read("src/main/java/com/crpg/ebb/dialogue/DialogueEffect.java")
    require("P28 conflict outcome effect", effect, "APPLY_CONFLICT_OUTCOME", "apply_conflict_outcome", "outcome_id", "ConflictService.applyOutcome")
    screen = read("src/client/java/com/crpg/ebb/client/gui/dialogue/DialogueScreen.java")
    require("P28 conflict status UI", screen, "conflict_status:", "冲突状态", "冲突结果", "conflict_outcome#")
    dev = read("src/main/java/com/crpg/ebb/dev/DevSnapshotService.java")
    require("P28 dev conflict browser", dev, "Conflict phase/status catalog", "phaseDescriptions", "leverage_clues", "ConflictService.statusLine")

    conflict = load_json(ROOT / "src/main/resources/data/ebb/conflicts/demo/hallway_confrontation.json")
    phases = [
        item.get("id") if isinstance(item, dict) else item
        for item in conflict.get("phases", [])
    ]
    for phase in ["setup", "pressure", "turn", "consequence", "resolution"]:
        if phase not in phases:
            raise AssertionError(f"P28 hallway conflict missing phase {phase}")
    if len(conflict.get("leverage_clues", [])) < 4:
        raise AssertionError("P28 hallway conflict should expose multiple leverage clues")
    outcomes = conflict.get("outcomes", [])
    if sum(1 for outcome in outcomes if isinstance(outcome, dict) and outcome.get("failure_forward")) < 2:
        raise AssertionError("P28 hallway conflict needs at least two failure-forward outcomes")
    kinds = {outcome.get("kind") for outcome in outcomes if isinstance(outcome, dict)}
    if "nonviolent" not in kinds or "messy" not in kinds:
        raise AssertionError("P28 hallway conflict needs nonviolent and messy outcomes")

    guard = read("src/main/resources/data/ebb/dialogues/demo/guard_intro.json")
    require(
        "P28 guard conflict dialogue",
        guard,
        "apply_conflict_outcome",
        "quiet_resolve",
        "messy_resolve",
        "public_pressure_fail",
        "guard_standoff_fail",
        "tenant_false_window",
        "bruised_shoulder",
        "clue_found",
    )
    docs = read("docs/json_authoring_guide.md")
    require("P28 authoring docs", docs, "leverage_clues", "apply_conflict_outcome", "failure-forward outcomes", "setup", "pressure", "turn", "consequence", "resolution")
    schema = read("docs/schemas/ebb.conflict.schema.json")
    require("P28 conflict schema", schema, "leverage_clues", "outcomes", "failure_forward", "nonviolent", "messy")
    validator = read("scripts/p24_authoring_validation.py")
    require("P28 authoring validator conflict outcome", validator, "apply_conflict_outcome", "conflict outcome", "outcome_id")
    test = read("src/test/java/com/crpg/ebb/DeepResearchDataTest.java")
    require("P28 JUnit coverage", test, "p28ConflictPhasesLeverageOutcomesPersistAndFailForward", "resolved_nonviolent", "failed_forward_public", "getConflictPhase")
    smoke = read("scripts/smoke/DeepResearchSmoke.java")
    require("P28 smoke coverage", smoke, "P28 conflict phases", "P28 conflict status", "resolved_nonviolent", "failed_forward_public")


def audit_p29_save_multiplayer_permissions_hardening() -> None:
    saved_data = read("src/main/java/com/crpg/ebb/state/NarrativeSavedData.java")
    require(
        "P29 saved-data migration",
        saved_data,
        "CURRENT_SCHEMA_VERSION = 3",
        "LEGACY_SCHEMA_VERSION",
        "migrateLoadedData",
        "migrateConflictPhasesFromLegacyStates",
        "inferConflictPhaseFromLegacyState",
        "conflict_phase",
    )
    dialogue_service = read("src/main/java/com/crpg/ebb/dialogue/DialogueService.java")
    require(
        "P29 dialogue packet/session hardening",
        dialogue_service,
        "validateChoicePacket",
        "ChoicePacketValidation",
        "session_player_mismatch",
        "entity_dialogue_busy",
        "entityReservedByAnotherPlayer",
        "securityEventSnapshot",
        "action_target_invalid",
        "ServerPlayConnectionEvents.DISCONNECT",
        "ServerPlayerEvents.AFTER_RESPAWN",
        "ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL",
    )
    sync = read("src/main/java/com/crpg/ebb/network/sync/InteractionSyncService.java")
    require(
        "P29 missing-client diagnostics",
        sync,
        "MISSING_CLIENT_MOD_PAYLOADS",
        "missingClientModDiagnostics",
        "missingClientModDiagnosticLines",
        "recordMissingClientPayload",
        "player may be missing the Ebb client mod",
    )
    dev = read("src/main/java/com/crpg/ebb/dev/DevSnapshotService.java")
    require("P29 dev diagnostics", dev, "P29 multiplayer/packet diagnostics", "dialogue_security_events", "missing_client_mod_diagnostics")
    commands = read("src/main/java/com/crpg/ebb/registry/ModCommands.java")
    permission_guards = read("src/main/java/com/crpg/ebb/registry/commands/EbbCommandPermissionGuards.java")
    for permission in [
        "command.dev",
        "command.dialogue",
        "command.routine",
        "command.export",
        "command.summon_npc",
        "command.attributes.grant",
        "command.attributes.set",
        "command.attributes.reset",
    ]:
        require("P29 OP command permission", permission_guards, f'group("{permission}")')
    require("P29 self-inspection commands", commands, 'Commands.literal("vars")', 'Commands.literal("journal")', 'Commands.literal("quest")', 'Commands.literal("spend")')
    docs = read("docs/json_authoring_guide.md")
    require("P29 docs", docs, "P29 save/load, multiplayer, and permission hardening", "entity_dialogue_busy", "missing-client-mod diagnostics", "CURRENT_SCHEMA_VERSION")
    test = read("src/test/java/com/crpg/ebb/DeepResearchDataTest.java")
    require(
        "P29 JUnit coverage",
        test,
        "p29SavedDataMigrationAddsConflictPhaseAndPreservesLegacyState",
        "p29DialogueSessionPreflightRejectsSpoofedStaleAndContendedSessions",
        "p29CommandPermissionSurfaceKeepsAdminAndSelfInspectionSplit",
        "session_player_mismatch",
        "entityReservedByAnotherPlayer",
    )
    smoke = read("scripts/smoke/DeepResearchSmoke.java")
    require("P29 smoke coverage", smoke, "P29 save migration", "P29 spoofed session packet rejected", "P29 same-NPC contention detected")


def audit_p30_vertical_slice_content_expansion() -> None:
    def count(category: str) -> int:
        return len(json_files(f"src/main/resources/data/ebb/{category}"))

    if count("interactions/block_groups") < 12:
        raise AssertionError("P30 requires at least 12 block groups")
    if count("interactions/entity_bindings") < 14:
        raise AssertionError("P30 requires at least 14 entity bindings including cook/courier tag/name fallbacks")
    if count("npc_routines") < 7:
        raise AssertionError("P30 requires at least 7 NPC routines")
    if count("quest_branches") < 12:
        raise AssertionError("P30 requires at least 12 quest branches total")
    if count("feats") < 12:
        raise AssertionError("P30 requires at least 12 feats")
    if count("journal_entries") < 20:
        raise AssertionError("P30 requires at least 20 journal entries")
    if count("clues") < 20:
        raise AssertionError("P30 requires at least 20 clues")
    if count("conflicts") < 3:
        raise AssertionError("P30 requires at least 3 conflicts")

    quest_files = [load_json(path) for path in json_files("src/main/resources/data/ebb/quest_branches")]
    majors = [quest for quest in quest_files if quest.get("kind") == "major"]
    minors = [quest for quest in quest_files if quest.get("kind", "minor") == "minor"]
    if len(majors) < 4 or len(minors) < 8:
        raise AssertionError(f"P30 branch kind count too small: major={len(majors)} minor={len(minors)}")
    chime_line_count = sum(len(load_json(path).get("lines", [])) for path in json_files("src/main/resources/data/ebb/chimes"))
    if chime_line_count < 40:
        raise AssertionError(f"P30 requires at least 40 chime lines, got {chime_line_count}")

    for path in [
        "src/main/resources/data/ebb/dialogues/demo/cook_intro.json",
        "src/main/resources/data/ebb/dialogues/demo/courier_intro.json",
        "src/main/resources/data/ebb/interactions/block_groups/demo/kitchen_manifest.json",
        "src/main/resources/data/ebb/interactions/block_groups/demo/stable_mud.json",
        "src/main/resources/data/ebb/conflicts/demo/kitchen_bargain.json",
        "src/main/resources/data/ebb/conflicts/demo/courtyard_standoff.json",
        "src/main/resources/assets/ebb/textures/entity/npc_cook.png",
        "src/main/resources/assets/ebb/textures/entity/npc_courier.png",
    ]:
        exists(path)
    back_door = read("src/main/resources/data/ebb/dialogues/demo/back_door_dialogue.json")
    require("P30 ending placeholders", back_door, "trade_end", "mercy_end", "ending_placeholder")
    docs = read("docs/current_status.md") + read("docs/json_authoring_guide.md")
    require("P30 docs/status", docs, "P30 Vertical slice content expansion", "12 block-group", "40 Chime lines", "3 set-piece conflicts")
    test = read("src/test/java/com/crpg/ebb/DeepResearchDataTest.java")
    require("P30 JUnit coverage", test, "p30VerticalSliceContentExpansionMeetsMinimumCounts", "ChimeRegistry.orderedDefinitions", "trade_end", "mercy_end")
    smoke = read("scripts/smoke/DeepResearchSmoke.java")
    require("P30 smoke coverage", smoke, "P30 vertical slice exposes at least 12", "P30 chime count and line count meet target", "P30 conflict count meets target")



def audit_p31_release_packaging_player_documentation() -> None:
    for path in [
        "docs/installation.md",
        "docs/release_metadata_draft.md",
        "docs/story_pack_tutorial.md",
        "CHANGELOG.md",
        "LICENSE.md",
    ]:
        exists(path)

    install = read("docs/installation.md")
    require(
        "P31 installation docs",
        install,
        "Client install",
        "Dedicated server install",
        "Fabric Loader",
        "0.19.2",
        "Fabric API",
        "0.150.0+26.1.2",
        "GeckoLib",
        "5.5.1",
        "Java",
        "25",
        "26.1.2-Fabric-Ebb-Test",
        "scripts/configure_pcl_test_client.sh",
        "missing-client diagnostics",
    )

    metadata = read("docs/release_metadata_draft.md")
    require(
        "P31 release metadata draft",
        metadata,
        "Modrinth draft fields",
        "CurseForge draft fields",
        "client_side: required",
        "server_side: required",
        "release_type: alpha",
        "required_dependencies",
        "Fabric API 0.150.0+26.1.2",
        "GeckoLib 5.5.1",
    )

    tutorial = read("docs/story_pack_tutorial.md")
    require(
        "P31 story pack tutorial",
        tutorial,
        "block-group investigation point",
        "data/<namespace>",
        "rusty_lock_dialogue",
        "reveal_clue",
        "failure_effects",
        "scripts/p24_authoring_validation.py",
        "scripts/gradle-local.sh --no-daemon validateEbbData",
        "server-authoritative",
        "canonical attributes",
    )

    changelog = read("CHANGELOG.md")
    require(
        "P31 changelog",
        changelog,
        "0.1.0-dev",
        "Added",
        "Fixed",
        "Known alpha limitations",
        "P30 tavern vertical slice",
    )

    license_text = read("LICENSE.md")
    require(
        "P31 license clarity",
        license_text,
        "Code and scripts",
        "MIT License",
        "Bundled story data",
        "CC BY-NC-SA 4.0",
        "Placeholder visual/audio assets",
        "Third-party and platform content",
    )

    readme = read("README.md")
    require(
        "P31 README links",
        readme,
        "docs/installation.md",
        "docs/story_pack_tutorial.md",
        "docs/release_metadata_draft.md",
        "CHANGELOG.md",
        "LICENSE.md",
        "six role NPCs",
        "12 interactable block groups",
    )

    status = read("docs/current_status.md")
    require("P31 current status", status, "P31 Release packaging", "docs/installation.md", "docs/story_pack_tutorial.md", "LICENSE.md")


def audit_p32_k_menu_and_live_dialogue_background() -> None:
    menu = read("src/client/java/com/crpg/ebb/client/gui/menu/EbbMenuScreen.java")
    require(
        "P32 K menu screen",
        menu,
        "screen.ebb.menu.title",
        "sendCommandAndClose(\"ebb journal\")",
        "sendCommandAndClose(\"ebb quest\")",
        "sendCommandAndClose(\"ebb attributes\")",
        "sendCommandAndClose(\"ebb vars\")",
        "ClientDialogueSettings.fontScaleLabel()",
        "ClientDialogueSettings.textSpeedLabel()",
        "GLFW.GLFW_KEY_K",
        "isPauseScreen()",
    )
    if "extractTransparentBackground" in menu:
        raise AssertionError("P32 EbbMenuScreen should not render a full-screen dark background")

    key_mappings = read("src/client/java/com/crpg/ebb/client/input/ClientKeyMappings.java")
    require("P32 K key binding", key_mappings, "key.ebb.menu", "GLFW.GLFW_KEY_K", "new EbbMenuScreen()", "client.screen instanceof EbbMenuScreen")

    dialogue_screen = read("src/client/java/com/crpg/ebb/client/gui/dialogue/DialogueScreen.java")
    if "extractTransparentBackground(graphics)" in dialogue_screen:
        raise AssertionError("P32 DialogueScreen must leave the player-view background visible")
    require("P32 transparent dialogue screen", dialogue_screen, "PANEL_COLOR", "isPauseScreen()", "super.extractRenderState")

    for lang in ["src/main/resources/assets/ebb/lang/en_us.json", "src/main/resources/assets/ebb/lang/zh_cn.json"]:
        text = read(lang)
        require("P32 menu translations", text, "key.ebb.menu", "screen.ebb.menu.title", "screen.ebb.menu.journal", "screen.ebb.menu.status.ready")

    runner = read("scripts/gui_e2e_run.py")
    require("P32 GUI automation", runner, "gui_k_menu", "k_menu_open.png", "_assert_live_world_background", "top_band_luminance")

    status = read("docs/current_status.md")
    require("P32 current status", status, "K-key Ebb menu", "dialogue screen no longer darkens")


def audit_p33_codebase_review_remediation() -> None:
    commands = read("src/main/java/com/crpg/ebb/registry/ModCommands.java")
    require(
        "P33 command permission split",
        commands,
        "EbbCommandPermissionGuards.dialogue()",
        ".then(Commands.argument(\"player\", EntityArgument.player())\n                                        .requires(EbbCommandPermissionGuards.dialogue())",
    )
    permission_guards = read("src/main/java/com/crpg/ebb/registry/commands/EbbCommandPermissionGuards.java")
    require(
        "P33 command permission helper",
        permission_guards,
        "Central permission surface",
        "command.dialogue",
        "command.attributes.reset",
    )

    condition = read("src/main/java/com/crpg/ebb/dialogue/DialogueCondition.java")
    require("P33 active feat condition", condition, "FEAT_ACTIVE", "HAS_ACTIVE_FEAT", "isFeatActive", "active ==")

    check = read("src/main/java/com/crpg/ebb/dialogue/DialogueCheck.java")
    require("P33 disadvantage check parser", check, "boolean disadvantage", '"disadvantage"', "disadvantage ?")

    dialogue_service = read("src/main/java/com/crpg/ebb/dialogue/DialogueService.java")
    require(
        "P33 retry/roll breakdown runtime",
        dialogue_service,
        "check.disadvantage()",
        "normal_cancelled",
        "secondRoll",
        "baseAttribute",
        "retryLockFlag",
        "retryUnlockFlags",
        "consumeRetryUnlockOrDeny",
        "check_locked:",
    )

    roll = read("src/main/java/com/crpg/ebb/network/dialogue/RollResultPayload.java")
    require(
        "P33 roll payload breakdown",
        roll,
        "baseAttribute",
        "staticModifier",
        "featModifier",
        "clueModifier",
        "firstRoll",
        "secondRoll",
        "rollMode",
        "modifierBreakdown",
    )
    choice = read("src/main/java/com/crpg/ebb/dialogue/DialogueChoice.java")
    require("P33 choice authoring semantics", choice, "pre_effects", "end_on_success", "endOnSuccess")
    definition = read("src/main/java/com/crpg/ebb/dialogue/DialogueDefinition.java")
    require("P33 checked-choice lint", definition, "successful checks should not silently close", "branch-specific state before the roll")

    raycast = read("src/main/java/com/crpg/ebb/interaction/InteractionRaycastPolicy.java")
    require("P33 centralized raycast policy", raycast, "blockModeForPrediction", "blockModeForAuthority", "blockModeForDevInspect", "ClipContext.Block.COLLIDER")
    detector = read("src/client/java/com/crpg/ebb/client/interaction/ClientTargetDetector.java")
    if "ClipContext.Block.OUTLINE" in detector or "ClipContext.Block.OUTLINE" in commands:
        raise AssertionError("P33 client/dev target detection must not use OUTLINE raycast mode")
    require("P33 detector uses shared policy", detector, "InteractionRaycastPolicy.blockModeForPrediction()", "InteractionRaycastPolicy.fluidMode()")
    service = read("src/main/java/com/crpg/ebb/interaction/InteractionService.java")
    require("P33 server LOS uses nearest member", service, "nearestBlockCenter", "hasClearRayToBlockGroupPoint", "InteractionRaycastPolicy.blockModeForAuthority()")
    block_group = read("src/main/java/com/crpg/ebb/interaction/BlockGroupDefinition.java")
    require("P33 block group nearest center", block_group, "nearestBlockCenter", "Vec3::atCenterOf")
    index = read("src/main/java/com/crpg/ebb/interaction/BlockGroupIndex.java")
    require("P33 duplicate block-group invalidation", index, "findDuplicateMembership", "duplicate block membership is not allowed", "continue;")

    routine = read("src/main/java/com/crpg/ebb/routine/NpcRoutineDefinition.java")
    require("P33 routine hardening", routine, "at least one valid step", "hasOverlappingSteps", "teleport_distance must be > 0")
    npc = read("src/main/java/com/crpg/ebb/npc/EbbNpcEntity.java")
    require("P33 role inference for expanded NPCs", npc, "contains(\"cook\")", "contains(\"courier\")", "dialogue.talk", "dialogue.nervous_idle")
    effect = read("src/main/java/com/crpg/ebb/dialogue/DialogueEffect.java")
    require("P33 item placeholder/branch pre-effect docs hooks", effect, "GIVE_ITEM_PLACEHOLDER", "TAKE_ITEM_PLACEHOLDER", "isBranchSpecificPreEffectRisk")

    docs = read("docs/json_authoring_guide.md")
    require(
        "P33 authoring docs",
        docs,
        "has_active_feat",
        "disadvantage",
        "pre_effects",
        "end_on_success",
        "retryable",
        "item_placeholder_give",
        "duplicate block membership",
    )
    status = read("docs/current_status.md")
    require("P33 current status", status, "Phase 33", "codebase review remediation", "retryable check locks")
    test = read("src/test/java/com/crpg/ebb/DeepResearchDataTest.java")
    require(
        "P33 JUnit coverage",
        test,
        "p33ActiveFeatDisadvantageAndCheckedChoiceSemanticsAreExplicit",
        "p33CheckedChoiceEndOnSuccessPreEffectsAndRetryLocksAreLinted",
        "p33RaycastBlockGroupAndRoutineHardeningRegressions",
    )



def audit_p34_llm_fake_provider_foundation() -> None:
    for path in [
        "src/main/java/com/crpg/ebb/llm/LlmConfig.java",
        "src/main/java/com/crpg/ebb/llm/LlmGatewayClient.java",
        "src/main/java/com/crpg/ebb/llm/FakeLlmGatewayClient.java",
        "src/main/java/com/crpg/ebb/llm/DisabledLlmGatewayClient.java",
        "src/main/java/com/crpg/ebb/llm/LlmChatService.java",
        "src/client/java/com/crpg/ebb/client/gui/llm/NpcChatScreen.java",
        "src/main/java/com/crpg/ebb/network/llm/LlmChatOpenedPayload.java",
        "src/main/java/com/crpg/ebb/network/llm/LlmChatMessagePayload.java",
        "src/main/java/com/crpg/ebb/network/llm/LlmChatChunkPayload.java",
        "src/main/java/com/crpg/ebb/network/llm/LlmChatOptionsPayload.java",
        "src/main/java/com/crpg/ebb/network/llm/LlmChatClosePayload.java",
        "src/main/java/com/crpg/ebb/network/llm/LlmChatCancelPayload.java",
        "src/main/java/com/crpg/ebb/network/llm/LlmChatErrorPayload.java",
    ]:
        exists(path)

    config = read("src/main/java/com/crpg/ebb/llm/LlmConfig.java")
    require("P34 LLM config", config, "SERVER_CONFIG_PATH", "enabled", "mode", "networkAccessAllowed", "DEFAULT_FAKE_REPLY", "toSafeJson")
    require("P34 LLM config defaults", config, "disabled()", "DEFAULT_MAX_INPUT_CHARS", "DEFAULT_SESSION_TIMEOUT_TICKS", "fakeForTesting")
    if any(secret in config.lower() for secret in ["api_key", "apikey", "authorization", "bearer"]):
        raise AssertionError("P34 LlmConfig must not read or expose API key/Authorization/Bearer secrets")

    fake = read("src/main/java/com/crpg/ebb/llm/FakeLlmGatewayClient.java")
    require("P34 fake provider", fake, "FAKE_NPC_REPLY", "CompletableFuture.completedFuture", "fake_reply", "fake:profile")
    if "java.net" in fake or "HttpClient" in fake or "openai" in fake.lower():
        raise AssertionError("P34 fake provider must not access network or OpenAI")

    disabled = read("src/main/java/com/crpg/ebb/llm/DisabledLlmGatewayClient.java")
    require("P34 disabled provider", disabled, "llm_disabled", "providerName()", "disabled")

    service = read("src/main/java/com/crpg/ebb/llm/LlmChatService.java")
    require("P34 chat service", service, "openFromDialogue", "handleMessage", "closeExpiredSessionsForTesting", "validateMessagePacket", "llm_disabled", "llm_session_timeout", "InteractionService.validateSessionTarget", "nonce")

    dialogue_choice = read("src/main/java/com/crpg/ebb/dialogue/DialogueChoice.java")
    require("P34 dialogue llm settings", dialogue_choice, "LlmChoiceSettings", "llmSettings", "expected dialogue/action/thought/llm_chat/free_chat")
    choice_type = read("src/main/java/com/crpg/ebb/dialogue/ChoiceType.java")
    require("P34 choice type", choice_type, "LLM_CHAT", "FREE_CHAT", "serializedName")
    dialogue_service = read("src/main/java/com/crpg/ebb/dialogue/DialogueService.java")
    require("P34 dialogue integration", dialogue_service, "ChoiceType.LLM_CHAT", "LlmChatService.openFromDialogue", "remove(session)")

    packets = read("src/main/java/com/crpg/ebb/network/ModPackets.java")
    require("P34 packet registration", packets, "LlmChatOpenedPayload.TYPE", "LlmChatMessagePayload.TYPE", "LlmChatChunkPayload.TYPE", "LlmChatOptionsPayload.TYPE", "LlmChatClosePayload.TYPE", "LlmChatCancelPayload.TYPE", "LlmChatErrorPayload.TYPE", "LlmChatService.handleMessage", "LlmChatService.closeFromClient")

    client_net = read("src/client/java/com/crpg/ebb/client/network/ClientInteractionNetworking.java")
    require("P34 client networking", client_net, "NpcChatScreen", "sendLlmChatMessage", "sendLlmChatCancel", "applyLlmChunk", "applyLlmOptions", "applyLlmError", "closeLlmChat")
    screen = read("src/client/java/com/crpg/ebb/client/gui/llm/NpcChatScreen.java")
    require("P34 NpcChatScreen", screen, "EditBox", "sendCurrentInput", "suggestedOptions", "appendChunk", "applyError", "isPauseScreen()")

    commands = read("src/main/java/com/crpg/ebb/registry/ModCommands.java")
    require("P34 llm command", commands, "Commands.literal(\"llm\")", "sendLlmStatus", "reloadLlmConfig", "LlmChatService.statusLine")

    innkeeper = read("src/main/resources/data/ebb/dialogues/demo/innkeeper_intro.json")
    require("P34 demo free chat", innkeeper, '"type": "llm_chat"', '"topic_hint"', '"allow_memory_write"')
    schema = read("docs/schemas/ebb.dialogue.schema.json")
    require("P34 schema", schema, "llm_chat", "free_chat", "llm_choice", "topic_hint", "return_node")
    docs = read("docs/json_authoring_guide.md")
    require("P34 authoring docs", docs, "P34 LLM / Free Chat Foundation", "/ebb llm status", "llm_disabled", "FAKE_NPC_REPLY")
    status = read("docs/current_status.md")
    require("P34 current status", status, "Phase 34 LLM fake-provider foundation", "LlmConfig", "NpcChatScreen", "llm_disabled")

    test = read("src/test/java/com/crpg/ebb/DeepResearchDataTest.java")
    require("P34 JUnit coverage", test, "p34LlmConfigFakeProviderAndChoiceParsingAreDeterministic", "p34LlmDisabledModeTimeoutAndCommandsAreAuditable", "llm_disabled")
    gametest = read("src/main/java/com/crpg/ebb/test/EbbGameTests.java")
    require("P34 GameTest coverage", gametest, "llmFakeDisabledAndTimeoutFoundationIsDeterministic", "FAKE_NPC_REPLY", "llm_disabled", "closeExpiredSessionsForTesting")

    for rel in ["src/main/java/com/crpg/ebb/llm", "src/main/java/com/crpg/ebb/network/llm"]:
        for path in (ROOT / rel).rglob("*.java"):
            text = path.read_text(encoding="utf-8")
            lowered = text.lower()
            if any(secret in lowered for secret in ["api_key", "apikey", "authorization: bearer", "sk-"]):
                raise AssertionError(f"P34 secret-like literal in {path.relative_to(ROOT)}")
            if path.name in {"LlmConfig.java", "FakeLlmGatewayClient.java", "DisabledLlmGatewayClient.java"} and ("HttpClient" in text or "java.net.http" in text):
                raise AssertionError(f"P34 disabled/fake config/provider must not use HTTP client: {path.relative_to(ROOT)}")


def audit_p35_npc_profile_tier_promotion_data_layer() -> None:
    for path in [
        "src/main/java/com/crpg/ebb/npc/profile/NpcTier.java",
        "src/main/java/com/crpg/ebb/npc/profile/NpcProfileDefinition.java",
        "src/main/java/com/crpg/ebb/npc/profile/NpcProfileRegistry.java",
        "src/main/java/com/crpg/ebb/npc/profile/NpcPromotionService.java",
        "docs/schemas/ebb.npc_profile.schema.json",
        "src/main/resources/data/ebb/dialogues/llm/minor_intro.json",
        "src/main/resources/data/ebb/interactions/entity_bindings/llm/minor_villager.json",
    ]:
        exists(path)

    tier = read("src/main/java/com/crpg/ebb/npc/profile/NpcTier.java")
    require("P35 NpcTier", tier, "MAJOR_SCRIPTED", "MINOR_GENERATABLE", "MAJOR_PROMOTED", "STATIC_NON_LLM", "DISABLED", "serializedName")
    profile_def = read("src/main/java/com/crpg/ebb/npc/profile/NpcProfileDefinition.java")
    require("P35 NpcProfileDefinition", profile_def, "LlmSettings", "CharacterProfile", "Stance", "Knowledge", "Promotion", "forbidden_to_reveal_until")
    profile_registry = read("src/main/java/com/crpg/ebb/npc/profile/NpcProfileRegistry.java")
    require("P35 NpcProfileRegistry", profile_registry, "rebuild", "byEntityBinding", "orderedDefinitions", "summaryLine", "debugLines")
    promotion = read("src/main/java/com/crpg/ebb/npc/profile/NpcPromotionService.java")
    require("P35 NpcPromotionService", promotion, "MINOR_NPC_TAG", "ensurePromotedIfMinor", "ensurePromotedProfile", "MAJOR_PROMOTED", "promotedProfileId", "generatePromotedProfileJson")

    registries = read("src/main/java/com/crpg/ebb/data/NarrativeDataRegistries.java")
    require("P35 data registry", registries, "NPC_PROFILES", "npc_profiles", "NpcProfileRegistry.rebuild", "NpcProfileRegistry.summaryLine")
    saved = read("src/main/java/com/crpg/ebb/state/NarrativeSavedData.java")
    require("P35 promoted persistence", saved, "CURRENT_SCHEMA_VERSION = 3", "promoted_npc_profiles", "putPromotedNpcProfile", "promotedNpcProfile", "promotedNpcProfileDebugLines")
    binding = read("src/main/java/com/crpg/ebb/interaction/entity/EntityBindingDefinition.java")
    require("P35 entity binding schema", binding, "npc_tier", "npc_profile", "promote_on_first_chat", "profile_seed_archetypes", "NpcTier.MINOR_GENERATABLE")
    sync = read("src/main/java/com/crpg/ebb/network/sync/EntityBindingSyncPayload.java")
    require("P35 binding sync", sync, "npcProfileId", "npcTier", "promoteOnFirstChat", "profileSeedArchetypes")
    llm = read("src/main/java/com/crpg/ebb/llm/LlmChatService.java")
    require("P35 LLM promotion hook", llm, "NpcPromotionService.ensurePromotedIfMinor", "promoted_major")
    commands = read("src/main/java/com/crpg/ebb/registry/ModCommands.java")
    require("P35 NPC commands", commands, "Commands.literal(\"npc\")", "showNpcProfileTarget", "showNpcProfileByKey", "minorizeEntity", "promoteEntity", "resetPromotedProfile")

    roles = ["innkeeper", "witness", "tenant", "guard", "cook", "courier"]
    for role in roles:
        path = f"src/main/resources/data/ebb/npc_profiles/demo/{role}.json"
        exists(path)
        text = read(path)
        require(f"P35 {role} profile", text, '"tier": "major_scripted"', '"display_name"', '"character"', '"stance"', '"knowledge"', '"allow_memory_write"')
        binding_path = f"src/main/resources/data/ebb/interactions/entity_bindings/demo/{role}_ebb_npc.json"
        require(f"P35 {role} binding metadata", read(binding_path), '"npc_tier": "major_scripted"', f'"npc_profile": "ebb:demo/{role}"')
    require("P35 minor binding", read("src/main/resources/data/ebb/interactions/entity_bindings/llm/minor_villager.json"), '"npc_tier": "minor_generatable"', '"tag": "ebb.npc.minor"', '"promote_on_first_chat": true')
    require("P35 minor dialogue", read("src/main/resources/data/ebb/dialogues/llm/minor_intro.json"), '"type": "llm_chat"', '"return_node": "start"')
    schema = read("docs/schemas/ebb.npc_profile.schema.json")
    require("P35 npc profile schema", schema, "major_scripted", "major_promoted", "character", "knowledge", "promotion")
    docs = read("docs/json_authoring_guide.md")
    require("P35 authoring docs", docs, "P35 NPC Profiles", "/ebb npc profile target", "minor_generatable", "promoted_npc_profiles")
    status = read("docs/current_status.md")
    require("P35 current status", status, "Phase 35 NPC Profile / Tier / Promotion", "promoted_npc_profiles", "NpcProfileRegistry")
    test = read("src/test/java/com/crpg/ebb/DeepResearchDataTest.java")
    require("P35 JUnit coverage", test, "p35NpcProfilesLoadAndResolveByBinding", "p35PromotedProfilesPersistThroughSavedDataCodec", "p35NpcCommandSurfaceIsRegistered")
    gametest = read("src/main/java/com/crpg/ebb/test/EbbGameTests.java")
    require("P35 GameTest coverage", gametest, "npcProfileRegistryLoadsScriptedProfilesAndPromotesMinor", "ensurePromotedProfile", "promoted_major")



def audit_p36_gateway_auth_minimal_service() -> None:
    for path in [
        "ebb-llm-gateway/build.gradle.kts",
        "ebb-llm-gateway/settings.gradle.kts",
        "ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/GatewayMain.java",
        "ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/GatewayServer.java",
        "ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/GatewayConfig.java",
        "ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/auth/AuthProvider.java",
        "ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/auth/DeviceAuthService.java",
        "ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/auth/DevLocalAuthProvider.java",
        "ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/auth/OidcAuthProvider.java",
        "ebb-llm-gateway/src/test/java/com/crpg/ebb/gateway/GatewaySmoke.java",
        "scripts/p36_gateway_smoke.sh",
        "src/main/java/com/crpg/ebb/llm/auth/LlmAuthService.java",
        "src/main/java/com/crpg/ebb/llm/auth/HttpLlmGatewayAuthClient.java",
        "src/main/java/com/crpg/ebb/llm/auth/DevLocalLlmAuthClient.java",
        "src/main/java/com/crpg/ebb/llm/auth/LlmAuthToken.java",
        "src/main/java/com/crpg/ebb/network/llm/LlmAuthStartPayload.java",
        "src/main/java/com/crpg/ebb/network/llm/LlmAuthStatusRequestPayload.java",
        "src/main/java/com/crpg/ebb/network/llm/LlmAuthUrlPayload.java",
        "src/main/java/com/crpg/ebb/network/llm/LlmAuthStatusPayload.java",
        "src/main/java/com/crpg/ebb/network/llm/NpcProfileSyncPayload.java",
        "src/main/java/com/crpg/ebb/network/llm/MemoryDebugSnapshotPayload.java",
    ]:
        exists(path)

    gateway = read("ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/GatewayServer.java")
    require("P36 gateway endpoints", gateway, "/v1/health", "/v1/auth/device/start", "/v1/auth/device/status", "/v1/auth/logout", "/v1/player/quota", "/v1/npc/profile/ensure", "/v1/chat/start", "/v1/chat/cancel", "/v1/chat/session", "/v1/knowledge/update", "DeviceAuthService")
    dev_provider = read("ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/auth/DevLocalAuthProvider.java")
    require("P36 dev local auth provider", dev_provider, "dev_local", "llm:chat", "memory:read_self", "memory:write_self")
    oidc_provider = read("ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/auth/OidcAuthProvider.java")
    require("P36 production OIDC provider", oidc_provider, "DEVICE_CODE_GRANT", "EBB_OIDC_DEVICE_AUTH_URL", "authorization_pending", "keycloak", "auth0", "stytch")
    config = read("ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/GatewayConfig.java")
    require("P36 gateway config", config, "EBB_GATEWAY_AUTH_PROVIDER", "keycloak", "auth0", "stytch", "createAuthProvider")
    smoke = read("ebb-llm-gateway/src/test/java/com/crpg/ebb/gateway/GatewaySmoke.java")
    require("P36 gateway smoke", smoke, "P36 gateway smoke passed", "/v1/health", "/v1/player/quota", "/v1/npc/profile/ensure", "/v1/chat/start", "/v1/chat/cancel", "opaque_player_token", "/v1/auth/logout")

    llm_config = read("src/main/java/com/crpg/ebb/llm/LlmConfig.java")
    require("P36 LLM config auth fields", llm_config, "gateway_base_url", "gateway_timeout_ms", "require_player_auth", "DEFAULT_REQUIRE_PLAYER_AUTH", "toSafeJson")
    auth_service = read("src/main/java/com/crpg/ebb/llm/auth/LlmAuthService.java")
    require("P36 LLM auth service", auth_service, "requiresAuth", "chatGateStatus", "server-only", "auth_required", "safeStatusLine", "token_value", "redacted")
    http_client = read("src/main/java/com/crpg/ebb/llm/auth/HttpLlmGatewayAuthClient.java")
    require("P36 Minecraft gateway auth client", http_client, "/v1/auth/device/start", "/v1/auth/device/status", "/v1/auth/logout", "opaque_player_token")
    token = read("src/main/java/com/crpg/ebb/llm/auth/LlmAuthToken.java")
    require("P36 token redaction", token, "redactedSummary", "SHA-256", "token=redacted")

    llm_service = read("src/main/java/com/crpg/ebb/llm/LlmChatService.java")
    require("P36 chat auth gate", llm_service, "LlmAuthService.requiresAuth", "auth_required", "llm_auth_required", "llm_auth_required_message")
    commands = read("src/main/java/com/crpg/ebb/registry/ModCommands.java")
    require("P36 llm auth commands", commands, "Commands.literal(\"auth\")", "Commands.literal(\"logout\")", "Commands.literal(\"quota\")", "Commands.literal(\"consent\")", "Commands.literal(\"auth_debug\")", "startLlmAuth", "logoutLlmAuth", "sendLlmQuota", "viewLlmConsent", "revokeLlmConsent", "sendLlmAuthStartResult", "redactedSummary")
    packets = read("src/main/java/com/crpg/ebb/network/ModPackets.java")
    require("P36 auth/profile/memory payload registration", packets, "LlmAuthStartPayload", "LlmAuthStatusRequestPayload", "LlmAuthUrlPayload", "LlmAuthStatusPayload", "NpcProfileSyncPayload", "MemoryDebugSnapshotPayload", "handleLlmAuthStart", "handleLlmAuthStatus")

    client_screen = read("src/client/java/com/crpg/ebb/client/gui/llm/NpcChatScreen.java")
    if "opaque_player_token" in client_screen or "redactedSummary" in client_screen:
        raise AssertionError("P36 client UI must not receive or render server auth tokens")
    client_network = read("src/client/java/com/crpg/ebb/client/network/ClientInteractionNetworking.java")
    if "opaque_player_token" in client_network:
        raise AssertionError("P36 client networking must not receive opaque auth tokens")

    docs = read("docs/json_authoring_guide.md")
    require("P36 authoring docs", docs, "P36 Gateway Auth", "/ebb llm auth", "require_player_auth", "server-side only")
    status = read("docs/current_status.md")
    require("P36 current status", status, "Phase 36 Gateway", "ebb-llm-gateway", "auth_required", "server-only token")
    test = read("src/test/java/com/crpg/ebb/DeepResearchDataTest.java")
    require("P36 JUnit coverage", test, "p36GatewayAuthFlowRequiresLoginAndKeepsTokensServerSide", "p36GatewayProjectConfigAndCommandSurfaceAreAuditable", "auth_required", "token=redacted")
    gametest = read("src/main/java/com/crpg/ebb/test/EbbGameTests.java")
    require("P36 GameTest coverage", gametest, "llmAuthGateDevLocalLoginAndLogoutAreServerSide", "DevLocalLlmAuthClient", "auth_required")


def audit_p37_openai_responses_gateway_chat() -> None:
    for path in [
        "ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/chat/GatewayChatRequest.java",
        "ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/chat/GatewayChatResponse.java",
        "ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/chat/GatewayChatProvider.java",
        "ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/chat/FakeGatewayChatProvider.java",
        "ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/chat/MockGatewayChatProvider.java",
        "ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/chat/OpenAiResponsesChatProvider.java",
        "ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/chat/SimpleCircuitBreaker.java",
        "src/main/java/com/crpg/ebb/llm/HttpLlmGatewayClient.java",
        "scripts/p37_gateway_chat_smoke.sh",
    ]:
        exists(path)

    build = read("ebb-llm-gateway/build.gradle.kts")
    require("P37 official OpenAI Java SDK", build, "com.openai:openai-java", "gatewaySmoke")

    config = read("ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/GatewayConfig.java")
    require("P37 gateway chat config", config, "EBB_GATEWAY_CHAT_PROVIDER", "EBB_OPENAI_MODEL", "EBB_OPENAI_STORE", "EBB_GATEWAY_CIRCUIT_FAILURE_THRESHOLD", "createChatProvider")

    gateway = read("ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/GatewayServer.java")
    require("P37 chat endpoint", gateway, "/v1/chat/message", "GatewayChatRequest.fromJson", "SimpleCircuitBreaker", "authService.tokenValid", "llm_circuit_open", "llm_gateway_error")

    openai = read("ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/chat/OpenAiResponsesChatProvider.java")
    require("P37 OpenAI Responses provider", openai, "OpenAIClient", "OpenAIOkHttpClient.fromEnv", "ResponseCreateParams", "ResponseFormatTextJsonSchemaConfig", "createStreaming", "ResponseAccumulator", "store(request.store() && allowStore)", "maxOutputTokens")
    if "OPENAI_API_KEY" in openai or "sk-" in openai:
        raise AssertionError("P37 provider must not hard-code OpenAI secrets")

    request = read("ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/chat/GatewayChatRequest.java")
    require("P37 chat request contract", request, "server_id", "world_id", "minecraft_player_uuid", "npc_key", "entity_uuid", "conversation_id", "scene_context", "structured", "store", "max_output_tokens")
    response = read("ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/chat/GatewayChatResponse.java")
    require("P37 chat response contract", response, "npc_reply", "mood", "suggested_options", "memory_writes", "citations", "proposed_effects", "warnings", "chunked_response", "structured_json")

    mock = read("ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/chat/MockGatewayChatProvider.java")
    require("P37 mock provider", mock, "mock_openai_responses")
    smoke = read("ebb-llm-gateway/src/test/java/com/crpg/ebb/gateway/GatewaySmoke.java")
    require("P37 gateway smoke", smoke, "/v1/chat/message", "FAKE_GATEWAY_REPLY", "chunked_response", "store\\\":false", "mock_openai_responses", "P37 gateway chat smoke passed")

    llm_config = read("src/main/java/com/crpg/ebb/llm/LlmConfig.java")
    require("P37 Minecraft LLM config", llm_config, "default_chat_model", "llm_chat_streaming", "structured_output", "openai_store", "DEFAULT_OPENAI_STORE", "toSafeJson")
    http_client = read("src/main/java/com/crpg/ebb/llm/HttpLlmGatewayClient.java")
    require("P37 Minecraft gateway chat client", http_client, "/v1/chat/message", "opaque_player_token", "LlmAuthService.validToken", "orTimeout", "llm_gateway_error", "suggested_options", "citations")
    service = read("src/main/java/com/crpg/ebb/llm/LlmChatService.java")
    require("P37 chat client wiring", service, "config.networkAccessAllowed", "new HttpLlmGatewayClient", "LlmChatErrorPayload")

    client_screen = read("src/client/java/com/crpg/ebb/client/gui/llm/NpcChatScreen.java")
    if "opaque_player_token" in client_screen or "OPENAI_API_KEY" in client_screen:
        raise AssertionError("P37 client UI must not receive player tokens or OpenAI secrets")

    docs = read("docs/json_authoring_guide.md")
    require("P37 authoring docs", docs, "P37 OpenAI Responses Gateway", "/v1/chat/message", "store:false", "mock_openai_responses")
    status = read("docs/current_status.md")
    require("P37 current status", status, "Phase 37 OpenAI Responses", "/v1/chat/message", "store:false", "HttpLlmGatewayClient")
    test = read("src/test/java/com/crpg/ebb/DeepResearchDataTest.java")
    require("P37 JUnit coverage", test, "p37HttpGatewayClientParsesResponseAndErrorsWithoutHanging", "p37GatewayOpenAiResponsesIntegrationIsAuditable", "llm_gateway_error", "store\\\":false")
    gametest = read("src/main/java/com/crpg/ebb/test/EbbGameTests.java")
    require("P37 GameTest coverage", gametest, "llmGatewayModeConfigIsTimeoutSafeAndPrivate", "HttpLlmGatewayClient", "store:false")


def audit_p38_memory_store_mvp() -> None:
    for path in [
        "ebb-llm-gateway/src/main/resources/db/migration/V001__memory_store.sql",
        "ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/memory/MemoryStore.java",
        "ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/memory/MemoryRecord.java",
        "ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/memory/MemoryFact.java",
        "ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/memory/MemoryConflict.java",
        "ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/memory/MemoryEmbeddingService.java",
        "ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/memory/MemoryFactExtractor.java",
        "ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/memory/MemorySearchRequest.java",
        "src/main/java/com/crpg/ebb/memory/MemoryGatewayClient.java",
        "scripts/p38_memory_smoke.sh",
    ]:
        exists(path)

    build = read("ebb-llm-gateway/build.gradle.kts")
    require("P38 H2 DB dependency", build, "com.h2database:h2", "gatewaySmoke")
    migration = read("ebb-llm-gateway/src/main/resources/db/migration/V001__memory_store.sql")
    require("P38 DB migration", migration, "schema_migrations", "memory_records", "memory_facts", "memory_conflicts", "citation_id", "embedding")

    store = read("ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/memory/MemoryStore.java")
    require("P38 MemoryStore", store, "MIGRATION_VERSION", "appendTurn", "search", "inspect", "conflicts", "currentFact", "supersedeFact", "newConflict", "memory:record:", "memory:fact:")
    require("P38 hybrid retrieval", store, "keyword", "embeddings.cosine", "entity", "recency", "citation_id")
    extractor = read("ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/memory/MemoryFactExtractor.java")
    require("P38 fact extraction", extractor, "fact", "remember", "self_description", "ExtractedFact")
    embedding = read("ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/memory/MemoryEmbeddingService.java")
    require("P38 embedding write path", embedding, "DIMENSIONS", "embed", "serialize", "cosine", "fnv1a")

    gateway = read("ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/GatewayServer.java")
    require("P38 memory endpoints", gateway, "/v1/memory/search", "/v1/memory/inspect", "/v1/memory/conflicts", "/v1/memory/ingest", "/v1/memory/correct", "/v1/memory/player", "memoryStore.appendTurn", "memoryStore.correctFact", "memoryStore.deletePlayer", "citation_ids")
    config = read("ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/GatewayConfig.java")
    require("P38 memory DB config", config, "EBB_MEMORY_DB_URL", "createMemoryStore", "MemoryStore")

    smoke = read("ebb-llm-gateway/src/test/java/com/crpg/ebb/gateway/GatewaySmoke.java")
    require("P38 gateway smoke", smoke, "P38 memory smoke passed", "/v1/memory/search", "/v1/memory/inspect", "/v1/memory/conflicts", "/v1/memory/correct", "/v1/memory/player", "favorite=blue", "favorite=red", "citation_ids", "append_only")

    client = read("src/main/java/com/crpg/ebb/memory/MemoryGatewayClient.java")
    require("P38 Minecraft memory gateway client", client, "/v1/memory/search", "/v1/memory/inspect", "/v1/memory/conflicts", "/v1/memory/correct", "/v1/memory/player", "/v1/player/quota", "citationIds", "memory_gateway_error")
    commands = read("src/main/java/com/crpg/ebb/registry/ModCommands.java")
    require("P38 memory commands", commands, "Commands.literal(\"memory\")", "Commands.literal(\"search\")", "Commands.literal(\"inspect\")", "Commands.literal(\"conflicts\")", "Commands.literal(\"correct\")", "Commands.literal(\"export\")", "Commands.literal(\"delete_player\")", "MemoryGatewayClient")

    docs = read("docs/json_authoring_guide.md")
    require("P38 authoring docs", docs, "P38 MemoryStore MVP", "/v1/memory/search", "/ebb memory search", "fact:player.favorite=blue", "citation ids")
    status = read("docs/current_status.md")
    require("P38 current status", status, "Phase 38 MemoryStore MVP", "MemoryStore", "MemoryConflict", "hybrid retrieval")
    test = read("src/test/java/com/crpg/ebb/DeepResearchDataTest.java")
    require("P38 JUnit coverage", test, "p38MemoryGatewayClientCommandSurfaceAndFilesAreAuditable", "memory:record:memrec_test", "requireCommandPath(ebb, \"memory\", \"search\", \"query\")")
    gametest = read("src/main/java/com/crpg/ebb/test/EbbGameTests.java")
    require("P38 GameTest coverage", gametest, "memoryGatewayClientSurfaceIsServerOnly", "MemoryGatewayClient", "P38 memory dev commands")


def audit_p39_memory_extraction_consolidation() -> None:
    for path in [
        "ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/memory/LlmMemoryOperationExtractor.java",
        "ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/memory/DeterministicMemoryValidator.java",
        "ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/memory/MemoryConsolidator.java",
        "ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/memory/MemoryOperation.java",
        "ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/memory/MemorySummary.java",
        "ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/memory/MemoryLink.java",
        "ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/memory/MemorySafetyLesson.java",
        "scripts/p39_memory_consolidation_smoke.sh",
    ]:
        exists(path)

    migration = read("ebb-llm-gateway/src/main/resources/db/migration/V001__memory_store.sql")
    require("P39 memory migration", migration, "memory_operations", "memory_summaries", "memory_links", "memory_safety_lessons", "summary", "related_memory_ids")
    extractor = read("ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/memory/LlmMemoryOperationExtractor.java")
    require("P39 LLM extractor proposals", extractor, "memory_writes", "llm_structured_json", "deterministic_ledger_question_extractor", "questioned_ledger", "我是旅馆老板", "tavern", "owner")
    validator = read("ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/memory/DeterministicMemoryValidator.java")
    require("P39 deterministic validator", validator, "CANONICAL_FACTS", "canonical_conflict", "tavern", "owner", "innkeeper", "confidence_below_threshold")
    consolidator = read("ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/memory/MemoryConsolidator.java")
    require("P39 background summarizer/evolution", consolidator, "backgroundSummarize", "evolveSummary", "raw episode text is preserved", "A-MemGuard safety lesson", "questioned_ledger")
    store = read("ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/memory/MemoryStore.java")
    require("P39 MemoryStore integration", store, "llmExtractor.propose", "validator.validate", "insertOperation", "insertSummary", "insertLink", "insertSafetyLesson", "evolveOldSummary", "raw_episode", "extracted_facts", "safetyLessons")
    gateway = read("ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/GatewayServer.java")
    require("P39 gateway dev endpoints", gateway, "/v1/memory/episodes", "/v1/memory/lessons", "handleMemoryEpisodes", "handleMemoryLessons")
    prompt = read("ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/chat/GatewayChatRequest.java")
    require("P39 chat prompt memory writes", prompt, "memory_writes", "fact:player.questioned_ledger=true", "lesson:")
    openai = read("ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/chat/OpenAiResponsesChatProvider.java")
    require("P39 structured memory writes", openai, "memory_writes", "HttpJson.stringArrayValue", "GatewayChatResponse")
    client = read("src/main/java/com/crpg/ebb/memory/MemoryGatewayClient.java")
    require("P39 Minecraft memory client", client, "/v1/memory/episodes", "/v1/memory/lessons", "episodes", "lessons")
    commands = read("src/main/java/com/crpg/ebb/registry/ModCommands.java")
    require("P39 Minecraft memory commands", commands, "Commands.literal(\"episodes\")", "Commands.literal(\"lessons\")", "Ebb memory safety lessons", "raw summaries")
    smoke = read("ebb-llm-gateway/src/test/java/com/crpg/ebb/gateway/GatewaySmoke.java")
    require("P39 gateway smoke", smoke, "P39 memory consolidation smoke passed", "canonical owner remains innkeeper", "questioned_ledger", "raw_episode", "extracted_facts")
    docs = read("docs/json_authoring_guide.md")
    require("P39 authoring docs", docs, "P39 Memory extraction / consolidation", "memory_writes", "canonical owner remains innkeeper", "/ebb memory episodes", "/ebb memory lessons")
    status = read("docs/current_status.md")
    require("P39 current status", status, "Phase 39 Memory extraction", "A-Mem-like", "A-MemGuard", "questioned_ledger")
    test = read("src/test/java/com/crpg/ebb/DeepResearchDataTest.java")
    require("P39 JUnit coverage", test, "p39MemoryExtractionConsolidationAndSafetyAreAuditable", "canonical owner remains innkeeper", "questioned_ledger", "memory_safety_lessons")
    gametest = read("src/main/java/com/crpg/ebb/test/EbbGameTests.java")
    require("P39 GameTest coverage", gametest, "P39 memory episodes/lessons dev surfaces", "MemoryGatewayClient")


def audit_p40_npc_knowledge_base_story_effects() -> None:
    for path in [
        "src/main/java/com/crpg/ebb/npc/knowledge/NpcKnowledgePackDefinition.java",
        "src/main/java/com/crpg/ebb/npc/knowledge/NpcKnowledgeRegistry.java",
        "src/main/java/com/crpg/ebb/npc/knowledge/NpcKnowledgeIndex.java",
        "src/main/java/com/crpg/ebb/npc/knowledge/NpcKnowledgeService.java",
    ]:
        exists(path)
    for pack in [
        "tavern_public_lore",
        "innkeeper_private_ledger",
        "kitchen_manifest",
        "courier_route",
        "guard_case_notes",
        "tenant_private_alibi",
        "witness_heard_knocks",
    ]:
        exists(f"src/main/resources/data/ebb/npc_knowledge_packs/demo/{pack}.json")
    registries = read("src/main/java/com/crpg/ebb/data/NarrativeDataRegistries.java")
    require("P40 registry reload", registries, "NPC_KNOWLEDGE_PACKS", "npc_knowledge_packs", "NpcKnowledgeRegistry.rebuild", "NpcKnowledgeRegistry.summaryLine")
    definition = read("src/main/java/com/crpg/ebb/npc/knowledge/NpcKnowledgePackDefinition.java")
    require("P40 KB parser", definition, "reveal_conditions", "DialogueCondition.parse", "secret", "chunks", "visible")
    index = read("src/main/java/com/crpg/ebb/npc/knowledge/NpcKnowledgeIndex.java")
    require("P40 KB deterministic embedding/index", index, "DIMENSIONS", "embed", "cosine", "fnv1a")
    service = read("src/main/java/com/crpg/ebb/npc/knowledge/NpcKnowledgeService.java")
    require("P40 visible-only prompt assembly", service, "promptContext", "visible chunks only", "hiddenChunks", "PACK_TAG_PREFIX", "FACT_TAG_PREFIX", "STANCE_TAG_PREFIX", "NpcProfileRegistry")
    effect = read("src/main/java/com/crpg/ebb/dialogue/DialogueEffect.java")
    require("P40 story effects", effect, "NPC_KB_ADD_FACT", "NPC_KB_ADD_PACK", "NPC_STANCE_SHIFT", "NpcKnowledgeService.addFact", "NpcKnowledgeService.addPack", "NpcKnowledgeService.shiftStance")
    commands = read("src/main/java/com/crpg/ebb/registry/ModCommands.java")
    require("P40 KB inspect/add command", commands, "Commands.literal(\"kb\")", "Commands.literal(\"inspect\")", "Commands.literal(\"add_pack\")", "inspectNpcKnowledge", "addNpcKnowledgePack", "NpcKnowledgeService.inspectLines", "NpcKnowledgeService.addPack", "/ebb kb inspect <npc>")
    innkeeper_pack = read("src/main/resources/data/ebb/npc_knowledge_packs/demo/innkeeper_private_ledger.json")
    require("P40 hidden/revealed demo secret", innkeeper_pack, "secret_ledger_tenant_cash", "tenant paid cash", "reveal_conditions", "ebb:demo/guestbook_gap")
    fake = read("src/main/java/com/crpg/ebb/llm/FakeLlmGatewayClient.java")
    gateway_fake = read("ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/chat/FakeGatewayChatProvider.java")
    require("P40 fake answer changes", fake + gateway_fake, "kbSignal", "secret_visible", "public_only", "tenant paid cash")
    docs = read("docs/json_authoring_guide.md")
    require("P40 authoring docs", docs, "P40 NPC Knowledge Base", "npc_knowledge_packs", "reveal_conditions", "npc_kb_add_pack", "/ebb kb inspect")
    status = read("docs/current_status.md")
    require("P40 current status", status, "Phase 40 NPC Knowledge Base", "final validation", "tenant paid cash", "/ebb kb inspect")
    test = read("src/test/java/com/crpg/ebb/DeepResearchDataTest.java")
    require("P40 JUnit coverage", test, "p40NpcKnowledgeHidesSecretsUntilClueAndSupportsStoryEffects", "tenant paid cash", "kb=secret_visible", "requireCommandPath(ebb, \"kb\", \"inspect\", \"npc\")")
    gametest = read("src/main/java/com/crpg/ebb/test/EbbGameTests.java")
    require("P40 GameTest coverage", gametest, "npcKnowledgeHidesSecretUntilClueAndChangesFakeAnswer", "secret_ledger_tenant_cash", "kb=public_only", "kb=secret_visible")


def audit_p41_minor_npc_instant_generation() -> None:
    exists("src/main/java/com/crpg/ebb/npc/profile/NpcProfileGenerator.java")
    generator = read("src/main/java/com/crpg/ebb/npc/profile/NpcProfileGenerator.java")
    require("P41 NPC profile generator", generator, "PROMPT_VERSION", "SCHEMA_ID", "promptTemplate", "schema", "generatePromotedProfileJson", "character", "stance", "knowledge_seed", "suggested_options")
    service = read("src/main/java/com/crpg/ebb/npc/profile/NpcPromotionService.java")
    require("P41 minor detector/rate limit", service, "MINOR_NPC_TAG", "isMinorCandidate", "ensurePromotedIfMinor", "MAX_PROMOTIONS_PER_WORLD_HOUR", "WORLD_HOUR_TICKS", "rate_limited", "canPromoteThisWorldHour")
    llm_service = read("src/main/java/com/crpg/ebb/llm/LlmChatService.java")
    require("P41 first chat promotion", llm_service, "ensurePromotedIfMinor", "PROMOTED_MAJOR_STATUS", "rate_limited", "activePromotion")
    state = read("src/main/java/com/crpg/ebb/state/NarrativeSavedData.java")
    require("P41 promoted profile persistence", state, "promoted_npc_profiles", "putPromotedNpcProfile", "removePromotedNpcProfile", "promotedNpcProfileDebugLines")
    binding = read("src/main/resources/data/ebb/interactions/entity_bindings/llm/minor_villager.json")
    require("P41 minor villager binding", binding, "minecraft:villager", "ebb.npc.minor", "minor_generatable", "promote_on_first_chat", "profile_seed_archetypes")
    dialogue = read("src/main/resources/data/ebb/dialogues/llm/minor_intro.json")
    require("P41 minor chat dialogue", dialogue, "llm_chat", "first conversation with a minor tavern NPC", "allow_memory_write")
    commands = read("src/main/java/com/crpg/ebb/registry/ModCommands.java")
    require("P41 OP review/reject/regenerate/demote commands", commands, "Commands.literal(\"minorize\")", "Commands.literal(\"promote\")", "Commands.literal(\"review\")", "Commands.literal(\"reject_profile\")", "Commands.literal(\"regenerate_profile\")", "Commands.literal(\"demote\")", "devReviewLines")
    docs = read("docs/json_authoring_guide.md") + read("docs/current_status.md")
    require("P41 docs", docs, "P41 Minor NPC instant generation", "NpcProfileGenerator", "knowledge_seed", "suggested_options", "rate limit", "/ebb npc reject_profile")
    test = read("src/test/java/com/crpg/ebb/DeepResearchDataTest.java")
    require("P41 JUnit coverage", test, "p41MinorNpcProfileGenerationRateLimitAndDevReviewAreAuditable", "MAX_PROMOTIONS_PER_WORLD_HOUR", "requireCommandPath(ebb, \"npc\", \"review\", \"npc_key\")")
    gametest = read("src/main/java/com/crpg/ebb/test/EbbGameTests.java")
    require("P41 GameTest coverage", gametest, "knowledge_seed", "suggested_options", "existing_promoted_major", "P41 generated profile")


def audit_p42_llm_chat_ui_completion() -> None:
    screen = read("src/client/java/com/crpg/ebb/client/gui/llm/NpcChatScreen.java")
    require("P42 LLM chat UI streaming merge", screen, "appendNpcChunk", "streamingNpcLineIndex", "payload.done()", "CLIENT_REPLY_TIMEOUT_MS")
    require("P42 LLM chat UI controls", screen, "return_to_script", "memory_correction", "toggleMemoryCorrection", "renderCitationsOverlay", "citations_visible")
    require("P42 LLM chat citations overlay", screen, "recentCitations", "screen.ebb.llm_chat.citations_heading")
    service = read("src/main/java/com/crpg/ebb/llm/LlmChatService.java")
    require("P42 server streaming and return", service, "sendStreamingNpcResponse", "streamingChunks", "DialogueService.reopenFromLlmChat", "script_return_unavailable")
    dialogue_service = read("src/main/java/com/crpg/ebb/dialogue/DialogueService.java")
    require("P42 dialogue resume", dialogue_service, "reopenFromLlmChat", "returnNodeId", "toOpenPayload", "returned_from_llm_chat")
    menu = read("src/client/java/com/crpg/ebb/client/gui/menu/EbbMenuScreen.java")
    require("P42 K menu LLM auth status", menu, "screen.ebb.menu.llm_auth_status_hint", "ebb llm status", "ebb llm auth", "ebb llm logout")
    networking = read("src/client/java/com/crpg/ebb/client/network/ClientInteractionNetworking.java")
    require("P42 cancel/send non-stuck feedback", networking, "sendLlmChatCancel", "message.ebb.llm_chat_network_unavailable", "Cannot send LLM chat cancel")
    gui = read("scripts/gui_e2e_run.py") + read("scripts/run_gui_automation_smoke.sh")
    require("P42 GUI E2E scenario", gui, "scenario_llm_chat", "llm_chat_reply", "llm_suggested_option_clicked", "llm_return_to_script", "expected-llm-chat-manifest.json")
    lang = read("src/main/resources/assets/ebb/lang/en_us.json") + read("src/main/resources/assets/ebb/lang/zh_cn.json")
    require("P42 translations", lang, "screen.ebb.llm_chat.return_to_script", "screen.ebb.llm_chat.memory_correction", "screen.ebb.llm_chat.citations_heading", "screen.ebb.menu.llm_auth_status_hint")
    docs = read("docs/json_authoring_guide.md") + read("docs/current_status.md")
    require("P42 docs", docs, "P42 LLM Chat UI", "return_to_script", "memory correction", "citations overlay", "K menu")
    test = read("src/test/java/com/crpg/ebb/DeepResearchDataTest.java")
    require("P42 JUnit coverage", test, "p42LlmChatUiCompletionSurfacesAreAuditable", "streamingChunks", "scenario_llm_chat", "requireCommandPath(ebb, \"llm\", \"status\")")
    gametest = read("src/main/java/com/crpg/ebb/test/EbbGameTests.java")
    require("P42 GameTest coverage", gametest, "llmChatStreamingChunksAndUiContractsArePresent", "P42 streaming helper")

def audit_p43_testing_evaluation_documentation() -> None:
    for path in [
        "docs/schemas/ebb.npc_profile.schema.json",
        "docs/schemas/ebb.npc_knowledge.schema.json",
        "scripts/p43_llm_safety_audit.py",
    ]:
        exists(path)

    docs = read("docs/json_authoring_guide.md")
    require(
        "P43 authoring docs",
        docs,
        "P43 Testing / Evaluation Authoring Reference",
        "NPC profile data",
        "NPC knowledge packs / KB",
        "LLM server config",
        "Memory effects and LLM memory writes",
        "P43 verification matrix",
    )
    profile_schema = read("docs/schemas/ebb.npc_profile.schema.json")
    require("P43 NPC profile schema", profile_schema, "major_scripted", "major_promoted", "character", "stance", "knowledge", "allow_memory_write")
    knowledge_schema = read("docs/schemas/ebb.npc_knowledge.schema.json")
    require("P43 NPC knowledge schema", knowledge_schema, "chunks", "secret", "reveal_conditions", "clue_found", "minItems")

    safety_audit = read("scripts/p43_llm_safety_audit.py")
    require(
        "P43 safety static audit",
        safety_audit,
        "audit_no_api_key_literals",
        "audit_fake_provider_in_tests",
        "audit_hidden_knowledge_not_in_client_sync",
        "audit_high_risk_effects_not_direct_llm_output",
        "SECRET_PATTERNS",
        "high-risk direct LLM effects",
    )
    if "scripts/p43_llm_safety_audit.py" not in read("scripts/run_smoke_checks.sh"):
        raise AssertionError("P43 safety audit must be part of scripts/run_smoke_checks.sh")

    secret_patterns = [
        re.compile(r"\bsk-[A-Za-z0-9_-]{20,}\b"),
        re.compile(r"\bgh[oprsu]_[A-Za-z0-9_]{20,}\b"),
        re.compile(r"\bAIza[0-9A-Za-z_-]{20,}\b"),
        re.compile(r"Bearer\s+[A-Za-z0-9._~+/-]{20,}", re.IGNORECASE),
    ]
    for relative in [
        "src/main/java",
        "src/client/java",
        "src/test/java",
        "ebb-llm-gateway/src/main/java",
        "ebb-llm-gateway/src/test/java",
        "docs",
        "scripts",
    ]:
        root = ROOT / relative
        for path in root.rglob("*"):
            if not path.is_file() or path.suffix.lower() not in {".java", ".py", ".sh", ".json", ".md", ".kts"}:
                continue
            body = path.read_text(encoding="utf-8", errors="replace")
            for pattern in secret_patterns:
                if pattern.search(body):
                    raise AssertionError(f"P43 secret-like literal in {path.relative_to(ROOT)}")

    test_text = read("src/test/java/com/crpg/ebb/DeepResearchDataTest.java")
    require(
        "P43 JUnit coverage",
        test_text,
        "p43TestingEvaluationAndSafetyGatesAreAuditable",
        "favorite=blue",
        "favorite=red",
        "promoted_npc_profiles",
        "prompt pack assembly",
        "scenario_llm_validation",
    )
    gametest = read("src/main/java/com/crpg/ebb/test/EbbGameTests.java")
    require(
        "P43 GameTest coverage",
        gametest,
        "p43FakeChatMinorPromotionAndRelationshipDeltaAreDeterministic",
        "FAKE_NPC_REPLY",
        "ensurePromotedProfile",
        "addRelation",
    )

    gui = read("scripts/gui_e2e_run.py") + read("scripts/run_gui_automation_smoke.sh")
    require(
        "P43 GUI E2E coverage",
        gui,
        "scenario_llm_validation",
        "auth_disabled",
        "fake_provider_chat_route",
        "real_gateway_dry_run",
        "expected-p43-llm-validation-manifest.json",
    )

    sync_payloads = "\n".join(path.read_text(encoding="utf-8", errors="replace") for path in (ROOT / "src/main/java/com/crpg/ebb/network").rglob("*.java"))
    client_sources = "\n".join(path.read_text(encoding="utf-8", errors="replace") for path in (ROOT / "src/client/java").rglob("*.java"))
    forbidden_sync_tokens = ["NpcKnowledgePackDefinition", "NpcKnowledgeService", "npc_knowledge_packs", "hiddenChunks", "secret_ledger_tenant_cash", "tenant paid cash"]
    for token in forbidden_sync_tokens:
        if token in sync_payloads or token in client_sources:
            raise AssertionError(f"P43 hidden KB token leaked to client/sync source: {token}")

    gateway_response = read("ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/chat/GatewayChatResponse.java")
    require(
        "P43 high-risk proposed effect guard",
        gateway_response,
        "sanitizeProposedEffects",
        "containsHighRiskEffectVerb",
        "high_risk_effects_rejected_from_llm_output",
        "complete_quest_branch",
        "give_item",
        "set_npc_routine",
    )
    if "proposed_effects" in read("src/main/java/com/crpg/ebb/llm/HttpLlmGatewayClient.java"):
        raise AssertionError("P43 Minecraft-side gateway client must ignore direct LLM proposed_effects")

def main() -> int:
    audit_p20_p21_documentation_and_baseline()
    audit_p22_interaction_highlight_polish()
    audit_p23_dialogue_ui_reading_rhythm_foundation()
    audit_p24_authoring_validation_hardening()
    audit_p25_quest_feat_maturation()
    audit_p26_chime_inner_voice_expansion()
    audit_p27_npc_animation_routine_production()
    audit_p28_investigation_conflict_expansion()
    audit_p29_save_multiplayer_permissions_hardening()
    audit_p30_vertical_slice_content_expansion()
    audit_p31_release_packaging_player_documentation()
    audit_p32_k_menu_and_live_dialogue_background()
    audit_p33_codebase_review_remediation()
    audit_p34_llm_fake_provider_foundation()
    audit_p35_npc_profile_tier_promotion_data_layer()
    audit_p36_gateway_auth_minimal_service()
    audit_p37_openai_responses_gateway_chat()
    audit_p38_memory_store_mvp()
    audit_p39_memory_extraction_consolidation()
    audit_p40_npc_knowledge_base_story_effects()
    audit_p41_minor_npc_instant_generation()
    audit_p42_llm_chat_ui_completion()
    audit_p43_testing_evaluation_documentation()

    exists("src/main/java/com/crpg/ebb/story/StoryVarLayer.java")
    exists("src/main/java/com/crpg/ebb/story/StoryVarValue.java")
    require("StoryVarLayer", read("src/main/java/com/crpg/ebb/story/StoryVarLayer.java"), "BRANCH", "MAJOR", "MINOR", "serializedName")
    require("StoryVarValue", read("src/main/java/com/crpg/ebb/story/StoryVarValue.java"), "fromJson", "asInt", "scalarEquals", "ofBoolean")

    player_state = read("src/main/java/com/crpg/ebb/state/PlayerNarrativeState.java")
    require("Player story-var persistence", player_state, "story_branch_vars", "story_major_vars", "story_minor_vars", "storyVariables(StoryVarLayer layer)")

    saved_data = read("src/main/java/com/crpg/ebb/state/NarrativeSavedData.java")
    require(
        "Saved story-var persistence",
        saved_data,
        "world_story_branch_vars",
        "world_story_major_vars",
        "world_story_minor_vars",
        "getPlayerStoryVariable",
        "setPlayerStoryVariable",
        "addPlayerStoryInt",
        "getWorldStoryVariable",
        "addWorldStoryInt",
        "storyVariableDebugLines",
    )

    effect = read("src/main/java/com/crpg/ebb/dialogue/DialogueEffect.java")
    require("Story-var effects", effect, "SET_STORY_VAR", "CLEAR_STORY_VAR", "ADD_STORY_INT", "set_story_var", "add_story_int", "StoryVarLayer")

    condition = read("src/main/java/com/crpg/ebb/dialogue/DialogueCondition.java")
    require("Story-var conditions", condition, "STORY_VAR", "StoryVarLayer", "getPlayerStoryVariable", "scalarEquals", "min")

    commands = read("src/main/java/com/crpg/ebb/registry/ModCommands.java")
    require("Dev command story vars", commands, "sendStoryVarLayer", "story.branch", "story.major", "story.minor")
    require("Dev snapshot story vars", read("src/main/java/com/crpg/ebb/dev/DevSnapshotService.java"), "storyVariableDebugLines")

    innkeeper = read("src/main/resources/data/ebb/dialogues/demo/innkeeper_intro.json")
    require("Bundled Branch demo", innkeeper, '"layer": "branch"', '"id": "tavern_route"', "public_ending_placeholder")
    require("Bundled Major demo", innkeeper, '"layer": "major"', '"id": "innkeeper_trust"', "trusted_line")
    require("Bundled Minor demo", innkeeper, '"layer": "minor"', '"id": "met_innkeeper"', '"id": "innkeeper_annoyance"')

    test = read("src/test/java/com/crpg/ebb/DeepResearchDataTest.java")
    require("JUnit story-var coverage", test, "storyVariablesSupportBranchMajorMinorEffectsConditionsAndCodec", "StoryVarLayer.BRANCH", "StoryVarLayer.MAJOR", "StoryVarLayer.MINOR")

    smoke = read("scripts/smoke/DeepResearchSmoke.java")
    require("Smoke story-var coverage", smoke, "set_story_var", "add_story_int", "getPlayerStoryVariable")

    docs = read("docs/json_authoring_guide.md")
    require("Authoring docs story vars", docs, "Story Variables", "set_story_var", "story_var", "Branch", "Major", "Minor")

    for path in [
        "src/main/java/com/crpg/ebb/quest/QuestBranchDefinition.java",
        "src/main/java/com/crpg/ebb/quest/QuestBranchRegistry.java",
        "src/main/java/com/crpg/ebb/quest/TakeRootService.java",
        "src/main/java/com/crpg/ebb/quest/QuestTreeService.java",
        "src/main/java/com/crpg/ebb/feat/FeatDefinition.java",
        "src/main/java/com/crpg/ebb/feat/FeatRegistry.java",
        "src/main/java/com/crpg/ebb/network/quest/QuestTreePayload.java",
        "src/client/java/com/crpg/ebb/client/gui/quest/QuestTreeScreen.java",
    ]:
        exists(path)

    require("Quest/feat data registries", read("src/main/java/com/crpg/ebb/data/NarrativeDataRegistries.java"), "QUEST_BRANCHES", "FEATS", "QuestBranchRegistry.rebuild", "FeatRegistry.rebuild")
    require("Quest state persistence", saved_data, "quest_states", "unlocked_feats", "active_feats", "getQuestState", "unlockFeat", "activateFeat", "questFeatDebugLines")
    require("Quest effects", effect, "START_QUEST_BRANCH", "COMPLETE_QUEST_BRANCH", "UNLOCK_FEAT", "ACTIVATE_FEAT", "TakeRootService")
    require("Quest/feat conditions", condition, "QUEST_STATE", "HAS_FEAT", "getQuestState", "hasFeat")
    require("Feat check modifiers", read("src/main/java/com/crpg/ebb/dialogue/DialogueService.java"), "FeatRegistry.totalCheckModifier")
    require("Quest tree command", commands, "QuestTreeService.build", "QuestTreePayload", "quest")
    require("Quest tree client receiver", read("src/client/java/com/crpg/ebb/client/network/ClientInteractionNetworking.java"), "QuestTreePayload", "QuestTreeScreen")
    require("Quest tree packet registration", read("src/main/java/com/crpg/ebb/network/ModPackets.java"), "QuestTreePayload.TYPE")
    require("Dev snapshot quest/feat", read("src/main/java/com/crpg/ebb/dev/DevSnapshotService.java"), "QuestBranchRegistry.summaryLine", "FeatRegistry.summaryLine", "questFeatDebugLines")

    quest_public = read("src/main/resources/data/ebb/quest_branches/demo/tavern_public.json")
    quest_quiet = read("src/main/resources/data/ebb/quest_branches/demo/tavern_quiet.json")
    require("Two major quest branches", quest_public + quest_quiet, '"kind": "major"', "take_root_text", "grant_feats")
    for feat in ["tavern_authority", "paranoid_pattern_reader", "cheap_empathy", "door_theology"]:
        exists(f"src/main/resources/data/ebb/feats/demo/{feat}.json")
    require("Demo quest effects", innkeeper, "complete_quest_branch", "ebb:demo/tavern_public", "ebb:demo/tavern_quiet", "has_feat")
    require("JUnit quest/feat coverage", test, "questTakeRootGrantsFeatsAndFeatModifiersApplyToChecks", "TakeRootService.completeBranch", "FeatRegistry.totalCheckModifier")
    require("Smoke quest/feat coverage", smoke, "QuestBranchRegistry", "FeatRegistry", "TakeRootService.completeBranch")
    require("Authoring docs quest/feat", docs, "Quest Branch", "Take Root", "Feat", "complete_quest_branch", "/ebb quest")

    for path in [
        "src/main/java/com/crpg/ebb/chime/ChimeDefinition.java",
        "src/main/java/com/crpg/ebb/chime/ChimeRegistry.java",
        "src/main/java/com/crpg/ebb/chime/ChimeResolver.java",
    ]:
        exists(path)
    require("Chime data registry", read("src/main/java/com/crpg/ebb/data/NarrativeDataRegistries.java"), "CHIMES", "ChimeRegistry.rebuild")
    require("Dialogue node chime tags", read("src/main/java/com/crpg/ebb/dialogue/DialogueNode.java"), "chimeTags", '"chime_tags"', '"tags"')
    require("Dialogue chime resolver hook", read("src/main/java/com/crpg/ebb/dialogue/DialogueService.java"), "ChimeResolver.resolve")
    require("DialogueScreen chime styling", read("src/client/java/com/crpg/ebb/client/gui/dialogue/DialogueScreen.java"), "CHIME_STATUS_COLOR", "[Chime:")
    require("Dev snapshot chimes", read("src/main/java/com/crpg/ebb/dev/DevSnapshotService.java"), "ChimeRegistry.summaryLine", "Chime validation")
    for chime in ["instinct", "rhetoric", "dread", "empathy"]:
        exists(f"src/main/resources/data/ebb/chimes/demo/{chime}.json")
    require("Demo chime scene", innkeeper, '"chime_tags"', "innkeeper.read", "rhetoric_insight", "chime:ebb:demo/rhetoric")
    require("JUnit chime coverage", test, "chimesResolveFromBuildAndUnlockPassiveInsightPath", "ChimeResolver.resolve")
    require("Smoke chime coverage", smoke, "ChimeRegistry", "ChimeResolver.resolve", "rhetoric_insight")
    require("Authoring docs chimes", docs, "Chime / Inner Voice", "chime_tags", "trigger_tags", "passive insert")

    for path in [
        "src/main/java/com/crpg/ebb/journal/JournalEntryCategory.java",
        "src/main/java/com/crpg/ebb/journal/JournalEntryDefinition.java",
        "src/main/java/com/crpg/ebb/journal/JournalEntryRegistry.java",
        "src/main/java/com/crpg/ebb/journal/JournalService.java",
        "src/main/java/com/crpg/ebb/network/journal/JournalPayload.java",
        "src/client/java/com/crpg/ebb/client/gui/journal/JournalScreen.java",
    ]:
        exists(path)
    require("Journal data registry", read("src/main/java/com/crpg/ebb/data/NarrativeDataRegistries.java"), "JOURNAL_ENTRIES", "JournalEntryRegistry.rebuild")
    require("Journal persistence", saved_data, "journal_entries", "unlockJournalEntry", "hasJournalEntry", "journalEntryIds", "journalEntryCount")
    require("Journal effects", effect, "ADD_JOURNAL_ENTRY", "JournalService.addEntry", "REVEAL_CLUE")
    require("Journal conditions", condition, "HAS_JOURNAL_ENTRY", "hasJournalEntry")
    require("Journal command", commands, "sendJournal", "JournalPayload", '"journal"')
    require("Journal client receiver", read("src/client/java/com/crpg/ebb/client/network/ClientInteractionNetworking.java"), "JournalPayload", "JournalScreen")
    require("Journal packet registration", read("src/main/java/com/crpg/ebb/network/ModPackets.java"), "JournalPayload.TYPE")
    require("Dialogue status echoes", read("src/client/java/com/crpg/ebb/client/gui/dialogue/DialogueScreen.java"), "statusLabel", "clue_gained", "quest_completed", "feat_unlocked", "relation_")
    for entry in ["door_scratches", "bruised_shoulder", "public_pressure", "quiet_compromise"]:
        exists(f"src/main/resources/data/ebb/journal_entries/demo/{entry}.json")
    require("Demo journal effects", read("src/main/resources/data/ebb/dialogues/demo/locked_door_dialogue.json") + innkeeper, "add_journal_entry", "ebb:demo/door_scratches", "ebb:demo/bruised_shoulder", "ebb:demo/public_pressure", "ebb:demo/quiet_compromise")
    require("JUnit journal coverage", test, "journalEntriesCanBeUnlockedDisplayedAndGateChoices", "JournalService.addEntry")
    require("Smoke journal coverage", smoke, "JournalEntryRegistry", "JournalService.addEntry", "hasJournalEntry")
    require("Authoring docs journal", docs, "Journal / Clues / Leads", "add_journal_entry", "/ebb journal", "status echoes")

    for path in [
        "src/main/java/com/crpg/ebb/relationship/RelationshipDefinition.java",
        "src/main/java/com/crpg/ebb/relationship/RelationshipRegistry.java",
    ]:
        exists(path)
    require("Relationship data registry", read("src/main/java/com/crpg/ebb/data/NarrativeDataRegistries.java"), "RELATIONSHIPS", "RelationshipRegistry.rebuild")
    require("Relationship persistence", saved_data, "relationships", "npc_state_tags", "world_npc_state_tags", "getRelation", "addRelation", "setPlayerNpcState", "setWorldNpcState", "relationshipDebugLines")
    require("Player relationship persistence", player_state, "relationships", "npcStateTags", "relationshipsForCodec", "npcStateTagsForCodec")
    require("Relationship effects", effect, "SET_RELATION", "ADD_RELATION", "SET_NPC_STATE", "CLEAR_NPC_STATE", "SET_NPC_ROUTINE")
    require("Relationship conditions", condition, "RELATION_AT_LEAST", "NPC_STATE", "TIME_WINDOW", "getRelation", "hasPlayerNpcState", "dayTime")
    require("Dev snapshot relationships", read("src/main/java/com/crpg/ebb/dev/DevSnapshotService.java"), "RelationshipRegistry.summaryLine", "relationshipDebugLines", "Relationship validation")
    require("Routine action expansion", read("src/main/java/com/crpg/ebb/routine/NpcRoutineDefinition.java"), "animation", "pose", "teleportDistance", "teleport_distance")
    require("Routine controller expansion", read("src/main/java/com/crpg/ebb/routine/NpcRoutineController.java"), "wait", "walk_path", "look_at", "play_animation", "set_pose", "teleport_fallback")
    require("NPC narrative key", read("src/main/java/com/crpg/ebb/npc/EbbNpcEntity.java"), "narrativeStateKey", "narrativePose", "narrativeAnimation")
    require("Routine inspect/summon keys", commands, "narrative_key", "narrativeKeyForRoutine", "narrativeTagForRoutine", "ebb.npc.")
    require("Dialogue P6 status echoes", read("src/client/java/com/crpg/ebb/client/gui/dialogue/DialogueScreen.java"), "npc_state_", "npc_routine_", "关系变化")
    for rel in ["innkeeper", "witness"]:
        exists(f"src/main/resources/data/ebb/relationships/demo/{rel}.json")
    for routine in ["innkeeper_backroom", "witness_day", "tenant_day", "guard_day"]:
        exists(f"src/main/resources/data/ebb/npc_routines/demo/{routine}.json")
    for dialogue in ["witness_intro", "tenant_intro", "guard_intro"]:
        exists(f"src/main/resources/data/ebb/dialogues/demo/{dialogue}.json")
    for binding in ["innkeeper_ebb_npc", "witness_ebb_npc", "tenant_ebb_npc", "guard_ebb_npc"]:
        exists(f"src/main/resources/data/ebb/interactions/entity_bindings/demo/{binding}.json")
    p6_dialogues = innkeeper + read("src/main/resources/data/ebb/dialogues/demo/witness_intro.json")
    require("Demo relationship/memory effects", p6_dialogues, "add_relation", "set_npc_state", "relation_at_least", "npc_state", "time_window", "set_npc_routine")
    require("JUnit relationship coverage", test, "relationshipsNpcStateAndTimeWindowConditionsWork", "RelationshipRegistry", "set_npc_state", "time_window")
    require("Smoke relationship coverage", smoke, "RelationshipRegistry", "add_relation", "set_npc_state", "time-window")
    require("Authoring docs relationships", docs, "Relationship / NPC Memory", "add_relation", "set_npc_state", "time_window", "set_npc_routine")

    for path in [
        "src/main/java/com/crpg/ebb/investigation/ClueDefinition.java",
        "src/main/java/com/crpg/ebb/investigation/InvestigationSceneDefinition.java",
        "src/main/java/com/crpg/ebb/investigation/InvestigationRegistry.java",
        "src/main/java/com/crpg/ebb/investigation/InvestigationService.java",
        "src/main/java/com/crpg/ebb/conflict/ConflictDefinition.java",
        "src/main/java/com/crpg/ebb/conflict/ConflictRegistry.java",
        "src/main/java/com/crpg/ebb/conflict/ConflictService.java",
    ]:
        exists(path)
    registry_source = read("src/main/java/com/crpg/ebb/data/NarrativeDataRegistries.java")
    require("Investigation/conflict registries", registry_source, "CLUES", "INVESTIGATION_SCENES", "CONFLICTS", "InvestigationRegistry.rebuildClues", "ConflictRegistry.rebuild")
    require("Investigation persistence", saved_data, "discovered_clues", "revealClue", "hasClue", "setScenePhase", "getConflictState", "addConflictScore", "investigationDebugLines")
    require("Player investigation persistence", player_state, "discoveredClues", "narrativeStates", "conflictScores")
    require("Investigation effects", effect, "REVEAL_CLUE", "START_CONFLICT", "ADD_CONFLICT_STRESS", "ADD_CONFLICT_RESOLVE", "SET_SCENE_PHASE")
    require("Investigation conditions", condition, "CLUE_FOUND", "CONFLICT_STATE", "SCENE_PHASE", "hasClue", "getConflictState", "getScenePhase")
    require("Clue check modifier hook", read("src/main/java/com/crpg/ebb/dialogue/DialogueService.java"), "InvestigationRegistry.totalCheckModifier", "clueModifier")
    require("Dev snapshot investigations", read("src/main/java/com/crpg/ebb/dev/DevSnapshotService.java"), "InvestigationRegistry.summaryLine", "ConflictRegistry.summaryLine", "investigationDebugLines")
    require("Dialogue P7 status echoes", read("src/client/java/com/crpg/ebb/client/gui/dialogue/DialogueScreen.java"), "clue_found", "conflict_", "scene_phase")
    for clue in ["door_scratches", "bruised_shoulder", "witness_knock_pattern", "tenant_false_window", "guard_denial"]:
        exists(f"src/main/resources/data/ebb/clues/demo/{clue}.json")
    exists("src/main/resources/data/ebb/investigation_scenes/demo/locked_room.json")
    exists("src/main/resources/data/ebb/conflicts/demo/hallway_confrontation.json")
    guard = read("src/main/resources/data/ebb/dialogues/demo/guard_intro.json")
    p7_dialogues = read("src/main/resources/data/ebb/dialogues/demo/locked_door_dialogue.json") + read("src/main/resources/data/ebb/dialogues/demo/witness_intro.json") + read("src/main/resources/data/ebb/dialogues/demo/tenant_intro.json") + guard
    require("Demo clue/conflict wiring", p7_dialogues, "reveal_clue", "clue_found", "start_conflict", "add_conflict_stress", "add_conflict_resolve", "set_scene_phase", "failed_forward")
    require("JUnit investigation coverage", test, "investigationCluesModifyChecksAndConflictFailsForward", "InvestigationRegistry.totalCheckModifier", "ConflictService.addStress")
    require("Smoke investigation coverage", smoke, "InvestigationRegistry", "reveal_clue", "clue-to-DC", "ConflictService")
    require("Authoring docs investigation", docs, "Investigation / Clues / Set-piece Conflict", "reveal_clue", "clue_found", "start_conflict", "add_conflict_stress")

    for block_group in [
        "locked_door",
        "counter_ledger",
        "washroom_mirror",
        "windowsill_ash",
        "tenant_luggage",
        "notice_board",
        "cellar_hatch",
        "back_door",
    ]:
        exists(f"src/main/resources/data/ebb/interactions/block_groups/demo/{block_group}.json")
    for dialogue in [
        "counter_ledger_dialogue",
        "washroom_mirror_dialogue",
        "windowsill_ash_dialogue",
        "tenant_luggage_dialogue",
        "notice_board_dialogue",
        "cellar_hatch_dialogue",
        "back_door_dialogue",
    ]:
        exists(f"src/main/resources/data/ebb/dialogues/demo/{dialogue}.json")
    back_door = read("src/main/resources/data/ebb/dialogues/demo/back_door_dialogue.json")
    require("Ending placeholders", back_door, "public_end", "quiet_end", "messy_end", "ending_placeholder")
    require("Vertical slice JUnit coverage", test, "playableVerticalSliceMeetsContentMinimums", "BlockGroupIndex.groupCount() >= 8", "back_door_dialogue")
    require("Vertical slice smoke coverage", smoke, "vertical slice should expose at least 8 interactable points", "ending placeholder dialogue should load")
    require("Authoring docs P8", docs, "Playable Tavern Vertical Slice Content Map", "eight block-group investigation points", "back door / ending placeholder")
    require("P8 completion audit", read("docs/goal_p8_vertical_slice_2026-06-01.md"), "four role NPCs", "eight interactable investigation points", "Ending placeholders")

    print("GoalStaticAudit passed: P20/P21 documentation, baseline pins, data directories, artifact status hashes, failure-forward lint, and major Take-Root guardrails are present; P22 interaction/highlight polish guardrails cover synced highlight styles, merged block outlines, target debug reasons, binding-range prediction, and server collider LOS; P23 dialogue UI/reading-rhythm guardrails cover text_key fallback, keyboard navigation, current-history focus, hidden DC/roll display controls, font-scale/text-speed client settings, clipped/scissored status rendering, and distinct dialogue/action/thought/status styling; P24 authoring/validation guardrails cover condition/effect reference docs, JSON schemas, compiler line diagnostics, cross-registry reference validation, failure-forward lint, and a tavern-case example pack; P25 quest/feat maturation guardrails cover branch-map lines, major/minor filters, Take Root coloring, feat loadout/source/modifier visibility, journal filters, and take-root idempotency testing; P26 chime expansion guardrails cover eight attribute voices, tone guides, active thought routes, and cooldown/one-shot anti-spam; P27 NPC production guardrails cover role-specific placeholder skins, GeckoLib conversation animations, routine validation/debug, and dialogue focus pause/restore; P28 conflict expansion guardrails cover phases, leverage status, outcome effects, failure-forward/nonviolent/messy paths, dev/browser/docs/schema/JUnit/smoke coverage; P29 hardening guardrails cover save migration, session spoof/stale/contention checks, command permissions, missing-client diagnostics, dev/docs/JUnit/smoke coverage; P30 content guardrails cover 12+ block groups, 6+ NPC coverage, 4 major/8 minor branches, 12 feats, 40 chime lines, 20+ journal/clue entries, 3 conflicts, endings, docs/JUnit/smoke; P31 release packaging guardrails cover installation, dedicated server dependencies, compatible profile instructions, Modrinth/CurseForge metadata, story-pack tutorial, changelog, and license clarity; P32 guardrails cover the K-key Ebb menu, player-safe menu actions, menu/settings translations, and dialogue screens that keep the live player view visible behind the panel; P33 guardrails cover codebase-review remediation for command permissions, active feats, disadvantage/roll breakdowns, retry locks, centralized raycasts, block-group duplicates, routine hardening, and docs/JUnit coverage; P34 guardrails cover disabled/fake LLM config, deterministic fake provider, LLM chat sessions/payloads/UI skeleton, llm_chat choice wiring, /ebb llm status, no secret literals, and JUnit/GameTest coverage; P35 guardrails cover NPC profile/tier/promotion data; P36 guardrails cover gateway auth endpoints, dev local/OIDC auth providers, Minecraft auth commands, auth_required gating, server-only token storage, redaction, and gateway smoke; P37 guardrails cover OpenAI Responses gateway chat, structured/chunked output, circuit breaker, store:false privacy, mock provider, and Minecraft HTTP gateway client; P38 guardrails cover gateway DB migration, append-only memory records, facts/conflicts, deterministic embeddings, hybrid retrieval, citation ids, and Minecraft memory dev commands; P39 guardrails cover LLM-proposed memory ops, deterministic validation, episodic summaries, related-memory links, A-Mem-like evolution, A-MemGuard safety lessons, and raw/fact/conflict dev visibility; P40 guardrails cover NPC KB packs, reveal-conditioned visible-only prompt assembly, KB story effects, /ebb kb inspect, and hidden-secret acceptance tests; P41 guardrails cover minor NPC candidate detection, profile generator prompt/schema, generated knowledge seeds/options, persistence, dev review/reject/regenerate commands, and world-hour rate limiting; P42 guardrails cover LLM chat streaming UI, suggested-option GUI automation, return-to-script, memory correction, dev citations overlay, timeout/error/cancel non-stuck behavior, and K-menu auth status; P43 guardrails cover NPC profile/knowledge schemas, LLM config and memory-effect docs, secret-literal/fake-provider/hidden-KB/high-risk-effect static audits, JUnit/GameTest coverage, and GUI E2E auth-disabled/fake-chat/gateway-dry-run routes; P2 Story Variables, P3 Quest/Take-Root/Feat, P4 Chime, P5 Journal/UI rhythm, P6 Relationship/NPC routine expansion, P7 Investigation/Conflict, and P8 Playable Vertical Slice content are wired through persistence, dialogue, dev/UI, docs, demo data, smoke, and JUnit.")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except AssertionError as exc:
        print(f"GoalStaticAudit failed: {exc}", file=sys.stderr)
        raise SystemExit(1)
