package com.crpg.ebb.dialogue;

import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public record DialogueCheck(
        String attribute,
        int dc,
        String die,
        Optional<String> success,
        Optional<String> failure,
        Optional<String> criticalSuccess,
        Optional<String> criticalFailure,
        List<DialogueEffect> successEffects,
        List<DialogueEffect> failureEffects,
        List<DialogueEffect> criticalSuccessEffects,
        List<DialogueEffect> criticalFailureEffects
) {
    public DialogueCheck {
        success = success == null ? Optional.empty() : success;
        failure = failure == null ? Optional.empty() : failure;
        criticalSuccess = criticalSuccess == null ? Optional.empty() : criticalSuccess;
        criticalFailure = criticalFailure == null ? Optional.empty() : criticalFailure;
        successEffects = successEffects == null ? List.of() : List.copyOf(successEffects);
        failureEffects = failureEffects == null ? List.of() : List.copyOf(failureEffects);
        criticalSuccessEffects = criticalSuccessEffects == null ? List.of() : List.copyOf(criticalSuccessEffects);
        criticalFailureEffects = criticalFailureEffects == null ? List.of() : List.copyOf(criticalFailureEffects);
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
                optionalString(json, "critical_failure"),
                DialogueEffect.parseList(json, "success_effects", path, messages),
                DialogueEffect.parseList(json, "failure_effects", path, messages),
                DialogueEffect.parseList(json, "critical_success_effects", path, messages),
                DialogueEffect.parseList(json, "critical_failure_effects", path, messages)
        ));
    }

    public List<DialogueEffect> effectsForOutcome(String outcome) {
        return switch (outcome) {
            case "critical_success" -> !criticalSuccessEffects.isEmpty() ? criticalSuccessEffects : successEffects;
            case "critical_failure" -> !criticalFailureEffects.isEmpty() ? criticalFailureEffects : failureEffects;
            case "success" -> successEffects;
            case "failure" -> failureEffects;
            default -> List.of();
        };
    }

    public String debugSummary() {
        StringBuilder builder = new StringBuilder(attribute + " DC " + dc + " " + die);
        success.ifPresent(value -> builder.append(" success=").append(value));
        failure.ifPresent(value -> builder.append(" failure=").append(value));
        criticalSuccess.ifPresent(value -> builder.append(" crit_success=").append(value));
        criticalFailure.ifPresent(value -> builder.append(" crit_failure=").append(value));
        appendEffects(builder, " success_effects", successEffects);
        appendEffects(builder, " failure_effects", failureEffects);
        appendEffects(builder, " critical_success_effects", criticalSuccessEffects);
        appendEffects(builder, " critical_failure_effects", criticalFailureEffects);
        return builder.toString();
    }

    private static void appendEffects(StringBuilder builder, String label, List<DialogueEffect> effects) {
        if (!effects.isEmpty()) {
            builder.append(label).append("=").append(effects.stream().map(DialogueEffect::debugSummary).collect(Collectors.joining(",", "[", "]")));
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
