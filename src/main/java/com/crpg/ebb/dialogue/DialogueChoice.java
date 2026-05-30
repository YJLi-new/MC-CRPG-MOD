package com.crpg.ebb.dialogue;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record DialogueChoice(
        String id,
        ChoiceType type,
        String text,
        Optional<String> next,
        Optional<DialogueCheck> check,
        List<DialogueCondition> conditions,
        List<DialogueEffect> effects
) {
    public DialogueChoice {
        next = next == null ? Optional.empty() : next;
        check = check == null ? Optional.empty() : check;
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
        effects = effects == null ? List.of() : List.copyOf(effects);
    }

    static Optional<DialogueChoice> parse(JsonObject json, String path, List<String> messages) {
        String id = requiredString(json, "id", path, messages).orElse(null);
        String rawType = requiredString(json, "type", path, messages).orElse(null);
        String text = requiredString(json, "text", path, messages).orElse(null);
        Optional<ChoiceType> type = ChoiceType.parse(rawType);
        if (rawType != null && type.isEmpty()) {
            messages.add(path + ": unknown choice type \"" + rawType + "\"; expected dialogue/action/thought");
        }
        if (id == null || type.isEmpty() || text == null) {
            return Optional.empty();
        }

        Optional<String> next = optionalString(json, "next");
        Optional<DialogueCheck> check = Optional.empty();
        if (json.has("check") && json.get("check").isJsonObject()) {
            check = DialogueCheck.parse(json.getAsJsonObject("check"), path + ".check", messages);
        } else if (json.has("check")) {
            messages.add(path + ": check must be an object when present");
        }

        List<DialogueCondition> conditions = parseConditions(json, path, messages);
        List<DialogueEffect> effects = parseEffects(json, path, messages);

        if (next.isEmpty() && check.isEmpty()) {
            // A terminal choice is valid; it closes the conversation.
        }
        return Optional.of(new DialogueChoice(id, type.get(), text, next, check, conditions, effects));
    }

    public Optional<String> defaultNextNode() {
        if (next.isPresent()) {
            return next;
        }
        return check.flatMap(DialogueCheck::success);
    }

    private static List<DialogueCondition> parseConditions(JsonObject json, String path, List<String> messages) {
        if (!json.has("conditions")) {
            return List.of();
        }
        if (!json.get("conditions").isJsonArray()) {
            messages.add(path + ": conditions must be an array when present");
            return List.of();
        }
        List<DialogueCondition> conditions = new ArrayList<>();
        JsonArray array = json.getAsJsonArray("conditions");
        for (int i = 0; i < array.size(); i++) {
            JsonElement element = array.get(i);
            if (!element.isJsonObject()) {
                messages.add(path + ".conditions[" + i + "]: condition must be an object");
                continue;
            }
            DialogueCondition.parse(element.getAsJsonObject(), path + ".conditions[" + i + "]", messages)
                    .ifPresent(conditions::add);
        }
        return List.copyOf(conditions);
    }

    private static List<DialogueEffect> parseEffects(JsonObject json, String path, List<String> messages) {
        if (!json.has("effects")) {
            return List.of();
        }
        if (!json.get("effects").isJsonArray()) {
            messages.add(path + ": effects must be an array when present");
            return List.of();
        }
        List<DialogueEffect> effects = new ArrayList<>();
        JsonArray array = json.getAsJsonArray("effects");
        for (int i = 0; i < array.size(); i++) {
            JsonElement element = array.get(i);
            if (!element.isJsonObject()) {
                messages.add(path + ".effects[" + i + "]: effect must be an object");
                continue;
            }
            DialogueEffect.parse(element.getAsJsonObject(), path + ".effects[" + i + "]", messages)
                    .ifPresent(effects::add);
        }
        return List.copyOf(effects);
    }

    private static Optional<String> requiredString(JsonObject json, String key, String path, List<String> messages) {
        Optional<String> value = optionalString(json, key);
        if (value.isEmpty() || value.get().isBlank()) {
            messages.add(path + ": missing required string \"" + key + "\"");
            return Optional.empty();
        }
        return value;
    }

    private static Optional<String> optionalString(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull()
                ? Optional.of(GsonHelper.getAsString(json, key))
                : Optional.empty();
    }
}
