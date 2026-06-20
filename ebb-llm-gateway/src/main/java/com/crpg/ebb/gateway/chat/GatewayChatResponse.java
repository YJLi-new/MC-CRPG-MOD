package com.crpg.ebb.gateway.chat;

import com.crpg.ebb.gateway.HttpJson;

import java.util.ArrayList;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        List<String> rawProposedEffects = proposedEffects == null ? List.of() : List.copyOf(proposedEffects);
        proposedEffects = sanitizeProposedEffects(rawProposedEffects);
        List<String> safeWarnings = new ArrayList<>(warnings == null ? List.of() : warnings);
        if (proposedEffects.size() != rawProposedEffects.size()) {
            safeWarnings.add("high_risk_effects_rejected_from_llm_output");
        }
        warnings = List.copyOf(safeWarnings);
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

    public GatewayChatResponse withValidatedMemoryCitations(List<String> allowedMemoryCitations) {
        if (citations.isEmpty()) {
            return this;
        }
        Set<String> allowed = Set.copyOf(allowedMemoryCitations == null ? List.of() : allowedMemoryCitations);
        List<String> kept = new ArrayList<>();
        List<String> safeWarnings = new ArrayList<>(warnings);
        boolean rejected = false;
        for (String citation : citations) {
            String value = citation == null ? "" : citation.strip();
            if (value.isBlank()) {
                continue;
            }
            if (value.startsWith("memory:") && !allowed.contains(value)) {
                rejected = true;
                continue;
            }
            kept.add(value);
        }
        if (rejected) {
            safeWarnings.add("invalid_memory_citation_rejected");
        }
        return new GatewayChatResponse(conversationId, npcReply, mood, suggestedOptions, memoryWrites, kept,
                proposedEffects, safeWarnings, chunks, structuredJson, provider, model, store, chunkedResponse, status, error);
    }

    /**
     * LLM output is advisory only.  The Minecraft server never applies direct
     * dialogue/gameplay effects from this field, and the gateway filters the
     * field to low-risk review hints so future providers cannot smuggle
     * high-authority effects such as quest/flag/item/routine mutation through
     * `proposed_effects`.
     */
    public static List<String> sanitizeProposedEffects(List<String> rawEffects) {
        if (rawEffects == null || rawEffects.isEmpty()) {
            return List.of();
        }
        List<String> safe = new ArrayList<>();
        for (String effect : rawEffects) {
            String value = effect == null ? "" : effect.strip();
            if (value.isBlank()) {
                continue;
            }
            if (isLowRiskProposedEffect(value)) {
                safe.add(value);
            }
        }
        return List.copyOf(safe);
    }

    public static boolean isLowRiskProposedEffect(String effect) {
        String lower = effect == null ? "" : effect.strip().toLowerCase(Locale.ROOT);
        if (lower.isBlank()) {
            return false;
        }
        if (lower.startsWith("suggest_option:")
                || lower.startsWith("memory_write:")
                || lower.startsWith("memory_note:")
                || lower.startsWith("stance_hint:")
                || lower.startsWith("mood:")
                || lower.startsWith("warning:")) {
            return true;
        }
        return !containsHighRiskEffectVerb(lower);
    }

    public static boolean containsHighRiskEffectVerb(String effect) {
        String lower = effect == null ? "" : effect.toLowerCase(Locale.ROOT);
        for (String token : List.of(
                "set_flag",
                "clear_flag",
                "set_story_var",
                "add_story_int",
                "start_quest",
                "complete_quest",
                "complete_quest_branch",
                "take_root",
                "unlock_feat",
                "activate_feat",
                "give_item",
                "remove_item",
                "set_npc_routine",
                "routine",
                "teleport",
                "summon",
                "command:",
                "op:",
                "grant",
                "reveal_clue",
                "add_journal_entry",
                "start_conflict",
                "apply_conflict_outcome",
                "add_relation",
                "set_relation",
                "npc_kb_add_pack",
                "npc_kb_add_fact")) {
            if (lower.contains(token)) {
                return true;
            }
        }
        return false;
    }
}
