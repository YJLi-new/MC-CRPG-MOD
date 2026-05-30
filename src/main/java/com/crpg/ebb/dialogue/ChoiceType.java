package com.crpg.ebb.dialogue;

import java.util.Locale;
import java.util.Optional;

public enum ChoiceType {
    DIALOGUE,
    ACTION,
    THOUGHT;

    public static Optional<ChoiceType> parse(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(ChoiceType.valueOf(value.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
