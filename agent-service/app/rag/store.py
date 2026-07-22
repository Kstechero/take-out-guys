from __future__ import annotations

import hashlib
import json
import math
import re
from dataclasses import asdict
from datetime import datetime, timezone
from pathlib import Path

from app.rag.embeddings import LocalHashEmbeddings
from app.rag.models import IndexStats, KnowledgeChunk, KnowledgeMatch


class JsonVectorStore:
    version = 1

    def __init__(self, index_path: Path, embeddings: LocalHashEmbeddings) -> None:
        self.index_path = index_path
        self.embeddings = embeddings
        self._records: list[dict[str, object]] = []
        self._knowledge_hash = ""
        self.load()

    @property
    def knowledge_hash(self) -> str:
        return self._knowledge_hash

    def load(self) -> None:
        if not self.index_path.is_file():
            return
        payload = json.loads(self.index_path.read_text(encoding="utf-8"))
        if payload.get("version") != self.version:
            return
        if payload.get("embedding_provider") != self.embeddings.provider:
            return
        if payload.get("dimensions") != self.embeddings.dimensions:
            return
        records = payload.get("records")
        if isinstance(records, list):
            self._records = [record for record in records if isinstance(record, dict)]
            self._knowledge_hash = str(payload.get("knowledge_hash", ""))

    def sync(self, chunks: list[KnowledgeChunk], *, documents: int) -> IndexStats:
        existing = {str(record.get("id")): record for record in self._records}
        records: list[dict[str, object]] = []
        embedded = 0
        reused = 0
        for chunk in chunks:
            old = existing.get(chunk.id)
            if old is not None and isinstance(old.get("vector"), list):
                vector = old["vector"]
                reused += 1
            else:
                vector = self.embeddings.embed_query(chunk.content)
                embedded += 1
            record = asdict(chunk)
            record["vector"] = vector
            records.append(record)

        current_ids = {chunk.id for chunk in chunks}
        deleted = sum(1 for record_id in existing if record_id not in current_ids)
        knowledge_hash = hashlib.sha256(
            "\n".join(sorted(chunk.content_hash for chunk in chunks)).encode("utf-8")
        ).hexdigest()
        self._records = records
        self._knowledge_hash = knowledge_hash
        self._persist()
        return IndexStats(
            documents=documents,
            chunks=len(chunks),
            embedded=embedded,
            reused=reused,
            deleted=deleted,
            knowledge_hash=knowledge_hash,
        )

    def search(
        self,
        query: str,
        *,
        actor_type: str,
        domain: str | None,
        top_k: int,
        min_score: float,
    ) -> list[KnowledgeMatch]:
        allowed = {"public", actor_type}
        query_vector = self.embeddings.embed_query(query)
        matches: list[KnowledgeMatch] = []
        for record in self._records:
            if record.get("visibility") not in allowed:
                continue
            if domain and record.get("domain") != domain:
                continue
            vector = record.get("vector")
            if not isinstance(vector, list):
                continue
            content = str(record.get("content", ""))
            score = max(
                self._cosine(query_vector, vector),
                self._lexical_support(query, content) * 0.55,
            )
            if score < min_score:
                continue
            chunk_fields = {key: record[key] for key in KnowledgeChunk.__dataclass_fields__}
            matches.append(KnowledgeMatch(chunk=KnowledgeChunk(**chunk_fields), score=score))
        matches.sort(key=lambda match: match.score, reverse=True)
        return matches[:top_k]

    def _persist(self) -> None:
        self.index_path.parent.mkdir(parents=True, exist_ok=True)
        payload = {
            "version": self.version,
            "embedding_provider": self.embeddings.provider,
            "dimensions": self.embeddings.dimensions,
            "knowledge_hash": self._knowledge_hash,
            "generated_at": datetime.now(timezone.utc).isoformat(),
            "records": self._records,
        }
        temporary = self.index_path.with_suffix(self.index_path.suffix + ".tmp")
        temporary.write_text(json.dumps(payload, ensure_ascii=False), encoding="utf-8")
        temporary.replace(self.index_path)

    def _cosine(self, left: list[float], right: list[object]) -> float:
        if len(left) != len(right):
            return 0.0
        dot = sum(a * float(b) for a, b in zip(left, right))
        left_norm = math.sqrt(sum(value * value for value in left))
        right_norm = math.sqrt(sum(float(value) ** 2 for value in right))
        return dot / (left_norm * right_norm) if left_norm and right_norm else 0.0

    def _lexical_support(self, query: str, content: str) -> float:
        normalized_query = self._normalize(query)
        normalized_content = self._normalize(content)
        bigrams = {
            normalized_query[index:index + 2]
            for index in range(max(len(normalized_query) - 1, 0))
        }
        if not bigrams:
            return 0.0
        matched = sum(1 for bigram in bigrams if bigram in normalized_content)
        return matched / len(bigrams)

    def _normalize(self, text: str) -> str:
        normalized = re.sub(r"\s+", "", text.lower())
        return normalized.replace("配送", "派送").replace("优惠卷", "优惠券")
