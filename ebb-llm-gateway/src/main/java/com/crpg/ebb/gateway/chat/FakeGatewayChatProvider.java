package com.crpg.ebb.gateway.chat;

import com.crpg.ebb.gateway.HttpJson;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FakeGatewayChatProvider implements GatewayChatProvider {
    private final String model;

    public FakeGatewayChatProvider(String model) {
        this.model = model == null || model.isBlank() ? "fake-gateway-model" : model;
    }

    @Override
    public GatewayChatResponse send(GatewayChatRequest request) {
        String reply = String.format(Locale.ROOT, "FAKE_GATEWAY_REPLY NPC=%s topic=%s player=\"%s\"",
                request.npcDisplayName(), blank(request.topicHint(), "general"), abbreviate(request.message(), 90));
        List<String> chunks = chunk(reply, 36);
        String structured = HttpJson.object(Map.of(
                "npc_reply", reply,
                "mood", "guarded",
                "suggested_options", List.of("继续追问", "换个角度", "结束自由交谈"),
                "citations", List.of("fake:profile:" + request.npcKey()),
                "warnings", List.of()
        ));
        return GatewayChatResponse.ok(request, reply,
                List.of("继续追问", "换个角度", "结束自由交谈"),
                List.of("fake:profile:" + request.npcKey(), "fake:conversation:" + request.conversationId()),
                chunks,
                structured,
                providerName(),
                request.modelOrDefault(model),
                false,
                "fake_gateway_reply");
    }

    @Override
    public String providerName() {
        return "fake_gateway";
    }

    private static String abbreviate(String value, int max) {
        String safe = value == null ? "" : value.replace('\n', ' ').strip();
        return safe.length() <= max ? safe : safe.substring(0, Math.max(0, max - 1)) + "…";
    }

    private static String blank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static List<String> chunk(String value, int size) {
        if (value == null || value.isEmpty()) {
            return List.of();
        }
        java.util.ArrayList<String> chunks = new java.util.ArrayList<>();
        for (int i = 0; i < value.length(); i += size) {
            chunks.add(value.substring(i, Math.min(value.length(), i + size)));
        }
        return List.copyOf(chunks);
    }
}
