package com.crpg.ebb.gateway.memory;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Product-level authority ladder for memory facts.  LLM/player claims are kept
 * as auditable claims unless a higher-authority source promotes or supersedes
 * them; low-authority claims cannot overwrite canon.
 */
public final class MemoryAuthorityPolicy {
    public static final String SYSTEM_CANON = "SYSTEM_CANON";
    public static final String SCRIPTED_EFFECT = "SCRIPTED_EFFECT";
    public static final String SERVER_EVENT = "SERVER_EVENT";
    public static final String NPC_OBSERVED = "NPC_OBSERVED";
    public static final String PLAYER_CLAIM = "PLAYER_CLAIM";
    public static final String MANUAL_PLAYER_CORRECTION = "MANUAL_PLAYER_CORRECTION";
    public static final String LLM_INFERRED = "LLM_INFERRED";
    public static final String DEV_NOTE = "DEV_NOTE";

    private static final Map<String, CanonFact> DEMO_CANON = Map.of(
            key("tavern", "owner"), new CanonFact("innkeeper", SYSTEM_CANON, 100, "demo_story_canon"),
            key("ebb:demo/tavern", "owner"), new CanonFact("ebb:demo/innkeeper", SYSTEM_CANON, 100, "demo_story_canon"),
            key("旅馆", "owner"), new CanonFact("innkeeper", SYSTEM_CANON, 100, "demo_story_canon")
    );

    public FactAuthority classify(MemoryOperation operation) {
        if (operation == null) {
            return new FactAuthority(LLM_INFERRED, 20, 0.35D, "private", 0L, 0L, 0L, 0L,
                    "unknown", "unknown");
        }
        String proposedBy = normalize(operation.proposedBy());
        String reason = normalize(operation.reason());
        if (proposedBy.contains("manual_player_correction") || reason.contains("manual_player_correction")
                || MemoryOperation.CORRECT_FACT.equals(operation.type())) {
            return authority(MANUAL_PLAYER_CORRECTION, 95, operation);
        }
        if (proposedBy.contains("scripted") || reason.contains("scripted")) {
            return authority(SCRIPTED_EFFECT, 90, operation);
        }
        if (proposedBy.contains("server_event") || reason.contains("server_event")) {
            return authority(SERVER_EVENT, 80, operation);
        }
        if (proposedBy.contains("npc_observed") || reason.contains("npc_observed")) {
            return authority(NPC_OBSERVED, 70, operation);
        }
        if (proposedBy.contains("dev")) {
            return authority(DEV_NOTE, 60, operation);
        }
        if (reason.contains("player") || proposedBy.contains("player")) {
            return authority(PLAYER_CLAIM, 40, operation);
        }
        return authority(LLM_INFERRED, 20, operation);
    }

    public Optional<CanonFact> canonicalFact(String subject, String predicate) {
        return Optional.ofNullable(DEMO_CANON.get(key(subject, predicate)));
    }

    public boolean canSupersede(MemoryFact previous, FactAuthority incoming) {
        if (previous == null) {
            return true;
        }
        return incoming.authorityRank() >= previous.authorityRank();
    }

    public boolean sameCanonicalValue(String canonical, String value) {
        String normalized = normalizeValue(value);
        String canonicalNormalized = normalizeValue(canonical);
        return normalized.equals(canonicalNormalized)
                || normalized.endsWith("/" + canonicalNormalized)
                || normalized.endsWith(":" + canonicalNormalized);
    }

    public String canonicalFactsSummary() {
        return DEMO_CANON.toString();
    }

    private static FactAuthority authority(String type, int rank, MemoryOperation operation) {
        return new FactAuthority(type, rank, operation.confidence(), "private", 0L, 0L, 0L, 0L,
                operation.proposedBy(), operation.proposedBy());
    }

    private static String key(String subject, String predicate) {
        return normalizeValue(subject) + "." + normalizeValue(predicate);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
    }

    private static String normalizeValue(String value) {
        return normalize(value)
                .replace("旅馆老板", "innkeeper")
                .replace("旅店老板", "innkeeper")
                .replace("酒馆老板", "innkeeper")
                .replace("老板", "owner");
    }

    public record FactAuthority(
            String sourceType,
            int authorityRank,
            double certainty,
            String visibility,
            long validFrom,
            long validTo,
            long worldTick,
            long mcDay,
            String createdBy,
            String updatedBy
    ) {
    }

    public record CanonFact(String value, String sourceType, int authorityRank, String sourceId) {
    }
}
