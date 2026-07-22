import json
from typing import Any

from langchain_core.language_models.fake_chat_models import FakeMessagesListChatModel
from langchain_core.messages import AIMessage, BaseMessage, SystemMessage, ToolMessage
from langchain_core.outputs import ChatResult
from langchain_core.tools import StructuredTool
from pydantic import BaseModel, Field

from app.clients.spring_internal import SpringInternalApiError
from app.core.config import Settings
from app.graphs.admin_operations import ADMIN_OUT_OF_SCOPE_MESSAGE, AdminOperationsAgentGraph
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


class EmptyInput(BaseModel):
    pass


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


class ConfirmationAdminRegistry:
    def langchain_tools(
        self, *, request_id: str, actor: ActorContext, session_id: str | None = None
    ) -> list[object]:
        async def create_admin_dish() -> dict[str, object]:
            return {
                "ok": True,
                "confirmation": {
                    "token": "token-1",
                    "action": "create_admin_dish",
                    "summary": "确认新增菜品“贵妃龙虾汤泡饭”，分类 汤类（ID 21），价格 88 元吗？",
                    "expires_at": "2026-07-22T13:30:00Z",
                },
            }

        return [
            StructuredTool.from_function(
                coroutine=create_admin_dish,
                name="create_admin_dish",
                description="Create an admin dish confirmation.",
                args_schema=EmptyInput,
            )
        ]


class FailingAdminRegistry:
    def langchain_tools(
        self, *, request_id: str, actor: ActorContext, session_id: str | None = None
    ) -> list[object]:
        async def admin_menu_search() -> dict[str, object]:
            raise SpringInternalApiError(
                "UPSTREAM_ERROR",
                "Spring internal API is temporarily unavailable",
                request_id,
            )

        return [
            StructuredTool.from_function(
                coroutine=admin_menu_search,
                name="admin_menu_search",
                description="Search admin menu.",
                args_schema=EmptyInput,
            )
        ]


def admin_request(message: str) -> ChatRequest:
    return ChatRequest(
        request_id="req-admin-1",
        actor=ActorContext(type="admin", id="1", roles=["ADMIN"]),
        session_id="admin-session-1",
        message=message,
    )


def test_admin_model_tool_schemas_strip_provider_rejected_keywords() -> None:
    graph = AdminOperationsAgentGraph(
        settings=Settings(),
        tools=StubAdminRegistry(),  # type: ignore[arg-type]
        model=ToolCapableFakeModel(responses=[AIMessage(content="ok")]),
    )

    schemas = graph.model_tool_schemas(
        graph.langchain_tools_for_request(admin_request("schema check"), "admin-session-1")
    )

    def find_rejected_keys(value: object) -> list[str]:
        rejected = {
            "anyOf",
            "default",
            "format",
            "pattern",
            "minimum",
            "maximum",
            "exclusiveMinimum",
            "exclusiveMaximum",
            "minLength",
            "maxLength",
            "minItems",
            "maxItems",
        }
        found: list[str] = []
        if isinstance(value, dict):
            for key, item in value.items():
                if key in rejected:
                    found.append(key)
                found.extend(find_rejected_keys(item))
        elif isinstance(value, list):
            for item in value:
                found.extend(find_rejected_keys(item))
        return found

    assert schemas
    for schema in schemas:
        assert find_rejected_keys(schema["function"]["parameters"]) == []


def test_admin_model_messages_keep_only_leading_system_prompt() -> None:
    graph = AdminOperationsAgentGraph(
        settings=Settings(),
        tools=StubAdminRegistry(),  # type: ignore[arg-type]
        model=ToolCapableFakeModel(responses=[AIMessage(content="ok")]),
    )

    messages = graph._model_messages(
        "fresh prompt",
        [AIMessage(content="before"), SystemMessage(content="checkpoint marker")],
    )

    assert [type(message) for message in messages] == [SystemMessage, AIMessage]
    assert messages[0].content == "fresh prompt"


async def test_admin_graph_rejects_out_of_scope_general_knowledge() -> None:
    model = ToolCapableFakeModel(responses=[AIMessage(content="should not be used")])
    graph = AdminOperationsAgentGraph(
        settings=Settings(),
        tools=StubAdminRegistry(),  # type: ignore[arg-type]
        model=model,
    )

    response = await graph.run(admin_request("冒泡排序是什么"))

    assert response.status == "completed"
    assert response.answer == ADMIN_OUT_OF_SCOPE_MESSAGE
    assert model.seen_messages == []


