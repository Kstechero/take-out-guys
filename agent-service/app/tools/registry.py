from __future__ import annotations

from dataclasses import dataclass
from typing import Any

from app.schemas.chat import ActorContext
from app.tools.addresses import AddressesTool
from app.tools.admin_queries import AdminQueryTools
from app.tools.admin_mutations import AdminMutationTools
from app.tools.business_overview import AdminBusinessOverviewTool
from app.tools.cart import CartTool
from app.tools.coupons import AvailableCouponsTool
from app.tools.menu_search import MenuSearchTool
from app.tools.knowledge_search import KnowledgeSearchTool
from app.tools.order_detail import OrderDetailTool
from app.tools.recent_orders import RecentOrdersTool
from app.tools.review_draft import ReviewDraftTool
from app.tools.sensitive_words import SensitiveWordsTool
from app.tools.shop_status import ShopStatusTool
from app.tools.user_mutations import UserMutationTools


@dataclass(slots=True)
class UserToolRegistry:
    menu_search: MenuSearchTool
    shop_status: ShopStatusTool
    recent_orders: RecentOrdersTool
    order_detail: OrderDetailTool
    cart: CartTool
    addresses: AddressesTool
    coupons: AvailableCouponsTool
    sensitive_words: SensitiveWordsTool
    review_draft: ReviewDraftTool | None = None
    knowledge: KnowledgeSearchTool | None = None
    mutations: UserMutationTools | None = None

    def langchain_tools(
        self, *, request_id: str, actor: ActorContext, session_id: str | None = None
    ) -> list[Any]:
        tools = [
            self.menu_search.as_langchain_tool(request_id=request_id, actor=actor),
            self.shop_status.as_langchain_tool(request_id=request_id, actor=actor),
            self.sensitive_words.as_langchain_tool(request_id=request_id, actor=actor),
        ]
        if self.knowledge is not None:
            tools.append(self.knowledge.as_langchain_tool(actor=actor))
        if actor.type == "user":
            tools.extend(
                [
                    self.recent_orders.as_langchain_tool(request_id=request_id, actor=actor),
                    self.order_detail.as_langchain_tool(request_id=request_id, actor=actor),
                    self.cart.as_langchain_tool(request_id=request_id, actor=actor),
                    self.addresses.as_langchain_tool(request_id=request_id, actor=actor),
                    self.coupons.as_langchain_tool(request_id=request_id, actor=actor),
                ]
            )
            if self.review_draft is not None:
                tools.append(self.review_draft.as_langchain_tool(request_id=request_id, actor=actor))
            if self.mutations is not None and session_id is not None:
                tools.extend(self.mutations.as_langchain_tools(actor=actor, session_id=session_id))
        return [tool for tool in tools if tool is not None]


@dataclass(slots=True)
class AdminToolRegistry:
    business_overview: AdminBusinessOverviewTool
    queries: AdminQueryTools
    menu_search: MenuSearchTool
    shop_status: ShopStatusTool
    sensitive_words: SensitiveWordsTool
    knowledge: KnowledgeSearchTool | None = None
    mutations: AdminMutationTools | None = None

    def langchain_tools(
        self, *, request_id: str, actor: ActorContext, session_id: str | None = None
    ) -> list[Any]:
        if actor.type != "admin":
            return []
        tools = [
            self.business_overview.as_langchain_tool(request_id=request_id, actor=actor),
            self.shop_status.as_langchain_tool(request_id=request_id, actor=actor),
            self.sensitive_words.as_langchain_tool(request_id=request_id, actor=actor),
        ]
        tools.extend(self.queries.as_langchain_tools(request_id=request_id, actor=actor))
        if self.mutations is not None and session_id is not None:
            tools.extend(
                self.mutations.as_langchain_tools(
                    request_id=request_id, actor=actor, session_id=session_id
                )
            )
        if self.knowledge is not None:
            tools.append(self.knowledge.as_langchain_tool(actor=actor))
        return [tool for tool in tools if tool is not None]
