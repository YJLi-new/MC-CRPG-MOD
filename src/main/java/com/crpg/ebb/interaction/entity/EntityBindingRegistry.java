package com.crpg.ebb.interaction.entity;

import com.crpg.ebb.EbbMod;
import com.crpg.ebb.interaction.InteractionSettings;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class EntityBindingRegistry {
    private static volatile Map<Identifier, EntityBindingDefinition> definitions = Map.of();
    private static volatile List<EntityBindingDefinition> sorted = List.of();
    private static volatile List<String> validationMessages = List.of();

    private EntityBindingRegistry() {
    }

    public static void rebuild(Map<Identifier, JsonObject> rawEntries) {
        Map<Identifier, EntityBindingDefinition> parsed = new LinkedHashMap<>();
        List<String> messages = new ArrayList<>();
        for (Map.Entry<Identifier, JsonObject> entry : rawEntries.entrySet()) {
            try {
                EntityBindingDefinition.parse(entry.getKey(), entry.getValue(), messages)
                        .ifPresent(definition -> parsed.put(definition.id(), definition));
            } catch (RuntimeException ex) {
                messages.add("entity binding " + entry.getKey() + ": parser exception: " + ex.getMessage());
            }
        }
        applyDefinitions(parsed, messages, "rebuilt");
    }

    public static Optional<EntityBindingDefinition> resolve(Entity entity) {
        for (EntityBindingDefinition definition : sorted) {
            if (definition.matches(entity)) {
                return Optional.of(definition);
            }
        }
        return InteractionSettings.enableDebugEntityFallback()
                ? Optional.of(InteractionSettings.debugFallbackDefinition())
                : Optional.empty();
    }

    public static boolean isRegisteredTarget(Entity entity) {
        return resolve(entity).isPresent();
    }

    public static void syncFromServer(List<EntityBindingDefinition> syncedDefinitions, InteractionSettings.Snapshot settings) {
        InteractionSettings.applySynced(settings);
        Map<Identifier, EntityBindingDefinition> synced = new LinkedHashMap<>();
        for (EntityBindingDefinition definition : syncedDefinitions) {
            synced.put(definition.id(), definition);
        }
        applyDefinitions(synced, List.of(), "synced");
    }

    public static void clearSynced() {
        definitions = Map.of();
        sorted = List.of();
        validationMessages = List.of();
        InteractionSettings.resetToDefaults();
    }

    public static Map<Identifier, EntityBindingDefinition> definitions() {
        return definitions;
    }

    public static List<String> validationMessages() {
        return validationMessages;
    }

    public static int size() {
        return definitions.size();
    }

    public static String summaryLine() {
        return "entity_bindings(valid=" + definitions.size()
                + ", debug_fallback=" + InteractionSettings.enableDebugEntityFallback()
                + ", validation_messages=" + validationMessages.size() + ")";
    }

    private static void applyDefinitions(Map<Identifier, EntityBindingDefinition> newDefinitions, List<String> messages, String source) {
        List<EntityBindingDefinition> sortedDefinitions = new ArrayList<>(newDefinitions.values());
        sortedDefinitions.sort(Comparator.comparingInt(EntityBindingDefinition::specificityScore).reversed()
                .thenComparing(definition -> definition.id().toString()));
        definitions = Collections.unmodifiableMap(new LinkedHashMap<>(newDefinitions));
        sorted = List.copyOf(sortedDefinitions);
        validationMessages = List.copyOf(messages);
        EbbMod.LOGGER.info("Entity binding registry {}: {} binding(s), debug fallback={}, {} message(s).",
                source, definitions.size(), InteractionSettings.enableDebugEntityFallback(), validationMessages.size());
        validationMessages.forEach(message -> EbbMod.LOGGER.warn("entity binding validation: {}", message));
    }
}
