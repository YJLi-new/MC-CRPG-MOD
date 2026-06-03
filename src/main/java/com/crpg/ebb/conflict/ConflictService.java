package com.crpg.ebb.conflict;

import com.crpg.ebb.state.NarrativeSavedData;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public final class ConflictService {
    private static final String ACTIVE_STATE = "active";
    private static final String PHASE_SETUP = "setup";
    private static final String PHASE_PRESSURE = "pressure";
    private static final String PHASE_TURN = "turn";
    private static final String PHASE_CONSEQUENCE = "consequence";
    private static final String PHASE_RESOLUTION = "resolution";

    private ConflictService() {
    }

    public static Optional<String> start(NarrativeSavedData state, UUID playerUuid, String rawId) {
        Identifier id = parseIdentifier(rawId);
        state.setConflictState(playerUuid, id.toString(), ACTIVE_STATE);
        state.setConflictPhase(playerUuid, id.toString(), PHASE_SETUP);
        state.setConflictScore(playerUuid, "stress", id.toString(), 0);
        state.setConflictScore(playerUuid, "resolve", id.toString(), 0);
        return Optional.of(statusEcho(state, playerUuid, id, "conflict_started"));
    }

    public static Optional<String> addStress(NarrativeSavedData state, UUID playerUuid, String rawId, int amount) {
        Identifier id = parseIdentifier(rawId);
        int next = state.addConflictScore(playerUuid, "stress", id.toString(), amount);
        Optional<ConflictDefinition> definition = ConflictRegistry.byId(id);
        if (definition.isPresent() && next >= definition.get().stressLimit()) {
            state.setConflictState(playerUuid, id.toString(), definition.get().failureState());
            state.setConflictPhase(playerUuid, id.toString(), PHASE_CONSEQUENCE);
        } else if (ACTIVE_STATE.equals(state.getConflictState(playerUuid, id.toString()))) {
            state.setConflictPhase(playerUuid, id.toString(), next > 0 ? PHASE_PRESSURE : PHASE_SETUP);
        }
        return Optional.of(statusEcho(state, playerUuid, id, "conflict_stress"));
    }

    public static Optional<String> addResolve(NarrativeSavedData state, UUID playerUuid, String rawId, int amount) {
        Identifier id = parseIdentifier(rawId);
        int next = state.addConflictScore(playerUuid, "resolve", id.toString(), amount);
        Optional<ConflictDefinition> definition = ConflictRegistry.byId(id);
        if (definition.isPresent() && next >= definition.get().resolveGoal()) {
            state.setConflictState(playerUuid, id.toString(), definition.get().successState());
            state.setConflictPhase(playerUuid, id.toString(), PHASE_RESOLUTION);
        } else if (ACTIVE_STATE.equals(state.getConflictState(playerUuid, id.toString()))) {
            state.setConflictPhase(playerUuid, id.toString(), next > 0 ? PHASE_TURN : currentPhase(state, playerUuid, id));
        }
        return Optional.of(statusEcho(state, playerUuid, id, "conflict_resolve"));
    }

    public static Optional<String> applyOutcome(NarrativeSavedData state, UUID playerUuid, String rawId, String rawOutcomeId) {
        Identifier id = parseIdentifier(rawId);
        Optional<ConflictDefinition> definition = ConflictRegistry.byId(id);
        if (definition.isEmpty()) {
            return Optional.of("conflict_outcome_missing_conflict:" + id);
        }
        Optional<ConflictOutcomeDefinition> outcome = definition.get().outcome(rawOutcomeId);
        if (outcome.isEmpty()) {
            return Optional.of("conflict_outcome_missing:" + id + "#" + rawOutcomeId);
        }
        ConflictOutcomeDefinition resolved = outcome.get();
        state.setConflictState(playerUuid, id.toString(), resolved.state());
        state.setConflictPhase(playerUuid, id.toString(), phaseForOutcome(resolved));
        return Optional.of(statusEcho(state, playerUuid, id, "conflict_outcome#" + resolved.id()));
    }

    public static String statusLine(NarrativeSavedData state, UUID playerUuid, Identifier id) {
        return statusEcho(state, playerUuid, id, "conflict_status");
    }

    public static List<Identifier> knownLeverageClues(NarrativeSavedData state, UUID playerUuid, Identifier id) {
        return ConflictRegistry.byId(id).stream()
                .flatMap(definition -> definition.leverageClues().stream())
                .filter(clue -> state.hasClue(playerUuid, clue.toString()))
                .toList();
    }

    public static String currentPhase(NarrativeSavedData state, UUID playerUuid, Identifier id) {
        String persisted = state.getConflictPhase(playerUuid, id.toString());
        if (!persisted.equals("not_started")) {
            return persisted;
        }
        String conflictState = state.getConflictState(playerUuid, id.toString());
        Optional<ConflictDefinition> definition = ConflictRegistry.byId(id);
        if (conflictState.equals("not_started")) {
            return PHASE_SETUP;
        }
        if (definition.map(def -> conflictState.equals(def.failureState())).orElse(false) || conflictState.startsWith("failed")) {
            return PHASE_CONSEQUENCE;
        }
        if (definition.map(def -> conflictState.equals(def.successState())).orElse(false) || conflictState.startsWith("resolved")) {
            return PHASE_RESOLUTION;
        }
        if (state.getConflictScore(playerUuid, "resolve", id.toString()) > 0) {
            return PHASE_TURN;
        }
        if (state.getConflictScore(playerUuid, "stress", id.toString()) > 0) {
            return PHASE_PRESSURE;
        }
        return PHASE_SETUP;
    }

    private static String statusEcho(NarrativeSavedData state, UUID playerUuid, Identifier id, String event) {
        ConflictDefinition definition = ConflictRegistry.byId(id).orElse(null);
        int stressLimit = definition == null ? 3 : definition.stressLimit();
        int resolveGoal = definition == null ? 3 : definition.resolveGoal();
        String leverage = knownLeverageClues(state, playerUuid, id).stream()
                .map(Identifier::toString)
                .map(value -> value.substring(value.lastIndexOf('/') + 1))
                .collect(Collectors.joining("+"));
        if (leverage.isBlank()) {
            leverage = "none";
        }
        int outcomeCount = definition == null ? 0 : definition.outcomes().size();
        return event + ":" + id
                + " state=" + state.getConflictState(playerUuid, id.toString())
                + " phase=" + currentPhase(state, playerUuid, id)
                + " stress=" + state.getConflictScore(playerUuid, "stress", id.toString()) + "/" + stressLimit
                + " resolve=" + state.getConflictScore(playerUuid, "resolve", id.toString()) + "/" + resolveGoal
                + " leverage=" + leverage
                + " outcomes=" + outcomeCount;
    }

    private static String phaseForOutcome(ConflictOutcomeDefinition outcome) {
        if (outcome.isFailureForwardKind()) {
            return PHASE_CONSEQUENCE;
        }
        return PHASE_RESOLUTION;
    }

    private static Identifier parseIdentifier(String raw) {
        return raw.contains(":") ? Identifier.parse(raw) : Identifier.fromNamespaceAndPath("ebb", raw);
    }
}
