package com.crpg.ebb.dialogue;

import com.crpg.ebb.interaction.InteractionTargetType;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record DialogueSession(
        UUID conversationId,
        UUID playerUuid,
        Identifier dialogueId,
        Identifier targetId,
        InteractionTargetType targetType,
        String nodeId
) {
    public DialogueSession withNode(String newNodeId) {
        return new DialogueSession(conversationId, playerUuid, dialogueId, targetId, targetType, newNodeId);
    }
}
