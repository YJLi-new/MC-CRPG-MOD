package com.crpg.ebb.client.interaction;

import com.crpg.ebb.EbbMod;
import com.crpg.ebb.interaction.entity.SyncedEntityTarget;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class ClientEntityTargetIndex {
    private static volatile Map<UUID, SyncedEntityTarget> targets = Map.of();

    private ClientEntityTargetIndex() {
    }

    public static void rebuild(List<SyncedEntityTarget> syncedTargets) {
        Map<UUID, SyncedEntityTarget> updated = new LinkedHashMap<>();
        for (SyncedEntityTarget target : syncedTargets) {
            updated.put(target.entityUuid(), target);
        }
        targets = Map.copyOf(updated);
        EbbMod.LOGGER.debug("Client synced {} registered entity target(s).", targets.size());
    }

    public static Optional<SyncedEntityTarget> byUuid(UUID uuid) {
        return Optional.ofNullable(targets.get(uuid));
    }

    public static boolean contains(UUID uuid) {
        return targets.containsKey(uuid);
    }

    public static void clear() {
        targets = Map.of();
    }

    public static int size() {
        return targets.size();
    }
}
