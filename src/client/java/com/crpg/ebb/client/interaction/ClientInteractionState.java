package com.crpg.ebb.client.interaction;

import com.crpg.ebb.interaction.InteractionTarget;

import java.util.Optional;

public final class ClientInteractionState {
    private static volatile Snapshot snapshot = Snapshot.empty();

    private ClientInteractionState() {
    }

    public static Snapshot snapshot() {
        return snapshot;
    }

    static void set(Snapshot newSnapshot) {
        snapshot = newSnapshot;
    }

    static void clear() {
        snapshot = Snapshot.empty();
    }

    public record Snapshot(
            Optional<InteractionTarget> target,
            double distance,
            boolean withinInteractionRange,
            boolean lineOfSight,
            String reason
    ) {
        public static Snapshot empty() {
            return new Snapshot(Optional.empty(), Double.NaN, false, false, "no_target");
        }
    }
}
