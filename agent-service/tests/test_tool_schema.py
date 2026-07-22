from langchain_openai import ChatOpenAI

from app.clients.spring_internal import SpringInternalApiClient
from app.core.config import Settings
from app.schemas.chat import ActorContext
from app.tools.addresses import AddressesTool
from app.tools.cart import CartTool
from app.tools.coupons import AvailableCouponsTool
from app.tools.menu_search import MenuSearchTool
from app.tools.order_detail import OrderDetailTool
from app.tools.recent_orders import RecentOrdersTool
from app.tools.registry import UserToolRegistry
from app.tools.sensitive_words import SensitiveWordsTool
from app.tools.shop_status import ShopStatusTool


def test_registered_tools_bind_to_openai_compatible_model() -> None:
    settings = Settings(llm_api_key="test-key")
    client = SpringInternalApiClient(settings=settings)
    registry = UserToolRegistry(
        menu_search=MenuSearchTool(client=client),
        shop_status=ShopStatusTool(client=client),
        recent_orders=RecentOrdersTool(client=client),
        order_detail=OrderDetailTool(client=client),
        cart=CartTool(client=client),
        addresses=AddressesTool(client=client),
        coupons=AvailableCouponsTool(client=client),
        sensitive_words=SensitiveWordsTool(client=client),
    )
    tools = registry.langchain_tools(
        request_id="req-schema",
        actor=ActorContext(type="user", id="1001", roles=["USER"]),
    )
    model = ChatOpenAI(
        model_name="test-model",
        openai_api_key="test-key",
        openai_api_base="http://127.0.0.1:1/v1",
    )

    bound = model.bind_tools(tools)

    schemas = bound.kwargs["tools"]
    assert [schema["function"]["name"] for schema in schemas] == [
        "menu_search",
        "get_shop_status",
        "check_sensitive_words",
        "list_recent_orders",
        "get_order_detail",
        "get_cart",
        "list_addresses",
        "list_available_coupons",
    ]
    assert schemas[0]["function"]["parameters"]["properties"]["limit"]["maximum"] == 10


def test_admin_actor_does_not_receive_user_private_tools() -> None:
    settings = Settings(llm_api_key="test-key")
    client = SpringInternalApiClient(settings=settings)
    registry = UserToolRegistry(
        menu_search=MenuSearchTool(client=client),
        shop_status=ShopStatusTool(client=client),
        recent_orders=RecentOrdersTool(client=client),
        order_detail=OrderDetailTool(client=client),
        cart=CartTool(client=client),
        addresses=AddressesTool(client=client),
        coupons=AvailableCouponsTool(client=client),
        sensitive_words=SensitiveWordsTool(client=client),
    )

    tools = registry.langchain_tools(
        request_id="req-admin-schema",
        actor=ActorContext(type="admin", id="2001", roles=["ADMIN"]),
    )

    assert [tool.name for tool in tools] == [
        "menu_search",
        "get_shop_status",
        "check_sensitive_words",
    ]
