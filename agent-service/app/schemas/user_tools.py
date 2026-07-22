from decimal import Decimal

from pydantic import BaseModel, Field
from app.schemas.chat import ActorContext


class RecentOrdersInput(BaseModel):
    status: int | None = Field(default=None, ge=1, le=6)
    limit: int = Field(default=5, ge=1, le=10)


class OrderDetailInput(BaseModel):
    order_id: int = Field(gt=0)


class AddressListInput(BaseModel):
    default_only: bool = False


class AvailableCouponsInput(BaseModel):
    order_amount: Decimal | None = Field(default=None, ge=0)


class SensitiveWordsInput(BaseModel):
    text: str = Field(min_length=1, max_length=2000)


class OrderItem(BaseModel):
    dish_id: int | None = None
    setmeal_id: int | None = None
    name: str
    flavor: str | None = None
    quantity: int = Field(ge=0)
    unit_amount: Decimal | None = None


class OrderSummary(BaseModel):
    id: int
    number: str
    status: int
    status_label: str
    amount: Decimal | None = None
    order_time: str | None = None
    estimated_delivery_time: str | None = None
    items_summary: str | None = None


class RecentOrdersResult(BaseModel):
    items: list[OrderSummary] = Field(default_factory=list)
    total: int = Field(ge=0)


class OrderDetailResult(OrderSummary):
    pay_status: int | None = None
    original_amount: Decimal | None = None
    discount_amount: Decimal | None = None
    items: list[OrderItem] = Field(default_factory=list)


class CartItem(BaseModel):
    id: int
    dish_id: int | None = None
    setmeal_id: int | None = None
    name: str
    flavor: str | None = None
    quantity: int = Field(ge=0)
    unit_amount: Decimal = Decimal("0")
    image: str | None = None


class CartResult(BaseModel):
    items: list[CartItem] = Field(default_factory=list)
    total_quantity: int = Field(ge=0)
    total_amount: Decimal = Decimal("0")


class AddressSummary(BaseModel):
    id: int
    consignee_masked: str
    phone_masked: str
    region: str
    detail_masked: str
    label: str | None = None
    is_default: bool


class AddressListResult(BaseModel):
    items: list[AddressSummary] = Field(default_factory=list)


class CouponSummary(BaseModel):
    user_coupon_id: int | None = None
    coupon_id: int
    name: str
    discount_amount: Decimal
    minimum_amount: Decimal
    valid_until: str | None = None
    description: str | None = None


class AvailableCouponsResult(BaseModel):
    items: list[CouponSummary] = Field(default_factory=list)
    order_amount: Decimal | None = None


class SensitiveWordsResult(BaseModel):
    safe: bool
    masked_text: str


class ReviewDraftCheckResult(BaseModel):
    eligible: bool
    safe: bool
    order_id: int
    dish_id: int
    dish_name: str
    rating: int = Field(ge=1, le=5)
    highlights: str = ""
    instruction: str


class RecommendationRequest(BaseModel):
    request_id: str = Field(min_length=1)
    actor: ActorContext
    requirement: str = Field(min_length=1, max_length=500)
    budget: Decimal | None = Field(default=None, gt=0)
    people_count: int = Field(default=1, ge=1, le=20)
    limit: int = Field(default=5, ge=1, le=5)


class RecommendationItem(BaseModel):
    type: str
    id: int
    name: str
    price: Decimal | None = None
    image: str | None = None
    reason: str


class RecommendationResult(BaseModel):
    items: list[RecommendationItem] = Field(default_factory=list)
    summary: str


class ReviewDraftRequest(BaseModel):
    request_id: str = Field(min_length=1)
    actor: ActorContext
    order_id: int = Field(gt=0)
    dish_id: int = Field(gt=0)
    rating: int = Field(default=5, ge=1, le=5)
    highlights: str = Field(default="", max_length=500)
    style: str = Field(default="自然", max_length=50)


class ReviewDraftResult(BaseModel):
    content: str
    flagged: bool
    publish_status: str = "draft_only"


class AdminBusinessOverviewResult(BaseModel):
    begin: str
    end: str
    turnover: Decimal = Decimal("0")
    valid_order_count: int = Field(ge=0)
    order_completion_rate: float = Field(ge=0)
    unit_price: Decimal = Decimal("0")
    new_users: int = Field(ge=0)
    waiting_orders: int = Field(ge=0)
    delivered_orders: int = Field(ge=0)
    completed_orders: int = Field(ge=0)
    cancelled_orders: int = Field(ge=0)
    all_orders: int = Field(ge=0)
    generated_at: str
    scope: str
    source: str
