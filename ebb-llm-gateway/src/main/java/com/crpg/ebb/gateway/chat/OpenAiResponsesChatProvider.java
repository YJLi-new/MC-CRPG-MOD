package com.crpg.ebb.gateway.chat;

import com.crpg.ebb.gateway.HttpJson;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.http.StreamResponse;
import com.openai.core.JsonValue;
import com.openai.helpers.ResponseAccumulator;
import com.openai.models.ChatModel;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseFormatTextJsonSchemaConfig;
import com.openai.models.responses.ResponseTextConfig;
import com.openai.models.responses.ResponseStreamEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class OpenAiResponsesChatProvider implements GatewayChatProvider {
    private final OpenAIClient client;
    private final String defaultModel;
    private final boolean allowStore;

    public OpenAiResponsesChatProvider(String defaultModel, boolean allowStore) {
        this(OpenAIOkHttpClient.fromEnv(), defaultModel, allowStore);
    }

    OpenAiResponsesChatProvider(OpenAIClient client, String defaultModel, boolean allowStore) {
        this.client = client;
        this.defaultModel = defaultModel == null || defaultModel.isBlank() ? "gpt-5.2" : defaultModel;
        this.allowStore = allowStore;
    }

    @Override
    public GatewayChatResponse send(GatewayChatRequest request) throws Exception {
        String model = request.modelOrDefault(defaultModel);
        ResponseCreateParams.Builder builder = ResponseCreateParams.builder()
                .input(request.prompt())
                .model(ChatModel.of(model))
                .maxOutputTokens((long) request.maxOutputTokens())
                .store(request.store() && allowStore);
        if (request.structured()) {
            builder.text(ResponseTextConfig.builder()
                    .format(ResponseFormatTextJsonSchemaConfig.builder()
                            .name("ebb_npc_chat_response")
                            .description("Structured response for an in-world Ebb CRPG NPC chat turn.")
                            .strict(true)
                            .schema(ResponseFormatTextJsonSchemaConfig.Schema.builder()
                                    .putAdditionalProperty("type", JsonValue.from("object"))
                                    .putAdditionalProperty("additionalProperties", JsonValue.from(false))
                                    .putAdditionalProperty("required", JsonValue.from(List.of("npc_reply", "mood", "suggested_options", "citations", "warnings", "memory_writes")))
                                    .putAdditionalProperty("properties", JsonValue.from(Map.of(
                                            "npc_reply", Map.of("type", "string"),
                                            "mood", Map.of("type", "string"),
                                            "suggested_options", Map.of("type", "array", "items", Map.of("type", "string"), "maxItems", 4),
                                            "citations", Map.of("type", "array", "items", Map.of("type", "string"), "maxItems", 8),
                                            "warnings", Map.of("type", "array", "items", Map.of("type", "string"), "maxItems", 4),
                                            "memory_writes", Map.of("type", "array", "items", Map.of("type", "string"), "maxItems", 8)
                                    )))
                                    .build())
                            .build())
                    .build());
        }
        ResponseCreateParams params = builder.build();

        ResponseAccumulator accumulator = ResponseAccumulator.create();
        List<String> chunks = new ArrayList<>();
        try (StreamResponse<ResponseStreamEvent> streamResponse = client.responses().createStreaming(params)) {
            streamResponse.stream()
                    .peek(accumulator::accumulate)
                    .flatMap(event -> event.outputTextDelta().stream())
                    .map(delta -> delta.delta())
                    .forEach(chunks::add);
        }
        String rawReply = String.join("", chunks).strip();
        if (rawReply.isBlank()) {
            rawReply = "(The NPC says nothing.)";
        }
        String structured = request.structured() && rawReply.startsWith("{") ? rawReply : "";
        String reply = structured.isBlank() ? rawReply
                : HttpJson.objectStrings(structured).getOrDefault("npc_reply", rawReply);
        List<String> displayChunks = chunks.isEmpty() || !structured.isBlank() ? chunk(reply, 80) : List.copyOf(chunks);
        return new GatewayChatResponse(request.conversationId(), reply, "guarded",
                structured.isBlank() ? List.of("继续追问", "换个角度", "结束自由交谈") : nonEmpty(HttpJson.stringArrayValue(structured, "suggested_options"), List.of("继续追问", "换个角度", "结束自由交谈")),
                structured.isBlank() ? List.of() : HttpJson.stringArrayValue(structured, "memory_writes"),
                structured.isBlank() ? List.of("openai:responses:" + request.conversationId()) : nonEmpty(HttpJson.stringArrayValue(structured, "citations"), List.of("openai:responses:" + request.conversationId())),
                List.of(),
                structured.isBlank() ? List.of() : HttpJson.stringArrayValue(structured, "warnings"),
                displayChunks, structured, providerName(), model, request.store() && allowStore, !displayChunks.isEmpty(),
                "openai_responses_streamed_store_" + (request.store() && allowStore), "");
    }

    private static List<String> nonEmpty(List<String> value, List<String> fallback) {
        return value == null || value.isEmpty() ? fallback : value;
    }

    private static List<String> chunk(String value, int size) {
        if (value == null || value.isEmpty()) {
            return List.of();
        }
        ArrayList<String> chunks = new ArrayList<>();
        for (int i = 0; i < value.length(); i += size) {
            chunks.add(value.substring(i, Math.min(value.length(), i + size)));
        }
        return List.copyOf(chunks);
    }

    @Override
    public String providerName() {
        return "openai_responses";
    }

    @Override
    public boolean usesOpenAiNetwork() {
        return true;
    }
}
