from __future__ import annotations

from dataclasses import dataclass
from decimal import Decimal
from typing import Any
import time

import httpx

from app.core.config import Settings
from app.schemas.chat import ActorContext
from app.schemas.admin_tools import (
    AdminCategorySearchResult,
    AdminCatalogSearchResult,
    AdminCouponSearchResult,
    AdminOrderSearchResult,
    AdminOrderSummary,
    AdminReviewSearchResult,
)
from app.schemas.internal_api import InternalApiEnvelope
from app.schemas.user_tools import (
    AddressListResult,
    AdminBusinessOverviewResult,
    AvailableCouponsResult,
    CartResult,
    OrderDetailResult,
    RecentOrdersResult,
    ReviewDraftCheckResult,
    SensitiveWordsResult,
)
from app.security.actor import build_internal_headers


class SpringInternalApiError(RuntimeError):
    def __init__(self, code: str, message: str, request_id: str) -> None:
        super().__init__(message)
        self.code = code
        self.request_id = request_id


@dataclass(slots=True)
class SpringInternalApiClient:
    settings: Settings
    _consecutive_failures: int = 0
    _circuit_open_until: float = 0.0

    async def get_shop_status(self, *, request_id: str, actor: ActorContext) -> dict[str, Any]:
        return await self._request(
            "GET",
            "/internal/agent/shop/status",
            request_id=request_id,
            actor=actor,
        )

    async def menu_search(
        self,
        *,
        request_id: str,
        actor: ActorContext,
        query: str,
        limit: int,
        budget_max: Decimal | None = None,
        dietary_preferences: str | None = None,
    ) -> dict[str, Any]:
        params: dict[str, Any] = {"query": query, "limit": limit}
        if budget_max is not None:
            params["budget_max"] = str(budget_max)
        if dietary_preferences:
            params["dietary_preferences"] = dietary_preferences
        return await self._request(
            "GET",
            "/internal/agent/menu/search",
            request_id=request_id,
            actor=actor,
            params=params,
        )

    async def list_recent_orders(
        self,
        *,
        request_id: str,
        actor: ActorContext,
        status: int | None,
        limit: int,
    ) -> RecentOrdersResult:
        params: dict[str, Any] = {"limit": limit}
        if status is not None:
            params["status"] = status
        data = await self._request(
            "GET",
            "/internal/agent/orders/recent",
            request_id=request_id,
            actor=actor,
            params=params,
        )
        return RecentOrdersResult.model_validate(data)

    async def get_order_detail(
        self,
        *,
        request_id: str,
        actor: ActorContext,
        order_id: int,
    ) -> OrderDetailResult:
        data = await self._request(
            "GET",
            f"/internal/agent/orders/{order_id}",
            request_id=request_id,
            actor=actor,
        )
        return OrderDetailResult.model_validate(data)

    async def get_cart(self, *, request_id: str, actor: ActorContext) -> CartResult:
        data = await self._request(
            "GET",
            "/internal/agent/cart",
            request_id=request_id,
            actor=actor,
        )
        return CartResult.model_validate(data)

    async def list_addresses(
        self,
        *,
        request_id: str,
        actor: ActorContext,
        default_only: bool,
    ) -> AddressListResult:
        data = await self._request(
            "GET",
            "/internal/agent/addresses",
            request_id=request_id,
            actor=actor,
            params={"default_only": default_only},
        )
        return AddressListResult.model_validate(data)

    async def list_available_coupons(
        self,
        *,
        request_id: str,
        actor: ActorContext,
        order_amount: Decimal | None,
    ) -> AvailableCouponsResult:
        params = {}
        if order_amount is not None:
            params["order_amount"] = str(order_amount)
        data = await self._request(
            "GET",
            "/internal/agent/coupons/available",
            request_id=request_id,
            actor=actor,
            params=params,
        )
        return AvailableCouponsResult.model_validate(data)

    async def check_sensitive_words(
        self,
        *,
        request_id: str,
        actor: ActorContext,
        text: str,
    ) -> SensitiveWordsResult:
        data = await self._request(
            "POST",
            "/internal/agent/sensitive-words/check",
            request_id=request_id,
            actor=actor,
            json_body={"text": text},
        )
        return SensitiveWordsResult.model_validate(data)

    async def check_review_draft(
        self,
        *,
        request_id: str,
        actor: ActorContext,
        order_id: int,
        dish_id: int,
        rating: int,
        highlights: str,
    ) -> ReviewDraftCheckResult:
        data = await self._request(
            "POST",
            "/internal/agent/reviews/draft/check",
            request_id=request_id,
            actor=actor,
            json_body={
                "order_id": order_id,
                "dish_id": dish_id,
                "rating": rating,
                "highlights": highlights,
            },
        )
        return ReviewDraftCheckResult.model_validate(data)

    async def get_admin_business_overview(
        self,
        *,
        request_id: str,
        actor: ActorContext,
        begin: str | None = None,
        end: str | None = None,
    ) -> AdminBusinessOverviewResult:
        params: dict[str, str] = {}
        if begin is not None:
            params["begin"] = begin
        if end is not None:
            params["end"] = end
        data = await self._request(
            "GET",
            "/internal/agent/admin/business/overview",
            request_id=request_id,
            actor=actor,
            params=params,
        )
        return AdminBusinessOverviewResult.model_validate(data)

    async def admin_order_search(
        self, *, request_id: str, actor: ActorContext, params: dict[str, Any]
    ) -> AdminOrderSearchResult:
        data = await self._request(
            "GET", "/internal/agent/admin/orders", request_id=request_id, actor=actor, params=params
        )
        return AdminOrderSearchResult.model_validate(data)

    async def admin_order_detail(
        self, *, request_id: str, actor: ActorContext, order_id: int
    ) -> AdminOrderSummary:
        data = await self._request(
            "GET",
            f"/internal/agent/admin/orders/{order_id}",
            request_id=request_id,
            actor=actor,
        )
        return AdminOrderSummary.model_validate(data)

    async def admin_catalog_search(
        self,
        *,
        request_id: str,
        actor: ActorContext,
        resource: str,
        params: dict[str, Any],
    ) -> AdminCatalogSearchResult:
        if resource not in {"menu", "setmeals"}:
            raise ValueError("unsupported admin catalog resource")
        data = await self._request(
            "GET",
            f"/internal/agent/admin/{resource}",
            request_id=request_id,
            actor=actor,
            params=params,
        )
        return AdminCatalogSearchResult.model_validate(data)

    async def admin_category_search(
        self, *, request_id: str, actor: ActorContext, params: dict[str, Any]
    ) -> AdminCategorySearchResult:
        data = await self._request(
            "GET",
            "/internal/agent/admin/categories",
            request_id=request_id,
            actor=actor,
            params=params,
        )
        return AdminCategorySearchResult.model_validate(data)

    async def admin_coupon_search(
        self, *, request_id: str, actor: ActorContext, params: dict[str, Any]
    ) -> AdminCouponSearchResult:
        data = await self._request(
            "GET", "/internal/agent/admin/coupons", request_id=request_id, actor=actor, params=params
        )
        return AdminCouponSearchResult.model_validate(data)

    async def admin_review_search(
        self, *, request_id: str, actor: ActorContext, params: dict[str, Any]
    ) -> AdminReviewSearchResult:
        data = await self._request(
            "GET", "/internal/agent/admin/reviews", request_id=request_id, actor=actor, params=params
        )
        return AdminReviewSearchResult.model_validate(data)

    async def set_admin_shop_status(
        self,
        *,
        request_id: str,
        actor: ActorContext,
        payload: dict[str, Any],
        confirmation_token: str,
        idempotency_key: str,
    ) -> dict[str, Any]:
        return await self._admin_write(
            "/internal/agent/admin/shop/status",
            request_id=request_id,
            actor=actor,
            payload=payload,
            confirmation_token=confirmation_token,
            idempotency_key=idempotency_key,
        )

    async def update_admin_order(
        self,
        *,
        request_id: str,
        actor: ActorContext,
        payload: dict[str, Any],
        confirmation_token: str,
        idempotency_key: str,
    ) -> dict[str, Any]:
        order_id = int(payload["order_id"])
        return await self._admin_write(
            f"/internal/agent/admin/orders/{order_id}/actions",
            request_id=request_id,
            actor=actor,
            payload={key: value for key, value in payload.items() if key != "order_id"},
            confirmation_token=confirmation_token,
            idempotency_key=idempotency_key,
        )

    async def manage_admin_coupon(
        self,
        *,
        request_id: str,
        actor: ActorContext,
        payload: dict[str, Any],
        confirmation_token: str,
        idempotency_key: str,
    ) -> dict[str, Any]:
        coupon_id = int(payload["coupon_id"])
        return await self._admin_write(
            f"/internal/agent/admin/coupons/{coupon_id}/actions",
            request_id=request_id,
            actor=actor,
            payload={key: value for key, value in payload.items() if key != "coupon_id"},
            confirmation_token=confirmation_token,
            idempotency_key=idempotency_key,
        )

    async def create_admin_dish(
        self,
        *,
        request_id: str,
        actor: ActorContext,
        payload: dict[str, Any],
        confirmation_token: str,
        idempotency_key: str,
    ) -> dict[str, Any]:
        return await self._admin_write(
            "/internal/agent/admin/menu/items",
            request_id=request_id,
            actor=actor,
            payload=payload,
            confirmation_token=confirmation_token,
            idempotency_key=idempotency_key,
        )

    async def update_admin_dish(
        self,
        *,
        request_id: str,
        actor: ActorContext,
        payload: dict[str, Any],
        confirmation_token: str,
        idempotency_key: str,
    ) -> dict[str, Any]:
        dish_id = int(payload["dish_id"])
        return await self._admin_write(
            f"/internal/agent/admin/menu/items/{dish_id}/actions",
            request_id=request_id,
            actor=actor,
            payload={key: value for key, value in payload.items() if key != "dish_id"},
            confirmation_token=confirmation_token,
            idempotency_key=idempotency_key,
        )

    async def create_admin_category(
        self,
        *,
        request_id: str,
        actor: ActorContext,
        payload: dict[str, Any],
        confirmation_token: str,
        idempotency_key: str,
    ) -> dict[str, Any]:
        return await self._admin_write(
            "/internal/agent/admin/categories",
            request_id=request_id,
            actor=actor,
            payload=payload,
            confirmation_token=confirmation_token,
            idempotency_key=idempotency_key,
        )

    async def create_admin_coupon(
        self,
        *,
        request_id: str,
        actor: ActorContext,
        payload: dict[str, Any],
        confirmation_token: str,
        idempotency_key: str,
    ) -> dict[str, Any]:
        return await self._admin_write(
            "/internal/agent/admin/coupons",
            request_id=request_id,
            actor=actor,
            payload=payload,
            confirmation_token=confirmation_token,
            idempotency_key=idempotency_key,
        )

    async def _admin_write(
        self,
        path: str,
        *,
        request_id: str,
        actor: ActorContext,
        payload: dict[str, Any],
        confirmation_token: str,
        idempotency_key: str,
    ) -> dict[str, Any]:
        return await self._request(
            "POST",
            path,
            request_id=request_id,
            actor=actor,
            json_body=payload,
            extra_headers={
                "X-Confirmation-Token": confirmation_token,
                "Idempotency-Key": idempotency_key,
            },
        )

    async def change_cart(
        self,
        *,
        request_id: str,
        actor: ActorContext,
        payload: dict[str, Any],
        confirmation_token: str,
        idempotency_key: str,
    ) -> dict[str, Any]:
        return await self._request(
            "POST",
            "/internal/agent/cart/changes",
            request_id=request_id,
            actor=actor,
            json_body=payload,
            extra_headers={
                "X-Confirmation-Token": confirmation_token,
                "Idempotency-Key": idempotency_key,
            },
        )

    async def claim_coupon(
        self,
        *,
        request_id: str,
        actor: ActorContext,
        coupon_id: int,
        confirmation_token: str,
        idempotency_key: str,
    ) -> dict[str, Any]:
        return await self._request(
            "POST",
            f"/internal/agent/coupons/{coupon_id}/claim",
            request_id=request_id,
            actor=actor,
            extra_headers={
                "X-Confirmation-Token": confirmation_token,
                "Idempotency-Key": idempotency_key,
            },
        )

    async def _request(
        self,
        method: str,
        path: str,
        *,
        request_id: str,
        actor: ActorContext,
        params: dict[str, Any] | None = None,
        json_body: dict[str, Any] | None = None,
        extra_headers: dict[str, str] | None = None,
    ) -> dict[str, Any]:
        if time.monotonic() < self._circuit_open_until:
            raise SpringInternalApiError(
                "CIRCUIT_OPEN", "Spring internal API circuit breaker is open", request_id
            )
        # Reads may be retried once on a transport/timeout failure. Writes are
        # deliberately single-attempt because repeating them could duplicate a
        # business mutation even when the upstream response was lost.
        attempts = 2 if method.upper() in {"GET", "HEAD"} else 1
        response: httpx.Response | None = None
        last_error: httpx.RequestError | None = None
        for _attempt in range(attempts):
            try:
                async with httpx.AsyncClient(
                    base_url=self.settings.spring_internal_base_url.rstrip("/"),
                    timeout=self.settings.spring_internal_timeout_seconds,
                ) as client:
                    headers = build_internal_headers(
                        request_id=request_id,
                        actor=actor,
                        auth_token=self.settings.spring_internal_auth_token,
                    )
                    if extra_headers:
                        headers.update(extra_headers)
                    response = await client.request(
                        method,
                        path,
                        params=params,
                        json=json_body,
                        headers=headers,
                    )
                break
            except httpx.RequestError as exc:
                last_error = exc
        if response is None:
            self._record_failure()
            raise SpringInternalApiError(
                "UPSTREAM_ERROR",
                "Spring internal API is temporarily unavailable",
                request_id,
            ) from last_error

        request_id_from_response = request_id
        try:
            payload = InternalApiEnvelope.model_validate(response.json())
            request_id_from_response = payload.request_id
        except Exception as exc:  # pragma: no cover - defensive fallback
            raise SpringInternalApiError("UPSTREAM_ERROR", "Invalid internal API response", request_id) from exc

        if response.status_code >= 400 or not payload.ok:
            if response.status_code >= 500:
                self._record_failure()
            raise SpringInternalApiError(
                payload.error_code or "UPSTREAM_ERROR",
                payload.message or "Internal API request failed",
                request_id_from_response,
            )

        self._consecutive_failures = 0
        self._circuit_open_until = 0.0
        data = payload.data
        if isinstance(data, dict):
            return data
        return {"items": data}

    def _record_failure(self) -> None:
        self._consecutive_failures += 1
        if self._consecutive_failures >= self.settings.circuit_breaker_failure_threshold:
            self._circuit_open_until = (
                time.monotonic() + self.settings.circuit_breaker_reset_seconds
            )
