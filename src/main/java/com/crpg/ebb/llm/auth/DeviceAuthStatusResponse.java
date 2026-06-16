package com.crpg.ebb.llm.auth;

import java.util.Optional;

public record DeviceAuthStatusResponse(
        String status,
        Optional<LlmAuthToken> token,
        long intervalSeconds,
        String error
) {
    public static DeviceAuthStatusResponse pending(long intervalSeconds) {
        return new DeviceAuthStatusResponse("pending", Optional.empty(), Math.max(1L, intervalSeconds), "");
    }

    public static DeviceAuthStatusResponse authenticated(LlmAuthToken token) {
        return new DeviceAuthStatusResponse("authenticated", Optional.of(token), 0L, "");
    }

    public static DeviceAuthStatusResponse error(String reason) {
        return new DeviceAuthStatusResponse("error", Optional.empty(), 0L, reason == null ? "auth_error" : reason);
    }

    public boolean authenticated() {
        return "authenticated".equals(status) && token.isPresent();
    }
}
