from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class KnowledgeDocument:
    source: str
    title: str
    domain: str
    visibility: str
    updated_at: str
    content_hash: str
    content: str


@dataclass(frozen=True, slots=True)
class KnowledgeChunk:
    id: str
    source: str
    title: str
    domain: str
    visibility: str
    updated_at: str
    content_hash: str
    chunk_index: int
    content: str


@dataclass(frozen=True, slots=True)
class KnowledgeMatch:
    chunk: KnowledgeChunk
    score: float


@dataclass(frozen=True, slots=True)
class IndexStats:
    documents: int
    chunks: int
    embedded: int
    reused: int
    deleted: int
    knowledge_hash: str
