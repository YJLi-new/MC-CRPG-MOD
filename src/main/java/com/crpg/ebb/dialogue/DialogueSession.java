package com.crpg.ebb.dialogue;

import com.crpg.ebb.interaction.InteractionTargetType;
import net.minecraft.resources.Identifier;

import java.util.Optional;
import java.util.UUID;

public record DialogueSession(
        UUID conversationId,
        UUID playerUuid,
        Identifier dialogueId,
        Identifier targetId,
        InteractionTargetType targetType,
        Optional<UUID> entityUuid,
        String nodeId,
        long lastTouchedGameTime
) {
    public DialogueSession {
        entityUuid = entityUuid == null ? Optional.empty() : entityUuid;
    }

    public DialogueSession withNode(String nextNodeId, long gameTime) {
        return new DialogueSession(conversationId, playerUuid, dialogueId, targetId, targetType, entityUuid, nextNodeId, gameTime);
    }

    public DialogueSession touch(long gameTime) {
        return new DialogueSession(conversationId, playerUuid, dialogueId, targetId, targetType, entityUuid, nodeId, gameTime);
    }
}
