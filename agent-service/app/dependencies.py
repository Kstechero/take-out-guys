from functools import lru_cache

from app.core.config import get_settings
from app.graphs.admin_operations import AdminOperationsAgentGraph, build_admin_operations_graph
from app.graphs.user_support import UserSupportAgentGraph, build_user_support_graph


@lru_cache
def get_user_support_graph() -> UserSupportAgentGraph:
    return build_user_support_graph(get_settings())


@lru_cache
def get_admin_operations_graph() -> AdminOperationsAgentGraph:
    return build_admin_operations_graph(get_settings())
