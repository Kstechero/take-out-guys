from decimal import Decimal
from typing import Any

import pytest
import httpx

from app.clients.spring_internal import SpringInternalApiClient
from app.core.config import Settings
from app.schemas.chat import ActorContext


def actor() -> ActorContext:
    return ActorContext(type="user", id="1001", roles=["USER"])


def admin_actor() -> ActorContext:
    return ActorContext(type="admin", id="7", roles=["ADMIN"])


async def test_recent_orders_omits_empty_status(monkeypatch: Any) -> None:
    captured: dict[str, Any] = {}

    async def fake_request(
        self: SpringInternalApiClient,
        method: str,
        path: str,
        **kwargs: Any,
    ) -> dict[str, Any]:
        captured.update(method=method, path=path, **kwargs)
        return {"items": [], "total": 0}

    monkeypatch.setattr(SpringInternalApiClient, "_request", fake_request)
    client = SpringInternalApiClient(Settings())

    result = await client.list_recent_orders(
        request_id="req-orders",
        actor=actor(),
        status=None,
        limit=5,
    )

    assert result.total == 0
    assert captured["params"] == {"limit": 5}


async def test_available_coupons_serializes_decimal_amount(monkeypatch: Any) -> None:
    captured: dict[str, Any] = {}

    async def fake_request(
        self: SpringInternalApiClient,
        method: str,
        path: str,
        **kwargs: Any,
    ) -> dict[str, Any]:
        captured.update(method=method, path=path, **kwargs)
        return {"items": [], "order_amount": "35.50"}

    monkeypatch.setattr(SpringInternalApiClient, "_request", fake_request)
    client = SpringInternalApiClient(Settings())

    result = await client.list_available_coupons(
        request_id="req-coupons",
        actor=actor(),
        order_amount=Decimal("35.50"),
    )

    assert result.order_amount == Decimal("35.50")
    assert captured["params"] == {"order_amount": "35.50"}


async def test_review_draft_uses_typed_validation_contract(monkeypatch: Any) -> None:
    captured: dict[str, Any] = {}

    async def fake_request(self: SpringInternalApiClient, method: str, path: str, **kwargs: Any):
        captured.update(method=method, path=path, **kwargs)
        return {
            "eligible": True, "safe": True, "order_id": 10, "dish_id": 9,
            "dish_name": "宫保鸡丁", "rating": 5, "highlights": "味道很好",
            "instruction": "generate draft"
        }

    monkeypatch.setattr(SpringInternalApiClient, "_request", fake_request)
    result = await SpringInternalApiClient(Settings()).check_review_draft(
        request_id="req-review", actor=actor(), order_id=10, dish_id=9,
        rating=5, highlights="味道很好"
    )
    assert result.eligible and result.safe
    assert captured["path"] == "/internal/agent/reviews/draft/check"
    assert captured["json_body"]["dish_id"] == 9


async def test_admin_business_overview_uses_contract_route(monkeypatch: Any) -> None:
    captured: dict[str, Any] = {}

    async def fake_request(
        self: SpringInternalApiClient,
        method: str,
        path: str,
        **kwargs: Any,
    ) -> dict[str, Any]:
        captured.update(method=method, path=path, **kwargs)
        return {
            "begin": "2026-07-01",
            "end": "2026-07-21",
            "turnover": 100,
            "valid_order_count": 5,
            "order_completion_rate": 0.5,
            "unit_price": 20,
            "new_users": 2,
            "waiting_orders": 1,
            "delivered_orders": 1,
            "completed_orders": 5,
            "cancelled_orders": 1,
            "all_orders": 8,
            "generated_at": "2026-07-21T12:00:00",
            "scope": "single_store,date=2026-07-01..2026-07-21",
            "source": "spring_internal_api",
        }

    monkeypatch.setattr(SpringInternalApiClient, "_request", fake_request)
    client = SpringInternalApiClient(Settings())

    result = await client.get_admin_business_overview(
        request_id="req-admin-overview",
        actor=admin_actor(),
        begin="2026-07-01",
        end="2026-07-21",
    )

    assert result.valid_order_count == 5
    assert captured["method"] == "GET"
    assert captured["path"] == "/internal/agent/admin/business/overview"
    assert captured["params"] == {"begin": "2026-07-01", "end": "2026-07-21"}


