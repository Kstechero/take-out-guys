from __future__ import annotations

from typing import Any

import pytest

from app.confirmations import ConfirmationError, ConfirmationService, ConfirmationStore
from app.schemas.chat import ActorContext
from app.schemas.confirmation import ConfirmationEdit


class FakeWriteClient:
    def __init__(self) -> None:
        self.cart_calls: list[dict[str, Any]] = []
        self.coupon_calls: list[dict[str, Any]] = []

    async def change_cart(self, **kwargs: Any) -> dict[str, Any]:
        self.cart_calls.append(kwargs)
        return {"status": "APPLIED"}

    async def claim_coupon(self, **kwargs: Any) -> dict[str, Any]:
        self.coupon_calls.append(kwargs)
        return {"status": "CLAIMED"}


def actor(actor_id: str = "1001") -> ActorContext:
    return ActorContext(type="user", id=actor_id, roles=["USER"])


def service() -> tuple[ConfirmationService, FakeWriteClient]:
    client = FakeWriteClient()
    return (
        ConfirmationService(
            store=ConfirmationStore(":memory:"),
            client=client,  # type: ignore[arg-type]
            ttl_seconds=300,
        ),
        client,
    )


async def test_confirmation_executes_bound_arguments_only_once() -> None:
    confirmations, client = service()
    proposal = confirmations.propose_user_action(
        actor=actor(),
        session_id="session-1",
        action="add",
        arguments={"dish_id": 9, "quantity": 2, "expected_unit_amount": "18.00"},
        summary="确认加入 2 份菜品 9 吗？",
    )
    token = str(proposal["confirmation"]["token"])

    _record, result, replayed = await confirmations.execute_user_action(
        token, request_id="req-1", actor=actor(), session_id="session-1"
    )
    _record, replay_result, second_replayed = await confirmations.execute_user_action(
        token, request_id="req-2", actor=actor(), session_id="session-1"
    )

    assert result == replay_result == {"status": "APPLIED"}
    assert replayed is False
    assert second_replayed is True
    assert len(client.cart_calls) == 1
    assert client.cart_calls[0]["payload"] == {
        "action": "add",
        "dish_id": 9,
        "quantity": 2,
        "expected_unit_amount": "18.00",
    }


async def test_confirmation_rejects_different_actor_and_session() -> None:
    confirmations, client = service()
    proposal = confirmations.propose_user_action(
        actor=actor(),
        session_id="session-1",
        action="claim_coupon",
        arguments={"coupon_id": 7},
        summary="确认领取优惠券 7 吗？",
    )
    token = str(proposal["confirmation"]["token"])

    with pytest.raises(ConfirmationError, match="不匹配"):
        await confirmations.execute_user_action(
            token, request_id="req-1", actor=actor("1002"), session_id="session-1"
        )
    with pytest.raises(ConfirmationError, match="不匹配"):
        await confirmations.execute_user_action(
            token, request_id="req-2", actor=actor(), session_id="session-2"
        )

    assert client.coupon_calls == []


async def test_expired_confirmation_never_calls_write_api() -> None:
    client = FakeWriteClient()
    confirmations = ConfirmationService(
        store=ConfirmationStore(":memory:"),
        client=client,  # type: ignore[arg-type]
        ttl_seconds=-1,
    )
    proposal = confirmations.propose_user_action(
        actor=actor(),
        session_id="session-1",
        action="clear",
        arguments={},
        summary="确认清空购物车吗？",
    )
    token = str(proposal["confirmation"]["token"])

    with pytest.raises(ConfirmationError, match="过期"):
        await confirmations.execute_user_action(
            token, request_id="req-1", actor=actor(), session_id="session-1"
        )

    assert client.cart_calls == []


async def test_edit_replaces_token_and_binds_changed_arguments() -> None:
    confirmations, client = service()
    proposal = confirmations.propose_user_action(
        actor=actor(),
        session_id="session-1",
        action="add",
        arguments={"dish_id": 9, "quantity": 1, "expected_unit_amount": "18.00"},
        summary="确认加入 1 份菜品 9 吗？",
    )
    old_token = str(proposal["confirmation"]["token"])

    edited = confirmations.edit_action(
        old_token,
        agent_name="user_support_agent",
        actor=actor(),
        session_id="session-1",
        edited=ConfirmationEdit(quantity=3),
    )
    new_token = str(edited["confirmation"]["token"])

    with pytest.raises(ConfirmationError, match="不能再次执行"):
        await confirmations.execute_user_action(
            old_token, request_id="req-old", actor=actor(), session_id="session-1"
        )
    await confirmations.execute_user_action(
        new_token, request_id="req-new", actor=actor(), session_id="session-1"
    )

    assert new_token != old_token
    assert client.cart_calls[0]["payload"] == {
        "action": "add",
        "dish_id": 9,
        "quantity": 3,
        "expected_unit_amount": "18.00",
    }


async def test_rejected_confirmation_never_calls_write_api() -> None:
    confirmations, client = service()
    proposal = confirmations.propose_user_action(
        actor=actor(), session_id="session-1", action="clear", arguments={}, summary="确认清空吗？"
    )
    token = str(proposal["confirmation"]["token"])

    confirmations.store.reject(
        token, agent_name="user_support_agent", actor=actor(), session_id="session-1"
    )

    with pytest.raises(ConfirmationError, match="不能再次执行"):
        await confirmations.execute_user_action(
            token, request_id="req-rejected", actor=actor(), session_id="session-1"
        )
    assert client.cart_calls == []
