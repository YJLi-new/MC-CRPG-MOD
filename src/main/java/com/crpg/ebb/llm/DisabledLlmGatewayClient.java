package com.crpg.ebb.llm;

import java.util.concurrent.CompletableFuture;

public final class DisabledLlmGatewayClient implements LlmGatewayClient {
    @Override
    public CompletableFuture<LlmChatResponse> sendMessage(LlmChatRequest request) {
        return CompletableFuture.completedFuture(LlmChatResponse.error("llm_disabled"));
    }

    @Override
    public String providerName() {
        return "disabled";
    }
}
