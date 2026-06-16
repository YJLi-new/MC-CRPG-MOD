package com.crpg.ebb.gateway.memory;

import com.crpg.ebb.gateway.HttpJson;

import java.util.Map;

public record MemorySearchRequest(
        String serverId,
        String worldId,
        String minecraftPlayerUuid,
        String npcKey,
        String entityUuid,
        String query,
        int limit
) {
    public MemorySearchRequest {
        serverId = blank(serverId, "local-dev");
        worldId = blank(worldId, "unknown-world");
        minecraftPlayerUuid = minecraftPlayerUuid == null ? "" : minecraftPlayerUuid.strip();
        npcKey = npcKey == null ? "" : npcKey.strip();
        entityUuid = entityUuid == null ? "" : entityUuid.strip();
        query = query == null ? "" : query.strip();
        limit = Math.max(1, Math.min(25, limit <= 0 ? 8 : limit));
    }

    public static MemorySearchRequest fromJson(String json) {
        Map<String, String> values = HttpJson.objectStrings(json);
        return new MemorySearchRequest(
                values.get("server_id"),
                values.get("world_id"),
                values.get("minecraft_player_uuid"),
                values.get("npc_key"),
                values.get("entity_uuid"),
                values.getOrDefault("query", values.getOrDefault("q", "")),
                (int) HttpJson.longValue(json, "limit", 8)
        );
    }

    private static String blank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.strip();
    }
}
