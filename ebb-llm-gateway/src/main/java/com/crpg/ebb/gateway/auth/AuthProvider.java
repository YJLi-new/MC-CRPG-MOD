package com.crpg.ebb.gateway.auth;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface AuthProvider {
    ProviderStart start(String minecraftUuid, String serverId, String authSessionId, String userCode);

    ProviderStatus poll(ProviderSession session);

    String providerName();

    record ProviderStart(
            String verificationUrl,
            String userCode,
            String providerSessionId,
            long expiresInSeconds,
            long intervalSeconds,
            Map<String, String> providerMetadata
    ) {
    }

    record ProviderSession(
            String providerSessionId,
            String minecraftUuid,
            String serverId,
            String userCode,
            Instant expiresAt,
            Map<String, String> providerMetadata
    ) {
    }

    record ProviderStatus(
            String status,
            String providerSubject,
            List<String> scopes,
            long expiresInSeconds,
            String error
    ) {
        public static ProviderStatus pending() {
            return new ProviderStatus("pending", "", List.of(), 0L, "");
        }

        public static ProviderStatus authenticated(String subject, List<String> scopes, long expiresInSeconds) {
            return new ProviderStatus("authenticated", subject == null ? "" : subject, List.copyOf(scopes), expiresInSeconds, "");
        }

        public static ProviderStatus error(String reason) {
            return new ProviderStatus("error", "", List.of(), 0L, reason == null ? "auth_error" : reason);
        }
    }
}
