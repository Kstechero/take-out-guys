from __future__ import annotations

import time
from collections import defaultdict, deque

import structlog
from fastapi import Request
from starlette.middleware.base import BaseHTTPMiddleware
from starlette.responses import PlainTextResponse, Response

logger = structlog.get_logger(__name__)
REQUESTS: dict[tuple[str, str, int], int] = defaultdict(int)
DURATION_SUM: dict[tuple[str, str], float] = defaultdict(float)
DURATION_COUNT: dict[tuple[str, str], int] = defaultdict(int)


class MetricsMiddleware(BaseHTTPMiddleware):
    def __init__(self, app, *, requests_per_minute: int) -> None:
        super().__init__(app)
        self.requests_per_minute = requests_per_minute
        self._windows: dict[str, deque[float]] = defaultdict(deque)

    async def dispatch(self, request: Request, call_next) -> Response:
        started = time.perf_counter()
        client = request.client.host if request.client else "unknown"
        now = time.monotonic()
        window = self._windows[client]
        while window and window[0] <= now - 60:
            window.popleft()
        if request.url.path not in {"/health", "/metrics"} and len(window) >= self.requests_per_minute:
            REQUESTS[(request.method, request.url.path, 429)] += 1
            return PlainTextResponse("rate limit exceeded", status_code=429)
        window.append(now)
        response = await call_next(request)
        duration = time.perf_counter() - started
        key = (request.method, request.url.path)
        REQUESTS[(key[0], key[1], response.status_code)] += 1
        DURATION_SUM[key] += duration
        DURATION_COUNT[key] += 1
        logger.info(
            "http_request_completed",
            method=request.method,
            path=request.url.path,
            status=response.status_code,
            duration_ms=round(duration * 1000, 2),
            request_id=request.headers.get("x-request-id"),
        )
        return response


def metrics_response() -> PlainTextResponse:
    lines = [
        "# HELP agent_http_requests_total Agent HTTP requests.",
        "# TYPE agent_http_requests_total counter",
    ]
    for (method, path, status), value in sorted(REQUESTS.items()):
        lines.append(
            f'agent_http_requests_total{{method="{method}",path="{path}",status="{status}"}} {value}'
        )
    lines.extend([
        "# HELP agent_http_request_duration_seconds_sum Total request duration.",
        "# TYPE agent_http_request_duration_seconds_sum counter",
    ])
    for (method, path), value in sorted(DURATION_SUM.items()):
        labels = f'method="{method}",path="{path}"'
        lines.append(f"agent_http_request_duration_seconds_sum{{{labels}}} {value:.6f}")
        lines.append(f"agent_http_request_duration_seconds_count{{{labels}}} {DURATION_COUNT[(method, path)]}")
    return PlainTextResponse("\n".join(lines) + "\n", media_type="text/plain; version=0.0.4")
