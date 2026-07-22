from typing import Literal

from pydantic import BaseModel, Field

class ActorContext(BaseModel):
    type: str = Field(pattern="^(user|admin)$")
    id: str
    roles: list[str] = Field(default_factory=list)
    expires_at: str | None = None


class ChatRequest(BaseModel):
    request_id: str = Field(min_length=1)
    actor: ActorContext
    message: str = Field(min_length=1, max_length=4000)
    session_id: str | None = None
    confirmed_action_token: str | None = None


class SourceCitation(BaseModel):
    title: str
    source: str
    updated_at: str | None = None


class ChatResponse(BaseModel):
    request_id: str
    session_id: str | None
    answer: str
    status: Literal["completed", "waiting_user", "failed", "unavailable"] = "completed"
    citations: list[SourceCitation] = Field(default_factory=list)
    suggested_actions: list[str] = Field(default_factory=list)
    confirmation: dict[str, object] | None = None
    trace_id: str | None = None
