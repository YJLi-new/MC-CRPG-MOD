package com.crpg.ebb.gateway.memory;

import java.util.Locale;
import java.util.Map;

/** Deterministic P39 gate: LLM memory ops are proposals; only accepted ops mutate memory. */
public final class DeterministicMemoryValidator {
    private static final Map<String, String> CANONICAL_FACTS = Map.of(
            key("tavern", "owner"), "innkeeper",
            key("ebb:demo/tavern", "owner"), "ebb:demo/innkeeper",
            key("旅馆", "owner"), "innkeeper"
    );

    public MemoryValidationDecision validate(MemoryOperation operation) {
        if (operation == null) {
            return MemoryValidationDecision.reject(null, "missing_operation", false);
        }
        if (operation.confidence() < 0.35D) {
            return MemoryValidationDecision.reject(operation, "confidence_below_threshold", false);
        }
        if (operation.value().length() > 2048) {
            return MemoryValidationDecision.reject(operation, "value_too_long", false);
        }
        if (MemoryOperation.ADD_FACT.equals(operation.type())) {
            String canonical = CANONICAL_FACTS.get(key(operation.subject(), operation.predicate()));
            if (canonical != null && !sameCanonicalValue(canonical, operation.value())) {
                return MemoryValidationDecision.reject(operation,
                        "canonical_conflict: canonical " + operation.subject() + "." + operation.predicate() + "=" + canonical,
                        true);
            }
        }
        if (MemoryOperation.ADD_SUMMARY.equals(operation.type()) && operation.value().isBlank()) {
            return MemoryValidationDecision.reject(operation, "empty_summary", false);
        }
        if (MemoryOperation.ADD_SAFETY_LESSON.equals(operation.type()) && operation.value().isBlank()) {
            return MemoryValidationDecision.reject(operation, "empty_safety_lesson", false);
        }
        return MemoryValidationDecision.accept(operation, "deterministic_validator_accepted");
    }

    public String canonicalFactsSummary() {
        return CANONICAL_FACTS.toString();
    }

    private static String key(String subject, String predicate) {
        return normalize(subject) + "." + normalize(predicate);
    }

    private static boolean sameCanonicalValue(String canonical, String value) {
        String normalized = normalize(value);
        String canonicalNormalized = normalize(canonical);
        return normalized.equals(canonicalNormalized)
                || normalized.endsWith("/" + canonicalNormalized)
                || normalized.endsWith(":" + canonicalNormalized);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip().toLowerCase(Locale.ROOT)
                .replace("旅馆老板", "innkeeper")
                .replace("旅店老板", "innkeeper")
                .replace("酒馆老板", "innkeeper")
                .replace("老板", "owner");
    }
}
