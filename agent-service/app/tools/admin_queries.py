from __future__ import annotations

from dataclasses import dataclass
from typing import Any

from langchain_core.tools import StructuredTool

from app.clients.spring_internal import SpringInternalApiClient
from app.schemas.admin_tools import (
    AdminCategorySearchInput,
    AdminCatalogSearchInput,
    AdminCouponSearchInput,
    AdminOrderDetailInput,
    AdminOrderSearchInput,
    AdminReviewSearchInput,
)
from app.schemas.chat import ActorContext


@dataclass(slots=True)
class AdminQueryTools:
    client: SpringInternalApiClient

    def as_langchain_tools(
        self, *, request_id: str, actor: ActorContext
    ) -> list[StructuredTool]:
        if actor.type != "admin":
            return []

        async def admin_order_search(
            number: str | None = None,
            status: int | None = None,
            begin: str | None = None,
            end: str | None = None,
            limit: int = 10,
        ) -> dict[str, Any]:
            args = AdminOrderSearchInput(
                number=number, status=status, begin=begin, end=end, limit=limit
            )
            result = await self.client.admin_order_search(
                request_id=request_id,
                actor=actor,
                params=args.model_dump(exclude_none=True),
            )
            return result.model_dump(mode="json")

        async def admin_order_detail(order_id: int) -> dict[str, Any]:
            args = AdminOrderDetailInput(order_id=order_id)
            result = await self.client.admin_order_detail(
                request_id=request_id, actor=actor, order_id=args.order_id
            )
            return result.model_dump(mode="json")

        async def admin_menu_search(
            name: str | None = None,
            query: str | None = None,
            status: int | None = None,
            category_id: int | None = None,
            page: int = 1,
            limit: int = 10,
        ) -> dict[str, Any]:
            args = AdminCatalogSearchInput(
                name=name,
                query=query,
                status=status,
                category_id=category_id,
                page=page,
                limit=limit,
            )
            params = args.model_dump(exclude_none=True)
            params.pop("query", None)
            result = await self.client.admin_catalog_search(
                request_id=request_id,
                actor=actor,
                resource="menu",
                params=params,
            )
            return result.model_dump(mode="json")

        async def admin_set_meal_search(
            name: str | None = None,
            query: str | None = None,
            status: int | None = None,
            category_id: int | None = None,
            page: int = 1,
            limit: int = 10,
        ) -> dict[str, Any]:
            args = AdminCatalogSearchInput(
                name=name,
                query=query,
                status=status,
                category_id=category_id,
                page=page,
                limit=limit,
            )
            params = args.model_dump(exclude_none=True)
            params.pop("query", None)
            result = await self.client.admin_catalog_search(
                request_id=request_id,
                actor=actor,
                resource="setmeals",
                params=params,
            )
            return result.model_dump(mode="json")

        async def admin_category_search(
            name: str | None = None,
            query: str | None = None,
            type: int | None = 1,
            status: int | None = 1,
            page: int = 1,
            limit: int = 20,
        ) -> dict[str, Any]:
            args = AdminCategorySearchInput(
                name=name,
                query=query,
                type=type,
                status=status,
                page=page,
                limit=limit,
            )
            params = args.model_dump(exclude_none=True)
            params.pop("query", None)
            params.pop("status", None)
            result = await self.client.admin_category_search(
                request_id=request_id,
                actor=actor,
                params=params,
            )
            return result.model_dump(mode="json")

        async def admin_coupon_search(
            query: str | None = None, status: int | None = None, limit: int = 10
        ) -> dict[str, Any]:
            args = AdminCouponSearchInput(query=query, status=status, limit=limit)
            result = await self.client.admin_coupon_search(
                request_id=request_id,
                actor=actor,
                params=args.model_dump(exclude_none=True),
            )
            return result.model_dump(mode="json")

        async def admin_review_search(
            keyword: str | None = None, status: int | None = None, limit: int = 10
        ) -> dict[str, Any]:
            args = AdminReviewSearchInput(keyword=keyword, status=status, limit=limit)
            result = await self.client.admin_review_search(
                request_id=request_id,
                actor=actor,
                params=args.model_dump(exclude_none=True),
            )
            return result.model_dump(mode="json")

        definitions = [
            (
                admin_order_search,
                "admin_order_search",
                "按受限条件查询订单；必须提供订单号、状态或明确时间范围之一。",
                AdminOrderSearchInput,
            ),
            (
                admin_order_detail,
                "admin_order_detail",
                "查询单个订单的脱敏运营摘要。",
                AdminOrderDetailInput,
            ),
            (
                admin_menu_search,
                "admin_menu_search",
                (
                    "按管理端菜品分页查询口径查询菜品目录；支持 name、category_id、"
                    "status、page、limit，total 为符合条件的总数。"
                ),
                AdminCatalogSearchInput,
            ),
            (
                admin_set_meal_search,
                "admin_set_meal_search",
                (
                    "按管理端套餐分页查询口径查询套餐目录；支持 name、category_id、"
                    "status、page、limit，total 为符合条件的总数。"
                ),
                AdminCatalogSearchInput,
            ),
            (
                admin_category_search,
                "admin_category_search",
                (
                    "按管理端分类分页查询口径查询真实分类；支持 name、type、page、limit。"
                    "新增或按分类查询菜品前先用它获取分类 ID。type=1 表示菜品分类，type=2 表示套餐分类。"
                ),
                AdminCategorySearchInput,
            ),
            (
                admin_coupon_search,
                "admin_coupon_search",
                "查询优惠券配置、余量、状态和有效期。",
                AdminCouponSearchInput,
            ),
            (
                admin_review_search,
                "admin_review_search",
                "查询脱敏评价及审核状态。",
                AdminReviewSearchInput,
            ),
        ]
        return [
            StructuredTool.from_function(
                coroutine=coroutine,
                name=name,
                description=description,
                args_schema=schema,
            )
            for coroutine, name, description, schema in definitions
        ]
