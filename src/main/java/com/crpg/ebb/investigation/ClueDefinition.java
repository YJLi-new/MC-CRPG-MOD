package com.crpg.ebb.investigation;

import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public record ClueDefinition(
        Identifier id,
        String title,
        Optional<Identifier> scene,
        Optional<Identifier> journalEntry,
        Map<String, Integer> checkModifiers,
        String text
) {
    public ClueDefinition {
        title = title == null || title.isBlank() ? id.toString() : title;
        scene = scene == null ? Optional.empty() : scene;
        journalEntry = journalEntry == null ? Optional.empty() : journalEntry;
        checkModifiers = checkModifiers == null ? Map.of() : Map.copyOf(checkModifiers);
        text = text == null ? "" : text;
    }

    public static Optional<ClueDefinition> parse(Identifier fileId, JsonObject json, java.util.List<String> messages) {
        Identifier id = optionalString(json, "id").map(value -> parseIdentifier(value, fileId, messages)).orElse(fileId);
        String title = optionalString(json, "title").or(() -> optionalString(json, "name")).orElse(fileId.toString());
        Optional<Identifier> scene = optionalString(json, "scene").map(value -> parseIdentifier(value, fileId, messages));
        Optional<Identifier> journal = optionalString(json, "journal_entry")
                .or(() -> optionalString(json, "journalEntry"))
                .or(() -> optionalString(json, "journal"))
                .map(value -> parseIdentifier(value, fileId, messages));
        Map<String, Integer> modifiers = parseModifierMap(fileId, json, messages);
        String text = optionalString(json, "text").or(() -> optionalString(json, "description")).orElse("");
        if (text.isBlank()) {
            messages.add("clue " + id + ": text/description should not be empty");
        }
        return Optional.of(new ClueDefinition(id, title, scene, journal, modifiers, text));
    }

    public int modifierFor(String attribute) {
        return checkModifiers.getOrDefault(attribute.trim().toLowerCase(java.util.Locale.ROOT), 0);
    }

    public String debugSummary() {
        return id + " title=\"" + title + "\" scene=" + scene.map(Identifier::toString).orElse("-")
                + " journal=" + journalEntry.map(Identifier::toString).orElse("-")
                + " modifiers=" + checkModifiers;
    }

    private static Map<String, Integer> parseModifierMap(Identifier fileId, JsonObject json, java.util.List<String> messages) {
        JsonObject object = null;
        if (json.has("check_modifiers") && json.get("check_modifiers").isJsonObject()) {
            object = json.getAsJsonObject("check_modifiers");
        } else if (json.has("dc_modifiers") && json.get("dc_modifiers").isJsonObject()) {
            object = json.getAsJsonObject("dc_modifiers");
        }
        if (object == null) return Map.of();
        Map<String, Integer> values = new LinkedHashMap<>();
        for (Map.Entry<String, com.google.gson.JsonElement> entry : object.entrySet()) {
            if (!entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isNumber()) {
                messages.add("clue " + fileId + ": check_modifiers." + entry.getKey() + " must be an integer");
                continue;
            }
            values.put(entry.getKey().trim().toLowerCase(java.util.Locale.ROOT), entry.getValue().getAsInt());
        }
        return Map.copyOf(values);
    }

    static Identifier parseIdentifier(String raw, Identifier fileId, java.util.List<String> messages) {
        try {
            return raw.contains(":") ? Identifier.parse(raw) : Identifier.fromNamespaceAndPath(fileId.getNamespace(), raw);
        } catch (RuntimeException ex) {
            messages.add("clue " + fileId + ": invalid id " + raw + " (" + ex.getMessage() + ")");
            return fileId;
        }
    }

    static Optional<String> optionalString(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull()
                ? Optional.of(GsonHelper.getAsString(json, key))
                : Optional.empty();
    }
}
