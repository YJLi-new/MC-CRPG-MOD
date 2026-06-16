package com.crpg.ebb.gateway.memory;

import java.util.List;

public record MemoryAppendResult(
        List<MemoryRecord> records,
        List<MemoryFact> facts,
        List<MemoryConflict> conflicts,
        List<MemoryOperation> operations,
        List<MemorySummary> summaries,
        List<MemoryLink> links,
        List<MemorySafetyLesson> safetyLessons
) {
    public MemoryAppendResult(List<MemoryRecord> records, List<MemoryFact> facts, List<MemoryConflict> conflicts) {
        this(records, facts, conflicts, List.of(), List.of(), List.of(), List.of());
    }

    public MemoryAppendResult {
        records = records == null ? List.of() : List.copyOf(records);
        facts = facts == null ? List.of() : List.copyOf(facts);
        conflicts = conflicts == null ? List.of() : List.copyOf(conflicts);
        operations = operations == null ? List.of() : List.copyOf(operations);
        summaries = summaries == null ? List.of() : List.copyOf(summaries);
        links = links == null ? List.of() : List.copyOf(links);
        safetyLessons = safetyLessons == null ? List.of() : List.copyOf(safetyLessons);
    }
}
