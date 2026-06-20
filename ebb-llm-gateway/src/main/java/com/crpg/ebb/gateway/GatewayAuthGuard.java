package com.crpg.ebb.gateway;

import com.crpg.ebb.gateway.auth.DeviceAuthService;
import com.sun.net.httpserver.HttpExchange;

import java.util.Map;

/** Shared authorization gate for player-scoped and server-admin gateway endpoints. */
public final class GatewayAuthGuard {
    private final GatewayConfig config;
    private final DeviceAuthService authService;

    public GatewayAuthGuard(GatewayConfig config, DeviceAuthService authService) {
        this.config = config;
        this.authService = authService;
    }

    public Decision requirePlayerToken(HttpExchange exchange, String opaqueToken, String minecraftUuid, String scope, boolean tokenRequired) {
        String token = opaqueToken == null || opaqueToken.isBlank() ? bearerToken(exchange) : opaqueToken.strip();
        if (token.isBlank()) {
            if (!tokenRequired || config.localDevBlankTokenAllowed()) {
                return Decision.allowed("local_dev_blank_token");
            }
            return Decision.denied("auth_required");
        }
        if (!authService.tokenValidForPlayer(token, minecraftUuid)) {
            return Decision.denied(authService.tokenMinecraftUuid(token).isPresent() ? "wrong_player_token" : "auth_required");
        }
        if (!authService.tokenHasScope(token, scope)) {
            return Decision.denied("forbidden_scope:" + scope);
        }
        return Decision.allowed("player_token");
    }

    public Decision requirePlayerOrServer(HttpExchange exchange, String opaqueToken, String minecraftUuid, String scope, Map<String, String> values) {
        Decision server = requireServerAdmin(exchange, values);
        if (server.allowed()) {
            return server;
        }
        return requirePlayerToken(exchange, opaqueToken, minecraftUuid, scope, true);
    }

    public Decision requireServerAdmin(HttpExchange exchange, Map<String, String> values) {
        if (!config.requireServerTokenForAdminEndpoints()) {
            return Decision.allowed("server_token_not_required");
        }
        if (config.localDevBlankTokenAllowed() && config.serverSharedSecret().isBlank()) {
            return Decision.allowed("local_dev_blank_server_token");
        }
        String expected = config.serverSharedSecret();
        if (expected.isBlank()) {
            return Decision.denied("server_token_not_configured");
        }
        String supplied = values == null ? "" : values.getOrDefault("server_token", "");
        if (supplied == null || supplied.isBlank()) {
            supplied = exchange == null ? "" : exchange.getRequestHeaders().getFirst("X-Ebb-Server-Token");
        }
        if (supplied == null || supplied.isBlank()) {
            supplied = bearerToken(exchange);
        }
        if (expected.equals(supplied)) {
            return Decision.allowed("server_token");
        }
        return Decision.denied("server_token_required");
    }

    public static int httpStatus(Decision decision) {
        if (decision == null || decision.allowed()) {
            return 200;
        }
        String reason = decision.reason();
        return reason.startsWith("wrong_player_token") || reason.startsWith("forbidden_scope") ? 403 : 401;
    }

    private static String bearerToken(HttpExchange exchange) {
        String header = exchange == null ? "" : exchange.getRequestHeaders().getFirst("Authorization");
        if (header == null || header.isBlank()) {
            return "";
        }
        String prefix = "Bearer ";
        return header.regionMatches(true, 0, prefix, 0, prefix.length()) ? header.substring(prefix.length()).strip() : "";
    }

    public record Decision(boolean allowed, String reason) {
        public static Decision allowed(String reason) {
            return new Decision(true, reason == null ? "ok" : reason);
        }

        public static Decision denied(String reason) {
            return new Decision(false, reason == null ? "auth_required" : reason);
        }
    }
}
