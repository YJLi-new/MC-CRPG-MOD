package com.crpg.ebb.conflict;

import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public record ConflictOutcomeDefinition(
        String id,
        String state,
        String kind,
        String text,
        boolean failureForward
) {
    public ConflictOutcomeDefinition {
        id = normalizeToken(id, "outcome");
        state = normalizeToken(state, id);
        kind = normalizeToken(kind, "resolution");
        text = text == null ? "" : text.strip();
    }

    public static Optional<ConflictOutcomeDefinition> parse(Identifier conflictId, JsonObject json, int index, List<String> messages) {
        String fallback = "outcome_" + index;
        String id = optionalString(json, "id").orElse(fallback).trim();
        if (id.isBlank()) {
            messages.add("conflict " + conflictId + ".outcomes[" + index + "]: id must not be blank");
            return Optional.empty();
        }
        String kind = optionalString(json, "kind").or(() -> optionalString(json, "type")).orElse("resolution");
        String state = optionalString(json, "state").or(() -> optionalString(json, "conflict_state")).orElse(id);
        String text = optionalString(json, "text")
                .or(() -> optionalString(json, "description"))
                .or(() -> optionalString(json, "summary"))
                .orElse("");
        boolean failureForward = optionalBoolean(json, "failure_forward")
                .or(() -> optionalBoolean(json, "failureForward"))
                .orElse(kind.equalsIgnoreCase("failure_forward") || kind.equalsIgnoreCase("fail_forward"));
        return Optional.of(new ConflictOutcomeDefinition(id, state, kind, text, failureForward));
    }

    public boolean isFailureForwardKind() {
        return failureForward || "failure_forward".equals(kind) || "fail_forward".equals(kind);
    }

    public boolean isMessyKind() {
        return "messy".equals(kind) || kind.contains("messy");
    }

    public boolean isNonViolentKind() {
        return "nonviolent".equals(kind) || "non_violent".equals(kind) || "quiet".equals(kind);
    }

    public String debugSummary() {
        return id + "[" + kind + "->" + state + (failureForward ? ",fail-forward" : "") + "]";
    }

    private static String normalizeToken(String value, String fallback) {
        String normalized = value == null || value.isBlank() ? fallback : value.trim();
        return normalized.toLowerCase(Locale.ROOT).replace('-', '_');
    }

    private static Optional<String> optionalString(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull()
                ? Optional.of(GsonHelper.getAsString(json, key))
                : Optional.empty();
    }

    private static Optional<Boolean> optionalBoolean(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull()
                && json.get(key).isJsonPrimitive()
                && json.get(key).getAsJsonPrimitive().isBoolean()
                ? Optional.of(GsonHelper.getAsBoolean(json, key))
                : Optional.empty();
    }
}
