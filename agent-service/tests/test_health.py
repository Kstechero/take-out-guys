from fastapi.testclient import TestClient

from app.main import create_app


def test_health_check() -> None:
    client = TestClient(create_app())

    response = client.get("/health")

    assert response.status_code == 200
    assert response.json()["status"] == "ok"


def test_metrics_endpoint_exposes_prometheus_counters() -> None:
    client = TestClient(create_app())
    client.get("/health")
    response = client.get("/metrics")
    assert response.status_code == 200
    assert "agent_http_requests_total" in response.text
