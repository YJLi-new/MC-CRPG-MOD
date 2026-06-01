package com.crpg.ebb.relationship;

import com.crpg.ebb.EbbMod;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class RelationshipRegistry {
    private static volatile Map<Identifier, RelationshipDefinition> definitions = Map.of();
    private static volatile Map<String, RelationshipDefinition> byNarrativeKey = Map.of();
    private static volatile List<String> validationMessages = List.of();

    private RelationshipRegistry() {
    }

    public static void rebuild(Map<Identifier, JsonObject> rawEntries) {
        Map<Identifier, RelationshipDefinition> next = new LinkedHashMap<>();
        Map<String, RelationshipDefinition> nextByKey = new LinkedHashMap<>();
        List<String> messages = new ArrayList<>();
        for (Map.Entry<Identifier, JsonObject> entry : rawEntries.entrySet()) {
            try {
                RelationshipDefinition.parse(entry.getKey(), entry.getValue(), messages).ifPresent(definition -> {
                    RelationshipDefinition previous = next.put(definition.id(), definition);
                    if (previous != null) {
                        messages.add("relationship " + definition.id() + ": duplicate definition overrides earlier entry");
                    }
                    RelationshipDefinition previousKey = nextByKey.put(definition.narrativeKey(), definition);
                    if (previousKey != null && !previousKey.id().equals(definition.id())) {
                        messages.add("relationship key " + definition.narrativeKey() + ": duplicate key used by "
                                + previousKey.id() + " and " + definition.id());
                    }
                });
            } catch (RuntimeException ex) {
                messages.add("relationship " + entry.getKey() + ": parser exception: " + ex.getMessage());
            }
        }
        definitions = Collections.unmodifiableMap(next);
        byNarrativeKey = Collections.unmodifiableMap(nextByKey);
        validationMessages = List.copyOf(messages);
        EbbMod.LOGGER.info("Relationship registry rebuilt: {} relationship(s), {} message(s).", definitions.size(), validationMessages.size());
        validationMessages.forEach(message -> EbbMod.LOGGER.warn("relationship validation: {}", message));
    }

    public static Optional<RelationshipDefinition> byId(Identifier id) {
        return Optional.ofNullable(definitions.get(id));
    }

    public static Optional<RelationshipDefinition> byNarrativeKey(String key) {
        return Optional.ofNullable(byNarrativeKey.get(RelationshipDefinition.normalizeKey(key)));
    }

    public static int defaultScore(String key) {
        return byNarrativeKey(key).map(RelationshipDefinition::defaultScore).orElse(0);
    }

    public static List<RelationshipDefinition> orderedDefinitions() {
        return definitions.values().stream().sorted(Comparator.comparing(definition -> definition.id().toString())).toList();
    }

    public static int size() {
        return definitions.size();
    }

    public static List<String> validationMessages() {
        return validationMessages;
    }

    public static String summaryLine() {
        return "relationships(valid=" + definitions.size() + ", validation_messages=" + validationMessages.size() + ")";
    }
}
