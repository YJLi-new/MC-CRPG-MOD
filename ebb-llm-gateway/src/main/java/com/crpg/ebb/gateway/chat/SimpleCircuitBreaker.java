package com.crpg.ebb.gateway.chat;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class SimpleCircuitBreaker {
    private final int failureThreshold;
    private final long cooldownMillis;
    private final Clock clock;
    private final AtomicInteger consecutiveFailures = new AtomicInteger();
    private final AtomicReference<Instant> openUntil = new AtomicReference<>(Instant.EPOCH);

    public SimpleCircuitBreaker(int failureThreshold, long cooldownMillis) {
        this(failureThreshold, cooldownMillis, Clock.systemUTC());
    }

    SimpleCircuitBreaker(int failureThreshold, long cooldownMillis, Clock clock) {
        this.failureThreshold = Math.max(1, failureThreshold);
        this.cooldownMillis = Math.max(1000L, cooldownMillis);
        this.clock = clock;
    }

    public boolean allowRequest() {
        return clock.instant().isAfter(openUntil.get());
    }

    public void recordSuccess() {
        consecutiveFailures.set(0);
        openUntil.set(Instant.EPOCH);
    }

    public void recordFailure() {
        int failures = consecutiveFailures.incrementAndGet();
        if (failures >= failureThreshold) {
            openUntil.set(clock.instant().plusMillis(cooldownMillis));
        }
    }

    public String debugStatus() {
        return "failures=" + consecutiveFailures.get() + " open_until=" + openUntil.get().toEpochMilli();
    }
}
