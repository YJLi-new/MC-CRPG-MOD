package com.crpg.ebb.interaction.entity;

import com.crpg.ebb.EbbMod;
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
        List<EntityBindingDefinition> sortedDefinitions = new ArrayList<>(parsed.values());
        sortedDefinitions.sort(Comparator.comparingInt(EntityBindingDefinition::specificityScore).reversed()
                .thenComparing(definition -> definition.id().toString()));
        definitions = Collections.unmodifiableMap(parsed);
        sorted = List.copyOf(sortedDefinitions);
        validationMessages = List.copyOf(messages);
        EbbMod.LOGGER.info("Entity binding registry rebuilt: {} binding(s), {} message(s).", definitions.size(), validationMessages.size());
        validationMessages.forEach(message -> EbbMod.LOGGER.warn("entity binding validation: {}", message));
    }

    public static Optional<EntityBindingDefinition> resolve(Entity entity) {
        for (EntityBindingDefinition definition : sorted) {
            if (definition.matches(entity)) {
                return Optional.of(definition);
            }
        }
        return EntityBindingDefinition.fallback(entity);
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
        return "entity_bindings(valid=" + definitions.size() + ", validation_messages=" + validationMessages.size() + ")";
    }
}
