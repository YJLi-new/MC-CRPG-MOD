package com.crpg.ebb.dialogue;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public record DialogueChoice(
        String id,
        ChoiceType type,
        String text,
        Optional<String> textKey,
        Optional<String> next,
        Optional<DialogueCheck> check,
        List<DialogueCondition> conditions,
        List<DialogueEffect> effects,
        boolean singleUse,
        boolean revalidateTarget
) {
    public DialogueChoice {
        textKey = textKey == null ? Optional.empty() : textKey;
        next = next == null ? Optional.empty() : next;
        check = check == null ? Optional.empty() : check;
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
        effects = effects == null ? List.of() : List.copyOf(effects);
    }

    static Optional<DialogueChoice> parse(JsonObject json, String path, List<String> messages) {
        String id = requiredString(json, "id", path, messages).orElse(null);
        String rawType = optionalString(json, "type").or(() -> optionalString(json, "kind")).orElse(null);
        if (rawType == null || rawType.isBlank()) {
            messages.add(path + ": missing required string \"type\" or \"kind\"");
        }
        Optional<String> text = optionalString(json, "text").or(() -> optionalString(json, "label"));
        Optional<String> textKey = optionalString(json, "text_key");
        if (text.isEmpty() && textKey.isEmpty()) {
            messages.add(path + ": missing required string \"text\" or \"text_key\"");
        }
        Optional<ChoiceType> type = ChoiceType.parse(rawType);
        if (rawType != null && type.isEmpty()) {
            messages.add(path + ": unknown choice type \"" + rawType + "\"; expected dialogue/action/thought");
        }
        if (id == null || type.isEmpty() || (text.isEmpty() && textKey.isEmpty())) {
            return Optional.empty();
        }

        Optional<String> next = optionalString(json, "next");
        Optional<DialogueCheck> check = Optional.empty();
        if (json.has("check") && json.get("check").isJsonObject()) {
            check = DialogueCheck.parse(json.getAsJsonObject("check"), path + ".check", messages);
        } else if (json.has("roll") && json.get("roll").isJsonObject()) {
            check = DialogueCheck.parse(json.getAsJsonObject("roll"), path + ".roll", messages);
        } else if (json.has("check") || json.has("roll")) {
            messages.add(path + ": check/roll must be an object when present");
        }

        List<DialogueCondition> conditions = parseConditions(json, path, messages);
        List<DialogueEffect> effects = DialogueEffect.parseList(json, "effects", path, messages);
        boolean singleUse = optionalBoolean(json, "once").orElse(false)
                || check.map(DialogueCheck::mode).filter(mode -> mode == RollMode.ONE_SHOT).isPresent();
        boolean defaultRevalidateTarget = type.get() == ChoiceType.ACTION
                && (next.isPresent() || check.isPresent() || !effects.isEmpty());
        boolean revalidateTarget = optionalBoolean(json, "revalidate_target").orElse(defaultRevalidateTarget);

        return Optional.of(new DialogueChoice(id, type.get(), text.orElse(""), textKey, next, check, conditions, effects, singleUse, revalidateTarget));
    }

    public Optional<String> defaultNextNode() {
        if (next.isPresent()) {
            return next;
        }
        return check.flatMap(DialogueCheck::success);
    }

    public String debugSummary() {
        StringBuilder builder = new StringBuilder("choice " + id + " [" + type + "] text=\"" + text + "\"");
        textKey.ifPresent(key -> builder.append(" text_key=").append(key));
        next.ifPresent(value -> builder.append(" -> ").append(value));
        if (type == ChoiceType.ACTION) {
            builder.append(" revalidate_target=").append(revalidateTarget);
        }
        if (singleUse) {
            builder.append(" once=true");
        }
        check.ifPresent(value -> builder.append(" check=").append(value.debugSummary()));
        if (!conditions.isEmpty()) {
            builder.append(" conditions=").append(conditions.stream().map(DialogueCondition::debugSummary).collect(Collectors.joining(",", "[", "]")));
        }
        if (!effects.isEmpty()) {
            builder.append(" effects(pre)=").append(effects.stream().map(DialogueEffect::debugSummary).collect(Collectors.joining(",", "[", "]")));
        }
        return builder.toString();
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

    private static Optional<Boolean> optionalBoolean(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull()
                ? Optional.of(GsonHelper.getAsBoolean(json, key))
                : Optional.empty();
    }
}
