from dataclasses import dataclass

from langchain_core.tools import StructuredTool

from app.clients.spring_internal import SpringInternalApiClient
from app.schemas.chat import ActorContext
from app.schemas.user_tools import AddressListInput, AddressListResult


@dataclass(slots=True)
class AddressesTool:
    client: SpringInternalApiClient

    async def run(
        self,
        *,
        request_id: str,
        actor: ActorContext,
        default_only: bool = False,
    ) -> AddressListResult:
        validated = AddressListInput(default_only=default_only)
        return await self.client.list_addresses(
            request_id=request_id,
            actor=actor,
            default_only=validated.default_only,
        )

    def as_langchain_tool(self, *, request_id: str, actor: ActorContext) -> StructuredTool:
        async def _coroutine(default_only: bool = False) -> dict[str, object]:
            result = await self.run(
                request_id=request_id,
                actor=actor,
                default_only=default_only,
            )
            return result.model_dump(mode="json")

        return StructuredTool.from_function(
            coroutine=_coroutine,
            name="list_addresses",
            description="查询当前用户自己的脱敏收货地址；default_only=true 时只查默认地址。",
            args_schema=AddressListInput,
        )
