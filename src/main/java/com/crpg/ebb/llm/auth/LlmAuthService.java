package com.crpg.ebb.llm.auth;

import com.crpg.ebb.llm.LlmConfig;
import com.crpg.ebb.llm.LlmMode;
import com.crpg.ebb.llm.LlmWorldIdentity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class LlmAuthService {
    // P36: server-only token storage; raw opaque tokens must never be sent to client UI/log payloads.
    private static final Map<UUID, PendingAuth> PENDING = new ConcurrentHashMap<>();
    private static final Map<UUID, LlmAuthToken> TOKENS = new ConcurrentHashMap<>();
    private static volatile LlmGatewayAuthClient testingClient;

    private LlmAuthService() {
    }

    public static boolean requiresAuth(LlmConfig config) {
        return config != null && config.active() && config.requirePlayerAuth();
    }

    public static String chatGateStatus(UUID playerUuid, LlmConfig config) {
        if (!requiresAuth(config)) {
            return "auth_not_required";
        }
        return hasValidToken(playerUuid) ? "authenticated" : "auth_required";
    }

    public static boolean hasValidToken(UUID playerUuid) {
        return validToken(playerUuid).isPresent();
    }

    public static Optional<LlmAuthToken> validToken(UUID playerUuid) {
        LlmAuthToken token = TOKENS.get(playerUuid);
        if (token == null) {
            return Optional.empty();
        }
        if (token.isExpired(Instant.now()) || !token.allowsChat()) {
            TOKENS.remove(playerUuid);
            return Optional.empty();
        }
        return Optional.of(token);
    }

    public static CompletableFuture<DeviceAuthStartResponse> startDeviceAuth(ServerPlayer player) {
        return startDeviceAuth(player.getUUID(), serverId(player));
    }

    public static CompletableFuture<DeviceAuthStartResponse> startDeviceAuth(UUID playerUuid, String serverId) {
        LlmConfig config = LlmConfig.current();
        LlmGatewayAuthClient client = clientFor(config);
        return client.startDeviceAuth(playerUuid, serverId).thenApply(response -> {
            if (response.started()) {
                PENDING.put(playerUuid, new PendingAuth(response.authSessionId(), response.provider(), response.intervalSeconds()));
            }
            return response;
        }).exceptionally(error -> DeviceAuthStartResponse.error("auth_start_failed"));
    }

    public static CompletableFuture<DeviceAuthStatusResponse> pollDeviceAuth(ServerPlayer player) {
        return pollDeviceAuth(player.getUUID());
    }

    public static CompletableFuture<DeviceAuthStatusResponse> pollDeviceAuth(UUID playerUuid) {
        Optional<LlmAuthToken> existing = validToken(playerUuid);
        if (existing.isPresent()) {
            return CompletableFuture.completedFuture(DeviceAuthStatusResponse.authenticated(existing.get()));
        }
        PendingAuth pending = PENDING.get(playerUuid);
        if (pending == null) {
            return CompletableFuture.completedFuture(DeviceAuthStatusResponse.error("not_authenticated"));
        }
        LlmGatewayAuthClient client = clientFor(LlmConfig.current());
        return client.pollDeviceAuth(pending.authSessionId()).thenApply(status -> {
            if (status.authenticated()) {
                TOKENS.put(playerUuid, status.token().orElseThrow());
                PENDING.remove(playerUuid);
            }
            return status;
        }).exceptionally(error -> DeviceAuthStatusResponse.error("auth_status_failed"));
    }

    public static CompletableFuture<Boolean> logout(ServerPlayer player) {
        return logout(player.getUUID());
    }

    public static CompletableFuture<Boolean> logout(UUID playerUuid) {
        PENDING.remove(playerUuid);
        LlmAuthToken removed = TOKENS.remove(playerUuid);
        if (removed == null) {
            return CompletableFuture.completedFuture(false);
        }
        return clientFor(LlmConfig.current()).logout(removed).exceptionally(error -> false);
    }

    public static String safeStatusLine(UUID playerUuid) {
        LlmConfig config = LlmConfig.current();
        String gate = chatGateStatus(playerUuid, config);
        PendingAuth pending = PENDING.get(playerUuid);
        Optional<LlmAuthToken> token = validToken(playerUuid);
        StringBuilder line = new StringBuilder("auth_required=").append(requiresAuth(config))
                .append(" gate=").append(gate)
                .append(" provider=").append(clientFor(config).providerName());
        if (pending != null) {
            line.append(" pending_session=").append(pending.authSessionId()).append(" interval_seconds=").append(pending.intervalSeconds());
        }
        token.ifPresent(value -> line.append(' ').append(value.redactedSummary()));
        return line.toString();
    }

    public static Map<String, String> debugSnapshot(UUID playerUuid) {
        Map<String, String> snapshot = new LinkedHashMap<>();
        LlmConfig config = LlmConfig.current();
        snapshot.put("require_player_auth", String.valueOf(requiresAuth(config)));
        snapshot.put("gate", chatGateStatus(playerUuid, config));
        snapshot.put("provider", clientFor(config).providerName());
        snapshot.put("has_pending_session", String.valueOf(PENDING.containsKey(playerUuid)));
        snapshot.put("has_server_token", String.valueOf(validToken(playerUuid).isPresent()));
        snapshot.put("token_value", "redacted");
        return Map.copyOf(snapshot);
    }

    public static void setClientForTesting(LlmGatewayAuthClient client) {
        testingClient = client;
    }

    public static void grantTokenForTesting(UUID playerUuid, LlmAuthToken token) {
        if (token == null) {
            TOKENS.remove(playerUuid);
        } else {
            TOKENS.put(playerUuid, token);
        }
    }

    public static void clearTestingOverrides() {
        testingClient = null;
        PENDING.clear();
        TOKENS.clear();
    }

    private static LlmGatewayAuthClient clientFor(LlmConfig config) {
        LlmGatewayAuthClient test = testingClient;
        if (test != null) {
            return test;
        }
        if (config != null && config.mode() == LlmMode.GATEWAY && !config.gatewayUrl().isBlank()) {
            return new HttpLlmGatewayAuthClient(config);
        }
        return new DevLocalLlmAuthClient();
    }

    private static String serverId(ServerPlayer player) {
        return LlmWorldIdentity.serverId(((ServerLevel) player.level()).getServer());
    }

    private record PendingAuth(String authSessionId, String provider, long intervalSeconds) {
    }
}
