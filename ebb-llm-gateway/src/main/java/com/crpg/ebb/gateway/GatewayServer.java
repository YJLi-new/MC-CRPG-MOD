package com.crpg.ebb.gateway;

import com.crpg.ebb.gateway.auth.DeviceAuthService;
import com.crpg.ebb.gateway.chat.GatewayChatProvider;
import com.crpg.ebb.gateway.chat.GatewayChatRequest;
import com.crpg.ebb.gateway.chat.GatewayChatResponse;
import com.crpg.ebb.gateway.chat.SimpleCircuitBreaker;
import com.crpg.ebb.gateway.memory.MemorySearchRequest;
import com.crpg.ebb.gateway.memory.MemoryStore;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class GatewayServer implements AutoCloseable {
    private final GatewayConfig config;
    private final DeviceAuthService authService;
    private final GatewayChatProvider chatProvider;
    private final SimpleCircuitBreaker chatCircuitBreaker;
    private final MemoryStore memoryStore;
    private final HttpServer server;

    private GatewayServer(GatewayConfig config, DeviceAuthService authService, GatewayChatProvider chatProvider, SimpleCircuitBreaker chatCircuitBreaker, MemoryStore memoryStore, HttpServer server) {
        this.config = config;
        this.authService = authService;
        this.chatProvider = chatProvider;
        this.chatCircuitBreaker = chatCircuitBreaker;
        this.memoryStore = memoryStore;
        this.server = server;
    }

    public static GatewayServer create(GatewayConfig config) throws IOException {
        DeviceAuthService authService = new DeviceAuthService(config.createAuthProvider());
        GatewayChatProvider chatProvider = config.createChatProvider();
        SimpleCircuitBreaker chatCircuitBreaker = new SimpleCircuitBreaker(config.circuitFailureThreshold(), config.circuitCooldownMs());
        MemoryStore memoryStore = config.createMemoryStore();
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(config.bindHost(), config.port()), 0);
        GatewayServer gateway = new GatewayServer(config, authService, chatProvider, chatCircuitBreaker, memoryStore, httpServer);
        gateway.registerContexts();
        return gateway;
    }

    public void start() {
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();
    }

    public int port() {
        return server.getAddress().getPort();
    }

    public String authProviderName() {
        return config.authProviderName();
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private void registerContexts() {
        server.createContext("/v1/health", this::handleHealth);
        server.createContext("/v1/auth/device/start", this::handleAuthStart);
        server.createContext("/v1/auth/device/status", this::handleAuthStatus);
        server.createContext("/v1/auth/logout", this::handleLogout);
        server.createContext("/v1/chat/message", this::handleChatMessage);
        server.createContext("/v1/memory/search", this::handleMemorySearch);
        server.createContext("/v1/memory/inspect", this::handleMemoryInspect);
        server.createContext("/v1/memory/conflicts", this::handleMemoryConflicts);
        server.createContext("/v1/memory/episodes", this::handleMemoryEpisodes);
        server.createContext("/v1/memory/lessons", this::handleMemoryLessons);
        server.createContext("/", exchange -> HttpJson.writeJson(exchange, 404, HttpJson.object(Map.of("error", "not_found"))));
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            HttpJson.methodNotAllowed(exchange);
            return;
        }
        HttpJson.writeJson(exchange, 200, authService.healthJson().replace("}", ",\"memory\":" + HttpJson.object(memoryStore.summary()) + "}"));
    }

    private void handleAuthStart(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            HttpJson.methodNotAllowed(exchange);
            return;
        }
        Map<String, String> body = HttpJson.objectStrings(HttpJson.readBody(exchange));
        String minecraftUuid = body.getOrDefault("minecraft_uuid", "");
        String serverId = body.getOrDefault("server_id", "default");
        if (minecraftUuid.isBlank()) {
            HttpJson.writeJson(exchange, 400, HttpJson.object(Map.of("error", "minecraft_uuid_required")));
            return;
        }
        DeviceAuthService.StartResult result = authService.start(minecraftUuid, serverId);
        HttpJson.writeJson(exchange, 200, result.toJson());
    }

    private void handleAuthStatus(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            HttpJson.methodNotAllowed(exchange);
            return;
        }
        String authSessionId = HttpJson.query(exchange.getRequestURI()).getOrDefault("auth_session_id", "");
        if (authSessionId.isBlank()) {
            HttpJson.writeJson(exchange, 400, HttpJson.object(Map.of("error", "auth_session_id_required")));
            return;
        }
        DeviceAuthService.StatusResult result = authService.status(authSessionId);
        HttpJson.writeJson(exchange, "error".equals(result.status()) ? 404 : 200, result.toJson());
    }


    private void handleChatMessage(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            HttpJson.methodNotAllowed(exchange);
            return;
        }
        String body = HttpJson.readBody(exchange);
        GatewayChatRequest request = GatewayChatRequest.fromJson(body, config.defaultChatModel(),
                config.defaultStreaming(), config.defaultStructuredOutput(), config.openAiStoreEnabled(), config.maxOutputTokens());
        String token = request.opaquePlayerToken();
        if (!token.isBlank() && !authService.tokenValid(token)) {
            HttpJson.writeJson(exchange, 401, GatewayChatResponse.error(request, chatProvider.providerName(),
                    request.modelOrDefault(config.defaultChatModel()), "auth_required").toJson());
            return;
        }
        if (!chatCircuitBreaker.allowRequest()) {
            HttpJson.writeJson(exchange, 503, GatewayChatResponse.error(request, chatProvider.providerName(),
                    request.modelOrDefault(config.defaultChatModel()), "llm_circuit_open").toJson());
            return;
        }
        try {
            GatewayChatResponse response = CompletableFuture.supplyAsync(() -> {
                        try {
                            return chatProvider.send(request);
                        } catch (Exception ex) {
                            throw new IllegalStateException(ex);
                        }
                    })
                    .get(config.httpTimeout().toMillis(), TimeUnit.MILLISECONDS);
            chatCircuitBreaker.recordSuccess();
            memoryStore.appendTurn(request, response);
            HttpJson.writeJson(exchange, 200, response.toJson());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            chatCircuitBreaker.recordFailure();
            HttpJson.writeJson(exchange, 502, GatewayChatResponse.error(request, chatProvider.providerName(),
                    request.modelOrDefault(config.defaultChatModel()), "llm_gateway_interrupted").toJson());
        } catch (ExecutionException | TimeoutException ex) {
            chatCircuitBreaker.recordFailure();
            HttpJson.writeJson(exchange, 502, GatewayChatResponse.error(request, chatProvider.providerName(),
                    request.modelOrDefault(config.defaultChatModel()), "llm_gateway_error").toJson());
        }
    }


    private void handleMemorySearch(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            HttpJson.methodNotAllowed(exchange);
            return;
        }
        MemorySearchRequest request = MemorySearchRequest.fromJson(HttpJson.readBody(exchange));
        var matches = memoryStore.search(request).stream().map(match -> match.toJsonMap()).toList();
        var citationIds = matches.stream().map(match -> String.valueOf(match.get("citation_id"))).filter(value -> !value.isBlank()).toList();
        HttpJson.writeJson(exchange, 200, HttpJson.object(Map.of(
                "status", "ok",
                "query", request.query(),
                "matches", matches,
                "citation_ids", citationIds
        )));
    }

    private void handleMemoryInspect(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            HttpJson.methodNotAllowed(exchange);
            return;
        }
        String id = HttpJson.query(exchange.getRequestURI()).getOrDefault("id", "");
        var inspected = memoryStore.inspect(id);
        if (inspected.isEmpty()) {
            HttpJson.writeJson(exchange, 404, HttpJson.object(Map.of("error", "memory_not_found", "id", id)));
            return;
        }
        HttpJson.writeJson(exchange, 200, HttpJson.object(inspected.get()));
    }

    private void handleMemoryConflicts(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            HttpJson.methodNotAllowed(exchange);
            return;
        }
        Map<String, String> query = HttpJson.query(exchange.getRequestURI());
        String serverId = query.getOrDefault("server_id", "local-dev");
        String worldId = query.getOrDefault("world_id", "unknown-world");
        int limit = parseInt(query.getOrDefault("limit", "25"), 25);
        var conflicts = memoryStore.conflicts(serverId, worldId, limit).stream().map(conflict -> conflict.toJsonMap()).toList();
        HttpJson.writeJson(exchange, 200, HttpJson.object(Map.of(
                "status", "ok",
                "conflicts", conflicts,
                "count", conflicts.size()
        )));
    }


    private void handleMemoryEpisodes(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            HttpJson.methodNotAllowed(exchange);
            return;
        }
        Map<String, String> query = HttpJson.query(exchange.getRequestURI());
        String serverId = query.getOrDefault("server_id", "local-dev");
        String worldId = query.getOrDefault("world_id", "unknown-world");
        int limit = parseInt(query.getOrDefault("limit", "25"), 25);
        var episodes = memoryStore.episodes(serverId, worldId, limit).stream().map(record -> record.toJsonMap()).toList();
        HttpJson.writeJson(exchange, 200, HttpJson.object(Map.of(
                "status", "ok",
                "episodes", episodes,
                "count", episodes.size()
        )));
    }

    private void handleMemoryLessons(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            HttpJson.methodNotAllowed(exchange);
            return;
        }
        Map<String, String> query = HttpJson.query(exchange.getRequestURI());
        String serverId = query.getOrDefault("server_id", "local-dev");
        String worldId = query.getOrDefault("world_id", "unknown-world");
        int limit = parseInt(query.getOrDefault("limit", "25"), 25);
        var lessons = memoryStore.safetyLessons(serverId, worldId, limit).stream().map(lesson -> lesson.toJsonMap()).toList();
        HttpJson.writeJson(exchange, 200, HttpJson.object(Map.of(
                "status", "ok",
                "safety_lessons", lessons,
                "count", lessons.size()
        )));
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    private void handleLogout(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            HttpJson.methodNotAllowed(exchange);
            return;
        }
        Map<String, String> body = HttpJson.objectStrings(HttpJson.readBody(exchange));
        String token = body.getOrDefault("opaque_player_token", body.getOrDefault("token", ""));
        boolean revoked = authService.logout(token);
        HttpJson.writeJson(exchange, 200, HttpJson.object(Map.of("status", "ok", "revoked", revoked)));
    }
}
