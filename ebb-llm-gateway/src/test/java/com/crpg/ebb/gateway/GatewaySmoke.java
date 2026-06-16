package com.crpg.ebb.gateway;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public final class GatewaySmoke {
    private GatewaySmoke() {
    }

    public static void main(String[] args) throws Exception {
        GatewayConfig config = new GatewayConfig(
                "127.0.0.1",
                0,
                "dev_local",
                URI.create("http://127.0.0.1:0"),
                null,
                null,
                "",
                "",
                "openid profile llm:chat",
                Duration.ofSeconds(5),
                "fake",
                "gpt-5.2",
                true,
                true,
                false,
                700,
                3,
                30_000L
        );
        try (GatewayServer server = GatewayServer.create(config)) {
            server.start();
            HttpClient client = HttpClient.newHttpClient();
            URI base = URI.create("http://127.0.0.1:" + server.port());
            String health = send(client, HttpRequest.newBuilder(base.resolve("/v1/health")).GET().build());
            require(health.contains("\"status\":\"ok\""), "health should be ok: " + health);
            require(health.contains("dev_local"), "health should report dev_local provider: " + health);

            String start = send(client, HttpRequest.newBuilder(base.resolve("/v1/auth/device/start"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"minecraft_uuid\":\"00000000-0000-0000-0000-000000000123\",\"server_id\":\"smoke\"}"))
                    .build());
            String authSessionId = HttpJson.stringValue(start, "auth_session_id").orElseThrow();
            require(!authSessionId.isBlank(), "auth_session_id should be returned: " + start);
            require(HttpJson.stringValue(start, "user_code").orElse("").contains("-"), "user_code should be returned: " + start);

            String status = send(client, HttpRequest.newBuilder(base.resolve("/v1/auth/device/status?auth_session_id=" + authSessionId)).GET().build());
            require(status.contains("\"status\":\"authenticated\""), "dev local should authenticate on poll: " + status);
            String token = HttpJson.stringValue(status, "opaque_player_token").orElseThrow();
            require(token.startsWith("ebb_player_"), "gateway should return only an opaque Ebb token: " + status);

            String logout = send(client, HttpRequest.newBuilder(base.resolve("/v1/auth/logout"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(HttpJson.object(Map.of("opaque_player_token", token))))
                    .build());
            require(logout.contains("\"revoked\":true"), "logout should revoke token: " + logout);
        }
        System.out.println("P36 gateway smoke passed");
    }

    private static String send(HttpClient client, HttpRequest request) throws Exception {
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new AssertionError(request.uri() + " returned HTTP " + response.statusCode() + ": " + response.body());
        }
        return response.body();
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
