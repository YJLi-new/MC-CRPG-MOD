package com.crpg.ebb.gateway;

import com.crpg.ebb.gateway.auth.DeviceAuthService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.Executors;

public final class GatewayServer implements AutoCloseable {
    private final GatewayConfig config;
    private final DeviceAuthService authService;
    private final HttpServer server;

    private GatewayServer(GatewayConfig config, DeviceAuthService authService, HttpServer server) {
        this.config = config;
        this.authService = authService;
        this.server = server;
    }

    public static GatewayServer create(GatewayConfig config) throws IOException {
        DeviceAuthService authService = new DeviceAuthService(config.createAuthProvider());
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(config.bindHost(), config.port()), 0);
        GatewayServer gateway = new GatewayServer(config, authService, httpServer);
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
        server.createContext("/", exchange -> HttpJson.writeJson(exchange, 404, HttpJson.object(Map.of("error", "not_found"))));
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            HttpJson.methodNotAllowed(exchange);
            return;
        }
        HttpJson.writeJson(exchange, 200, authService.healthJson());
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
