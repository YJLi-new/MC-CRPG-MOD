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
    require("Server LOS collider policy", service, "ClipContext.Block.COLLIDER", "blocked_line_of_sight")

    locked_door = load_json(ROOT / "src/main/resources/data/ebb/interactions/block_groups/demo/locked_door.json")
    if "highlight" not in locked_door or locked_door["highlight"].get("render_mode") != "merged":
        raise AssertionError("locked_door block group should exercise highlight.render_mode=merged")
    innkeeper_binding = load_json(ROOT / "src/main/resources/data/ebb/interactions/entity_bindings/demo/innkeeper_ebb_npc_name.json")
    if "highlight" not in innkeeper_binding or innkeeper_binding["highlight"].get("render_mode") != "outline":
        raise AssertionError("innkeeper name binding should exercise highlight.render_mode=outline")

    docs = read("docs/json_authoring_guide.md")
    require("Authoring docs highlight style", docs, "Optional `highlight` fields", "render_mode", "merged", "bounds", "far_opacity")


def main() -> int:
    audit_p20_p21_documentation_and_baseline()
    audit_p22_interaction_highlight_polish()

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

    print("GoalStaticAudit passed: P20/P21 documentation, baseline pins, data directories, artifact status hashes, failure-forward lint, and major Take-Root guardrails are present; P22 interaction/highlight polish guardrails cover synced highlight styles, merged block outlines, target debug reasons, binding-range prediction, and server collider LOS; P2 Story Variables, P3 Quest/Take-Root/Feat, P4 Chime, P5 Journal/UI rhythm, P6 Relationship/NPC routine expansion, P7 Investigation/Conflict, and P8 Playable Vertical Slice content are wired through persistence, dialogue, dev/UI, docs, demo data, smoke, and JUnit.")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except AssertionError as exc:
        print(f"GoalStaticAudit failed: {exc}", file=sys.stderr)
        raise SystemExit(1)
