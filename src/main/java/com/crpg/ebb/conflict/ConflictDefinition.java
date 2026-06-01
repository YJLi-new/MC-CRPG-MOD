package com.crpg.ebb.conflict;

import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;

import java.util.Optional;

public record ConflictDefinition(
        Identifier id,
        String title,
        Optional<Identifier> scene,
        int stressLimit,
        int resolveGoal,
        String failureState,
        String successState
) {
    public ConflictDefinition {
        title = title == null || title.isBlank() ? id.toString() : title;
        scene = scene == null ? Optional.empty() : scene;
        stressLimit = stressLimit <= 0 ? 3 : stressLimit;
        resolveGoal = resolveGoal <= 0 ? 3 : resolveGoal;
        failureState = failureState == null || failureState.isBlank() ? "failed_forward" : failureState;
        successState = successState == null || successState.isBlank() ? "resolved" : successState;
    }

    public static Optional<ConflictDefinition> parse(Identifier fileId, JsonObject json, java.util.List<String> messages) {
        Identifier id = optionalString(json, "id").map(value -> parseIdentifier(value, fileId, messages)).orElse(fileId);
        String title = optionalString(json, "title").or(() -> optionalString(json, "name")).orElse(fileId.toString());
        Optional<Identifier> scene = optionalString(json, "scene").map(value -> parseIdentifier(value, fileId, messages));
        int stressLimit = optionalInt(json, "stress_limit").or(() -> optionalInt(json, "stressLimit")).orElse(3);
        int resolveGoal = optionalInt(json, "resolve_goal").or(() -> optionalInt(json, "resolveGoal")).orElse(3);
        String failureState = optionalString(json, "failure_state").orElse("failed_forward");
        String successState = optionalString(json, "success_state").orElse("resolved");
        return Optional.of(new ConflictDefinition(id, title, scene, stressLimit, resolveGoal, failureState, successState));
    }

    public String debugSummary() {
        return id + " title=\"" + title + "\" stress_limit=" + stressLimit + " resolve_goal=" + resolveGoal;
    }

    private static Identifier parseIdentifier(String raw, Identifier fileId, java.util.List<String> messages) {
        try {
            return raw.contains(":") ? Identifier.parse(raw) : Identifier.fromNamespaceAndPath(fileId.getNamespace(), raw);
        } catch (RuntimeException ex) {
            messages.add("conflict " + fileId + ": invalid id " + raw + " (" + ex.getMessage() + ")");
            return fileId;
        }
    }

    private static Optional<String> optionalString(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull()
                ? Optional.of(GsonHelper.getAsString(json, key))
                : Optional.empty();
    }

    private static Optional<Integer> optionalInt(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull()
                && json.get(key).isJsonPrimitive()
                && json.get(key).getAsJsonPrimitive().isNumber()
                ? Optional.of(GsonHelper.getAsInt(json, key))
                : Optional.empty();
    }
}
