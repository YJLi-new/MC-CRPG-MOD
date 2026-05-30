package com.crpg.ebb.dialogue;

import com.crpg.ebb.EbbMod;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class DialogueRegistry {
    private static volatile Map<Identifier, DialogueDefinition> definitions = Map.of();
    private static volatile List<String> validationMessages = List.of();

    private DialogueRegistry() {
    }

    public static void rebuild(Map<Identifier, JsonObject> rawEntries) {
        Map<Identifier, DialogueDefinition> parsed = new LinkedHashMap<>();
        List<String> messages = new ArrayList<>();
        for (Map.Entry<Identifier, JsonObject> entry : rawEntries.entrySet()) {
            try {
                DialogueDefinition.parse(entry.getKey(), entry.getValue(), messages)
                        .ifPresent(definition -> parsed.put(entry.getKey(), definition));
            } catch (RuntimeException ex) {
                messages.add("dialogue " + entry.getKey() + ": parser exception: " + ex.getMessage());
            }
        }
        definitions = Collections.unmodifiableMap(parsed);
        validationMessages = List.copyOf(messages);
        EbbMod.LOGGER.info("Dialogue registry rebuilt: {} valid dialogue(s), {} message(s).",
                definitions.size(), validationMessages.size());
        for (String message : validationMessages) {
            EbbMod.LOGGER.warn("dialogue validation: {}", message);
        }
    }

    public static Optional<DialogueDefinition> byId(Identifier id) {
        return Optional.ofNullable(definitions.get(id));
    }

    public static Map<Identifier, DialogueDefinition> definitions() {
        return definitions;
    }

    public static List<String> validationMessages() {
        return validationMessages;
    }

    public static int size() {
        return definitions.size();
    }

    public static String summaryLine() {
        return "dialogues(valid=" + definitions.size() + ", validation_messages=" + validationMessages.size() + ")";
    }
}
