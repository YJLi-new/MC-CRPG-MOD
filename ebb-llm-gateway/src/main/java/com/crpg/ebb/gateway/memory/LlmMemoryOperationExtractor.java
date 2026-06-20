package com.crpg.ebb.gateway.memory;

import com.crpg.ebb.gateway.chat.GatewayChatRequest;
import com.crpg.ebb.gateway.chat.GatewayChatResponse;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * P39 extractor: treats LLM-proposed memory_writes as proposals only, then adds a
 * deterministic fallback for tests/dev authoring. Nothing from this class is written
 * until {@link DeterministicMemoryValidator} accepts it.
 */
public final class LlmMemoryOperationExtractor {
    private final MemoryFactExtractor deterministicFactExtractor = new MemoryFactExtractor();

    public List<MemoryOperation> propose(GatewayChatRequest request, GatewayChatResponse response, MemoryRecord playerRecord) {
        long now = Instant.now().toEpochMilli();
        String recordId = playerRecord == null ? "" : playerRecord.id();
        List<MemoryOperation> proposals = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        if (response != null) {
            for (String write : response.memoryWrites()) {
                addParsedWrite(proposals, seen, request, recordId, write, "llm_memory_writes", now);
            }
            for (MemoryOperation operation : structuredMemoryOps(request, response.structuredJson(), recordId, now)) {
                add(proposals, seen, operation);
            }
            for (String write : structuredMemoryWrites(response.structuredJson())) {
                addParsedWrite(proposals, seen, request, recordId, write, "llm_structured_json", now);
            }
        }
        for (MemoryFactExtractor.ExtractedFact fact : deterministicFactExtractor.extract(request)) {
            add(proposals, seen, new MemoryOperation(id("memop"), now, request.serverId(), request.worldId(), recordId,
                    MemoryOperation.ADD_FACT, fact.subject(), fact.predicate(), fact.value(), "proposed",
                    "deterministic_fact_extractor", "deterministic_validator", 0.92D));
        }
        addLedgerQuestionProposals(proposals, seen, request, recordId, now);
        addCanonicalOwnershipProbe(proposals, seen, request, recordId, now);
        return List.copyOf(proposals);
    }

