import pytest
from fastapi import HTTPException

from app.core.config import settings
from app.security.service_auth import require_agent_api_token


@pytest.mark.asyncio
async def test_service_auth_accepts_matching_token() -> None:
    previous = settings.agent_service_auth_token
    settings.agent_service_auth_token = "test-token"
    try:
        await require_agent_api_token("test-token")
    finally:
        settings.agent_service_auth_token = previous


@pytest.mark.asyncio
async def test_service_auth_rejects_unconfigured_token() -> None:
    previous = settings.agent_service_auth_token
    settings.agent_service_auth_token = ""
    try:
        with pytest.raises(HTTPException) as exc_info:
            await require_agent_api_token("any-token")
    finally:
        settings.agent_service_auth_token = previous

    assert exc_info.value.status_code == 503
