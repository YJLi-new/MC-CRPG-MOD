package com.crpg.ebb.llm.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

public record LlmAuthToken(
        String opaqueToken,
        List<String> scopes,
        long expiresAtEpochSeconds,
        String provider
) {
    public LlmAuthToken {
        opaqueToken = opaqueToken == null ? "" : opaqueToken;
        scopes = scopes == null ? List.of() : List.copyOf(scopes);
        provider = provider == null || provider.isBlank() ? "unknown" : provider;
    }

    public boolean isExpired(Instant now) {
        return expiresAtEpochSeconds > 0 && now.getEpochSecond() >= expiresAtEpochSeconds;
    }

    public boolean allowsChat() {
        return scopes.isEmpty() || scopes.contains("llm:chat");
    }

    public String redactedSummary() {
        return "provider=" + provider + " token=redacted:" + fingerprint(opaqueToken)
                + " scopes=" + scopes + " expires_at=" + expiresAtEpochSeconds;
    }

    private static String fingerprint(String value) {
        if (value == null || value.isBlank()) {
            return "none";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 12);
        } catch (RuntimeException | java.security.NoSuchAlgorithmException ex) {
            return "unavailable";
        }
    }
}
