package com.crpg.ebb.npc.profile;

import com.crpg.ebb.EbbMod;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class NpcProfileRegistry {
    private static volatile Map<Identifier, NpcProfileDefinition> definitions = Map.of();
    private static volatile Map<Identifier, Identifier> profileByEntityBinding = Map.of();
    private static volatile List<String> validationMessages = List.of();

    private NpcProfileRegistry() {
    }

    public static void rebuild(Map<Identifier, JsonObject> rawEntries) {
        Map<Identifier, NpcProfileDefinition> parsed = new LinkedHashMap<>();
        Map<Identifier, Identifier> byBinding = new LinkedHashMap<>();
        List<String> messages = new ArrayList<>();
        for (Map.Entry<Identifier, JsonObject> entry : rawEntries.entrySet()) {
            try {
                NpcProfileDefinition.parse(entry.getKey(), entry.getValue(), messages).ifPresent(definition -> {
                    parsed.put(definition.id(), definition);
                    definition.entityBinding().ifPresent(binding -> {
                        Identifier previous = byBinding.put(binding, definition.id());
                        if (previous != null) {
                            messages.add("npc profile " + definition.id() + ": entity_binding " + binding
                                    + " already claimed by " + previous);
                        }
                    });
                });
            } catch (RuntimeException ex) {
                messages.add("npc profile " + entry.getKey() + ": parser exception: " + ex.getMessage());
            }
        }
        definitions = Collections.unmodifiableMap(new LinkedHashMap<>(parsed));
        profileByEntityBinding = Collections.unmodifiableMap(new LinkedHashMap<>(byBinding));
        validationMessages = List.copyOf(messages);
        EbbMod.LOGGER.info("NPC profile registry rebuilt: {} profile(s), {} message(s).", definitions.size(), validationMessages.size());
        validationMessages.forEach(message -> EbbMod.LOGGER.warn("npc profile validation: {}", message));
    }

    public static Optional<NpcProfileDefinition> byId(Identifier id) {
        return Optional.ofNullable(definitions.get(id));
    }

    public static Optional<NpcProfileDefinition> byEntityBinding(Identifier bindingId) {
        Identifier profileId = profileByEntityBinding.get(bindingId);
        return profileId == null ? Optional.empty() : byId(profileId);
    }

    public static Map<Identifier, NpcProfileDefinition> definitions() {
        return definitions;
    }

    public static List<NpcProfileDefinition> orderedDefinitions() {
        return definitions.values().stream()
                .sorted(java.util.Comparator.comparing(definition -> definition.id().toString()))
                .toList();
    }

    public static int size() {
        return definitions.size();
    }

    public static List<String> validationMessages() {
        return validationMessages;
    }

    public static String summaryLine() {
        Map<NpcTier, Long> counts = new EnumMap<>(NpcTier.class);
        for (NpcProfileDefinition definition : definitions.values()) {
            counts.merge(definition.tier(), 1L, Long::sum);
        }
        return "npc_profiles(valid=" + definitions.size()
                + ", major_scripted=" + counts.getOrDefault(NpcTier.MAJOR_SCRIPTED, 0L)
                + ", major_promoted_static=" + counts.getOrDefault(NpcTier.MAJOR_PROMOTED, 0L)
                + ", validation_messages=" + validationMessages.size() + ")";
    }

    public static List<String> debugLines(int limit) {
        List<String> lines = new ArrayList<>();
        lines.add("NPC profiles (" + definitions.size() + "):");
        if (definitions.isEmpty()) {
            lines.add("- none");
            return List.copyOf(lines);
        }
        orderedDefinitions().stream().limit(Math.max(0, limit)).forEach(definition -> lines.add("- " + definition.debugSummary()));
        if (definitions.size() > limit) {
            lines.add("- ... " + (definitions.size() - limit) + " more");
        }
        return List.copyOf(lines);
    }
}
