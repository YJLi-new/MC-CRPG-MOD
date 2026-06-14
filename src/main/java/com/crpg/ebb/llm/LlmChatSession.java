package com.crpg.ebb.llm;

import com.crpg.ebb.interaction.InteractionTargetType;
import net.minecraft.resources.Identifier;

import java.util.Optional;
import java.util.UUID;

public record LlmChatSession(
        UUID conversationId,
        UUID playerUuid,
        Identifier dialogueId,
        Identifier targetId,
        InteractionTargetType targetType,
        Optional<UUID> entityUuid,
        String sourceNodeId,
        String returnNodeId,
        String npcKey,
        String npcDisplayName,
        String topicHint,
        long openedGameTime,
        long lastTouchedGameTime,
        boolean awaitingResponse,
        long lastNonce
) {
    public LlmChatSession {
        entityUuid = entityUuid == null ? Optional.empty() : entityUuid;
        sourceNodeId = blankDefault(sourceNodeId, "start");
        returnNodeId = blankDefault(returnNodeId, sourceNodeId);
        npcKey = blankDefault(npcKey, "unknown_npc");
        npcDisplayName = blankDefault(npcDisplayName, npcKey);
        topicHint = topicHint == null ? "" : topicHint.strip();
    }

    public LlmChatSession touch(long gameTime) {
        return new LlmChatSession(conversationId, playerUuid, dialogueId, targetId, targetType, entityUuid,
                sourceNodeId, returnNodeId, npcKey, npcDisplayName, topicHint, openedGameTime, gameTime,
                awaitingResponse, lastNonce);
    }

    public LlmChatSession awaiting(long gameTime, long nonce) {
        return new LlmChatSession(conversationId, playerUuid, dialogueId, targetId, targetType, entityUuid,
                sourceNodeId, returnNodeId, npcKey, npcDisplayName, topicHint, openedGameTime, gameTime, true, nonce);
    }

    public LlmChatSession replied(long gameTime, long nonce) {
        return new LlmChatSession(conversationId, playerUuid, dialogueId, targetId, targetType, entityUuid,
                sourceNodeId, returnNodeId, npcKey, npcDisplayName, topicHint, openedGameTime, gameTime, false, nonce);
    }

    private static String blankDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.strip();
    }
}
