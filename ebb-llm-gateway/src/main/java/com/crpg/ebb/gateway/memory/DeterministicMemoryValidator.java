package com.crpg.ebb.gateway.memory;

/** Deterministic P39 gate: LLM memory ops are proposals; only accepted ops mutate memory. */
public final class DeterministicMemoryValidator {
    private final MemoryAuthorityPolicy authorityPolicy = new MemoryAuthorityPolicy();

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
        if (!MemoryOperation.ADD_FACT.equals(operation.type())
                && !MemoryOperation.ADD_SUMMARY.equals(operation.type())
                && !MemoryOperation.ADD_SAFETY_LESSON.equals(operation.type())
                && !MemoryOperation.CORRECT_FACT.equals(operation.type())) {
            return MemoryValidationDecision.reject(operation, "unsupported_memory_op:" + operation.type(), true);
        }
        if (containsHighRiskMutation(operation.value()) || containsHighRiskMutation(operation.predicate())
                || containsHighRiskMutation(operation.subject())) {
            return MemoryValidationDecision.reject(operation, "high_risk_memory_op_rejected", true);
        }
        if (MemoryOperation.ADD_FACT.equals(operation.type())) {
            var canonical = authorityPolicy.canonicalFact(operation.subject(), operation.predicate());
            if (canonical.isPresent() && !authorityPolicy.sameCanonicalValue(canonical.get().value(), operation.value())) {
                return MemoryValidationDecision.reject(operation,
                        "canonical_conflict: canonical " + operation.subject() + "." + operation.predicate() + "=" + canonical.get().value()
                                + " source=" + canonical.get().sourceType() + " authority=" + canonical.get().authorityRank(),
                        true);
            }
        }
        if (MemoryOperation.ADD_SUMMARY.equals(operation.type()) && operation.value().isBlank()) {
            return MemoryValidationDecision.reject(operation, "empty_summary", false);
        }
        if (MemoryOperation.ADD_SAFETY_LESSON.equals(operation.type()) && operation.value().isBlank()) {
            return MemoryValidationDecision.reject(operation, "empty_safety_lesson", false);
        }
        if (MemoryOperation.CORRECT_FACT.equals(operation.type())) {
            return MemoryValidationDecision.reject(operation, "correction_requires_explicit_memory_correct_endpoint", true);
        }
        return MemoryValidationDecision.accept(operation, "deterministic_validator_accepted");
    }

    public String canonicalFactsSummary() {
        return authorityPolicy.canonicalFactsSummary();
    }

    private static boolean containsHighRiskMutation(String value) {
        String lower = value == null ? "" : value.toLowerCase(java.util.Locale.ROOT);
        for (String token : java.util.List.of(
                "complete_quest",
                "complete_quest_branch",
                "set_flag",
                "give_item",
                "remove_item",
                "set_story_var",
                "unlock_feat",
                "reveal_clue",
                "set_npc_routine",
                "command:",
                "op:",
                "delete other player",
                "hidden kb",
                "hidden_knowledge")) {
            if (lower.contains(token)) {
                return true;
            }
        }
        return false;
    }
}
