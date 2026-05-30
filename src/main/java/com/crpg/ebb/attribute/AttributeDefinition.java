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
        List<String> aliases,
        int defaultScore,
        int min,
        int max
) {
    public AttributeDefinition {
        aliases = aliases == null ? List.of() : List.copyOf(aliases);
    }

    public static Optional<AttributeDefinition> parse(Identifier id, JsonObject json, List<String> messages) {
        String key = optionalString(json, "key").orElse(id.getPath());
        String displayName = optionalString(json, "name").orElse(key);
        List<String> aliases = parseAliases(json);
        int defaultScore = optionalInt(json, "default").orElse(0);
        int min = optionalInt(json, "min").orElse(-5);
        int max = optionalInt(json, "max").orElse(10);
        if (min > max) {
            messages.add("attribute " + id + ": min is greater than max");
            return Optional.empty();
        }
        if (defaultScore < min || defaultScore > max) {
            int clamped = Math.max(min, Math.min(max, defaultScore));
            messages.add("attribute " + id + ": default score " + defaultScore + " is outside [" + min + ", " + max + "]; clamped to " + clamped);
            defaultScore = clamped;
        }
        return Optional.of(new AttributeDefinition(id, key, displayName, aliases, defaultScore, min, max));
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

    private static List<String> parseAliases(JsonObject json) {
        if (!json.has("aliases") || json.get("aliases").isJsonNull()) {
            return List.of();
        }
        if (json.get("aliases").isJsonArray()) {
            java.util.ArrayList<String> aliases = new java.util.ArrayList<>();
            for (com.google.gson.JsonElement element : json.getAsJsonArray("aliases")) {
                String alias = element.getAsString();
                if (!alias.isBlank()) {
                    aliases.add(alias);
                }
            }
            return List.copyOf(aliases);
        }
        String alias = GsonHelper.getAsString(json, "aliases");
        return alias.isBlank() ? List.of() : List.of(alias);
    }
}
