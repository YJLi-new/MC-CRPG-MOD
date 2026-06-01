package com.crpg.ebb.investigation;

import com.crpg.ebb.EbbMod;
import com.crpg.ebb.state.NarrativeSavedData;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class InvestigationRegistry {
    private static volatile Map<Identifier, ClueDefinition> clues = Map.of();
    private static volatile Map<Identifier, InvestigationSceneDefinition> scenes = Map.of();
    private static volatile List<String> validationMessages = List.of();

    private InvestigationRegistry() {
    }

    public static void rebuildClues(Map<Identifier, JsonObject> rawEntries) {
        Map<Identifier, ClueDefinition> next = new LinkedHashMap<>();
        List<String> messages = new ArrayList<>(sceneReferenceMessages(rawEntries));
        for (Map.Entry<Identifier, JsonObject> entry : rawEntries.entrySet()) {
            try {
                ClueDefinition.parse(entry.getKey(), entry.getValue(), messages).ifPresent(definition -> next.put(definition.id(), definition));
            } catch (RuntimeException ex) {
                messages.add("clue " + entry.getKey() + ": parser exception: " + ex.getMessage());
            }
        }
        clues = Collections.unmodifiableMap(next);
        mergeValidation(messages);
        EbbMod.LOGGER.info("Clue registry rebuilt: {} clue(s), {} message(s).", clues.size(), validationMessages.size());
        validationMessages.forEach(message -> EbbMod.LOGGER.warn("investigation validation: {}", message));
    }

    public static void rebuildScenes(Map<Identifier, JsonObject> rawEntries) {
        Map<Identifier, InvestigationSceneDefinition> next = new LinkedHashMap<>();
        List<String> messages = new ArrayList<>();
        for (Map.Entry<Identifier, JsonObject> entry : rawEntries.entrySet()) {
            try {
                InvestigationSceneDefinition.parse(entry.getKey(), entry.getValue(), messages).ifPresent(definition -> next.put(definition.id(), definition));
            } catch (RuntimeException ex) {
                messages.add("investigation_scene " + entry.getKey() + ": parser exception: " + ex.getMessage());
            }
        }
        scenes = Collections.unmodifiableMap(next);
        mergeValidation(messages);
        EbbMod.LOGGER.info("Investigation scene registry rebuilt: {} scene(s), {} message(s).", scenes.size(), validationMessages.size());
        validationMessages.forEach(message -> EbbMod.LOGGER.warn("investigation validation: {}", message));
    }

    private static List<String> sceneReferenceMessages(Map<Identifier, JsonObject> ignored) {
        return List.of();
    }

    private static void mergeValidation(List<String> messages) {
        validationMessages = List.copyOf(messages);
    }

    public static Optional<ClueDefinition> clue(Identifier id) {
        return Optional.ofNullable(clues.get(id));
    }

    public static Optional<InvestigationSceneDefinition> scene(Identifier id) {
        return Optional.ofNullable(scenes.get(id));
    }

    public static List<ClueDefinition> orderedClues() {
        return clues.values().stream().sorted(Comparator.comparing(definition -> definition.id().toString())).toList();
    }

    public static List<InvestigationSceneDefinition> orderedScenes() {
        return scenes.values().stream().sorted(Comparator.comparing(definition -> definition.id().toString())).toList();
    }

    public static int clueCount() {
        return clues.size();
    }

    public static int sceneCount() {
        return scenes.size();
    }

    public static int totalCheckModifier(NarrativeSavedData state, UUID playerUuid, String attribute) {
        return state.clueIds(playerUuid).stream()
                .map(id -> Identifier.parse(id))
                .map(InvestigationRegistry::clue)
                .flatMap(Optional::stream)
                .mapToInt(clue -> clue.modifierFor(attribute))
                .sum();
    }

    public static List<String> validationMessages() {
        return validationMessages;
    }

    public static String summaryLine() {
        return "investigation(clues=" + clues.size() + ", scenes=" + scenes.size()
                + ", validation_messages=" + validationMessages.size() + ")";
    }
}
