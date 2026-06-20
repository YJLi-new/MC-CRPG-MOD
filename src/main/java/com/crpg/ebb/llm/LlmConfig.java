package com.crpg.ebb.llm;

import com.crpg.ebb.EbbMod;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.util.GsonHelper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

public record LlmConfig(
        boolean enabled,
        LlmMode mode,
        String gatewayUrl,
        int gatewayTimeoutMs,
        boolean requirePlayerAuth,
        String defaultChatModel,
        boolean llmChatStreaming,
        boolean structuredOutput,
        boolean openAiStore,
        int maxInputChars,
        int maxOutputChars,
        int sessionTimeoutTicks,
        int rateLimitPerMinute,
        String serverId,
        String worldIdStrategy,
        String worldIdOverride,
        String fakeReply
) {
    public static final Path SERVER_CONFIG_PATH = Path.of("config", "ebb-llm-server.json");
    public static final int DEFAULT_GATEWAY_TIMEOUT_MS = 30000;
    public static final boolean DEFAULT_REQUIRE_PLAYER_AUTH = true;
    public static final String DEFAULT_CHAT_MODEL = "gpt-5.2";
    public static final boolean DEFAULT_LLM_CHAT_STREAMING = true;
    public static final boolean DEFAULT_STRUCTURED_OUTPUT = true;
    public static final boolean DEFAULT_OPENAI_STORE = false;
    public static final int DEFAULT_MAX_INPUT_CHARS = 512;
    public static final int DEFAULT_MAX_OUTPUT_CHARS = 700;
    public static final int DEFAULT_SESSION_TIMEOUT_TICKS = 20 * 60;
    public static final int DEFAULT_RATE_LIMIT_PER_MINUTE = 20;
    public static final String DEFAULT_WORLD_ID_STRATEGY = "level_directory_hash";
    public static final String DEFAULT_FAKE_REPLY = "FAKE_NPC_REPLY: I can hear you, but this is the deterministic fake LLM provider.";
    private static final Gson GSON = new Gson();
    private static volatile LlmConfig overrideForTesting;
    private static volatile LlmConfig cached;

    public LlmConfig {
        mode = mode == null ? LlmMode.DISABLED : mode;
        gatewayUrl = gatewayUrl == null ? "" : gatewayUrl.trim();
        gatewayTimeoutMs = clamp(gatewayTimeoutMs, 1000, 120000);
        defaultChatModel = defaultChatModel == null || defaultChatModel.isBlank() ? DEFAULT_CHAT_MODEL : defaultChatModel.trim();
        maxInputChars = clamp(maxInputChars, 32, 4096);
        maxOutputChars = clamp(maxOutputChars, 64, 4096);
        sessionTimeoutTicks = clamp(sessionTimeoutTicks, 20, 20 * 60 * 30);
        rateLimitPerMinute = clamp(rateLimitPerMinute, 1, 120);
        serverId = serverId == null ? "" : serverId.strip();
        worldIdStrategy = worldIdStrategy == null || worldIdStrategy.isBlank() ? DEFAULT_WORLD_ID_STRATEGY : worldIdStrategy.strip();
        worldIdOverride = worldIdOverride == null ? "" : worldIdOverride.strip();
        fakeReply = fakeReply == null || fakeReply.isBlank() ? DEFAULT_FAKE_REPLY : fakeReply.strip();
        if (!enabled) {
            mode = LlmMode.DISABLED;
        }
        if (mode == LlmMode.GATEWAY && gatewayUrl.isBlank()) {
            EbbMod.LOGGER.warn("Ebb LLM config requested gateway mode without gateway_url; disabling LLM.");
            mode = LlmMode.DISABLED;
        }
    }

    public LlmConfig(boolean enabled, LlmMode mode, String gatewayUrl, int gatewayTimeoutMs, boolean requirePlayerAuth,
                     String defaultChatModel, boolean llmChatStreaming, boolean structuredOutput, boolean openAiStore,
                     int maxInputChars, int maxOutputChars, int sessionTimeoutTicks, int rateLimitPerMinute,
                     String fakeReply) {
        this(enabled, mode, gatewayUrl, gatewayTimeoutMs, requirePlayerAuth, defaultChatModel, llmChatStreaming,
                structuredOutput, openAiStore, maxInputChars, maxOutputChars, sessionTimeoutTicks, rateLimitPerMinute,
                "", DEFAULT_WORLD_ID_STRATEGY, "", fakeReply);
    }

    public static LlmConfig disabled() {
        return new LlmConfig(false, LlmMode.DISABLED, "", DEFAULT_GATEWAY_TIMEOUT_MS, DEFAULT_REQUIRE_PLAYER_AUTH,
                DEFAULT_CHAT_MODEL, DEFAULT_LLM_CHAT_STREAMING, DEFAULT_STRUCTURED_OUTPUT, DEFAULT_OPENAI_STORE,
                DEFAULT_MAX_INPUT_CHARS, DEFAULT_MAX_OUTPUT_CHARS, DEFAULT_SESSION_TIMEOUT_TICKS, DEFAULT_RATE_LIMIT_PER_MINUTE,
                "", DEFAULT_WORLD_ID_STRATEGY, "", DEFAULT_FAKE_REPLY);
    }

    public static LlmConfig fakeForTesting() {
        return new LlmConfig(true, LlmMode.FAKE, "", DEFAULT_GATEWAY_TIMEOUT_MS, false,
                DEFAULT_CHAT_MODEL, DEFAULT_LLM_CHAT_STREAMING, DEFAULT_STRUCTURED_OUTPUT, DEFAULT_OPENAI_STORE,
                DEFAULT_MAX_INPUT_CHARS, DEFAULT_MAX_OUTPUT_CHARS, DEFAULT_SESSION_TIMEOUT_TICKS, DEFAULT_RATE_LIMIT_PER_MINUTE,
                "", DEFAULT_WORLD_ID_STRATEGY, "", DEFAULT_FAKE_REPLY);
    }

    public static LlmConfig current() {
        LlmConfig override = overrideForTesting;
        if (override != null) {
            return override;
        }
        LlmConfig local = cached;
        if (local == null) {
            local = loadFromDisk(SERVER_CONFIG_PATH);
            cached = local;
        }
        return local;
    }

    public static void reload() {
        cached = null;
    }

    public static void setForTesting(LlmConfig config) {
        overrideForTesting = config == null ? disabled() : config;
    }

    public static void clearTestingOverride() {
        overrideForTesting = null;
        cached = null;
    }

    public static LlmConfig loadFromDisk(Path path) {
        if (path == null || !Files.isRegularFile(path)) {
            return disabled();
        }
        try {
            String text = Files.readString(path, StandardCharsets.UTF_8);
            JsonObject json = JsonParser.parseString(text).getAsJsonObject();
            return parse(json);
        } catch (IOException | RuntimeException ex) {
            EbbMod.LOGGER.warn("Failed to read Ebb LLM server config {}; LLM disabled: {}", path, ex.toString());
            return disabled();
        }
    }

    public static LlmConfig parse(JsonObject json) {
        if (json == null) {
            return disabled();
        }
        boolean enabled = GsonHelper.getAsBoolean(json, "enabled", false);
        LlmMode mode = Optional.ofNullable(GsonHelper.getAsString(json, "mode", enabled ? "fake" : "disabled"))
                .flatMap(LlmMode::parse)
                .orElse(enabled ? LlmMode.FAKE : LlmMode.DISABLED);
        String gatewayUrl = GsonHelper.getAsString(json, "gateway_base_url",
                GsonHelper.getAsString(json, "gateway_url", GsonHelper.getAsString(json, "gatewayUrl", "")));
        int gatewayTimeout = GsonHelper.getAsInt(json, "gateway_timeout_ms", DEFAULT_GATEWAY_TIMEOUT_MS);
        boolean requireAuth = GsonHelper.getAsBoolean(json, "require_player_auth", DEFAULT_REQUIRE_PLAYER_AUTH);
        String model = GsonHelper.getAsString(json, "default_chat_model",
                GsonHelper.getAsString(json, "chat_model", GsonHelper.getAsString(json, "model", DEFAULT_CHAT_MODEL)));
        boolean streaming = GsonHelper.getAsBoolean(json, "llm_chat_streaming",
                GsonHelper.getAsBoolean(json, "streaming", DEFAULT_LLM_CHAT_STREAMING));
        boolean structured = GsonHelper.getAsBoolean(json, "structured_output",
                GsonHelper.getAsBoolean(json, "structured_json_output", DEFAULT_STRUCTURED_OUTPUT));
        boolean openAiStore = GsonHelper.getAsBoolean(json, "openai_store",
                GsonHelper.getAsBoolean(json, "store", DEFAULT_OPENAI_STORE));
        int maxInput = GsonHelper.getAsInt(json, "max_input_chars", DEFAULT_MAX_INPUT_CHARS);
        int maxOutput = GsonHelper.getAsInt(json, "max_output_chars", DEFAULT_MAX_OUTPUT_CHARS);
        int timeout = GsonHelper.getAsInt(json, "session_timeout_ticks", DEFAULT_SESSION_TIMEOUT_TICKS);
        int rate = GsonHelper.getAsInt(json, "rate_limit_per_minute", DEFAULT_RATE_LIMIT_PER_MINUTE);
        String serverId = GsonHelper.getAsString(json, "server_id", GsonHelper.getAsString(json, "serverId", ""));
        String worldIdStrategy = GsonHelper.getAsString(json, "world_id_strategy", DEFAULT_WORLD_ID_STRATEGY);
        String worldIdOverride = GsonHelper.getAsString(json, "world_id_override", GsonHelper.getAsString(json, "worldId", ""));
        String fakeReply = GsonHelper.getAsString(json, "fake_reply", DEFAULT_FAKE_REPLY);
        return new LlmConfig(enabled, mode, gatewayUrl, gatewayTimeout, requireAuth, model, streaming, structured, openAiStore,
                maxInput, maxOutput, timeout, rate, serverId, worldIdStrategy, worldIdOverride, fakeReply);
    }

    public boolean active() {
        return enabled && mode != LlmMode.DISABLED;
    }

    public boolean fakeMode() {
        return active() && mode == LlmMode.FAKE;
    }

    public boolean networkAccessAllowed() {
        return active() && mode == LlmMode.GATEWAY;
    }

    public String summary() {
        return String.format(Locale.ROOT,
                "enabled=%s mode=%s provider=%s network=%s auth_required=%s gateway_timeout_ms=%d model=%s streaming=%s structured=%s openai_store=%s max_input=%d timeout_ticks=%d rate_limit_per_minute=%d server_id_configured=%s world_id_strategy=%s active_fake=%s",
                enabled,
                mode.serializedName(),
                mode == LlmMode.FAKE ? "fake" : mode == LlmMode.GATEWAY ? "gateway" : "disabled",
                networkAccessAllowed() ? "gateway_only" : "blocked",
                requirePlayerAuth,
                gatewayTimeoutMs,
                defaultChatModel,
                llmChatStreaming,
                structuredOutput,
                openAiStore,
                maxInputChars,
                sessionTimeoutTicks,
                rateLimitPerMinute,
                !serverId.isBlank(),
                worldIdStrategy,
                fakeMode());
    }

    public JsonObject toSafeJson() {
        JsonObject json = new JsonObject();
        json.addProperty("enabled", enabled);
        json.addProperty("mode", mode.serializedName());
        json.addProperty("gateway_base_url_configured", !gatewayUrl.isBlank());
        json.addProperty("gateway_url_configured", !gatewayUrl.isBlank());
        json.addProperty("gateway_timeout_ms", gatewayTimeoutMs);
        json.addProperty("require_player_auth", requirePlayerAuth);
        json.addProperty("default_chat_model", defaultChatModel);
        json.addProperty("llm_chat_streaming", llmChatStreaming);
        json.addProperty("structured_output", structuredOutput);
        json.addProperty("openai_store", openAiStore);
        json.addProperty("max_input_chars", maxInputChars);
        json.addProperty("max_output_chars", maxOutputChars);
        json.addProperty("session_timeout_ticks", sessionTimeoutTicks);
        json.addProperty("rate_limit_per_minute", rateLimitPerMinute);
        json.addProperty("server_id_configured", !serverId.isBlank());
        json.addProperty("world_id_strategy", worldIdStrategy);
        json.addProperty("world_id_override_configured", !worldIdOverride.isBlank());
        json.addProperty("network_access_allowed", networkAccessAllowed());
        return json;
    }

    @Override
    public String toString() {
        return GSON.toJson(toSafeJson());
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
