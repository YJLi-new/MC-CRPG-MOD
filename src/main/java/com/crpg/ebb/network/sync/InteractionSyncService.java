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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InteractionSyncService {
    private static final int ENTITY_TARGET_SYNC_INTERVAL_TICKS = 20;
    private static final double ENTITY_TARGET_SYNC_RADIUS = 64.0D;
    private static final Map<UUID, Set<String>> MISSING_CLIENT_MOD_PAYLOADS = new ConcurrentHashMap<>();
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
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> MISSING_CLIENT_MOD_PAYLOADS.clear());
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
            recordMissingClientPayload(player, BlockGroupSyncPayload.TYPE.id().toString());
            return;
        }
        markPayloadPresent(player, BlockGroupSyncPayload.TYPE.id().toString());
        ServerPlayNetworking.send(player, new BlockGroupSyncPayload(new ArrayList<>(BlockGroupIndex.definitions().values())));
    }

    public static void syncEntityBindings(ServerPlayer player) {
        if (!ServerPlayNetworking.canSend(player, EntityBindingSyncPayload.TYPE)) {
            recordMissingClientPayload(player, EntityBindingSyncPayload.TYPE.id().toString());
            return;
        }
        markPayloadPresent(player, EntityBindingSyncPayload.TYPE.id().toString());
        ServerPlayNetworking.send(player, new EntityBindingSyncPayload(
                new ArrayList<>(EntityBindingRegistry.definitions().values()),
                InteractionSettings.snapshot()
        ));
    }

    public static void syncEntityTargets(ServerPlayer player) {
        if (!ServerPlayNetworking.canSend(player, EntityTargetSyncPayload.TYPE)) {
            recordMissingClientPayload(player, EntityTargetSyncPayload.TYPE.id().toString());
            return;
        }
        markPayloadPresent(player, EntityTargetSyncPayload.TYPE.id().toString());

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

    public static Map<UUID, Set<String>> missingClientModDiagnostics() {
        Map<UUID, Set<String>> snapshot = new LinkedHashMap<>();
        MISSING_CLIENT_MOD_PAYLOADS.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> snapshot.put(entry.getKey(), Set.copyOf(entry.getValue())));
        return Map.copyOf(snapshot);
    }

    public static List<String> missingClientModDiagnosticLines() {
        if (MISSING_CLIENT_MOD_PAYLOADS.isEmpty()) {
            return List.of("- none");
        }
        return MISSING_CLIENT_MOD_PAYLOADS.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> "- " + entry.getKey() + " missing_client_payloads=" + entry.getValue())
                .toList();
    }

    private static void recordMissingClientPayload(ServerPlayer player, String payloadId) {
        MISSING_CLIENT_MOD_PAYLOADS.computeIfAbsent(player.getUUID(), ignored -> ConcurrentHashMap.newKeySet()).add(payloadId);
        EbbMod.LOGGER.warn("Skipping Ebb sync payload {} to {}; client does not advertise it. If this is a dedicated server, the player may be missing the Ebb client mod.",
                payloadId, player.getName().getString());
    }

    private static void markPayloadPresent(ServerPlayer player, String payloadId) {
        Set<String> missing = MISSING_CLIENT_MOD_PAYLOADS.get(player.getUUID());
        if (missing == null) {
            return;
        }
        missing.remove(payloadId);
        if (missing.isEmpty()) {
            MISSING_CLIENT_MOD_PAYLOADS.remove(player.getUUID());
        }
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
