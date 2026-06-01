package com.crpg.ebb.story;

import com.google.gson.JsonElement;

import java.util.Locale;
import java.util.OptionalInt;

/**
 * Small scalar wrapper for story-variable values.
 *
 * <p>Saved data stores story variables as strings for codec stability and
 * datapack friendliness, while this helper preserves predictable bool/int/string
 * normalization for dialogue effects and conditions.</p>
 */
public record StoryVarValue(String raw) {
    public StoryVarValue {
        raw = raw == null ? "" : raw.trim();
    }

    public static StoryVarValue ofString(String value) {
        return new StoryVarValue(value);
    }

    public static StoryVarValue ofInt(int value) {
        return new StoryVarValue(Integer.toString(value));
    }

    public static StoryVarValue ofBoolean(boolean value) {
        return new StoryVarValue(Boolean.toString(value));
    }

    public static StoryVarValue fromJson(JsonElement element) {
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            return new StoryVarValue("");
        }
        var primitive = element.getAsJsonPrimitive();
        if (primitive.isBoolean()) {
            return ofBoolean(primitive.getAsBoolean());
        }
        if (primitive.isNumber()) {
            return ofInt(primitive.getAsInt());
        }
        return ofString(primitive.getAsString());
    }

    public OptionalInt asInt() {
        try {
            return OptionalInt.of(Integer.parseInt(raw));
        } catch (NumberFormatException ex) {
            return OptionalInt.empty();
        }
    }

    public boolean stringEquals(StoryVarValue other) {
        return raw.equals(other.raw());
    }

    public boolean boolEquals(StoryVarValue other) {
        return normalizeBoolean(raw).equals(normalizeBoolean(other.raw()));
    }

    public boolean scalarEquals(StoryVarValue other) {
        OptionalInt left = asInt();
        OptionalInt right = other.asInt();
        if (left.isPresent() && right.isPresent()) {
            return left.getAsInt() == right.getAsInt();
        }
        if (isBooleanLike(raw) || isBooleanLike(other.raw())) {
            return boolEquals(other);
        }
        return stringEquals(other);
    }

    private static boolean isBooleanLike(String value) {
        String normalized = normalizeBoolean(value);
        return "true".equals(normalized) || "false".equals(normalized);
    }

    private static String normalizeBoolean(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
