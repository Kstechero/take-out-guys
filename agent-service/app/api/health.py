from fastapi import APIRouter

from app.core.config import settings
from app.schemas.health import HealthResponse
from app.observability import metrics_response

router = APIRouter(tags=["health"])


@router.get("/health", response_model=HealthResponse)
async def health_check() -> HealthResponse:
    return HealthResponse(
        status="ok",
        service=settings.app_name,
        environment=settings.app_env,
        llm_configured=bool(settings.llm_api_key),
        llm_model=settings.llm_model,
    )


@router.get("/metrics", include_in_schema=False)
async def metrics():
    return metrics_response()
