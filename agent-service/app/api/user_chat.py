import json
from langchain_core.messages import HumanMessage, SystemMessage

from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.responses import StreamingResponse

from app.dependencies import get_user_support_graph
from app.graphs.user_support import UserSupportAgentGraph
from app.schemas.chat import ChatRequest, ChatResponse
from app.security.service_auth import require_agent_api_token
from app.clients.spring_internal import SpringInternalApiClient
from app.core.config import settings
from app.schemas.user_tools import (
    RecommendationItem,
    RecommendationRequest,
    RecommendationResult,
    ReviewDraftRequest,
    ReviewDraftResult,
)

router = APIRouter()


@router.post("/recommendations", response_model=RecommendationResult)
async def recommend(
    request: RecommendationRequest,
    _: None = Depends(require_agent_api_token),
    graph: UserSupportAgentGraph = Depends(get_user_support_graph),
) -> RecommendationResult:
    if request.actor.type != "user":
        raise HTTPException(status_code=403, detail="User actor required")
    data = await SpringInternalApiClient(settings).menu_search(
        request_id=request.request_id,
        actor=request.actor,
        query=request.requirement,
        limit=request.limit,
        budget_max=request.budget,
        dietary_preferences=request.requirement,
    )
    raw_items = list(data.get("items") or [])[: request.limit]
    names = "、".join(str(item.get("name")) for item in raw_items)
    summary = f"根据“{request.requirement}”筛选到：{names}" if names else "暂无符合条件的在售商品。"
    if raw_items and graph.model is not None:
        response = await graph.model.ainvoke([
            SystemMessage(content="你是外卖推荐助手，只能依据候选商品生成一句简洁中文推荐总结。"),
            HumanMessage(content=json.dumps({
                "requirement": request.requirement,
                "budget": str(request.budget) if request.budget else None,
                "people_count": request.people_count,
                "candidates": raw_items,
            }, ensure_ascii=False)),
        ])
        if isinstance(response.content, str) and response.content.strip():
            summary = response.content.strip()
    items = [RecommendationItem(
        type=str(item.get("type") or "dish"),
        id=int(item["id"]),
        name=str(item.get("name") or ""),
        price=item.get("price"),
        image=item.get("image"),
        reason=f"符合“{request.requirement}”并且当前在售",
    ) for item in raw_items]
    return RecommendationResult(items=items, summary=summary)


@router.post("/reviews/draft", response_model=ReviewDraftResult)
async def review_draft(
    request: ReviewDraftRequest,
    _: None = Depends(require_agent_api_token),
    graph: UserSupportAgentGraph = Depends(get_user_support_graph),
) -> ReviewDraftResult:
    if request.actor.type != "user":
        raise HTTPException(status_code=403, detail="User actor required")
    checked = await SpringInternalApiClient(settings).check_review_draft(
        request_id=request.request_id,
        actor=request.actor,
        order_id=request.order_id,
        dish_id=request.dish_id,
        rating=request.rating,
        highlights=request.highlights,
    )
    if not checked.safe:
        return ReviewDraftResult(content=checked.highlights, flagged=True)
    content = f"{checked.dish_name}味道不错，整体体验很好。"
    if graph.model is not None:
        response = await graph.model.ainvoke([
            SystemMessage(content="生成一段30到100字的中文菜品评价草稿，不得声称已经发布。"),
            HumanMessage(content=json.dumps({
                "dish": checked.dish_name, "rating": checked.rating,
                "highlights": checked.highlights, "style": request.style,
            }, ensure_ascii=False)),
        ])
        if isinstance(response.content, str) and response.content.strip():
            content = response.content.strip()
    return ReviewDraftResult(content=content, flagged=False)


@router.post(
    "/chat",
    response_model=ChatResponse,
    status_code=status.HTTP_202_ACCEPTED,
)
async def user_chat(
    request: ChatRequest,
    _: None = Depends(require_agent_api_token),
    graph: UserSupportAgentGraph = Depends(get_user_support_graph),
) -> ChatResponse:
    if request.actor.type != "user":
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="User actor required")
    return await graph.run(request)


@router.post("/chat/stream", response_class=StreamingResponse)
async def user_chat_stream(
    request: ChatRequest,
    _: None = Depends(require_agent_api_token),
    graph: UserSupportAgentGraph = Depends(get_user_support_graph),
) -> StreamingResponse:
    if request.actor.type != "user":
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="User actor required")

    async def events():
        async for event_name, data in graph.stream(request):
            payload = json.dumps(data, ensure_ascii=False, separators=(",", ":"))
            yield f"event: {event_name}\ndata: {payload}\n\n"

    return StreamingResponse(events(), media_type="text/event-stream")
