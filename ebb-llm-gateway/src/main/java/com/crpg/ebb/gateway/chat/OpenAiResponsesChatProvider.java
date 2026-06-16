package com.crpg.ebb.gateway.chat;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.http.StreamResponse;
import com.openai.helpers.ResponseAccumulator;
import com.openai.models.ChatModel;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseStreamEvent;

import java.util.ArrayList;
import java.util.List;

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
        ResponseCreateParams params = ResponseCreateParams.builder()
                .input(request.prompt())
                .model(ChatModel.of(model))
                .store(request.store() && allowStore)
                .build();

        ResponseAccumulator accumulator = ResponseAccumulator.create();
        List<String> chunks = new ArrayList<>();
        try (StreamResponse<ResponseStreamEvent> streamResponse = client.responses().createStreaming(params)) {
            streamResponse.stream()
                    .peek(accumulator::accumulate)
                    .flatMap(event -> event.outputTextDelta().stream())
                    .map(delta -> delta.delta())
                    .forEach(chunks::add);
        }
        String reply = String.join("", chunks).strip();
        if (reply.isBlank()) {
            reply = "(The NPC says nothing.)";
        }
        String structured = request.structured() && reply.startsWith("{") ? reply : "";
        return GatewayChatResponse.ok(request, reply,
                List.of("继续追问", "换个角度", "结束自由交谈"),
                List.of("openai:responses:" + request.conversationId()),
                chunks,
                structured,
                providerName(),
                model,
                request.store() && allowStore,
                "openai_responses_streamed_store_" + (request.store() && allowStore));
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
