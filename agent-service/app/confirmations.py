from __future__ import annotations

import hashlib
import json
import secrets
import sqlite3
from dataclasses import dataclass
from datetime import UTC, datetime, timedelta
from pathlib import Path
from threading import Lock
from typing import Any

from app.clients.spring_internal import SpringInternalApiClient
from app.schemas.chat import ActorContext
from app.schemas.confirmation import (
    AddToCartInput,
    ClaimCouponInput,
    ConfirmationCard,
    ConfirmationEdit,
    CreateAdminCategoryInput,
    CreateAdminDishInput,
    ManageCouponInput,
    SetShopStatusInput,
    UpdateCartItemInput,
    UpdateOrderActionInput,
)


class ConfirmationError(RuntimeError):
    def __init__(self, code: str, message: str) -> None:
        super().__init__(message)
        self.code = code


@dataclass(frozen=True, slots=True)
class ConfirmationRecord:
    token_hash: str
    agent_name: str
    actor_type: str
    actor_id: str
    session_id: str
    action: str
    arguments: dict[str, Any]
    summary: str
    expires_at: datetime
    status: str
    result: dict[str, Any] | None


class ConfirmationStore:
    def __init__(self, path: str) -> None:
        self.path = path
        self._lock = Lock()
        if path != ":memory:":
            Path(path).parent.mkdir(parents=True, exist_ok=True)
        self._connection = sqlite3.connect(path, check_same_thread=False)
        self._connection.row_factory = sqlite3.Row
        self._connection.execute(
            """
            CREATE TABLE IF NOT EXISTS confirmations (
                token_hash TEXT PRIMARY KEY,
                agent_name TEXT NOT NULL,
                actor_type TEXT NOT NULL,
                actor_id TEXT NOT NULL,
                session_id TEXT NOT NULL,
                action TEXT NOT NULL,
                arguments_json TEXT NOT NULL,
                summary TEXT NOT NULL,
                expires_at TEXT NOT NULL,
                status TEXT NOT NULL,
                result_json TEXT
            )
            """
        )
        self._connection.commit()

    def create(
        self,
        *,
        agent_name: str,
        actor: ActorContext,
        session_id: str,
        action: str,
        arguments: dict[str, Any],
        summary: str,
        ttl_seconds: int,
    ) -> ConfirmationCard:
        token = secrets.token_urlsafe(32)
        expires_at = datetime.now(UTC) + timedelta(seconds=ttl_seconds)
        with self._lock:
            self._connection.execute(
                "INSERT INTO confirmations VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'pending', NULL)",
                (
                    self._hash(token),
                    agent_name,
                    actor.type,
                    actor.id,
                    session_id,
                    action,
                    json.dumps(arguments, ensure_ascii=False, sort_keys=True),
                    summary,
                    expires_at.isoformat(),
                ),
            )
            self._connection.commit()
        return ConfirmationCard(
            token=token,
            action=action,
            summary=summary,
            expires_at=expires_at,
        )

    def checkout(
        self,
        token: str,
        *,
        agent_name: str,
        actor: ActorContext,
        session_id: str,
    ) -> tuple[ConfirmationRecord, bool]:
        token_hash = self._hash(token)
        with self._lock:
            row = self._connection.execute(
                "SELECT * FROM confirmations WHERE token_hash = ?", (token_hash,)
            ).fetchone()
            if row is None:
                raise ConfirmationError("CONFIRMATION_INVALID", "确认信息无效，请重新发起操作")
            record = self._record(row)
            if (
                record.agent_name != agent_name
                or record.actor_type != actor.type
                or record.actor_id != actor.id
                or record.session_id != session_id
            ):
                raise ConfirmationError("CONFIRMATION_FORBIDDEN", "确认信息与当前用户或会话不匹配")
            if record.expires_at <= datetime.now(UTC):
                self._set_status(token_hash, "expired")
                raise ConfirmationError("CONFIRMATION_EXPIRED", "确认已过期，请重新发起操作")
            if record.status == "completed":
                return record, True
            if record.status != "pending":
                raise ConfirmationError("CONFIRMATION_CONSUMED", "该确认已处理，不能再次执行")
            updated = self._connection.execute(
                "UPDATE confirmations SET status = 'executing' "
                "WHERE token_hash = ? AND status = 'pending'",
                (token_hash,),
            )
            self._connection.commit()
            if updated.rowcount != 1:
                raise ConfirmationError("CONFIRMATION_CONSUMED", "该确认已处理，不能再次执行")
            return ConfirmationRecord(
                token_hash=record.token_hash,
                agent_name=record.agent_name,
                actor_type=record.actor_type,
                actor_id=record.actor_id,
                session_id=record.session_id,
                action=record.action,
                arguments=record.arguments,
                summary=record.summary,
                expires_at=record.expires_at,
                status="executing",
                result=record.result,
            ), False

    def complete(self, token_hash: str, result: dict[str, Any]) -> None:
        with self._lock:
            self._connection.execute(
                "UPDATE confirmations SET status = 'completed', result_json = ? WHERE token_hash = ?",
                (json.dumps(result, ensure_ascii=False, sort_keys=True), token_hash),
            )
            self._connection.commit()

    def fail(self, token_hash: str) -> None:
        with self._lock:
            self._set_status(token_hash, "failed")

    def reject(
        self, token: str, *, agent_name: str, actor: ActorContext, session_id: str
    ) -> ConfirmationRecord:
        record, replayed = self.checkout(
            token, agent_name=agent_name, actor=actor, session_id=session_id
        )
        if replayed:
            raise ConfirmationError("CONFIRMATION_CONSUMED", "该确认已经执行，无法撤销")
        with self._lock:
            self._set_status(record.token_hash, "rejected")
        return record

    def _set_status(self, token_hash: str, status: str) -> None:
        self._connection.execute(
            "UPDATE confirmations SET status = ? WHERE token_hash = ?", (status, token_hash)
        )
        self._connection.commit()

    def _record(self, row: sqlite3.Row) -> ConfirmationRecord:
        return ConfirmationRecord(
            token_hash=row["token_hash"],
            agent_name=row["agent_name"],
            actor_type=row["actor_type"],
            actor_id=row["actor_id"],
            session_id=row["session_id"],
            action=row["action"],
            arguments=json.loads(row["arguments_json"]),
            summary=row["summary"],
            expires_at=datetime.fromisoformat(row["expires_at"]),
            status=row["status"],
            result=json.loads(row["result_json"]) if row["result_json"] else None,
        )

    @staticmethod
    def _hash(token: str) -> str:
        return hashlib.sha256(token.encode("utf-8")).hexdigest()


