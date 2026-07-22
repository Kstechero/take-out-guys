from __future__ import annotations

from dataclasses import dataclass
from decimal import Decimal

from langchain_core.tools import StructuredTool

from app.clients.spring_internal import SpringInternalApiClient
from app.confirmations import ConfirmationService
from app.schemas.chat import ActorContext
from app.schemas.confirmation import (
    CreateAdminCategoryInput,
    CreateAdminCouponInput,
    CreateAdminDishInput,
    ManageCouponInput,
    SetShopStatusInput,
    UpdateAdminDishInput,
    UpdateOrderActionInput,
)


@dataclass(slots=True)
class AdminMutationTools:
    client: SpringInternalApiClient
    confirmations: ConfirmationService

    def as_langchain_tools(
        self, *, request_id: str, actor: ActorContext, session_id: str
    ) -> list[StructuredTool]:
        if actor.type != "admin":
            return []

        async def set_shop_status(status: str, audit_reason: str) -> dict[str, object]:
            args = SetShopStatusInput(status=status, audit_reason=audit_reason)
            current = await self.client.get_shop_status(request_id=request_id, actor=actor)
            old_status = str(current.get("status", "UNKNOWN"))
            return self.confirmations.propose_admin_action(
                actor=actor,
                session_id=session_id,
                action="set_shop_status",
                arguments={**args.model_dump(), "expected_status": old_status},
                summary=f"确认将门店状态从 {old_status} 改为 {args.status} 吗？",
                details={
                    "old_value": old_status,
                    "new_value": args.status,
                    "impact_count": 1,
                    "risk": "high",
                    "audit_reason": args.audit_reason,
                },
            )

        async def update_order(
            order_id: int, action: str, audit_reason: str
        ) -> dict[str, object]:
            args = UpdateOrderActionInput(
                order_id=order_id, action=action, audit_reason=audit_reason
            )
            current = await self.client.admin_order_detail(
                request_id=request_id, actor=actor, order_id=args.order_id
            )
            target = {"confirm": 3, "deliver": 4, "complete": 5}[args.action]
            return self.confirmations.propose_admin_action(
                actor=actor,
                session_id=session_id,
                action="update_order",
                arguments={**args.model_dump(), "expected_status": current.status},
                summary=f"确认将订单 {current.number} 从状态 {current.status} 更新为 {target} 吗？",
                details={
                    "resource_id": args.order_id,
                    "old_value": current.status,
                    "new_value": target,
                    "impact_count": 1,
                    "risk": "high",
                    "audit_reason": args.audit_reason,
                },
            )

        async def manage_coupon(
            coupon_id: int, action: str, audit_reason: str
        ) -> dict[str, object]:
            args = ManageCouponInput(
                coupon_id=coupon_id, action=action, audit_reason=audit_reason
            )
            result = await self.client.admin_coupon_search(
                request_id=request_id,
                actor=actor,
                params={"query": str(args.coupon_id), "limit": 20},
            )
            coupon = next((item for item in result.items if item.id == args.coupon_id), None)
            if coupon is None:
                raise ValueError("coupon not found in authorized scope")
            target = 1 if args.action == "activate" else 0
            return self.confirmations.propose_admin_action(
                actor=actor,
                session_id=session_id,
                action="manage_coupon",
                arguments={**args.model_dump(), "expected_status": coupon.status},
                summary=f"确认将优惠券 {coupon.name} 从状态 {coupon.status} 更新为 {target} 吗？",
                details={
                    "resource_id": args.coupon_id,
                    "old_value": coupon.status,
                    "new_value": target,
                    "impact_count": 1,
                    "risk": "high",
                    "audit_reason": args.audit_reason,
                },
            )

        async def create_admin_dish(
            name: str,
            category_id: int,
            price: Decimal,
            image: str | None = None,
            description: str | None = None,
            status: int = 1,
            flavors: list[dict[str, object]] | None = None,
            audit_reason: str = "",
        ) -> dict[str, object]:
            args = CreateAdminDishInput(
                name=name,
                category_id=category_id,
                price=price,
                image=image,
                description=description,
                status=status,
                flavors=flavors or [],
                audit_reason=audit_reason,
            )
            categories = await self.client.admin_category_search(
                request_id=request_id,
                actor=actor,
                params={"query": str(args.category_id), "type": 1, "status": 1, "limit": 20},
            )
            category = next(
                (item for item in categories.items if item.id == args.category_id),
                None,
            )
            if category is None:
                raise ValueError("category_id must reference an existing enabled dish category")
            return self.confirmations.propose_admin_action(
                actor=actor,
                session_id=session_id,
                action="create_admin_dish",
                arguments=args.model_dump(mode="json", exclude_none=True),
                summary=f"确认新增菜品“{args.name}”，价格 {args.price} 元吗？",
                details={
                    "resource": "dish",
                    "new_value": {
                        "name": args.name,
                        "category_id": args.category_id,
                        "category_name": category.name,
                        "price": str(args.price),
                        "status": args.status,
                    },
                    "impact_count": 1,
                    "risk": "high",
                    "audit_reason": args.audit_reason,
                },
            )

        async def create_admin_category(
            name: str,
            type: int,
            sort: int = 0,
            audit_reason: str = "",
        ) -> dict[str, object]:
            args = CreateAdminCategoryInput(
                name=name,
                type=type,
                sort=sort,
                audit_reason=audit_reason,
            )
            existing = await self.client.admin_category_search(
                request_id=request_id,
                actor=actor,
                params={"name": args.name, "type": args.type, "page": 1, "limit": 20},
            )
            duplicate = next(
                (
                    item
                    for item in existing.items
                    if item.name == args.name and item.type == args.type
                ),
                None,
            )
            if duplicate is not None:
                raise ValueError("category with the same name and type already exists")
            category_type = "菜品分类" if args.type == 1 else "套餐分类"
            return self.confirmations.propose_admin_action(
                actor=actor,
                session_id=session_id,
                action="create_admin_category",
                arguments=args.model_dump(mode="json"),
                summary=f"确认新增{category_type}“{args.name}”，排序 {args.sort} 吗？",
                details={
                    "resource": "category",
                    "new_value": {
                        "name": args.name,
                        "type": args.type,
                        "sort": args.sort,
                        "initial_status": 0,
                    },
                    "impact_count": 1,
                    "risk": "high",
                    "audit_reason": args.audit_reason,
                },
            )

        async def update_admin_dish(
            dish_id: int,
            name: str | None = None,
            category_id: int | None = None,
            price: Decimal | None = None,
            image: str | None = None,
            description: str | None = None,
            status: int | None = None,
            audit_reason: str = "",
        ) -> dict[str, object]:
            args = UpdateAdminDishInput(
                dish_id=dish_id,
                name=name,
                category_id=category_id,
                price=price,
                image=image,
                description=description,
                status=status,
                audit_reason=audit_reason,
            )
            result = await self.client.admin_catalog_search(
                request_id=request_id,
                actor=actor,
                resource="menu",
                params={"query": str(args.dish_id), "limit": 20},
            )
            dish = next((item for item in result.items if item.id == args.dish_id), None)
            if dish is None:
                raise ValueError("dish not found in authorized scope")
            changes = args.model_dump(mode="json", exclude_none=True)
            changes["expected_updated_at"] = dish.updated_at
            return self.confirmations.propose_admin_action(
                actor=actor,
                session_id=session_id,
                action="update_admin_dish",
                arguments=changes,
                summary=f"确认修改菜品“{dish.name}”（ID {dish.id}）吗？",
                details={
                    "resource_id": dish.id,
                    "old_value": {
                        "name": dish.name,
                        "price": str(dish.price) if dish.price is not None else None,
                        "status": dish.status,
                        "updated_at": dish.updated_at,
                    },
                    "new_value": args.model_dump(mode="json", exclude_none=True),
                    "impact_count": 1,
                    "risk": "high",
                    "audit_reason": args.audit_reason,
                },
            )

        async def create_admin_coupon(
            name: str,
            type: int,
            discount_amount: Decimal,
            minimum_amount: Decimal,
            total_count: int,
            per_user_limit: int,
            valid_from: str,
            valid_until: str,
            status: int = 0,
            description: str | None = None,
            audit_reason: str = "",
        ) -> dict[str, object]:
            args = CreateAdminCouponInput(
                name=name,
                type=type,
                discount_amount=discount_amount,
                minimum_amount=minimum_amount,
                total_count=total_count,
                per_user_limit=per_user_limit,
                valid_from=valid_from,
                valid_until=valid_until,
                status=status,
                description=description,
                audit_reason=audit_reason,
            )
            return self.confirmations.propose_admin_action(
                actor=actor,
                session_id=session_id,
                action="create_admin_coupon",
                arguments=args.model_dump(mode="json", exclude_none=True),
                summary=(
                    f"确认新增优惠券“{args.name}”，满 {args.minimum_amount} 元减 "
                    f"{args.discount_amount} 元，共 {args.total_count} 张吗？"
                ),
                details={
                    "resource": "coupon",
                    "new_value": args.model_dump(mode="json", exclude_none=True),
                    "impact_count": 1,
                    "risk": "high",
                    "audit_reason": args.audit_reason,
                },
            )

        return [
            StructuredTool.from_function(
                coroutine=set_shop_status,
                name="set_shop_status",
                description="提出门店营业状态变更并生成高风险确认卡片；不会立即执行。",
                args_schema=SetShopStatusInput,
            ),
            StructuredTool.from_function(
                coroutine=update_order,
                name="update_order",
                description="提出受控订单状态推进并生成确认卡片；不会立即执行。",
                args_schema=UpdateOrderActionInput,
            ),
            StructuredTool.from_function(
                coroutine=manage_coupon,
                name="manage_coupon",
                description="提出优惠券启停并生成确认卡片；不会立即执行。",
                args_schema=ManageCouponInput,
            ),
            StructuredTool.from_function(
                coroutine=create_admin_dish,
                name="create_admin_dish",
                description=(
                    "提出新增菜品并生成高风险确认卡片；不会立即执行。"
                    "必须提供菜品名称、分类ID category_id、价格 price、审计理由 audit_reason；"
                    "可选图片、描述、状态 status 和口味 flavors。"
                ),
                args_schema=CreateAdminDishInput,
            ),
            StructuredTool.from_function(
                coroutine=create_admin_category,
                name="create_admin_category",
                description=(
                    "提出新增管理端分类并生成高风险确认卡；不会立即执行。"
                    "必须提供分类名称 name、分类类型 type（1=菜品分类，2=套餐分类）、排序 sort 和 audit_reason。"
                    "执行后沿用管理端 CategoryService.save，初始状态为停用。"
                ),
                args_schema=CreateAdminCategoryInput,
            ),
            StructuredTool.from_function(
                coroutine=update_admin_dish,
                name="update_admin_dish",
                description=(
                    "提出修改单个菜品并生成高风险确认卡片；不会立即执行。"
                    "必须提供菜品ID、至少一个要修改的字段和审计理由。"
                ),
                args_schema=UpdateAdminDishInput,
            ),
            StructuredTool.from_function(
                coroutine=create_admin_coupon,
                name="create_admin_coupon",
                description=(
                    "提出新增优惠券并生成高风险确认卡片；不会立即执行。"
                    "必须提供金额、发行量、每人限领数、有效期和审计理由。"
                ),
                args_schema=CreateAdminCouponInput,
            ),
        ]
