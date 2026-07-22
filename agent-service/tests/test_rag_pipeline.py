from pathlib import Path

from app.core.config import Settings
from app.graphs.user_support import build_user_support_graph
from app.rag.embeddings import LocalHashEmbeddings
from app.rag.loader import MarkdownKnowledgeLoader
from app.rag.retriever import KnowledgeRetriever, build_knowledge_retriever
from app.rag.store import JsonVectorStore
from app.schemas.chat import ActorContext, ChatRequest
from app.tools.knowledge_search import KnowledgeSearchTool, NO_EVIDENCE_MESSAGE


def write_document(
    path: Path,
    *,
    title: str,
    domain: str,
    visibility: str,
    content: str,
) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        "\n".join(
            [
                "---",
                f"title: {title}",
                "maintainer: test-team",
                "updated_at: 2026-07-13",
                f"domain: {domain}",
                f"visibility: {visibility}",
                "status: approved",
                "source_refs:",
                "  - test-source",
                "---",
                "",
                f"# {title}",
                "",
                content,
            ]
        ),
        encoding="utf-8",
    )


def actor(actor_type: str) -> ActorContext:
    return ActorContext(type=actor_type, id="1001", roles=[actor_type.upper()])


def test_loader_indexes_only_approved_documents_and_preserves_metadata(tmp_path: Path) -> None:
    knowledge_dir = tmp_path / "docs" / "knowledge"
    write_document(
        knowledge_dir / "user" / "coupon.md",
        title="优惠券规则",
        domain="coupon",
        visibility="user",
        content="优惠券达到订单门槛后才可以使用。",
    )
    (knowledge_dir / "README.md").write_text("not indexed", encoding="utf-8")
    loader = MarkdownKnowledgeLoader(knowledge_dir, tmp_path)

    documents = loader.load()
    chunks = loader.split(documents[0])

    assert len(documents) == 1
    assert chunks[0].source == "docs/knowledge/user/coupon.md"
    assert chunks[0].visibility == "user"
    assert chunks[0].content_hash == documents[0].content_hash


def test_loader_ingests_approved_pdf_xlsx_csv_and_json_sources() -> None:
    repository_root = Path(__file__).resolve().parents[2]
    knowledge_dir = repository_root / "docs" / "agent-service" / "knowledge"
    documents = MarkdownKnowledgeLoader(knowledge_dir, repository_root).load()
    structured = {Path(document.source).suffix for document in documents}

    assert {".pdf", ".xlsx", ".csv", ".json"} <= structured
    assert all("order-event-samples.json" not in document.source for document in documents)
    assert all(document.content.strip() for document in documents)


def test_vector_store_filters_visibility_before_scoring_and_removes_deleted_docs(
    tmp_path: Path,
) -> None:
    knowledge_dir = tmp_path / "knowledge"
    user_path = knowledge_dir / "user.md"
    admin_path = knowledge_dir / "admin.md"
    write_document(
        user_path,
        title="用户退款规则",
        domain="refund",
        visibility="user",
        content="用户待接单订单取消后可以申请退款。",
    )
    write_document(
        admin_path,
        title="管理拒单规范",
        domain="operations",
        visibility="admin",
        content="管理员只能拒绝待接单订单。",
    )
    loader = MarkdownKnowledgeLoader(knowledge_dir, tmp_path)
    store = JsonVectorStore(tmp_path / "index.json", LocalHashEmbeddings(256))
    retriever = KnowledgeRetriever(loader=loader, store=store, top_k=4, min_score=0.05)

    first = retriever.sync()
    user_matches = retriever.search("待接单退款", actor=actor("user"))
    admin_matches = retriever.search("拒绝待接单订单", actor=actor("admin"))

    assert first.embedded == 2
    assert all(match.chunk.visibility != "admin" for match in user_matches)
    assert all(match.chunk.visibility != "user" for match in admin_matches)
    assert admin_matches[0].chunk.title == "管理拒单规范"

    admin_path.unlink()
    second = retriever.sync()

    assert second.deleted == 1
    assert retriever.search("拒绝待接单订单", actor=actor("admin")) == []


async def test_knowledge_tool_returns_citations_and_rejects_without_evidence(
    tmp_path: Path,
) -> None:
    settings = Settings(
        rag_knowledge_dir=str(Path(__file__).resolve().parents[2] / "docs/agent-service/knowledge"),
        rag_index_path=str(tmp_path / "rag-index.json"),
        rag_auto_index=True,
    )
    tool = KnowledgeSearchTool(build_knowledge_retriever(settings))

    found = await tool.run(actor=actor("user"), query="优惠券可以叠加使用吗")
    missing = await tool.run(
        actor=actor("user"),
        query="支持无人机配送吗",
        domain="order",
    )

    assert found.found is True
    assert found.citations[0].source.endswith("knowledge/user/coupon-policy.md")
    assert missing.found is False
    assert missing.answer_guidance == NO_EVIDENCE_MESSAGE


def test_rag_eval_cases_meet_expected_retrieval_and_visibility(tmp_path: Path) -> None:
    import json

    settings = Settings(
        rag_knowledge_dir=str(Path(__file__).resolve().parents[2] / "docs/agent-service/knowledge"),
        rag_index_path=str(tmp_path / "rag-eval-index.json"),
        rag_auto_index=True,
    )
    retriever = build_knowledge_retriever(settings)
    eval_path = Path(__file__).resolve().parents[1] / "evals" / "rag_cases.jsonl"

    for line in eval_path.read_text(encoding="utf-8").splitlines():
        case = json.loads(line)
        matches = retriever.search(
            case["query"],
            actor=actor(case["actor"]),
            domain=case.get("domain"),
        )
        sources = {match.chunk.source for match in matches}
        if case["expected_source"] is None:
            assert not matches, case["id"]
        else:
            assert case["expected_source"] in sources, case["id"]


def test_graph_registers_actor_specific_knowledge_tools_and_degrades_safely(
    tmp_path: Path,
) -> None:
    knowledge_dir = Path(__file__).resolve().parents[2] / "docs/agent-service/knowledge"
    graph = build_user_support_graph(
        Settings(
            rag_knowledge_dir=str(knowledge_dir),
            rag_index_path=str(tmp_path / "working-index.json"),
        )
    )

    user_request = ChatRequest(
        request_id="req-user-rag",
        actor=actor("user"),
        message="配送范围是什么",
    )
    admin_request = ChatRequest(
        request_id="req-admin-rag",
        actor=actor("admin"),
        message="订单操作规范",
    )

    assert "search_service_knowledge" in [
        tool.name for tool in graph.langchain_tools_for_request(user_request)
    ]
    assert "search_operational_knowledge" in [
        tool.name for tool in graph.langchain_tools_for_request(admin_request)
    ]

    degraded = build_user_support_graph(
        Settings(
            rag_knowledge_dir=str(tmp_path / "missing-knowledge"),
            rag_index_path=str(tmp_path / "missing-index.json"),
        )
    )
    degraded_names = [
        tool.name for tool in degraded.langchain_tools_for_request(user_request)
    ]
    assert "get_shop_status" in degraded_names
    assert "search_service_knowledge" not in degraded_names
