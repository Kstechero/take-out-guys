from __future__ import annotations

import json
from asyncio import Lock
from collections.abc import AsyncIterator
from dataclasses import dataclass, field
from pathlib import Path
from uuid import uuid4

import structlog
import aiosqlite
from langchain_core.language_models.chat_models import BaseChatModel
from langchain_core.messages import (
    AIMessage,
    AIMessageChunk,
    BaseMessage,
    HumanMessage,
    SystemMessage,
    ToolMessage,
)
from langchain_openai import ChatOpenAI
from langgraph.checkpoint.base import BaseCheckpointSaver
from langgraph.checkpoint.memory import MemorySaver
from langgraph.checkpoint.sqlite.aio import AsyncSqliteSaver
from langgraph.checkpoint.postgres.aio import AsyncPostgresSaver
from langgraph.graph import END, MessagesState, StateGraph
from langgraph.prebuilt import ToolNode
from langgraph.types import Command, interrupt

from app.clients.spring_internal import SpringInternalApiClient
from app.confirmations import ConfirmationError, ConfirmationService, ConfirmationStore
from app.core.config import Settings
from app.prompts.user import USER_AGENT_SYSTEM_PROMPT
from app.rag.retriever import build_knowledge_retriever
from app.schemas.chat import ChatRequest, ChatResponse, SourceCitation
from app.tools.addresses import AddressesTool
from app.tools.cart import CartTool
from app.tools.coupons import AvailableCouponsTool
from app.tools.menu_search import MenuSearchTool
from app.tools.knowledge_search import KnowledgeSearchTool, NO_EVIDENCE_MESSAGE
from app.tools.order_detail import OrderDetailTool
from app.tools.recent_orders import RecentOrdersTool
from app.tools.review_draft import ReviewDraftTool
from app.tools.registry import UserToolRegistry
from app.tools.sensitive_words import SensitiveWordsTool
from app.tools.shop_status import ShopStatusTool
from app.tools.user_mutations import UserMutationTools

logger = structlog.get_logger(__name__)


class UserAgentState(MessagesState):
    intent: str
    plan: list[str]
    response_status: str


