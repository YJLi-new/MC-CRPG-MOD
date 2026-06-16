#!/usr/bin/env python3
"""P43 LLM/NPC memory safety static audits.

Checks:
- no literal API keys/tokens are committed in tracked project files,
- tracked tests/smokes use fake or mock LLM providers instead of real OpenAI,
- hidden NPC knowledge is not serialized through client/sync payload code,
- high-risk Minecraft effects are not accepted from direct LLM `proposed_effects` output.
"""
from __future__ import annotations

import re
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

SECRET_PATTERNS = [
    re.compile(r"\bsk-[A-Za-z0-9_-]{20,}\b"),
    re.compile(r"\bgh[oprsu]_[A-Za-z0-9_]{20,}\b"),
    re.compile(r"\bAIza[0-9A-Za-z_-]{20,}\b"),
    re.compile(r"Bearer\s+[A-Za-z0-9._~+/-]{20,}", re.IGNORECASE),
]
TRACKED_EXTENSIONS = {
    ".java", ".py", ".sh", ".json", ".md", ".kts", ".gradle", ".properties", ".yml", ".yaml", ".toml", ".txt"
}
SKIP_PARTS = {".gradle", "build", "node_modules", ".git"}


def tracked_files() -> list[Path]:
    output = subprocess.check_output(["git", "ls-files", "--cached", "--others", "--exclude-standard"], cwd=ROOT, text=True)
    files: list[Path] = []
    for raw in output.splitlines():
        path = ROOT / raw
        if not path.is_file():
            continue
        if any(part in SKIP_PARTS for part in path.relative_to(ROOT).parts):
            continue
        if path.suffix.lower() in TRACKED_EXTENSIONS or path.name in {"gradlew", "LICENSE", "README"}:
            files.append(path)
    return files


def text(path: Path) -> str:
    return path.read_text(encoding="utf-8", errors="replace")


def fail(message: str) -> None:
    raise AssertionError(message)


def audit_no_api_key_literals(files: list[Path]) -> None:
    for path in files:
        body = text(path)
        for pattern in SECRET_PATTERNS:
            match = pattern.search(body)
            if match:
                fail(f"secret-like API key/token literal in {path.relative_to(ROOT)}: {match.group(0)[:8]}…")


def audit_fake_provider_in_tests() -> None:
    test_roots = [
        ROOT / "src/test",
        ROOT / "ebb-llm-gateway/src/test",
        ROOT / "scripts",
    ]
    joined = "\n".join(
        text(path)
        for root in test_roots if root.exists()
        for path in root.rglob("*")
        if path.is_file()
        and path.suffix.lower() in TRACKED_EXTENSIONS
        and path.name != Path(__file__).name
    )
    if "FAKE_NPC_REPLY" not in joined or "FAKE_GATEWAY_REPLY" not in joined:
        fail("tests/smokes must exercise fake LLM providers")
    if "mock_openai_responses" not in joined:
        fail("tests/smokes must use mock_openai_responses for OpenAI-shaped coverage")
    real_provider = re.search(r"EBB_GATEWAY_CHAT_PROVIDER[^\n]*[=:,]\s*['\"]?openai_responses['\"]?", joined)
    if real_provider:
        fail("tests/smokes must not select the real openai_responses provider")


def audit_hidden_knowledge_not_in_client_sync() -> None:
    payload_roots = [
        ROOT / "src/client/java",
        ROOT / "src/main/java/com/crpg/ebb/network",
    ]
    forbidden = [
        "NpcKnowledgePackDefinition",
        "NpcKnowledgeService",
        "npc_knowledge_packs",
        "hiddenChunks",
        "secret_ledger_tenant_cash",
        "tenant paid cash",
    ]
    for root in payload_roots:
        if not root.exists():
            continue
        for path in root.rglob("*.java"):
            body = text(path)
            for token in forbidden:
                if token in body:
                    fail(f"hidden NPC knowledge token {token!r} found in client/sync payload surface {path.relative_to(ROOT)}")


def audit_high_risk_effects_not_direct_llm_output() -> None:
    gateway_response = text(ROOT / "ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/chat/GatewayChatResponse.java")
    for token in ["sanitizeProposedEffects", "containsHighRiskEffectVerb", "high_risk_effects_rejected_from_llm_output", "set_flag", "complete_quest_branch", "give_item", "set_npc_routine", "reveal_clue"]:
        if token not in gateway_response:
            fail(f"GatewayChatResponse missing direct-LLM high-risk effect guard marker: {token}")
    minecraft_response = text(ROOT / "src/main/java/com/crpg/ebb/llm/LlmChatResponse.java")
    minecraft_client = text(ROOT / "src/main/java/com/crpg/ebb/llm/HttpLlmGatewayClient.java")
    if "proposedEffects" in minecraft_response or "proposed_effects" in minecraft_client:
        fail("Minecraft-side LlmChatResponse/HttpLlmGatewayClient must not expose or apply gateway proposed_effects")
    openai_provider = text(ROOT / "ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/chat/OpenAiResponsesChatProvider.java")
    if '"proposed_effects"' in openai_provider:
        fail("OpenAI structured schema must not request direct proposed_effects from the model")


def main() -> int:
    files = tracked_files()
    audit_no_api_key_literals(files)
    audit_fake_provider_in_tests()
    audit_hidden_knowledge_not_in_client_sync()
    audit_high_risk_effects_not_direct_llm_output()
    print("P43LlmSafetyAudit passed: no secret literals, fake/mock tests, no hidden KB client sync, and high-risk direct LLM effects are rejected/ignored.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
