package com.crpg.ebb.chime;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record ChimeDefinition(
        Identifier id,
        String name,
        String sourceAttribute,
        int minScore,
        List<String> triggerTags,
        String speakerStyle,
        int cooldownTicks,
        List<String> lines
) {
    public ChimeDefinition {
        name = name == null || name.isBlank() ? id.toString() : name;
        sourceAttribute = sourceAttribute == null || sourceAttribute.isBlank() ? "wisdom" : sourceAttribute;
        triggerTags = triggerTags == null ? List.of() : List.copyOf(triggerTags);
        speakerStyle = speakerStyle == null ? "inner" : speakerStyle;
        lines = lines == null ? List.of() : List.copyOf(lines);
    }

    public static Optional<ChimeDefinition> parse(Identifier fileId, JsonObject json, List<String> messages) {
        Identifier id = optionalString(json, "id").map(value -> parseIdentifier(value, fileId, messages)).orElse(fileId);
        String name = optionalString(json, "name").or(() -> optionalString(json, "display_name")).orElse(fileId.toString());
        String sourceAttribute = optionalString(json, "source_attribute")
                .or(() -> optionalString(json, "attribute"))
                .orElse("wisdom");
        int minScore = optionalInt(json, "min_score").or(() -> optionalInt(json, "min_attribute_score")).orElse(0);
        List<String> triggerTags = parseStringList(fileId, json, "trigger_tags", messages);
        if (triggerTags.isEmpty()) {
            triggerTags = parseStringList(fileId, json, "tags", messages);
        }
        String speakerStyle = optionalString(json, "speaker_style").orElse("inner");
        int cooldownTicks = optionalInt(json, "cooldown").or(() -> optionalInt(json, "cooldown_ticks")).orElse(0);
        List<String> lines = parseStringList(fileId, json, "lines", messages);
        if (lines.isEmpty()) {
            optionalString(json, "text").ifPresent(lines::add);
        }
        if (triggerTags.isEmpty()) {
            messages.add("chime " + id + ": trigger_tags should not be empty");
        }
        if (lines.isEmpty()) {
            messages.add("chime " + id + ": lines/text should not be empty");
        }
        return Optional.of(new ChimeDefinition(id, name, sourceAttribute, minScore, triggerTags, speakerStyle, cooldownTicks, lines));
    }

    public String lineForStableIndex(int index) {
        if (lines.isEmpty()) return "";
        return lines.get(Math.floorMod(index, lines.size()));
    }

    public String debugSummary() {
        return id + " name=\"" + name + "\" attribute=" + sourceAttribute + ">=" + minScore
                + " tags=" + triggerTags + " lines=" + lines.size();
    }

    private static List<String> parseStringList(Identifier fileId, JsonObject json, String key, List<String> messages) {
        if (!json.has(key)) return new ArrayList<>();
        if (!json.get(key).isJsonArray()) {
            messages.add("chime " + fileId + ": " + key + " must be an array");
            return new ArrayList<>();
        }
        JsonArray array = json.getAsJsonArray(key);
        List<String> values = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            if (!array.get(i).isJsonPrimitive()) {
                messages.add("chime " + fileId + ": " + key + "[" + i + "] must be a string");
                continue;
            }
            values.add(array.get(i).getAsString());
        }
        return values;
    }

    private static Identifier parseIdentifier(String raw, Identifier fileId, List<String> messages) {
        try {
            return raw.contains(":") ? Identifier.parse(raw) : Identifier.fromNamespaceAndPath(fileId.getNamespace(), raw);
        } catch (RuntimeException ex) {
            messages.add("chime " + fileId + ": invalid id " + raw + " (" + ex.getMessage() + ")");
            return fileId;
        }
    }

    private static Optional<String> optionalString(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull() ? Optional.of(GsonHelper.getAsString(json, key)) : Optional.empty();
    }

    private static Optional<Integer> optionalInt(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull() && json.get(key).isJsonPrimitive() && json.get(key).getAsJsonPrimitive().isNumber()
                ? Optional.of(GsonHelper.getAsInt(json, key)) : Optional.empty();
    }
}
