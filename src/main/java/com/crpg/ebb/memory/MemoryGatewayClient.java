package com.crpg.ebb.memory;

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
import java.util.concurrent.TimeUnit;

public final class MemoryGatewayClient {
    private final LlmConfig config;
    private final HttpClient httpClient;
    private final String baseUrl;
    private final Duration timeout;

    public MemoryGatewayClient(LlmConfig config) {
        this.config = config == null ? LlmConfig.disabled() : config;
        this.baseUrl = trimTrailingSlash(this.config.gatewayUrl());
        this.timeout = Duration.ofMillis(this.config.gatewayTimeoutMs());
        this.httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    public CompletableFuture<MemorySearchResult> search(UUID playerUuid, String query, int limit) {
        if (baseUrl.isBlank()) {
            return CompletableFuture.completedFuture(MemorySearchResult.error("gateway_url_missing"));
        }
        JsonObject body = new JsonObject();
        body.addProperty("server_id", "minecraft-server");
        body.addProperty("world_id", "minecraft-world");
        if (playerUuid != null) {
            body.addProperty("minecraft_player_uuid", playerUuid.toString());
        }
        body.addProperty("query", query == null ? "" : query);
        body.addProperty("limit", Math.max(1, Math.min(25, limit)));
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/v1/memory/search"))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        return CompletableFuture.supplyAsync(() -> send(request))
                .orTimeout(timeout.toMillis() + 1000L, TimeUnit.MILLISECONDS)
                .thenApply(MemoryGatewayClient::parseSearch)
                .exceptionally(error -> MemorySearchResult.error("memory_gateway_error"));
    }

    public CompletableFuture<String> inspect(String id) {
        if (baseUrl.isBlank()) {
            return CompletableFuture.completedFuture("gateway_url_missing");
        }
        String encoded = URLEncoder.encode(id == null ? "" : id, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/v1/memory/inspect?id=" + encoded))
                .timeout(timeout)
                .GET()
                .build();
        return CompletableFuture.supplyAsync(() -> send(request))
                .orTimeout(timeout.toMillis() + 1000L, TimeUnit.MILLISECONDS)
                .exceptionally(error -> "memory_gateway_error");
    }

    public CompletableFuture<String> conflicts(int limit) {
        if (baseUrl.isBlank()) {
            return CompletableFuture.completedFuture("gateway_url_missing");
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/v1/memory/conflicts?server_id=minecraft-server&world_id=minecraft-world&limit=" + Math.max(1, limit)))
                .timeout(timeout)
                .GET()
                .build();
        return CompletableFuture.supplyAsync(() -> send(request))
                .orTimeout(timeout.toMillis() + 1000L, TimeUnit.MILLISECONDS)
                .exceptionally(error -> "memory_gateway_error");
    }

    private String send(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                return "{\"error\":\"memory_gateway_http_" + response.statusCode() + "\"}";
            }
            return response.body();
        } catch (IOException ex) {
            return "{\"error\":\"memory_gateway_io_error\"}";
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return "{\"error\":\"memory_gateway_interrupted\"}";
        }
    }

    private static MemorySearchResult parseSearch(String json) {
        try {
            JsonObject object = JsonParser.parseString(json).getAsJsonObject();
            if (object.has("error")) {
                return MemorySearchResult.error(object.get("error").getAsString());
            }
            List<String> lines = new ArrayList<>();
            JsonArray matches = object.getAsJsonArray("matches");
            if (matches != null) {
                for (JsonElement element : matches) {
                    if (!element.isJsonObject()) {
                        continue;
                    }
                    JsonObject match = element.getAsJsonObject();
                    lines.add(string(match, "citation_id") + " score=" + string(match, "score") + " role=" + string(match, "role") + " text=" + abbreviate(string(match, "text"), 96));
                }
            }
            List<String> citations = new ArrayList<>();
            JsonArray citationArray = object.getAsJsonArray("citation_ids");
            if (citationArray != null) {
                for (JsonElement citation : citationArray) {
                    if (citation.isJsonPrimitive()) {
                        citations.add(citation.getAsString());
                    }
                }
            }
            return new MemorySearchResult(true, "ok", List.copyOf(lines), List.copyOf(citations));
        } catch (RuntimeException ex) {
            return MemorySearchResult.error("bad_memory_gateway_response");
        }
    }

    private static String string(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        return object.get(key).getAsString();
    }

    private static String abbreviate(String value, int max) {
        String safe = value == null ? "" : value.replace('\n', ' ').strip();
        return safe.length() <= max ? safe : safe.substring(0, Math.max(0, max - 1)) + "…";
    }

    private static String trimTrailingSlash(String value) {
        String trimmed = value == null ? "" : value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    public record MemorySearchResult(boolean ok, String status, List<String> lines, List<String> citationIds) {
        public MemorySearchResult {
            status = status == null ? "" : status;
            lines = lines == null ? List.of() : List.copyOf(lines);
            citationIds = citationIds == null ? List.of() : List.copyOf(citationIds);
        }

        public static MemorySearchResult error(String reason) {
            return new MemorySearchResult(false, reason == null ? "memory_gateway_error" : reason, List.of(), List.of());
        }
    }
}
