package com.crpg.ebb.dialogue;

import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

import java.util.List;
import java.util.Optional;

public record DialogueCheck(
        String attribute,
        int dc,
        String die,
        Optional<String> success,
        Optional<String> failure,
        Optional<String> criticalSuccess,
        Optional<String> criticalFailure
) {
    public DialogueCheck {
        success = success == null ? Optional.empty() : success;
        failure = failure == null ? Optional.empty() : failure;
        criticalSuccess = criticalSuccess == null ? Optional.empty() : criticalSuccess;
        criticalFailure = criticalFailure == null ? Optional.empty() : criticalFailure;
    }

    static Optional<DialogueCheck> parse(JsonObject json, String path, List<String> messages) {
        if (json == null) {
            return Optional.empty();
        }
        String attribute = optionalString(json, "attribute").orElse("logic");
        int dc = optionalInt(json, "dc").orElse(10);
        String die = optionalString(json, "die").orElse("d20");
        if (!"d20".equalsIgnoreCase(die)) {
            messages.add(path + ": only die=\"d20\" is currently supported; got " + die);
        }
        if (dc < 1) {
            messages.add(path + ": dc should be >= 1; got " + dc);
        }
        return Optional.of(new DialogueCheck(
                attribute,
                dc,
                die,
                optionalString(json, "success"),
                optionalString(json, "failure"),
                optionalString(json, "critical_success"),
                optionalString(json, "critical_failure")
        ));
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
