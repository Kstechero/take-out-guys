from __future__ import annotations

from dataclasses import dataclass
from uuid import uuid4

import structlog
from langchain_core.messages import AIMessage, BaseMessage, SystemMessage
from langgraph.graph import END, MessagesState, StateGraph
from langgraph.prebuilt import ToolNode
from langgraph.types import interrupt

from app.clients.spring_internal import SpringInternalApiClient
from app.confirmations import ConfirmationError, ConfirmationService, ConfirmationStore
from app.core.config import Settings
from app.graphs.user_support import (
    UserSupportAgentGraph,
    build_chat_model,
)
from app.prompts.admin import ADMIN_AGENT_SYSTEM_PROMPT
from app.rag.retriever import build_knowledge_retriever
from app.schemas.chat import ChatRequest, ChatResponse
from app.tools.admin_mutations import AdminMutationTools
from app.tools.admin_queries import AdminQueryTools
from app.tools.business_overview import AdminBusinessOverviewTool
from app.tools.knowledge_search import KnowledgeSearchTool
from app.tools.menu_search import MenuSearchTool
from app.tools.registry import AdminToolRegistry
from app.tools.sensitive_words import SensitiveWordsTool
from app.tools.shop_status import ShopStatusTool

logger = structlog.get_logger(__name__)

ADMIN_OUT_OF_SCOPE_MESSAGE = (
    "我只能协助处理外卖平台管理端运营相关问题，例如订单、菜品、分类、套餐、优惠券、"
    "评价、门店状态和经营数据。这个问题不属于当前管理端 Agent 的处理范围。"
)


class AdminAgentState(MessagesState):
    admin_scope: dict[str, str]
    response_status: str


