from __future__ import annotations

from datetime import datetime
from decimal import Decimal
from enum import Enum
from typing import Literal

from pydantic import BaseModel, Field, model_validator

from app.schemas.chat import ActorContext


class ConfirmationDecision(str, Enum):
    approve = "approve"
    reject = "reject"
    edit = "edit"


class ConfirmationCard(BaseModel):
    token: str
    action: str
    summary: str
    expires_at: datetime
    details: dict[str, object] = Field(default_factory=dict)


class SetShopStatusInput(BaseModel):
    status: Literal["OPEN", "CLOSED"]
    audit_reason: str = Field(min_length=5, max_length=200)


class UpdateOrderActionInput(BaseModel):
    order_id: int = Field(gt=0)
    action: Literal["confirm", "deliver", "complete"]
    audit_reason: str = Field(min_length=5, max_length=200)


class ManageCouponInput(BaseModel):
    coupon_id: int = Field(gt=0)
    action: Literal["activate", "deactivate"]
    audit_reason: str = Field(min_length=5, max_length=200)


class DishFlavorInput(BaseModel):
    name: str = Field(min_length=1, max_length=50)
    value: str = Field(min_length=1, max_length=200)


class CreateAdminDishInput(BaseModel):
    name: str = Field(min_length=1, max_length=64)
    category_id: int = Field(gt=0)
    price: Decimal = Field(gt=0, max_digits=10, decimal_places=2)
    image: str | None = Field(default=None, max_length=500)
    description: str | None = Field(default=None, max_length=500)
    status: Literal[0, 1] = 1
    flavors: list[DishFlavorInput] = Field(default_factory=list, max_length=20)
    audit_reason: str = Field(min_length=5, max_length=200)


class UpdateAdminDishInput(BaseModel):
    dish_id: int = Field(gt=0)
    name: str | None = Field(default=None, min_length=1, max_length=64)
    category_id: int | None = Field(default=None, gt=0)
    price: Decimal | None = Field(default=None, gt=0, max_digits=10, decimal_places=2)
    image: str | None = Field(default=None, max_length=500)
    description: str | None = Field(default=None, max_length=500)
    status: Literal[0, 1] | None = None
    audit_reason: str = Field(min_length=5, max_length=200)

    @model_validator(mode="after")
    def require_change(self) -> "UpdateAdminDishInput":
        if not any(
            value is not None
            for value in (
                self.name,
                self.category_id,
                self.price,
                self.image,
                self.description,
                self.status,
            )
        ):
            raise ValueError("at least one dish field must be changed")
        return self


class CreateAdminCouponInput(BaseModel):
    name: str = Field(min_length=1, max_length=64)
    type: int = Field(ge=1, le=3)
    discount_amount: Decimal = Field(gt=0, max_digits=10, decimal_places=2)
    minimum_amount: Decimal = Field(ge=0, max_digits=10, decimal_places=2)
    total_count: int = Field(ge=1, le=1_000_000)
    per_user_limit: int = Field(ge=1, le=10_000)
    valid_from: datetime
    valid_until: datetime
    status: Literal[0, 1] = 0
    description: str | None = Field(default=None, max_length=255)
    audit_reason: str = Field(min_length=5, max_length=200)

    @model_validator(mode="after")
    def validate_coupon_rules(self) -> "CreateAdminCouponInput":
        if self.per_user_limit > self.total_count:
            raise ValueError("per_user_limit cannot exceed total_count")
        if self.minimum_amount > 0 and self.discount_amount > self.minimum_amount:
            raise ValueError("discount_amount cannot exceed minimum_amount")
        if self.valid_until <= self.valid_from:
            raise ValueError("valid_until must be after valid_from")
        return self


class ResumeRequest(BaseModel):
    request_id: str = Field(min_length=1)
    agent_name: Literal["user_support_agent", "admin_operations_agent"]
    actor: ActorContext
    confirmation_token: str = Field(min_length=20)
    decision: ConfirmationDecision
    edited_arguments: "ConfirmationEdit | None" = None

    @model_validator(mode="after")
    def require_edit_arguments(self) -> "ResumeRequest":
        if self.decision == ConfirmationDecision.edit and self.edited_arguments is None:
            raise ValueError("edited_arguments is required for edit")
        return self


class ConfirmationEdit(BaseModel):
    dish_id: int | None = Field(default=None, gt=0)
    setmeal_id: int | None = Field(default=None, gt=0)
    cart_item_id: int | None = Field(default=None, gt=0)
    flavor: str | None = Field(default=None, max_length=200)
    quantity: int | None = Field(default=None, ge=1, le=20)
    expected_unit_amount: Decimal | None = Field(
        default=None, gt=0, max_digits=10, decimal_places=2
    )
    coupon_id: int | None = Field(default=None, gt=0)
    order_id: int | None = Field(default=None, gt=0)
    action: Literal["confirm", "deliver", "complete", "activate", "deactivate"] | None = None
    status: Literal["OPEN", "CLOSED"] | None = None
    audit_reason: str | None = Field(default=None, min_length=5, max_length=200)


class AddToCartInput(BaseModel):
    dish_id: int | None = Field(default=None, gt=0)
    setmeal_id: int | None = Field(default=None, gt=0)
    flavor: str | None = Field(default=None, max_length=200)
    quantity: int = Field(default=1, ge=1, le=20)
    expected_unit_amount: Decimal = Field(gt=0, max_digits=10, decimal_places=2)

    @model_validator(mode="after")
    def exactly_one_product(self) -> "AddToCartInput":
        if (self.dish_id is None) == (self.setmeal_id is None):
            raise ValueError("exactly one of dish_id or setmeal_id is required")
        return self


class UpdateCartItemInput(BaseModel):
    cart_item_id: int = Field(gt=0)
    quantity: int = Field(ge=1, le=20)
    flavor: str | None = Field(default=None, max_length=200)


class RemoveCartItemInput(BaseModel):
    cart_item_id: int = Field(gt=0)


class ClearCartInput(BaseModel):
    pass


class ClaimCouponInput(BaseModel):
    coupon_id: int = Field(gt=0)


class CartChangeRequest(BaseModel):
    action: Literal["add", "update", "remove", "clear"]
    dish_id: int | None = Field(default=None, gt=0)
    setmeal_id: int | None = Field(default=None, gt=0)
    cart_item_id: int | None = Field(default=None, gt=0)
    flavor: str | None = Field(default=None, max_length=200)
    quantity: int | None = Field(default=None, ge=1, le=20)
    expected_unit_amount: Decimal | None = Field(
        default=None, gt=0, max_digits=10, decimal_places=2
    )
