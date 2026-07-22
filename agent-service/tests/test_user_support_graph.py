from pathlib import Path
from typing import Any

from langchain_core.language_models.fake_chat_models import FakeMessagesListChatModel
from langchain_core.messages import AIMessage, BaseMessage, HumanMessage
from langchain_core.outputs import ChatResult
from langchain_core.tools import StructuredTool
from pydantic import BaseModel, Field

from app.core.config import Settings
from app.confirmations import ConfirmationService, ConfirmationStore
from app.graphs.user_support import UserSupportAgentGraph
from app.schemas.chat import ActorContext, ChatRequest
from app.tools.knowledge_search import NO_EVIDENCE_MESSAGE


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


class FailingFakeModel(ToolCapableFakeModel):
    def _generate(
        self,
        messages: list[BaseMessage],
        stop: list[str] | None = None,
        run_manager: Any = None,
        **kwargs: Any,
    ) -> ChatResult:
        raise RuntimeError("secret upstream failure")


class EmptyInput(BaseModel):
    pass


class MenuInput(BaseModel):
    query: str
    limit: int = 5


class RecentOrdersInput(BaseModel):
    status: int | None = None
    limit: int = 5


class KnowledgeInput(BaseModel):
    query: str
    domain: str | None = None


class StubRegistry:
    def __init__(self) -> None:
        self.shop_calls = 0
        self.menu_calls = 0
        self.recent_order_calls = 0
        self.knowledge_calls = 0

    def langchain_tools(
        self, *, request_id: str, actor: ActorContext, session_id: str | None = None
    ) -> list[object]:
        async def shop_status() -> dict[str, object]:
            self.shop_calls += 1
            return {"status": "OPEN", "updated_at": "2026-07-13T10:00:00Z"}

        async def menu_search(query: str, limit: int = 5) -> dict[str, object]:
            self.menu_calls += 1
            return {
                "items": [{"name": "番茄炒蛋", "price": 18}],
                "citations": [
                    {
                        "title": "实时菜单",
                        "source": "internal://menu",
                        "updated_at": "2026-07-13T09:30:00Z",
                    }
                ],
            }

        async def list_recent_orders(
            status: int | None = None,
            limit: int = 5,
        ) -> dict[str, object]:
            self.recent_order_calls += 1
            return {
                "items": [
                    {
                        "id": 10,
                        "number": "ORDER-10",
                        "status": status or 5,
                        "status_label": "COMPLETED",
                    }
                ],
                "total": 1,
            }

        async def search_service_knowledge(
            query: str,
            domain: str | None = None,
        ) -> dict[str, object]:
            self.knowledge_calls += 1
            if query == "配送规则":
                return {
                    "available": True,
                    "found": True,
                    "snippets": [{"content": "配送规则正文", "score": 0.8, "domain": "order"}],
                    "citations": [
                        {
                            "title": "下单与配送规则",
                            "source": "docs/agent-service/knowledge/user/order-and-delivery-guide.md",
                            "updated_at": "2026-07-13",
                        }
                    ],
                    "answer_guidance": "仅依据 snippets 回答。",
                }
            return {
                "available": True,
                "found": False,
                "snippets": [],
                "citations": [],
                "answer_guidance": NO_EVIDENCE_MESSAGE,
            }

        return [
            StructuredTool.from_function(
                coroutine=shop_status,
                name="get_shop_status",
                description="Get live shop status.",
                args_schema=EmptyInput,
            ),
            StructuredTool.from_function(
                coroutine=menu_search,
                name="menu_search",
                description="Search the live menu.",
                args_schema=MenuInput,
            ),
            StructuredTool.from_function(
                coroutine=list_recent_orders,
                name="list_recent_orders",
                description="List the current user's recent orders.",
                args_schema=RecentOrdersInput,
            ),
            StructuredTool.from_function(
                coroutine=search_service_knowledge,
                name="search_service_knowledge",
                description="Search approved user-visible knowledge.",
                args_schema=KnowledgeInput,
            ),
        ]


