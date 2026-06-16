package com.crpg.ebb.gateway.memory;

import java.util.List;

public record MemoryAppendResult(List<MemoryRecord> records, List<MemoryFact> facts, List<MemoryConflict> conflicts) {
    public MemoryAppendResult {
        records = records == null ? List.of() : List.copyOf(records);
        facts = facts == null ? List.of() : List.copyOf(facts);
        conflicts = conflicts == null ? List.of() : List.copyOf(conflicts);
    }
}
