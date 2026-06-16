package com.crpg.ebb.gateway.memory;

import com.crpg.ebb.gateway.chat.GatewayChatRequest;

import java.util.List;
import java.util.Locale;

/** P39 deterministic background summarizer/evolution helper. */
public final class MemoryConsolidator {
    public String backgroundSummarize(GatewayChatRequest request, MemoryRecord record, List<MemoryOperation> acceptedOperations) {
        StringBuilder summary = new StringBuilder();
        String role = record == null ? "episode" : record.role();
        summary.append(role).append(" episode: ").append(abbreviate(record == null ? "" : record.text(), 160));
        for (MemoryOperation operation : acceptedOperations == null ? List.<MemoryOperation>of() : acceptedOperations) {
            if (!MemoryOperation.ADD_SUMMARY.equals(operation.type())) {
                continue;
            }
            summary.append(" | summary: ").append(operation.value());
        }
        for (MemoryOperation operation : acceptedOperations == null ? List.<MemoryOperation>of() : acceptedOperations) {
            if (MemoryOperation.ADD_FACT.equals(operation.type())) {
                summary.append(" | extracted fact: ").append(operation.subject()).append('.').append(operation.predicate()).append('=').append(operation.value());
            }
        }
        return summary.toString();
    }

    public String evolveSummary(String previousSummary, MemoryFact oldFact, MemoryFact newFact) {
        String base = previousSummary == null || previousSummary.isBlank()
                ? "A previous raw episode is preserved."
                : previousSummary.strip();
        return base + " | evolved summary: later memory changed " + oldFact.subject() + '.' + oldFact.predicate()
                + " from " + oldFact.value() + " to " + newFact.value() + "; raw episode text is preserved.";
    }

    public String relationReason(MemoryRecord source, MemoryRecord target) {
        if (source == null || target == null) {
            return "related_memory";
        }
        if (!source.conversationId().isBlank() && source.conversationId().equals(target.conversationId())) {
            return "same_conversation";
        }
        if (!source.npcKey().isBlank() && source.npcKey().equals(target.npcKey())) {
            return "same_npc_context";
        }
        return "recent_context";
    }

    public String safetyLessonForCanonicalRejection(MemoryOperation operation, String reason) {
        String value = operation == null ? "" : operation.value();
        return "A-MemGuard safety lesson: do not overwrite canonical tavern ownership from player self-claims; "
                + "canonical owner remains innkeeper. Rejected value=" + value + " reason=" + (reason == null ? "canonical_conflict" : reason);
    }

    public boolean isLedgerQuestion(MemoryOperation operation) {
        return operation != null
                && MemoryOperation.ADD_FACT.equals(operation.type())
                && "questioned_ledger".equals(operation.predicate())
                && "true".equals(operation.value().toLowerCase(Locale.ROOT));
    }

    private static String abbreviate(String value, int max) {
        String safe = value == null ? "" : value.replace('\n', ' ').strip();
        return safe.length() <= max ? safe : safe.substring(0, Math.max(0, max - 1)) + "…";
    }
}
