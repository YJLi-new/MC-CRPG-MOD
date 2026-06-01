#!/usr/bin/env python3
"""Static source audit for the 2026-05-31 third-review runtime wiring concerns.

This intentionally checks the runtime wiring points that regressed in the Drive
source sample cited by the third review: client prediction indexes, sync payload
registration/receivers, server sync lifecycle, mod/client entrypoints, command
surface, and dialogue effect/session semantics.
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


def require(name: str, haystack: str, *needles: str) -> None:
    missing = [needle for needle in needles if needle not in haystack]
    if missing:
        raise AssertionError(f"{name}: missing {missing}")


def forbid(name: str, haystack: str, *needles: str) -> None:
    present = [needle for needle in needles if needle in haystack]
    if present:
        raise AssertionError(f"{name}: forbidden stale pattern(s) present: {present}")


def main() -> int:
    detector = read("src/client/java/com/crpg/ebb/client/interaction/ClientTargetDetector.java")
    require(
        "ClientTargetDetector entity prediction",
        detector,
        "ClientEntityTargetIndex.byUuid(entity.getUUID())",
        "ClientEntityTargetIndex.contains(entity.getUUID())",
        "EntityBindingRegistry.isRegisteredTarget(entity)",
        "binding.dialogueId()",
        "binding.interactionRange()",
        "binding.highlightRange()",
        "targetData.dialogueId()",
        "targetData.interactionRange()",
        "targetData.highlightRange()",
    )
    require("ClientTargetDetector block groups", detector, "ClientBlockGroupIndex.byBlock")
    forbid("ClientTargetDetector stale debug fallback", detector, 'EbbMod.id("debug/entity")')

    packets = read("src/main/java/com/crpg/ebb/network/ModPackets.java")
    require(
        "ModPackets sync payload registration",
        packets,
        "BlockGroupSyncPayload.TYPE",
        "EntityBindingSyncPayload.TYPE",
        "EntityTargetSyncPayload.TYPE",
        "PayloadTypeRegistry.clientboundPlay().register(BlockGroupSyncPayload.TYPE, BlockGroupSyncPayload.CODEC)",
        "PayloadTypeRegistry.clientboundPlay().register(EntityBindingSyncPayload.TYPE, EntityBindingSyncPayload.CODEC)",
        "PayloadTypeRegistry.clientboundPlay().register(EntityTargetSyncPayload.TYPE, EntityTargetSyncPayload.CODEC)",
    )

    networking = read("src/client/java/com/crpg/ebb/client/network/ClientInteractionNetworking.java")
    require(
        "ClientInteractionNetworking sync receivers",
        networking,
        "ClientBlockGroupIndex.rebuild(payload.definitions())",
        "EntityBindingRegistry.syncFromServer(payload.definitions(), payload.settings())",
        "ClientEntityTargetIndex.rebuild(payload.targets())",
        "ClientPlayConnectionEvents.JOIN.register",
        "ClientPlayConnectionEvents.DISCONNECT.register",
        "ClientBlockGroupIndex.clear()",
        "ClientEntityTargetIndex.clear()",
        "EntityBindingRegistry.clearSynced()",
    )

    sync = read("src/main/java/com/crpg/ebb/network/sync/InteractionSyncService.java")
    require(
        "InteractionSyncService lifecycle",
        sync,
        "ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register",
        "ServerLifecycleEvents.END_DATA_PACK_RELOAD.register",
        "ServerTickEvents.END_SERVER_TICK.register",
        "syncBlockGroups(player)",
        "syncEntityBindings(player)",
        "syncEntityTargets(player)",
        "new EntityBindingSyncPayload",
        "new EntityTargetSyncPayload",
        "EntityBindingRegistry.resolve(entity)",
    )

    mod = read("src/main/java/com/crpg/ebb/EbbMod.java")
    require(
        "EbbMod entrypoint",
        mod,
        "ModEntityTypes.register()",
        "ModPackets.register()",
        "NarrativeDataRegistries.registerReloadListeners()",
        "DialogueService.registerLifecycleEvents()",
        "InteractionSyncService.registerLifecycleEvents()",
        "ModCommands.register()",
    )

    client = read("src/client/java/com/crpg/ebb/client/EbbClient.java")
    require("EbbClient entrypoint", client, "ModEntityRenderers.register()")

    commands = read("src/main/java/com/crpg/ebb/registry/ModCommands.java")
    require(
        "ModCommands player/dev commands",
        commands,
        'createAttributesCommand("attributes")',
        'createAttributesCommand("attr")',
        'Commands.literal("spend")',
        'Commands.literal("grant")',
        'Commands.literal("set")',
        'Commands.literal("reset")',
        'Commands.literal("summon_npc")',
        'Commands.literal("dev")',
        'Commands.literal("on")',
        'Commands.literal("off")',
        'Commands.literal("dialogue")',
        'Commands.literal("inspect")',
        'Commands.literal("tree")',
        'Commands.literal("vars")',
        'Commands.literal("reload")',
        'Commands.literal("routine")',
        'Commands.literal("save-debug")',
        "DialogueDebugDumper.appendDialogueTree",
        "DialogueService.currentSessionForPlayer",
        "exportSaveDebug",
        "detectServerFocus",
        "ModEntityTypes.NPC.spawn",
        'npc.addTag("ebb.npc")',
    )

    check = read("src/main/java/com/crpg/ebb/dialogue/DialogueCheck.java")
    require(
        "DialogueCheck outcome effects",
        check,
        "successEffects",
        "failureEffects",
        "criticalSuccessEffects",
        "criticalFailureEffects",
        '"success_effects"',
        '"failure_effects"',
        '"critical_success_effects"',
        '"critical_failure_effects"',
        "effectsForOutcome",
    )

    node = read("src/main/java/com/crpg/ebb/dialogue/DialogueNode.java")
    require("DialogueNode enter effects", node, "enterEffects", '"enter_effects"')

    service = read("src/main/java/com/crpg/ebb/dialogue/DialogueService.java")
    require(
        "DialogueService lifecycle/effects",
        service,
        "ServerPlayConnectionEvents.DISCONNECT.register",
        "ServerPlayerEvents.AFTER_RESPAWN.register",
        "ServerPlayerEvents.LEAVE.register",
        "ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register",
        "ServerLifecycleEvents.SERVER_STOPPING.register",
        "ServerTickEvents.END_SERVER_TICK.register",
        "choice.get().type() == ChoiceType.ACTION && choice.get().revalidateTarget()",
        "applyEffects(choice.get().effects()",
        "applyEffects(resolution.outcomeEffects()",
        "applyEffects(next.get().enterEffects()",
        "activeConversationPlayerForEntity",
    )

    screen = read("src/client/java/com/crpg/ebb/client/gui/dialogue/DialogueScreen.java")
    require(
        "DialogueScreen dialogue log",
        screen,
        "List<HistoryEntry> history",
        "appendHistoryEntry",
        "lastHistoryKey",
        "entry.speaker() + \": \"",
        "textScroll",
    )

    routine_controller = read("src/main/java/com/crpg/ebb/routine/NpcRoutineController.java")
    require(
        "NPC routine conversation focus",
        routine_controller,
        "applyConversationFocus",
        "DialogueService.activeConversationPlayerForEntity",
        "npc.getNavigation().stop()",
        "npc.getLookControl().setLookAt",
        "!config.requiresLineOfSight() || npc.hasLineOfSight(player)",
    )

    routine_definition = read("src/main/java/com/crpg/ebb/routine/NpcRoutineDefinition.java")
    require(
        "NPC routine look policy",
        routine_definition,
        "requires_line_of_sight",
        "requiresLineOfSight",
        "record LookAtPlayer(boolean enabled, double range, float maxYawSpeed, boolean requiresLineOfSight)",
    )

    saved_data = read("src/main/java/com/crpg/ebb/state/NarrativeSavedData.java")
    require(
        "Narrative saved-data schema version",
        saved_data,
        "CURRENT_SCHEMA_VERSION",
        'optionalFieldOf("version"',
        "schemaVersion()",
        'root.addProperty("version", version)',
    )

    authoring = read("scripts/compile_authoring_sources.py")
    require(
        "Authoring compiler",
        authoring,
        "compile_dialogues",
        "compile_interactables",
        "compile_npc",
        "fail forward",
        "build/generated/ebb_authoring/data/ebb",
        "requires_line_of_sight",
    )

    smoke = read("scripts/smoke/DeepResearchSmoke.java")
    require(
        "Deep research smoke",
        smoke,
        "DeepResearchSmoke passed",
        "RollMode.ONE_SHOT",
        "failureEffects().size()",
        "set_var effect writes player variable",
        "compiled YAML preserves node type",
    )

    junit = read("src/test/java/com/crpg/ebb/DeepResearchDataTest.java")
    require(
        "JUnit deep research coverage",
        junit,
        "bundledRegistriesLoadWithoutValidationMessages",
        "checkedChoicesMustFailForward",
        "variablesTraitsThoughtsAndUnlockEffectsPersistInNarrativeState",
        "narrativeVariablesRoundTripThroughSavedDataCodec",
        "authoringCompilerOutputLoadsAsRuntimeData",
        "RollMode.ONE_SHOT",
        "DialogueNodeType.LINE",
    )

    gametest = read("src/main/java/com/crpg/ebb/test/EbbGameTests.java")
    require(
        "Fabric GameTest coverage",
        gametest,
        "@GameTest",
        "bundledDataRegistriesAreValid",
        "ebbNpcSpawnsWithRoutineState",
        "taggedEbbNpcResolvesConfiguredBinding",
        "EntityBindingRegistry.resolve",
        'npc.addTag("ebb.npc")',
    )

    mod_json = read("src/main/resources/fabric.mod.json")
    require("Fabric GameTest entrypoint", mod_json, '"fabric-gametest"', "com.crpg.ebb.test.EbbGameTests")

    build_gradle = read("build.gradle")
    require(
        "Gradle verification tasks",
        build_gradle,
        "testImplementation platform('org.junit:junit-bom",
        "useJUnitPlatform()",
        "compileEbbAuthoring",
        "validateEbbData",
        "gametestServer",
        "-Dfabric-api.gametest.report-file",
    )

    workflow = read(".github/workflows/build.yml")
    require(
        "GitHub Actions validation",
        workflow,
        "java-version: '25'",
        "Compile authoring sources",
        "Validate data and run unit tests",
        "Run Fabric game tests",
        "Build mod jar",
    )

    print("ThirdReviewStaticAudit passed: runtime wiring and documented command/effect surfaces are present.")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except AssertionError as exc:
        print(f"ThirdReviewStaticAudit failed: {exc}", file=sys.stderr)
        raise SystemExit(1)
