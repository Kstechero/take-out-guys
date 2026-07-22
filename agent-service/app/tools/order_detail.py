from dataclasses import dataclass

from langchain_core.tools import StructuredTool

from app.clients.spring_internal import SpringInternalApiClient
from app.schemas.chat import ActorContext
from app.schemas.user_tools import OrderDetailInput, OrderDetailResult


@dataclass(slots=True)
class OrderDetailTool:
    client: SpringInternalApiClient

    async def run(
        self,
        *,
        request_id: str,
        actor: ActorContext,
        order_id: int,
    ) -> OrderDetailResult:
        validated = OrderDetailInput(order_id=order_id)
        return await self.client.get_order_detail(
            request_id=request_id,
            actor=actor,
            order_id=validated.order_id,
        )

    def as_langchain_tool(self, *, request_id: str, actor: ActorContext) -> StructuredTool:
        async def _coroutine(order_id: int) -> dict[str, object]:
            result = await self.run(request_id=request_id, actor=actor, order_id=order_id)
            return result.model_dump(mode="json")

        return StructuredTool.from_function(
            coroutine=_coroutine,
            name="get_order_detail",
            description="按订单 ID 查询当前用户自己的订单详情；不能查询其他用户订单。",
            args_schema=OrderDetailInput,
        )
