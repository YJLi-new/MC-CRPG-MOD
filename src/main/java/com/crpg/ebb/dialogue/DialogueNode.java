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
        String speaker,
        String text,
        Optional<String> textKey,
        List<DialogueEffect> enterEffects,
        List<DialogueChoice> choices
) {
    public DialogueNode {
        textKey = textKey == null ? Optional.empty() : textKey;
        enterEffects = enterEffects == null ? List.of() : List.copyOf(enterEffects);
        choices = List.copyOf(choices);
    }

    static Optional<DialogueNode> parse(String nodeId, JsonObject json, String path, List<String> messages) {
        String speaker = optionalString(json, "speaker").orElse("narrator");
        Optional<String> text = optionalString(json, "text");
        Optional<String> textKey = optionalString(json, "text_key");
        if (text.isEmpty() && textKey.isEmpty()) {
            messages.add(path + ": missing required string \"text\" or \"text_key\"");
            return Optional.empty();
        }

        List<DialogueEffect> enterEffects = DialogueEffect.parseList(json, "enter_effects", path, messages);
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
        return Optional.of(new DialogueNode(nodeId, speaker, text.orElse(""), textKey, enterEffects, choices));
    }

    public Optional<DialogueChoice> choice(String choiceId) {
        return choices.stream().filter(choice -> choice.id().equals(choiceId)).findFirst();
    }

    public String debugSummary() {
        StringBuilder builder = new StringBuilder("node " + id + ": " + speaker + " — " + text.replace('\n', ' '));
        textKey.ifPresent(key -> builder.append(" text_key=").append(key));
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
}
