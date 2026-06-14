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
        long gameTime
) {
    public LlmChatRequest {
        entityUuid = entityUuid == null ? Optional.empty() : entityUuid;
        npcKey = blankDefault(npcKey, "unknown_npc");
        npcDisplayName = blankDefault(npcDisplayName, npcKey);
        topicHint = topicHint == null ? "" : topicHint.strip();
        playerMessage = playerMessage == null ? "" : playerMessage.strip();
    }

    private static String blankDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.strip();
    }
}
