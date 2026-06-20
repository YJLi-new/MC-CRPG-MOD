package com.crpg.ebb.llm;

import net.minecraft.resources.Identifier;

import java.util.Optional;
import java.util.UUID;

public record LlmChatRequest(
        UUID conversationId,
        UUID playerUuid,
        Optional<UUID> entityUuid,
        Identifier dialogueId,
        String nodeId,
        String npcKey,
        String npcDisplayName,
        String topicHint,
        String playerMessage,
        long gameTime,
        String knowledgeContext,
        String serverId,
        String worldId
) {
    public LlmChatRequest(UUID conversationId, UUID playerUuid, Optional<UUID> entityUuid, Identifier dialogueId, String nodeId,
                          String npcKey, String npcDisplayName, String topicHint, String playerMessage, long gameTime) {
        this(conversationId, playerUuid, entityUuid, dialogueId, nodeId, npcKey, npcDisplayName, topicHint, playerMessage, gameTime, "");
    }

    public LlmChatRequest(UUID conversationId, UUID playerUuid, Optional<UUID> entityUuid, Identifier dialogueId, String nodeId,
                          String npcKey, String npcDisplayName, String topicHint, String playerMessage, long gameTime,
                          String knowledgeContext) {
        this(conversationId, playerUuid, entityUuid, dialogueId, nodeId, npcKey, npcDisplayName, topicHint, playerMessage, gameTime,
                knowledgeContext, "", "");
    }

    public LlmChatRequest {
        entityUuid = entityUuid == null ? Optional.empty() : entityUuid;
        npcKey = blankDefault(npcKey, "unknown_npc");
        npcDisplayName = blankDefault(npcDisplayName, npcKey);
        topicHint = topicHint == null ? "" : topicHint.strip();
        playerMessage = playerMessage == null ? "" : playerMessage.strip();
        knowledgeContext = knowledgeContext == null ? "" : knowledgeContext.strip();
        serverId = serverId == null ? "" : serverId.strip();
        worldId = worldId == null ? "" : worldId.strip();
    }

    private static String blankDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.strip();
    }
}
