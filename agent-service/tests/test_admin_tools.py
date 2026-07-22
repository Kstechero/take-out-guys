from typing import Any

import pytest
from pydantic import ValidationError

from app.clients.spring_internal import SpringInternalApiClient
from app.confirmations import ConfirmationService, ConfirmationStore
from app.core.config import Settings
from app.schemas.admin_tools import (
    AdminCategoryItem,
    AdminCategorySearchResult,
    AdminCatalogItem,
    AdminCatalogSearchResult,
    AdminOrderSearchInput,
)
from app.schemas.chat import ActorContext
from app.tools.admin_queries import AdminQueryTools
from app.tools.admin_mutations import AdminMutationTools
from app.tools.business_overview import AdminBusinessOverviewTool
from app.tools.menu_search import MenuSearchTool
from app.tools.registry import AdminToolRegistry
from app.tools.sensitive_words import SensitiveWordsTool
from app.tools.shop_status import ShopStatusTool


def registry() -> AdminToolRegistry:
    client = SpringInternalApiClient(Settings())
    return AdminToolRegistry(
        business_overview=AdminBusinessOverviewTool(client),
        queries=AdminQueryTools(client),
        menu_search=MenuSearchTool(client),
        shop_status=ShopStatusTool(client),
        sensitive_words=SensitiveWordsTool(client),
    )


def test_admin_registry_exposes_only_reviewed_read_tools() -> None:
    tools = registry().langchain_tools(
        request_id="req-admin",
        actor=ActorContext(type="admin", id="7", roles=["ADMIN"]),
        session_id="admin-session",
    )

    assert [tool.name for tool in tools] == [
        "query_business_overview",
        "get_shop_status",
        "check_sensitive_words",
        "admin_order_search",
        "admin_order_detail",
        "admin_menu_search",
        "admin_set_meal_search",
        "admin_category_search",
        "admin_coupon_search",
        "admin_review_search",
    ]


def test_user_actor_receives_no_admin_tools() -> None:
    tools = registry().langchain_tools(
        request_id="req-user",
        actor=ActorContext(type="user", id="1001", roles=["USER"]),
    )

    assert tools == []


def test_admin_order_search_requires_bounded_filter() -> None:
    with pytest.raises(ValidationError, match="at least one order filter"):
        AdminOrderSearchInput()


class FakeAdminMutationClient:
    def __init__(self) -> None:
        self.calls: list[dict[str, Any]] = []

    async def get_shop_status(self, **_kwargs: Any) -> dict[str, Any]:
        return {"status": "OPEN", "updated_at": "2026-07-22T00:00:00Z"}

    async def set_admin_shop_status(self, **kwargs: Any) -> dict[str, Any]:
        self.calls.append(kwargs)
        return {"status": "APPLIED", "old_value": "OPEN", "new_value": "CLOSED"}

    async def admin_catalog_search(self, **_kwargs: Any) -> AdminCatalogSearchResult:
        return AdminCatalogSearchResult(
            items=[
                AdminCatalogItem(
                    id=46,
                    name="王老吉",
                    price="6.00",
                    status=1,
                    updated_at="2026-07-22T12:00:00",
                )
            ],
            total=1,
            generated_at="2026-07-22T12:00:01",
            scope="single_store",
            source="spring_internal_api",
        )

    async def admin_category_search(self, **_kwargs: Any) -> AdminCategorySearchResult:
        return AdminCategorySearchResult(
            items=[
                AdminCategoryItem(id=11, name="酒水饮料", type=1, status=1),
                AdminCategoryItem(id=12, name="传统主食", type=1, status=1),
                AdminCategoryItem(id=21, name="汤类", type=1, status=1),
            ],
            total=3,
            generated_at="2026-07-22T12:00:01",
            scope="single_store",
            source="spring_internal_api",
        )

    async def create_admin_dish(self, **kwargs: Any) -> dict[str, Any]:
        self.calls.append({"method": "create_admin_dish", **kwargs})
        return {"status": "APPLIED", "dish_id": 100}

    async def update_admin_dish(self, **kwargs: Any) -> dict[str, Any]:
        self.calls.append({"method": "update_admin_dish", **kwargs})
        return {"status": "APPLIED", "dish_id": 46}

    async def create_admin_coupon(self, **kwargs: Any) -> dict[str, Any]:
        self.calls.append({"method": "create_admin_coupon", **kwargs})
        return {"status": "APPLIED", "coupon_name": kwargs["payload"]["name"]}


