from __future__ import annotations

from dataclasses import dataclass
from decimal import Decimal

from langchain_core.tools import StructuredTool

from app.confirmations import ConfirmationService
from app.schemas.chat import ActorContext
from app.schemas.confirmation import (
    AddToCartInput,
    ClaimCouponInput,
    ClearCartInput,
    RemoveCartItemInput,
    UpdateCartItemInput,
)


@dataclass(slots=True)
class UserMutationTools:
    confirmations: ConfirmationService

    def as_langchain_tools(self, *, actor: ActorContext, session_id: str) -> list[StructuredTool]:
        if actor.type != "user":
            return []

        async def add_to_cart(
            expected_unit_amount: Decimal,
            dish_id: int | None = None,
            setmeal_id: int | None = None,
            flavor: str | None = None,
            quantity: int = 1,
        ) -> dict[str, object]:
            args = AddToCartInput(
                dish_id=dish_id,
                setmeal_id=setmeal_id,
                flavor=flavor,
                quantity=quantity,
                expected_unit_amount=expected_unit_amount,
            )
            product = f"菜品 {args.dish_id}" if args.dish_id else f"套餐 {args.setmeal_id}"
            return self.confirmations.propose_user_action(
                actor=actor,
                session_id=session_id,
                action="add",
                arguments=args.model_dump(mode="json", exclude_none=True),
                summary=f"确认将 {args.quantity} 份{product}加入购物车吗？",
            )

        async def update_cart_item(
            cart_item_id: int, quantity: int, flavor: str | None = None
        ) -> dict[str, object]:
            args = UpdateCartItemInput(
                cart_item_id=cart_item_id, quantity=quantity, flavor=flavor
            )
            return self.confirmations.propose_user_action(
                actor=actor,
                session_id=session_id,
                action="update",
                arguments=args.model_dump(exclude_none=True),
                summary=f"确认将购物车项 {args.cart_item_id} 的数量改为 {args.quantity} 吗？",
            )

        async def remove_from_cart(cart_item_id: int) -> dict[str, object]:
            args = RemoveCartItemInput(cart_item_id=cart_item_id)
            return self.confirmations.propose_user_action(
                actor=actor,
                session_id=session_id,
                action="remove",
                arguments=args.model_dump(),
                summary=f"确认从购物车移除购物车项 {args.cart_item_id} 吗？",
            )

        async def clear_cart() -> dict[str, object]:
            ClearCartInput()
            return self.confirmations.propose_user_action(
                actor=actor,
                session_id=session_id,
                action="clear",
                arguments={},
                summary="确认清空购物车吗？",
            )

        async def claim_coupon(coupon_id: int) -> dict[str, object]:
            args = ClaimCouponInput(coupon_id=coupon_id)
            return self.confirmations.propose_user_action(
                actor=actor,
                session_id=session_id,
                action="claim_coupon",
                arguments=args.model_dump(),
                summary=f"确认领取优惠券 {args.coupon_id} 吗？",
            )

        return [
            StructuredTool.from_function(
                coroutine=add_to_cart,
                name="add_to_cart",
                description=(
                    "提出加入购物车操作。expected_unit_amount 必须使用刚查询到的实时单价；"
                    "只生成确认卡片，不会立即修改购物车。"
                ),
                args_schema=AddToCartInput,
            ),
            StructuredTool.from_function(
                coroutine=update_cart_item,
                name="update_cart_item",
                description="提出修改购物车项操作。必须等待用户确认。",
                args_schema=UpdateCartItemInput,
            ),
            StructuredTool.from_function(
                coroutine=remove_from_cart,
                name="remove_from_cart",
                description="提出移除购物车项操作。必须等待用户确认。",
                args_schema=RemoveCartItemInput,
            ),
            StructuredTool.from_function(
                coroutine=clear_cart,
                name="clear_cart",
                description="提出清空购物车操作。必须等待用户确认。",
                args_schema=ClearCartInput,
            ),
            StructuredTool.from_function(
                coroutine=claim_coupon,
                name="claim_coupon",
                description="提出领取优惠券操作。必须等待用户确认并由业务服务校验资格。",
                args_schema=ClaimCouponInput,
            ),
        ]
