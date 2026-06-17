package com.crpg.ebb.gateway;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
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
                30_000L,
                "jdbc:h2:mem:ebb_gateway_smoke;DB_CLOSE_DELAY=-1"
        );
        try (GatewayServer server = GatewayServer.create(config)) {
            server.start();
            HttpClient client = HttpClient.newHttpClient();
            URI base = URI.create("http://127.0.0.1:" + server.port());
            String health = send(client, HttpRequest.newBuilder(base.resolve("/v1/health")).GET().build());
            require(health.contains("\"status\":\"ok\""), "health should be ok: " + health);
            require(health.contains("dev_local"), "health should report dev_local provider: " + health);

            String quota = send(client, HttpRequest.newBuilder(base.resolve("/v1/player/quota")).GET().build());
            require(quota.contains("\"status\":\"ok\"") && quota.contains("llm:chat"),
                    "quota endpoint should expose safe limits/scopes: " + quota);

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

            String profile = send(client, HttpRequest.newBuilder(base.resolve("/v1/npc/profile/ensure"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(HttpJson.object(Map.of(
                            "world_id", "smoke-world",
                            "npc_key", "ebb:demo/innkeeper",
                            "entity_type", "minecraft:villager",
                            "display_name", "Smoke Innkeeper"
                    ))))
                    .build());
            require(profile.contains("\"status\":\"ok\"") && profile.contains("Smoke Innkeeper"),
                    "profile ensure endpoint should return deterministic profile data: " + profile);
            String fetchedProfile = send(client, HttpRequest.newBuilder(base.resolve("/v1/npc/profile/smoke-world/ebb:demo/innkeeper")).GET().build());
            require(fetchedProfile.contains("Smoke Innkeeper"), "profile get endpoint should return ensured profile: " + fetchedProfile);

            String chatStart = send(client, HttpRequest.newBuilder(base.resolve("/v1/chat/start"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(HttpJson.object(Map.of(
                            "server_id", "smoke",
                            "world_id", "smoke-world",
                            "minecraft_player_uuid", "00000000-0000-0000-0000-000000000123",
                            "npc_key", "ebb:demo/innkeeper",
                            "conversation_id", "gateway-start-smoke"
                    ))))
                    .build());
            require(chatStart.contains("gateway-start-smoke") && chatStart.contains("\"open\""),
                    "chat start/session should be recorded: " + chatStart);
            String chatSession = send(client, HttpRequest.newBuilder(base.resolve("/v1/chat/session/gateway-start-smoke")).GET().build());
            require(chatSession.contains("\"status\":\"ok\"") && chatSession.contains("gateway-start-smoke"),
                    "chat session endpoint should expose the started session: " + chatSession);

            Map<String, Object> chatBody = new LinkedHashMap<>();
            chatBody.put("server_id", "smoke");
            chatBody.put("world_id", "smoke-world");
            chatBody.put("minecraft_player_uuid", "00000000-0000-0000-0000-000000000123");
            chatBody.put("npc_key", "ebb:demo/innkeeper");
            chatBody.put("npc_display_name", "innkeeper");
            chatBody.put("conversation_id", "smoke-conversation");
            chatBody.put("dialogue_id", "ebb:demo/innkeeper_intro");
            chatBody.put("source_node_id", "start");
            chatBody.put("topic_hint", "smoke");
            chatBody.put("scene_context", "gateway smoke");
            chatBody.put("message", "hello");
            chatBody.put("opaque_player_token", token);
            chatBody.put("model", "mock-model");
            chatBody.put("stream", true);
            chatBody.put("structured", true);
            chatBody.put("store", false);
            chatBody.put("max_output_tokens", 128);
            String chat = send(client, HttpRequest.newBuilder(base.resolve("/v1/chat/message"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(HttpJson.object(chatBody)))
                    .build());
            require(chat.contains("FAKE_GATEWAY_REPLY"), "fake gateway chat should return deterministic reply: " + chat);
            require(chat.contains("\"chunked_response\":true"), "gateway chat should expose chunked response evidence: " + chat);
            require(chat.contains("\"store\":false"), "gateway chat must default to store:false privacy mode: " + chat);
            require(chat.contains("structured_json"), "gateway chat should include structured_json field: " + chat);

            chatBody.put("conversation_id", "smoke-conversation-2");
            chatBody.put("message", "fact:player.favorite=blue");
            String secondChat = send(client, HttpRequest.newBuilder(base.resolve("/v1/chat/message"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(HttpJson.object(chatBody)))
                    .build());
            require(secondChat.contains("FAKE_GATEWAY_REPLY"), "second memory-writing chat should work: " + secondChat);

            String search = send(client, HttpRequest.newBuilder(base.resolve("/v1/memory/search"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(HttpJson.object(Map.of(
                            "server_id", "smoke",
                            "world_id", "smoke-world",
                            "minecraft_player_uuid", "00000000-0000-0000-0000-000000000123",
                            "npc_key", "ebb:demo/innkeeper",
                            "query", "favorite blue",
                            "limit", 5
                    ))))
                    .build());
            require(search.contains("favorite=blue") || search.contains("blue"), "memory search should retrieve earlier turn: " + search);
            require(search.contains("citation_ids"), "memory retrieval should return citation ids: " + search);
            String recordId = HttpJson.stringValue(search, "id").orElse("");
            if (!recordId.isBlank()) {
                String inspect = send(client, HttpRequest.newBuilder(base.resolve("/v1/memory/inspect?id=" + URLEncoder.encode(recordId, StandardCharsets.UTF_8))).GET().build());
                require(inspect.contains(recordId), "memory inspect should return the requested record: " + inspect);
            }

            chatBody.put("conversation_id", "smoke-conversation-3");
            chatBody.put("message", "fact:player.favorite=red");
            send(client, HttpRequest.newBuilder(base.resolve("/v1/chat/message"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(HttpJson.object(chatBody)))
                    .build());
            String conflicts = send(client, HttpRequest.newBuilder(base.resolve("/v1/memory/conflicts?server_id=smoke&world_id=smoke-world&limit=5")).GET().build());
            require(conflicts.contains("favorite") && conflicts.contains("blue") && conflicts.contains("red"),
                    "changed fact should create a supersede/conflict record: " + conflicts);
            String factId = HttpJson.stringValue(conflicts, "new_fact_id").orElse("");
            if (!factId.isBlank()) {
                String correction = send(client, HttpRequest.newBuilder(base.resolve("/v1/memory/correct"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(HttpJson.object(Map.of(
                                "fact_id", factId,
                                "new_value", "green",
                                "reason", "smoke correction"
                        ))))
                        .build());
                require(correction.contains("\"accepted\":true") && correction.contains("append_only"),
                        "memory correction should append an audit lesson rather than mutating raw memory: " + correction);
            }

            chatBody.put("conversation_id", "smoke-conversation-4");
            chatBody.put("message", "我是旅馆老板");
            send(client, HttpRequest.newBuilder(base.resolve("/v1/chat/message"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(HttpJson.object(chatBody)))
                    .build());
            String lessons = send(client, HttpRequest.newBuilder(base.resolve("/v1/memory/lessons?server_id=smoke&world_id=smoke-world&limit=5")).GET().build());
            require(lessons.contains("canonical owner remains innkeeper") && lessons.contains("canonical_conflict"),
                    "canonical innkeeper ownership should be protected by a safety lesson: " + lessons);

            chatBody.put("conversation_id", "smoke-conversation-5");
            chatBody.put("message", "I question the ledger about the missing page");
            send(client, HttpRequest.newBuilder(base.resolve("/v1/chat/message"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(HttpJson.object(chatBody)))
                    .build());
            String ledgerSearch = send(client, HttpRequest.newBuilder(base.resolve("/v1/memory/search"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(HttpJson.object(Map.of(
                            "server_id", "smoke",
                            "world_id", "smoke-world",
                            "minecraft_player_uuid", "00000000-0000-0000-0000-000000000123",
                            "npc_key", "ebb:demo/innkeeper",
                            "query", "questioned_ledger",
                            "limit", 5
                    ))))
                    .build());
            require(ledgerSearch.contains("questioned_ledger") || ledgerSearch.contains("previously questioned the ledger"),
                    "NPC memory retrieval should find the player questioned the ledger: " + ledgerSearch);
            String ledgerRecordId = HttpJson.stringValue(ledgerSearch, "id").orElse("");
            require(!ledgerRecordId.isBlank(), "ledger memory search should expose an inspectable record id: " + ledgerSearch);
            String ledgerInspect = send(client, HttpRequest.newBuilder(base.resolve("/v1/memory/inspect?id=" + URLEncoder.encode(ledgerRecordId, StandardCharsets.UTF_8))).GET().build());
            require(ledgerInspect.contains("raw_episode") && ledgerInspect.contains("extracted_facts")
                            && ledgerInspect.contains("memory_operations") && ledgerInspect.contains("summaries"),
                    "dev inspect should expose raw episode, extracted facts, operations, and summaries: " + ledgerInspect);
            String episodes = send(client, HttpRequest.newBuilder(base.resolve("/v1/memory/episodes?server_id=smoke&world_id=smoke-world&limit=5")).GET().build());
            require(episodes.contains("raw_episode") && episodes.contains("summary"),
                    "episodes endpoint should expose raw episodes and summaries: " + episodes);

            String ingest = send(client, HttpRequest.newBuilder(base.resolve("/v1/memory/ingest"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(HttpJson.object(Map.of(
                            "server_id", "smoke",
                            "world_id", "smoke-world",
                            "event", "manual-audit"
                    ))))
                    .build());
            require(ingest.contains("\"accepted\":true"), "memory ingest route should be present and auditable: " + ingest);
            String kbUpdate = send(client, HttpRequest.newBuilder(base.resolve("/v1/knowledge/update"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(HttpJson.object(Map.of(
                            "npc_key", "ebb:demo/innkeeper",
                            "fact", "smoke_fact",
                            "reason", "smoke"
                    ))))
                    .build());
            require(kbUpdate.contains("\"accepted\":true"), "knowledge update route should accept audited updates: " + kbUpdate);
            String kbInspect = send(client, HttpRequest.newBuilder(base.resolve("/v1/knowledge/npc/ebb:demo/innkeeper")).GET().build());
            require(kbInspect.contains("smoke_fact"), "knowledge npc route should return audited updates: " + kbInspect);
            String cancel = send(client, HttpRequest.newBuilder(base.resolve("/v1/chat/cancel"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(HttpJson.object(Map.of(
                            "conversation_id", "gateway-start-smoke",
                            "reason", "smoke_done"
                    ))))
                    .build());
            require(cancel.contains("\"cancelled\":true"), "chat cancel route should cancel tracked sessions: " + cancel);

            String logout = send(client, HttpRequest.newBuilder(base.resolve("/v1/auth/logout"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(HttpJson.object(Map.of("opaque_player_token", token))))
                    .build());
            require(logout.contains("\"revoked\":true"), "logout should revoke token: " + logout);

            var denied = client.send(HttpRequest.newBuilder(base.resolve("/v1/chat/message"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(HttpJson.object(Map.of(
                            "conversation_id", "smoke-conversation",
                            "npc_key", "ebb:demo/innkeeper",
                            "message", "after logout",
                            "opaque_player_token", token
                    ))))
                    .build(), HttpResponse.BodyHandlers.ofString());
            require(denied.statusCode() == 401, "revoked token should be rejected by chat endpoint: " + denied.statusCode() + " " + denied.body());

            String deleted = send(client, HttpRequest.newBuilder(base.resolve("/v1/memory/player/00000000-0000-0000-0000-000000000123")).DELETE().build());
            require(deleted.contains("\"deleted\":true") && deleted.contains("\"records\""),
                    "memory delete_player should delete/exportable player memory rows: " + deleted);
        }
        GatewayConfig mockConfig = GatewayConfig.fromEnv(Map.of("EBB_GATEWAY_CHAT_PROVIDER", "mock_openai_responses", "EBB_MEMORY_DB_URL", "jdbc:h2:mem:mock_gateway;DB_CLOSE_DELAY=-1"));
        require("mock_openai_responses".equals(mockConfig.createChatProvider().providerName()),
                "mock OpenAI provider should be selectable without API access");
        System.out.println("P36 gateway smoke passed; P37 gateway chat smoke passed; P38 memory smoke passed; P39 memory consolidation smoke passed");
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
