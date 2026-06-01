package com.crpg.ebb.investigation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record InvestigationSceneDefinition(
        Identifier id,
        String title,
        String description,
        List<Identifier> clues,
        int completionThreshold
) {
    public InvestigationSceneDefinition {
        title = title == null || title.isBlank() ? id.toString() : title;
        description = description == null ? "" : description;
        clues = clues == null ? List.of() : List.copyOf(clues);
        completionThreshold = completionThreshold <= 0 ? clues.size() : completionThreshold;
    }

    public static Optional<InvestigationSceneDefinition> parse(Identifier fileId, JsonObject json, List<String> messages) {
        Identifier id = ClueDefinition.optionalString(json, "id").map(value -> ClueDefinition.parseIdentifier(value, fileId, messages)).orElse(fileId);
        String title = ClueDefinition.optionalString(json, "title").or(() -> ClueDefinition.optionalString(json, "name")).orElse(fileId.toString());
        String description = ClueDefinition.optionalString(json, "description").or(() -> ClueDefinition.optionalString(json, "text")).orElse("");
        List<Identifier> clues = parseIdentifiers(fileId, json, "clues", messages);
        int threshold = json.has("completion_threshold") ? GsonHelper.getAsInt(json, "completion_threshold") : clues.size();
        if (clues.isEmpty()) {
            messages.add("investigation_scene " + id + ": at least one clue is recommended");
        }
        return Optional.of(new InvestigationSceneDefinition(id, title, description, clues, threshold));
    }

    public String debugSummary() {
        return id + " title=\"" + title + "\" clues=" + clues.size() + " completion_threshold=" + completionThreshold;
    }

    private static List<Identifier> parseIdentifiers(Identifier fileId, JsonObject json, String key, List<String> messages) {
        if (!json.has(key)) return List.of();
        if (!json.get(key).isJsonArray()) {
            messages.add("investigation_scene " + fileId + ": " + key + " must be an array");
            return List.of();
        }
        JsonArray array = json.getAsJsonArray(key);
        List<Identifier> values = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            if (!array.get(i).isJsonPrimitive()) {
                messages.add("investigation_scene " + fileId + ": " + key + "[" + i + "] must be a string id");
                continue;
            }
            values.add(ClueDefinition.parseIdentifier(array.get(i).getAsString(), fileId, messages));
        }
        return List.copyOf(values);
    }
}
