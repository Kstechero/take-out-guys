from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path

from app.core.config import Settings
from app.rag.embeddings import LocalHashEmbeddings
from app.rag.loader import MarkdownKnowledgeLoader
from app.rag.models import IndexStats, KnowledgeMatch
from app.rag.store import JsonVectorStore
from app.schemas.chat import ActorContext


@dataclass(slots=True)
class KnowledgeRetriever:
    loader: MarkdownKnowledgeLoader
    store: JsonVectorStore
    top_k: int
    min_score: float

    def sync(self) -> IndexStats:
        documents = self.loader.load()
        chunks = [chunk for document in documents for chunk in self.loader.split(document)]
        return self.store.sync(chunks, documents=len(documents))

    def search(
        self,
        query: str,
        *,
        actor: ActorContext,
        domain: str | None = None,
    ) -> list[KnowledgeMatch]:
        effective_domain = domain or self._infer_domain(query, actor.type)
        return self.store.search(
            query,
            actor_type=actor.type,
            domain=effective_domain,
            top_k=self.top_k,
            min_score=self.min_score,
        )

    def _infer_domain(self, query: str, actor_type: str) -> str | None:
        if actor_type == "admin":
            return "operations"
        if any(word in query for word in ("不付款", "支付超时", "催单", "配送", "派送", "订单状态")):
            return "order"
        if any(word in query for word in ("评价", "客服", "敏感词")):
            return "service"
        if any(word in query for word in ("退款", "取消", "拒单", "赔付")):
            return "refund"
        if any(word in query for word in ("优惠券", "领券", "券")):
            return "coupon"
        return None


def build_knowledge_retriever(settings: Settings) -> KnowledgeRetriever:
    service_root = Path(__file__).resolve().parents[2]
    repository_root = service_root.parent
    knowledge_dir = _resolve_path(settings.rag_knowledge_dir, service_root)
    index_path = _resolve_path(settings.rag_index_path, service_root)
    embeddings = LocalHashEmbeddings(settings.rag_embedding_dimensions)
    retriever = KnowledgeRetriever(
        loader=MarkdownKnowledgeLoader(knowledge_dir, repository_root),
        store=JsonVectorStore(index_path, embeddings),
        top_k=settings.rag_top_k,
        min_score=settings.rag_min_score,
    )
    if settings.rag_auto_index:
        retriever.sync()
    return retriever


def _resolve_path(configured: str, service_root: Path) -> Path:
    path = Path(configured)
    return path.resolve() if path.is_absolute() else (service_root / path).resolve()