@dataclass(slots=True)
class UserSupportAgentGraph:
    settings: Settings
    tools: UserToolRegistry
    model: BaseChatModel | None
    checkpointer: BaseCheckpointSaver = field(default_factory=MemorySaver)
    confirmations: ConfirmationService | None = None
    checkpoint_path: str | None = None
    _checkpointer_ready: bool = False
    _checkpointer_lock: Lock = field(default_factory=Lock)
    _postgres_context: object | None = None

    async def run(self, request: ChatRequest) -> ChatResponse:
        trace_id = str(uuid4())
        session_id = request.session_id or self._build_session_id(request.actor.id)

        if request.confirmed_action_token:
            return await self._execute_confirmation(request, session_id, trace_id)

        if self.model is None:
            return self._unavailable_response(request, session_id, trace_id)

        try:
            await self._ensure_checkpointer()
            graph = self._compile_for_request(request, session_id)
            result = await graph.ainvoke(
                {"messages": [HumanMessage(content=request.message.strip())]},
                config={
                    "configurable": {"thread_id": self._thread_id(request, session_id)},
                    "recursion_limit": 16,
                },
            )
            messages = result.get("messages", [])
            answer = (
                NO_EVIDENCE_MESSAGE
                if self._knowledge_miss(messages)
                else self._final_answer(messages)
            )
            confirmation = self._extract_confirmation(messages)
            return ChatResponse(
                request_id=request.request_id,
                session_id=session_id,
                answer=str(confirmation["summary"]) if confirmation else answer,
                status="waiting_user" if confirmation else "completed",
                citations=self._extract_citations(messages),
                trace_id=trace_id,
                confirmation=confirmation,
            )
        except Exception as exc:
            logger.warning(
                "agent_graph_failed",
                request_id=request.request_id,
                trace_id=trace_id,
                actor_type=request.actor.type,
                error_type=type(exc).__name__,
            )
            return self._unavailable_response(request, session_id, trace_id)

    async def stream(self, request: ChatRequest) -> AsyncIterator[tuple[str, dict[str, object]]]:
        trace_id = str(uuid4())
        session_id = request.session_id or self._build_session_id(request.actor.id)
        if request.confirmed_action_token:
            response = await self._execute_confirmation(request, session_id, trace_id)
            yield "run_started", {"thread_id": session_id, "trace_id": response.trace_id}
            yield "delta", {"text": response.answer}
            yield "done", {
                "session_id": response.session_id,
                "trace_id": response.trace_id,
                "status": response.status,
            }
            return
        if self.model is None:
            yield "error", {"code": "MODEL_UNAVAILABLE", "message": "智能助手暂时不可用"}
            return

        seen_tools: set[str] = set()
        knowledge_miss = False
        waiting_confirmation = False
        try:
            await self._ensure_checkpointer()
            graph = self._compile_for_request(request, session_id)
            yield "run_started", {"thread_id": session_id, "trace_id": trace_id}
            for node_name in (
                "validate_user_context",
                "classify_user_intent",
                "make_user_plan",
            ):
                yield "node_started", {"node": node_name}
            seen_nodes: set[str] = set()
            async for message, _metadata in graph.astream(
                {"messages": [HumanMessage(content=request.message.strip())]},
                config={
                    "configurable": {"thread_id": self._thread_id(request, session_id)},
                    "recursion_limit": 16,
                },
                stream_mode="messages",
            ):
                node_name = str(_metadata.get("langgraph_node", ""))
                if node_name and node_name not in seen_nodes:
                    seen_nodes.add(node_name)
                    yield "node_started", {"node": node_name}
                if isinstance(message, AIMessageChunk):
                    text = self._content_text(message.content)
                    if text and not knowledge_miss:
                        yield "delta", {"text": text}
                    for tool_call in message.tool_call_chunks:
                        tool_name = tool_call.get("name")
                        if tool_name and tool_name not in seen_tools:
                            seen_tools.add(tool_name)
                            yield "tool_started", {"tool": tool_name}
                            yield "tool_status", {"tool": tool_name, "status": "running"}
                elif isinstance(message, ToolMessage) and message.name:
                    if message.name not in seen_tools:
                        seen_tools.add(message.name)
                        yield "tool_started", {"tool": message.name}
                        yield "tool_status", {"tool": message.name, "status": "running"}
                    yield "node_started", {"node": "check_user_result"}
                    yield "tool_finished", {"tool": message.name, "status": "ok"}
                    yield "tool_status", {"tool": message.name, "status": "completed"}
                    if self._is_knowledge_tool(message.name):
                        payload = self._tool_payload(message.content)
                        knowledge_miss = payload.get("found") is False
                        raw_citations = payload.get("citations")
                        if isinstance(raw_citations, list):
                            for citation in raw_citations:
                                if isinstance(citation, dict):
                                    yield "citation", citation
                    confirmation = self._tool_payload(message.content).get("confirmation")
                    if isinstance(confirmation, dict):
                        waiting_confirmation = True
                        yield "confirmation", confirmation
                        yield "interrupt", {"kind": "confirmation", "request": confirmation}

            if knowledge_miss and not waiting_confirmation:
                yield "delta", {"text": NO_EVIDENCE_MESSAGE}
            yield "node_started", {"node": "compose_user_answer"}
            yield "done", {
                "session_id": session_id,
                "trace_id": trace_id,
                "status": "waiting_user" if waiting_confirmation else "completed",
            }
        except Exception as exc:
            logger.warning(
                "agent_graph_stream_failed",
                request_id=request.request_id,
                trace_id=trace_id,
                actor_type=request.actor.type,
                error_type=type(exc).__name__,
            )
            yield "error", {"code": "AGENT_UNAVAILABLE", "message": "智能助手暂时不可用"}

    def langchain_tools_for_request(
        self, request: ChatRequest, session_id: str | None = None
    ) -> list[object]:
        return self.tools.langchain_tools(
            request_id=request.request_id, actor=request.actor, session_id=session_id
        )

    def _compile_for_request(self, request: ChatRequest, session_id: str):
        tools = self.langchain_tools_for_request(request, session_id)
        model_with_tools = self.model.bind_tools(tools)
        async def validate_user_context(_: UserAgentState) -> dict[str, str]:
            if request.actor.type != "user":
                raise PermissionError("User actor required")
            return {"response_status": "running"}

        async def classify_user_intent(_: UserAgentState) -> dict[str, str]:
            return {"intent": "model_routed"}

        async def make_user_plan(_: UserAgentState) -> dict[str, list[str]]:
            return {"plan": ["select_authorized_tool_or_answer", "compose_grounded_answer"]}

        async def call_model(state: UserAgentState) -> dict[str, list[BaseMessage]]:
            response = await model_with_tools.ainvoke(
                [SystemMessage(content=USER_AGENT_SYSTEM_PROMPT), *state["messages"]]
            )
            return {"messages": [response]}

        def route_after_agent(state: UserAgentState) -> str:
            messages = state["messages"]
            latest = messages[-1] if messages else None
            if not isinstance(latest, AIMessage) or not latest.tool_calls:
                return "compose_user_answer"
            current = {self._tool_call_signature(call) for call in latest.tool_calls}
            previous = self._turn_tool_call_signatures(messages[:-1])
            if current.intersection(previous) or len(previous) >= 3:
                return "finalize_from_tools"
            return "tools"

        async def finalize_from_tools(
            state: UserAgentState,
        ) -> dict[str, list[BaseMessage]]:
            response = await self.model.ainvoke(
                [
                    SystemMessage(
                        content=(
                            USER_AGENT_SYSTEM_PROMPT
                            + "\n你已经获得足够的工具结果。禁止再次调用任何工具；"
                            "必须仅依据现有 ToolMessage 直接给出最终答案。"
                        )
                    ),
                    *state["messages"],
                ]
            )
            text = self._content_text(response.content)
            if not text:
                text = self._grounded_tool_fallback(state["messages"])
            return {"messages": [AIMessage(content=text)]}

        async def check_user_result(
            state: UserAgentState,
        ) -> dict[str, object]:
            confirmation = self._extract_confirmation(state["messages"])
            if confirmation is None:
                return {"response_status": "running"}
            resolution = interrupt({"kind": "confirmation", "request": confirmation})
            decision = resolution.get("decision", "unknown") if isinstance(resolution, dict) else "unknown"
            summary = (
                resolution.get("summary", confirmation.get("summary", ""))
                if isinstance(resolution, dict)
                else confirmation.get("summary", "")
            )
            return {
                "messages": [
                    SystemMessage(content=f"确认流程已由系统处理：decision={decision}; summary={summary}")
                ],
                "response_status": "confirmation_resolved",
            }

        def route_after_check(state: UserAgentState) -> str:
            return (
                "compose_user_answer"
                if state.get("response_status") == "confirmation_resolved"
                else "agent"
            )

        async def compose_user_answer(_: UserAgentState) -> dict[str, str]:
            return {"response_status": "completed"}

        workflow = StateGraph(UserAgentState)
        workflow.add_node("validate_user_context", validate_user_context)
        workflow.add_node("classify_user_intent", classify_user_intent)
        workflow.add_node("make_user_plan", make_user_plan)
        workflow.add_node("agent", call_model)
        workflow.add_node("finalize_from_tools", finalize_from_tools)
        workflow.add_node(
            "tools",
            ToolNode(tools, handle_tool_errors="业务查询暂时不可用，请稍后再试。"),
        )
        workflow.add_node("check_user_result", check_user_result)
        workflow.add_node("compose_user_answer", compose_user_answer)
        workflow.set_entry_point("validate_user_context")
        workflow.add_edge("validate_user_context", "classify_user_intent")
        workflow.add_edge("classify_user_intent", "make_user_plan")
        workflow.add_edge("make_user_plan", "agent")
        workflow.add_conditional_edges(
            "agent",
            route_after_agent,
            {
                "tools": "tools",
                "finalize_from_tools": "finalize_from_tools",
                "compose_user_answer": "compose_user_answer",
            },
        )
        workflow.add_edge("finalize_from_tools", "compose_user_answer")
        workflow.add_edge("tools", "check_user_result")
        workflow.add_conditional_edges(
            "check_user_result",
            route_after_check,
            {"agent": "agent", "compose_user_answer": "compose_user_answer"},
        )
        workflow.add_edge("compose_user_answer", END)
        return workflow.compile(checkpointer=self.checkpointer)

    async def _ensure_checkpointer(self) -> None:
        if self._checkpointer_ready or (
            not self.settings.graph_checkpoint_postgres_dsn and not self.checkpoint_path
        ):
            return
        async with self._checkpointer_lock:
            if self._checkpointer_ready:
                return
            if self.settings.graph_checkpoint_postgres_dsn:
                context = AsyncPostgresSaver.from_conn_string(
                    self.settings.graph_checkpoint_postgres_dsn
                )
                self.checkpointer = await context.__aenter__()
                self._postgres_context = context
                await self.checkpointer.setup()
                self._checkpointer_ready = True
                return
            if not isinstance(self.checkpointer, AsyncSqliteSaver):
                if self.checkpoint_path != ":memory:":
                    Path(self.checkpoint_path).parent.mkdir(parents=True, exist_ok=True)
                self.checkpointer = AsyncSqliteSaver(aiosqlite.connect(self.checkpoint_path))
            await self.checkpointer.conn
            await self.checkpointer.setup()
            self._checkpointer_ready = True

    async def close(self) -> None:
        if self._postgres_context is not None and self._checkpointer_ready:
            await self._postgres_context.__aexit__(None, None, None)  # type: ignore[attr-defined]
            self._postgres_context = None
            self._checkpointer_ready = False
            return
        if isinstance(self.checkpointer, AsyncSqliteSaver) and self._checkpointer_ready:
            await self.checkpointer.conn.close()
            self._checkpointer_ready = False

    def _final_answer(self, messages: list[BaseMessage]) -> str:
        for message in reversed(messages):
            if isinstance(message, AIMessage) and not message.tool_calls:
                content = self._content_text(message.content)
                if content:
                    return content
        return "暂时无法生成可靠回答，请稍后再试。"

    def _extract_citations(self, messages: list[BaseMessage]) -> list[SourceCitation]:
        citations: list[SourceCitation] = []
        seen: set[tuple[str, str]] = set()
        for message in messages:
            if not isinstance(message, ToolMessage):
                continue
            payload = self._tool_payload(message.content)
            raw_citations = payload.get("citations")
            if not isinstance(raw_citations, list):
                continue
            for item in raw_citations:
                if not isinstance(item, dict) or not item.get("title") or not item.get("source"):
                    continue
                key = (str(item["title"]), str(item["source"]))
                if key in seen:
                    continue
                seen.add(key)
                citations.append(
                    SourceCitation(
                        title=key[0],
                        source=key[1],
                        updated_at=str(item["updated_at"]) if item.get("updated_at") else None,
                    )
                )
        return citations

    def _extract_confirmation(self, messages: list[BaseMessage]) -> dict[str, object] | None:
        for message in reversed(messages):
            if isinstance(message, ToolMessage):
                confirmation = self._tool_payload(message.content).get("confirmation")
                if isinstance(confirmation, dict):
                    return confirmation
        return None

    async def _execute_confirmation(
        self, request: ChatRequest, session_id: str, trace_id: str
    ) -> ChatResponse:
        if self.confirmations is None:
            return ChatResponse(
                request_id=request.request_id,
                session_id=session_id,
                answer="当前操作不支持确认执行。",
                status="failed",
                trace_id=trace_id,
            )
        try:
            record, _result, replayed = await self.confirmations.execute_user_action(
                request.confirmed_action_token or "",
                request_id=request.request_id,
                actor=request.actor,
                session_id=session_id,
            )
            await self.resolve_pending_interrupt(
                request,
                session_id,
                {"decision": "approve", "summary": record.summary, "replayed": replayed},
            )
            suffix = "（该操作此前已完成）" if replayed else ""
            return ChatResponse(
                request_id=request.request_id,
                session_id=session_id,
                answer=f"操作已完成：{record.summary}{suffix}",
                trace_id=trace_id,
            )
        except ConfirmationError as exc:
            return ChatResponse(
                request_id=request.request_id,
                session_id=session_id,
                answer=str(exc),
                status="failed",
                trace_id=trace_id,
            )
        except Exception as exc:
            logger.warning(
                "confirmed_action_failed",
                request_id=request.request_id,
                trace_id=trace_id,
                actor_type=request.actor.type,
                error_type=type(exc).__name__,
            )
            return self._unavailable_response(request, session_id, trace_id)

    async def resolve_pending_interrupt(
        self, request: ChatRequest, session_id: str, resolution: dict[str, object]
    ) -> None:
        if self.model is None:
            return
        try:
            await self._ensure_checkpointer()
            graph = self._compile_for_request(request, session_id)
            await graph.ainvoke(
                Command(resume=resolution),
                config={
                    "configurable": {"thread_id": self._thread_id(request, session_id)},
                    "recursion_limit": 16,
                },
            )
        except Exception as exc:
            logger.warning(
                "confirmation_checkpoint_resume_failed",
                request_id=request.request_id,
                actor_type=request.actor.type,
                error_type=type(exc).__name__,
            )

    def _tool_payload(self, content: object) -> dict[str, object]:
        if isinstance(content, dict):
            return content
        if not isinstance(content, str):
            return {}
        try:
            parsed = json.loads(content)
        except json.JSONDecodeError:
            return {}
        return parsed if isinstance(parsed, dict) else {}

    def _turn_tool_call_signatures(self, messages: list[BaseMessage]) -> set[str]:
        last_human = -1
        for index, message in enumerate(messages):
            if isinstance(message, HumanMessage):
                last_human = index
        signatures: set[str] = set()
        for message in messages[last_human + 1 :]:
            if isinstance(message, AIMessage):
                signatures.update(
                    self._tool_call_signature(call) for call in message.tool_calls
                )
        return signatures

    def _tool_call_signature(self, call: dict[str, object]) -> str:
        return json.dumps(
            {"name": call.get("name"), "args": call.get("args", {})},
            ensure_ascii=False,
            sort_keys=True,
            default=str,
        )

    def _grounded_tool_fallback(self, messages: list[BaseMessage]) -> str:
        for message in reversed(messages):
            if not isinstance(message, ToolMessage):
                continue
            payload = self._tool_payload(message.content)
            items = payload.get("items")
            if isinstance(items, list) and items:
                lines: list[str] = []
                for item in items[:5]:
                    if not isinstance(item, dict):
                        continue
                    name = str(item.get("name") or item.get("title") or "未命名项目")
                    price = item.get("price", item.get("amount"))
                    lines.append(f"{name}（{price} 元）" if price is not None else name)
                if lines:
                    return "查询到以下结果：" + "、".join(lines) + "。"
            if payload.get("status") is not None:
                return f"门店当前状态为 {payload['status']}。"
        return "查询已完成，但暂时无法整理结果，请稍后再试。"

    def _knowledge_miss(self, messages: list[BaseMessage]) -> bool:
        found: bool | None = None
        for message in messages:
            if isinstance(message, ToolMessage) and self._is_knowledge_tool(message.name):
                payload = self._tool_payload(message.content)
                if isinstance(payload.get("found"), bool):
                    found = bool(payload["found"])
        return found is False

    def _is_knowledge_tool(self, name: str | None) -> bool:
        return name in {"search_service_knowledge", "search_operational_knowledge"}

    def _content_text(self, content: object) -> str:
        if isinstance(content, str):
            return content.strip()
        if not isinstance(content, list):
            return ""
        parts: list[str] = []
        for item in content:
            if isinstance(item, str):
                parts.append(item)
            elif isinstance(item, dict) and isinstance(item.get("text"), str):
                parts.append(item["text"])
        return "".join(parts).strip()

    def _thread_id(self, request: ChatRequest, session_id: str) -> str:
        return f"{request.actor.type}:{request.actor.id}:{session_id}"

    def _build_session_id(self, actor_id: str) -> str:
        return f"{self.settings.agent_default_session_prefix}-{actor_id}-{uuid4().hex[:12]}"

    def _unavailable_response(
        self,
        request: ChatRequest,
        session_id: str,
        trace_id: str,
    ) -> ChatResponse:
        return ChatResponse(
            request_id=request.request_id,
            session_id=session_id,
            answer="智能助手暂时不可用，请稍后再试。",
            status="unavailable",
            trace_id=trace_id,
        )


