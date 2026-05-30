package com.crpg.ebb.routine;

import com.crpg.ebb.EbbMod;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class NpcRoutineRegistry {
    private static volatile Map<Identifier, NpcRoutineDefinition> definitions = Map.of();
    private static volatile List<String> validationMessages = List.of();

    private NpcRoutineRegistry() {
    }

    public static void rebuild(Map<Identifier, JsonObject> rawEntries) {
        Map<Identifier, NpcRoutineDefinition> parsed = new LinkedHashMap<>();
        List<String> messages = new ArrayList<>();
        for (Map.Entry<Identifier, JsonObject> entry : rawEntries.entrySet()) {
            try {
                NpcRoutineDefinition.parse(entry.getKey(), entry.getValue(), messages)
                        .ifPresent(definition -> parsed.put(definition.id(), definition));
            } catch (RuntimeException ex) {
                messages.add("npc routine " + entry.getKey() + ": parser exception: " + ex.getMessage());
            }
        }
        definitions = Collections.unmodifiableMap(parsed);
        validationMessages = List.copyOf(messages);
        EbbMod.LOGGER.info("NPC routine registry rebuilt: {} routine(s), {} message(s).", definitions.size(), validationMessages.size());
        validationMessages.forEach(message -> EbbMod.LOGGER.warn("npc routine validation: {}", message));
    }

    public static Optional<NpcRoutineDefinition> byId(Identifier id) {
        return Optional.ofNullable(definitions.get(id));
    }

    public static Map<Identifier, NpcRoutineDefinition> definitions() {
        return definitions;
    }

    public static List<String> validationMessages() {
        return validationMessages;
    }

    public static int size() {
        return definitions.size();
    }

    public static String summaryLine() {
        return "npc_routines(valid=" + definitions.size() + ", validation_messages=" + validationMessages.size() + ")";
    }
}
