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
                "memory_ops", List.of(),
                "proposed_effects", List.of(),
                "citations", List.of("fake:profile:" + request.npcKey()),
                "warnings", List.of(),
                "memory_writes", memoryWrites(request)
        ));
        return new GatewayChatResponse(request.conversationId(), reply, "guarded",
                List.of("继续追问", "换个角度", "结束自由交谈"),
                memoryWrites(request),
                List.of("fake:profile:" + request.npcKey(), "fake:conversation:" + request.conversationId()),
                List.of(), List.of(), chunks, structured, providerName(), request.modelOrDefault(model), false, !chunks.isEmpty(), "fake_gateway_reply", "");
    }

    private static List<String> memoryWrites(GatewayChatRequest request) {
        String lower = request.message() == null ? "" : request.message().toLowerCase(Locale.ROOT);
        if (lower.contains("ledger") || lower.contains("账本") || lower.contains("帳本")) {
            return List.of("fact:player.questioned_ledger=true", "summary:Player previously questioned the ledger. 玩家之前质问过账本。");
        }
        return List.of();
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

    private static String kbSignal(String sceneContext) {
        if (sceneContext == null || sceneContext.isBlank()) {
            return "none";
        }
        String lower = sceneContext.toLowerCase(Locale.ROOT);
        if (lower.contains("tenant paid cash") || lower.contains("secret:ledger")) {
            return "secret_visible";
        }
        return "public_only";
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
