from __future__ import annotations

from dataclasses import dataclass
from typing import Any

from app.clients.spring_internal import SpringInternalApiClient
from app.schemas.chat import ActorContext

try:
    from langchain_core.tools import StructuredTool
except ImportError:  # pragma: no cover - fallback for partial environments
    StructuredTool = None


@dataclass(slots=True)
class ShopStatusTool:
    client: SpringInternalApiClient

    async def run(self, *, request_id: str, actor: ActorContext) -> dict[str, Any]:
        return await self.client.get_shop_status(request_id=request_id, actor=actor)

    def as_langchain_tool(self, *, request_id: str, actor: ActorContext) -> Any:
        if StructuredTool is None:
            return None

        async def _coroutine() -> dict[str, Any]:
            return await self.run(request_id=request_id, actor=actor)

        return StructuredTool.from_function(
            coroutine=_coroutine,
            name="get_shop_status",
            description="Get the current shop open/closed status from the internal Spring Boot API.",
        )
