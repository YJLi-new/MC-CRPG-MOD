package com.crpg.ebb.relationship;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record RelationshipDefinition(
        Identifier id,
        String displayName,
        String narrativeKey,
        int defaultScore,
        List<String> tags
) {
    public RelationshipDefinition {
        displayName = displayName == null || displayName.isBlank() ? id.toString() : displayName;
        narrativeKey = normalizeKey(narrativeKey == null || narrativeKey.isBlank() ? id.toString() : narrativeKey);
        tags = tags == null ? List.of() : List.copyOf(tags);
    }

    public static Optional<RelationshipDefinition> parse(Identifier fileId, JsonObject json, List<String> messages) {
        Identifier id = optionalString(json, "id").map(value -> parseIdentifier(value, fileId, messages)).orElse(fileId);
        String displayName = optionalString(json, "display_name")
                .or(() -> optionalString(json, "displayName"))
                .or(() -> optionalString(json, "name"))
                .orElse(fileId.toString());
        String narrativeKey = optionalString(json, "narrative_key")
                .or(() -> optionalString(json, "narrativeKey"))
                .or(() -> optionalString(json, "key"))
                .orElse(id.toString());
        int defaultScore = optionalInt(json, "default_score")
                .or(() -> optionalInt(json, "defaultScore"))
                .or(() -> optionalInt(json, "base"))
                .orElse(0);
        List<String> tags = parseStringList(fileId, json, "tags", messages);
        return Optional.of(new RelationshipDefinition(id, displayName, narrativeKey, defaultScore, tags));
    }

    public String debugSummary() {
        return id + " key=" + narrativeKey + " name=\"" + displayName + "\" default=" + defaultScore + " tags=" + tags;
    }

    public static String normalizeKey(String key) {
        return key == null ? "" : key.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static List<String> parseStringList(Identifier fileId, JsonObject json, String key, List<String> messages) {
        if (!json.has(key)) return List.of();
        if (!json.get(key).isJsonArray()) {
            messages.add("relationship " + fileId + ": " + key + " must be an array");
            return List.of();
        }
        JsonArray array = json.getAsJsonArray(key);
        List<String> values = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            if (!array.get(i).isJsonPrimitive()) {
                messages.add("relationship " + fileId + ": " + key + "[" + i + "] must be a string");
                continue;
            }
            values.add(array.get(i).getAsString());
        }
        return List.copyOf(values);
    }

    private static Identifier parseIdentifier(String raw, Identifier fileId, List<String> messages) {
        try {
            return raw.contains(":") ? Identifier.parse(raw) : Identifier.fromNamespaceAndPath(fileId.getNamespace(), raw);
        } catch (RuntimeException ex) {
            messages.add("relationship " + fileId + ": invalid id " + raw + " (" + ex.getMessage() + ")");
            return fileId;
        }
    }

    private static Optional<String> optionalString(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull()
                ? Optional.of(GsonHelper.getAsString(json, key))
                : Optional.empty();
    }

    private static Optional<Integer> optionalInt(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull()
                && json.get(key).isJsonPrimitive()
                && json.get(key).getAsJsonPrimitive().isNumber()
                ? Optional.of(GsonHelper.getAsInt(json, key))
                : Optional.empty();
    }
}
