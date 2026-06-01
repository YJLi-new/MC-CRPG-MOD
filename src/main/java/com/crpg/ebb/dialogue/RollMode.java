package com.crpg.ebb.dialogue;

import java.util.Locale;
import java.util.Optional;

public enum RollMode {
    RETRYABLE,
    ONE_SHOT;

    public static Optional<RollMode> parse(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        if ("RED".equals(normalized) || "RED_CHECK".equals(normalized)) {
            normalized = "ONE_SHOT";
        } else if ("WHITE".equals(normalized) || "WHITE_CHECK".equals(normalized)) {
            normalized = "RETRYABLE";
        }
        try {
            return Optional.of(RollMode.valueOf(normalized));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
