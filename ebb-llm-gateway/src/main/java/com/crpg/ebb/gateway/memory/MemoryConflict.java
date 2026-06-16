package com.crpg.ebb.gateway.memory;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record MemoryConflict(
        String id,
        long createdAt,
        String serverId,
        String worldId,
        String subject,
        String predicate,
        String oldFactId,
        String newFactId,
        String oldValue,
        String newValue,
        String citationIds,
        String status
) {
    public List<String> citations() {
        if (citationIds == null || citationIds.isBlank()) {
            return List.of();
        }
        return Arrays.stream(citationIds.split(",")).filter(value -> !value.isBlank()).toList();
    }

    public Map<String, Object> toJsonMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", id);
        values.put("created_at", createdAt);
        values.put("server_id", serverId);
        values.put("world_id", worldId);
        values.put("subject", subject);
        values.put("predicate", predicate);
        values.put("old_fact_id", oldFactId);
        values.put("new_fact_id", newFactId);
        values.put("old_value", oldValue);
        values.put("new_value", newValue);
        values.put("citation_ids", citations());
        values.put("status", status);
        return values;
    }
}