class ConfirmationRegistry:
    def __init__(self, confirmations: ConfirmationService) -> None:
        self.confirmations = confirmations

    def langchain_tools(
        self, *, request_id: str, actor: ActorContext, session_id: str | None = None
    ) -> list[object]:
        async def clear_cart() -> dict[str, object]:
            return self.confirmations.propose_user_action(
                actor=actor,
                session_id=session_id or "",
                action="clear",
                arguments={},
                summary="确认清空购物车吗？",
            )

        return [
            StructuredTool.from_function(
                coroutine=clear_cart,
                name="clear_cart",
                description="提出清空购物车并等待确认。",
                args_schema=EmptyInput,
            )
        ]


def request(message: str, *, request_id: str = "req-1", actor_id: str = "1001") -> ChatRequest:
    return ChatRequest(
        request_id=request_id,
        actor=ActorContext(type="user", id=actor_id, roles=["USER"]),
        session_id="session-1",
        message=message,
    )


def graph_with(model: ToolCapableFakeModel | None) -> tuple[UserSupportAgentGraph, StubRegistry]:
    registry = StubRegistry()
    graph = UserSupportAgentGraph(
        settings=Settings(),
        tools=registry,
        model=model,
    )
    return graph, registry


async def test_graph_executes_model_selected_shop_status_tool() -> None:
    model = ToolCapableFakeModel(
        responses=[
            AIMessage(
                content="",
                tool_calls=[{"name": "get_shop_status", "args": {}, "id": "call-shop"}],
            ),
            AIMessage(content="门店当前正在营业。"),
        ]
    )
    graph, registry = graph_with(model)

    response = await graph.run(request("现在营业吗"))

    assert registry.shop_calls == 1
    assert response.answer == "门店当前正在营业。"


async def test_graph_executes_menu_tool_and_returns_citations() -> None:
    model = ToolCapableFakeModel(
        responses=[
            AIMessage(
                content="",
                tool_calls=[
                    {
                        "name": "menu_search",
                        "args": {"query": "不辣午餐", "limit": 5},
                        "id": "call-menu",
                    }
                ],
            ),
            AIMessage(content="推荐番茄炒蛋，价格 18 元。"),
        ]
    )
    graph, registry = graph_with(model)

    response = await graph.run(request("推荐一份不辣午餐"))

    assert registry.menu_calls == 1
    assert response.citations[0].source == "internal://menu"


async def test_graph_stops_repeated_menu_tool_calls_and_finalizes_answer() -> None:
    repeated_call = {
        "name": "menu_search",
        "args": {"query": "鸡肉", "limit": 3},
        "id": "call-menu-1",
    }
    model = ToolCapableFakeModel(
        responses=[
            AIMessage(content="", tool_calls=[repeated_call]),
            AIMessage(
                content="",
                tool_calls=[{**repeated_call, "id": "call-menu-2"}],
            ),
            AIMessage(content="推荐菜单中的鸡肉菜品。"),
        ]
    )
    graph, registry = graph_with(model)

    response = await graph.run(request("推荐三道鸡肉菜品"))

    assert registry.menu_calls == 1
    assert response.status == "completed"
    assert response.answer == "推荐菜单中的鸡肉菜品。"


async def test_graph_executes_model_selected_recent_orders_tool() -> None:
    model = ToolCapableFakeModel(
        responses=[
            AIMessage(
                content="",
                tool_calls=[
                    {
                        "name": "list_recent_orders",
                        "args": {"status": 5, "limit": 3},
                        "id": "call-recent-orders",
                    }
                ],
            ),
            AIMessage(content="Your latest completed order is ORDER-10."),
        ]
    )
    graph, registry = graph_with(model)

    response = await graph.run(request("Show my recent completed orders"))

    assert registry.recent_order_calls == 1
    assert response.answer == "Your latest completed order is ORDER-10."