    private static void addParsedWrite(List<MemoryOperation> proposals, Set<String> seen, GatewayChatRequest request,
                                       String recordId, String write, String proposedBy, long now) {
        if (write == null || write.isBlank()) {
            return;
        }
        String trimmed = write.strip();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.startsWith("lesson:") || lower.startsWith("safety:")) {
            int colon = trimmed.indexOf(':');
            add(proposals, seen, new MemoryOperation(id("memop"), now, request.serverId(), request.worldId(), recordId,
                    MemoryOperation.ADD_SAFETY_LESSON, "llm_safety", "lesson", trimmed.substring(colon + 1).strip(), "proposed",
                    "llm_proposed_safety_lesson", proposedBy, 0.8D));
            return;
        }
        if (lower.startsWith("summary:")) {
            add(proposals, seen, new MemoryOperation(id("memop"), now, request.serverId(), request.worldId(), recordId,
                    MemoryOperation.ADD_SUMMARY, "episode", "summary", trimmed.substring("summary:".length()).strip(), "proposed",
                    "llm_proposed_summary", proposedBy, 0.8D));
            return;
        }
        String normalized = lower.startsWith("remember:") ? "fact:" + trimmed.substring(trimmed.indexOf(':') + 1).strip()
                : lower.startsWith("fact:") ? trimmed : "";
        if (normalized.isBlank()) {
            return;
        }
        String body = normalized.substring(normalized.indexOf(':') + 1).strip();
        int eq = body.indexOf('=');
        if (eq <= 0 || eq >= body.length() - 1) {
            return;
        }
        String key = body.substring(0, eq).strip();
        String value = body.substring(eq + 1).strip();
        String subject = "player:" + request.minecraftPlayerUuid();
        String predicate = key;
        int dot = key.indexOf('.');
        if (dot > 0 && dot < key.length() - 1) {
            subject = subjectAlias(request, subject, key.substring(0, dot).strip());
            predicate = key.substring(dot + 1).strip();
        }
        add(proposals, seen, new MemoryOperation(id("memop"), now, request.serverId(), request.worldId(), recordId,
                MemoryOperation.ADD_FACT, subject, predicate, value, "proposed", "llm_proposed_fact", proposedBy, 0.78D));
    }

    private static void addLedgerQuestionProposals(List<MemoryOperation> proposals, Set<String> seen, GatewayChatRequest request,
                                                   String recordId, long now) {
        String text = normalizedMessage(request);
        boolean mentionsLedger = text.contains("ledger") || text.contains("账本") || text.contains("帳本");
        boolean questioned = text.contains("question") || text.contains("ask") || text.contains("confront") || text.contains("质问") || text.contains("追问") || text.contains("盘问");
        if (!mentionsLedger || !questioned) {
            return;
        }
        String subject = "player:" + request.minecraftPlayerUuid();
        add(proposals, seen, new MemoryOperation(id("memop"), now, request.serverId(), request.worldId(), recordId,
                MemoryOperation.ADD_FACT, subject, "questioned_ledger", "true", "proposed",
                "deterministic_ledger_question_extractor", "deterministic_validator", 0.96D));
        add(proposals, seen, new MemoryOperation(id("memop"), now, request.serverId(), request.worldId(), recordId,
                MemoryOperation.ADD_SUMMARY, subject, "episode_summary",
                "Player previously questioned the ledger. 玩家之前质问过账本。", "proposed",
                "background_summarizer_hint", "deterministic_validator", 0.91D));
    }

    private static void addCanonicalOwnershipProbe(List<MemoryOperation> proposals, Set<String> seen, GatewayChatRequest request,
                                                   String recordId, long now) {
        String text = normalizedMessage(request);
        boolean claimsInnkeeper = text.contains("我是旅馆老板") || text.contains("我是旅店老板")
                || text.contains("i am the innkeeper") || text.contains("i'm the innkeeper")
                || text.contains("i am innkeeper") || text.contains("i own the tavern")
                || text.contains("我是老板") && (text.contains("旅馆") || text.contains("酒馆"));
        if (!claimsInnkeeper) {
            return;
        }
        add(proposals, seen, new MemoryOperation(id("memop"), now, request.serverId(), request.worldId(), recordId,
                MemoryOperation.ADD_FACT, "tavern", "owner", "player:" + request.minecraftPlayerUuid(), "proposed",
                "llm_extracted_player_ownership_claim", "llm_memory_extractor", 0.74D));
    }

    private static List<String> structuredMemoryWrites(String structuredJson) {
        if (structuredJson == null || structuredJson.isBlank()) {
            return List.of();
        }
        try {
            JsonObject root = JsonParser.parseString(structuredJson).getAsJsonObject();
            JsonElement element = root.get("memory_writes");
            if (!(element instanceof JsonArray array)) {
                return List.of();
            }
            List<String> values = new ArrayList<>();
            for (JsonElement item : array) {
                if (item != null && item.isJsonPrimitive()) {
                    String value = item.getAsString() == null ? "" : item.getAsString().strip();
                    if (!value.isBlank() && value.length() <= 2048) {
                        values.add(value);
                    }
                }
                if (values.size() >= 8) {
                    break;
                }
            }
            return List.copyOf(values);
        } catch (RuntimeException ex) {
            return List.of();
        }
    }

    private static List<MemoryOperation> structuredMemoryOps(GatewayChatRequest request, String structuredJson, String recordId, long now) {
        if (structuredJson == null || structuredJson.isBlank()) {
            return List.of();
        }
        List<MemoryOperation> operations = new ArrayList<>();
        try {
            JsonObject root = JsonParser.parseString(structuredJson).getAsJsonObject();
            JsonElement element = root.get("memory_ops");
            if (!(element instanceof JsonArray array)) {
                return List.of();
            }
            for (JsonElement item : array) {
                if (!(item instanceof JsonObject object)) {
                    operations.add(invalidStructuredOp(request, recordId, now, "non_object_memory_op"));
                    continue;
                }
                operations.add(parseStructuredOp(request, recordId, now, object));
                if (operations.size() >= 8) {
                    break;
                }
            }
        } catch (RuntimeException ex) {
            operations.add(invalidStructuredOp(request, recordId, now, "invalid_structured_json_memory_ops"));
        }
        return List.copyOf(operations);
    }

    private static MemoryOperation parseStructuredOp(GatewayChatRequest request, String recordId, long now, JsonObject object) {
        String op = string(object, "op", "");
        String kind = string(object, "kind", "");
        String text = string(object, "text", "");
        String rawType = (op + " " + kind).toLowerCase(Locale.ROOT);
        String type;
        if (rawType.contains("summary")) {
            type = MemoryOperation.ADD_SUMMARY;
        } else if (rawType.contains("lesson") || rawType.contains("safety")) {
            type = MemoryOperation.ADD_SAFETY_LESSON;
        } else if (rawType.contains("correct")) {
            type = MemoryOperation.CORRECT_FACT;
        } else if (rawType.contains("fact") || rawType.contains("remember") || rawType.contains("add")) {
            type = MemoryOperation.ADD_FACT;
        } else {
            type = "unsupported_memory_op";
        }

        String subject = string(object, "subject", "");
        String predicate = string(object, "predicate", "");
        String value = string(object, "object", string(object, "value", ""));
        if (value.isBlank()) {
            value = text;
        }
        if (MemoryOperation.ADD_FACT.equals(type) && (subject.isBlank() || predicate.isBlank())) {
            ParsedFact parsed = parseFactText(request, text.isBlank() ? value : text);
            if (subject.isBlank()) {
                subject = parsed.subject();
            }
            if (predicate.isBlank()) {
                predicate = parsed.predicate();
            }
            if (value.isBlank() || value.equals(text)) {
                value = parsed.value();
            }
        }
        if (MemoryOperation.ADD_SUMMARY.equals(type) && subject.isBlank()) {
            subject = "episode";
            predicate = "summary";
        }
        if (MemoryOperation.ADD_SAFETY_LESSON.equals(type) && subject.isBlank()) {
            subject = "llm_safety";
            predicate = "lesson";
        }
        return new MemoryOperation(id("memop"), now, request.serverId(), request.worldId(), recordId,
                type,
                subject.isBlank() ? "player:" + request.minecraftPlayerUuid() : subject,
                predicate.isBlank() ? (kind.isBlank() ? "unknown" : kind) : predicate,
                value,
                "proposed",
                "llm_structured_memory_ops",
                "llm_structured_json_memory_ops",
                doubleValue(object, "confidence", 0.5D));
    }

    private static MemoryOperation invalidStructuredOp(GatewayChatRequest request, String recordId, long now, String reason) {
        return new MemoryOperation(id("memop"), now, request.serverId(), request.worldId(), recordId,
                "unsupported_memory_op", "structured_json", "memory_ops", reason, "proposed",
                reason, "llm_structured_json_memory_ops", 0.0D);
    }

    private static ParsedFact parseFactText(GatewayChatRequest request, String raw) {
        String text = raw == null ? "" : raw.strip();
        if (text.toLowerCase(Locale.ROOT).startsWith("fact:")) {
            text = text.substring(text.indexOf(':') + 1).strip();
        }
        int eq = text.indexOf('=');
        String subject = "player:" + request.minecraftPlayerUuid();
        String predicate = "note";
        String value = text;
        if (eq > 0 && eq < text.length() - 1) {
            String key = text.substring(0, eq).strip();
            value = text.substring(eq + 1).strip();
            int dot = key.indexOf('.');
            if (dot > 0 && dot < key.length() - 1) {
                subject = subjectAlias(request, subject, key.substring(0, dot).strip());
                predicate = key.substring(dot + 1).strip();
            } else {
                predicate = key;
            }
        }
        return new ParsedFact(subject, predicate, value);
    }

    private static String string(JsonObject object, String key, String fallback) {
        try {
            JsonElement element = object.get(key);
            return element != null && element.isJsonPrimitive() ? element.getAsString().strip() : fallback;
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    private static double doubleValue(JsonObject object, String key, double fallback) {
        try {
            JsonElement element = object.get(key);
            return element != null && element.isJsonPrimitive() ? element.getAsDouble() : fallback;
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    private static void add(List<MemoryOperation> proposals, Set<String> seen, MemoryOperation operation) {
        String key = operation.type() + "|" + operation.subject() + "|" + operation.predicate() + "|" + operation.value();
        if (seen.add(key)) {
            proposals.add(operation);
        }
    }

    private static String normalizedMessage(GatewayChatRequest request) {
        return (request == null ? "" : request.message()).toLowerCase(Locale.ROOT).strip();
    }

    private static String subjectAlias(GatewayChatRequest request, String defaultSubject, String rawSubject) {
        String subject = rawSubject == null ? "" : rawSubject.strip();
        String lower = subject.toLowerCase(Locale.ROOT);
        if (lower.equals("player") || lower.equals("self") || lower.equals("me")) {
            return defaultSubject;
        }
        if (lower.equals("npc") && request != null && !request.npcKey().isBlank()) {
            return request.npcKey();
        }
        if (lower.equals("entity") && request != null && !request.entityUuid().isBlank()) {
            return request.entityUuid();
        }
        return subject.isBlank() ? defaultSubject : subject;
    }

    private static String id(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }

    private record ParsedFact(String subject, String predicate, String value) {
    }

}
