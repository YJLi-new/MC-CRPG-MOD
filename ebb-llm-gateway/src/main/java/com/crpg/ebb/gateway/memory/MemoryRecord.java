package com.crpg.ebb.gateway.memory;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record MemoryRecord(
        String id,
        long createdAt,
        String serverId,
        String worldId,
        String minecraftPlayerUuid,
        String npcKey,
        String entityUuid,
        String conversationId,
        String dialogueId,
        String sourceNodeId,
        String role,
        String text,
        String citationId,
        String embedding,
        String summary,
        long summaryUpdatedAt,
        String relatedMemoryIds
) {
    public MemoryRecord(String id, long createdAt, String serverId, String worldId, String minecraftPlayerUuid, String npcKey,
                        String entityUuid, String conversationId, String dialogueId, String sourceNodeId, String role,
                        String text, String citationId, String embedding) {
        this(id, createdAt, serverId, worldId, minecraftPlayerUuid, npcKey, entityUuid, conversationId, dialogueId, sourceNodeId,
                role, text, citationId, embedding, "", 0L, "");
    }

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
        values.put("server_id", serverId);
        values.put("world_id", worldId);
        values.put("minecraft_player_uuid", minecraftPlayerUuid);
        values.put("npc_key", npcKey);
        values.put("entity_uuid", entityUuid == null ? "" : entityUuid);
        values.put("conversation_id", conversationId);
        values.put("dialogue_id", dialogueId == null ? "" : dialogueId);
        values.put("source_node_id", sourceNodeId == null ? "" : sourceNodeId);
        values.put("role", role);
        values.put("text", text);
        values.put("raw_episode", text);
        values.put("summary", summary == null ? "" : summary);
        values.put("summary_updated_at", summaryUpdatedAt);
        values.put("related_memory_ids", relatedIds());
        values.put("citation_id", citationId);
        return values;
    }
}
