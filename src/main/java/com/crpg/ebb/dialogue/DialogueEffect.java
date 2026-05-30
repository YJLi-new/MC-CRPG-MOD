package com.crpg.ebb.dialogue;

import com.crpg.ebb.state.NarrativeSavedData;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public record DialogueEffect(
        EffectType type,
        DialogueScope scope,
        String id,
        Optional<Integer> attributeValue
) {
    public DialogueEffect {
        attributeValue = attributeValue == null ? Optional.empty() : attributeValue;
    }

    public enum EffectType {
        SET_FLAG,
        CLEAR_FLAG,
        SET_ATTRIBUTE,
        GIVE_ITEM_PLACEHOLDER,
        TAKE_ITEM_PLACEHOLDER,
        ROUTINE_PLACEHOLDER;

        static Optional<EffectType> parse(String value) {
            if (value == null || value.isBlank()) {
                return Optional.empty();
            }
            String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
            if ("GIVE_ITEM".equals(normalized)) {
                normalized = "GIVE_ITEM_PLACEHOLDER";
            } else if ("TAKE_ITEM".equals(normalized)) {
                normalized = "TAKE_ITEM_PLACEHOLDER";
            } else if ("ROUTINE".equals(normalized) || "SET_ROUTINE".equals(normalized)) {
                normalized = "ROUTINE_PLACEHOLDER";
            }
            try {
                return Optional.of(EffectType.valueOf(normalized));
            } catch (IllegalArgumentException ex) {
                return Optional.empty();
            }
        }
    }

    public static List<DialogueEffect> parseList(JsonObject json, String key, String path, List<String> messages) {
        if (!json.has(key)) {
            return List.of();
        }
        if (!json.get(key).isJsonArray()) {
            messages.add(path + ": " + key + " must be an array when present");
            return List.of();
        }
        List<DialogueEffect> effects = new ArrayList<>();
        JsonArray array = json.getAsJsonArray(key);
        for (int i = 0; i < array.size(); i++) {
            JsonElement element = array.get(i);
            if (!element.isJsonObject()) {
                messages.add(path + "." + key + "[" + i + "]: effect must be an object");
                continue;
            }
            parse(element.getAsJsonObject(), path + "." + key + "[" + i + "]", messages).ifPresent(effects::add);
        }
        return List.copyOf(effects);
    }

    public static Optional<DialogueEffect> parse(JsonObject json, String path, List<String> messages) {
        Optional<EffectType> type = EffectType.parse(optionalString(json, "type").orElse("set_flag"));
        if (type.isEmpty()) {
            messages.add(path + ": unknown effect type");
            return Optional.empty();
        }
        Optional<DialogueScope> scope = DialogueScope.parse(optionalString(json, "scope").orElse("player"));
        if (scope.isEmpty()) {
            messages.add(path + ": scope must be player or world");
            return Optional.empty();
        }
        Optional<String> id = optionalString(json, "id").or(() -> optionalString(json, "flag")).or(() -> optionalString(json, "attribute")).or(() -> optionalString(json, "routine"));
        if (id.isEmpty() || id.get().isBlank()) {
            messages.add(path + ": missing id/flag/attribute/routine string");
            return Optional.empty();
        }
        Optional<Integer> value = optionalInt(json, "value");
        if (type.get() == EffectType.SET_ATTRIBUTE && value.isEmpty()) {
            messages.add(path + ": set_attribute requires integer value");
            return Optional.empty();
        }
        return Optional.of(new DialogueEffect(type.get(), scope.get(), id.get(), value));
    }

    public Optional<String> apply(NarrativeSavedData state, UUID playerUuid) {
        switch (type) {
            case SET_FLAG -> setFlag(state, playerUuid, true);
            case CLEAR_FLAG -> setFlag(state, playerUuid, false);
            case SET_ATTRIBUTE -> {
                if (scope == DialogueScope.WORLD) {
                    return Optional.of("world_attribute_effect_ignored:" + id);
                }
                state.setAttribute(playerUuid, id, attributeValue.orElse(0));
            }
            case GIVE_ITEM_PLACEHOLDER -> {
                state.setPlayerFlag(playerUuid, "item:" + id, true);
                return Optional.of("item_placeholder_give:" + id);
            }
            case TAKE_ITEM_PLACEHOLDER -> {
                state.setPlayerFlag(playerUuid, "item:" + id, false);
                return Optional.of("item_placeholder_take:" + id);
            }
            case ROUTINE_PLACEHOLDER -> {
                setFlag(state, playerUuid, true);
                return Optional.of("routine_placeholder:" + id);
            }
        }
        return Optional.empty();
    }

    public String debugSummary() {
        String value = attributeValue.map(v -> "=" + v).orElse("");
        return type.name().toLowerCase(Locale.ROOT) + "(" + scope.name().toLowerCase(Locale.ROOT) + "," + id + value + ")";
    }

    private void setFlag(NarrativeSavedData state, UUID playerUuid, boolean value) {
        switch (scope) {
            case PLAYER -> state.setPlayerFlag(playerUuid, id, value);
            case WORLD -> state.setWorldFlag(id, value);
        }
    }

    private static Optional<String> optionalString(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull()
                ? Optional.of(GsonHelper.getAsString(json, key))
                : Optional.empty();
    }

    private static Optional<Integer> optionalInt(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull()
                ? Optional.of(GsonHelper.getAsInt(json, key))
                : Optional.empty();
    }
}
