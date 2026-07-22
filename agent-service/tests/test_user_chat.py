from fastapi.testclient import TestClient

from app.dependencies import get_user_support_graph
from app.main import create_app
from app.schemas.chat import ChatRequest, ChatResponse
from app.security.service_auth import require_agent_api_token
from app.clients.spring_internal import SpringInternalApiClient
from app.schemas.user_tools import ReviewDraftCheckResult


class StubGraph:
    model = None

    async def run(self, request: ChatRequest) -> ChatResponse:
        return ChatResponse(
            request_id=request.request_id,
            session_id=request.session_id or "user-1001",
            answer="stubbed response",
            trace_id="trace-1",
        )

    async def stream(self, request: ChatRequest):
        yield "delta", {"text": "流式"}
        yield "delta", {"text": "回答"}
        yield "done", {"session_id": request.session_id or "user-1001", "trace_id": "trace-1"}


def test_user_chat_uses_graph_dependency() -> None:
    app = create_app()
    app.dependency_overrides[get_user_support_graph] = lambda: StubGraph()
    app.dependency_overrides[require_agent_api_token] = lambda: None
    client = TestClient(app)

    response = client.post(
        "/v1/user/chat",
        json={
            "request_id": "req-1",
            "actor": {"type": "user", "id": "1001", "roles": ["customer"]},
            "message": "\u5e2e\u6211\u63a8\u8350\u4e00\u4efd\u5348\u9910",
        },
    )

    assert response.status_code == 202
    assert response.json() == {
        "request_id": "req-1",
        "session_id": "user-1001",
        "answer": "stubbed response",
        "status": "completed",
        "citations": [],
        "suggested_actions": [],
        "confirmation": None,
        "trace_id": "trace-1",
    }


def test_user_chat_rejects_invalid_service_token() -> None:
    app = create_app()
    app.dependency_overrides[get_user_support_graph] = lambda: StubGraph()

    async def reject_token() -> None:
        from fastapi import HTTPException

        raise HTTPException(status_code=401, detail="Invalid agent service token")

    app.dependency_overrides[require_agent_api_token] = reject_token
    client = TestClient(app)

    response = client.post(
        "/v1/user/chat",
        json={
            "request_id": "req-auth",
            "actor": {"type": "user", "id": "1001", "roles": ["USER"]},
            "message": "现在营业吗",
        },
    )

    assert response.status_code == 401


def test_user_chat_stream_returns_sse_events() -> None:
    app = create_app()
    app.dependency_overrides[get_user_support_graph] = lambda: StubGraph()
    app.dependency_overrides[require_agent_api_token] = lambda: None
    client = TestClient(app)

    with client.stream(
        "POST",
        "/v1/user/chat/stream",
        json={
            "request_id": "req-stream",
            "actor": {"type": "user", "id": "1001", "roles": ["USER"]},
            "session_id": "session-1",
            "message": "你好",
        },
    ) as response:
        body = "".join(response.iter_text())

    assert response.status_code == 200
    assert "event: delta" in body
    assert 'data: {"text":"流式"}' in body
    assert "event: done" in body


def test_structured_recommendation_contract_uses_realtime_menu(monkeypatch) -> None:
    async def fake_menu(self, **kwargs):
        return {"items": [{"type": "dish", "id": 9, "name": "宫保鸡丁", "price": "18.00"}]}
    monkeypatch.setattr(SpringInternalApiClient, "menu_search", fake_menu)
    app = create_app()
    app.dependency_overrides[get_user_support_graph] = lambda: StubGraph()
    app.dependency_overrides[require_agent_api_token] = lambda: None
    response = TestClient(app).post("/v1/user/recommendations", json={
        "request_id": "req-rec", "actor": {"type": "user", "id": "1001"},
        "requirement": "推荐不辣午餐", "budget": 30, "people_count": 1
    })
    assert response.status_code == 200
    assert response.json()["items"][0]["id"] == 9


def test_structured_review_draft_contract(monkeypatch) -> None:
    async def fake_check(self, **kwargs):
        return ReviewDraftCheckResult(
            eligible=True, safe=True, order_id=10, dish_id=9, dish_name="宫保鸡丁",
            rating=5, highlights="味道很好", instruction="generate"
        )
    monkeypatch.setattr(SpringInternalApiClient, "check_review_draft", fake_check)
    app = create_app()
    app.dependency_overrides[get_user_support_graph] = lambda: StubGraph()
    app.dependency_overrides[require_agent_api_token] = lambda: None
    response = TestClient(app).post("/v1/user/reviews/draft", json={
        "request_id": "req-review", "actor": {"type": "user", "id": "1001"},
        "order_id": 10, "dish_id": 9, "rating": 5, "highlights": "味道很好"
    })
    assert response.status_code == 200
    assert response.json()["publish_status"] == "draft_only"
