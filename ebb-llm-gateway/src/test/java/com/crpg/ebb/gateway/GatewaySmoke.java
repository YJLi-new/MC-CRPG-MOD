package com.crpg.ebb.gateway;

import com.crpg.ebb.gateway.auth.CodexCliAuthProvider;
import com.crpg.ebb.gateway.chat.GatewayChatRequest;
import com.crpg.ebb.gateway.chat.GatewayChatResponse;
import com.crpg.ebb.gateway.memory.MemoryOperation;
import com.crpg.ebb.gateway.memory.MemoryStore;

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
                "codex",
                "./build/gateway-smoke-codex-auth",
                5,
                Duration.ofSeconds(5),
                "fake",
                "gpt-5.2",
                true,
                true,
                false,
                700,
                3,
                30_000L,
                "jdbc:h2:mem:ebb_gateway_smoke;DB_CLOSE_DELAY=-1",
                true,
                false,
                "",
                true,
                20,
                240,
                500,
                6
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
                    "memory correction should append an audit trail: " + correction);
            require(correction.contains("replacement_fact_id") && correction.contains("superseded_fact_id"),
                    "memory correction should create a replacement fact and supersede the old fact: " + correction);
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
        GatewayConfig hardenedConfig = new GatewayConfig(
                "0.0.0.0",
                0,
                "dev_local",
                URI.create("http://127.0.0.1:0"),
                null,
                null,
                "",
                "",
                "openid profile llm:chat memory:read_self memory:write_self memory:delete_self",
                "codex",
                "./build/gateway-smoke-codex-auth-hardened",
                5,
                Duration.ofSeconds(5),
                "fake",
                "gpt-5.2",
                true,
                true,
                false,
                700,
                3,
                30_000L,
                "jdbc:h2:mem:ebb_gateway_hardened_smoke;DB_CLOSE_DELAY=-1",
                true,
                true,
                "server-secret-smoke",
                false,
                2,
                5,
                100,
                6
        );
        try (GatewayServer server = GatewayServer.create(hardenedConfig)) {
            server.start();
            HttpClient client = HttpClient.newHttpClient();
            URI base = URI.create("http://127.0.0.1:" + server.port());
            String start = send(client, HttpRequest.newBuilder(base.resolve("/v1/auth/device/start"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"minecraft_uuid\":\"00000000-0000-0000-0000-000000000999\",\"server_id\":\"hardened\"}"))
                    .build());
            String authSessionId = HttpJson.stringValue(start, "auth_session_id").orElseThrow();
            String status = send(client, HttpRequest.newBuilder(base.resolve("/v1/auth/device/status?auth_session_id=" + authSessionId)).GET().build());
            String token = HttpJson.stringValue(status, "opaque_player_token").orElseThrow();

            var blankDenied = sendRaw(client, HttpRequest.newBuilder(base.resolve("/v1/chat/message"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(HttpJson.object(Map.of(
                            "server_id", "hardened",
                            "world_id", "world-a",
                            "minecraft_player_uuid", "00000000-0000-0000-0000-000000000999",
                            "npc_key", "ebb:demo/innkeeper",
                            "conversation_id", "blank-denied",
                            "message", "blank token should fail"
                    ))))
                    .build());
            require(blankDenied.statusCode() == 401 && blankDenied.body().contains("auth_required"),
                    "blank token should be rejected in hardened config: " + blankDenied.statusCode() + " " + blankDenied.body());

            var wrongPlayer = sendRaw(client, HttpRequest.newBuilder(base.resolve("/v1/chat/message"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(HttpJson.object(Map.of(
                            "server_id", "hardened",
                            "world_id", "world-a",
                            "minecraft_player_uuid", "00000000-0000-0000-0000-000000000998",
                            "npc_key", "ebb:demo/innkeeper",
                            "conversation_id", "wrong-player-denied",
                            "message", "wrong token should fail",
                            "opaque_player_token", token
                    ))))
                    .build());
            require(wrongPlayer.statusCode() == 403 && wrongPlayer.body().contains("wrong_player_token"),
                    "wrong player token should be rejected: " + wrongPlayer.statusCode() + " " + wrongPlayer.body());

            var inspectDenied = sendRaw(client, HttpRequest.newBuilder(base.resolve("/v1/memory/inspect?id=anything")).GET().build());
            require(inspectDenied.statusCode() == 401 && inspectDenied.body().contains("server_token"),
                    "memory inspect should require server token: " + inspectDenied.statusCode() + " " + inspectDenied.body());
            var memorySearchDenied = sendRaw(client, HttpRequest.newBuilder(base.resolve("/v1/memory/search"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(HttpJson.object(Map.of(
                            "server_id", "hardened",
                            "world_id", "world-a",
                            "minecraft_player_uuid", "00000000-0000-0000-0000-000000000999",
                            "query", "favorite"
                    ))))
                    .build());
            require(memorySearchDenied.statusCode() == 401 && memorySearchDenied.body().contains("auth_required"),
                    "unauthenticated memory search should be denied: " + memorySearchDenied.statusCode() + " " + memorySearchDenied.body());
            var wrongPlayerMemorySearch = sendRaw(client, HttpRequest.newBuilder(base.resolve("/v1/memory/search"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(HttpJson.object(Map.of(
                            "server_id", "hardened",
                            "world_id", "world-a",
                            "minecraft_player_uuid", "00000000-0000-0000-0000-000000000998",
                            "query", "favorite",
                            "opaque_player_token", token
                    ))))
                    .build());
            require(wrongPlayerMemorySearch.statusCode() == 403 && wrongPlayerMemorySearch.body().contains("wrong_player_token"),
                    "A player's token must not read another player's memory: " + wrongPlayerMemorySearch.statusCode() + " " + wrongPlayerMemorySearch.body());

            String generated = send(client, HttpRequest.newBuilder(base.resolve("/v1/npc/profile/generate"))
                    .header("Content-Type", "application/json")
                    .header("X-Ebb-Server-Token", "server-secret-smoke")
                    .POST(HttpRequest.BodyPublishers.ofString(HttpJson.object(Map.of(
                            "world_id", "world-a",
                            "npc_key", "minor:smith",
                            "entity_type", "minecraft:villager",
                            "visible_public_lore", "forge smoke",
                            "forbidden_secret_list", "hidden murder note"
                    ))))
                    .build());
            require(generated.contains("/v1/npc/profile/generate") && generated.contains("validated_no_hidden_kb_leak"),
                    "profile generate endpoint should validate hidden KB leak guardrails: " + generated);

            Map<String, Object> chatBody = new LinkedHashMap<>();
            chatBody.put("server_id", "hardened");
            chatBody.put("world_id", "world-a");
            chatBody.put("minecraft_player_uuid", "00000000-0000-0000-0000-000000000999");
            chatBody.put("npc_key", "ebb:demo/innkeeper");
            chatBody.put("npc_display_name", "innkeeper");
            chatBody.put("conversation_id", "memory-proof-a");
            chatBody.put("dialogue_id", "ebb:demo/innkeeper_intro");
            chatBody.put("source_node_id", "start");
            chatBody.put("topic_hint", "memory proof");
            chatBody.put("message", "fact:player.hometown=Riverside");
            chatBody.put("opaque_player_token", token);
            send(client, HttpRequest.newBuilder(base.resolve("/v1/chat/message"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(HttpJson.object(chatBody)))
                    .build());
            chatBody.put("conversation_id", "memory-proof-b");
            chatBody.put("message", "Do you remember where I am from?");
            String recalled = send(client, HttpRequest.newBuilder(base.resolve("/v1/chat/message"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(HttpJson.object(chatBody)))
                    .build());
            require(recalled.contains("memory=recall") && recalled.contains("Riverside") && recalled.contains("memory:"),
                    "second chat should inject prior memory context and expose memory citation: " + recalled);
            String quotaAfterTwo = send(client, HttpRequest.newBuilder(base.resolve("/v1/player/quota?server_id=hardened&world_id=world-a&minecraft_player_uuid=00000000-0000-0000-0000-000000000999&opaque_player_token=" + URLEncoder.encode(token, StandardCharsets.UTF_8))).GET().build());
            require(quotaAfterTwo.contains("\"remaining\":0") && quotaAfterTwo.contains("daily_remaining"),
                    "quota endpoint should report remaining/reset/daily_remaining after reservations: " + quotaAfterTwo);

            var limited = sendRaw(client, HttpRequest.newBuilder(base.resolve("/v1/chat/message"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(HttpJson.object(chatBody)))
                    .build());
            require(limited.statusCode() == 429 && limited.body().contains("rate_limited"),
                    "third same-minute chat should trip hardened per-player limiter: " + limited.statusCode() + " " + limited.body());
        }
        GatewayConfig mockConfig = GatewayConfig.fromEnv(Map.of("EBB_GATEWAY_CHAT_PROVIDER", "mock_openai_responses", "EBB_MEMORY_DB_URL", "jdbc:h2:mem:mock_gateway;DB_CLOSE_DELAY=-1"));
        require("mock_openai_responses".equals(mockConfig.createChatProvider().providerName()),
                "mock OpenAI provider should be selectable without API access");
        GatewayConfig codexConfig = GatewayConfig.fromEnv(Map.of(
                "EBB_GATEWAY_AUTH_PROVIDER", "openai_codex",
                "EBB_CODEX_CLI_COMMAND", "codex",
                "EBB_CODEX_HOME", "./build/gateway-smoke-codex-auth-config",
                "EBB_CODEX_DEVICE_START_TIMEOUT_SECONDS", "9",
                "EBB_MEMORY_DB_URL", "jdbc:h2:mem:codex_gateway;DB_CLOSE_DELAY=-1"));
        require("openai_codex".equals(codexConfig.createAuthProvider().providerName()),
                "OpenAI Codex CLI device auth provider should be selectable from env");
        var codexInfo = CodexCliAuthProvider.extractDeviceCodeInfo("""
                Follow these steps to sign in with ChatGPT using device code authorization:
                1. Open this link: https://auth.openai.com/codex/device
                2. Enter this one-time code (expires in 15 minutes)
                   W7BM-QVCKE
                Device codes are a common phishing target. Never share this code.
                """).orElseThrow();
        require("https://auth.openai.com/codex/device".equals(codexInfo.verificationUrl()),
                "Codex device auth parser should extract the public verification URL");
        require("W7BM-QVCKE".equals(codexInfo.userCode()),
                "Codex device auth parser should extract the one-time user code");
        require(codexInfo.expiresInSeconds() == 900L,
                "Codex device auth parser should extract the 15-minute expiry");
        runStructuredMemoryOpsSmoke();
        runGatewayPersistenceSmoke();
        System.out.println("P36 gateway smoke passed; P36 Codex OAuth device-code smoke passed; P37 gateway chat smoke passed; P38 memory smoke passed; P39 memory consolidation smoke passed");
    }

    private static void runStructuredMemoryOpsSmoke() {
        MemoryStore store = new MemoryStore("jdbc:h2:mem:memory_ops_smoke;DB_CLOSE_DELAY=-1");
        GatewayChatRequest request = new GatewayChatRequest(
                "ops", "world", "00000000-0000-0000-0000-000000000321", "ebb:demo/innkeeper",
                "innkeeper", "", "ops-conv", "ebb:demo/innkeeper_intro", "start",
                "ops", "", "hello", "", "mock-model", false, true, false, 128, "", java.util.List.of());
        GatewayChatResponse response = new GatewayChatResponse(
                request.conversationId(),
                "I will remember that.",
                "guarded",
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of(),
                """
                {"npc_reply":"ok","mood":"guarded","suggested_options":[],"memory_ops":[
                  {"op":"add","kind":"fact","text":"player.hometown=Riverside","subject":"player:00000000-0000-0000-0000-000000000321","predicate":"hometown","object":"Riverside","confidence":0.91},
                  {"op":"complete_quest_branch","kind":"effect","text":"complete_quest_branch:secret","confidence":0.99},
                  {"op":"add","kind":"fact","text":"player.secret=too-low","subject":"player:00000000-0000-0000-0000-000000000321","predicate":"secret","object":"too-low","confidence":0.1}
                ],"citations":[],"warnings":[],"memory_writes":[]}
                """,
                "test", "mock", false, false, "ok", "");
        var result = store.appendTurn(request, response);
        require(result.operations().stream().anyMatch(operation -> operation.proposedBy().contains("memory_ops")
                        && operation.value().contains("Riverside") && "accepted".equals(operation.status())),
                "memory_ops-only structured JSON should create an accepted operation: " + result.operations());
        require(result.facts().stream().anyMatch(fact -> fact.value().contains("Riverside")),
                "memory_ops-only structured JSON should create a fact: " + result.facts());
        require(result.operations().stream().anyMatch(operation -> "rejected".equals(operation.status())
                        && (operation.reason().contains("unsupported_memory_op") || operation.reason().contains("high_risk"))),
                "high-risk memory_ops should be persisted as rejected operations: " + result.operations());
        require(result.operations().stream().anyMatch(operation -> "rejected".equals(operation.status())
                        && operation.reason().contains("confidence_below_threshold")),
                "low-confidence memory_ops should be persisted as rejected operations: " + result.operations());
    }

    private static void runGatewayPersistenceSmoke() throws Exception {
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
                "codex",
                "./build/gateway-smoke-codex-auth-persist",
                5,
                Duration.ofSeconds(5),
                "fake",
                "gpt-5.2",
                true,
                true,
                false,
                700,
                3,
                30_000L,
                "jdbc:h2:mem:ebb_gateway_persistence;DB_CLOSE_DELAY=-1",
                true,
                false,
                "",
                true,
                20,
                240,
                500,
                6
        );
        HttpClient client = HttpClient.newHttpClient();
        try (GatewayServer server = GatewayServer.create(config)) {
            server.start();
            URI base = URI.create("http://127.0.0.1:" + server.port());
            send(client, HttpRequest.newBuilder(base.resolve("/v1/npc/profile/ensure"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(HttpJson.object(Map.of(
                            "world_id", "persist-world",
                            "npc_key", "ebb:demo/persisted",
                            "display_name", "Persistent NPC"
                    ))))
                    .build());
            send(client, HttpRequest.newBuilder(base.resolve("/v1/chat/start"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(HttpJson.object(Map.of(
                            "server_id", "persist",
                            "world_id", "persist-world",
                            "minecraft_player_uuid", "00000000-0000-0000-0000-000000000777",
                            "npc_key", "ebb:demo/persisted",
                            "conversation_id", "persist-session"
                    ))))
                    .build());
            send(client, HttpRequest.newBuilder(base.resolve("/v1/knowledge/update"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(HttpJson.object(Map.of(
                            "npc_key", "ebb:demo/persisted",
                            "fact", "persisted_fact"
                    ))))
                    .build());
        }
        try (GatewayServer restarted = GatewayServer.create(config)) {
            restarted.start();
            URI base = URI.create("http://127.0.0.1:" + restarted.port());
            String profile = send(client, HttpRequest.newBuilder(base.resolve("/v1/npc/profile/persist-world/ebb:demo/persisted")).GET().build());
            String session = send(client, HttpRequest.newBuilder(base.resolve("/v1/chat/session/persist-session")).GET().build());
            String knowledge = send(client, HttpRequest.newBuilder(base.resolve("/v1/knowledge/npc/ebb:demo/persisted")).GET().build());
            require(profile.contains("Persistent NPC"), "profile should survive gateway restart: " + profile);
            require(session.contains("persist-session"), "chat session should survive gateway restart: " + session);
            require(knowledge.contains("persisted_fact"), "knowledge update should survive gateway restart: " + knowledge);
        }
    }

    private static String send(HttpClient client, HttpRequest request) throws Exception {
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new AssertionError(request.uri() + " returned HTTP " + response.statusCode() + ": " + response.body());
        }
        return response.body();
    }

    private static HttpResponse<String> sendRaw(HttpClient client, HttpRequest request) throws Exception {
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
