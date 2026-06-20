package com.crpg.ebb.gateway.memory;

import java.util.LinkedHashMap;
import java.util.Map;

public record MemoryFact(
        String id,
        String recordId,
        long createdAt,
        String serverId,
        String worldId,
        String subject,
        String predicate,
        String value,
        String status,
        String supersededBy,
        String citationId,
        String embedding,
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
    public MemoryFact(String id, String recordId, long createdAt, String serverId, String worldId, String subject,
                      String predicate, String value, String status, String supersededBy, String citationId, String embedding) {
        this(id, recordId, createdAt, serverId, worldId, subject, predicate, value, status, supersededBy, citationId, embedding,
                MemoryAuthorityPolicy.LLM_INFERRED, 20, 0.5D, "private", 0L, 0L, 0L, 0L,
                "legacy_memory_store", "legacy_memory_store");
    }

    public MemoryFact withStatus(String newStatus) {
        return new MemoryFact(id, recordId, createdAt, serverId, worldId, subject, predicate, value,
                newStatus == null || newStatus.isBlank() ? status : newStatus, supersededBy, citationId, embedding,
                sourceType, authorityRank, certainty, visibility, validFrom, validTo, worldTick, mcDay, createdBy, updatedBy);
    }

    public Map<String, Object> toJsonMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", id);
        values.put("record_id", recordId);
        values.put("created_at", createdAt);
        values.put("server_id", serverId);
        values.put("world_id", worldId);
        values.put("subject", subject);
        values.put("predicate", predicate);
        values.put("value", value);
        values.put("status", status);
        values.put("superseded_by", supersededBy == null ? "" : supersededBy);
        values.put("citation_id", citationId);
        values.put("source_type", sourceType == null ? "" : sourceType);
        values.put("authority_rank", authorityRank);
        values.put("certainty", Math.round(certainty * 1000.0D) / 1000.0D);
        values.put("visibility", visibility == null ? "" : visibility);
        values.put("valid_from", validFrom);
        values.put("valid_to", validTo);
        values.put("world_tick", worldTick);
        values.put("mc_day", mcDay);
        values.put("created_by", createdBy == null ? "" : createdBy);
        values.put("updated_by", updatedBy == null ? "" : updatedBy);
        return values;
    }
}
