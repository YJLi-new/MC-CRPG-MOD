package com.crpg.ebb.llm.auth;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class DevLocalLlmAuthClient implements LlmGatewayAuthClient {
    private final Map<String, UUID> sessions = new ConcurrentHashMap<>();

    @Override
    public CompletableFuture<DeviceAuthStartResponse> startDeviceAuth(UUID playerUuid, String serverId) {
        String session = "dev_local_" + playerUuid.toString().replace("-", "").substring(0, 16);
        sessions.put(session, playerUuid);
        return CompletableFuture.completedFuture(new DeviceAuthStartResponse(
                true,
                session,
                "http://127.0.0.1:8787/v1/auth/dev-local/complete?auth_session_id=" + session,
                "DEV-LOCAL",
                600L,
                1L,
                providerName(),
                ""
        ));
    }

    @Override
    public CompletableFuture<DeviceAuthStatusResponse> pollDeviceAuth(String authSessionId) {
        UUID playerUuid = sessions.get(authSessionId);
        if (playerUuid == null) {
            return CompletableFuture.completedFuture(DeviceAuthStatusResponse.error("unknown_auth_session"));
        }
        String opaque = "dev-local-token-" + playerUuid + "-" + authSessionId;
        LlmAuthToken token = new LlmAuthToken(
                opaque,
                List.of("llm:chat", "memory:read_self", "memory:write_self"),
                Instant.now().plusSeconds(3600L).getEpochSecond(),
                providerName()
        );
        return CompletableFuture.completedFuture(DeviceAuthStatusResponse.authenticated(token));
    }

    @Override
    public CompletableFuture<Boolean> logout(LlmAuthToken token) {
        return CompletableFuture.completedFuture(token != null && !token.opaqueToken().isBlank());
    }

    @Override
    public String providerName() {
        return "dev_local";
    }
}
