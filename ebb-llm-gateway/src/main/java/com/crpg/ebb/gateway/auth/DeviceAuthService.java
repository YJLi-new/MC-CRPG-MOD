package com.crpg.ebb.gateway.auth;

import com.crpg.ebb.gateway.HttpJson;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class DeviceAuthService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final AuthProvider provider;
    private final Clock clock;
    private final Map<String, AuthSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, TokenRecord> tokens = new ConcurrentHashMap<>();

    public DeviceAuthService(AuthProvider provider) {
        this(provider, Clock.systemUTC());
    }

    DeviceAuthService(AuthProvider provider, Clock clock) {
        this.provider = provider;
        this.clock = clock;
    }

    public StartResult start(String minecraftUuid, String serverId) {
        String authSessionId = "ebb_auth_" + randomHex(18);
        String userCode = userCode();
        AuthProvider.ProviderStart providerStart = provider.start(minecraftUuid, serverId, authSessionId, userCode);
        Instant expiresAt = clock.instant().plusSeconds(Math.max(30L, providerStart.expiresInSeconds()));
        AuthProvider.ProviderSession providerSession = new AuthProvider.ProviderSession(
                providerStart.providerSessionId(),
                minecraftUuid,
                serverId,
                providerStart.userCode(),
                expiresAt,
                providerStart.providerMetadata() == null ? Map.of() : providerStart.providerMetadata()
        );
        sessions.put(authSessionId, new AuthSession(authSessionId, providerSession, providerStart.intervalSeconds(), false));
        return new StartResult(authSessionId, providerStart.verificationUrl(), providerStart.userCode(),
                Math.max(0L, expiresAt.getEpochSecond() - clock.instant().getEpochSecond()),
                Math.max(1L, providerStart.intervalSeconds()), provider.providerName());
    }

    public StatusResult status(String authSessionId) {
        AuthSession session = sessions.get(authSessionId);
        if (session == null) {
            return StatusResult.error("unknown_auth_session");
        }
        if (clock.instant().isAfter(session.providerSession().expiresAt())) {
            sessions.remove(authSessionId);
            return StatusResult.error("auth_session_expired");
        }
        if (session.authenticated()) {
            Optional<TokenRecord> token = tokens.values().stream()
                    .filter(record -> authSessionId.equals(record.authSessionId()))
                    .findFirst();
            if (token.isPresent()) {
                return StatusResult.authenticated(token.get().opaqueToken(), token.get().scopes(), token.get().expiresAt().getEpochSecond());
            }
        }
        AuthProvider.ProviderStatus providerStatus = provider.poll(session.providerSession());
        if ("pending".equals(providerStatus.status())) {
            return StatusResult.pending(session.intervalSeconds());
        }
        if ("authenticated".equals(providerStatus.status())) {
            String opaqueToken = "ebb_player_" + randomHex(32);
            Instant expiresAt = clock.instant().plusSeconds(Math.max(60L, providerStatus.expiresInSeconds()));
            List<String> scopes = providerStatus.scopes().isEmpty() ? List.of("llm:chat") : providerStatus.scopes();
            tokens.put(opaqueToken, new TokenRecord(opaqueToken, authSessionId, session.providerSession().minecraftUuid(),
                    providerStatus.providerSubject(), scopes, expiresAt));
            sessions.put(authSessionId, new AuthSession(authSessionId, session.providerSession(), session.intervalSeconds(), true));
            return StatusResult.authenticated(opaqueToken, scopes, expiresAt.getEpochSecond());
        }
        return StatusResult.error(providerStatus.error().isBlank() ? "auth_error" : providerStatus.error());
    }

    public boolean logout(String opaqueToken) {
        if (opaqueToken == null || opaqueToken.isBlank()) {
            return false;
        }
        return tokens.remove(opaqueToken) != null;
    }

    public boolean tokenValid(String opaqueToken) {
        TokenRecord record = tokens.get(opaqueToken);
        if (record == null) {
            return false;
        }
        if (clock.instant().isAfter(record.expiresAt())) {
            tokens.remove(opaqueToken);
            return false;
        }
        return true;
    }

    public String healthJson() {
        return HttpJson.object(Map.of(
                "status", "ok",
                "service", "ebb-llm-gateway",
                "auth_provider", provider.providerName(),
                "active_auth_sessions", sessions.size(),
                "active_player_tokens", tokens.size()
        ));
    }

    private static String randomHex(int bytes) {
        byte[] data = new byte[bytes];
        RANDOM.nextBytes(data);
        return HexFormat.of().formatHex(data);
    }

    private static String userCode() {
        String raw = randomHex(4).toUpperCase();
        return raw.substring(0, 4) + "-" + raw.substring(4);
    }

    public record StartResult(
            String authSessionId,
            String verificationUrl,
            String userCode,
            long expiresInSeconds,
            long intervalSeconds,
            String providerName
    ) {
        public String toJson() {
            return HttpJson.object(new LinkedHashMap<>(Map.of(
                    "status", "pending",
                    "auth_session_id", authSessionId,
                    "verification_url", verificationUrl,
                    "user_code", userCode,
                    "expires_in_seconds", expiresInSeconds,
                    "interval_seconds", intervalSeconds,
                    "provider", providerName
            )));
        }
    }

    public record StatusResult(String status, String opaqueToken, List<String> scopes, long expiresAtEpochSeconds, long intervalSeconds, String error) {
        public static StatusResult pending(long intervalSeconds) {
            return new StatusResult("pending", "", List.of(), 0L, Math.max(1L, intervalSeconds), "");
        }

        public static StatusResult authenticated(String opaqueToken, List<String> scopes, long expiresAtEpochSeconds) {
            return new StatusResult("authenticated", opaqueToken, List.copyOf(scopes), expiresAtEpochSeconds, 0L, "");
        }

        public static StatusResult error(String reason) {
            return new StatusResult("error", "", List.of(), 0L, 0L, reason);
        }

        public String toJson() {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("status", status);
            if ("authenticated".equals(status)) {
                values.put("opaque_player_token", opaqueToken);
                values.put("scopes", scopes);
                values.put("expires_at_epoch_seconds", expiresAtEpochSeconds);
            } else if ("pending".equals(status)) {
                values.put("interval_seconds", intervalSeconds);
            } else {
                values.put("error", error);
            }
            return HttpJson.object(values);
        }
    }

    private record AuthSession(String authSessionId, AuthProvider.ProviderSession providerSession, long intervalSeconds, boolean authenticated) {
    }

    private record TokenRecord(String opaqueToken, String authSessionId, String minecraftUuid, String providerSubject, List<String> scopes, Instant expiresAt) {
    }
}
