package com.crpg.ebb.journal;

import java.util.Locale;
import java.util.Optional;

public enum JournalEntryCategory {
    CLUE,
    SCENE_NOTE,
    LEAD,
    QUEST_NOTE;

    public static Optional<JournalEntryCategory> parse(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        normalized = switch (normalized) {
            case "NOTE", "SCENE" -> "SCENE_NOTE";
            case "QUEST", "QUEST_BRANCH" -> "QUEST_NOTE";
            default -> normalized;
        };
        try {
            return Optional.of(JournalEntryCategory.valueOf(normalized));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
