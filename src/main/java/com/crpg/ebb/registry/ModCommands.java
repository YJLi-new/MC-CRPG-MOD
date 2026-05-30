package com.crpg.ebb.registry;

import com.crpg.ebb.attribute.AttributeDefinition;
import com.crpg.ebb.attribute.AttributeRegistry;
import com.crpg.ebb.EbbMod;
import com.crpg.ebb.data.NarrativeDataRegistries;
import com.crpg.ebb.dev.DevSnapshotService;
import com.crpg.ebb.network.dev.DevSnapshotPayload;
import com.crpg.ebb.npc.EbbNpcEntity;
import com.crpg.ebb.npc.ModEntityTypes;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.permission.v1.PermissionPredicates;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import com.crpg.ebb.state.NarrativeSavedData;
import com.crpg.ebb.state.PlayerNarrativeState;

import java.util.List;

public final class ModCommands {
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
                        .then(Commands.literal("summary")
                                .executes(context -> sendDevSummaryText(context.getSource()))))
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

    private static int summonNpc(CommandSourceStack source, String routine) {
        Identifier routineId;
        try {
            routineId = Identifier.parse(routine);
        } catch (RuntimeException ex) {
            source.sendFailure(Component.literal("Invalid routine id: " + routine));
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
        npc.addTag("ebb.npc");
        npc.addTag("ebb.npc." + routineId.getPath().replace('/', '.'));
        npc.setCustomName(Component.literal("Ebb NPC: " + routineId.getPath()));
        source.sendSuccess(() -> Component.literal("Spawned Ebb NPC at " + pos.toShortString() + " with routine " + routineId), true);
        return 1;
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
