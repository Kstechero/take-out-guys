from __future__ import annotations

from collections.abc import Mapping

from app.schemas.chat import ActorContext


def build_internal_headers(*, request_id: str, actor: ActorContext, auth_token: str) -> Mapping[str, str]:
    headers = {
        "X-Request-Id": request_id,
        "X-Actor-Type": actor.type,
        "X-Actor-Id": actor.id,
        "X-Actor-Roles": ",".join(actor.roles),
    }
    if actor.expires_at:
        headers["X-Actor-Expires-At"] = actor.expires_at
    if auth_token:
        headers["X-Agent-Service-Token"] = auth_token
    return headers
