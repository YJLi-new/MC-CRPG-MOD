package com.crpg.ebb.feat;

import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public record FeatDefinition(
        Identifier id,
        String displayName,
        String description,
        boolean permanentPassive,
        boolean activeSlotCandidate,
        Map<String, Integer> checkModifiers
) {
    public FeatDefinition {
        displayName = displayName == null || displayName.isBlank() ? id.toString() : displayName;
        description = description == null ? "" : description;
        checkModifiers = checkModifiers == null ? Map.of() : Map.copyOf(checkModifiers);
    }

    public static Optional<FeatDefinition> parse(Identifier fileId, JsonObject json, java.util.List<String> messages) {
        Identifier id = optionalString(json, "id").map(value -> parseIdentifier(value, fileId, messages)).orElse(fileId);
        String displayName = optionalString(json, "display_name").or(() -> optionalString(json, "name")).orElse(fileId.toString());
        String description = optionalString(json, "description").orElse("");
        boolean permanent = optionalBoolean(json, "permanent_passive").or(() -> optionalBoolean(json, "permanentPassive")).orElse(true);
        boolean slotCandidate = optionalBoolean(json, "active_slot_candidate").or(() -> optionalBoolean(json, "activeSlotCandidate")).orElse(true);
        Map<String, Integer> modifiers = parseModifiers(fileId, json, messages);
        return Optional.of(new FeatDefinition(id, displayName, description, permanent, slotCandidate, modifiers));
    }

    public int modifierFor(String canonicalAttribute) {
        String normalized = canonicalAttribute.trim().toLowerCase(Locale.ROOT);
        return checkModifiers.getOrDefault(normalized, 0);
    }

    public String debugSummary() {
        return id + " name=\"" + displayName + "\" permanent=" + permanentPassive
                + " active_slot_candidate=" + activeSlotCandidate + " check_modifiers=" + checkModifiers;
    }

    private static Map<String, Integer> parseModifiers(Identifier fileId, JsonObject json, java.util.List<String> messages) {
        Map<String, Integer> values = new LinkedHashMap<>();
        if (!json.has("check_modifiers")) {
            return values;
        }
        if (!json.get("check_modifiers").isJsonObject()) {
            messages.add("feat " + fileId + ": check_modifiers must be an object");
            return values;
        }
        JsonObject modifiers = json.getAsJsonObject("check_modifiers");
        for (Map.Entry<String, com.google.gson.JsonElement> entry : modifiers.entrySet()) {
            if (!entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isNumber()) {
                messages.add("feat " + fileId + ": check_modifiers." + entry.getKey() + " must be an integer");
                continue;
            }
            values.put(entry.getKey().trim().toLowerCase(Locale.ROOT), entry.getValue().getAsInt());
        }
        return values;
    }

    private static Identifier parseIdentifier(String raw, Identifier fileId, java.util.List<String> messages) {
        try {
            return raw.contains(":") ? Identifier.parse(raw) : Identifier.fromNamespaceAndPath(fileId.getNamespace(), raw);
        } catch (RuntimeException ex) {
            messages.add("feat " + fileId + ": invalid id " + raw + " (" + ex.getMessage() + ")");
            return fileId;
        }
    }

    private static Optional<String> optionalString(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull()
                ? Optional.of(GsonHelper.getAsString(json, key))
                : Optional.empty();
    }

    private static Optional<Boolean> optionalBoolean(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull()
                ? Optional.of(GsonHelper.getAsBoolean(json, key))
                : Optional.empty();
    }
}
