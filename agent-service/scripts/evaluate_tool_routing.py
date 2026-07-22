from __future__ import annotations

import json
import time
from datetime import UTC, datetime, timedelta
from pathlib import Path

import httpx

from app.core.config import settings


def main() -> None:
    cases_path = Path(__file__).resolve().parents[1] / "evals/tool_routing_cases.jsonl"
    cases = [json.loads(line) for line in cases_path.read_text(encoding="utf-8").splitlines()]
    passed = 0
    durations: list[float] = []
    failed: list[dict[str, object]] = []
    run_id = int(time.time())
    headers = {"X-Agent-Service-Token": settings.agent_service_auth_token}
    with httpx.Client(base_url="http://127.0.0.1:8000", timeout=180) as client:
        for index, case in enumerate(cases, 1):
            actor_type = case["actor"]
            payload = {
                "request_id": f"routing-{case['id']}",
                "actor": {
                    "type": actor_type, "id": "1", "roles": [actor_type.upper()],
                    "expires_at": (datetime.now(UTC) + timedelta(minutes=5)).isoformat(),
                },
                "session_id": f"routing-eval-{run_id}-{index}",
                "message": case["message"],
            }
            started = time.perf_counter()
            response = client.post(f"/v1/{actor_type}/chat/stream", headers=headers, json=payload)
            durations.append((time.perf_counter() - started) * 1000)
            tools: list[str] = []
            event = ""
            for line in response.text.splitlines():
                if line.startswith("event:"):
                    event = line[6:].strip()
                elif line.startswith("data:") and event == "tool_started":
                    tools.append(str(json.loads(line[5:].strip()).get("tool")))
            if response.is_success and case["expected_tool"] in tools:
                passed += 1
            else:
                failed.append({"id": case["id"], "expected": case["expected_tool"], "actual": tools})
    ordered = sorted(durations)
    p95 = ordered[min(len(ordered) - 1, int(len(ordered) * 0.95))]
    print(json.dumps({
        "total": len(cases), "passed": passed, "accuracy": passed / len(cases),
        "end_to_end_p95_ms": p95, "failed": failed,
    }, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
