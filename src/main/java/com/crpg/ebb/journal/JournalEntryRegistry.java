package com.crpg.ebb.journal;

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

public final class JournalEntryRegistry {
    private static volatile Map<Identifier, JournalEntryDefinition> definitions = Map.of();
    private static volatile List<String> validationMessages = List.of();

    private JournalEntryRegistry() {
    }

    public static void rebuild(Map<Identifier, JsonObject> rawEntries) {
        Map<Identifier, JournalEntryDefinition> next = new LinkedHashMap<>();
        List<String> messages = new ArrayList<>();
        for (Map.Entry<Identifier, JsonObject> entry : rawEntries.entrySet()) {
            try {
                JournalEntryDefinition.parse(entry.getKey(), entry.getValue(), messages).ifPresent(definition -> next.put(definition.id(), definition));
            } catch (RuntimeException ex) {
                messages.add("journal_entry " + entry.getKey() + ": parser exception: " + ex.getMessage());
            }
        }
        definitions = Collections.unmodifiableMap(next);
        validationMessages = List.copyOf(messages);
        EbbMod.LOGGER.info("Journal entry registry rebuilt: {} entry(s), {} message(s).", definitions.size(), validationMessages.size());
        validationMessages.forEach(message -> EbbMod.LOGGER.warn("journal entry validation: {}", message));
    }

    public static Optional<JournalEntryDefinition> byId(Identifier id) {
        return Optional.ofNullable(definitions.get(id));
    }

    public static Map<Identifier, JournalEntryDefinition> definitions() {
        return definitions;
    }

    public static List<JournalEntryDefinition> orderedDefinitions() {
        return definitions.values().stream().sorted(Comparator.comparing(definition -> definition.id().toString())).toList();
    }

    public static int size() {
        return definitions.size();
    }

    public static List<String> validationMessages() {
        return validationMessages;
    }

    public static String summaryLine() {
        long clues = definitions.values().stream().filter(definition -> definition.category() == JournalEntryCategory.CLUE).count();
        return "journal_entries(valid=" + definitions.size() + ", clues=" + clues + ", validation_messages=" + validationMessages.size() + ")";
    }
}
