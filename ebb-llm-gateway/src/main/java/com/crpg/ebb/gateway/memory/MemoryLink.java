package com.crpg.ebb.gateway.memory;

import java.util.LinkedHashMap;
import java.util.Map;

public record MemoryLink(
        String id,
        long createdAt,
        String serverId,
        String worldId,
        String sourceRecordId,
        String targetRecordId,
        String relation,
        String reason
) {
    public Map<String, Object> toJsonMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", id);
        values.put("created_at", createdAt);
        values.put("server_id", serverId);
        values.put("world_id", worldId);
        values.put("source_record_id", sourceRecordId);
        values.put("target_record_id", targetRecordId);
        values.put("relation", relation);
        values.put("reason", reason);
        return values;
    }
}
