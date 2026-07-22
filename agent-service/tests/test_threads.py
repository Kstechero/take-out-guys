from __future__ import annotations

from typing import Any

from fastapi.testclient import TestClient

from app.confirmations import ConfirmationService, ConfirmationStore
from app.core.config import Settings
from app.dependencies import get_admin_operations_graph, get_user_support_graph
from app.graphs.user_support import UserSupportAgentGraph
from app.main import create_app
from app.schemas.chat import ActorContext
from app.security.service_auth import require_agent_api_token


class EmptyRegistry:
    def langchain_tools(self, **_kwargs: Any) -> list[object]:
        return []


class FakeWriteClient:
    def __init__(self) -> None:
        self.calls = 0

    async def change_cart(self, **_kwargs: Any) -> dict[str, Any]:
        self.calls += 1
        return {"status": "APPLIED"}

    async def claim_coupon(self, **_kwargs: Any) -> dict[str, Any]:
        self.calls += 1
        return {"status": "CLAIMED"}


def setup_graph() -> tuple[UserSupportAgentGraph, FakeWriteClient, str]:
    actor = ActorContext(type="user", id="1001", roles=["USER"])
    client = FakeWriteClient()
    confirmations = ConfirmationService(
        store=ConfirmationStore(":memory:"),
        client=client,  # type: ignore[arg-type]
    )
    proposal = confirmations.propose_user_action(
        actor=actor,
        session_id="session-1",
        action="clear",
        arguments={},
        summary="确认清空购物车吗？",
    )
    token = str(proposal["confirmation"]["token"])
    graph = UserSupportAgentGraph(
        settings=Settings(),
        tools=EmptyRegistry(),  # type: ignore[arg-type]
        model=None,
        confirmations=confirmations,
    )
    return graph, client, token


def test_resume_endpoint_approves_pending_user_action_once() -> None:
    graph, write_client, token = setup_graph()
    app = create_app()
    app.dependency_overrides[get_user_support_graph] = lambda: graph
    app.dependency_overrides[get_admin_operations_graph] = lambda: graph
    app.dependency_overrides[require_agent_api_token] = lambda: None
    client = TestClient(app)
    payload = {
        "request_id": "req-resume",
        "agent_name": "user_support_agent",
        "actor": {"type": "user", "id": "1001", "roles": ["USER"]},
        "confirmation_token": token,
        "decision": "approve",
    }

    first = client.post("/v1/threads/session-1/resume", json=payload)
    second = client.post("/v1/threads/session-1/resume", json=payload)

    assert first.status_code == 200
    assert second.status_code == 200
    assert write_client.calls == 1
    assert "已完成" in first.json()["answer"]
    assert "此前已完成" in second.json()["answer"]


def test_resume_endpoint_rejects_actor_swap() -> None:
    graph, write_client, token = setup_graph()
    app = create_app()
    app.dependency_overrides[get_user_support_graph] = lambda: graph
    app.dependency_overrides[get_admin_operations_graph] = lambda: graph
    app.dependency_overrides[require_agent_api_token] = lambda: None
    client = TestClient(app)

    response = client.post(
        "/v1/threads/session-1/resume",
        json={
            "request_id": "req-swap",
            "agent_name": "user_support_agent",
            "actor": {"type": "user", "id": "1002", "roles": ["USER"]},
            "confirmation_token": token,
            "decision": "approve",
        },
    )

    assert response.status_code == 200
    assert "不匹配" in response.json()["answer"]
    assert write_client.calls == 0
