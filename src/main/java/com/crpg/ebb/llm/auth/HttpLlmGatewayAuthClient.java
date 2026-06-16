package com.crpg.ebb.llm.auth;

import com.crpg.ebb.llm.LlmConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class HttpLlmGatewayAuthClient implements LlmGatewayAuthClient {
    private final String baseUrl;
    private final HttpClient httpClient;
    private final Duration timeout;

    public HttpLlmGatewayAuthClient(LlmConfig config) {
        this.baseUrl = trimTrailingSlash(config.gatewayUrl());
        this.timeout = Duration.ofMillis(config.gatewayTimeoutMs());
        this.httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    @Override
    public CompletableFuture<DeviceAuthStartResponse> startDeviceAuth(UUID playerUuid, String serverId) {
        JsonObject body = new JsonObject();
        body.addProperty("minecraft_uuid", playerUuid.toString());
        body.addProperty("server_id", serverId == null || serverId.isBlank() ? "minecraft-server" : serverId);
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/v1/auth/device/start"))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        return send(request).thenApply(this::parseStart);
    }

    @Override
    public CompletableFuture<DeviceAuthStatusResponse> pollDeviceAuth(String authSessionId) {
        String encoded = URLEncoder.encode(authSessionId, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/v1/auth/device/status?auth_session_id=" + encoded))
                .timeout(timeout)
                .GET()
                .build();
        return send(request).thenApply(this::parseStatus);
    }

    @Override
    public CompletableFuture<Boolean> logout(LlmAuthToken token) {
        if (token == null || token.opaqueToken().isBlank()) {
            return CompletableFuture.completedFuture(false);
        }
        JsonObject body = new JsonObject();
        body.addProperty("opaque_player_token", token.opaqueToken());
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/v1/auth/logout"))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        return send(request).thenApply(json -> {
            JsonObject object = JsonParser.parseString(json).getAsJsonObject();
            return object.has("revoked") && object.get("revoked").getAsBoolean();
        }).exceptionally(error -> false);
    }

    @Override
    public String providerName() {
        return "gateway_auth";
    }

    @Override
    public boolean usesNetwork() {
        return true;
    }

    private CompletableFuture<String> send(HttpRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() / 100 != 2) {
                    throw new IllegalStateException("gateway_http_" + response.statusCode());
                }
                return response.body();
            } catch (IOException ex) {
                throw new IllegalStateException("gateway_io_error", ex);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("gateway_interrupted", ex);
            }
        });
    }

    private DeviceAuthStartResponse parseStart(String json) {
        try {
            JsonObject object = JsonParser.parseString(json).getAsJsonObject();
            return new DeviceAuthStartResponse(
                    true,
                    string(object, "auth_session_id"),
                    string(object, "verification_url"),
                    string(object, "user_code"),
                    longValue(object, "expires_in_seconds", 600L),
                    longValue(object, "interval_seconds", 2L),
                    string(object, "provider", providerName()),
                    ""
            );
        } catch (RuntimeException ex) {
            return DeviceAuthStartResponse.error("bad_gateway_auth_start_response");
        }
    }

    private DeviceAuthStatusResponse parseStatus(String json) {
        try {
            JsonObject object = JsonParser.parseString(json).getAsJsonObject();
            String status = string(object, "status", "error");
            if ("authenticated".equals(status)) {
                LlmAuthToken token = new LlmAuthToken(
                        string(object, "opaque_player_token"),
                        scopes(object.get("scopes")),
                        longValue(object, "expires_at_epoch_seconds", 0L),
                        providerName()
                );
                return DeviceAuthStatusResponse.authenticated(token);
            }
            if ("pending".equals(status)) {
                return DeviceAuthStatusResponse.pending(longValue(object, "interval_seconds", 2L));
            }
            return DeviceAuthStatusResponse.error(string(object, "error", "gateway_auth_error"));
        } catch (RuntimeException ex) {
            return DeviceAuthStatusResponse.error("bad_gateway_auth_status_response");
        }
    }

    private static List<String> scopes(JsonElement element) {
        if (!(element instanceof JsonArray array)) {
            return List.of("llm:chat");
        }
        List<String> values = new ArrayList<>();
        for (JsonElement value : array) {
            if (value.isJsonPrimitive()) {
                values.add(value.getAsString());
            }
        }
        return values.isEmpty() ? List.of("llm:chat") : values;
    }

    private static String string(JsonObject object, String key) {
        return string(object, key, "");
    }

    private static String string(JsonObject object, String key, String fallback) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : fallback;
    }

    private static long longValue(JsonObject object, String key, long fallback) {
        try {
            return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsLong() : fallback;
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    private static String trimTrailingSlash(String value) {
        String trimmed = value == null ? "" : value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
