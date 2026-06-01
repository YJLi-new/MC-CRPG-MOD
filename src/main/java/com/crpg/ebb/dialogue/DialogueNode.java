package com.crpg.ebb.dialogue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public record DialogueNode(
        String id,
        DialogueNodeType type,
        String speaker,
        String text,
        Optional<String> textKey,
        List<String> chimeTags,
        List<DialogueEffect> enterEffects,
        Optional<String> next,
        List<DialogueChoice> choices
) {
    public DialogueNode {
        type = type == null ? DialogueNodeType.CHOICE : type;
        textKey = textKey == null ? Optional.empty() : textKey;
        chimeTags = chimeTags == null ? List.of() : List.copyOf(chimeTags);
        enterEffects = enterEffects == null ? List.of() : List.copyOf(enterEffects);
        next = next == null ? Optional.empty() : next;
        choices = List.copyOf(choices);
    }

    static Optional<DialogueNode> parse(String nodeId, JsonObject json, String path, List<String> messages) {
        DialogueNodeType type = optionalString(json, "type")
                .flatMap(DialogueNodeType::parse)
                .orElse(DialogueNodeType.CHOICE);
        String speaker = optionalString(json, "speaker").orElse("narrator");
        Optional<String> text = optionalString(json, "text");
        Optional<String> textKey = optionalString(json, "text_key");
        if (text.isEmpty() && textKey.isEmpty() && type != DialogueNodeType.JUMP && type != DialogueNodeType.EFFECT) {
            messages.add(path + ": missing required string \"text\" or \"text_key\"");
            return Optional.empty();
        }

        List<DialogueEffect> enterEffects = new ArrayList<>(DialogueEffect.parseList(json, "enter_effects", path, messages));
        enterEffects.addAll(DialogueEffect.parseList(json, "effects", path, messages));
        List<String> chimeTags = parseTags(json, path, messages);
        Optional<String> next = optionalString(json, "next");
        List<DialogueChoice> choices = new ArrayList<>();
        if (json.has("choices")) {
            if (!json.get("choices").isJsonArray()) {
                messages.add(path + ": choices must be an array when present");
            } else {
                JsonArray choiceArray = json.getAsJsonArray("choices");
                for (int i = 0; i < choiceArray.size(); i++) {
                    JsonElement element = choiceArray.get(i);
                    if (!element.isJsonObject()) {
                        messages.add(path + ".choices[" + i + "]: choice must be an object");
                        continue;
                    }
                    DialogueChoice.parse(element.getAsJsonObject(), path + ".choices[" + i + "]", messages)
                            .ifPresent(choices::add);
                }
            }
        }
        return Optional.of(new DialogueNode(nodeId, type, speaker, text.orElse(""), textKey, chimeTags, enterEffects, next, choices));
    }

    public Optional<DialogueChoice> choice(String choiceId) {
        return choices.stream().filter(choice -> choice.id().equals(choiceId)).findFirst();
    }

    public String debugSummary() {
        StringBuilder builder = new StringBuilder("node " + id + " [" + type.serializedName() + "]: " + speaker + " — " + text.replace('\n', ' '));
        textKey.ifPresent(key -> builder.append(" text_key=").append(key));
        if (!chimeTags.isEmpty()) {
            builder.append(" chime_tags=").append(chimeTags);
        }
        next.ifPresent(value -> builder.append(" next=").append(value));
        if (!enterEffects.isEmpty()) {
            builder.append(" enter_effects=").append(enterEffects.stream().map(DialogueEffect::debugSummary).collect(Collectors.joining(",", "[", "]")));
        }
        return builder.toString();
    }

    private static Optional<String> optionalString(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull()
                ? Optional.of(GsonHelper.getAsString(json, key))
                : Optional.empty();
    }

    private static List<String> parseTags(JsonObject json, String path, List<String> messages) {
        List<String> tags = new ArrayList<>();
        appendTags(json, "tags", path, messages, tags);
        appendTags(json, "chime_tags", path, messages, tags);
        return tags.stream().distinct().toList();
    }

    private static void appendTags(JsonObject json, String key, String path, List<String> messages, List<String> tags) {
        if (!json.has(key)) {
            return;
        }
        if (!json.get(key).isJsonArray()) {
            messages.add(path + ": " + key + " must be an array when present");
            return;
        }
        for (JsonElement element : json.getAsJsonArray(key)) {
            if (!element.isJsonPrimitive()) {
                messages.add(path + ": " + key + " entries must be strings");
                continue;
            }
            tags.add(element.getAsString());
        }
    }
}
