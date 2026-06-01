package com.crpg.ebb.story;

import java.util.Locale;
import java.util.Optional;

/**
 * Narrative variable layers from the GOAL.md product model.
 *
 * <p>Branch variables are reserved for major route/ending commitments, Major
 * variables for quest/NPC/world-state pivots, and Minor variables for local or
 * short-lived beats. The values themselves are stored by the authoritative
 * server state; this enum only classifies their persistence/authoring intent.</p>
 */
public enum StoryVarLayer {
    BRANCH,
    MAJOR,
    MINOR;

    public static Optional<StoryVarLayer> parse(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        normalized = switch (normalized) {
            case "ROUTE", "ENDING", "PATH" -> "BRANCH";
            case "QUEST", "WORLD", "NPC", "RELATIONSHIP" -> "MAJOR";
            case "LOCAL", "SCENE", "BEAT" -> "MINOR";
            default -> normalized;
        };
        try {
            return Optional.of(StoryVarLayer.valueOf(normalized));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
