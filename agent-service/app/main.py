from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.api import admin_chat, health, threads, user_chat
from app.core.config import settings
from app.dependencies import get_admin_operations_graph, get_user_support_graph
from app.observability import MetricsMiddleware


@asynccontextmanager
async def lifespan(_: FastAPI):
    yield
    for factory in (get_user_support_graph, get_admin_operations_graph):
        if factory.cache_info().currsize:
            await factory().close()
            factory.cache_clear()


def create_app() -> FastAPI:
    app = FastAPI(
        title=settings.app_name,
        version="0.1.0",
        description="FastAPI agent service aligned with the internal Spring API contract.",
        lifespan=lifespan,
    )
    app.add_middleware(
        MetricsMiddleware,
        requests_per_minute=settings.rate_limit_requests_per_minute,
    )
    app.include_router(health.router)
    app.include_router(user_chat.router, prefix="/v1/user", tags=["user-agent"])
    app.include_router(admin_chat.router, prefix="/v1/admin", tags=["admin-agent"])
    app.include_router(threads.router, prefix="/v1/threads", tags=["agent-threads"])
    return app


app = create_app()
