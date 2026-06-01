package com.crpg.ebb.conflict;

import com.crpg.ebb.state.NarrativeSavedData;
import net.minecraft.resources.Identifier;

import java.util.Optional;
import java.util.UUID;

public final class ConflictService {
    private ConflictService() {
    }

    public static Optional<String> start(NarrativeSavedData state, UUID playerUuid, String rawId) {
        Identifier id = parseIdentifier(rawId);
        state.setConflictState(playerUuid, id.toString(), "active");
        state.setConflictScore(playerUuid, "stress", id.toString(), 0);
        state.setConflictScore(playerUuid, "resolve", id.toString(), 0);
        return Optional.of("conflict_started:" + id);
    }

    public static Optional<String> addStress(NarrativeSavedData state, UUID playerUuid, String rawId, int amount) {
        Identifier id = parseIdentifier(rawId);
        int next = state.addConflictScore(playerUuid, "stress", id.toString(), amount);
        ConflictRegistry.byId(id).ifPresent(definition -> {
            if (next >= definition.stressLimit()) {
                state.setConflictState(playerUuid, id.toString(), definition.failureState());
            }
        });
        return Optional.of("conflict_stress:" + id + "=" + next);
    }

    public static Optional<String> addResolve(NarrativeSavedData state, UUID playerUuid, String rawId, int amount) {
        Identifier id = parseIdentifier(rawId);
        int next = state.addConflictScore(playerUuid, "resolve", id.toString(), amount);
        ConflictRegistry.byId(id).ifPresent(definition -> {
            if (next >= definition.resolveGoal()) {
                state.setConflictState(playerUuid, id.toString(), definition.successState());
            }
        });
        return Optional.of("conflict_resolve:" + id + "=" + next);
    }

    private static Identifier parseIdentifier(String raw) {
        return raw.contains(":") ? Identifier.parse(raw) : Identifier.fromNamespaceAndPath("ebb", raw);
    }
}
