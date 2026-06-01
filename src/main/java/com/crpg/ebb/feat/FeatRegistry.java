package com.crpg.ebb.feat;

import com.crpg.ebb.EbbMod;
import com.crpg.ebb.attribute.AttributeRegistry;
import com.crpg.ebb.state.NarrativeSavedData;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class FeatRegistry {
    private static volatile Map<Identifier, FeatDefinition> definitions = Map.of();
    private static volatile List<String> validationMessages = List.of();

    private FeatRegistry() {
    }

    public static void rebuild(Map<Identifier, JsonObject> rawEntries) {
        Map<Identifier, FeatDefinition> next = new LinkedHashMap<>();
        List<String> messages = new ArrayList<>();
        for (Map.Entry<Identifier, JsonObject> entry : rawEntries.entrySet()) {
            try {
                FeatDefinition.parse(entry.getKey(), entry.getValue(), messages).ifPresent(definition -> {
                    FeatDefinition previous = next.put(definition.id(), definition);
                    if (previous != null) {
                        messages.add("feat " + definition.id() + ": duplicate definition overrides earlier entry");
                    }
                });
            } catch (RuntimeException ex) {
                messages.add("feat " + entry.getKey() + ": parser exception: " + ex.getMessage());
            }
        }
        definitions = Collections.unmodifiableMap(next);
        validationMessages = List.copyOf(messages);
        EbbMod.LOGGER.info("Feat registry rebuilt: {} feat(s), {} message(s).", definitions.size(), validationMessages.size());
        validationMessages.forEach(message -> EbbMod.LOGGER.warn("feat validation: {}", message));
    }

    public static Optional<FeatDefinition> byId(Identifier id) {
        return Optional.ofNullable(definitions.get(id));
    }

    public static Map<Identifier, FeatDefinition> definitions() {
        return definitions;
    }

    public static List<FeatDefinition> orderedDefinitions() {
        return definitions.values().stream().sorted(Comparator.comparing(definition -> definition.id().toString())).toList();
    }

    public static int totalCheckModifier(NarrativeSavedData state, UUID playerUuid, String attribute) {
        String canonical = AttributeRegistry.canonicalKey(attribute);
        int total = 0;
        for (FeatDefinition definition : definitions.values()) {
            String id = definition.id().toString();
            if (!state.hasFeat(playerUuid, id)) {
                continue;
            }
            if (!definition.permanentPassive() && !state.isFeatActive(playerUuid, id)) {
                continue;
            }
            total += definition.modifierFor(canonical);
        }
        return total;
    }

    public static int size() {
        return definitions.size();
    }

    public static List<String> validationMessages() {
        return validationMessages;
    }

    public static String summaryLine() {
        long slotCandidates = definitions.values().stream().filter(FeatDefinition::activeSlotCandidate).count();
        return "feats(valid=" + definitions.size() + ", slot_candidates=" + slotCandidates + ", validation_messages=" + validationMessages.size() + ")";
    }
}
