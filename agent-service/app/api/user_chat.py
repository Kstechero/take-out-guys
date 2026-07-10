from fastapi import APIRouter, status

from app.schemas.chat import ChatRequest, ChatResponse

router = APIRouter()


@router.post(
    "/chat",
    response_model=ChatResponse,
    status_code=status.HTTP_202_ACCEPTED,
)
async def user_chat(request: ChatRequest) -> ChatResponse:
    return ChatResponse(
        request_id=request.request_id,
        session_id=request.session_id,
        answer="Agent workflow is not implemented yet. Continue with the next planned slice.",
        trace_id=None,
    )
