package com.crpg.ebb.llm;

import java.util.concurrent.CompletableFuture;

public interface LlmGatewayClient {
    CompletableFuture<LlmChatResponse> sendMessage(LlmChatRequest request);

    String providerName();

    default boolean usesNetwork() {
        return false;
    }
}
