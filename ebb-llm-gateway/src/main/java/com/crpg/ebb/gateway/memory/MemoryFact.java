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
        String embedding
) {
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
        return values;
    }
}
