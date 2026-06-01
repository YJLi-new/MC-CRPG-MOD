package com.crpg.ebb.quest;

import com.crpg.ebb.dialogue.DialogueEffect;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record QuestBranchDefinition(
        Identifier id,
        String title,
        QuestBranchKind kind,
        String description,
        String takeRootText,
        List<Identifier> grantFeats,
        List<DialogueEffect> takeRootEffects
) {
    public QuestBranchDefinition {
        title = title == null || title.isBlank() ? id.toString() : title;
        kind = kind == null ? QuestBranchKind.MINOR : kind;
        description = description == null ? "" : description;
        takeRootText = takeRootText == null ? "" : takeRootText;
        grantFeats = grantFeats == null ? List.of() : List.copyOf(grantFeats);
        takeRootEffects = takeRootEffects == null ? List.of() : List.copyOf(takeRootEffects);
    }

    public static Optional<QuestBranchDefinition> parse(Identifier fileId, JsonObject json, List<String> messages) {
        Identifier id = optionalString(json, "id").map(value -> parseIdentifier(value, fileId, messages)).orElse(fileId);
        String title = optionalString(json, "title").orElse(fileId.toString());
        QuestBranchKind kind = QuestBranchKind.parse(optionalString(json, "kind").or(() -> optionalString(json, "type")).orElse("minor"))
                .orElse(QuestBranchKind.MINOR);
        String description = optionalString(json, "description").orElse("");
        String takeRootText = optionalString(json, "take_root_text")
                .or(() -> optionalString(json, "takeRootText"))
                .orElse("");
        List<Identifier> feats = parseIdentifiers(json, "grant_feats", fileId, messages);
        if (feats.isEmpty()) {
            feats = parseIdentifiers(json, "feats", fileId, messages);
        }
        List<DialogueEffect> effects = DialogueEffect.parseList(json, "take_root_effects", "quest_branch " + fileId, messages);
        if (kind == QuestBranchKind.MAJOR && takeRootText.isBlank()) {
            messages.add("quest_branch " + id + ": major branches should provide take_root_text");
        }
        return Optional.of(new QuestBranchDefinition(id, title, kind, description, takeRootText, feats, effects));
    }

    public String debugSummary() {
        return id + " [" + kind.serializedName() + "] title=\"" + title + "\" grants=" + grantFeats.size()
                + " take_root_effects=" + takeRootEffects.size();
    }

    private static List<Identifier> parseIdentifiers(JsonObject json, String key, Identifier fileId, List<String> messages) {
        if (!json.has(key)) {
            return List.of();
        }
        if (!json.get(key).isJsonArray()) {
            messages.add("quest_branch " + fileId + ": " + key + " must be an array");
            return List.of();
        }
        List<Identifier> values = new ArrayList<>();
        JsonArray array = json.getAsJsonArray(key);
        for (int i = 0; i < array.size(); i++) {
            if (!array.get(i).isJsonPrimitive()) {
                messages.add("quest_branch " + fileId + ": " + key + "[" + i + "] must be a string id");
                continue;
            }
            values.add(parseIdentifier(array.get(i).getAsString(), fileId, messages));
        }
        return List.copyOf(values);
    }

    private static Identifier parseIdentifier(String raw, Identifier fileId, List<String> messages) {
        try {
            return raw.contains(":") ? Identifier.parse(raw) : Identifier.fromNamespaceAndPath(fileId.getNamespace(), raw);
        } catch (RuntimeException ex) {
            messages.add("quest_branch " + fileId + ": invalid id " + raw + " (" + ex.getMessage() + ")");
            return fileId;
        }
    }

    private static Optional<String> optionalString(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull()
                ? Optional.of(GsonHelper.getAsString(json, key))
                : Optional.empty();
    }
}
