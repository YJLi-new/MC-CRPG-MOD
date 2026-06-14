package com.crpg.ebb.llm;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class FakeLlmGatewayClient implements LlmGatewayClient {
    public static final String FIXED_REPLY_MARKER = "FAKE_NPC_REPLY";
    private final LlmConfig config;

    public FakeLlmGatewayClient(LlmConfig config) {
        this.config = config == null ? LlmConfig.fakeForTesting() : config;
    }

    @Override
    public CompletableFuture<LlmChatResponse> sendMessage(LlmChatRequest request) {
        String message = abbreviate(request.playerMessage(), 90);
        String reply = String.format(Locale.ROOT, "%s NPC=%s topic=%s player=\"%s\"",
                config.fakeReply(), request.npcDisplayName(), blank(request.topicHint(), "general"), message);
        return CompletableFuture.completedFuture(LlmChatResponse.ok(
                reply,
                List.of("继续追问", "换个角度", "结束自由交谈"),
                List.of("fake:profile:" + request.npcKey(), "fake:recent:" + request.conversationId()),
                "fake_reply"
        ));
    }

    @Override
    public String providerName() {
        return "fake";
    }

    private static String abbreviate(String value, int max) {
        String safe = value == null ? "" : value.replace('\n', ' ').strip();
        if (safe.length() <= max) {
            return safe;
        }
        return safe.substring(0, Math.max(0, max - 1)) + "…";
    }

    private static String blank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
