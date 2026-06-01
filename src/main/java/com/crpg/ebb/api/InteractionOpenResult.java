package com.crpg.ebb.api;

import net.minecraft.resources.Identifier;

import java.util.Optional;

public record InteractionOpenResult(boolean allowed, Identifier dialogueId, Optional<String> denialReason) {
    public InteractionOpenResult {
        denialReason = denialReason == null ? Optional.empty() : denialReason;
    }

    public static InteractionOpenResult allow(Identifier dialogueId) {
        return new InteractionOpenResult(true, dialogueId, Optional.empty());
    }

    public static InteractionOpenResult deny(Identifier dialogueId, String reason) {
        return new InteractionOpenResult(false, dialogueId, Optional.of(reason));
    }
}