async def test_graph_forces_no_evidence_response_after_knowledge_miss() -> None:
    model = ToolCapableFakeModel(
        responses=[
            AIMessage(
                content="",
                tool_calls=[
                    {
                        "name": "search_service_knowledge",
                        "args": {"query": "无人机配送"},
                        "id": "call-knowledge-miss",
                    }
                ],
            ),
            AIMessage(content="系统支持无人机配送。"),
        ]
    )
    graph, registry = graph_with(model)

    response = await graph.run(request("支持无人机配送吗"))

    assert registry.knowledge_calls == 1
    assert response.answer == NO_EVIDENCE_MESSAGE
    assert response.citations == []


async def test_graph_stream_returns_knowledge_citation_event() -> None:
    model = ToolCapableFakeModel(
        responses=[
            AIMessage(
                content="",
                tool_calls=[
                    {
                        "name": "search_service_knowledge",
                        "args": {"query": "配送规则", "domain": "order"},
                        "id": "call-knowledge-hit",
                    }
                ],
            ),
            AIMessage(content="当前系统没有固定配送半径。"),
        ]
    )
    graph, _ = graph_with(model)

    events = [event async for event in graph.stream(request("配送范围是什么"))]

    citations = [payload for event, payload in events if event == "citation"]
    nodes = [payload["node"] for event, payload in events if event == "node_started"]
    assert citations[0]["title"] == "下单与配送规则"
    assert "validate_user_context" in nodes
    assert "classify_user_intent" in nodes
    assert "make_user_plan" in nodes
    assert "check_user_result" in nodes
    assert any(event == "tool_started" for event, _payload in events)
    assert any(event == "tool_finished" for event, _payload in events)
    assert any(event == "done" for event, _payload in events)


async def test_graph_keeps_history_for_same_actor_and_session() -> None:
    model = ToolCapableFakeModel(
        responses=[AIMessage(content="第一轮回答"), AIMessage(content="第二轮回答")]
    )
    graph, _ = graph_with(model)

    await graph.run(request("第一轮问题", request_id="req-1"))
    await graph.run(request("继续说明", request_id="req-2"))

    second_call_human_messages = [
        message.content for message in model.seen_messages[1] if isinstance(message, HumanMessage)
    ]
    assert second_call_human_messages == ["第一轮问题", "继续说明"]


async def test_graph_restores_history_after_process_restart(tmp_path: Path) -> None:
    checkpoint_path = str(tmp_path / "graph-checkpoints.sqlite3")
    first_model = ToolCapableFakeModel(responses=[AIMessage(content="第一轮回答")])
    first_graph, _ = graph_with(first_model)
    first_graph.checkpoint_path = checkpoint_path
    await first_graph.run(request("第一轮问题", request_id="req-restart-1"))
    await first_graph.close()

    second_model = ToolCapableFakeModel(responses=[AIMessage(content="第二轮回答")])
    second_graph, _ = graph_with(second_model)
    second_graph.checkpoint_path = checkpoint_path
    try:
        await second_graph.run(request("重启后继续", request_id="req-restart-2"))
        human_messages = [
            message.content
            for message in second_model.seen_messages[0]
            if isinstance(message, HumanMessage)
        ]
        assert human_messages == ["第一轮问题", "重启后继续"]
    finally:
        await second_graph.close()


async def test_graph_interrupts_and_resumes_confirmed_write() -> None:
    class WriteClient:
        def __init__(self) -> None:
            self.calls = 0

        async def change_cart(self, **_kwargs: Any) -> dict[str, Any]:
            self.calls += 1
            return {"status": "APPLIED"}

    client = WriteClient()
    confirmations = ConfirmationService(
        store=ConfirmationStore(":memory:"),
        client=client,  # type: ignore[arg-type]
    )
    model = ToolCapableFakeModel(
        responses=[
            AIMessage(
                content="",
                tool_calls=[{"name": "clear_cart", "args": {}, "id": "call-clear"}],
            )
        ]
    )
    graph = UserSupportAgentGraph(
        settings=Settings(),
        tools=ConfirmationRegistry(confirmations),  # type: ignore[arg-type]
        model=model,
        confirmations=confirmations,
    )

    pending = await graph.run(request("清空购物车", request_id="req-interrupt"))
    compiled = graph._compile_for_request(request("清空购物车"), "session-1")
    snapshot = await compiled.aget_state(
        {"configurable": {"thread_id": "user:1001:session-1"}}
    )

    assert pending.confirmation is not None
    assert snapshot.tasks[0].interrupts
    completed = await graph.run(
        ChatRequest(
            request_id="req-resume",
            actor=ActorContext(type="user", id="1001", roles=["USER"]),
            session_id="session-1",
            message="确认",
            confirmed_action_token=str(pending.confirmation["token"]),
        )
    )
    resolved = await compiled.aget_state(
        {"configurable": {"thread_id": "user:1001:session-1"}}
    )

    assert client.calls == 1
    assert "操作已完成" in completed.answer
    assert not resolved.tasks


