from __future__ import annotations

from dataclasses import dataclass
from decimal import Decimal
from typing import Any

from pydantic import BaseModel, Field

from app.clients.spring_internal import SpringInternalApiClient
from app.schemas.chat import ActorContext

try:
    from langchain_core.tools import StructuredTool
except ImportError:  # pragma: no cover - fallback for partial environments
    StructuredTool = None


class MenuSearchInput(BaseModel):
    query: str = Field(min_length=1, max_length=200)
    limit: int = Field(default=5, ge=1, le=10)
    budget_max: Decimal | None = Field(default=None, gt=0)
    dietary_preferences: str | None = Field(default=None, max_length=200)


@dataclass(slots=True)
class MenuSearchTool:
    client: SpringInternalApiClient

    async def run(
        self, *, request_id: str, actor: ActorContext, query: str, limit: int = 5,
        budget_max: Decimal | None = None, dietary_preferences: str | None = None
    ) -> dict[str, Any]:
        validated = MenuSearchInput(
            query=query, limit=limit, budget_max=budget_max,
            dietary_preferences=dietary_preferences
        )
        return await self.client.menu_search(
            request_id=request_id,
            actor=actor,
            query=validated.query,
            limit=validated.limit,
            budget_max=validated.budget_max,
            dietary_preferences=validated.dietary_preferences,
        )

    def as_langchain_tool(self, *, request_id: str, actor: ActorContext) -> Any:
        if StructuredTool is None:
            return None

        async def _coroutine(
            query: str, limit: int = 5, budget_max: Decimal | None = None,
            dietary_preferences: str | None = None
        ) -> dict[str, Any]:
            return await self.run(
                request_id=request_id, actor=actor, query=query, limit=limit,
                budget_max=budget_max, dietary_preferences=dietary_preferences
            )

        return StructuredTool.from_function(
            coroutine=_coroutine,
            name="menu_search",
            description=("Search and recommend currently available dishes or set meals using "
                         "requirements, budget and dietary preferences."),
            args_schema=MenuSearchInput,
        )
