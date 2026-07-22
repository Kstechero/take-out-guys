from dataclasses import dataclass

from langchain_core.tools import StructuredTool
from pydantic import BaseModel, Field

from app.clients.spring_internal import SpringInternalApiClient
from app.schemas.chat import ActorContext


class ReviewDraftInput(BaseModel):
    order_id: int = Field(gt=0)
    dish_id: int = Field(gt=0)
    rating: int = Field(default=5, ge=1, le=5)
    highlights: str = Field(default="", max_length=500)


@dataclass(slots=True)
class ReviewDraftTool:
    client: SpringInternalApiClient

    def as_langchain_tool(self, *, request_id: str, actor: ActorContext) -> StructuredTool:
        async def _coroutine(
            order_id: int, dish_id: int, rating: int = 5, highlights: str = ""
        ) -> dict[str, object]:
            result = await self.client.check_review_draft(
                request_id=request_id,
                actor=actor,
                order_id=order_id,
                dish_id=dish_id,
                rating=rating,
                highlights=highlights,
            )
            return result.model_dump(mode="json")

        return StructuredTool.from_function(
            coroutine=_coroutine,
            name="check_review_draft",
            description=(
                "Validate ownership, completed-order status, ordered dish and sensitive content "
                "before generating an unpublished review draft."
            ),
            args_schema=ReviewDraftInput,
        )
