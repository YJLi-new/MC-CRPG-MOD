package com.crpg.ebb.gateway.chat;

import com.crpg.ebb.gateway.HttpJson;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record GatewayChatResponse(
        String conversationId,
        String npcReply,
        String mood,
        List<String> suggestedOptions,
        List<String> memoryWrites,
        List<String> citations,
        List<String> proposedEffects,
        List<String> warnings,
        List<String> chunks,
        String structuredJson,
        String provider,
        String model,
        boolean store,
        boolean chunkedResponse,
        String status,
        String error
) {
    public GatewayChatResponse {
        conversationId = conversationId == null ? "" : conversationId;
        npcReply = npcReply == null ? "" : npcReply;
        mood = mood == null || mood.isBlank() ? "neutral" : mood;
        suggestedOptions = suggestedOptions == null ? List.of() : List.copyOf(suggestedOptions);
        memoryWrites = memoryWrites == null ? List.of() : List.copyOf(memoryWrites);
        citations = citations == null ? List.of() : List.copyOf(citations);
        proposedEffects = proposedEffects == null ? List.of() : List.copyOf(proposedEffects);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        chunks = chunks == null ? List.of() : List.copyOf(chunks);
        structuredJson = structuredJson == null ? "" : structuredJson;
        provider = provider == null ? "" : provider;
        model = model == null ? "" : model;
        status = status == null ? "" : status;
        error = error == null ? "" : error;
    }

    public static GatewayChatResponse ok(GatewayChatRequest request, String reply, List<String> options,
                                         List<String> citations, List<String> chunks, String structuredJson,
                                         String provider, String model, boolean store, String status) {
        return new GatewayChatResponse(request.conversationId(), reply, "guarded", options, List.of(), citations,
                List.of(), List.of(), chunks, structuredJson, provider, model, store, !chunks.isEmpty(), status, "");
    }

    public static GatewayChatResponse error(GatewayChatRequest request, String provider, String model, String reason) {
        return new GatewayChatResponse(request == null ? "" : request.conversationId(), "", "neutral", List.of(), List.of(),
                List.of(), List.of(), List.of(reason == null ? "llm_gateway_error" : reason), List.of(), "", provider, model,
                false, false, reason == null ? "llm_gateway_error" : reason, reason == null ? "llm_gateway_error" : reason);
    }

    public String toJson() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("conversation_id", conversationId);
        if (!error.isBlank()) {
            values.put("error", error);
        }
        values.put("npc_reply", npcReply);
        values.put("mood", mood);
        values.put("suggested_options", suggestedOptions);
        values.put("memory_writes", memoryWrites);
        values.put("citations", citations);
        values.put("proposed_effects", proposedEffects);
        values.put("warnings", warnings);
        values.put("chunks", chunks);
        values.put("structured_json", structuredJson);
        values.put("provider", provider);
        values.put("model", model);
        values.put("store", store);
        values.put("chunked_response", chunkedResponse);
        values.put("status", status);
        return HttpJson.object(values);
    }
}
