package com.crpg.ebb.llm.auth;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface LlmGatewayAuthClient {
    CompletableFuture<DeviceAuthStartResponse> startDeviceAuth(UUID playerUuid, String serverId);

    CompletableFuture<DeviceAuthStatusResponse> pollDeviceAuth(String authSessionId);

    CompletableFuture<Boolean> logout(LlmAuthToken token);

    String providerName();

    default boolean usesNetwork() {
        return false;
    }
}