@dataclass(slots=True)
class AdminOperationsAgentGraph(UserSupportAgentGraph):
    tools: AdminToolRegistry

    async def run(self, request: ChatRequest) -> ChatResponse:
        trace_id = str(uuid4())
        session_id = request.session_id or self._build_session_id(request.actor.id)
        if not request.confirmed_action_token and not self._is_admin_domain_message(request.message):
            return ChatResponse(
                request_id=request.request_id,
                session_id=session_id,
                answer=ADMIN_OUT_OF_SCOPE_MESSAGE,
                trace_id=trace_id,
            )
        return await UserSupportAgentGraph.run(self, request)

    async def stream(self, request: ChatRequest):
        trace_id = str(uuid4())
        session_id = request.session_id or self._build_session_id(request.actor.id)
        if not request.confirmed_action_token and not self._is_admin_domain_message(request.message):
            yield "run_started", {"thread_id": session_id, "trace_id": trace_id}
            yield "delta", {"text": ADMIN_OUT_OF_SCOPE_MESSAGE}
            yield "done", {
                "session_id": session_id,
                "trace_id": trace_id,
                "status": "completed",
            }
            return
        async for event in UserSupportAgentGraph.stream(self, request):
            yield event

    def _is_admin_domain_message(self, message: str) -> bool:
        text = message.strip().lower()
        if not text:
            return False
        blocked_keywords = {
            "冒泡排序",
            "bubble sort",
            "排序算法",
            "python",
            "java",
            "javascript",
            "算法",
            "编程",
            "代码实现",
            "翻译",
            "作文",
            "诗",
            "笑话",
        }
        if any(keyword in text for keyword in blocked_keywords):
            return False
        allowed_keywords = {
            "订单",
            "菜品",
            "菜",
            "菜单",
            "分类",
            "主食",
            "套餐",
            "优惠券",
            "券",
            "评价",
            "评论",
            "门店",
            "营业",
            "打烊",
            "经营",
            "营业额",
            "销售",
            "销量",
            "退款",
            "取消",
            "配送",
            "客服",
            "风险",
            "敏感词",
            "新增",
            "修改",
            "上架",
            "下架",
            "启用",
            "停用",
            "价格",
            "库存",
            "agent",
            "工具",
            "tool",
            "schema",
            "查询",
            "数据",
            "管理端",
            "后台",
            "运营",
            "需要",
            "确认",
            "同意",
            "继续",
            "可以",
            "执行",
        }
        return any(keyword in text for keyword in allowed_keywords)

    def _compile_for_request(self, request: ChatRequest, session_id: str):
        tools = self.langchain_tools_for_request(request, session_id)
        model_with_tools = self.model.bind_tools(self.model_tool_schemas(tools))

        async def call_admin_model(state: AdminAgentState) -> dict[str, list[BaseMessage]]:
            response = await model_with_tools.ainvoke(
                self._model_messages(ADMIN_AGENT_SYSTEM_PROMPT, state["messages"])
            )
            return {"messages": [response]}

        def route_after_admin_agent(state: AdminAgentState) -> str:
            messages = state["messages"]
            latest = messages[-1] if messages else None
            if not isinstance(latest, AIMessage) or not latest.tool_calls:
                return "admin_done"
            current = {self._tool_call_signature(call) for call in latest.tool_calls}
            previous = self._turn_tool_call_signatures(messages[:-1])
            if current.intersection(previous) or len(previous) >= 3:
                return "finalize_from_tools"
            return "admin_tools"

        async def finalize_from_tools(
            state: AdminAgentState,
        ) -> dict[str, list[BaseMessage]]:
            response = await self.model.ainvoke(
                self._model_messages(
                    ADMIN_AGENT_SYSTEM_PROMPT
                    + "\n你已经获得足够的运营工具结果。禁止再次调用任何工具；"
                    "必须仅依据现有 ToolMessage 直接给出最终答案。",
                    state["messages"],
                )
            )
            text = self._content_text(response.content)
            if not text:
                text = self._grounded_tool_fallback(state["messages"])
            return {"messages": [AIMessage(content=text)]}

        async def check_admin_result(state: AdminAgentState) -> dict[str, object]:
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
                    SystemMessage(content=f"管理确认已由系统处理：decision={decision}; summary={summary}")
                ],
                "response_status": "confirmation_resolved",
            }

        def route_after_check(state: AdminAgentState) -> str:
            return "admin_done" if state.get("response_status") == "confirmation_resolved" else "admin_agent"

        async def admin_done(_: AdminAgentState) -> dict[str, str]:
            return {"response_status": "completed"}

        workflow = StateGraph(AdminAgentState)
        workflow.add_node("admin_agent", call_admin_model)
        workflow.add_node("finalize_from_tools", finalize_from_tools)
        workflow.add_node(
            "admin_tools",
            ToolNode(tools, handle_tool_errors=self._tool_error_payload),
        )
        workflow.add_node("check_admin_result", check_admin_result)
        workflow.add_node("admin_done", admin_done)
        workflow.set_entry_point("admin_agent")
        workflow.add_conditional_edges(
            "admin_agent",
            route_after_admin_agent,
            {
                "admin_tools": "admin_tools",
                "finalize_from_tools": "finalize_from_tools",
                "admin_done": "admin_done",
            },
        )
        workflow.add_edge("finalize_from_tools", "admin_done")
        workflow.add_edge("admin_tools", "check_admin_result")
        workflow.add_conditional_edges(
            "check_admin_result",
            route_after_check,
            {"admin_agent": "admin_agent", "admin_done": "admin_done"},
        )
        workflow.add_edge("admin_done", END)
        return workflow.compile(checkpointer=self.checkpointer)

    def _thread_id(self, request: ChatRequest, session_id: str) -> str:
        return f"admin_operations_agent:{request.actor.id}:{session_id}"

    async def _execute_confirmation(
        self, request: ChatRequest, session_id: str, trace_id: str
    ) -> ChatResponse:
        if self.confirmations is None:
            return ChatResponse(
                request_id=request.request_id,
                session_id=session_id,
                answer="当前管理操作不支持确认执行。",
                status="failed",
                trace_id=trace_id,
            )
        try:
            record, _result, replayed = await self.confirmations.execute_admin_action(
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
                answer=f"管理操作已完成：{record.summary}{suffix}",
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
                "admin_confirmed_action_failed",
                request_id=request.request_id,
                trace_id=trace_id,
                actor_type=request.actor.type,
                error_type=type(exc).__name__,
                error=str(exc)[:500],
            )
            return self._unavailable_response(request, session_id, trace_id)


def build_admin_operations_graph(settings: Settings) -> AdminOperationsAgentGraph:
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
            logger.warning("admin_rag_initialization_failed", error_type=type(exc).__name__)
    registry = AdminToolRegistry(
        business_overview=AdminBusinessOverviewTool(client=client),
        queries=AdminQueryTools(client=client),
        menu_search=MenuSearchTool(client=client),
        shop_status=ShopStatusTool(client=client),
        sensitive_words=SensitiveWordsTool(client=client),
        knowledge=knowledge_tool,
        mutations=AdminMutationTools(client=client, confirmations=confirmations),
    )
    return AdminOperationsAgentGraph(
        settings=settings,
        tools=registry,
        model=build_chat_model(settings),
        checkpoint_path=settings.graph_checkpoint_path,
        confirmations=confirmations,
    )