async def test_admin_mutation_previews_old_value_and_executes_bound_payload() -> None:
    client = FakeAdminMutationClient()
    confirmations = ConfirmationService(
        store=ConfirmationStore(":memory:"),
        client=client,  # type: ignore[arg-type]
    )
    tools = AdminMutationTools(
        client=client,  # type: ignore[arg-type]
        confirmations=confirmations,
    ).as_langchain_tools(
        request_id="req-admin-write",
        actor=ActorContext(type="admin", id="7", roles=["ADMIN"]),
        session_id="admin-session",
    )
    tool = next(item for item in tools if item.name == "set_shop_status")

    proposal = await tool.ainvoke(
        {"status": "CLOSED", "audit_reason": "晚间设备维护"}
    )
    confirmation = proposal["confirmation"]
    await confirmations.execute_admin_action(
        confirmation["token"],
        request_id="req-admin-execute",
        actor=ActorContext(type="admin", id="7", roles=["ADMIN"]),
        session_id="admin-session",
    )

    assert confirmation["details"]["old_value"] == "OPEN"
    assert confirmation["details"]["new_value"] == "CLOSED"
    assert client.calls[0]["payload"] == {
        "status": "CLOSED",
        "audit_reason": "晚间设备维护",
        "expected_status": "OPEN",
    }


async def test_admin_create_dish_generates_confirmation_card() -> None:
    client = FakeAdminMutationClient()
    confirmations = ConfirmationService(
        store=ConfirmationStore(":memory:"),
        client=client,  # type: ignore[arg-type]
    )
    tools = AdminMutationTools(
        client=client,  # type: ignore[arg-type]
        confirmations=confirmations,
    ).as_langchain_tools(
        request_id="req-admin-create-dish",
        actor=ActorContext(type="admin", id="7", roles=["ADMIN"]),
        session_id="admin-session",
    )
    tool = next(item for item in tools if item.name == "create_admin_dish")

    proposal = await tool.ainvoke(
        {
            "name": "测试鸡腿饭",
            "category_id": 11,
            "price": "25.00",
            "status": 1,
            "description": "联调用测试菜品",
            "audit_reason": "管理端联调测试新增菜品",
        }
    )

    confirmation = proposal["confirmation"]
    assert confirmation["action"] == "create_admin_dish"
    assert confirmation["details"]["new_value"]["name"] == "测试鸡腿饭"

    await confirmations.execute_admin_action(
        confirmation["token"],
        request_id="req-admin-create-dish-execute",
        actor=ActorContext(type="admin", id="7", roles=["ADMIN"]),
        session_id="admin-session",
    )
    assert client.calls[-1]["method"] == "create_admin_dish"


async def test_admin_update_dish_binds_reviewed_version() -> None:
    client = FakeAdminMutationClient()
    confirmations = ConfirmationService(
        store=ConfirmationStore(":memory:"),
        client=client,  # type: ignore[arg-type]
    )
    tool = next(
        item
        for item in AdminMutationTools(
            client=client,  # type: ignore[arg-type]
            confirmations=confirmations,
        ).as_langchain_tools(
            request_id="req-admin-update-dish",
            actor=ActorContext(type="admin", id="7", roles=["ADMIN"]),
            session_id="admin-session",
        )
        if item.name == "update_admin_dish"
    )

    proposal = await tool.ainvoke(
        {"dish_id": 46, "price": "7.00", "audit_reason": "饮品采购成本调整"}
    )
    confirmation = proposal["confirmation"]
    assert confirmation["action"] == "update_admin_dish"

    await confirmations.execute_admin_action(
        confirmation["token"],
        request_id="req-admin-update-dish-execute",
        actor=ActorContext(type="admin", id="7", roles=["ADMIN"]),
        session_id="admin-session",
    )
    assert client.calls[-1]["method"] == "update_admin_dish"
    assert client.calls[-1]["payload"]["expected_updated_at"] == "2026-07-22T12:00:00"
    assert client.calls[-1]["payload"]["price"] == "7.00"


async def test_admin_create_coupon_executes_confirmed_payload() -> None:
    client = FakeAdminMutationClient()
    confirmations = ConfirmationService(
        store=ConfirmationStore(":memory:"),
        client=client,  # type: ignore[arg-type]
    )
    tool = next(
        item
        for item in AdminMutationTools(
            client=client,  # type: ignore[arg-type]
            confirmations=confirmations,
        ).as_langchain_tools(
            request_id="req-admin-create-coupon",
            actor=ActorContext(type="admin", id="7", roles=["ADMIN"]),
            session_id="admin-session",
        )
        if item.name == "create_admin_coupon"
    )

    proposal = await tool.ainvoke(
        {
            "name": "夏日满减券",
            "type": 1,
            "discount_amount": "5.00",
            "minimum_amount": "30.00",
            "total_count": 100,
            "per_user_limit": 1,
            "valid_from": "2026-07-22T00:00:00",
            "valid_until": "2026-08-22T00:00:00",
            "status": 0,
            "audit_reason": "暑期运营活动创建优惠券",
        }
    )
    confirmation = proposal["confirmation"]
    assert confirmation["action"] == "create_admin_coupon"

    await confirmations.execute_admin_action(
        confirmation["token"],
        request_id="req-admin-create-coupon-execute",
        actor=ActorContext(type="admin", id="7", roles=["ADMIN"]),
        session_id="admin-session",
    )
    assert client.calls[-1]["method"] == "create_admin_coupon"
    assert client.calls[-1]["payload"]["total_count"] == 100
