package com.crpg.ebb.dialogue;

import java.util.Locale;
import java.util.Optional;

public enum DialogueScope {
    PLAYER,
    WORLD;

    public static Optional<DialogueScope> parse(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(DialogueScope.valueOf(value.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
