package com.crpg.ebb.attribute;

import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;

import java.util.List;
import java.util.Optional;

public record AttributeDefinition(
        Identifier id,
        String key,
        String displayName,
        int defaultScore,
        int min,
        int max
) {
    public static Optional<AttributeDefinition> parse(Identifier id, JsonObject json, List<String> messages) {
        String key = optionalString(json, "key").orElse(id.getPath());
        String displayName = optionalString(json, "name").orElse(key);
        int defaultScore = optionalInt(json, "default").orElse(0);
        int min = optionalInt(json, "min").orElse(-5);
        int max = optionalInt(json, "max").orElse(10);
        if (min > max) {
            messages.add("attribute " + id + ": min is greater than max");
            return Optional.empty();
        }
        if (defaultScore < min || defaultScore > max) {
            messages.add("attribute " + id + ": default score " + defaultScore + " is outside [" + min + ", " + max + "]");
        }
        return Optional.of(new AttributeDefinition(id, key, displayName, defaultScore, min, max));
    }

    public int clamp(int value) {
        return Math.max(min, Math.min(max, value));
    }

    private static Optional<String> optionalString(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull()
                ? Optional.of(GsonHelper.getAsString(json, key))
                : Optional.empty();
    }

    private static Optional<Integer> optionalInt(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull()
                ? Optional.of(GsonHelper.getAsInt(json, key))
                : Optional.empty();
    }
}
