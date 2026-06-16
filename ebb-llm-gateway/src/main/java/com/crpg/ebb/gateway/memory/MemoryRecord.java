package com.crpg.ebb.gateway.memory;

import java.util.LinkedHashMap;
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
        String embedding
) {
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
        values.put("citation_id", citationId);
        return values;
    }
}
