import json
from pathlib import Path

from app.core.config import Settings
from app.graphs.admin_operations import build_admin_operations_graph
from app.graphs.user_support import build_user_support_graph
from app.schemas.chat import ActorContext, ChatRequest


def request(actor_type: str) -> ChatRequest:
    return ChatRequest(
        request_id=f"quality-{actor_type}",
        actor=ActorContext(type=actor_type, id="1", roles=[actor_type.upper()]),
        session_id="quality-session",
        message="quality gate",
    )


def test_every_registered_tool_has_five_dimension_quality_evidence(tmp_path: Path) -> None:
    root = Path(__file__).resolve().parents[2]
    settings = Settings(
        rag_knowledge_dir=str(root / "docs/agent-service/knowledge"),
        rag_index_path=str(tmp_path / "quality-rag.json"),
        confirmation_store_path=str(tmp_path / "quality-confirmations.sqlite3"),
        graph_checkpoint_path=":memory:",
    )
    user = build_user_support_graph(settings)
    admin = build_admin_operations_graph(settings)
    actual = {
        tool.name for tool in user.langchain_tools_for_request(request("user"), "quality-session")
    } | {
        tool.name for tool in admin.langchain_tools_for_request(request("admin"), "quality-session")
    }
    matrix = json.loads(
        (root / "agent-service/evals/tool_test_matrix.json").read_text(encoding="utf-8")
    )

    assert actual == set(matrix["tools"])
    assert set(matrix["required_dimensions"]) == set(matrix["shared_evidence"])
    assert all(matrix["shared_evidence"][dimension] for dimension in matrix["required_dimensions"])