@dataclass(slots=True)
class ConfirmationService:
    store: ConfirmationStore
    client: SpringInternalApiClient
    ttl_seconds: int = 300

    def propose_user_action(
        self,
        *,
        actor: ActorContext,
        session_id: str,
        action: str,
        arguments: dict[str, Any],
        summary: str,
    ) -> dict[str, Any]:
        card = self.store.create(
            agent_name="user_support_agent",
            actor=actor,
            session_id=session_id,
            action=action,
            arguments=arguments,
            summary=summary,
            ttl_seconds=self.ttl_seconds,
        )
        return {"ok": True, "confirmation": card.model_dump(mode="json")}

    def propose_admin_action(
        self,
        *,
        actor: ActorContext,
        session_id: str,
        action: str,
        arguments: dict[str, Any],
        summary: str,
        details: dict[str, Any],
    ) -> dict[str, Any]:
        card = self.store.create(
            agent_name="admin_operations_agent",
            actor=actor,
            session_id=session_id,
            action=action,
            arguments=arguments,
            summary=summary,
            ttl_seconds=self.ttl_seconds,
        )
        payload = card.model_dump(mode="json")
        target = (
            arguments.get("dish_id")
            or arguments.get("coupon_id")
            or arguments.get("order_id")
            or arguments.get("name")
            or "SHOP_STATUS"
        )
        payload["details"] = {
            "operator": actor.id,
            "actor_roles": actor.roles,
            "scope": "default_store",
            "resource": action,
            "target": target,
            "expected_version": arguments.get("expected_updated_at")
            or arguments.get("expected_status"),
            **details,
        }
        return {"ok": True, "confirmation": payload}

    def edit_action(
        self,
        token: str,
        *,
        agent_name: str,
        actor: ActorContext,
        session_id: str,
        edited: ConfirmationEdit,
    ) -> dict[str, Any]:
        record = self.store.reject(
            token, agent_name=agent_name, actor=actor, session_id=session_id
        )
        changes = edited.model_dump(exclude_none=True)
        merged = {**record.arguments, **changes}
        if agent_name == "user_support_agent":
            arguments, summary = self._edited_user_action(record.action, merged)
            return self.propose_user_action(
                actor=actor,
                session_id=session_id,
                action=record.action,
                arguments=arguments,
                summary=summary,
            )
        arguments, summary, details = self._edited_admin_action(record, merged)
        return self.propose_admin_action(
            actor=actor,
            session_id=session_id,
            action=record.action,
            arguments=arguments,
            summary=summary,
            details=details,
        )

    def _edited_user_action(
        self, action: str, merged: dict[str, Any]
    ) -> tuple[dict[str, Any], str]:
        if action == "add":
            args = AddToCartInput.model_validate(merged)
            product = f"菜品 {args.dish_id}" if args.dish_id else f"套餐 {args.setmeal_id}"
            return args.model_dump(mode="json", exclude_none=True), (
                f"确认将 {args.quantity} 份{product}加入购物车吗？"
            )
        if action == "update":
            args = UpdateCartItemInput.model_validate(merged)
            return args.model_dump(exclude_none=True), (
                f"确认将购物车项 {args.cart_item_id} 的数量改为 {args.quantity} 吗？"
            )
        if action == "claim_coupon":
            args = ClaimCouponInput.model_validate(merged)
            return args.model_dump(), f"确认领取优惠券 {args.coupon_id} 吗？"
        raise ConfirmationError("CONFIRMATION_NOT_EDITABLE", "该操作不支持修改，请取消后重新发起")

    def _edited_admin_action(
        self, record: ConfirmationRecord, merged: dict[str, Any]
    ) -> tuple[dict[str, Any], str, dict[str, Any]]:
        expected = record.arguments.get("expected_status")
        if record.action == "set_shop_status":
            args = SetShopStatusInput.model_validate(merged)
            arguments = {**args.model_dump(), "expected_status": expected}
            return arguments, f"确认将门店状态从 {expected} 改为 {args.status} 吗？", {
                "old_value": expected,
                "new_value": args.status,
                "impact_count": 1,
                "risk": "high",
                "audit_reason": args.audit_reason,
            }
        if record.action == "update_order":
            merged["order_id"] = record.arguments["order_id"]
            args = UpdateOrderActionInput.model_validate(merged)
            target = {"confirm": 3, "deliver": 4, "complete": 5}[args.action]
            arguments = {**args.model_dump(), "expected_status": expected}
            return arguments, f"确认将订单 {args.order_id} 从状态 {expected} 更新为 {target} 吗？", {
                "resource_id": args.order_id,
                "old_value": expected,
                "new_value": target,
                "impact_count": 1,
                "risk": "high",
                "audit_reason": args.audit_reason,
            }
        if record.action == "manage_coupon":
            merged["coupon_id"] = record.arguments["coupon_id"]
            args = ManageCouponInput.model_validate(merged)
            target = 1 if args.action == "activate" else 0
            arguments = {**args.model_dump(), "expected_status": expected}
            return arguments, f"确认将优惠券 {args.coupon_id} 从状态 {expected} 更新为 {target} 吗？", {
                "resource_id": args.coupon_id,
                "old_value": expected,
                "new_value": target,
                "impact_count": 1,
                "risk": "high",
                "audit_reason": args.audit_reason,
            }
        if record.action == "create_admin_dish":
            args = CreateAdminDishInput.model_validate(merged)
            arguments = args.model_dump(mode="json", exclude_none=True)
            return arguments, f"确认新增菜品“{args.name}”，价格 {args.price} 元吗？", {
                "resource": "dish",
                "new_value": {
                    "name": args.name,
                    "category_id": args.category_id,
                    "price": str(args.price),
                    "status": args.status,
                },
                "impact_count": 1,
                "risk": "high",
                "audit_reason": args.audit_reason,
            }
        if record.action == "create_admin_category":
            args = CreateAdminCategoryInput.model_validate(merged)
            arguments = args.model_dump(mode="json")
            category_type = "菜品分类" if args.type == 1 else "套餐分类"
            return arguments, f"确认新增{category_type}“{args.name}”，排序 {args.sort} 吗？", {
                "resource": "category",
                "new_value": {
                    "name": args.name,
                    "type": args.type,
                    "sort": args.sort,
                    "initial_status": 0,
                },
                "impact_count": 1,
                "risk": "high",
                "audit_reason": args.audit_reason,
            }
        raise ConfirmationError("CONFIRMATION_NOT_EDITABLE", "该管理操作不支持修改")


    async def execute_user_action(
        self,
        token: str,
        *,
        request_id: str,
        actor: ActorContext,
        session_id: str,
    ) -> tuple[ConfirmationRecord, dict[str, Any], bool]:
        record, replayed = self.store.checkout(
            token,
            agent_name="user_support_agent",
            actor=actor,
            session_id=session_id,
        )
        if replayed:
            return record, record.result or {}, True
        try:
            if record.action == "claim_coupon":
                result = await self.client.claim_coupon(
                    request_id=request_id,
                    actor=actor,
                    coupon_id=int(record.arguments["coupon_id"]),
                    confirmation_token=token,
                    idempotency_key=record.token_hash,
                )
            else:
                result = await self.client.change_cart(
                    request_id=request_id,
                    actor=actor,
                    payload={"action": record.action, **record.arguments},
                    confirmation_token=token,
                    idempotency_key=record.token_hash,
                )
        except Exception:
            self.store.fail(record.token_hash)
            raise
        self.store.complete(record.token_hash, result)
        return record, result, False

    async def execute_admin_action(
        self,
        token: str,
        *,
        request_id: str,
        actor: ActorContext,
        session_id: str,
    ) -> tuple[ConfirmationRecord, dict[str, Any], bool]:
        record, replayed = self.store.checkout(
            token,
            agent_name="admin_operations_agent",
            actor=actor,
            session_id=session_id,
        )
        if replayed:
            return record, record.result or {}, True
        common = {
            "request_id": request_id,
            "actor": actor,
            "payload": record.arguments,
            "confirmation_token": token,
            "idempotency_key": record.token_hash,
        }
        try:
            if record.action == "set_shop_status":
                result = await self.client.set_admin_shop_status(**common)
            elif record.action == "update_order":
                result = await self.client.update_admin_order(**common)
            elif record.action == "manage_coupon":
                result = await self.client.manage_admin_coupon(**common)
            elif record.action == "create_admin_dish":
                result = await self.client.create_admin_dish(**common)
            elif record.action == "update_admin_dish":
                result = await self.client.update_admin_dish(**common)
            elif record.action == "create_admin_category":
                result = await self.client.create_admin_category(**common)
            elif record.action == "create_admin_coupon":
                result = await self.client.create_admin_coupon(**common)
            else:
                raise ConfirmationError("CONFIRMATION_INVALID", "不支持的管理操作")
        except Exception:
            self.store.fail(record.token_hash)
            raise
        self.store.complete(record.token_hash, result)
        return record, result, False
