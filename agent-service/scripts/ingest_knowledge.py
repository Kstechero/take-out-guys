from __future__ import annotations

import json
from dataclasses import asdict

from app.core.config import get_settings
from app.rag.retriever import build_knowledge_retriever


def main() -> None:
    settings = get_settings()
    retriever = build_knowledge_retriever(settings.model_copy(update={"rag_auto_index": False}))
    stats = retriever.sync()
    print(json.dumps(asdict(stats), ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
