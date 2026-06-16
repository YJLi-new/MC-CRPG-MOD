package com.crpg.ebb.gateway.memory;

public record MemoryValidationDecision(MemoryOperation operation, boolean accepted, String status, String reason, boolean safetyLessonRequired) {
    public static MemoryValidationDecision accept(MemoryOperation operation, String reason) {
        return new MemoryValidationDecision(operation, true, "accepted", reason == null ? "accepted" : reason, false);
    }

    public static MemoryValidationDecision reject(MemoryOperation operation, String reason, boolean safetyLessonRequired) {
        return new MemoryValidationDecision(operation, false, "rejected", reason == null ? "rejected" : reason, safetyLessonRequired);
    }
}
