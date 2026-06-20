package com.crpg.ebb.gateway;

import com.crpg.ebb.gateway.auth.AuthProvider;
import com.crpg.ebb.gateway.auth.CodexCliAuthProvider;
import com.crpg.ebb.gateway.auth.DevLocalAuthProvider;
import com.crpg.ebb.gateway.auth.OidcAuthProvider;
import com.crpg.ebb.gateway.chat.FakeGatewayChatProvider;
import com.crpg.ebb.gateway.chat.GatewayChatProvider;
import com.crpg.ebb.gateway.chat.MockGatewayChatProvider;
import com.crpg.ebb.gateway.chat.OpenAiResponsesChatProvider;
import com.crpg.ebb.gateway.memory.MemoryStore;

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
        String codexCliCommand,
        String codexHome,
        int codexDeviceStartTimeoutSeconds,
        Duration httpTimeout,
        String chatProviderName,
        String defaultChatModel,
        boolean defaultStreaming,
        boolean defaultStructuredOutput,
        boolean openAiStoreEnabled,
        int maxOutputTokens,
        int circuitFailureThreshold,
        long circuitCooldownMs,
        String memoryDbUrl,
        boolean requirePlayerTokenForChat,
        boolean requireServerTokenForAdminEndpoints,
        String serverSharedSecret,
        boolean allowBlankTokenOnlyLocalDev,
        int playerRateLimitPerMinute,
        int globalRateLimitPerMinute,
        int playerDailyLimit,
        int memoryPromptTopK
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
        String codexCommand = value(env, "EBB_CODEX_CLI_COMMAND", CodexCliAuthProvider.DEFAULT_COMMAND);
        String codexHome = value(env, "EBB_CODEX_HOME", "./ebb-llm-gateway-data/codex-auth");
        int codexStartTimeout = parseInt(value(env, "EBB_CODEX_DEVICE_START_TIMEOUT_SECONDS", "45"), 45);
        int timeoutMs = parseInt(value(env, "EBB_GATEWAY_HTTP_TIMEOUT_MS", "30000"), 30000);
        String chatProvider = value(env, "EBB_GATEWAY_CHAT_PROVIDER", "fake").toLowerCase(Locale.ROOT);
        String defaultModel = value(env, "EBB_OPENAI_MODEL", value(env, "EBB_DEFAULT_CHAT_MODEL", "gpt-5.2"));
        boolean streaming = parseBool(value(env, "EBB_LLM_CHAT_STREAMING", "true"), true);
        boolean structured = parseBool(value(env, "EBB_LLM_STRUCTURED_OUTPUT", "true"), true);
        boolean store = parseBool(value(env, "EBB_OPENAI_STORE", "false"), false);
        int maxOutput = parseInt(value(env, "EBB_OPENAI_MAX_OUTPUT_TOKENS", "700"), 700);
        int circuitThreshold = parseInt(value(env, "EBB_GATEWAY_CIRCUIT_FAILURE_THRESHOLD", "3"), 3);
        long circuitCooldown = parseInt(value(env, "EBB_GATEWAY_CIRCUIT_COOLDOWN_MS", "30000"), 30000);
        String memoryDbUrl = value(env, "EBB_MEMORY_DB_URL", "jdbc:h2:./ebb-llm-gateway-data/memory;AUTO_SERVER=TRUE");
        boolean requirePlayerToken = parseBool(value(env, "EBB_GATEWAY_REQUIRE_PLAYER_TOKEN_FOR_CHAT", "true"), true);
        boolean requireServerToken = parseBool(value(env, "EBB_GATEWAY_REQUIRE_SERVER_TOKEN_FOR_ADMIN_ENDPOINTS",
                provider.equals("dev_local") && isLoopback(bindHost) ? "false" : "true"), !(provider.equals("dev_local") && isLoopback(bindHost)));
        String serverSecret = value(env, "EBB_GATEWAY_SERVER_SHARED_SECRET",
                value(env, "EBB_GATEWAY_SERVER_TOKEN", value(env, "EBB_SERVER_SHARED_SECRET", "")));
        boolean allowBlankLocalDev = parseBool(value(env, "EBB_GATEWAY_ALLOW_BLANK_TOKEN_ONLY_LOCAL_DEV", "true"), true);
        int playerRateLimit = parseInt(value(env, "EBB_GATEWAY_PLAYER_RATE_LIMIT_PER_MINUTE", "20"), 20);
        int globalRateLimit = parseInt(value(env, "EBB_GATEWAY_GLOBAL_RATE_LIMIT_PER_MINUTE", "240"), 240);
        int playerDailyLimit = parseInt(value(env, "EBB_GATEWAY_PLAYER_DAILY_LIMIT", "500"), 500);
        int memoryPromptTopK = parseInt(value(env, "EBB_GATEWAY_MEMORY_PROMPT_TOP_K", "6"), 6);
        return new GatewayConfig(bindHost, port, provider, publicBase, deviceEndpoint, tokenEndpoint,
                clientId, clientSecret, scope,
                codexCommand, codexHome, Math.max(5, codexStartTimeout),
                Duration.ofMillis(Math.max(1000, timeoutMs)), chatProvider, defaultModel,
                streaming, structured, store, Math.max(64, maxOutput), Math.max(1, circuitThreshold), Math.max(1000L, circuitCooldown), memoryDbUrl,
                requirePlayerToken, requireServerToken, serverSecret, allowBlankLocalDev,
                Math.max(1, playerRateLimit), Math.max(1, globalRateLimit), Math.max(1, playerDailyLimit),
                Math.max(0, Math.min(12, memoryPromptTopK)));
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

    public MemoryStore createMemoryStore() {
        return new MemoryStore(memoryDbUrl);
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
        if ("openai_codex".equals(authProviderName) || "codex".equals(authProviderName)
                || "codex_cli".equals(authProviderName) || "chatgpt_codex".equals(authProviderName)) {
            return new CodexCliAuthProvider(codexCliCommand, java.nio.file.Path.of(codexHome),
                    Duration.ofSeconds(Math.max(5, codexDeviceStartTimeoutSeconds)));
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

    public boolean localDevBlankTokenAllowed() {
        return allowBlankTokenOnlyLocalDev
                && ("dev".equals(authProviderName) || "local".equals(authProviderName) || "dev_local".equals(authProviderName))
                && isLoopback(bindHost);
    }

    private static boolean isLoopback(String host) {
        String value = host == null ? "" : host.trim();
        return value.equals("127.0.0.1") || value.equals("localhost") || value.equals("::1");
    }
}
