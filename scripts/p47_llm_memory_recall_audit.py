#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def require(name: str, text: str, *needles: str) -> None:
    missing = [needle for needle in needles if needle not in text]
    if missing:
        raise AssertionError(f"{name} missing: {missing}")


def require_order(name: str, text: str, before: str, after: str) -> None:
    left = text.find(before)
    right = text.find(after)
    if left < 0 or right < 0 or left >= right:
        raise AssertionError(f"{name} expected {before!r} before {after!r}")


def main() -> None:
    gateway = read("ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/GatewayServer.java")
    guard = read("ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/GatewayAuthGuard.java")
    config = read("ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/GatewayConfig.java")
    store = read("ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/memory/MemoryStore.java")
    renderer = read("ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/memory/MemoryPromptRenderer.java")
    recall = read("ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/memory/MemoryRecall.java")
    extractor = read("ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/memory/LlmMemoryOperationExtractor.java")
    fact_extractor = read("ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/memory/MemoryFactExtractor.java")
    validator = read("ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/memory/DeterministicMemoryValidator.java")
    operation = read("ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/memory/MemoryOperation.java")
    migration = read("ebb-llm-gateway/src/main/resources/db/migration/V001__memory_store.sql")
    fake = read("ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/chat/FakeGatewayChatProvider.java")
    smoke = read("ebb-llm-gateway/src/test/java/com/crpg/ebb/gateway/GatewaySmoke.java")
    guide = read("docs/json_authoring_guide.md")
    status = read("docs/current_status.md")
    remediation = read("docs/p47_llm_memory_review_remediation_2026-06-20.md")

    require("P47 recall record", recall, "activeFacts", "openConflicts", "safetyLessons", "episodes")
    require("P47 recall store", store,
            "public MemoryRecall recall", "activeFactsFor", "openConflictsFor", "safetyLessonsFor",
            "status IN ('current', 'active')", "authority_rank DESC", "search(request)")
    require("P47 chat-before-provider retrieval", gateway,
            "memoryStore.recall", "MemoryPromptRenderer.render", "withMemoryContext", "withValidatedMemoryCitations", "chatProvider.send(enrichedRequest)")
    require_order("P47 retrieval before provider", gateway, "memoryStore.recall", "chatProvider.send(enrichedRequest)")
    require("P47 prompt renderer", renderer,
            "Active facts", "Open/recorded memory conflicts", "Safety/correction lessons", "Relevant remembered episodes", "citationIds")

    require("P47 memory_ops extractor", extractor,
            "JsonParser", "memory_ops", "structuredMemoryOps", "unsupported_memory_op", "parseStructuredOp", "subjectAlias")
    require("P47 legacy fact subject aliases", extractor + fact_extractor,
            "lower.equals(\"player\")", "lower.equals(\"self\")", "lower.equals(\"me\")", "request.npcKey", "request.entityUuid")
    require("P47 validator hardening", validator + operation,
            "CORRECT_FACT", "unsupported_memory_op", "high_risk_memory_op_rejected", "correction_requires_explicit_memory_correct_endpoint")

    require("P47 correction semantics", store,
            "replacement_fact_id", "superseded_fact_id", "correction_conflict_id", "manual_player_correction", "resolved_correction", "replacementFact")
    require("P47 auth guard", gateway + guard,
            "GatewayAuthGuard", "Authorization", "Bearer ", "wrong_player_token", "forbidden_scope", "authHttpStatus(auth)", "return reason.startsWith(\"wrong_player_token\")")
    require("P47 quota", config + gateway + store + read("ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/memory/QuotaDecision.java"),
            "EBB_GATEWAY_PLAYER_DAILY_LIMIT", "reserveQuota", "remaining", "daily_remaining", "daily_reset_epoch_ms", "quota_windows")
    require("P47 persisted gateway state", migration + store,
            "npc_profiles", "chat_sessions", "npc_knowledge_updates", "quota_windows",
            "saveNpcProfile", "saveChatSession", "addKnowledgeUpdate", "privacy_deleted_at")

    require("P47 acceptance smoke", smoke + fake,
            "runStructuredMemoryOpsSmoke", "runGatewayPersistenceSmoke", "fact:player.hometown=Riverside",
            "memory=recall", "Riverside", "daily_remaining", "wrongPlayer.statusCode() == 403", "abbreviate(request.memoryContext(), 360)")
    require("P47 docs", guide + status + remediation,
            "P47 LLM Memory Recall Hardening", "memory_ops", "Gateway/OIDC/OpenAI staging checklist",
            "Prompt injection behavior", "LLM output boundaries", "current_project_review_llm_memory_2026-06-19.md")

    print("P47 LLM memory recall hardening audit passed")


if __name__ == "__main__":
    main()
