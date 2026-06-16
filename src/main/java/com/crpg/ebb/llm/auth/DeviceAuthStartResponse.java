package com.crpg.ebb.llm.auth;

public record DeviceAuthStartResponse(
        boolean started,
        String authSessionId,
        String verificationUrl,
        String userCode,
        long expiresInSeconds,
        long intervalSeconds,
        String provider,
        String error
) {
    public static DeviceAuthStartResponse error(String reason) {
        return new DeviceAuthStartResponse(false, "", "", "", 0L, 0L, "", reason == null ? "auth_start_failed" : reason);
    }
}
