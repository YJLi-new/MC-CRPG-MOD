package com.crpg.ebb.gateway.memory;

import com.crpg.ebb.gateway.chat.GatewayChatRequest;
import com.crpg.ebb.gateway.chat.GatewayChatResponse;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * P39 extractor: treats LLM-proposed memory_writes as proposals only, then adds a
 * deterministic fallback for tests/dev authoring. Nothing from this class is written
 * until {@link DeterministicMemoryValidator} accepts it.
 */
public final class LlmMemoryOperationExtractor {
    private static final Pattern WRITE_ARRAY = Pattern.compile("\\\"memory_writes\\\"\\s*:\\s*\\[(.*?)]", Pattern.DOTALL);
    private static final Pattern QUOTED = Pattern.compile("\\\"((?:\\\\.|[^\\\\\\\"])*)\\\"");
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
            subject = key.substring(0, dot).strip();
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
        Matcher array = WRITE_ARRAY.matcher(structuredJson);
        if (!array.find()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        Matcher quoted = QUOTED.matcher(array.group(1));
        while (quoted.find()) {
            values.add(unescape(quoted.group(1)));
        }
        return List.copyOf(values);
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

    private static String id(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }

    private static String unescape(String value) {
        return value == null ? "" : value.replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t").replace("\\\"", "\"").replace("\\\\", "\\");
    }
}
