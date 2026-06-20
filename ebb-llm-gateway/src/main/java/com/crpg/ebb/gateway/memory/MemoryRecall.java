package com.crpg.ebb.gateway.memory;

import java.util.List;

/** Layered memory retrieval result used before LLM/NPC chat generation. */
public record MemoryRecall(
        List<MemoryFact> activeFacts,
        List<MemoryConflict> openConflicts,
        List<MemorySafetyLesson> safetyLessons,
        List<ScoredMemory> episodes
) {
    public MemoryRecall {
        activeFacts = activeFacts == null ? List.of() : List.copyOf(activeFacts);
        openConflicts = openConflicts == null ? List.of() : List.copyOf(openConflicts);
        safetyLessons = safetyLessons == null ? List.of() : List.copyOf(safetyLessons);
        episodes = episodes == null ? List.of() : List.copyOf(episodes);
    }

    public boolean isEmpty() {
        return activeFacts.isEmpty() && openConflicts.isEmpty() && safetyLessons.isEmpty() && episodes.isEmpty();
    }
}
