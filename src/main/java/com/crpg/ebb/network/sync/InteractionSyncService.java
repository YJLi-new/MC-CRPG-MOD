package com.crpg.ebb.network.sync;

import com.crpg.ebb.EbbMod;
import com.crpg.ebb.interaction.BlockGroupIndex;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;

public final class InteractionSyncService {
    private InteractionSyncService() {
    }

    public static void registerLifecycleEvents() {
        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) -> syncBlockGroups(player));
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, manager, success) -> {
            if (success) {
                syncBlockGroupsToAll(server);
            }
        });
    }

    public static void syncBlockGroupsToAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncBlockGroups(player);
        }
    }

    public static void syncBlockGroups(ServerPlayer player) {
        if (!ServerPlayNetworking.canSend(player, BlockGroupSyncPayload.TYPE)) {
            EbbMod.LOGGER.debug("Skipping block-group sync to {}; client does not advertise {}", player.getName().getString(), BlockGroupSyncPayload.TYPE.id());
            return;
        }
        ServerPlayNetworking.send(player, new BlockGroupSyncPayload(new ArrayList<>(BlockGroupIndex.definitions().values())));
    }
}
