from secrets import compare_digest

from fastapi import Header, HTTPException, status

from app.core.config import settings


async def require_agent_api_token(
    x_agent_service_token: str | None = Header(default=None),
) -> None:
    expected = settings.agent_service_auth_token
    if not expected:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Agent API service authentication is not configured",
        )
    if x_agent_service_token is None or not compare_digest(x_agent_service_token, expected):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid agent service token",
        )