async def test_admin_category_search_uses_contract_route(monkeypatch: Any) -> None:
    captured: dict[str, Any] = {}

    async def fake_request(
        self: SpringInternalApiClient,
        method: str,
        path: str,
        **kwargs: Any,
    ) -> dict[str, Any]:
        captured.update(method=method, path=path, **kwargs)
        return {
            "items": [{"id": 21, "name": "汤类", "type": 1, "status": 1, "sort": 11}],
            "total": 1,
            "generated_at": "2026-07-22T12:00:00",
            "scope": "single_store",
            "source": "spring_internal_api",
        }

    monkeypatch.setattr(SpringInternalApiClient, "_request", fake_request)
    result = await SpringInternalApiClient(Settings()).admin_category_search(
        request_id="req-category",
        actor=admin_actor(),
        params={"query": "汤", "type": 1, "status": 1, "limit": 20},
    )

    assert result.items[0].id == 21
    assert captured["method"] == "GET"
    assert captured["path"] == "/internal/agent/admin/categories"


async def test_cart_write_sends_confirmation_and_idempotency_headers(monkeypatch: Any) -> None:
    captured: dict[str, Any] = {}

    async def fake_request(
        self: SpringInternalApiClient,
        method: str,
        path: str,
        **kwargs: Any,
    ) -> dict[str, Any]:
        captured.update(method=method, path=path, **kwargs)
        return {"status": "APPLIED"}

    monkeypatch.setattr(SpringInternalApiClient, "_request", fake_request)
    client = SpringInternalApiClient(Settings())

    await client.change_cart(
        request_id="req-write",
        actor=actor(),
        payload={"action": "clear"},
        confirmation_token="confirmation-token",
        idempotency_key="idempotency-key",
    )

    assert captured["method"] == "POST"
    assert captured["path"] == "/internal/agent/cart/changes"
    assert captured["extra_headers"] == {
        "X-Confirmation-Token": "confirmation-token",
        "Idempotency-Key": "idempotency-key",
    }


async def test_read_retries_once_on_transport_error(monkeypatch: Any) -> None:
    calls = 0

    class FakeClient:
        def __init__(self, **_: Any) -> None:
            pass

        async def __aenter__(self) -> "FakeClient":
            return self

        async def __aexit__(self, *_: Any) -> None:
            return None

        async def request(self, *_: Any, **__: Any) -> httpx.Response:
            nonlocal calls
            calls += 1
            if calls == 1:
                raise httpx.ConnectError("temporary")
            return httpx.Response(
                200,
                json={"ok": True, "request_id": "req-read", "data": {"status": "OPEN"}},
            )

    monkeypatch.setattr(httpx, "AsyncClient", FakeClient)
    result = await SpringInternalApiClient(Settings()).get_shop_status(
        request_id="req-read", actor=actor()
    )
    assert result == {"status": "OPEN"}
    assert calls == 2


async def test_write_does_not_retry_on_transport_error(monkeypatch: Any) -> None:
    calls = 0

    class FakeClient:
        def __init__(self, **_: Any) -> None:
            pass

        async def __aenter__(self) -> "FakeClient":
            return self

        async def __aexit__(self, *_: Any) -> None:
            return None

        async def request(self, *_: Any, **__: Any) -> httpx.Response:
            nonlocal calls
            calls += 1
            raise httpx.ConnectError("temporary")

    monkeypatch.setattr(httpx, "AsyncClient", FakeClient)
    with pytest.raises(Exception):
        await SpringInternalApiClient(Settings()).change_cart(
            request_id="req-write", actor=actor(), payload={"action": "clear"},
            confirmation_token="token", idempotency_key="key"
        )
    assert calls == 1


async def test_circuit_breaker_opens_after_repeated_failures(monkeypatch: Any) -> None:
    calls = 0

    class FakeClient:
        def __init__(self, **_: Any) -> None: pass
        async def __aenter__(self): return self
        async def __aexit__(self, *_: Any) -> None: return None
        async def request(self, *_: Any, **__: Any):
            nonlocal calls
            calls += 1
            raise httpx.ConnectError("down")

    monkeypatch.setattr(httpx, "AsyncClient", FakeClient)
    client = SpringInternalApiClient(Settings(circuit_breaker_failure_threshold=2))
    for _ in range(2):
        with pytest.raises(Exception):
            await client.change_cart(
                request_id="req", actor=actor(), payload={"action": "clear"},
                confirmation_token="token", idempotency_key="key"
            )
    with pytest.raises(Exception) as exc:
        await client.get_shop_status(request_id="req", actor=actor())
    assert "circuit breaker" in str(exc.value)
    assert calls == 2
