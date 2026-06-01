#!/usr/bin/env python3
"""Static audit for the active GOAL.md implementation track.

The GOAL document is larger than the previous review reports. This audit grows
phase-by-phase; it currently gates P2 Story Variables, P3 Quest Branch /
Take Root / Feat MVP, P4 Chime / Inner Voice MVP, P5 Journal/UI rhythm,
P6 relationship/NPC-memory/routine wiring, P7 investigation/conflict, and
P8 playable vertical-slice content minimums.
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

    print("GoalStaticAudit passed: P2 Story Variables, P3 Quest/Take-Root/Feat, P4 Chime, P5 Journal/UI rhythm, P6 Relationship/NPC routine expansion, P7 Investigation/Conflict, and P8 Playable Vertical Slice content are wired through persistence, dialogue, dev/UI, docs, demo data, smoke, and JUnit.")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except AssertionError as exc:
        print(f"GoalStaticAudit failed: {exc}", file=sys.stderr)
        raise SystemExit(1)
