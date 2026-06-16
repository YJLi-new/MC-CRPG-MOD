package com.crpg.ebb.llm;

import com.crpg.ebb.llm.auth.LlmAuthService;
import com.crpg.ebb.llm.auth.LlmAuthToken;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class HttpLlmGatewayClient implements LlmGatewayClient {
    private final LlmConfig config;
    private final String baseUrl;
    private final HttpClient httpClient;
    private final Duration timeout;

    public HttpLlmGatewayClient(LlmConfig config) {
        this.config = config == null ? LlmConfig.disabled() : config;
        this.baseUrl = trimTrailingSlash(this.config.gatewayUrl());
        this.timeout = Duration.ofMillis(this.config.gatewayTimeoutMs());
        this.httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    @Override
    public CompletableFuture<LlmChatResponse> sendMessage(LlmChatRequest request) {
        if (baseUrl.isBlank()) {
            return CompletableFuture.completedFuture(LlmChatResponse.error("gateway_url_missing"));
        }
        JsonObject body = new JsonObject();
        body.addProperty("server_id", "minecraft-server");
        body.addProperty("world_id", "minecraft-world");
        body.addProperty("minecraft_player_uuid", request.playerUuid().toString());
        body.addProperty("npc_key", request.npcKey());
        body.addProperty("npc_display_name", request.npcDisplayName());
        request.entityUuid().ifPresent(uuid -> body.addProperty("entity_uuid", uuid.toString()));
        body.addProperty("conversation_id", request.conversationId().toString());
        body.addProperty("dialogue_id", request.dialogueId().toString());
        body.addProperty("source_node_id", request.nodeId());
        body.addProperty("topic_hint", request.topicHint());
        body.addProperty("scene_context", sceneContext(request));
        body.addProperty("message", request.playerMessage());
        Optional<LlmAuthToken> token = LlmAuthService.validToken(request.playerUuid());
        token.ifPresent(value -> body.addProperty("opaque_player_token", value.opaqueToken()));
        body.addProperty("model", config.defaultChatModel());
        body.addProperty("stream", config.llmChatStreaming());
        body.addProperty("structured", config.structuredOutput());
        body.addProperty("store", config.openAiStore());
        body.addProperty("max_output_tokens", config.maxOutputChars());

        HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(baseUrl + "/v1/chat/message"))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        return CompletableFuture.supplyAsync(() -> sendAndParse(httpRequest))
                .orTimeout(timeout.toMillis() + 1000L, TimeUnit.MILLISECONDS)
                .exceptionally(error -> LlmChatResponse.error("llm_gateway_error"));
    }

    @Override
    public String providerName() {
        return "gateway_http";
    }

    @Override
    public boolean usesNetwork() {
        return true;
    }

    private LlmChatResponse sendAndParse(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                return LlmChatResponse.error(errorReason(response.body(), "gateway_http_" + response.statusCode()));
            }
            return parseResponse(response.body());
        } catch (IOException ex) {
            return LlmChatResponse.error("gateway_io_error");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return LlmChatResponse.error("gateway_interrupted");
        } catch (RuntimeException ex) {
            return LlmChatResponse.error("bad_gateway_chat_response");
        }
    }

    private LlmChatResponse parseResponse(String json) {
        JsonObject object = JsonParser.parseString(json).getAsJsonObject();
        String error = string(object, "error", "");
        if (!error.isBlank()) {
            return LlmChatResponse.error(error);
        }
        String reply = string(object, "npc_reply", "");
        if (reply.isBlank()) {
            reply = string(object, "reply", "");
        }
        return LlmChatResponse.ok(
                reply,
                stringList(object.get("suggested_options")),
                stringList(object.get("citations")),
                string(object, "status", "gateway_reply")
        );
    }

    private static String errorReason(String json, String fallback) {
        try {
            JsonObject object = JsonParser.parseString(json).getAsJsonObject();
            return string(object, "error", fallback);
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    private static String sceneContext(LlmChatRequest request) {
        String context = "dialogue=" + request.dialogueId()
                + " node=" + request.nodeId()
                + " topic=" + request.topicHint()
                + " game_time=" + request.gameTime();
        if (!request.knowledgeContext().isBlank()) {
            context += "\n" + request.knowledgeContext();
        }
        return context;
    }

    private static List<String> stringList(JsonElement element) {
        if (!(element instanceof JsonArray array)) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonElement item : array) {
            if (item == null || item.isJsonNull()) {
                continue;
            }
            if (item.isJsonPrimitive()) {
                values.add(item.getAsString());
            } else if (item.isJsonObject()) {
                JsonObject object = item.getAsJsonObject();
                String value = string(object, "label", string(object, "message", string(object, "text", "")));
                if (!value.isBlank()) {
                    values.add(value);
                }
            }
        }
        return List.copyOf(values);
    }

    private static String string(JsonObject object, String key, String fallback) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : fallback;
    }

    private static String trimTrailingSlash(String value) {
        String trimmed = value == null ? "" : value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
