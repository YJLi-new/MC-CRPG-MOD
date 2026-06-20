package com.crpg.ebb.gateway;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** Fixed-window rate limiter for gateway cost/safety gates. */
public final class GatewayRateLimiter {
    private static final long WINDOW_MS = 60_000L;
    private final Clock clock;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public GatewayRateLimiter() {
        this(Clock.systemUTC());
    }

    GatewayRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public RateDecision allow(String key, int limitPerMinute) {
        int limit = Math.max(1, limitPerMinute);
        long now = clock.millis();
        long windowStart = now - Math.floorMod(now, WINDOW_MS);
        Bucket bucket = buckets.computeIfAbsent(key == null ? "" : key, ignored -> new Bucket(windowStart));
        synchronized (bucket) {
            if (bucket.windowStartMs != windowStart) {
                bucket.windowStartMs = windowStart;
                bucket.count.set(0L);
            }
            long next = bucket.count.incrementAndGet();
            boolean allowed = next <= limit;
            long retryAfterMs = allowed ? 0L : Math.max(1L, windowStart + WINDOW_MS - now);
            return new RateDecision(allowed, key, limit, next, retryAfterMs);
        }
    }

    public Map<String, Object> snapshot() {
        Map<String, Object> out = new LinkedHashMap<>();
        buckets.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> out.put(entry.getKey(), entry.getValue().count.get()));
        return out;
    }

    public record RateDecision(boolean allowed, String key, int limit, long count, long retryAfterMs) {
        public Map<String, Object> toJsonMap() {
            return Map.of(
                    "allowed", allowed,
                    "key", key == null ? "" : key,
                    "limit_per_minute", limit,
                    "count", count,
                    "retry_after_ms", retryAfterMs
            );
        }
    }

    private static final class Bucket {
        private long windowStartMs;
        private final AtomicLong count = new AtomicLong();

        private Bucket(long windowStartMs) {
            this.windowStartMs = windowStartMs;
        }
    }
}
