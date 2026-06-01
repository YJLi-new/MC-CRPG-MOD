package com.crpg.ebb.journal;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record JournalEntryDefinition(
        Identifier id,
        String title,
        JournalEntryCategory category,
        String text,
        Optional<Identifier> quest,
        List<String> tags
) {
    public JournalEntryDefinition {
        title = title == null || title.isBlank() ? id.toString() : title;
        category = category == null ? JournalEntryCategory.SCENE_NOTE : category;
        text = text == null ? "" : text;
        quest = quest == null ? Optional.empty() : quest;
        tags = tags == null ? List.of() : List.copyOf(tags);
    }

    public static Optional<JournalEntryDefinition> parse(Identifier fileId, JsonObject json, List<String> messages) {
        Identifier id = optionalString(json, "id").map(value -> parseIdentifier(value, fileId, messages)).orElse(fileId);
        String title = optionalString(json, "title").or(() -> optionalString(json, "name")).orElse(fileId.toString());
        JournalEntryCategory category = JournalEntryCategory.parse(optionalString(json, "category").or(() -> optionalString(json, "type")).orElse("scene_note"))
                .orElse(JournalEntryCategory.SCENE_NOTE);
        String text = optionalString(json, "text").or(() -> optionalString(json, "body")).orElse("");
        Optional<Identifier> quest = optionalString(json, "quest").map(value -> parseIdentifier(value, fileId, messages));
        List<String> tags = parseStringList(fileId, json, "tags", messages);
        if (text.isBlank()) {
            messages.add("journal_entry " + id + ": text/body should not be empty");
        }
        return Optional.of(new JournalEntryDefinition(id, title, category, text, quest, tags));
    }

    public String debugSummary() {
        return id + " [" + category.serializedName() + "] title=\"" + title + "\" quest=" + quest.map(Identifier::toString).orElse("-");
    }

    private static List<String> parseStringList(Identifier fileId, JsonObject json, String key, List<String> messages) {
        if (!json.has(key)) return List.of();
        if (!json.get(key).isJsonArray()) {
            messages.add("journal_entry " + fileId + ": " + key + " must be an array");
            return List.of();
        }
        JsonArray array = json.getAsJsonArray(key);
        List<String> values = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            if (!array.get(i).isJsonPrimitive()) {
                messages.add("journal_entry " + fileId + ": " + key + "[" + i + "] must be a string");
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
            messages.add("journal_entry " + fileId + ": invalid id " + raw + " (" + ex.getMessage() + ")");
            return fileId;
        }
    }

    private static Optional<String> optionalString(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull()
                ? Optional.of(GsonHelper.getAsString(json, key))
                : Optional.empty();
    }
}
