from dataclasses import dataclass

from langchain_core.tools import StructuredTool

from app.clients.spring_internal import SpringInternalApiClient
from app.schemas.chat import ActorContext
from app.schemas.user_tools import RecentOrdersInput, RecentOrdersResult


@dataclass(slots=True)
class RecentOrdersTool:
    client: SpringInternalApiClient

    async def run(
        self,
        *,
        request_id: str,
        actor: ActorContext,
        status: int | None = None,
        limit: int = 5,
    ) -> RecentOrdersResult:
        validated = RecentOrdersInput(status=status, limit=limit)
        return await self.client.list_recent_orders(
            request_id=request_id,
            actor=actor,
            status=validated.status,
            limit=validated.limit,
        )

    def as_langchain_tool(self, *, request_id: str, actor: ActorContext) -> StructuredTool:
        async def _coroutine(status: int | None = None, limit: int = 5) -> dict[str, object]:
            result = await self.run(
                request_id=request_id,
                actor=actor,
                status=status,
                limit=limit,
            )
            return result.model_dump(mode="json")

        return StructuredTool.from_function(
            coroutine=_coroutine,
            name="list_recent_orders",
            description=(
                "查询当前用户自己的最近订单。用户询问最近订单、历史订单或订单列表时使用；"
                "status 可选，1待付款、2待接单、3已接单、4配送中、5已完成、6已取消。"
            ),
            args_schema=RecentOrdersInput,
        )
