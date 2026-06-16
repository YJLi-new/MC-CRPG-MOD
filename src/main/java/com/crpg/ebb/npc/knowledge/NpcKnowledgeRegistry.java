package com.crpg.ebb.npc.knowledge;

import com.crpg.ebb.EbbMod;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class NpcKnowledgeRegistry {
    private static volatile Map<Identifier, NpcKnowledgePackDefinition> definitions = Map.of();
    private static volatile List<String> validationMessages = List.of();

    private NpcKnowledgeRegistry() {}

    public static void rebuild(Map<Identifier, JsonObject> rawEntries) {
        Map<Identifier, NpcKnowledgePackDefinition> parsed = new LinkedHashMap<>();
        List<String> messages = new ArrayList<>();
        for (Map.Entry<Identifier, JsonObject> entry : rawEntries.entrySet()) {
            try {
                NpcKnowledgePackDefinition.parse(entry.getKey(), entry.getValue(), messages)
                        .ifPresent(definition -> parsed.put(definition.id(), definition));
            } catch (RuntimeException ex) {
                messages.add("npc knowledge pack " + entry.getKey() + ": parser exception: " + ex.getMessage());
            }
        }
        definitions = Collections.unmodifiableMap(new LinkedHashMap<>(parsed));
        validationMessages = List.copyOf(messages);
        EbbMod.LOGGER.info("NPC knowledge registry rebuilt: {} pack(s), {} message(s).", definitions.size(), validationMessages.size());
        validationMessages.forEach(message -> EbbMod.LOGGER.warn("npc knowledge validation: {}", message));
    }

    public static Optional<NpcKnowledgePackDefinition> byId(Identifier id) {
        return Optional.ofNullable(definitions.get(id));
    }

    public static Map<Identifier, NpcKnowledgePackDefinition> definitions() {
        return definitions;
    }

    public static int size() {
        return definitions.size();
    }

    public static List<String> validationMessages() {
        return validationMessages;
    }

    public static String summaryLine() {
        int chunks = definitions.values().stream().mapToInt(pack -> pack.chunks().size()).sum();
        return "npc_knowledge(valid=" + definitions.size() + ", chunks=" + chunks + ", validation_messages=" + validationMessages.size() + ")";
    }

    public static List<String> debugLines(int limit) {
        List<String> lines = new ArrayList<>();
        lines.add("NPC knowledge packs (" + definitions.size() + "):");
        if (definitions.isEmpty()) {
            lines.add("- none");
            return List.copyOf(lines);
        }
        definitions.values().stream()
                .sorted(java.util.Comparator.comparing(pack -> pack.id().toString()))
                .limit(Math.max(0, limit))
                .forEach(pack -> lines.add("- " + pack.debugSummary()));
        if (definitions.size() > limit) {
            lines.add("- ... " + (definitions.size() - limit) + " more");
        }
        return List.copyOf(lines);
    }
}
