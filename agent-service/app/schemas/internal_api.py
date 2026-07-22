from typing import Any

from pydantic import BaseModel, Field


class InternalApiEnvelope(BaseModel):
    ok: bool
    data: Any = None
    error_code: str | None = None
    message: str = ""
    request_id: str = Field(min_length=1)
