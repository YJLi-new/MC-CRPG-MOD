package com.crpg.ebb.llm;

import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

import java.util.Optional;

public record LlmChoiceSettings(
        Optional<String> npc,
        Optional<String> topicHint,
        Optional<String> returnNode,
        boolean allowMemoryWrite
) {
    public LlmChoiceSettings {
        npc = npc == null ? Optional.empty() : npc.filter(value -> !value.isBlank()).map(String::strip);
        topicHint = topicHint == null ? Optional.empty() : topicHint.filter(value -> !value.isBlank()).map(String::strip);
        returnNode = returnNode == null ? Optional.empty() : returnNode.filter(value -> !value.isBlank()).map(String::strip);
    }

    public static LlmChoiceSettings empty() {
        return new LlmChoiceSettings(Optional.empty(), Optional.empty(), Optional.empty(), true);
    }

    public static LlmChoiceSettings parse(JsonObject json) {
        if (json == null || !json.has("llm") || !json.get("llm").isJsonObject()) {
            return empty();
        }
        JsonObject llm = json.getAsJsonObject("llm");
        return new LlmChoiceSettings(
                optionalString(llm, "npc"),
                optionalString(llm, "topic_hint").or(() -> optionalString(llm, "topic")),
                optionalString(llm, "return_node"),
                GsonHelper.getAsBoolean(llm, "allow_memory_write", true)
        );
    }

    public String debugSummary() {
        StringBuilder builder = new StringBuilder("llm");
        npc.ifPresent(value -> builder.append(" npc=").append(value));
        topicHint.ifPresent(value -> builder.append(" topic=").append(value));
        returnNode.ifPresent(value -> builder.append(" return=").append(value));
        builder.append(" memory_write=").append(allowMemoryWrite);
        return builder.toString();
    }

    private static Optional<String> optionalString(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull()
                ? Optional.of(GsonHelper.getAsString(json, key))
                : Optional.empty();
    }
}
