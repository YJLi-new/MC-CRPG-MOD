package com.crpg.ebb.gateway.memory;

import java.util.LinkedHashMap;
import java.util.Map;

/** Persistent fixed-window + daily quota decision for player LLM chat. */
public record QuotaDecision(
        boolean allowed,
        String key,
        int perMinuteLimit,
        long usedInWindow,
        long remaining,
        long resetEpochMs,
        int dailyLimit,
        long dailyUsed,
        long dailyRemaining,
        long dailyResetEpochMs
) {
    public Map<String, Object> toJsonMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("allowed", allowed);
        values.put("key", key == null ? "" : key);
        values.put("limit_per_minute", perMinuteLimit);
        values.put("used_in_window", usedInWindow);
        values.put("remaining", remaining);
        values.put("reset_epoch_ms", resetEpochMs);
        values.put("daily_limit", dailyLimit);
        values.put("daily_used", dailyUsed);
        values.put("daily_remaining", dailyRemaining);
        values.put("daily_reset_epoch_ms", dailyResetEpochMs);
        return values;
    }
}
