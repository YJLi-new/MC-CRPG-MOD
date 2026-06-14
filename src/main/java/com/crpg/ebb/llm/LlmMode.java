package com.crpg.ebb.llm;

import java.util.Locale;
import java.util.Optional;

public enum LlmMode {
    DISABLED,
    FAKE,
    GATEWAY;

    public static Optional<LlmMode> parse(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String normalized = value.trim().replace('-', '_').toUpperCase(Locale.ROOT);
        if ("OFF".equals(normalized) || "NONE".equals(normalized)) {
            normalized = "DISABLED";
        }
        if ("MOCK".equals(normalized) || "TEST".equals(normalized)) {
            normalized = "FAKE";
        }
        try {
            return Optional.of(LlmMode.valueOf(normalized));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
