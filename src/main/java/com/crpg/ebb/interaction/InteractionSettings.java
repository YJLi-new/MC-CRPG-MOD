package com.crpg.ebb.interaction;

import com.crpg.ebb.EbbMod;
import com.crpg.ebb.interaction.entity.EntityBindingDefinition;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InteractionSettings {
    public static final boolean DEFAULT_ENABLE_DEBUG_ENTITY_FALLBACK = false;
    public static final Identifier DEFAULT_DEBUG_ENTITY_FALLBACK_DIALOGUE = EbbMod.id("debug/entity");
    public static final double DEFAULT_DEBUG_ENTITY_FALLBACK_INTERACTION_RANGE = 2.0D;
    public static final double DEFAULT_DEBUG_ENTITY_FALLBACK_HIGHLIGHT_RANGE = 10.0D;
    public static final int DEFAULT_MAX_BLOCKS_PER_GROUP = InteractionSyncLimits.MAX_BLOCKS_PER_GROUP;

    private static volatile Snapshot snapshot = defaults();
    private static volatile List<String> validationMessages = List.of();

    private InteractionSettings() {
    }

    public static void rebuild(Map<Identifier, JsonObject> rawEntries) {
        Snapshot current = defaults();
        List<String> messages = new ArrayList<>();
        for (Map.Entry<Identifier, JsonObject> entry : rawEntries.entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getKey().toString()))
                .toList()) {
            current = parse(entry.getKey(), entry.getValue(), current, messages);
        }
        apply(current, messages, "rebuilt");
    }

    public static void applySynced(Snapshot synced) {
        apply(synced == null ? defaults() : synced, List.of(), "synced");
    }

    public static void resetToDefaults() {
        apply(defaults(), List.of(), "reset");
    }

    public static Snapshot snapshot() {
        return snapshot;
    }

    public static boolean enableDebugEntityFallback() {
        return snapshot.enableDebugEntityFallback();
    }

    public static int maxBlocksPerGroup() {
        return snapshot.maxBlocksPerGroup();
    }

    public static EntityBindingDefinition debugFallbackDefinition() {
        Snapshot settings = snapshot;
        return new EntityBindingDefinition(
                EbbMod.id("fallback/debug_entity"),
                Optional.empty(),
                List.of(),
                Optional.empty(),
                List.of(),
                settings.debugEntityFallbackDialogue(),
                settings.debugEntityFallbackInteractionRange(),
                settings.debugEntityFallbackHighlightRange(),
                Integer.MIN_VALUE,
                HighlightStyle.entityDefault()
        );
    }

    public static List<String> validationMessages() {
        return validationMessages;
    }

    public static String summaryLine() {
        Snapshot settings = snapshot;
        return "interaction_settings(debug_entity_fallback=" + settings.enableDebugEntityFallback()
                + ", fallback_dialogue=" + settings.debugEntityFallbackDialogue()
                + ", max_blocks_per_group=" + settings.maxBlocksPerGroup()
                + ", validation_messages=" + validationMessages.size() + ")";
    }

    private static Snapshot parse(Identifier id, JsonObject json, Snapshot base, List<String> messages) {
        boolean enableFallback = optionalBoolean(json, "enable_debug_entity_fallback").orElse(base.enableDebugEntityFallback());
        Identifier fallbackDialogue = optionalString(json, "debug_entity_fallback_dialogue")
                .or(() -> optionalString(json, "fallback_dialogue"))
                .map(value -> parseIdentifier(id, value, "debug_entity_fallback_dialogue", messages))
                .orElse(base.debugEntityFallbackDialogue());
        double fallbackInteractionRange = optionalDouble(json, "debug_entity_fallback_interaction_range")
                .or(() -> optionalDouble(json, "fallback_interaction_range"))
                .orElse(base.debugEntityFallbackInteractionRange());
        double fallbackHighlightRange = optionalDouble(json, "debug_entity_fallback_highlight_range")
                .or(() -> optionalDouble(json, "fallback_highlight_range"))
                .orElse(base.debugEntityFallbackHighlightRange());
        int maxBlocksPerGroup = optionalInt(json, "max_blocks_per_group").orElse(base.maxBlocksPerGroup());

        if (fallbackInteractionRange <= 0.0D) {
            messages.add("interaction settings " + id + ": debug_entity_fallback_interaction_range must be > 0; using default");
            fallbackInteractionRange = DEFAULT_DEBUG_ENTITY_FALLBACK_INTERACTION_RANGE;
        }
        if (fallbackHighlightRange < fallbackInteractionRange) {
            messages.add("interaction settings " + id + ": debug_entity_fallback_highlight_range is smaller than interaction range; clamped up");
            fallbackHighlightRange = fallbackInteractionRange;
        }
        if (maxBlocksPerGroup <= 0) {
            messages.add("interaction settings " + id + ": max_blocks_per_group must be > 0; using default");
            maxBlocksPerGroup = DEFAULT_MAX_BLOCKS_PER_GROUP;
        } else if (maxBlocksPerGroup > InteractionSyncLimits.MAX_BLOCKS_PER_GROUP) {
            messages.add("interaction settings " + id + ": max_blocks_per_group " + maxBlocksPerGroup
                    + " exceeds sync hard limit " + InteractionSyncLimits.MAX_BLOCKS_PER_GROUP + "; clamped");
            maxBlocksPerGroup = InteractionSyncLimits.MAX_BLOCKS_PER_GROUP;
        }

        return new Snapshot(enableFallback, fallbackDialogue, fallbackInteractionRange, fallbackHighlightRange, maxBlocksPerGroup);
    }

    private static Identifier parseIdentifier(Identifier settingsId, String value, String key, List<String> messages) {
        try {
            return Identifier.parse(value);
        } catch (RuntimeException ex) {
            messages.add("interaction settings " + settingsId + ": invalid " + key + " '" + value + "'; using default");
            return DEFAULT_DEBUG_ENTITY_FALLBACK_DIALOGUE;
        }
    }

    private static void apply(Snapshot newSnapshot, List<String> messages, String source) {
        snapshot = newSnapshot;
        validationMessages = List.copyOf(messages);
        EbbMod.LOGGER.info("Interaction settings {}: debug_entity_fallback={}, fallback_dialogue={}, max_blocks_per_group={}, {} message(s).",
                source,
                newSnapshot.enableDebugEntityFallback(),
                newSnapshot.debugEntityFallbackDialogue(),
                newSnapshot.maxBlocksPerGroup(),
                validationMessages.size());
        validationMessages.forEach(message -> EbbMod.LOGGER.warn("interaction settings: {}", message));
    }

    private static Snapshot defaults() {
        return new Snapshot(
                DEFAULT_ENABLE_DEBUG_ENTITY_FALLBACK,
                DEFAULT_DEBUG_ENTITY_FALLBACK_DIALOGUE,
                DEFAULT_DEBUG_ENTITY_FALLBACK_INTERACTION_RANGE,
                DEFAULT_DEBUG_ENTITY_FALLBACK_HIGHLIGHT_RANGE,
                DEFAULT_MAX_BLOCKS_PER_GROUP
        );
    }

    private static Optional<Boolean> optionalBoolean(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull()
                ? Optional.of(GsonHelper.getAsBoolean(json, key))
                : Optional.empty();
    }

    private static Optional<String> optionalString(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull()
                ? Optional.of(GsonHelper.getAsString(json, key))
                : Optional.empty();
    }

    private static Optional<Double> optionalDouble(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull()
                ? Optional.of(GsonHelper.getAsDouble(json, key))
                : Optional.empty();
    }

    private static Optional<Integer> optionalInt(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull()
                ? Optional.of(GsonHelper.getAsInt(json, key))
                : Optional.empty();
    }

    public record Snapshot(
            boolean enableDebugEntityFallback,
            Identifier debugEntityFallbackDialogue,
            double debugEntityFallbackInteractionRange,
            double debugEntityFallbackHighlightRange,
            int maxBlocksPerGroup
    ) {
    }
}
