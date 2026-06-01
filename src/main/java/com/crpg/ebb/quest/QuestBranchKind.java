package com.crpg.ebb.quest;

import java.util.Locale;
import java.util.Optional;

public enum QuestBranchKind {
    MINOR,
    MAJOR;

    public static Optional<QuestBranchKind> parse(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        try {
            return Optional.of(QuestBranchKind.valueOf(normalized));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
