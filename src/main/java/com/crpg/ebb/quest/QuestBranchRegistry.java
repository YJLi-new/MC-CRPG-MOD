package com.crpg.ebb.quest;

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

public final class QuestBranchRegistry {
    private static volatile Map<Identifier, QuestBranchDefinition> definitions = Map.of();
    private static volatile List<String> validationMessages = List.of();

    private QuestBranchRegistry() {
    }

    public static void rebuild(Map<Identifier, JsonObject> rawEntries) {
        Map<Identifier, QuestBranchDefinition> next = new LinkedHashMap<>();
        List<String> messages = new ArrayList<>();
        for (Map.Entry<Identifier, JsonObject> entry : rawEntries.entrySet()) {
            try {
                QuestBranchDefinition.parse(entry.getKey(), entry.getValue(), messages).ifPresent(definition -> {
                    QuestBranchDefinition previous = next.put(definition.id(), definition);
                    if (previous != null) {
                        messages.add("quest_branch " + definition.id() + ": duplicate definition overrides earlier entry");
                    }
                });
            } catch (RuntimeException ex) {
                messages.add("quest_branch " + entry.getKey() + ": parser exception: " + ex.getMessage());
            }
        }
        definitions = Collections.unmodifiableMap(next);
        validationMessages = List.copyOf(messages);
        EbbMod.LOGGER.info("Quest branch registry rebuilt: {} branch(es), {} message(s).", definitions.size(), validationMessages.size());
        validationMessages.forEach(message -> EbbMod.LOGGER.warn("quest branch validation: {}", message));
    }

    public static Optional<QuestBranchDefinition> byId(Identifier id) {
        return Optional.ofNullable(definitions.get(id));
    }

    public static Map<Identifier, QuestBranchDefinition> definitions() {
        return definitions;
    }

    public static List<QuestBranchDefinition> orderedDefinitions() {
        return definitions.values().stream().sorted(Comparator.comparing(definition -> definition.id().toString())).toList();
    }

    public static int size() {
        return definitions.size();
    }

    public static List<String> validationMessages() {
        return validationMessages;
    }

    public static String summaryLine() {
        long majors = definitions.values().stream().filter(definition -> definition.kind() == QuestBranchKind.MAJOR).count();
        return "quest_branches(valid=" + definitions.size() + ", major=" + majors + ", validation_messages=" + validationMessages.size() + ")";
    }
}
