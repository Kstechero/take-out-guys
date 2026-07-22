from uuid import uuid4

from fastapi import APIRouter, Depends, HTTPException, status

from app.confirmations import ConfirmationError
from app.dependencies import get_admin_operations_graph, get_user_support_graph
from app.graphs.admin_operations import AdminOperationsAgentGraph
from app.graphs.user_support import UserSupportAgentGraph
from app.schemas.chat import ChatRequest, ChatResponse
from app.schemas.confirmation import ConfirmationDecision, ResumeRequest
from app.security.service_auth import require_agent_api_token

router = APIRouter()


@router.post("/{thread_id}/resume", response_model=ChatResponse)
async def resume_thread(
    thread_id: str,
    request: ResumeRequest,
    _: None = Depends(require_agent_api_token),
    user_graph: UserSupportAgentGraph = Depends(get_user_support_graph),
    admin_graph: AdminOperationsAgentGraph = Depends(get_admin_operations_graph),
) -> ChatResponse:
    if not thread_id.strip():
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, detail="thread required")
    if request.agent_name == "admin_operations_agent":
        if request.actor.type != "admin":
            raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Admin actor required")
        graph: UserSupportAgentGraph = admin_graph
    else:
        if request.actor.type != "user":
            raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="User actor required")
        graph = user_graph

    if request.decision == ConfirmationDecision.reject:
        if graph.confirmations is None:
            raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="No pending confirmation")
        try:
            record = graph.confirmations.store.reject(
                request.confirmation_token,
                agent_name=request.agent_name,
                actor=request.actor,
                session_id=thread_id,
            )
        except ConfirmationError as exc:
            raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail=str(exc)) from exc
        await graph.resolve_pending_interrupt(
            ChatRequest(
                request_id=request.request_id,
                actor=request.actor,
                session_id=thread_id,
                message="拒绝待确认操作",
            ),
            thread_id,
            {"decision": "reject", "summary": record.summary},
        )
        return ChatResponse(
            request_id=request.request_id,
            session_id=thread_id,
            answer=f"已取消操作：{record.summary}",
            status="completed",
            trace_id=str(uuid4()),
        )

    if request.decision == ConfirmationDecision.edit:
        if graph.confirmations is None or request.edited_arguments is None:
            raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="No pending confirmation")
        try:
            proposal = graph.confirmations.edit_action(
                request.confirmation_token,
                agent_name=request.agent_name,
                actor=request.actor,
                session_id=thread_id,
                edited=request.edited_arguments,
            )
        except ConfirmationError as exc:
            raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail=str(exc)) from exc
        confirmation = proposal["confirmation"]
        await graph.resolve_pending_interrupt(
            ChatRequest(
                request_id=request.request_id,
                actor=request.actor,
                session_id=thread_id,
                message="修改待确认操作",
            ),
            thread_id,
            {"decision": "edit", "summary": confirmation["summary"]},
        )
        return ChatResponse(
            request_id=request.request_id,
            session_id=thread_id,
            answer=str(confirmation["summary"]),
            status="waiting_user",
            confirmation=confirmation,
            trace_id=str(uuid4()),
        )

    return await graph.run(
        ChatRequest(
            request_id=request.request_id,
            actor=request.actor,
            session_id=thread_id,
            message="批准执行待确认操作",
            confirmed_action_token=request.confirmation_token,
        )
    )
