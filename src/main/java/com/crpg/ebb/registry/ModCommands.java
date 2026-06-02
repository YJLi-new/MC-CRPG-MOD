package com.crpg.ebb.registry;

import com.crpg.ebb.attribute.AttributeDefinition;
import com.crpg.ebb.attribute.AttributeRegistry;
import com.crpg.ebb.EbbMod;
import com.crpg.ebb.data.NarrativeDataRegistries;
import com.crpg.ebb.dev.DialogueDebugDumper;
import com.crpg.ebb.dev.DevSnapshotService;
import com.crpg.ebb.dialogue.DialogueRegistry;
import com.crpg.ebb.dialogue.DialogueService;
import com.crpg.ebb.dialogue.DialogueSession;
import com.crpg.ebb.interaction.BlockGroupDefinition;
import com.crpg.ebb.interaction.BlockGroupIndex;
import com.crpg.ebb.interaction.entity.EntityBindingDefinition;
import com.crpg.ebb.interaction.entity.EntityBindingRegistry;
import com.crpg.ebb.journal.JournalService;
import com.crpg.ebb.network.dev.DevSnapshotPayload;
import com.crpg.ebb.network.journal.JournalPayload;
import com.crpg.ebb.network.quest.QuestTreePayload;
import com.crpg.ebb.npc.EbbNpcEntity;
import com.crpg.ebb.npc.ModEntityTypes;
import com.crpg.ebb.quest.QuestTreeService;
import com.crpg.ebb.routine.NpcRoutineDefinition;
import com.crpg.ebb.routine.NpcRoutineRegistry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.permission.v1.PermissionPredicates;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import com.crpg.ebb.state.NarrativeSavedData;
import com.crpg.ebb.state.PlayerNarrativeState;
import com.crpg.ebb.story.StoryVarLayer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class ModCommands {
    private static final Gson PRETTY_GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter EXPORT_TIMESTAMP_FORMAT = DateTimeFormatter
            .ofPattern("yyyyMMdd-HHmmss")
            .withLocale(Locale.ROOT)
            .withZone(ZoneOffset.UTC);
    private static final double DEV_INSPECT_RANGE = 10.0D;

    private ModCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registerEbbCommand(dispatcher));
    }

    private static void registerEbbCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(EbbMod.MOD_ID)
                .executes(context -> sendStatus(context.getSource()))
                .then(Commands.literal("status")
                        .executes(context -> sendStatus(context.getSource())))
                .then(Commands.literal("data")
                        .executes(context -> sendDataSummary(context.getSource())))
                .then(createAttributesCommand("attributes"))
                .then(createAttributesCommand("attr"))
                .then(Commands.literal("dev")
                        .requires(PermissionPredicates.require(EbbMod.id("command.dev"), PermissionLevel.GAMEMASTERS))
                        .executes(context -> sendDevSnapshot(context.getSource()))
                        .then(Commands.literal("on")
                                .executes(context -> setDevMode(context.getSource(), true)))
                        .then(Commands.literal("off")
                                .executes(context -> setDevMode(context.getSource(), false)))
                        .then(Commands.literal("summary")
                                .executes(context -> sendDevSummaryText(context.getSource()))))
                .then(Commands.literal("dialogue")
                        .then(Commands.literal("inspect")
                                .requires(PermissionPredicates.require(EbbMod.id("command.dialogue"), PermissionLevel.GAMEMASTERS))
                                .then(Commands.literal("current")
                                        .executes(context -> inspectCurrentTarget(context.getSource())))
                                .then(Commands.literal("entity")
                                        .then(Commands.argument("entity", EntityArgument.entity())
                                                .executes(context -> inspectEntityTarget(
                                                        context.getSource(),
                                                        EntityArgument.getEntity(context, "entity")))))
                                .then(Commands.literal("dialogue")
                                        .then(Commands.argument("dialogue_id", StringArgumentType.string())
                                                .executes(context -> inspectDialogueById(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "dialogue_id"))))))
                        .then(Commands.literal("tree")
                                .requires(PermissionPredicates.require(EbbMod.id("command.dialogue"), PermissionLevel.GAMEMASTERS))
                                .then(Commands.argument("dialogue_id", StringArgumentType.string())
                                        .executes(context -> inspectDialogueById(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "dialogue_id")))))
                        .then(Commands.literal("vars")
                                .executes(context -> sendDialogueVars(context.getSource(), context.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> sendDialogueVars(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "player")))))
                        .then(Commands.literal("reload")
                                .requires(PermissionPredicates.require(EbbMod.id("command.dialogue"), PermissionLevel.GAMEMASTERS))
                                .executes(context -> reloadDialogueData(context.getSource()))))
                .then(Commands.literal("vars")
                        .executes(context -> sendDialogueVars(context.getSource(), context.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .requires(PermissionPredicates.require(EbbMod.id("command.dialogue"), PermissionLevel.GAMEMASTERS))
                                .executes(context -> sendDialogueVars(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "player")))))
                .then(Commands.literal("quest")
                        .executes(context -> sendQuestTree(context.getSource()))
                        .then(Commands.literal("tree")
                                .executes(context -> sendQuestTree(context.getSource()))))
                .then(Commands.literal("journal")
                        .executes(context -> sendJournal(context.getSource())))
                .then(Commands.literal("routine")
                        .requires(PermissionPredicates.require(EbbMod.id("command.routine"), PermissionLevel.GAMEMASTERS))
                        .then(Commands.literal("inspect")
                                .then(Commands.argument("entity", EntityArgument.entity())
                                        .executes(context -> inspectRoutine(
                                                context.getSource(),
                                                EntityArgument.getEntity(context, "entity"))))))
                .then(Commands.literal("export")
                        .requires(PermissionPredicates.require(EbbMod.id("command.export"), PermissionLevel.GAMEMASTERS))
                        .then(Commands.literal("save-debug")
                                .executes(context -> exportSaveDebug(context.getSource()))))
                .then(Commands.literal("summon_npc")
                        .requires(PermissionPredicates.require(EbbMod.id("command.summon_npc"), PermissionLevel.GAMEMASTERS))
                        .then(Commands.argument("routine", StringArgumentType.string())
                                .executes(context -> summonNpc(context.getSource(), StringArgumentType.getString(context, "routine"))))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createAttributesCommand(String literal) {
        return Commands.literal(literal)
                .executes(context -> showAttributes(context.getSource()))
                .then(Commands.literal("spend")
                        .then(Commands.argument("attribute", StringArgumentType.word())
                                .then(Commands.argument("points", IntegerArgumentType.integer(1))
                                        .executes(context -> spendAttributePoints(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "attribute"),
                                                IntegerArgumentType.getInteger(context, "points")
                                        )))))
                .then(Commands.literal("grant")
                        .requires(PermissionPredicates.require(EbbMod.id("command.attributes.grant"), PermissionLevel.GAMEMASTERS))
                        .then(Commands.argument("points", IntegerArgumentType.integer(0))
                                .executes(context -> grantAttributePoints(
                                        context.getSource(),
                                        IntegerArgumentType.getInteger(context, "points")
                                ))))
                .then(Commands.literal("set")
                        .requires(PermissionPredicates.require(EbbMod.id("command.attributes.set"), PermissionLevel.GAMEMASTERS))
                        .then(Commands.argument("attribute", StringArgumentType.word())
                                .then(Commands.argument("score", IntegerArgumentType.integer(-5, 10))
                                        .executes(context -> setAttributeScore(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "attribute"),
                                                IntegerArgumentType.getInteger(context, "score")
                                        )))))
                .then(Commands.literal("reset")
                        .requires(PermissionPredicates.require(EbbMod.id("command.attributes.reset"), PermissionLevel.GAMEMASTERS))
                        .executes(context -> resetAttributes(context.getSource())));
    }

    private static int sendStatus(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("Esoteric Ebb CRPG skeleton is loaded. Use /ebb data for registry counts."), false);
        return 1;
    }

    private static int sendDataSummary(CommandSourceStack source) {
        String summary = NarrativeDataRegistries.summaryLine();
        source.sendSuccess(() -> Component.literal(summary), false);
        return NarrativeDataRegistries.totalEntryCount();
    }

    private static int showAttributes(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("/ebb attributes can only be used by a player for now."));
            return 0;
        }
        NarrativeSavedData state = NarrativeSavedData.get((ServerLevel) player.level());
        source.sendSuccess(() -> Component.literal("Ebb DND-8 attributes for " + player.getName().getString()
                + " | unspent points=" + state.getAttributePoints(player.getUUID())), false);
        for (AttributeDefinition definition : AttributeRegistry.orderedDefinitions()) {
            source.sendSuccess(() -> Component.literal("- " + definition.key()
                    + " (" + definition.displayName() + ") = "
                    + state.getAttribute(player.getUUID(), definition.key())
                    + aliasSuffix(definition)), false);
        }
        source.sendSuccess(() -> Component.literal("Spend with: /ebb attributes spend <attribute> <points>"), false);
        return AttributeRegistry.size();
    }

    private static int spendAttributePoints(CommandSourceStack source, String attribute, int points) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("/ebb attributes spend can only be used by a player for now."));
            return 0;
        }
        NarrativeSavedData state = NarrativeSavedData.get((ServerLevel) player.level());
        NarrativeSavedData.SpendResult result = state.spendAttributePoint(player.getUUID(), attribute, points);
        if (!result.success()) {
            source.sendFailure(Component.literal("Cannot spend attribute points: " + result.reason()
                    + " (score=" + result.score() + ", unspent=" + result.remainingPoints() + ")"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Spent " + points + " point(s) on " + result.reason()
                + ": score=" + result.score() + ", unspent=" + result.remainingPoints()), false);
        return result.remainingPoints();
    }

    private static int grantAttributePoints(CommandSourceStack source, int points) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("/ebb attributes grant can only be used by a player for now."));
            return 0;
        }
        NarrativeSavedData state = NarrativeSavedData.get((ServerLevel) player.level());
        state.addAttributePoints(player.getUUID(), points);
        source.sendSuccess(() -> Component.literal("Granted " + points + " Ebb attribute point(s). Unspent="
                + state.getAttributePoints(player.getUUID())), true);
        return state.getAttributePoints(player.getUUID());
    }

    private static int setAttributeScore(CommandSourceStack source, String attribute, int score) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("/ebb attributes set can only be used by a player for now."));
            return 0;
        }
        NarrativeSavedData state = NarrativeSavedData.get((ServerLevel) player.level());
        state.setAttribute(player.getUUID(), attribute, score);
        String canonical = AttributeRegistry.canonicalKey(attribute);
        source.sendSuccess(() -> Component.literal("Set " + canonical + " to "
                + state.getAttribute(player.getUUID(), canonical)), true);
        return state.getAttribute(player.getUUID(), canonical);
    }

    private static int resetAttributes(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("/ebb attributes reset can only be used by a player for now."));
            return 0;
        }
        NarrativeSavedData state = NarrativeSavedData.get((ServerLevel) player.level());
        state.resetAttributes(player.getUUID());
        source.sendSuccess(() -> Component.literal("Reset Ebb DND-8 attributes. Unspent="
                + PlayerNarrativeState.DEFAULT_ATTRIBUTE_POINTS), true);
        return PlayerNarrativeState.DEFAULT_ATTRIBUTE_POINTS;
    }

    private static String aliasSuffix(AttributeDefinition definition) {
        return definition.aliases().isEmpty() ? "" : " aliases=" + definition.aliases();
    }

    private static int sendDevSnapshot(CommandSourceStack source) {
        List<String> lines = DevSnapshotService.build(source.getServer());
        ServerPlayer player = source.getPlayer();
        if (player != null && ServerPlayNetworking.canSend(player, DevSnapshotPayload.TYPE)) {
            ServerPlayNetworking.send(player, new DevSnapshotPayload(lines));
            source.sendSuccess(() -> Component.literal("Opened Ebb developer tree browser with " + lines.size() + " line(s)."), false);
            return lines.size();
        }

        sendDevSummaryLines(source, lines, 16);
        return lines.size();
    }

    private static int sendDevSummaryText(CommandSourceStack source) {
        List<String> lines = DevSnapshotService.build(source.getServer());
        sendDevSummaryLines(source, lines, 64);
        return lines.size();
    }

    private static int setDevMode(CommandSourceStack source, boolean enabled) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("/ebb dev on/off can only be used by a player."));
            return 0;
        }
        NarrativeSavedData state = NarrativeSavedData.get((ServerLevel) player.level());
        state.setPlayerFlag(player.getUUID(), "ebb.dev_mode", enabled);
        source.sendSuccess(() -> Component.literal("Ebb developer mode " + (enabled ? "enabled" : "disabled")
                + " for " + player.getName().getString() + "."), true);
        return enabled ? 1 : 0;
    }

    private static int inspectCurrentTarget(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("/ebb dialogue inspect current can only be used by a player."));
            return 0;
        }
        Optional<TargetInspection> inspection = detectServerFocus(player);
        if (inspection.isEmpty()) {
            source.sendFailure(Component.literal("No registered Ebb target in the current 10-block view ray."));
            return 0;
        }

        sendInspectionLines(source, inspection.get());
        DialogueService.currentSessionForPlayer(player.getUUID())
                .ifPresent(session -> source.sendSuccess(() -> Component.literal("active_session: "
                        + session.dialogueId() + " node=" + session.nodeId()
                        + " target=" + session.targetId()
                        + " conversation=" + session.conversationId()), false));
        return 1;
    }

    private static int inspectEntityTarget(CommandSourceStack source, Entity entity) {
        EntityBindingDefinition binding = EntityBindingRegistry.resolve(entity).orElse(null);
        if (binding == null) {
            source.sendFailure(Component.literal("Entity " + entity.getStringUUID() + " has no Ebb entity binding."));
            return 0;
        }

        TargetInspection inspection = new TargetInspection(
                "entity",
                binding.dialogueId(),
                "entity=" + entity.getStringUUID()
                        + " type=" + net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType())
                        + " name=" + entity.getName().getString(),
                "binding=" + binding.debugSummary()
        );
        sendInspectionLines(source, inspection);
        return 1;
    }

    private static int inspectDialogueById(CommandSourceStack source, String rawDialogueId) {
        Identifier dialogueId;
        try {
            dialogueId = parseEbbIdentifier(rawDialogueId);
        } catch (RuntimeException ex) {
            source.sendFailure(Component.literal("Invalid dialogue id: " + rawDialogueId));
            return 0;
        }

        List<String> lines = new ArrayList<>();
        boolean found = DialogueDebugDumper.appendDialogueTree(lines, dialogueId);
        sendDevSummaryLines(source, lines, 128);
        if (!found) {
            source.sendFailure(Component.literal("Known dialogue count=" + DialogueRegistry.size()));
            return 0;
        }
        return lines.size();
    }

    private static int sendDialogueVars(CommandSourceStack source, ServerPlayer target) {
        NarrativeSavedData state = NarrativeSavedData.get((ServerLevel) target.level());
        PlayerNarrativeState playerState = state.player(target.getUUID());
        source.sendSuccess(() -> Component.literal("Dialogue vars for " + target.getName().getString()
                + " (" + target.getUUID() + ")"), false);
        source.sendSuccess(() -> Component.literal("- unspent_attribute_points=" + playerState.attributePoints()), false);
        if (playerState.variables().isEmpty()) {
            source.sendSuccess(() -> Component.literal("- player_variables: none"), false);
        } else {
            source.sendSuccess(() -> Component.literal("- player_variables:"), false);
            playerState.variables().forEach((key, value) ->
                    source.sendSuccess(() -> Component.literal("  " + key + " = " + value), false));
        }
        sendStoryVarLayer(source, "- story.branch", playerState.storyVariables(StoryVarLayer.BRANCH));
        sendStoryVarLayer(source, "- story.major", playerState.storyVariables(StoryVarLayer.MAJOR));
        sendStoryVarLayer(source, "- story.minor", playerState.storyVariables(StoryVarLayer.MINOR));
        if (playerState.flags().isEmpty()) {
            source.sendSuccess(() -> Component.literal("- player_flags/tags: none"), false);
        } else {
            source.sendSuccess(() -> Component.literal("- player_flags/tags:"), false);
            playerState.flags().stream().sorted().limit(96).forEach(flag ->
                    source.sendSuccess(() -> Component.literal("  " + flag), false));
            if (playerState.flags().size() > 96) {
                source.sendSuccess(() -> Component.literal("  ... " + (playerState.flags().size() - 96) + " more flag(s)"), false);
            }
        }
        DialogueService.currentSessionForPlayer(target.getUUID())
                .ifPresentOrElse(
                        session -> source.sendSuccess(() -> Component.literal("- active_session: "
                                + session.dialogueId() + " node=" + session.nodeId()
                                + " target=" + session.targetId()), false),
                        () -> source.sendSuccess(() -> Component.literal("- active_session: none"), false)
                );
        return playerState.variables().size()
                + playerState.storyVariables(StoryVarLayer.BRANCH).size()
                + playerState.storyVariables(StoryVarLayer.MAJOR).size()
                + playerState.storyVariables(StoryVarLayer.MINOR).size()
                + playerState.flags().size();
    }

    private static void sendStoryVarLayer(CommandSourceStack source, String label, Map<String, String> variables) {
        if (variables.isEmpty()) {
            source.sendSuccess(() -> Component.literal(label + ": none"), false);
            return;
        }
        source.sendSuccess(() -> Component.literal(label + ":"), false);
        variables.entrySet().stream().sorted(Map.Entry.comparingByKey()).limit(96).forEach(entry ->
                source.sendSuccess(() -> Component.literal("  " + entry.getKey() + " = " + entry.getValue()), false));
        if (variables.size() > 96) {
            source.sendSuccess(() -> Component.literal("  ... " + (variables.size() - 96) + " more story var(s)"), false);
        }
    }

    private static int reloadDialogueData(CommandSourceStack source) {
        List<String> selectedPacks = List.copyOf(source.getServer().getPackRepository().getSelectedIds());
        source.sendSuccess(() -> Component.literal("Scheduling Minecraft resource reload for Ebb dialogue/interactable data. selected_packs="
                + selectedPacks), true);
        source.getServer().reloadResources(selectedPacks)
                .thenRun(() -> source.getServer().execute(() ->
                        source.sendSuccess(() -> Component.literal("Ebb dialogue/interactable resource reload finished. "
                                + NarrativeDataRegistries.summaryLine()), true)))
                .exceptionally(error -> {
                    source.getServer().execute(() ->
                            source.sendFailure(Component.literal("Ebb resource reload failed: " + error.getMessage())));
                    return null;
                });
        return 1;
    }

    private static int sendQuestTree(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("/ebb quest can only be used by a player for now."));
            return 0;
        }
        List<String> lines = QuestTreeService.build(player);
        if (ServerPlayNetworking.canSend(player, QuestTreePayload.TYPE)) {
            ServerPlayNetworking.send(player, new QuestTreePayload(lines));
        } else {
            sendDevSummaryLines(source, lines, 128);
        }
        return lines.size();
    }

    private static int sendJournal(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("/ebb journal can only be used by a player for now."));
            return 0;
        }
        List<String> lines = JournalService.build(player);
        if (ServerPlayNetworking.canSend(player, JournalPayload.TYPE)) {
            ServerPlayNetworking.send(player, new JournalPayload(lines));
        } else {
            sendDevSummaryLines(source, lines, 128);
        }
        return lines.size();
    }

    private static int inspectRoutine(CommandSourceStack source, Entity entity) {
        if (!(entity instanceof EbbNpcEntity npc)) {
            source.sendFailure(Component.literal("Entity " + entity.getStringUUID() + " is not an ebb:npc."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Ebb NPC routine inspect: entity=" + npc.getStringUUID()
                + " name=" + npc.getName().getString()), false);
        source.sendSuccess(() -> Component.literal("- routine_id=" + npc.routineId().map(Identifier::toString).orElse("-")), false);
        source.sendSuccess(() -> Component.literal("- narrative_key=" + npc.narrativeStateKey()
                + " pose=" + npc.narrativePose()
                + " animation=" + npc.narrativeAnimation()), false);
        source.sendSuccess(() -> Component.literal("- path_key=" + (npc.routinePathKey().isBlank() ? "-" : npc.routinePathKey())
                + " path_index=" + npc.routinePathIndex()), false);
        source.sendSuccess(() -> Component.literal("- pos=" + npc.position()
                + " navigation_done=" + npc.getNavigation().isDone()), false);

        if (npc.routineId().isPresent()) {
            Identifier routineId = npc.routineId().get();
            Optional<NpcRoutineDefinition> routine = NpcRoutineRegistry.byId(routineId);
            if (routine.isEmpty()) {
                source.sendFailure(Component.literal("Routine " + routineId + " is not loaded."));
                return 0;
            }
            long time = source.getLevel().getOverworldClockTime();
            routine.get().stepForTime(time).ifPresent(step ->
                    source.sendSuccess(() -> Component.literal("- current_step@time=" + Math.floorMod(time, 24000L)
                            + ": " + step.debugSummary()), false));
            List<String> lines = new ArrayList<>();
            DialogueDebugDumper.appendRoutine(lines, routineId);
            sendDevSummaryLines(source, lines, 96);
            return lines.size();
        }
        return 1;
    }

    private static int exportSaveDebug(CommandSourceStack source) {
        NarrativeSavedData state = NarrativeSavedData.get(source.getServer());
        Path directory = source.getServer().getWorldPath(LevelResource.ROOT).resolve("ebb-debug-exports");
        String timestamp = EXPORT_TIMESTAMP_FORMAT.format(Instant.now());
        Path file = directory.resolve("narrative-state-" + timestamp + ".json");
        try {
            Files.createDirectories(directory);
            Files.writeString(file, PRETTY_GSON.toJson(state.debugSnapshot()), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            source.sendFailure(Component.literal("Failed to export Ebb debug snapshot: " + ex.getMessage()));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Exported Ebb debug snapshot to " + file.toAbsolutePath()), true);
        return 1;
    }

    private static Optional<TargetInspection> detectServerFocus(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(DEV_INSPECT_RANGE));

        BlockHitResult blockHit = player.level().clip(new ClipContext(
                eye,
                end,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                player
        ));
        double blockDistanceSqr = hitDistanceSqr(eye, blockHit);
        Optional<BlockGroupDefinition> blockGroup = blockHit.getType() == HitResult.Type.BLOCK
                ? BlockGroupIndex.byBlock(player.level().dimension(), blockHit.getBlockPos())
                : Optional.empty();

        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(DEV_INSPECT_RANGE)).inflate(1.0D);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                player,
                eye,
                end,
                searchBox,
                entity -> entity != player
                        && entity.isPickable()
                        && !entity.isSpectator()
                        && EntityBindingRegistry.resolve(entity).isPresent(),
                DEV_INSPECT_RANGE * DEV_INSPECT_RANGE
        );
        double entityDistanceSqr = entityHit == null ? Double.POSITIVE_INFINITY : eye.distanceToSqr(entityHit.getLocation());

        if (entityHit != null && entityDistanceSqr <= blockDistanceSqr) {
            Entity entity = entityHit.getEntity();
            return EntityBindingRegistry.resolve(entity).map(binding -> new TargetInspection(
                    "entity",
                    binding.dialogueId(),
                    "entity=" + entity.getStringUUID()
                            + " type=" + net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType())
                            + " distance=" + String.format(Locale.ROOT, "%.2f", Math.sqrt(entityDistanceSqr)),
                    "binding=" + binding.debugSummary()
            ));
        }
        return blockGroup.map(definition -> new TargetInspection(
                "block_group",
                definition.dialogueId(),
                "block_group=" + definition.id()
                        + " dimension=" + definition.dimension().identifier()
                        + " block_count=" + definition.blocks().size(),
                "interaction_point=" + definition.interactionPoint()
                        + " bounds=" + definition.bounds()
        ));
    }

    private static void sendInspectionLines(CommandSourceStack source, TargetInspection inspection) {
        source.sendSuccess(() -> Component.literal("Ebb target inspection: type=" + inspection.kind()
                + " dialogue=" + inspection.dialogueId()), false);
        source.sendSuccess(() -> Component.literal("- " + inspection.summary()), false);
        source.sendSuccess(() -> Component.literal("- " + inspection.detail()), false);
        List<String> lines = new ArrayList<>();
        DialogueDebugDumper.appendDialogueTree(lines, inspection.dialogueId());
        sendDevSummaryLines(source, lines, 96);
    }

    private static double hitDistanceSqr(Vec3 eye, HitResult hitResult) {
        if (hitResult == null || hitResult.getType() == HitResult.Type.MISS) {
            return Double.POSITIVE_INFINITY;
        }
        return eye.distanceToSqr(hitResult.getLocation());
    }

    private static Identifier parseEbbIdentifier(String raw) {
        return raw.contains(":") ? Identifier.parse(raw) : EbbMod.id(raw);
    }

    private static int summonNpc(CommandSourceStack source, String routine) {
        Identifier routineId;
        try {
            routineId = parseRoutineIdentifier(routine);
        } catch (RuntimeException ex) {
            source.sendFailure(Component.literal("Invalid or unknown routine id: " + routine
                    + ". Use e.g. /ebb summon_npc demo/innkeeper_day. Loaded routines="
                    + NpcRoutineRegistry.size()));
            return 0;
        }

        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());
        EbbNpcEntity npc = ModEntityTypes.NPC.spawn(level, pos, EntitySpawnReason.COMMAND);
        if (npc == null) {
            source.sendFailure(Component.literal("Failed to spawn Ebb NPC."));
            return 0;
        }
        npc.setRoutineId(routineId);
        npc.setNarrativeStateKey(narrativeKeyForRoutine(routineId));
        npc.addTag("ebb.npc");
        npc.addTag("ebb.npc." + routineId.getPath().replace('/', '.'));
        npc.addTag("ebb.npc." + narrativeTagForRoutine(routineId));
        npc.setCustomName(Component.literal("Ebb NPC: " + routineId.getPath()));
        source.sendSuccess(() -> Component.literal("Spawned Ebb NPC at " + pos.toShortString() + " with routine " + routineId), true);
        return 1;
    }

    private static Identifier parseRoutineIdentifier(String raw) {
        Identifier direct = parseEbbIdentifier(raw);
        if (NpcRoutineRegistry.byId(direct).isPresent()) {
            return direct;
        }
        if (!raw.contains(":") && !raw.startsWith("demo/")) {
            Identifier demo = EbbMod.id("demo/" + raw);
            if (NpcRoutineRegistry.byId(demo).isPresent()) {
                return demo;
            }
        }
        throw new IllegalArgumentException("unknown routine " + raw);
    }

    private static String narrativeKeyForRoutine(Identifier routineId) {
        String path = routineId.getPath();
        int slash = path.lastIndexOf('/');
        String leaf = slash >= 0 ? path.substring(slash + 1) : path;
        leaf = leaf.replaceAll("(_day|_night|_routine)$", "");
        return routineId.getNamespace() + ":" + (slash >= 0 ? path.substring(0, slash + 1) : "") + leaf;
    }

    private static String narrativeTagForRoutine(Identifier routineId) {
        String key = narrativeKeyForRoutine(routineId);
        String path = key.contains(":") ? key.substring(key.indexOf(':') + 1) : key;
        return path.replace('/', '.');
    }

    private record TargetInspection(String kind, Identifier dialogueId, String summary, String detail) {
    }

    private static void sendDevSummaryLines(CommandSourceStack source, List<String> lines, int limit) {
        int count = Math.min(lines.size(), limit);
        for (int i = 0; i < count; i++) {
            String line = lines.get(i);
            source.sendSuccess(() -> Component.literal(line), false);
        }
        if (lines.size() > count) {
            source.sendSuccess(() -> Component.literal("... " + (lines.size() - count) + " more line(s); run as a modded client player to open the browser screen."), false);
        }
    }
}
