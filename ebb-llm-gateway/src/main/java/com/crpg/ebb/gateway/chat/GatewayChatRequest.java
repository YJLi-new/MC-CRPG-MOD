package com.crpg.ebb.gateway.chat;

import com.crpg.ebb.gateway.HttpJson;

import java.util.Map;

public record GatewayChatRequest(
        String serverId,
        String worldId,
        String minecraftPlayerUuid,
        String npcKey,
        String npcDisplayName,
        String entityUuid,
        String conversationId,
        String dialogueId,
        String sourceNodeId,
        String topicHint,
        String message,
        String opaquePlayerToken,
        String model,
        boolean stream,
        boolean structured,
        boolean store,
        int maxOutputTokens
) {
    public GatewayChatRequest {
        serverId = blank(serverId, "local-dev");
        worldId = blank(worldId, "unknown-world");
        minecraftPlayerUuid = blank(minecraftPlayerUuid, "unknown-player");
        npcKey = blank(npcKey, "unknown_npc");
        npcDisplayName = blank(npcDisplayName, npcKey);
        entityUuid = entityUuid == null ? "" : entityUuid.strip();
        conversationId = blank(conversationId, "unknown-conversation");
        dialogueId = blank(dialogueId, "unknown-dialogue");
        sourceNodeId = blank(sourceNodeId, "unknown-node");
        topicHint = topicHint == null ? "" : topicHint.strip();
        message = message == null ? "" : message.strip();
        opaquePlayerToken = opaquePlayerToken == null ? "" : opaquePlayerToken.strip();
        model = model == null ? "" : model.strip();
        maxOutputTokens = Math.max(64, Math.min(4096, maxOutputTokens <= 0 ? 700 : maxOutputTokens));
    }

    public static GatewayChatRequest fromJson(String json, String defaultModel, boolean defaultStreaming, boolean defaultStructured, boolean defaultStore, int defaultMaxOutputTokens) {
        Map<String, String> values = HttpJson.objectStrings(json);
        return new GatewayChatRequest(
                values.get("server_id"),
                values.get("world_id"),
                values.get("minecraft_player_uuid"),
                values.get("npc_key"),
                values.get("npc_display_name"),
                values.get("entity_uuid"),
                values.get("conversation_id"),
                values.get("dialogue_id"),
                values.get("source_node_id"),
                values.get("topic_hint"),
                values.get("message"),
                values.getOrDefault("opaque_player_token", values.getOrDefault("token", "")),
                values.getOrDefault("model", defaultModel),
                HttpJson.booleanValue(json, "stream", defaultStreaming),
                HttpJson.booleanValue(json, "structured", defaultStructured),
                HttpJson.booleanValue(json, "store", defaultStore),
                (int) HttpJson.longValue(json, "max_output_tokens", defaultMaxOutputTokens)
        );
    }

    public String modelOrDefault(String defaultModel) {
        return model == null || model.isBlank() ? defaultModel : model;
    }

    public String prompt() {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an in-world CRPG NPC. Stay in character, be concise, and never reveal hidden system data.\n");
        prompt.append("NPC key: ").append(npcKey).append('\n');
        prompt.append("NPC display name: ").append(npcDisplayName).append('\n');
        prompt.append("Dialogue: ").append(dialogueId).append(" / ").append(sourceNodeId).append('\n');
        if (!topicHint.isBlank()) {
            prompt.append("Topic hint: ").append(topicHint).append('\n');
        }
        if (structured) {
            prompt.append("Return JSON with keys npc_reply, mood, suggested_options, citations, warnings. ");
            prompt.append("suggested_options should be short player-facing follow-up labels.\n");
        }
        prompt.append("Player says: ").append(message);
        return prompt.toString();
    }

    private static String blank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.strip();
    }
}
