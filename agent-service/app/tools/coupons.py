from dataclasses import dataclass
from decimal import Decimal

from langchain_core.tools import StructuredTool

from app.clients.spring_internal import SpringInternalApiClient
from app.schemas.chat import ActorContext
from app.schemas.user_tools import AvailableCouponsInput, AvailableCouponsResult


@dataclass(slots=True)
class AvailableCouponsTool:
    client: SpringInternalApiClient

    async def run(
        self,
        *,
        request_id: str,
        actor: ActorContext,
        order_amount: Decimal | None = None,
    ) -> AvailableCouponsResult:
        validated = AvailableCouponsInput(order_amount=order_amount)
        return await self.client.list_available_coupons(
            request_id=request_id,
            actor=actor,
            order_amount=validated.order_amount,
        )

    def as_langchain_tool(self, *, request_id: str, actor: ActorContext) -> StructuredTool:
        async def _coroutine(order_amount: Decimal | None = None) -> dict[str, object]:
            result = await self.run(
                request_id=request_id,
                actor=actor,
                order_amount=order_amount,
            )
            return result.model_dump(mode="json")

        return StructuredTool.from_function(
            coroutine=_coroutine,
            name="list_available_coupons",
            description=(
                "查询当前用户自己的未使用优惠券；提供 order_amount 时只返回满足该订单金额的优惠券。"
            ),
            args_schema=AvailableCouponsInput,
        )
