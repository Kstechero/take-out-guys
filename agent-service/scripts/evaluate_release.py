from __future__ import annotations

import json
import statistics
import tempfile
import time
from datetime import UTC, datetime, timedelta
from pathlib import Path

import httpx

from app.core.config import Settings
from app.rag.retriever import build_knowledge_retriever
from app.schemas.chat import ActorContext


def percentile(values: list[float], ratio: float) -> float:
    ordered = sorted(values)
    return ordered[min(len(ordered) - 1, int(len(ordered) * ratio))]


def main() -> None:
    root = Path(__file__).resolve().parents[2]
    cases = [
        json.loads(line)
        for line in (root / "agent-service/evals/rag_cases.jsonl")
        .read_text(encoding="utf-8").splitlines()
        if line.strip()
    ]
    with tempfile.TemporaryDirectory(prefix="agent-eval-") as temp_dir:
        settings = Settings(
            rag_knowledge_dir=str(root / "docs/agent-service/knowledge"),
            rag_index_path=str(Path(temp_dir) / "index.json"),
            rag_top_k=5,
        )
        retriever = build_knowledge_retriever(settings)
        ranks: list[int] = []
        misses = 0
        refusal_pass = 0
        visibility_pass = 0
        latencies: list[float] = []
        for case in cases:
            actor = ActorContext(type=case["actor"], id="eval", roles=[case["actor"].upper()])
            started = time.perf_counter()
            matches = retriever.search(case["query"], actor=actor, domain=case.get("domain"))
            latencies.append((time.perf_counter() - started) * 1000)
            if all(match.chunk.visibility in {"public", case["actor"]} for match in matches):
                visibility_pass += 1
            expected = case["expected_source"]
            if expected is None:
                refusal_pass += int(not matches)
                continue
            sources = [match.chunk.source for match in matches]
            if expected in sources:
                ranks.append(sources.index(expected) + 1)
            else:
                misses += 1
        expected_count = sum(case["expected_source"] is not None for case in cases)
        refusal_count = len(cases) - expected_count
        tool_paths = [
            ("user", "/internal/agent/shop/status"),
            ("user", "/internal/agent/menu/search?query=%E6%8E%A8%E8%8D%90&limit=5"),
            ("user", "/internal/agent/orders/recent?limit=5"),
            ("user", "/internal/agent/cart"),
            ("user", "/internal/agent/addresses"),
            ("user", "/internal/agent/coupons/available"),
            ("admin", "/internal/agent/admin/business/overview"),
            ("admin", "/internal/agent/admin/orders?status=2&limit=5"),
            ("admin", "/internal/agent/admin/menu?limit=5"),
            ("admin", "/internal/agent/admin/setmeals?limit=5"),
            ("admin", "/internal/agent/admin/coupons?limit=5"),
            ("admin", "/internal/agent/admin/reviews?limit=5"),
        ]
        tool_latencies: list[float] = []
        tool_success = 0
        with httpx.Client(base_url=settings.spring_internal_base_url, timeout=10) as client:
            for index, (actor_type, path) in enumerate(tool_paths):
                headers = {
                    "X-Request-Id": f"release-tool-{index}",
                    "X-Actor-Type": actor_type,
                    "X-Actor-Id": "1",
                    "X-Actor-Roles": actor_type.upper(),
                    "X-Actor-Expires-At": (datetime.now(UTC) + timedelta(minutes=5)).isoformat(),
                    "X-Agent-Service-Token": settings.spring_internal_auth_token,
                }
                started = time.perf_counter()
                response = client.get(path, headers=headers)
                tool_latencies.append((time.perf_counter() - started) * 1000)
                tool_success += int(response.is_success)
        report = {
            "total_cases": len(cases),
            "recall_at_1": sum(rank <= 1 for rank in ranks) / expected_count,
            "recall_at_3": sum(rank <= 3 for rank in ranks) / expected_count,
            "recall_at_5": sum(rank <= 5 for rank in ranks) / expected_count,
            "mrr": statistics.mean(1 / rank for rank in ranks) if ranks else 0,
            "visibility_accuracy": visibility_pass / len(cases),
            "no_evidence_refusal_rate": refusal_pass / refusal_count if refusal_count else 1,
            "retrieval_p95_ms": percentile(latencies, 0.95),
            "tool_calls": len(tool_paths),
            "tool_success_rate": tool_success / len(tool_paths),
            "tool_p95_ms_excluding_model": percentile(tool_latencies, 0.95),
            "misses": misses,
        }
        print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
