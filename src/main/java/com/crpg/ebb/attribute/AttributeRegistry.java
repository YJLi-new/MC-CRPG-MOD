package com.crpg.ebb.attribute;

import com.crpg.ebb.EbbMod;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class AttributeRegistry {
    private static volatile Map<String, AttributeDefinition> byKey = Map.of();
    private static volatile Map<Identifier, AttributeDefinition> byId = Map.of();
    private static volatile List<String> validationMessages = List.of();

    private AttributeRegistry() {
    }

    public static void rebuild(Map<Identifier, JsonObject> rawEntries) {
        Map<String, AttributeDefinition> nextByKey = new LinkedHashMap<>();
        Map<Identifier, AttributeDefinition> nextById = new LinkedHashMap<>();
        List<String> messages = new ArrayList<>();
        for (Map.Entry<Identifier, JsonObject> entry : rawEntries.entrySet()) {
            try {
                AttributeDefinition.parse(entry.getKey(), entry.getValue(), messages).ifPresent(definition -> {
                    nextById.put(definition.id(), definition);
                    putKey(nextByKey, definition.key(), definition, messages);
                    putKey(nextByKey, definition.id().toString(), definition, messages);
                    putKey(nextByKey, definition.id().getPath(), definition, messages);
                });
            } catch (RuntimeException ex) {
                messages.add("attribute " + entry.getKey() + ": parser exception: " + ex.getMessage());
            }
        }
        byId = Collections.unmodifiableMap(nextById);
        byKey = Collections.unmodifiableMap(nextByKey);
        validationMessages = List.copyOf(messages);
        EbbMod.LOGGER.info("Attribute registry rebuilt: {} valid attribute(s), {} lookup key(s), {} message(s).",
                byId.size(), byKey.size(), validationMessages.size());
        for (String message : validationMessages) {
            EbbMod.LOGGER.warn("attribute validation: {}", message);
        }
    }

    public static Optional<AttributeDefinition> byKey(String key) {
        if (key == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(byKey.get(normalize(key)));
    }

    public static int defaultScore(String key) {
        return byKey(key).map(AttributeDefinition::defaultScore).orElse(0);
    }

    public static int clamp(String key, int value) {
        return byKey(key).map(definition -> definition.clamp(value)).orElse(value);
    }

    public static int size() {
        return byId.size();
    }

    public static List<String> validationMessages() {
        return validationMessages;
    }

    public static String summaryLine() {
        return "attributes(valid=" + byId.size() + ", validation_messages=" + validationMessages.size() + ")";
    }

    private static void putKey(Map<String, AttributeDefinition> map, String key, AttributeDefinition definition, List<String> messages) {
        String normalized = normalize(key);
        AttributeDefinition previous = map.put(normalized, definition);
        if (previous != null && !previous.id().equals(definition.id())) {
            messages.add("attribute " + definition.id() + ": lookup key \"" + key + "\" also used by " + previous.id());
        }
    }

    private static String normalize(String key) {
        return key.trim().toLowerCase(Locale.ROOT);
    }
}
