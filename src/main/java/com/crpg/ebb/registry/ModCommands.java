package com.crpg.ebb.registry;

import com.crpg.ebb.EbbMod;
import com.crpg.ebb.data.NarrativeDataRegistries;
import com.crpg.ebb.dev.DevSnapshotService;
import com.crpg.ebb.network.dev.DevSnapshotPayload;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.permission.v1.PermissionPredicates;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionLevel;

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
                .then(Commands.literal("dev")
                        .requires(PermissionPredicates.require(EbbMod.id("command.dev"), PermissionLevel.GAMEMASTERS))
                        .executes(context -> sendDevSnapshot(context.getSource()))
                        .then(Commands.literal("summary")
                                .executes(context -> sendDevSummaryText(context.getSource())))));
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

    private static int sendDevSnapshot(CommandSourceStack source) {
        List<String> lines = DevSnapshotService.build(source.getServer());
        ServerPlayer player = source.getPlayer();
        if (player != null && ServerPlayNetworking.canSend(player, DevSnapshotPayload.TYPE)) {
            ServerPlayNetworking.send(player, new DevSnapshotPayload(lines));
            source.sendSuccess(() -> Component.literal("Opened Ebb developer snapshot screen with " + lines.size() + " line(s)."), false);
            return lines.size();
        }

        sendDevSummaryLines(source, lines, 16);
        return lines.size();
    }

    private static int sendDevSummaryText(CommandSourceStack source) {
        List<String> lines = DevSnapshotService.build(source.getServer());
        sendDevSummaryLines(source, lines, 32);
        return lines.size();
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