def build_chat_model(settings: Settings) -> BaseChatModel | None:
    if not settings.llm_api_key:
        return None
    return ChatOpenAI(
        model_name=settings.llm_model,
        openai_api_base=settings.llm_base_url.rstrip("/"),
        openai_api_key=settings.llm_api_key,
        temperature=settings.llm_temperature,
        request_timeout=settings.llm_timeout_seconds,
        max_tokens=settings.llm_max_tokens,
        max_retries=1,
    )


def build_user_support_graph(settings: Settings) -> UserSupportAgentGraph:
    client = SpringInternalApiClient(settings=settings)
    confirmations = ConfirmationService(
        store=ConfirmationStore(settings.confirmation_store_path),
        client=client,
        ttl_seconds=settings.confirmation_ttl_seconds,
    )
    knowledge_tool = None
    if settings.rag_enabled:
        try:
            knowledge_tool = KnowledgeSearchTool(build_knowledge_retriever(settings))
        except Exception as exc:
            logger.warning("rag_initialization_failed", error_type=type(exc).__name__)
    registry = UserToolRegistry(
        menu_search=MenuSearchTool(client=client),
        shop_status=ShopStatusTool(client=client),
        recent_orders=RecentOrdersTool(client=client),
        order_detail=OrderDetailTool(client=client),
        cart=CartTool(client=client),
        addresses=AddressesTool(client=client),
        coupons=AvailableCouponsTool(client=client),
        sensitive_words=SensitiveWordsTool(client=client),
        review_draft=ReviewDraftTool(client=client),
        knowledge=knowledge_tool,
        mutations=UserMutationTools(confirmations=confirmations),
    )
    return UserSupportAgentGraph(
        settings=settings,
        tools=registry,
        model=build_chat_model(settings),
        checkpoint_path=settings.graph_checkpoint_path,
        confirmations=confirmations,
    )
