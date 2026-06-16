package com.crpg.ebb.gateway;

import com.crpg.ebb.gateway.auth.AuthProvider;
import com.crpg.ebb.gateway.auth.DevLocalAuthProvider;
import com.crpg.ebb.gateway.auth.OidcAuthProvider;
import com.crpg.ebb.gateway.chat.FakeGatewayChatProvider;
import com.crpg.ebb.gateway.chat.GatewayChatProvider;
import com.crpg.ebb.gateway.chat.MockGatewayChatProvider;
import com.crpg.ebb.gateway.chat.OpenAiResponsesChatProvider;

import java.net.URI;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;

public record GatewayConfig(
        String bindHost,
        int port,
        String authProviderName,
        URI publicBaseUri,
        URI oidcDeviceAuthorizationEndpoint,
        URI oidcTokenEndpoint,
        String oidcClientId,
        String oidcClientSecret,
        String oidcScope,
        Duration httpTimeout,
        String chatProviderName,
        String defaultChatModel,
        boolean defaultStreaming,
        boolean defaultStructuredOutput,
        boolean openAiStoreEnabled,
        int maxOutputTokens,
        int circuitFailureThreshold,
        long circuitCooldownMs
) {
    public static GatewayConfig fromEnv(Map<String, String> env) {
        String bindHost = value(env, "EBB_GATEWAY_BIND", "127.0.0.1");
        int port = parseInt(value(env, "EBB_GATEWAY_PORT", "8787"), 8787);
        String provider = value(env, "EBB_GATEWAY_AUTH_PROVIDER", "dev_local").toLowerCase(Locale.ROOT);
        URI publicBase = URI.create(value(env, "EBB_GATEWAY_PUBLIC_BASE_URL", "http://127.0.0.1:" + port));
        URI deviceEndpoint = optionalUri(value(env, "EBB_OIDC_DEVICE_AUTH_URL", ""));
        URI tokenEndpoint = optionalUri(value(env, "EBB_OIDC_TOKEN_URL", ""));
        String clientId = value(env, "EBB_OIDC_CLIENT_ID", "");
        String clientSecret = value(env, "EBB_OIDC_CLIENT_SECRET", "");
        String scope = value(env, "EBB_OIDC_SCOPE", "openid profile llm:chat memory:read_self memory:write_self");
        int timeoutMs = parseInt(value(env, "EBB_GATEWAY_HTTP_TIMEOUT_MS", "30000"), 30000);
        String chatProvider = value(env, "EBB_GATEWAY_CHAT_PROVIDER", "fake").toLowerCase(Locale.ROOT);
        String defaultModel = value(env, "EBB_OPENAI_MODEL", value(env, "EBB_DEFAULT_CHAT_MODEL", "gpt-5.2"));
        boolean streaming = parseBool(value(env, "EBB_LLM_CHAT_STREAMING", "true"), true);
        boolean structured = parseBool(value(env, "EBB_LLM_STRUCTURED_OUTPUT", "true"), true);
        boolean store = parseBool(value(env, "EBB_OPENAI_STORE", "false"), false);
        int maxOutput = parseInt(value(env, "EBB_OPENAI_MAX_OUTPUT_TOKENS", "700"), 700);
        int circuitThreshold = parseInt(value(env, "EBB_GATEWAY_CIRCUIT_FAILURE_THRESHOLD", "3"), 3);
        long circuitCooldown = parseInt(value(env, "EBB_GATEWAY_CIRCUIT_COOLDOWN_MS", "30000"), 30000);
        return new GatewayConfig(bindHost, port, provider, publicBase, deviceEndpoint, tokenEndpoint,
                clientId, clientSecret, scope, Duration.ofMillis(Math.max(1000, timeoutMs)), chatProvider, defaultModel,
                streaming, structured, store, Math.max(64, maxOutput), Math.max(1, circuitThreshold), Math.max(1000L, circuitCooldown));
    }

    public GatewayChatProvider createChatProvider() {
        if ("fake".equals(chatProviderName) || "dev".equals(chatProviderName) || "dev_local".equals(chatProviderName)) {
            return new FakeGatewayChatProvider(defaultChatModel);
        }
        if ("mock".equals(chatProviderName) || "mock_openai".equals(chatProviderName) || "mock_openai_responses".equals(chatProviderName)) {
            return new MockGatewayChatProvider(defaultChatModel);
        }
        if ("openai".equals(chatProviderName) || "openai_responses".equals(chatProviderName) || "real".equals(chatProviderName)) {
            return new OpenAiResponsesChatProvider(defaultChatModel, openAiStoreEnabled);
        }
        throw new IllegalArgumentException("Unsupported EBB_GATEWAY_CHAT_PROVIDER: " + chatProviderName);
    }

    public AuthProvider createAuthProvider() {
        if ("dev".equals(authProviderName) || "local".equals(authProviderName) || "dev_local".equals(authProviderName)) {
            return new DevLocalAuthProvider(publicBaseUri);
        }
        if ("oidc".equals(authProviderName) || "keycloak".equals(authProviderName)
                || "auth0".equals(authProviderName) || "stytch".equals(authProviderName)) {
            return new OidcAuthProvider(authProviderName, oidcDeviceAuthorizationEndpoint, oidcTokenEndpoint,
                    oidcClientId, oidcClientSecret, oidcScope, httpTimeout);
        }
        throw new IllegalArgumentException("Unsupported EBB_GATEWAY_AUTH_PROVIDER: " + authProviderName);
    }

    private static String value(Map<String, String> env, String key, String fallback) {
        String value = env.get(key);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static URI optionalUri(String value) {
        return value == null || value.isBlank() ? null : URI.create(value.trim());
    }

    private static boolean parseBool(String value, boolean fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        if ("true".equalsIgnoreCase(value) || "1".equals(value.trim()) || "yes".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value) || "0".equals(value.trim()) || "no".equalsIgnoreCase(value)) {
            return false;
        }
        return fallback;
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (RuntimeException ex) {
            return fallback;
        }
    }
}
