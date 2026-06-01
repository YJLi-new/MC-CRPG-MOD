package com.crpg.ebb.chime;

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

public final class ChimeRegistry {
    private static volatile Map<Identifier, ChimeDefinition> definitions = Map.of();
    private static volatile List<String> validationMessages = List.of();

    private ChimeRegistry() {
    }

    public static void rebuild(Map<Identifier, JsonObject> rawEntries) {
        Map<Identifier, ChimeDefinition> next = new LinkedHashMap<>();
        List<String> messages = new ArrayList<>();
        for (Map.Entry<Identifier, JsonObject> entry : rawEntries.entrySet()) {
            try {
                ChimeDefinition.parse(entry.getKey(), entry.getValue(), messages).ifPresent(definition -> next.put(definition.id(), definition));
            } catch (RuntimeException ex) {
                messages.add("chime " + entry.getKey() + ": parser exception: " + ex.getMessage());
            }
        }
        definitions = Collections.unmodifiableMap(next);
        validationMessages = List.copyOf(messages);
        EbbMod.LOGGER.info("Chime registry rebuilt: {} chime(s), {} message(s).", definitions.size(), validationMessages.size());
        validationMessages.forEach(message -> EbbMod.LOGGER.warn("chime validation: {}", message));
    }

    public static Optional<ChimeDefinition> byId(Identifier id) {
        return Optional.ofNullable(definitions.get(id));
    }

    public static Map<Identifier, ChimeDefinition> definitions() {
        return definitions;
    }

    public static List<ChimeDefinition> orderedDefinitions() {
        return definitions.values().stream().sorted(Comparator.comparing(definition -> definition.id().toString())).toList();
    }

    public static int size() {
        return definitions.size();
    }

    public static List<String> validationMessages() {
        return validationMessages;
    }

    public static String summaryLine() {
        return "chimes(valid=" + definitions.size() + ", validation_messages=" + validationMessages.size() + ")";
    }
}
