import json

from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.responses import StreamingResponse

from app.dependencies import get_admin_operations_graph
from app.graphs.admin_operations import AdminOperationsAgentGraph
from app.schemas.chat import ChatRequest, ChatResponse
from app.security.service_auth import require_agent_api_token

router = APIRouter()


@router.post(
    "/chat",
    response_model=ChatResponse,
    status_code=status.HTTP_202_ACCEPTED,
)
async def admin_chat(
    request: ChatRequest,
    _: None = Depends(require_agent_api_token),
    graph: AdminOperationsAgentGraph = Depends(get_admin_operations_graph),
) -> ChatResponse:
    if request.actor.type != "admin" or "ADMIN" not in request.actor.roles:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Admin actor required")
    return await graph.run(request)


@router.post("/chat/stream", response_class=StreamingResponse)
async def admin_chat_stream(
    request: ChatRequest,
    _: None = Depends(require_agent_api_token),
    graph: AdminOperationsAgentGraph = Depends(get_admin_operations_graph),
) -> StreamingResponse:
    if request.actor.type != "admin" or "ADMIN" not in request.actor.roles:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Admin actor required")

    async def events():
        async for event_name, data in graph.stream(request):
            payload = json.dumps(data, ensure_ascii=False, separators=(",", ":"))
            yield f"event: {event_name}\ndata: {payload}\n\n"

    return StreamingResponse(events(), media_type="text/event-stream")
