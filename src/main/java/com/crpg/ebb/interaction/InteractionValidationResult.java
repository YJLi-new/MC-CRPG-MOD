package com.crpg.ebb.interaction;

import java.util.Optional;

public record InteractionValidationResult(
        boolean allowed,
        Optional<InteractionTarget> target,
        String reason
) {
    public static InteractionValidationResult allow(InteractionTarget target) {
        return new InteractionValidationResult(true, Optional.of(target), "allowed");
    }

    public static InteractionValidationResult deny(String reason) {
        return new InteractionValidationResult(false, Optional.empty(), reason);
    }
}
