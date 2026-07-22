from typing import Any

from langchain_core.language_models.fake_chat_models import FakeMessagesListChatModel
from langchain_core.messages import AIMessage, BaseMessage
from langchain_core.outputs import ChatResult
from langchain_core.tools import StructuredTool
from pydantic import BaseModel, Field

from app.core.config import Settings
from app.graphs.admin_operations import AdminOperationsAgentGraph
from app.schemas.chat import ActorContext, ChatRequest


class ToolCapableFakeModel(FakeMessagesListChatModel):
    seen_messages: list[list[BaseMessage]] = Field(default_factory=list)

    def bind_tools(self, tools: list[object], **kwargs: Any) -> "ToolCapableFakeModel":
        return self

    def _generate(
        self,
        messages: list[BaseMessage],
        stop: list[str] | None = None,
        run_manager: Any = None,
        **kwargs: Any,
    ) -> ChatResult:
        self.seen_messages.append(messages)
        return super()._generate(messages, stop=stop, run_manager=run_manager, **kwargs)


class BusinessOverviewInput(BaseModel):
    begin: str | None = None
    end: str | None = None


class StubAdminRegistry:
    def __init__(self) -> None:
        self.business_calls = 0

    def langchain_tools(
        self, *, request_id: str, actor: ActorContext, session_id: str | None = None
    ) -> list[object]:
        async def query_business_overview(
            begin: str | None = None, end: str | None = None
        ) -> dict[str, object]:
            self.business_calls += 1
            return {
                "turnover": 1288.5,
                "valid_order_count": 12,
                "order_completion_rate": 0.92,
                "unit_price": 107.38,
                "new_users": 4,
            }

        return [
            StructuredTool.from_function(
                coroutine=query_business_overview,
                name="query_business_overview",
                description="Query authoritative operating metrics.",
                args_schema=BusinessOverviewInput,
            )
        ]


def admin_request(message: str) -> ChatRequest:
    return ChatRequest(
        request_id="req-admin-1",
        actor=ActorContext(type="admin", id="1", roles=["ADMIN"]),
        session_id="admin-session-1",
        message=message,
    )


async def test_admin_graph_stops_repeated_query_tool_calls_and_finalizes_answer() -> None:
    repeated_call = {
        "name": "query_business_overview",
        "args": {"begin": "2026-07-22", "end": "2026-07-22"},
        "id": "call-business-1",
    }
    model = ToolCapableFakeModel(
        responses=[
            AIMessage(content="", tool_calls=[repeated_call]),
            AIMessage(content="", tool_calls=[{**repeated_call, "id": "call-business-2"}]),
            AIMessage(content="今日营业额 1288.5 元，有效订单 12 单。"),
        ]
    )
    registry = StubAdminRegistry()
    graph = AdminOperationsAgentGraph(
        settings=Settings(),
        tools=registry,  # type: ignore[arg-type]
        model=model,
    )

    response = await graph.run(admin_request("查一下今天经营数据"))

    assert registry.business_calls == 1
    assert response.status == "completed"
    assert response.answer == "今日营业额 1288.5 元，有效订单 12 单。"
