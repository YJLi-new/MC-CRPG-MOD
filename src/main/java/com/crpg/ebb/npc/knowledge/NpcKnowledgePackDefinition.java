package com.crpg.ebb.npc.knowledge;

import com.crpg.ebb.dialogue.DialogueCondition;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record NpcKnowledgePackDefinition(Identifier id, List<Chunk> chunks) {
    public NpcKnowledgePackDefinition {
        if (id == null) throw new IllegalArgumentException("id cannot be null");
        chunks = chunks == null ? List.of() : List.copyOf(chunks);
    }

    public static Optional<NpcKnowledgePackDefinition> parse(Identifier dataId, JsonObject json, List<String> messages) {
        try {
            Optional<String> declaredId = optionalString(json, "id");
            if (declaredId.isPresent() && !parseIdentifier(declaredId.get()).equals(dataId)) {
                messages.add("npc knowledge pack " + dataId + ": declared id " + declaredId.get() + " does not match file id; using file id");
            }
            List<Chunk> chunks = new ArrayList<>();
            if (!json.has("chunks") || !json.get("chunks").isJsonArray()) {
                messages.add("npc knowledge pack " + dataId + ": chunks must be a non-empty array");
                return Optional.empty();
            }
            JsonArray array = json.getAsJsonArray("chunks");
            for (int i = 0; i < array.size(); i++) {
                JsonElement element = array.get(i);
                if (!element.isJsonObject()) {
                    messages.add("npc knowledge pack " + dataId + ".chunks[" + i + "]: chunk must be an object");
                    continue;
                }
                parseChunk(dataId, element.getAsJsonObject(), i, messages).ifPresent(chunks::add);
            }
            if (chunks.isEmpty()) {
                messages.add("npc knowledge pack " + dataId + ": no valid chunks");
                return Optional.empty();
            }
            return Optional.of(new NpcKnowledgePackDefinition(dataId, chunks));
        } catch (RuntimeException ex) {
            messages.add("npc knowledge pack " + dataId + ": " + ex.getMessage());
            return Optional.empty();
        }
    }

    private static Optional<Chunk> parseChunk(Identifier packId, JsonObject json, int index, List<String> messages) {
        String id = optionalString(json, "id").orElse("chunk_" + index).strip();
        String text = optionalString(json, "text").or(() -> optionalString(json, "content")).orElse("").strip();
        if (text.isBlank()) {
            messages.add("npc knowledge pack " + packId + ".chunks[" + index + "]: text/content is required");
            return Optional.empty();
        }
        boolean secret = optionalBoolean(json, "secret").orElse(false);
        List<String> tags = parseStringList(json, "tags");
        List<DialogueCondition> revealConditions = new ArrayList<>();
        if (json.has("reveal_conditions")) {
            if (!json.get("reveal_conditions").isJsonArray()) {
                messages.add("npc knowledge pack " + packId + ".chunks[" + index + "]: reveal_conditions must be an array");
            } else {
                JsonArray conditions = json.getAsJsonArray("reveal_conditions");
                for (int i = 0; i < conditions.size(); i++) {
                    JsonElement condition = conditions.get(i);
                    if (!condition.isJsonObject()) {
                        messages.add("npc knowledge pack " + packId + ".chunks[" + index + "].reveal_conditions[" + i + "]: condition must be an object");
                        continue;
                    }
                    DialogueCondition.parse(condition.getAsJsonObject(), "npc knowledge pack " + packId + ".chunks[" + index + "].reveal_conditions[" + i + "]", messages)
                            .ifPresent(revealConditions::add);
                }
            }
        }
        return Optional.of(new Chunk(id, text, secret, tags, revealConditions));
    }

    public String debugSummary() {
        long secretCount = chunks.stream().filter(Chunk::secret).count();
        return "kb_pack " + id + " chunks=" + chunks.size() + " secret=" + secretCount;
    }

    private static List<String> parseStringList(JsonObject json, String key) {
        List<String> values = new ArrayList<>();
        if (!json.has(key) || json.get(key).isJsonNull()) return values;
        JsonElement element = json.get(key);
        if (element.isJsonArray()) {
            for (JsonElement item : element.getAsJsonArray()) {
                if (item.isJsonPrimitive()) values.add(item.getAsString());
            }
        } else if (element.isJsonPrimitive()) {
            values.add(element.getAsString());
        }
        return List.copyOf(values);
    }

    private static Optional<String> optionalString(JsonObject json, String key) {
        return json != null && json.has(key) && !json.get(key).isJsonNull()
                ? Optional.of(GsonHelper.getAsString(json, key))
                : Optional.empty();
    }

    private static Optional<Boolean> optionalBoolean(JsonObject json, String key) {
        return json != null && json.has(key) && !json.get(key).isJsonNull()
                ? Optional.of(GsonHelper.getAsBoolean(json, key))
                : Optional.empty();
    }

    private static Identifier parseIdentifier(String value) {
        return value.contains(":") ? Identifier.parse(value) : Identifier.fromNamespaceAndPath("ebb", value);
    }

    public record Chunk(String id, String text, boolean secret, List<String> tags, List<DialogueCondition> revealConditions) {
        public Chunk {
            id = id == null || id.isBlank() ? "chunk" : id.strip();
            text = text == null ? "" : text.strip();
            tags = tags == null ? List.of() : List.copyOf(tags);
            revealConditions = revealConditions == null ? List.of() : List.copyOf(revealConditions);
        }

        public boolean visible(com.crpg.ebb.state.NarrativeSavedData state, java.util.UUID playerUuid, long dayTime) {
            return revealConditions.isEmpty() || revealConditions.stream().allMatch(condition -> condition.matches(state, playerUuid, dayTime));
        }
    }
}
