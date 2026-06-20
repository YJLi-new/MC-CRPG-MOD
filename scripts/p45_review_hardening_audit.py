#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def require(name: str, text: str, *needles: str) -> None:
    missing = [needle for needle in needles if needle not in text]
    if missing:
        raise AssertionError(f"{name} missing: {missing}")


def main() -> None:
    gateway = read("ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/GatewayServer.java")
    auth_guard = read("ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/GatewayAuthGuard.java")
    config = read("ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/GatewayConfig.java")
    request = read("ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/chat/GatewayChatRequest.java")
    response = read("ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/chat/GatewayChatResponse.java")
    memory = read("ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/memory/MemoryStore.java")
    fact = read("ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/memory/MemoryFact.java")
    policy = read("ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/memory/MemoryAuthorityPolicy.java")
    smoke = read("ebb-llm-gateway/src/test/java/com/crpg/ebb/gateway/GatewaySmoke.java")
    llm_config = read("src/main/java/com/crpg/ebb/llm/LlmConfig.java")
    llm_service = read("src/main/java/com/crpg/ebb/llm/LlmChatService.java")
    llm_world = read("src/main/java/com/crpg/ebb/llm/LlmWorldIdentity.java")
    http_client = read("src/main/java/com/crpg/ebb/llm/HttpLlmGatewayClient.java")
    gui = read("scripts/gui_e2e_run.py")

    require("P45 gateway auth config", config,
            "EBB_GATEWAY_REQUIRE_PLAYER_TOKEN_FOR_CHAT",
            "EBB_GATEWAY_REQUIRE_SERVER_TOKEN_FOR_ADMIN_ENDPOINTS",
            "EBB_GATEWAY_SERVER_SHARED_SECRET",
            "localDevBlankTokenAllowed")
    require("P45/P47 gateway auth gates", gateway + auth_guard,
            "authorizePlayer", "tokenValidForPlayer", "wrong_player_token",
            "authorizeServer", "server_token_required", "handleMemoryInspect",
            "handleKnowledgeUpdate", "handleNpcProfileGenerate")
    require("P45 memory prompt retrieval", gateway + request + response,
            "MemorySearchRequest.forChat", "MemoryPromptRenderer.render",
            "withMemoryContext", "MEMORY CONTEXT", "Allowed memory citations",
            "withValidatedMemoryCitations", "invalid_memory_citation_rejected")
    require("P45 rate limits", config + gateway + llm_config + llm_service,
            "EBB_GATEWAY_PLAYER_RATE_LIMIT_PER_MINUTE", "GatewayRateLimiter",
            "rate_limited", "rateLimitPerMinute", "RATE_WINDOWS")
    require("P45 world identity", llm_config + llm_world + http_client + llm_service,
            "server_id", "world_id_strategy", "world_id_override",
            "LlmWorldIdentity.serverId", "LlmWorldIdentity.worldId")
    if '"minecraft-server"' in http_client or '"minecraft-world"' in http_client:
        raise AssertionError("P45 HttpLlmGatewayClient must not hard-code minecraft-server/minecraft-world")
    require("P45 authority policy", fact + policy + memory,
            "source_type", "authority_rank", "certainty", "visibility",
            "SYSTEM_CANON", "PLAYER_CLAIM", "LLM_INFERRED", "canSupersede")
    require("P45/P47 profile endpoint and proof smoke", gateway + smoke,
            "/v1/npc/profile/generate", "validated_no_hidden_kb_leak",
            "blank token should be rejected", "wrong player token should be rejected",
            "memory=recall", "memory:", "rate_limited")
    require("P45 GUI proof manifest", gui,
            "memory_proof", "expected-p45-memory-proof-manifest.json",
            "player_claim_written_to_gateway_memory",
            "second_chat_retrieves_memory_context_before_provider")
    print("P45 review hardening audit passed")


if __name__ == "__main__":
    main()
