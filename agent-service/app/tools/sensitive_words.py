from dataclasses import dataclass

from langchain_core.tools import StructuredTool

from app.clients.spring_internal import SpringInternalApiClient
from app.schemas.chat import ActorContext
from app.schemas.user_tools import SensitiveWordsInput, SensitiveWordsResult


@dataclass(slots=True)
class SensitiveWordsTool:
    client: SpringInternalApiClient

    async def run(
        self,
        *,
        request_id: str,
        actor: ActorContext,
        text: str,
    ) -> SensitiveWordsResult:
        validated = SensitiveWordsInput(text=text)
        return await self.client.check_sensitive_words(
            request_id=request_id,
            actor=actor,
            text=validated.text,
        )

    def as_langchain_tool(self, *, request_id: str, actor: ActorContext) -> StructuredTool:
        async def _coroutine(text: str) -> dict[str, object]:
            result = await self.run(request_id=request_id, actor=actor, text=text)
            return result.model_dump(mode="json")

        return StructuredTool.from_function(
            coroutine=_coroutine,
            name="check_sensitive_words",
            description="检查评价草稿等用户文本是否安全，并返回处理后的文本；不返回敏感词词库。",
            args_schema=SensitiveWordsInput,
        )
