package com.crpg.ebb.gateway.memory;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public record MemoryOperation(
        String id,
        long createdAt,
        String serverId,
        String worldId,
        String recordId,
        String type,
        String subject,
        String predicate,
        String value,
        String status,
        String reason,
        String proposedBy,
        double confidence
) {
    public static final String ADD_FACT = "add_fact";
    public static final String ADD_SUMMARY = "add_summary";
    public static final String ADD_SAFETY_LESSON = "add_safety_lesson";
    public static final String CORRECT_FACT = "correct_fact";

    public MemoryOperation {
        id = id == null ? "" : id.strip();
        serverId = serverId == null || serverId.isBlank() ? "local-dev" : serverId.strip();
        worldId = worldId == null || worldId.isBlank() ? "unknown-world" : worldId.strip();
        recordId = recordId == null ? "" : recordId.strip();
        type = type == null || type.isBlank() ? ADD_FACT : type.strip().toLowerCase(Locale.ROOT);
        subject = subject == null || subject.isBlank() ? "unknown" : subject.strip();
        predicate = predicate == null || predicate.isBlank() ? "unknown" : predicate.strip().toLowerCase(Locale.ROOT);
        value = value == null ? "" : value.strip();
        status = status == null ? "proposed" : status.strip().toLowerCase(Locale.ROOT);
        reason = reason == null ? "" : reason.strip();
        proposedBy = proposedBy == null || proposedBy.isBlank() ? "llm_memory_extractor" : proposedBy.strip();
        confidence = Math.max(0.0D, Math.min(1.0D, confidence));
    }

    public MemoryOperation withRecord(String newRecordId) {
        return new MemoryOperation(id, createdAt, serverId, worldId, newRecordId, type, subject, predicate, value, status, reason, proposedBy, confidence);
    }

    public MemoryOperation withStatus(String newStatus, String newReason) {
        return new MemoryOperation(id, createdAt, serverId, worldId, recordId, type, subject, predicate, value, newStatus, newReason, proposedBy, confidence);
    }

    public Map<String, Object> toJsonMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", id);
        values.put("created_at", createdAt);
        values.put("server_id", serverId);
        values.put("world_id", worldId);
        values.put("record_id", recordId);
        values.put("type", type);
        values.put("subject", subject);
        values.put("predicate", predicate);
        values.put("value", value);
        values.put("status", status);
        values.put("reason", reason);
        values.put("proposed_by", proposedBy);
        values.put("confidence", Math.round(confidence * 1000.0D) / 1000.0D);
        return values;
    }
}
