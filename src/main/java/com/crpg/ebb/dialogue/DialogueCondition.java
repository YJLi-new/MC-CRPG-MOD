package com.crpg.ebb.dialogue;

import com.crpg.ebb.state.NarrativeSavedData;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record DialogueCondition(
        DialogueScope scope,
        String flag,
        boolean expected
) {
    static Optional<DialogueCondition> parse(JsonObject json, String path, List<String> messages) {
        String type = optionalString(json, "type").orElse("flag");
        if (!"flag".equalsIgnoreCase(type)) {
            messages.add(path + ": only condition type=flag is currently supported; got " + type);
            return Optional.empty();
        }
        Optional<DialogueScope> scope = DialogueScope.parse(optionalString(json, "scope").orElse("player"));
        if (scope.isEmpty()) {
            messages.add(path + ": scope must be player or world");
            return Optional.empty();
        }
        Optional<String> flag = optionalString(json, "id").or(() -> optionalString(json, "flag"));
        if (flag.isEmpty() || flag.get().isBlank()) {
            messages.add(path + ": missing flag/id string");
            return Optional.empty();
        }
        boolean expected = optionalBoolean(json, "value").orElse(true);
        return Optional.of(new DialogueCondition(scope.get(), flag.get(), expected));
    }

    public boolean matches(NarrativeSavedData state, UUID playerUuid) {
        boolean actual = switch (scope) {
            case PLAYER -> state.hasPlayerFlag(playerUuid, flag);
            case WORLD -> state.hasWorldFlag(flag);
        };
        return actual == expected;
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
