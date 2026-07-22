from dataclasses import dataclass

from langchain_core.tools import StructuredTool

from app.clients.spring_internal import SpringInternalApiClient
from app.schemas.chat import ActorContext
from app.schemas.user_tools import CartResult


@dataclass(slots=True)
class CartTool:
    client: SpringInternalApiClient

    async def run(self, *, request_id: str, actor: ActorContext) -> CartResult:
        return await self.client.get_cart(request_id=request_id, actor=actor)

    def as_langchain_tool(self, *, request_id: str, actor: ActorContext) -> StructuredTool:
        async def _coroutine() -> dict[str, object]:
            result = await self.run(request_id=request_id, actor=actor)
            return result.model_dump(mode="json")

        return StructuredTool.from_function(
            coroutine=_coroutine,
            name="get_cart",
            description="查询当前用户自己的购物车、商品数量和总金额。只读，不修改购物车。",
        )
