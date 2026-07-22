from __future__ import annotations

from decimal import Decimal

from pydantic import BaseModel, Field, model_validator


def _clamp_limit(data: object, *, default: int = 10, maximum: int = 20) -> object:
    if not isinstance(data, dict) or "limit" not in data:
        return data
    try:
        limit = int(data.get("limit") or default)
    except (TypeError, ValueError):
        return data
    return {**data, "limit": max(1, min(limit, maximum))}


class AdminOrderSearchInput(BaseModel):
    number: str | None = Field(default=None, max_length=64)
    status: int | None = Field(default=None, ge=1, le=6)
    begin: str | None = Field(default=None, max_length=32)
    end: str | None = Field(default=None, max_length=32)
    limit: int = Field(default=10, ge=1, le=20)

    @model_validator(mode="before")
    @classmethod
    def normalize_limit(cls, data: object) -> object:
        return _clamp_limit(data)

    @model_validator(mode="after")
    def require_bounded_filter(self) -> "AdminOrderSearchInput":
        if not any((self.number, self.status, self.begin, self.end)):
            raise ValueError("at least one order filter is required")
        return self


class AdminOrderDetailInput(BaseModel):
    order_id: int = Field(gt=0)


class AdminCatalogSearchInput(BaseModel):
    name: str | None = Field(default=None, max_length=100)
    query: str | None = Field(default=None, max_length=100)
    status: int | None = Field(default=None, ge=0, le=1)
    category_id: int | None = Field(default=None, gt=0)
    page: int = Field(default=1, ge=1, le=1000)
    limit: int = Field(default=10, ge=1, le=20)

    @model_validator(mode="before")
    @classmethod
    def normalize_limit(cls, data: object) -> object:
        return _clamp_limit(data)

    @model_validator(mode="after")
    def use_query_as_name_alias(self) -> "AdminCatalogSearchInput":
        if self.name is None and self.query is not None:
            self.name = self.query
        return self


class AdminCategorySearchInput(BaseModel):
    name: str | None = Field(default=None, max_length=100)
    query: str | None = Field(default=None, max_length=100)
    type: int | None = Field(default=1, ge=1, le=2)
    status: int | None = Field(default=1, ge=0, le=1)
    page: int = Field(default=1, ge=1, le=1000)
    limit: int = Field(default=20, ge=1, le=20)

    @model_validator(mode="before")
    @classmethod
    def normalize_limit(cls, data: object) -> object:
        return _clamp_limit(data, default=20)

    @model_validator(mode="after")
    def use_query_as_name_alias(self) -> "AdminCategorySearchInput":
        if self.name is None and self.query is not None:
            self.name = self.query
        return self


class AdminCouponSearchInput(AdminCatalogSearchInput):
    pass


class AdminReviewSearchInput(BaseModel):
    keyword: str | None = Field(default=None, max_length=100)
    status: int | None = Field(default=None, ge=0, le=1)
    limit: int = Field(default=10, ge=1, le=20)

    @model_validator(mode="before")
    @classmethod
    def normalize_limit(cls, data: object) -> object:
        return _clamp_limit(data)


class AdminOrderSummary(BaseModel):
    id: int
    number: str
    status: int
    amount: Decimal | None = None
    order_time: str | None = None
    items_summary: str | None = None
    source: str = "spring_internal_api"
    scope: str | None = None
    generated_at: str | None = None


class AdminOrderSearchResult(BaseModel):
    items: list[AdminOrderSummary] = Field(default_factory=list)
    total: int = Field(ge=0)
    generated_at: str
    scope: str
    source: str


class AdminCatalogItem(BaseModel):
    id: int
    name: str
    category_id: int | None = None
    category_name: str | None = None
    price: Decimal | None = None
    status: int
    description: str | None = None
    updated_at: str | None = None


class AdminCatalogSearchResult(BaseModel):
    items: list[AdminCatalogItem] = Field(default_factory=list)
    total: int = Field(ge=0)
    generated_at: str
    scope: str
    source: str


class AdminCategoryItem(BaseModel):
    id: int
    name: str
    type: int
    status: int
    sort: int | None = None


class AdminCategorySearchResult(BaseModel):
    items: list[AdminCategoryItem] = Field(default_factory=list)
    total: int = Field(ge=0)
    generated_at: str
    scope: str
    source: str


class AdminCouponItem(BaseModel):
    id: int
    name: str
    discount_amount: Decimal
    minimum_amount: Decimal
    remaining_count: int = Field(ge=0)
    valid_from: str | None = None
    valid_until: str | None = None
    status: int


class AdminCouponSearchResult(BaseModel):
    items: list[AdminCouponItem] = Field(default_factory=list)
    total: int = Field(ge=0)
    generated_at: str
    scope: str
    source: str


class AdminReviewItem(BaseModel):
    id: int
    order_number: str | None = None
    dish_name: str | None = None
    user_name_masked: str
    rating: int = Field(ge=1, le=5)
    content_masked: str
    status: int
    created_at: str | None = None


class AdminReviewSearchResult(BaseModel):
    items: list[AdminReviewItem] = Field(default_factory=list)
    total: int = Field(ge=0)
    generated_at: str
    scope: str
    source: str
