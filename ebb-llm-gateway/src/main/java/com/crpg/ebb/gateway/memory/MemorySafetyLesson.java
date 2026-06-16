package com.crpg.ebb.gateway.memory;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record MemorySafetyLesson(
        String id,
        long createdAt,
        String serverId,
        String worldId,
        String subject,
        String lesson,
        String sourceRecordId,
        String conflictId,
        String citationIds,
        String status
) {
    public List<String> citations() {
        if (citationIds == null || citationIds.isBlank()) {
            return List.of();
        }
        return Arrays.stream(citationIds.split(",")).map(String::strip).filter(value -> !value.isBlank()).toList();
    }

    public Map<String, Object> toJsonMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", id);
        values.put("created_at", createdAt);
        values.put("server_id", serverId);
        values.put("world_id", worldId);
        values.put("subject", subject);
        values.put("lesson", lesson);
        values.put("source_record_id", sourceRecordId);
        values.put("conflict_id", conflictId == null ? "" : conflictId);
        values.put("citation_ids", citations());
        values.put("status", status);
        return values;
    }
}
