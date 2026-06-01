package com.crpg.ebb.conflict;

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

public final class ConflictRegistry {
    private static volatile Map<Identifier, ConflictDefinition> definitions = Map.of();
    private static volatile List<String> validationMessages = List.of();

    private ConflictRegistry() {
    }

    public static void rebuild(Map<Identifier, JsonObject> rawEntries) {
        Map<Identifier, ConflictDefinition> next = new LinkedHashMap<>();
        List<String> messages = new ArrayList<>();
        for (Map.Entry<Identifier, JsonObject> entry : rawEntries.entrySet()) {
            try {
                ConflictDefinition.parse(entry.getKey(), entry.getValue(), messages).ifPresent(definition -> next.put(definition.id(), definition));
            } catch (RuntimeException ex) {
                messages.add("conflict " + entry.getKey() + ": parser exception: " + ex.getMessage());
            }
        }
        definitions = Collections.unmodifiableMap(next);
        validationMessages = List.copyOf(messages);
        EbbMod.LOGGER.info("Conflict registry rebuilt: {} conflict(s), {} message(s).", definitions.size(), validationMessages.size());
        validationMessages.forEach(message -> EbbMod.LOGGER.warn("conflict validation: {}", message));
    }

    public static Optional<ConflictDefinition> byId(Identifier id) {
        return Optional.ofNullable(definitions.get(id));
    }

    public static List<ConflictDefinition> orderedDefinitions() {
        return definitions.values().stream().sorted(Comparator.comparing(definition -> definition.id().toString())).toList();
    }

    public static int size() {
        return definitions.size();
    }

    public static List<String> validationMessages() {
        return validationMessages;
    }

    public static String summaryLine() {
        return "conflicts(valid=" + definitions.size() + ", validation_messages=" + validationMessages.size() + ")";
    }
}
