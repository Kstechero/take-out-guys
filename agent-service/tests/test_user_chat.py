from fastapi.testclient import TestClient

from app.main import create_app


def test_user_chat_placeholder() -> None:
    client = TestClient(create_app())

    response = client.post(
        "/v1/user/chat",
        json={
            "request_id": "req-1",
            "actor": {"type": "user", "id": "1001", "roles": ["customer"]},
            "message": "帮我推荐一份午餐",
        },
    )

    assert response.status_code == 202
    assert response.json()["request_id"] == "req-1"
