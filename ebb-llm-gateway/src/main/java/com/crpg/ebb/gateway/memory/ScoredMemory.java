package com.crpg.ebb.gateway.memory;

import java.util.LinkedHashMap;
import java.util.Map;

public record ScoredMemory(MemoryRecord record, double score, String reason) {
    public Map<String, Object> toJsonMap() {
        Map<String, Object> values = new LinkedHashMap<>(record.toJsonMap());
        values.put("score", Math.round(score * 1000.0D) / 1000.0D);
        values.put("reason", reason);
        return values;
    }
}
