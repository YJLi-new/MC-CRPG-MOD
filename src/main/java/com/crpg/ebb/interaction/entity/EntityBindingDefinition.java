package com.crpg.ebb.interaction.entity;

import com.crpg.ebb.EbbMod;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record EntityBindingDefinition(
        Identifier id,
        Optional<UUID> uuid,
        List<String> tags,
        Optional<String> name,
        List<Identifier> entityTypes,
        Identifier dialogueId,
        double interactionRange,
        double highlightRange,
        int priority
) {
    public static final double DEFAULT_INTERACTION_RANGE = 2.0D;
    public static final double DEFAULT_HIGHLIGHT_RANGE = 10.0D;

    public EntityBindingDefinition {
        uuid = uuid == null ? Optional.empty() : uuid;
        tags = tags == null ? List.of() : List.copyOf(tags);
        name = name == null ? Optional.empty() : name;
        entityTypes = entityTypes == null ? List.of() : List.copyOf(entityTypes);
    }

    public static Optional<EntityBindingDefinition> parse(Identifier id, JsonObject json, List<String> messages) {
        try {
            JsonObject match = json.has("match") && json.get("match").isJsonObject()
                    ? json.getAsJsonObject("match")
                    : new JsonObject();
            Optional<UUID> uuid = optionalString(match, "uuid").map(UUID::fromString);
            List<String> tags = parseStringList(match, "tags");
            optionalString(match, "tag").ifPresent(tags::add);
            Optional<String> name = optionalString(match, "name");
            List<Identifier> entityTypes = new ArrayList<>(parseIdentifierList(match, "entity_types", "minecraft"));
            optionalString(match, "entity_type").map(value -> parseIdentifier(value, "minecraft")).ifPresent(entityTypes::add);
            for (Identifier entityType : entityTypes) {
                if (!BuiltInRegistries.ENTITY_TYPE.containsKey(entityType) && !EbbMod.id("npc").equals(entityType)) {
                    messages.add("entity binding " + id + ": unknown entity_type " + entityType);
                }
            }
            if (uuid.isEmpty() && tags.isEmpty() && name.isEmpty() && entityTypes.isEmpty()) {
                messages.add("entity binding " + id + ": match is empty; binding would match every entity");
                return Optional.empty();
            }
            Identifier dialogue = parseIdentifier(GsonHelper.getAsString(json, "dialogue"));
            double interactionRange = optionalDouble(json, "interaction_range").orElse(DEFAULT_INTERACTION_RANGE);
            double highlightRange = optionalDouble(json, "highlight_range").orElse(DEFAULT_HIGHLIGHT_RANGE);
            int priority = optionalInt(json, "priority").orElse(0);
            if (interactionRange <= 0.0D) {
                messages.add("entity binding " + id + ": interaction_range must be > 0");
                interactionRange = DEFAULT_INTERACTION_RANGE;
            }
            if (highlightRange < interactionRange) {
                messages.add("entity binding " + id + ": highlight_range is smaller than interaction_range; clamped up");
                highlightRange = interactionRange;
            }
            return Optional.of(new EntityBindingDefinition(id, uuid, tags, name, entityTypes, dialogue, interactionRange, highlightRange, priority));
        } catch (RuntimeException ex) {
            messages.add("entity binding " + id + ": " + ex.getMessage());
            return Optional.empty();
        }
    }

    public boolean matches(Entity entity) {
        if (uuid.isPresent() && !uuid.get().equals(entity.getUUID())) {
            return false;
        }
        if (!tags.isEmpty() && tags.stream().noneMatch(entity.entityTags()::contains)) {
            return false;
        }
        if (name.isPresent()) {
            String customName = entity.hasCustomName() && entity.getCustomName() != null
                    ? entity.getCustomName().getString()
                    : entity.getName().getString();
            if (!name.get().equals(customName)) {
                return false;
            }
        }
        if (!entityTypes.isEmpty()) {
            Identifier actual = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            if (!entityTypes.contains(actual)) {
                return false;
            }
        }
        return true;
    }

    public int specificityScore() {
        int score = priority;
        if (uuid.isPresent()) {
            score += 4000;
        }
        if (!tags.isEmpty()) {
            score += 3000 + tags.size();
        }
        if (name.isPresent()) {
            score += 2000;
        }
        if (!entityTypes.isEmpty()) {
            score += 1000 + entityTypes.size();
        }
        return score;
    }

    public String debugSummary() {
        return "binding " + id + " -> " + dialogueId
                + " match(uuid=" + uuid.map(UUID::toString).orElse("-")
                + ", tags=" + tags
                + ", name=" + name.orElse("-")
                + ", entity_types=" + entityTypes
                + ") range=" + interactionRange
                + "/" + highlightRange
                + " priority=" + priority;
    }

    private static List<String> parseStringList(JsonObject json, String key) {
        List<String> values = new ArrayList<>();
        if (!json.has(key) || json.get(key).isJsonNull()) {
            return values;
        }
        JsonElement element = json.get(key);
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement item : array) {
                values.add(item.getAsString());
            }
        } else {
            values.add(element.getAsString());
        }
        return values;
    }

    private static List<Identifier> parseIdentifierList(JsonObject json, String key, String defaultNamespace) {
        return new ArrayList<>(parseStringList(json, key).stream().map(value -> parseIdentifier(value, defaultNamespace)).toList());
    }

    private static Identifier parseIdentifier(String value) {
        return parseIdentifier(value, "ebb");
    }

    private static Identifier parseIdentifier(String value, String defaultNamespace) {
        return value.contains(":") ? Identifier.parse(value) : Identifier.fromNamespaceAndPath(defaultNamespace, value);
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
}
