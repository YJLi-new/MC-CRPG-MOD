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
        String kbSignal = kbSignal(request.knowledgeContext());
        String reply = String.format(Locale.ROOT, "%s NPC=%s topic=%s kb=%s player=\"%s\"",
                config.fakeReply(), request.npcDisplayName(), blank(request.topicHint(), "general"), kbSignal, message);
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

    private static String kbSignal(String knowledgeContext) {
        if (knowledgeContext == null || knowledgeContext.isBlank()) {
            return "none";
        }
        String lower = knowledgeContext.toLowerCase(Locale.ROOT);
        if (lower.contains("tenant paid cash") || lower.contains("secret:ledger")) {
            return "secret_visible";
        }
        return "public_only";
    }
}
