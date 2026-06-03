package com.crpg.ebb.conflict;

import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record ConflictDefinition(
        Identifier id,
        String title,
        Optional<Identifier> scene,
        int stressLimit,
        int resolveGoal,
        String failureState,
        String successState,
        List<String> phases,
        Map<String, String> phaseDescriptions,
        List<Identifier> leverageClues,
        List<ConflictOutcomeDefinition> outcomes
) {
    public static final List<String> REQUIRED_PHASES = List.of("setup", "pressure", "turn", "consequence", "resolution");

    public ConflictDefinition {
        title = title == null || title.isBlank() ? id.toString() : title;
        scene = scene == null ? Optional.empty() : scene;
        stressLimit = stressLimit <= 0 ? 3 : stressLimit;
        resolveGoal = resolveGoal <= 0 ? 3 : resolveGoal;
        failureState = failureState == null || failureState.isBlank() ? "failed_forward" : failureState;
        successState = successState == null || successState.isBlank() ? "resolved" : successState;
        phases = phases == null || phases.isEmpty() ? REQUIRED_PHASES : List.copyOf(phases);
        phaseDescriptions = phaseDescriptions == null ? Map.of() : Map.copyOf(phaseDescriptions);
        leverageClues = leverageClues == null ? List.of() : List.copyOf(leverageClues);
        outcomes = outcomes == null ? List.of() : List.copyOf(outcomes);
    }

    public static Optional<ConflictDefinition> parse(Identifier fileId, JsonObject json, java.util.List<String> messages) {
        Identifier id = optionalString(json, "id").map(value -> parseIdentifier(value, fileId, messages)).orElse(fileId);
        String title = optionalString(json, "title").or(() -> optionalString(json, "name")).orElse(fileId.toString());
        Optional<Identifier> scene = optionalString(json, "scene").map(value -> parseIdentifier(value, fileId, messages));
        int stressLimit = optionalInt(json, "stress_limit").or(() -> optionalInt(json, "stressLimit")).orElse(3);
        int resolveGoal = optionalInt(json, "resolve_goal").or(() -> optionalInt(json, "resolveGoal")).orElse(3);
        String failureState = optionalString(json, "failure_state").orElse("failed_forward");
        String successState = optionalString(json, "success_state").orElse("resolved");
        ParsedPhases parsedPhases = parsePhases(id, json, messages);
        List<Identifier> leverageClues = parseIdentifierArray(id, json, "leverage_clues", messages);
        if (leverageClues.isEmpty()) {
            leverageClues = parseIdentifierArray(id, json, "leverageClues", messages);
        }
        List<ConflictOutcomeDefinition> outcomes = parseOutcomes(id, json, messages);
        return Optional.of(new ConflictDefinition(
                id,
                title,
                scene,
                stressLimit,
                resolveGoal,
                failureState,
                successState,
                parsedPhases.phases(),
                parsedPhases.descriptions(),
                leverageClues,
                outcomes
        ));
    }

    public String debugSummary() {
        return id + " title=\"" + title + "\" stress_limit=" + stressLimit + " resolve_goal=" + resolveGoal
                + " phases=" + phases + " leverage=" + leverageClues.size() + " outcomes=" + outcomes.size();
    }

    public Optional<ConflictOutcomeDefinition> outcome(String outcomeId) {
        String normalized = outcomeId == null ? "" : outcomeId.trim().toLowerCase(java.util.Locale.ROOT).replace('-', '_');
        return outcomes.stream().filter(outcome -> outcome.id().equals(normalized)).findFirst();
    }

    public boolean hasRequiredPhase(String phase) {
        return phases.contains(phase);
    }

    private record ParsedPhases(List<String> phases, Map<String, String> descriptions) {
    }

    private static Identifier parseIdentifier(String raw, Identifier fileId, java.util.List<String> messages) {
        try {
            return raw.contains(":") ? Identifier.parse(raw) : Identifier.fromNamespaceAndPath(fileId.getNamespace(), raw);
        } catch (RuntimeException ex) {
            messages.add("conflict " + fileId + ": invalid id " + raw + " (" + ex.getMessage() + ")");
            return fileId;
        }
    }

    private static ParsedPhases parsePhases(Identifier id, JsonObject json, java.util.List<String> messages) {
        if (!json.has("phases") || !json.get("phases").isJsonArray()) {
            return new ParsedPhases(REQUIRED_PHASES, Map.of());
        }
        List<String> phases = new ArrayList<>();
        Map<String, String> descriptions = new LinkedHashMap<>();
        int index = 0;
        for (JsonElement element : json.getAsJsonArray("phases")) {
            String phaseId = "";
            String description = "";
            if (element.isJsonPrimitive()) {
                phaseId = element.getAsString();
            } else if (element.isJsonObject()) {
                JsonObject object = element.getAsJsonObject();
                phaseId = optionalString(object, "id").or(() -> optionalString(object, "name")).orElse("");
                description = optionalString(object, "text")
                        .or(() -> optionalString(object, "description"))
                        .orElse("");
            } else {
                messages.add("conflict " + id + ".phases[" + index + "]: phase must be a string or object");
            }
            String normalized = normalizePhase(phaseId);
            if (!normalized.isBlank() && !phases.contains(normalized)) {
                phases.add(normalized);
                if (!description.isBlank()) {
                    descriptions.put(normalized, description.strip());
                }
            } else if (normalized.isBlank()) {
                messages.add("conflict " + id + ".phases[" + index + "]: phase id must not be blank");
            }
            index++;
        }
        if (phases.isEmpty()) {
            phases = REQUIRED_PHASES;
        }
        return new ParsedPhases(phases, descriptions);
    }

    private static List<Identifier> parseIdentifierArray(Identifier id, JsonObject json, String key, java.util.List<String> messages) {
        if (!json.has(key)) {
            return List.of();
        }
        if (!json.get(key).isJsonArray()) {
            messages.add("conflict " + id + ": " + key + " must be an array of clue identifiers");
            return List.of();
        }
        List<Identifier> identifiers = new ArrayList<>();
        int index = 0;
        for (JsonElement element : json.getAsJsonArray(key)) {
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
                messages.add("conflict " + id + "." + key + "[" + index + "]: expected identifier string");
            } else {
                identifiers.add(parseIdentifier(element.getAsString(), id, messages));
            }
            index++;
        }
        return List.copyOf(identifiers);
    }

    private static List<ConflictOutcomeDefinition> parseOutcomes(Identifier id, JsonObject json, java.util.List<String> messages) {
        if (!json.has("outcomes")) {
            return List.of();
        }
        if (!json.get("outcomes").isJsonArray()) {
            messages.add("conflict " + id + ": outcomes must be an array");
            return List.of();
        }
        List<ConflictOutcomeDefinition> outcomes = new ArrayList<>();
        int index = 0;
        for (JsonElement element : json.getAsJsonArray("outcomes")) {
            if (!element.isJsonObject()) {
                messages.add("conflict " + id + ".outcomes[" + index + "]: outcome must be an object");
            } else {
                ConflictOutcomeDefinition.parse(id, element.getAsJsonObject(), index, messages).ifPresent(outcomes::add);
            }
            index++;
        }
        return List.copyOf(outcomes);
    }

    private static String normalizePhase(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(java.util.Locale.ROOT).replace('-', '_');
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
