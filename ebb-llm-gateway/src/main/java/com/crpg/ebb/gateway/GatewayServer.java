package com.crpg.ebb.gateway;

import com.crpg.ebb.gateway.auth.DeviceAuthService;
import com.crpg.ebb.gateway.chat.GatewayChatProvider;
import com.crpg.ebb.gateway.chat.GatewayChatRequest;
import com.crpg.ebb.gateway.chat.GatewayChatResponse;
import com.crpg.ebb.gateway.chat.SimpleCircuitBreaker;
import com.crpg.ebb.gateway.memory.MemoryPromptRenderer;
import com.crpg.ebb.gateway.memory.MemoryRecall;
import com.crpg.ebb.gateway.memory.MemorySearchRequest;
import com.crpg.ebb.gateway.memory.MemoryStore;
import com.crpg.ebb.gateway.memory.QuotaDecision;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class GatewayServer implements AutoCloseable {
    private final GatewayConfig config;
    private final DeviceAuthService authService;
    private final GatewayAuthGuard authGuard;
    private final GatewayChatProvider chatProvider;
    private final SimpleCircuitBreaker chatCircuitBreaker;
    private final GatewayRateLimiter rateLimiter = new GatewayRateLimiter();
    private final MemoryStore memoryStore;
    private final Map<String, Map<String, Object>> ensuredProfiles = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> chatSessions = new ConcurrentHashMap<>();
    private final Map<String, List<Map<String, Object>>> knowledgeUpdates = new ConcurrentHashMap<>();
    private final HttpServer server;

    private GatewayServer(GatewayConfig config, DeviceAuthService authService, GatewayChatProvider chatProvider, SimpleCircuitBreaker chatCircuitBreaker, MemoryStore memoryStore, HttpServer server) {
        this.config = config;
        this.authService = authService;
        this.authGuard = new GatewayAuthGuard(config, authService);
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
        server.createContext("/v1/player/quota", this::handlePlayerQuota);
        server.createContext("/v1/npc/profile/ensure", this::handleNpcProfileEnsure);
        server.createContext("/v1/npc/profile/generate", this::handleNpcProfileGenerate);
        server.createContext("/v1/npc/profile/regenerate", this::handleNpcProfileRegenerate);
        server.createContext("/v1/npc/profile", this::handleNpcProfileGet);
        server.createContext("/v1/chat/start", this::handleChatStart);
        server.createContext("/v1/chat/message", this::handleChatMessage);
        server.createContext("/v1/chat/cancel", this::handleChatCancel);
        server.createContext("/v1/chat/session", this::handleChatSession);
        server.createContext("/v1/memory/ingest", this::handleMemoryIngest);
        server.createContext("/v1/memory/search", this::handleMemorySearch);
        server.createContext("/v1/memory/inspect", this::handleMemoryInspect);
        server.createContext("/v1/memory/conflicts", this::handleMemoryConflicts);
        server.createContext("/v1/memory/episodes", this::handleMemoryEpisodes);
        server.createContext("/v1/memory/lessons", this::handleMemoryLessons);
        server.createContext("/v1/memory/correct", this::handleMemoryCorrect);
        server.createContext("/v1/memory/player", this::handleMemoryDeletePlayer);
        server.createContext("/v1/knowledge/update", this::handleKnowledgeUpdate);
        server.createContext("/v1/knowledge/npc", this::handleKnowledgeNpc);
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
        AuthDecision auth = authorizePlayer(exchange, request.opaquePlayerToken(), request.minecraftPlayerUuid(), "llm:chat", config.requirePlayerTokenForChat());
        if (!auth.allowed()) {
            HttpJson.writeJson(exchange, authHttpStatus(auth), GatewayChatResponse.error(request, chatProvider.providerName(),
                    request.modelOrDefault(config.defaultChatModel()), auth.reason()).toJson());
            return;
        }
        GatewayRateLimiter.RateDecision globalRate = rateLimiter.allow("global:chat", config.globalRateLimitPerMinute());
        QuotaDecision playerQuota = memoryStore.reserveQuota(quotaKey(request), config.playerRateLimitPerMinute(), config.playerDailyLimit());
        if (!globalRate.allowed() || !playerQuota.allowed()) {
            HttpJson.writeJson(exchange, 429, GatewayChatResponse.error(request, chatProvider.providerName(),
                    request.modelOrDefault(config.defaultChatModel()), "rate_limited").toJson());
            return;
        }
        if (!chatCircuitBreaker.allowRequest()) {
            HttpJson.writeJson(exchange, 503, GatewayChatResponse.error(request, chatProvider.providerName(),
                    request.modelOrDefault(config.defaultChatModel()), "llm_circuit_open").toJson());
            return;
        }
        try {
            MemoryRecall recall = config.memoryPromptTopK() <= 0
                    ? new MemoryRecall(List.of(), List.of(), List.of(), List.of())
                    : memoryStore.recall(MemorySearchRequest.forChat(request, config.memoryPromptTopK()));
            List<String> memoryCitations = MemoryPromptRenderer.citationIds(recall);
            GatewayChatRequest enrichedRequest = request.withMemoryContext(MemoryPromptRenderer.render(recall), memoryCitations);
            GatewayChatResponse response = CompletableFuture.supplyAsync(() -> {
                        try {
                            return chatProvider.send(enrichedRequest).withValidatedMemoryCitations(memoryCitations);
                        } catch (Exception ex) {
                            throw new IllegalStateException(ex);
                        }
                    })
                    .get(config.httpTimeout().toMillis(), TimeUnit.MILLISECONDS);
            chatCircuitBreaker.recordSuccess();
            memoryStore.appendTurn(enrichedRequest, response);
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

    private void handlePlayerQuota(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            HttpJson.methodNotAllowed(exchange);
            return;
        }
        Map<String, String> query = HttpJson.query(exchange.getRequestURI());
        String token = query.getOrDefault("opaque_player_token", "");
        AuthDecision auth = authorizePlayer(exchange, token, query.getOrDefault("minecraft_player_uuid", ""), "memory:read_self", config.requirePlayerTokenForChat());
        boolean authenticated = auth.allowed();
        HttpJson.writeJson(exchange, authenticated ? 200 : authHttpStatus(auth), HttpJson.object(Map.of(
                "status", authenticated ? "ok" : auth.reason(),
                "provider", chatProvider.providerName(),
                "chat_model", config.defaultChatModel(),
                "max_output_tokens", config.maxOutputTokens(),
                "circuit_open", !chatCircuitBreaker.allowRequest(),
                "rate_limit_per_minute", config.playerRateLimitPerMinute(),
                "global_rate_limit_per_minute", config.globalRateLimitPerMinute(),
                "quota", memoryStore.quota(quotaKey(
                        query.getOrDefault("server_id", "quota"),
                        query.getOrDefault("world_id", "unknown-world"),
                        query.getOrDefault("minecraft_player_uuid", "")),
                        config.playerRateLimitPerMinute(), config.playerDailyLimit()).toJsonMap(),
                "rate_limit_snapshot", rateLimiter.snapshot(),
                "scopes", List.of("llm:chat", "memory:read_self", "memory:write_self", "memory:delete_self")
        )));
    }

    private void handleNpcProfileEnsure(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            HttpJson.methodNotAllowed(exchange);
            return;
        }
        Map<String, String> body = HttpJson.objectStrings(HttpJson.readBody(exchange));
        AuthDecision auth = authorizeServer(exchange, body);
        if (!auth.allowed()) {
            HttpJson.writeJson(exchange, authHttpStatus(auth), HttpJson.object(Map.of("error", auth.reason())));
            return;
        }
        Map<String, Object> profile = profileFromBody(body, false);
        ensuredProfiles.put(profileKey(String.valueOf(profile.get("world_id")), String.valueOf(profile.get("npc_key"))), profile);
        memoryStore.saveNpcProfile(profile);
        HttpJson.writeJson(exchange, 200, HttpJson.object(Map.of(
                "status", "ok",
                "promoted_major", "major_promoted".equals(profile.get("tier")),
                "profile", profile
        )));
    }

    private void handleNpcProfileGenerate(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            HttpJson.methodNotAllowed(exchange);
            return;
        }
        Map<String, String> body = HttpJson.objectStrings(HttpJson.readBody(exchange));
        AuthDecision auth = authorizeServer(exchange, body);
        if (!auth.allowed()) {
            HttpJson.writeJson(exchange, authHttpStatus(auth), HttpJson.object(Map.of("error", auth.reason())));
            return;
        }
        Map<String, Object> profile = profileFromBody(body, true);
        profile.put("provider", chatProvider.providerName());
        profile.put("generation_endpoint", "/v1/npc/profile/generate");
        profile.put("character", value(body, "character",
                "A promoted minor NPC generated from visible local context only."));
        profile.put("stance", value(body, "stance", "cautious_helpful"));
        profile.put("knowledge_seed", List.of(
                "visible_public_lore:" + value(body, "visible_public_lore", "tavern rumors"),
                "location:" + value(body, "location", "unknown_location")
        ));
        profile.put("suggested_options", List.of("你在这里看见了什么？", "你认识旅馆老板吗？", "先说你确定知道的。"));
        profile.put("speech_rules", List.of("stay in-world", "do not reveal hidden KB", "avoid quest/effect claims"));
        profile.put("safety_flags", profileLeaksForbiddenSecret(profile, body.getOrDefault("forbidden_secret_list", ""))
                ? List.of("unsafe_profile_rejected_hidden_kb_leak", "deterministic_fallback_used")
                : List.of("validated_no_hidden_kb_leak"));
        profile.put("validated", !profile.toString().contains("unsafe_profile_rejected_hidden_kb_leak"));
        ensuredProfiles.put(profileKey(String.valueOf(profile.get("world_id")), String.valueOf(profile.get("npc_key"))), profile);
        memoryStore.saveNpcProfile(profile);
        HttpJson.writeJson(exchange, 200, HttpJson.object(Map.of(
                "status", "ok",
                "generated", true,
                "deterministic_fallback_available", true,
                "profile", profile
        )));
    }

    private void handleNpcProfileRegenerate(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            HttpJson.methodNotAllowed(exchange);
            return;
        }
        Map<String, String> body = HttpJson.objectStrings(HttpJson.readBody(exchange));
        AuthDecision auth = authorizeServer(exchange, body);
        if (!auth.allowed()) {
            HttpJson.writeJson(exchange, authHttpStatus(auth), HttpJson.object(Map.of("error", auth.reason())));
            return;
        }
        Map<String, Object> profile = profileFromBody(body, true);
        ensuredProfiles.put(profileKey(String.valueOf(profile.get("world_id")), String.valueOf(profile.get("npc_key"))), profile);
        memoryStore.saveNpcProfile(profile);
        HttpJson.writeJson(exchange, 200, HttpJson.object(Map.of("status", "ok", "regenerated", true, "profile", profile)));
    }

    private void handleNpcProfileGet(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            HttpJson.methodNotAllowed(exchange);
            return;
        }
        AuthDecision auth = authorizeServer(exchange, HttpJson.query(exchange.getRequestURI()));
        if (!auth.allowed()) {
            HttpJson.writeJson(exchange, authHttpStatus(auth), HttpJson.object(Map.of("error", auth.reason())));
            return;
        }
        PathParts path = pathParts(exchange, "/v1/npc/profile/");
        if (path.parts().size() < 2) {
            HttpJson.writeJson(exchange, 400, HttpJson.object(Map.of("error", "world_id_and_npc_key_required")));
            return;
        }
        String worldId = path.parts().get(0);
        String npcKey = String.join("/", path.parts().subList(1, path.parts().size()));
        Map<String, Object> profile = memoryStore.npcProfile(worldId, npcKey)
                .orElseGet(() -> ensuredProfiles.get(profileKey(worldId, npcKey)));
        if (profile == null) {
            HttpJson.writeJson(exchange, 404, HttpJson.object(Map.of("error", "npc_profile_not_found", "world_id", worldId, "npc_key", npcKey)));
            return;
        }
        HttpJson.writeJson(exchange, 200, HttpJson.object(Map.of("status", "ok", "profile", profile)));
    }

    private void handleChatStart(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            HttpJson.methodNotAllowed(exchange);
            return;
        }
        Map<String, String> body = HttpJson.objectStrings(HttpJson.readBody(exchange));
        AuthDecision auth = authorizePlayerOrServer(exchange, body.getOrDefault("opaque_player_token", ""), value(body, "minecraft_player_uuid", ""), "llm:chat", body);
        if (!auth.allowed()) {
            HttpJson.writeJson(exchange, authHttpStatus(auth), HttpJson.object(Map.of("error", auth.reason())));
            return;
        }
        String conversationId = value(body, "conversation_id", "gateway-chat-" + UUID.randomUUID());
        Map<String, Object> session = new LinkedHashMap<>();
        session.put("conversation_id", conversationId);
        session.put("server_id", value(body, "server_id", "local-dev"));
        session.put("world_id", value(body, "world_id", "unknown-world"));
        session.put("minecraft_player_uuid", value(body, "minecraft_player_uuid", ""));
        session.put("npc_key", value(body, "npc_key", ""));
        session.put("status", "open");
        session.put("started_at_epoch_ms", System.currentTimeMillis());
        chatSessions.put(conversationId, session);
        memoryStore.saveChatSession(session);
        HttpJson.writeJson(exchange, 200, HttpJson.object(Map.of("status", "ok", "session", session)));
    }

    private void handleChatCancel(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            HttpJson.methodNotAllowed(exchange);
            return;
        }
        Map<String, String> body = HttpJson.objectStrings(HttpJson.readBody(exchange));
        AuthDecision auth = authorizePlayerOrServer(exchange, body.getOrDefault("opaque_player_token", ""), "", "llm:chat", body);
        if (!auth.allowed()) {
            HttpJson.writeJson(exchange, authHttpStatus(auth), HttpJson.object(Map.of("error", auth.reason())));
            return;
        }
        String conversationId = value(body, "conversation_id", "");
        Map<String, Object> session = chatSessions.get(conversationId);
        String reason = value(body, "reason", "client_cancelled");
        boolean persisted = memoryStore.updateChatSessionStatus(conversationId, "cancelled", reason);
        if (session != null) {
            session.put("status", "cancelled");
            session.put("cancel_reason", reason);
            memoryStore.saveChatSession(session);
        }
        HttpJson.writeJson(exchange, 200, HttpJson.object(Map.of(
                "status", "ok",
                "cancelled", session != null || persisted,
                "conversation_id", conversationId
        )));
    }

    private void handleChatSession(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            HttpJson.methodNotAllowed(exchange);
            return;
        }
        PathParts path = pathParts(exchange, "/v1/chat/session/");
        String conversationId = path.parts().isEmpty()
                ? HttpJson.query(exchange.getRequestURI()).getOrDefault("session_id", "")
                : path.parts().get(0);
        AuthDecision auth = authorizeServer(exchange, HttpJson.query(exchange.getRequestURI()));
        if (!auth.allowed()) {
            HttpJson.writeJson(exchange, authHttpStatus(auth), HttpJson.object(Map.of("error", auth.reason())));
            return;
        }
        Map<String, Object> session = memoryStore.chatSession(conversationId).orElseGet(() -> chatSessions.get(conversationId));
        if (session == null) {
            HttpJson.writeJson(exchange, 404, HttpJson.object(Map.of("error", "chat_session_not_found", "conversation_id", conversationId)));
            return;
        }
        HttpJson.writeJson(exchange, 200, HttpJson.object(Map.of("status", "ok", "session", session)));
    }

    private void handleMemoryIngest(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            HttpJson.methodNotAllowed(exchange);
            return;
        }
        Map<String, String> body = HttpJson.objectStrings(HttpJson.readBody(exchange));
        AuthDecision auth = authorizeServer(exchange, body);
        if (!auth.allowed()) {
            HttpJson.writeJson(exchange, authHttpStatus(auth), HttpJson.object(Map.of("error", auth.reason())));
            return;
        }
        HttpJson.writeJson(exchange, 200, HttpJson.object(Map.of(
                "status", "ok",
                "accepted", true,
                "note", "Use /v1/chat/message for append-only raw turn ingestion; direct ingest request was audited.",
                "server_id", value(body, "server_id", "local-dev"),
                "world_id", value(body, "world_id", "unknown-world")
        )));
    }

    private void handleMemoryCorrect(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            HttpJson.methodNotAllowed(exchange);
            return;
        }
        Map<String, String> body = HttpJson.objectStrings(HttpJson.readBody(exchange));
        AuthDecision auth = authorizeServer(exchange, body);
        if (!auth.allowed()) {
            HttpJson.writeJson(exchange, authHttpStatus(auth), HttpJson.object(Map.of("error", auth.reason())));
            return;
        }
        String factId = value(body, "fact_id", value(body, "id", ""));
        String newValue = value(body, "new_value", value(body, "value", ""));
        Map<String, Object> result = memoryStore.correctFact(factId, newValue, value(body, "reason", "manual_correction"));
        HttpJson.writeJson(exchange, Boolean.TRUE.equals(result.get("accepted")) ? 200 : 404, HttpJson.object(result));
    }

    private void handleMemoryDeletePlayer(HttpExchange exchange) throws IOException {
        if (!"DELETE".equals(exchange.getRequestMethod())) {
            HttpJson.methodNotAllowed(exchange);
            return;
        }
        PathParts path = pathParts(exchange, "/v1/memory/player/");
        String playerUuid = path.parts().isEmpty()
                ? HttpJson.query(exchange.getRequestURI()).getOrDefault("player_uuid", "")
                : path.parts().get(0);
        AuthDecision auth = authorizePlayerOrServer(exchange,
                HttpJson.query(exchange.getRequestURI()).getOrDefault("opaque_player_token", ""),
                playerUuid, "memory:delete_self", HttpJson.query(exchange.getRequestURI()));
        if (!auth.allowed()) {
            HttpJson.writeJson(exchange, authHttpStatus(auth), HttpJson.object(Map.of("error", auth.reason())));
            return;
        }
        Map<String, Object> result = memoryStore.deletePlayer(playerUuid);
        HttpJson.writeJson(exchange, 200, HttpJson.object(result));
    }

    private void handleKnowledgeUpdate(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            HttpJson.methodNotAllowed(exchange);
            return;
        }
        Map<String, String> body = HttpJson.objectStrings(HttpJson.readBody(exchange));
        AuthDecision auth = authorizeServer(exchange, body);
        if (!auth.allowed()) {
            HttpJson.writeJson(exchange, authHttpStatus(auth), HttpJson.object(Map.of("error", auth.reason())));
            return;
        }
        String npcKey = value(body, "npc_key", value(body, "npc", "unknown_npc"));
        Map<String, Object> update = new LinkedHashMap<>();
        update.put("npc_key", npcKey);
        update.put("pack_id", value(body, "pack_id", value(body, "pack", "")));
        update.put("fact", value(body, "fact", ""));
        update.put("reason", value(body, "reason", "gateway_knowledge_update"));
        update.put("created_at_epoch_ms", System.currentTimeMillis());
        knowledgeUpdates.computeIfAbsent(npcKey, ignored -> new ArrayList<>()).add(update);
        memoryStore.addKnowledgeUpdate(npcKey, update);
        HttpJson.writeJson(exchange, 200, HttpJson.object(Map.of("status", "ok", "accepted", true, "update", update)));
    }

    private void handleKnowledgeNpc(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            HttpJson.methodNotAllowed(exchange);
            return;
        }
        PathParts path = pathParts(exchange, "/v1/knowledge/npc/");
        String npcKey = path.parts().isEmpty()
                ? HttpJson.query(exchange.getRequestURI()).getOrDefault("npc_key", "")
                : String.join("/", path.parts());
        AuthDecision auth = authorizeServer(exchange, HttpJson.query(exchange.getRequestURI()));
        if (!auth.allowed()) {
            HttpJson.writeJson(exchange, authHttpStatus(auth), HttpJson.object(Map.of("error", auth.reason())));
            return;
        }
        List<Map<String, Object>> persistedUpdates = memoryStore.knowledgeUpdates(npcKey);
        List<Map<String, Object>> updates = persistedUpdates.isEmpty() ? knowledgeUpdates.getOrDefault(npcKey, List.of()) : persistedUpdates;
        HttpJson.writeJson(exchange, 200, HttpJson.object(Map.of(
                "status", "ok",
                "npc_key", npcKey,
                "updates", updates,
                "count", updates.size()
        )));
    }

    private void handleMemorySearch(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            HttpJson.methodNotAllowed(exchange);
            return;
        }
        String body = HttpJson.readBody(exchange);
        MemorySearchRequest request = MemorySearchRequest.fromJson(body);
        Map<String, String> bodyValues = HttpJson.objectStrings(body);
        AuthDecision auth = authorizePlayerOrServer(exchange, bodyValues.getOrDefault("opaque_player_token", ""), request.minecraftPlayerUuid(), "memory:read_self", bodyValues);
        if (!auth.allowed()) {
            HttpJson.writeJson(exchange, authHttpStatus(auth), HttpJson.object(Map.of("error", auth.reason())));
            return;
        }
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
        AuthDecision auth = authorizeServer(exchange, HttpJson.query(exchange.getRequestURI()));
        if (!auth.allowed()) {
            HttpJson.writeJson(exchange, authHttpStatus(auth), HttpJson.object(Map.of("error", auth.reason())));
            return;
        }
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
        AuthDecision auth = authorizeServer(exchange, query);
        if (!auth.allowed()) {
            HttpJson.writeJson(exchange, authHttpStatus(auth), HttpJson.object(Map.of("error", auth.reason())));
            return;
        }
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
        AuthDecision auth = authorizeServer(exchange, query);
        if (!auth.allowed()) {
            HttpJson.writeJson(exchange, authHttpStatus(auth), HttpJson.object(Map.of("error", auth.reason())));
            return;
        }
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
        AuthDecision auth = authorizeServer(exchange, query);
        if (!auth.allowed()) {
            HttpJson.writeJson(exchange, authHttpStatus(auth), HttpJson.object(Map.of("error", auth.reason())));
            return;
        }
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

    private static Map<String, Object> profileFromBody(Map<String, String> body, boolean regenerated) {
        String worldId = value(body, "world_id", "unknown-world");
        String npcKey = value(body, "npc_key", value(body, "profile_id", "generated:" + UUID.randomUUID()));
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", npcKey);
        profile.put("world_id", worldId);
        profile.put("npc_key", npcKey);
        profile.put("entity_uuid", value(body, "entity_uuid", ""));
        profile.put("tier", value(body, "tier", "major_promoted"));
        profile.put("display_name", value(body, "display_name", npcKey));
        profile.put("provider", "gateway");
        profile.put("regenerated", regenerated);
        profile.put("created_at_epoch_ms", System.currentTimeMillis());
        profile.put("generation_seed", value(body, "generation_seed",
                worldId + "|" + npcKey + "|" + value(body, "entity_type", "unknown_entity")));
        return profile;
    }

    private static String profileKey(String worldId, String npcKey) {
        return (worldId == null ? "" : worldId) + "|" + (npcKey == null ? "" : npcKey);
    }

    private static String quotaKey(GatewayChatRequest request) {
        return quotaKey(request.serverId(), request.worldId(), request.minecraftPlayerUuid());
    }

    private static String quotaKey(String serverId, String worldId, String minecraftUuid) {
        return "player:" + (serverId == null || serverId.isBlank() ? "local-dev" : serverId)
                + ":" + (worldId == null || worldId.isBlank() ? "unknown-world" : worldId)
                + ":" + (minecraftUuid == null || minecraftUuid.isBlank() ? "unknown-player" : minecraftUuid);
    }

    private static int authHttpStatus(AuthDecision decision) {
        return GatewayAuthGuard.httpStatus(new GatewayAuthGuard.Decision(decision.allowed(), decision.reason()));
    }

    private AuthDecision authorizePlayer(HttpExchange exchange, String opaqueToken, String minecraftUuid, String scope, boolean tokenRequired) {
        GatewayAuthGuard.Decision decision = authGuard.requirePlayerToken(exchange, opaqueToken, minecraftUuid, scope, tokenRequired);
        return new AuthDecision(decision.allowed(), decision.reason());
    }

    private AuthDecision authorizePlayerOrServer(HttpExchange exchange, String opaqueToken, String minecraftUuid, String scope, Map<String, String> values) {
        GatewayAuthGuard.Decision decision = authGuard.requirePlayerOrServer(exchange, opaqueToken, minecraftUuid, scope, values);
        return new AuthDecision(decision.allowed(), decision.reason());
    }

    private AuthDecision authorizeServer(HttpExchange exchange, Map<String, String> values) {
        GatewayAuthGuard.Decision decision = authGuard.requireServerAdmin(exchange, values);
        return new AuthDecision(decision.allowed(), decision.reason());
    }

    private static boolean profileLeaksForbiddenSecret(Map<String, Object> profile, String forbiddenSecretList) {
        if (forbiddenSecretList == null || forbiddenSecretList.isBlank()) {
            return false;
        }
        String profileText = String.valueOf(profile).toLowerCase(java.util.Locale.ROOT);
        for (String raw : forbiddenSecretList.split("[,;\\n]")) {
            String secret = raw.strip().toLowerCase(java.util.Locale.ROOT);
            if (!secret.isBlank() && profileText.contains(secret)) {
                return true;
            }
        }
        return false;
    }

    private static String value(Map<String, String> values, String key, String fallback) {
        String value = values.get(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static PathParts pathParts(HttpExchange exchange, String prefix) {
        String rawPath = exchange.getRequestURI().getPath();
        String suffix = rawPath.startsWith(prefix) ? rawPath.substring(prefix.length()) : "";
        List<String> parts = new ArrayList<>();
        for (String part : suffix.split("/")) {
            if (!part.isBlank()) {
                parts.add(part);
            }
        }
        return new PathParts(List.copyOf(parts));
    }

    private record AuthDecision(boolean allowed, String reason) {
        static AuthDecision allowed(String reason) {
            return new AuthDecision(true, reason == null ? "ok" : reason);
        }

        static AuthDecision denied(String reason) {
            return new AuthDecision(false, reason == null ? "auth_required" : reason);
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

    private record PathParts(List<String> parts) {
    }
}
