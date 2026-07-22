from fastapi.testclient import TestClient

from app.dependencies import get_admin_operations_graph
from app.main import create_app
from app.schemas.chat import ChatRequest, ChatResponse
from app.security.service_auth import require_agent_api_token


class StubGraph:
    async def run(self, request: ChatRequest) -> ChatResponse:
        return ChatResponse(
            request_id=request.request_id,
            session_id=request.session_id,
            answer="admin response",
        )


def test_admin_chat_accepts_admin_actor() -> None:
    app = create_app()
    app.dependency_overrides[get_admin_operations_graph] = lambda: StubGraph()
    app.dependency_overrides[require_agent_api_token] = lambda: None
    client = TestClient(app)

    response = client.post(
        "/v1/admin/chat",
        json={
            "request_id": "req-admin",
            "actor": {"type": "admin", "id": "7", "roles": ["ADMIN"]},
            "session_id": "10",
            "message": "查询营业状态",
        },
    )

    assert response.status_code == 202
    assert response.json()["answer"] == "admin response"


def test_admin_chat_rejects_user_actor() -> None:
    app = create_app()
    app.dependency_overrides[get_admin_operations_graph] = lambda: StubGraph()
    app.dependency_overrides[require_agent_api_token] = lambda: None
    client = TestClient(app)

    response = client.post(
        "/v1/admin/chat",
        json={
            "request_id": "req-user",
            "actor": {"type": "user", "id": "1001", "roles": ["USER"]},
            "message": "查询营业状态",
        },
    )

    assert response.status_code == 403