async def test_interrupted_write_resumes_after_process_restart(tmp_path: Path) -> None:
    class WriteClient:
        def __init__(self) -> None:
            self.calls = 0

        async def change_cart(self, **_kwargs: Any) -> dict[str, Any]:
            self.calls += 1
            return {"status": "APPLIED"}

    client = WriteClient()
    confirmation_path = str(tmp_path / "confirmations.sqlite3")
    checkpoint_path = str(tmp_path / "checkpoints.sqlite3")
    first_confirmations = ConfirmationService(
        store=ConfirmationStore(confirmation_path),
        client=client,  # type: ignore[arg-type]
    )
    first_graph = UserSupportAgentGraph(
        settings=Settings(),
        tools=ConfirmationRegistry(first_confirmations),  # type: ignore[arg-type]
        model=ToolCapableFakeModel(
            responses=[
                AIMessage(
                    content="",
                    tool_calls=[{"name": "clear_cart", "args": {}, "id": "call-restart"}],
                )
            ]
        ),
        confirmations=first_confirmations,
        checkpoint_path=checkpoint_path,
    )
    pending = await first_graph.run(request("清空购物车", request_id="req-pause"))
    await first_graph.close()

    second_confirmations = ConfirmationService(
        store=ConfirmationStore(confirmation_path),
        client=client,  # type: ignore[arg-type]
    )
    second_graph = UserSupportAgentGraph(
        settings=Settings(),
        tools=ConfirmationRegistry(second_confirmations),  # type: ignore[arg-type]
        model=ToolCapableFakeModel(responses=[AIMessage(content="unused")]),
        confirmations=second_confirmations,
        checkpoint_path=checkpoint_path,
    )
    try:
        completed = await second_graph.run(
            ChatRequest(
                request_id="req-after-restart",
                actor=ActorContext(type="user", id="1001", roles=["USER"]),
                session_id="session-1",
                message="确认",
                confirmed_action_token=str(pending.confirmation["token"]),
            )
        )
        assert client.calls == 1
        assert "操作已完成" in completed.answer
    finally:
        await second_graph.close()


async def test_graph_isolates_history_between_actors() -> None:
    model = ToolCapableFakeModel(
        responses=[AIMessage(content="用户一回答"), AIMessage(content="用户二回答")]
    )
    graph, _ = graph_with(model)

    await graph.run(request("用户一问题", actor_id="1001"))
    await graph.run(request("用户二问题", actor_id="1002"))

    second_call_human_messages = [
        message.content for message in model.seen_messages[1] if isinstance(message, HumanMessage)
    ]
    assert second_call_human_messages == ["用户二问题"]


async def test_graph_returns_safe_fallback_when_model_fails() -> None:
    graph, _ = graph_with(FailingFakeModel(responses=[AIMessage(content="unused")]))

    response = await graph.run(request("现在营业吗"))

    assert response.answer == "智能助手暂时不可用，请稍后再试。"
    assert "secret" not in response.answer


async def test_graph_returns_safe_fallback_when_model_is_not_configured() -> None:
    graph, _ = graph_with(None)

    response = await graph.run(request("现在营业吗"))

    assert response.answer == "智能助手暂时不可用，请稍后再试。"
