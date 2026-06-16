package com.crpg.ebb.gateway.memory;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record MemorySummary(
        String id,
        long createdAt,
        long updatedAt,
        String serverId,
        String worldId,
        String recordId,
        String summary,
        String rawEpisodeCitationId,
        String relatedMemoryIds,
        int evolutionCount
) {
    public List<String> relatedIds() {
        if (relatedMemoryIds == null || relatedMemoryIds.isBlank()) {
            return List.of();
        }
        return Arrays.stream(relatedMemoryIds.split(",")).map(String::strip).filter(value -> !value.isBlank()).toList();
    }

    public Map<String, Object> toJsonMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", id);
        values.put("created_at", createdAt);
        values.put("updated_at", updatedAt);
        values.put("server_id", serverId);
        values.put("world_id", worldId);
        values.put("record_id", recordId);
        values.put("summary", summary);
        values.put("raw_episode_citation_id", rawEpisodeCitationId);
        values.put("related_memory_ids", relatedIds());
        values.put("evolution_count", evolutionCount);
        return values;
    }
}
