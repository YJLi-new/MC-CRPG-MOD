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
import com.crpg.ebb.interaction.InteractionRaycastPolicy;
import com.crpg.ebb.interaction.InteractionService;
import com.crpg.ebb.interaction.InteractionValidationResult;
import com.crpg.ebb.interaction.entity.EntityBindingDefinition;
import com.crpg.ebb.interaction.entity.EntityBindingRegistry;
import com.crpg.ebb.journal.JournalService;
import com.crpg.ebb.llm.LlmChatService;
import com.crpg.ebb.llm.LlmConfig;
import com.crpg.ebb.llm.auth.DeviceAuthStartResponse;
import com.crpg.ebb.llm.auth.DeviceAuthStatusResponse;
import com.crpg.ebb.llm.auth.LlmAuthService;
import com.crpg.ebb.memory.MemoryGatewayClient;
import com.crpg.ebb.network.dev.DevSnapshotPayload;
import com.crpg.ebb.network.journal.JournalPayload;
import com.crpg.ebb.network.quest.QuestTreePayload;
import com.crpg.ebb.npc.EbbNpcEntity;
import com.crpg.ebb.npc.profile.NpcProfileDefinition;
import com.crpg.ebb.npc.profile.NpcProfileRegistry;
import com.crpg.ebb.npc.profile.NpcPromotionService;
import com.crpg.ebb.npc.ModEntityTypes;
import com.crpg.ebb.quest.QuestTreeService;
import com.crpg.ebb.routine.NpcRoutineDefinition;
import com.crpg.ebb.routine.NpcRoutineRegistry;
import com.crpg.ebb.registry.commands.EbbCommandPermissionGuards;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import java.util.UUID;

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
                .then(Commands.literal("llm")
                        .executes(context -> sendLlmStatus(context.getSource()))
                        .then(Commands.literal("status")
                                .executes(context -> sendLlmStatus(context.getSource())))
                        .then(Commands.literal("auth")
                                .executes(context -> startLlmAuth(context.getSource())))
                        .then(Commands.literal("logout")
                                .executes(context -> logoutLlmAuth(context.getSource())))
                        .then(Commands.literal("reload_config")
                                .requires(EbbCommandPermissionGuards.dev())
                                .executes(context -> reloadLlmConfig(context.getSource()))))
                .then(Commands.literal("memory")
                        .requires(EbbCommandPermissionGuards.dev())
                        .executes(context -> sendMemorySearch(context.getSource(), "", 8))
                        .then(Commands.literal("search")
                                .then(Commands.argument("query", StringArgumentType.greedyString())
                                        .executes(context -> sendMemorySearch(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "query"),
                                                8))))
                        .then(Commands.literal("inspect")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(context -> inspectMemory(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "id")))))
                        .then(Commands.literal("conflicts")
                                .executes(context -> showMemoryConflicts(context.getSource(), 25)))
                        .then(Commands.literal("episodes")
                                .executes(context -> showMemoryEpisodes(context.getSource(), 25)))
                        .then(Commands.literal("lessons")
                                .executes(context -> showMemoryLessons(context.getSource(), 25))))
                .then(createAttributesCommand("attributes"))
                .then(createAttributesCommand("attr"))
                .then(Commands.literal("dev")
                        .requires(EbbCommandPermissionGuards.dev())
                        .executes(context -> sendDevSnapshot(context.getSource()))
                        .then(Commands.literal("on")
                                .executes(context -> setDevMode(context.getSource(), true)))
                        .then(Commands.literal("off")
                                .executes(context -> setDevMode(context.getSource(), false)))
                        .then(Commands.literal("summary")
                                .executes(context -> sendDevSummaryText(context.getSource()))))
                .then(Commands.literal("dialogue")
                        .then(Commands.literal("inspect")
                                .requires(EbbCommandPermissionGuards.dialogue())
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
                                .requires(EbbCommandPermissionGuards.dialogue())
                                .then(Commands.argument("dialogue_id", StringArgumentType.string())
                                        .executes(context -> inspectDialogueById(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "dialogue_id")))))
                        .then(Commands.literal("vars")
                                .executes(context -> sendDialogueVars(context.getSource(), context.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .requires(EbbCommandPermissionGuards.dialogue())
                                        .executes(context -> sendDialogueVars(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "player")))))
                        .then(Commands.literal("reload")
                                .requires(EbbCommandPermissionGuards.dialogue())
                                .executes(context -> reloadDialogueData(context.getSource()))))
                .then(Commands.literal("vars")
                        .executes(context -> sendDialogueVars(context.getSource(), context.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .requires(EbbCommandPermissionGuards.dialogue())
                                .executes(context -> sendDialogueVars(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "player")))))
                .then(Commands.literal("quest")
                        .executes(context -> sendQuestTree(context.getSource()))
                        .then(Commands.literal("tree")
                                .executes(context -> sendQuestTree(context.getSource()))))
                .then(Commands.literal("journal")
                        .executes(context -> sendJournal(context.getSource())))
                .then(Commands.literal("npc")
                        .requires(EbbCommandPermissionGuards.dev())
                        .then(Commands.literal("profile")
                                .then(Commands.literal("target")
                                        .executes(context -> showNpcProfileTarget(context.getSource())))
                                .then(Commands.argument("npc_key", StringArgumentType.string())
                                        .executes(context -> showNpcProfileByKey(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "npc_key")))))
                        .then(Commands.literal("minorize")
                                .then(Commands.argument("entity", EntityArgument.entity())
                                        .executes(context -> minorizeEntity(
                                                context.getSource(),
                                                EntityArgument.getEntity(context, "entity")))))
                        .then(Commands.literal("promote")
                                .then(Commands.argument("entity", EntityArgument.entity())
                                        .executes(context -> promoteEntity(
                                                context.getSource(),
                                                EntityArgument.getEntity(context, "entity")))))
                        .then(Commands.literal("regenerate_profile")
                                .then(Commands.argument("npc_key", StringArgumentType.string())
                                        .executes(context -> resetPromotedProfile(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "npc_key"))))))
                .then(Commands.literal("routine")
                        .requires(EbbCommandPermissionGuards.routine())
                        .then(Commands.literal("inspect")
                                .then(Commands.argument("entity", EntityArgument.entity())
                                        .executes(context -> inspectRoutine(
                                                context.getSource(),
                                                EntityArgument.getEntity(context, "entity"))))))
                .then(Commands.literal("export")
                        .requires(EbbCommandPermissionGuards.export())
                        .then(Commands.literal("save-debug")
                                .executes(context -> exportSaveDebug(context.getSource()))))
                .then(Commands.literal("summon_npc")
                        .requires(EbbCommandPermissionGuards.summonNpc())
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
                        .requires(EbbCommandPermissionGuards.attributeGrant())
                        .then(Commands.argument("points", IntegerArgumentType.integer(0))
                                .executes(context -> grantAttributePoints(
                                        context.getSource(),
                                        IntegerArgumentType.getInteger(context, "points")
                                ))))
                .then(Commands.literal("set")
                        .requires(EbbCommandPermissionGuards.attributeSet())
                        .then(Commands.argument("attribute", StringArgumentType.word())
                                .then(Commands.argument("score", IntegerArgumentType.integer(-5, 10))
                                        .executes(context -> setAttributeScore(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "attribute"),
                                                IntegerArgumentType.getInteger(context, "score")
                                        )))))
                .then(Commands.literal("reset")
                        .requires(EbbCommandPermissionGuards.attributeReset())
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


    private static int sendMemorySearch(CommandSourceStack source, String query, int limit) {
        LlmConfig config = LlmConfig.current();
        if (!config.networkAccessAllowed()) {
            source.sendFailure(Component.literal("Ebb memory search requires LLM gateway mode and gateway_base_url."));
            return 0;
        }
        UUID playerUuid = source.getEntity() instanceof ServerPlayer player ? player.getUUID() : null;
        new MemoryGatewayClient(config).search(playerUuid, query, limit).thenAccept(result -> source.getServer().execute(() -> {
            if (!result.ok()) {
                source.sendFailure(Component.literal("Ebb memory search failed: " + result.status()));
                return;
            }
            source.sendSuccess(() -> Component.literal("Ebb memory search: " + result.lines().size()
                    + " match(es), citations=" + result.citationIds()), false);
            for (String line : result.lines()) {
                source.sendSuccess(() -> Component.literal("- " + line), false);
            }
        }));
        source.sendSuccess(() -> Component.literal("Ebb memory search requested."), false);
        return 1;
    }

    private static int inspectMemory(CommandSourceStack source, String id) {
        LlmConfig config = LlmConfig.current();
        if (!config.networkAccessAllowed()) {
            source.sendFailure(Component.literal("Ebb memory inspect requires LLM gateway mode and gateway_base_url."));
            return 0;
        }
        new MemoryGatewayClient(config).inspect(id).thenAccept(result -> source.getServer().execute(() -> {
            if (result.contains("\"error\"")) {
                source.sendFailure(Component.literal("Ebb memory inspect failed: " + result));
            } else {
                source.sendSuccess(() -> Component.literal("Ebb memory inspect: " + result), false);
            }
        }));
        source.sendSuccess(() -> Component.literal("Ebb memory inspect requested: " + id), false);
        return 1;
    }

    private static int showMemoryConflicts(CommandSourceStack source, int limit) {
        LlmConfig config = LlmConfig.current();
        if (!config.networkAccessAllowed()) {
            source.sendFailure(Component.literal("Ebb memory conflicts requires LLM gateway mode and gateway_base_url."));
            return 0;
        }
        new MemoryGatewayClient(config).conflicts(limit).thenAccept(result -> source.getServer().execute(() -> {
            if (result.contains("\"error\"")) {
                source.sendFailure(Component.literal("Ebb memory conflicts failed: " + result));
            } else {
                source.sendSuccess(() -> Component.literal("Ebb memory conflicts: " + result), false);
            }
        }));
        source.sendSuccess(() -> Component.literal("Ebb memory conflicts requested."), false);
        return 1;
    }


    private static int showMemoryEpisodes(CommandSourceStack source, int limit) {
        LlmConfig config = LlmConfig.current();
        if (!config.networkAccessAllowed()) {
            source.sendFailure(Component.literal("Ebb memory episodes requires LLM gateway mode and gateway_base_url."));
            return 0;
        }
        new MemoryGatewayClient(config).episodes(limit).thenAccept(result -> source.getServer().execute(() -> {
            if (result.contains("\"error\"")) {
                source.sendFailure(Component.literal("Ebb memory episodes failed: " + result));
            } else {
                source.sendSuccess(() -> Component.literal("Ebb memory episodes/raw summaries: " + result), false);
            }
        }));
        source.sendSuccess(() -> Component.literal("Ebb memory episodes requested."), false);
        return 1;
    }

    private static int showMemoryLessons(CommandSourceStack source, int limit) {
        LlmConfig config = LlmConfig.current();
        if (!config.networkAccessAllowed()) {
            source.sendFailure(Component.literal("Ebb memory lessons requires LLM gateway mode and gateway_base_url."));
            return 0;
        }
        new MemoryGatewayClient(config).lessons(limit).thenAccept(result -> source.getServer().execute(() -> {
            if (result.contains("\"error\"")) {
                source.sendFailure(Component.literal("Ebb memory lessons failed: " + result));
            } else {
                source.sendSuccess(() -> Component.literal("Ebb memory safety lessons: " + result), false);
            }
        }));
        source.sendSuccess(() -> Component.literal("Ebb memory lessons requested."), false);
        return 1;
    }

    private static int sendLlmStatus(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal(LlmChatService.statusLine()), false);
        source.sendSuccess(() -> Component.literal("Config path: " + LlmConfig.SERVER_CONFIG_PATH
                + " (safe fields only; no API keys or player tokens are read by the mod jar)"), false);
        ServerPlayer player = source.getPlayer();
        if (player != null) {
            source.sendSuccess(() -> Component.literal("LLM auth: " + LlmAuthService.safeStatusLine(player.getUUID())), false);
            LlmAuthService.pollDeviceAuth(player).thenAccept(status -> source.getServer().execute(() -> sendLlmAuthStatusResult(source, status, false)));
        }
        return LlmChatService.activeSessionCount();
    }

    private static int startLlmAuth(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("/ebb llm auth can only be used by a player."));
            return 0;
        }
        LlmAuthService.startDeviceAuth(player).thenAccept(response -> source.getServer().execute(() -> sendLlmAuthStartResult(source, response)));
        source.sendSuccess(() -> Component.literal("Starting Ebb LLM browser auth..."), false);
        return 1;
    }

    private static int logoutLlmAuth(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("/ebb llm logout can only be used by a player."));
            return 0;
        }
        LlmAuthService.logout(player).thenAccept(revoked -> source.getServer().execute(() ->
                source.sendSuccess(() -> Component.literal("Ebb LLM logout complete: server token removed; gateway_revoked=" + revoked), false)));
        return 1;
    }

    private static void sendLlmAuthStartResult(CommandSourceStack source, DeviceAuthStartResponse response) {
        if (!response.started()) {
            source.sendFailure(Component.literal("Ebb LLM auth start failed: " + response.error()));
            return;
        }
        source.sendSuccess(() -> Component.literal("Ebb LLM auth provider=" + response.provider()
                + " session=" + response.authSessionId()
                + " code=" + response.userCode()), false);
        source.sendSuccess(() -> Component.literal("Open this URL in your browser: " + response.verificationUrl()), false);
        source.sendSuccess(() -> Component.literal("Then run /ebb llm status to finish login. Tokens stay server-side only."), false);
    }

    private static void sendLlmAuthStatusResult(CommandSourceStack source, DeviceAuthStatusResponse status, boolean verbosePending) {
        if (status.authenticated()) {
            source.sendSuccess(() -> Component.literal("Ebb LLM auth: authenticated ("
                    + status.token().orElseThrow().redactedSummary() + ")"), false);
        } else if ("pending".equals(status.status())) {
            if (verbosePending) {
                source.sendSuccess(() -> Component.literal("Ebb LLM auth: pending; try /ebb llm status again in "
                        + status.intervalSeconds() + "s."), false);
            }
        } else if (!"not_authenticated".equals(status.error())) {
            source.sendFailure(Component.literal("Ebb LLM auth: " + status.error()));
        }
    }

    private static int reloadLlmConfig(CommandSourceStack source) {
        LlmConfig.reload();
        source.sendSuccess(() -> Component.literal("Reloaded Ebb LLM server config. " + LlmChatService.statusLine()), true);
        return 1;
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

    private static int showNpcProfileTarget(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("/ebb npc profile target can only be used by a player."));
            return 0;
        }
        Optional<Entity> entity = detectServerFocusedEntity(player);
        if (entity.isEmpty()) {
            source.sendFailure(Component.literal("No registered Ebb entity target in the current 10-block view ray."));
            return 0;
        }
        return showNpcProfileForEntity(source, entity.get());
    }

    private static int showNpcProfileForEntity(CommandSourceStack source, Entity entity) {
        EntityBindingDefinition binding = EntityBindingRegistry.resolve(entity).orElse(null);
        if (binding == null) {
            source.sendFailure(Component.literal("Entity " + entity.getStringUUID() + " has no Ebb entity binding."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Ebb NPC profile target: entity=" + entity.getStringUUID()
                + " binding=" + binding.id()
                + " tier=" + binding.npcTier().serializedName()
                + " profile=" + binding.npcProfileId().map(Identifier::toString).orElse("-")), false);
        Optional<NpcProfileDefinition> staticProfile = binding.npcProfileId().flatMap(NpcProfileRegistry::byId)
                .or(() -> NpcProfileRegistry.byEntityBinding(binding.id()));
        if (staticProfile.isPresent()) {
            sendNpcProfileDefinitionLines(source, staticProfile.get(), 96);
            return 1;
        }
        NarrativeSavedData state = NarrativeSavedData.get(source.getServer());
        Identifier promotedId = NpcPromotionService.promotedProfileId(entity);
        Optional<JsonObject> promoted = state.promotedNpcProfile(promotedId.toString());
        if (promoted.isPresent()) {
            sendPromotedProfileLines(source, promotedId, promoted.get(), 96);
            return 1;
        }
        if (NpcPromotionService.isMinorCandidate(entity)) {
            source.sendSuccess(() -> Component.literal("- minor candidate: no promoted profile yet; first LLM/fake chat will create "
                    + promotedId), false);
            return 1;
        }
        source.sendFailure(Component.literal("No NPC profile mapped for binding " + binding.id()
                + "; loaded static profiles=" + NpcProfileRegistry.size()
                + ", promoted profiles=" + state.promotedNpcProfileCount()));
        return 0;
    }

    private static int showNpcProfileByKey(CommandSourceStack source, String rawNpcKey) {
        Identifier npcKey;
        try {
            npcKey = parseEbbIdentifier(rawNpcKey);
        } catch (RuntimeException ex) {
            source.sendFailure(Component.literal("Invalid npc_key: " + rawNpcKey));
            return 0;
        }
        Optional<NpcProfileDefinition> staticProfile = NpcProfileRegistry.byId(npcKey);
        if (staticProfile.isPresent()) {
            sendNpcProfileDefinitionLines(source, staticProfile.get(), 128);
            return 1;
        }
        NarrativeSavedData state = NarrativeSavedData.get(source.getServer());
        Optional<JsonObject> promoted = state.promotedNpcProfile(npcKey.toString());
        if (promoted.isPresent()) {
            sendPromotedProfileLines(source, npcKey, promoted.get(), 128);
            return 1;
        }
        source.sendFailure(Component.literal("NPC profile not found: " + npcKey
                + " (static=" + NpcProfileRegistry.size()
                + ", promoted=" + state.promotedNpcProfileCount() + ")"));
        return 0;
    }

    private static int minorizeEntity(CommandSourceStack source, Entity entity) {
        boolean added = entity.addTag(NpcPromotionService.MINOR_NPC_TAG);
        source.sendSuccess(() -> Component.literal("Marked entity " + entity.getStringUUID()
                + " as minor NPC candidate tag=" + NpcPromotionService.MINOR_NPC_TAG
                + " added=" + added), true);
        return added ? 1 : 0;
    }

    private static int promoteEntity(CommandSourceStack source, Entity entity) {
        if (!(entity.level() instanceof ServerLevel level)) {
            source.sendFailure(Component.literal("Entity is not in a server level."));
            return 0;
        }
        UUID playerUuid = Optional.ofNullable(source.getPlayer()).map(ServerPlayer::getUUID).orElse(entity.getUUID());
        NpcPromotionService.PromotionResult result = NpcPromotionService.ensurePromotedProfile(level, entity, playerUuid, "dev_command");
        source.sendSuccess(() -> Component.literal("Promoted NPC profile: " + result.debugSummary()), true);
        return result.created() ? 1 : 0;
    }

    private static int resetPromotedProfile(CommandSourceStack source, String rawNpcKey) {
        Identifier npcKey;
        try {
            npcKey = parseEbbIdentifier(rawNpcKey);
        } catch (RuntimeException ex) {
            source.sendFailure(Component.literal("Invalid npc_key: " + rawNpcKey));
            return 0;
        }
        NarrativeSavedData state = NarrativeSavedData.get(source.getServer());
        boolean removed = state.removePromotedNpcProfile(npcKey.toString());
        source.sendSuccess(() -> Component.literal("Reset promoted profile " + npcKey
                + ": removed=" + removed + ". It will be regenerated on the next eligible minor chat."), true);
        return removed ? 1 : 0;
    }

    private static void sendNpcProfileDefinitionLines(CommandSourceStack source, NpcProfileDefinition profile, int limit) {
        List<String> lines = new ArrayList<>();
        lines.add("NPC profile " + profile.id());
        lines.add("- tier=" + profile.tier().serializedName()
                + " display='" + profile.displayName() + "'"
                + " binding=" + profile.entityBinding().map(Identifier::toString).orElse("-"));
        lines.add("- llm=" + profile.llm().debugSummary());
        lines.add("- character archetype='" + profile.character().archetype()
                + "' voice='" + profile.character().voice() + "'");
        lines.add("- values=" + profile.character().values());
        lines.add("- fears=" + profile.character().fears());
        lines.add("- speech_rules=" + profile.character().speechRules());
        lines.add("- stance=" + profile.stance().debugSummary());
        lines.add("- knowledge.initial_packs=" + profile.knowledge().initialPacks());
        lines.add("- knowledge.forbidden=" + profile.knowledge().forbiddenToRevealUntil());
        sendDevSummaryLines(source, lines, limit);
    }

    private static void sendPromotedProfileLines(CommandSourceStack source, Identifier profileId, JsonObject profile, int limit) {
        List<String> lines = new ArrayList<>();
        lines.add("Promoted NPC profile " + profileId);
        lines.add("- tier=" + jsonString(profile, "tier", "-")
                + " display='" + jsonString(profile, "display_name", profileId.toString()) + "'"
                + " entity_uuid=" + jsonString(profile, "entity_uuid", "-"));
        lines.add("- entity_type=" + jsonString(profile, "entity_type", "-")
                + " source_binding=" + jsonString(profile, "source_binding", "-"));
        if (profile.has("character") && profile.get("character").isJsonObject()) {
            JsonObject character = profile.getAsJsonObject("character");
            lines.add("- archetype=" + jsonString(character, "archetype", "-")
                    + " voice=" + jsonString(character, "voice", "-"));
        }
        if (profile.has("stance") && profile.get("stance").isJsonObject()) {
            JsonObject stance = profile.getAsJsonObject("stance");
            lines.add("- attitude=" + jsonString(stance, "default_attitude_to_player", "-")
                    + " trust=" + jsonString(stance, "trust", "0")
                    + " fear=" + jsonString(stance, "fear", "0")
                    + " resentment=" + jsonString(stance, "resentment", "0"));
        }
        sendDevSummaryLines(source, lines, limit);
    }

    private static String jsonString(JsonObject json, String key, String fallback) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : fallback;
    }

    private static Optional<Entity> detectServerFocusedEntity(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(DEV_INSPECT_RANGE));
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
        return entityHit == null ? Optional.empty() : Optional.of(entityHit.getEntity());
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
                + " visual_role=" + npc.visualRole()
                + " pose=" + npc.narrativePose()
                + " animation=" + npc.narrativeAnimation()), false);
        source.sendSuccess(() -> Component.literal("- path_key=" + (npc.routinePathKey().isBlank() ? "-" : npc.routinePathKey())
                + " path_index=" + npc.routinePathIndex()), false);
        source.sendSuccess(() -> Component.literal("- " + npc.routineDebugSummary()), false);
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
                InteractionRaycastPolicy.blockModeForDevInspect(),
                InteractionRaycastPolicy.fluidMode(),
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
            InteractionValidationResult validation = InteractionService.validateEntity(player, entity.getUUID());
            return EntityBindingRegistry.resolve(entity).map(binding -> new TargetInspection(
                    "entity",
                    binding.dialogueId(),
                    "entity=" + entity.getStringUUID()
                            + " type=" + net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType())
                            + " distance=" + String.format(Locale.ROOT, "%.2f", Math.sqrt(entityDistanceSqr))
                            + " allowed=" + validation.allowed()
                            + " reason=" + validation.reason(),
                    "binding=" + binding.debugSummary()
            ));
        }
        return blockGroup.map(definition -> {
            InteractionValidationResult validation = InteractionService.validateBlockGroup(player, definition);
            return new TargetInspection(
                    "block_group",
                    definition.dialogueId(),
                    "block_group=" + definition.id()
                            + " dimension=" + definition.dimension().identifier()
                            + " block_count=" + definition.blocks().size()
                            + " allowed=" + validation.allowed()
                            + " reason=" + validation.reason(),
                    "interaction_point=" + definition.interactionPoint()
                            + " nearest_block_center=" + definition.nearestBlockCenter(eye)
                            + " bounds=" + definition.bounds()
            );
        });
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
        npc.setVisualRole(narrativeTagForRoutine(routineId));
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
