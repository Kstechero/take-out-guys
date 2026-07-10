from fastapi import FastAPI

from app.api import health, user_chat
from app.core.config import settings


def create_app() -> FastAPI:
    app = FastAPI(
        title=settings.app_name,
        version="0.1.0",
        description="FastAPI scaffold for the Takeout Guys LangChain Agent Service.",
    )
    app.include_router(health.router)
    app.include_router(user_chat.router, prefix="/v1/user", tags=["user-agent"])
    return app


app = create_app()