async def test_admin_stream_rejects_out_of_scope_general_knowledge() -> None:
    model = ToolCapableFakeModel(responses=[AIMessage(content="should not be used")])
    graph = AdminOperationsAgentGraph(
        settings=Settings(),
        tools=StubAdminRegistry(),  # type: ignore[arg-type]
        model=model,
    )

    events = [event async for event in graph.stream(admin_request("bubble sort 是什么"))]

    assert ("delta", {"text": ADMIN_OUT_OF_SCOPE_MESSAGE}) in events
    assert events[-1][0] == "done"
    assert model.seen_messages == []


def test_admin_category_fallback_recommends_real_soup_category() -> None:
    graph = AdminOperationsAgentGraph(
        settings=Settings(),
        tools=StubAdminRegistry(),  # type: ignore[arg-type]
        model=ToolCapableFakeModel(responses=[AIMessage(content="ok")]),
    )
    message = ToolMessage(
        name="admin_category_search",
        tool_call_id="call-category",
        content=json.dumps(
            {
                "items": [
                    {"id": 12, "name": "传统主食", "type": 1, "status": 1},
                    {"id": 21, "name": "汤类", "type": 1, "status": 1},
                ],
                "total": 2,
            },
            ensure_ascii=False,
        ),
    )

    assert graph._grounded_tool_fallback([message]) == (
        "查询到可用菜品分类：传统主食（ID 12）、汤类（ID 21）。建议使用：汤类（ID 21）。"
    )


def test_admin_catalog_fallback_includes_dish_category() -> None:
    graph = AdminOperationsAgentGraph(
        settings=Settings(),
        tools=StubAdminRegistry(),  # type: ignore[arg-type]
        model=ToolCapableFakeModel(responses=[AIMessage(content="ok")]),
    )
    message = ToolMessage(
        name="admin_menu_search",
        tool_call_id="call-menu",
        content=json.dumps(
            {
                "items": [
                    {
                        "id": 100,
                        "name": "贵妃龙虾汤泡饭",
                        "price": 88.0,
                        "status": 1,
                        "category_name": "汤类",
                    }
                ],
                "total": 1,
            },
            ensure_ascii=False,
        ),
    )

    assert graph._grounded_tool_fallback([message]) == (
        "查询到以下结果：贵妃龙虾汤泡饭 属于 汤类，价格 88.0 元。"
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


async def test_admin_stream_falls_back_to_tool_result_when_model_returns_empty_text() -> None:
    model = ToolCapableFakeModel(
        responses=[
            AIMessage(
                content="",
                tool_calls=[
                    {
                        "name": "query_business_overview",
                        "args": {"begin": "2026-07-22", "end": "2026-07-22"},
                        "id": "call-business-1",
                    }
                ],
            ),
            AIMessage(content=""),
        ]
    )
    graph = AdminOperationsAgentGraph(
        settings=Settings(),
        tools=StubAdminRegistry(),  # type: ignore[arg-type]
        model=model,
    )

    events = [event async for event in graph.stream(admin_request("查一下今天经营数据"))]

    deltas = [payload["text"] for event, payload in events if event == "delta"]
    assert deltas == [
        "查询到以下结果：turnover=1288.5、valid_order_count=12、order_completion_rate=0.92、unit_price=107.38、new_users=4。"
    ]


async def test_admin_stream_emits_confirmation_summary_as_visible_text() -> None:
    model = ToolCapableFakeModel(
        responses=[
            AIMessage(
                content="",
                tool_calls=[
                    {"name": "create_admin_dish", "args": {}, "id": "call-create-dish"}
                ],
            )
        ]
    )
    graph = AdminOperationsAgentGraph(
        settings=Settings(),
        tools=ConfirmationAdminRegistry(),  # type: ignore[arg-type]
        model=model,
    )

    events = [event async for event in graph.stream(admin_request("需要"))]

    deltas = [payload["text"] for event, payload in events if event == "delta"]
    assert deltas == ["确认新增菜品“贵妃龙虾汤泡饭”，分类 汤类（ID 21），价格 88 元吗？"]
    assert any(event == "confirmation" for event, _payload in events)


async def test_admin_stream_surfaces_tool_error_details() -> None:
    model = ToolCapableFakeModel(
        responses=[
            AIMessage(
                content="",
                tool_calls=[{"name": "admin_menu_search", "args": {}, "id": "call-menu"}],
            ),
            AIMessage(content=""),
        ]
    )
    graph = AdminOperationsAgentGraph(
        settings=Settings(),
        tools=FailingAdminRegistry(),  # type: ignore[arg-type]
        model=model,
    )

    events = [event async for event in graph.stream(admin_request("查询菜品分类"))]

    deltas = [payload["text"] for event, payload in events if event == "delta"]
    assert deltas == [
        "工具调用失败：Spring 内部服务暂时不可用，请检查后端是否已启动、端口配置是否正确（UPSTREAM_ERROR）。"
    ]
