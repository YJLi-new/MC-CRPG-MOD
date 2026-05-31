package com.crpg.ebb.network.sync;

import com.crpg.ebb.EbbMod;
import com.crpg.ebb.interaction.BlockGroupIndex;
import com.crpg.ebb.interaction.InteractionSettings;
import com.crpg.ebb.interaction.InteractionSyncLimits;
import com.crpg.ebb.interaction.entity.EntityBindingRegistry;
import com.crpg.ebb.interaction.entity.SyncedEntityTarget;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class InteractionSyncService {
    private static final int ENTITY_TARGET_SYNC_INTERVAL_TICKS = 20;
    private static final double ENTITY_TARGET_SYNC_RADIUS = 64.0D;
    private static int entityTargetSyncTickCounter;

    private InteractionSyncService() {
    }

    public static void registerLifecycleEvents() {
        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) -> syncInteractionData(player));
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, manager, success) -> {
            if (success) {
                syncInteractionDataToAll(server);
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(InteractionSyncService::onServerTick);
    }

    public static void syncInteractionDataToAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncInteractionData(player);
        }
    }

    public static void syncInteractionData(ServerPlayer player) {
        syncBlockGroups(player);
        syncEntityBindings(player);
        syncEntityTargets(player);
    }

    public static void syncBlockGroups(ServerPlayer player) {
        if (!ServerPlayNetworking.canSend(player, BlockGroupSyncPayload.TYPE)) {
            EbbMod.LOGGER.debug("Skipping block-group sync to {}; client does not advertise {}", player.getName().getString(), BlockGroupSyncPayload.TYPE.id());
            return;
        }
        ServerPlayNetworking.send(player, new BlockGroupSyncPayload(new ArrayList<>(BlockGroupIndex.definitions().values())));
    }

    public static void syncEntityBindings(ServerPlayer player) {
        if (!ServerPlayNetworking.canSend(player, EntityBindingSyncPayload.TYPE)) {
            EbbMod.LOGGER.debug("Skipping entity-binding sync to {}; client does not advertise {}", player.getName().getString(), EntityBindingSyncPayload.TYPE.id());
            return;
        }
        ServerPlayNetworking.send(player, new EntityBindingSyncPayload(
                new ArrayList<>(EntityBindingRegistry.definitions().values()),
                InteractionSettings.snapshot()
        ));
    }

    public static void syncEntityTargets(ServerPlayer player) {
        if (!ServerPlayNetworking.canSend(player, EntityTargetSyncPayload.TYPE)) {
            EbbMod.LOGGER.debug("Skipping entity-target sync to {}; client does not advertise {}", player.getName().getString(), EntityTargetSyncPayload.TYPE.id());
            return;
        }

        if (InteractionSettings.enableDebugEntityFallback()) {
            // In debug fallback mode the client intentionally predicts any pickable entity via the synced settings.
            // Do not enumerate every entity in loaded chunks just to mirror that fallback.
            ServerPlayNetworking.send(player, new EntityTargetSyncPayload(List.of()));
            return;
        }

        ServerLevel level = (ServerLevel) player.level();
        Vec3 eye = player.getEyePosition();
        AABB scanBox = player.getBoundingBox().inflate(ENTITY_TARGET_SYNC_RADIUS);
        List<SyncedEntityTarget> syncedTargets = new ArrayList<>();
        List<Entity> candidates = level.getEntities(EntityTypeTest.forClass(Entity.class), scanBox, entity ->
                entity != player
                        && !entity.isRemoved()
                        && entity.isPickable()
                        && !entity.isSpectator()
        );
        for (Entity entity : candidates) {
            EntityBindingRegistry.resolve(entity).ifPresent(binding -> {
                if (syncedTargets.size() >= InteractionSyncLimits.MAX_SYNCED_ENTITY_TARGETS) {
                    return;
                }
                double range = Math.min(ENTITY_TARGET_SYNC_RADIUS, Math.max(binding.highlightRange(), binding.interactionRange()));
                if (eye.distanceToSqr(entity.getBoundingBox().getCenter()) <= range * range) {
                    syncedTargets.add(SyncedEntityTarget.from(entity, binding));
                }
            });
            if (syncedTargets.size() >= InteractionSyncLimits.MAX_SYNCED_ENTITY_TARGETS) {
                break;
            }
        }
        ServerPlayNetworking.send(player, new EntityTargetSyncPayload(syncedTargets));
    }

    private static void onServerTick(MinecraftServer server) {
        entityTargetSyncTickCounter++;
        if (entityTargetSyncTickCounter % ENTITY_TARGET_SYNC_INTERVAL_TICKS != 0) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncEntityTargets(player);
        }
    }
}
