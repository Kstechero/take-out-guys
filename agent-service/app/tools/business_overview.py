from __future__ import annotations

from dataclasses import dataclass
from datetime import date
from typing import Any

from pydantic import BaseModel

from app.clients.spring_internal import SpringInternalApiClient
from app.schemas.chat import ActorContext
from app.schemas.user_tools import AdminBusinessOverviewResult

try:
    from langchain_core.tools import StructuredTool
except ImportError:  # pragma: no cover
    StructuredTool = None


class BusinessOverviewInput(BaseModel):
    begin: date | None = None
    end: date | None = None


@dataclass(slots=True)
class AdminBusinessOverviewTool:
    client: SpringInternalApiClient

    async def run(
        self,
        *,
        request_id: str,
        actor: ActorContext,
        begin: date | None = None,
        end: date | None = None,
    ) -> AdminBusinessOverviewResult:
        if actor.type != "admin":
            raise PermissionError("Admin actor required")
        if begin is not None and end is not None and begin > end:
            raise ValueError("begin must not be after end")
        return await self.client.get_admin_business_overview(
            request_id=request_id,
            actor=actor,
            begin=begin.isoformat() if begin else None,
            end=end.isoformat() if end else None,
        )

    def as_langchain_tool(self, *, request_id: str, actor: ActorContext) -> Any:
        if StructuredTool is None:
            return None

        async def _coroutine(
            begin: date | None = None,
            end: date | None = None,
        ) -> dict[str, object]:
            result = await self.run(
                request_id=request_id,
                actor=actor,
                begin=begin,
                end=end,
            )
            return result.model_dump(mode="json")

        return StructuredTool.from_function(
            coroutine=_coroutine,
            name="query_business_overview",
            description=(
                "Query authoritative operating metrics and order overview for a date range. "
                "Dates use YYYY-MM-DD and default to today."
            ),
            args_schema=BusinessOverviewInput,
        )
