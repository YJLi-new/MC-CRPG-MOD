package com.crpg.ebb.dialogue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record DialogueNode(
        String id,
        String speaker,
        String text,
        List<DialogueChoice> choices
) {
    public DialogueNode {
        choices = List.copyOf(choices);
    }

    static Optional<DialogueNode> parse(String nodeId, JsonObject json, String path, List<String> messages) {
        String speaker = optionalString(json, "speaker").orElse("narrator");
        String text = optionalString(json, "text").orElse(null);
        if (text == null) {
            messages.add(path + ": missing required string \"text\"");
            return Optional.empty();
        }

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
        return Optional.of(new DialogueNode(nodeId, speaker, text, choices));
    }

    public Optional<DialogueChoice> choice(String choiceId) {
        return choices.stream().filter(choice -> choice.id().equals(choiceId)).findFirst();
    }

    private static Optional<String> optionalString(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull()
                ? Optional.of(GsonHelper.getAsString(json, key))
                : Optional.empty();
    }
}
