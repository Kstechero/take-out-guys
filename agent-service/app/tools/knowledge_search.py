from __future__ import annotations

from dataclasses import dataclass

from langchain_core.tools import StructuredTool

from app.rag.retriever import KnowledgeRetriever
from app.schemas.chat import ActorContext
from app.schemas.knowledge import (
    KnowledgeCitation,
    KnowledgeSearchInput,
    KnowledgeSearchResult,
    KnowledgeSnippet,
)


NO_EVIDENCE_MESSAGE = "未在当前知识库找到确认依据。"


@dataclass(slots=True)
class KnowledgeSearchTool:
    retriever: KnowledgeRetriever

    async def run(
        self,
        *,
        actor: ActorContext,
        query: str,
        domain: str | None = None,
    ) -> KnowledgeSearchResult:
        validated = KnowledgeSearchInput(query=query, domain=domain)
        matches = self.retriever.search(
            validated.query,
            actor=actor,
            domain=validated.domain,
        )
        if not matches:
            return KnowledgeSearchResult(
                found=False,
                answer_guidance=NO_EVIDENCE_MESSAGE,
            )

        citations: list[KnowledgeCitation] = []
        seen_sources: set[str] = set()
        snippets: list[KnowledgeSnippet] = []
        for match in matches:
            snippets.append(
                KnowledgeSnippet(
                    content=match.chunk.content,
                    score=round(match.score, 4),
                    domain=match.chunk.domain,
                )
            )
            if match.chunk.source not in seen_sources:
                seen_sources.add(match.chunk.source)
                citations.append(
                    KnowledgeCitation(
                        title=match.chunk.title,
                        source=match.chunk.source,
                        updated_at=match.chunk.updated_at,
                    )
                )
        return KnowledgeSearchResult(
            found=True,
            snippets=snippets,
            citations=citations,
            answer_guidance="仅依据 snippets 回答，并在回答中保留 citations。",
        )

    def as_langchain_tool(self, *, actor: ActorContext) -> StructuredTool:
        async def _coroutine(query: str, domain: str | None = None) -> dict[str, object]:
            result = await self.run(actor=actor, query=query, domain=domain)
            return result.model_dump(mode="json")

        name = (
            "search_operational_knowledge"
            if actor.type == "admin"
            else "search_service_knowledge"
        )
        description = (
            "检索已审核的内部运营规范；只返回当前管理员可见内容和引用。"
            if actor.type == "admin"
            else "检索已审核的下单、配送、退款、优惠券、评价和客服规则；返回依据和引用。"
        )
        return StructuredTool.from_function(
            coroutine=_coroutine,
            name=name,
            description=description,
            args_schema=KnowledgeSearchInput,
        )
