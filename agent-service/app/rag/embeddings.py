from __future__ import annotations

import hashlib
import math
import re


class LocalHashEmbeddings:
    """Deterministic character n-gram vectors for offline Chinese knowledge retrieval."""

    provider = "local-hash-v1"

    def __init__(self, dimensions: int = 384) -> None:
        self.dimensions = dimensions

    def embed_documents(self, texts: list[str]) -> list[list[float]]:
        return [self.embed_query(text) for text in texts]

    def embed_query(self, text: str) -> list[float]:
        normalized = re.sub(r"\s+", "", text.lower())
        normalized = normalized.replace("配送", "派送").replace("优惠卷", "优惠券")
        vector = [0.0] * self.dimensions
        features: list[str] = []
        for size, weight in ((1, 0.35), (2, 1.0), (3, 0.65)):
            features.extend(
                f"{weight}:{normalized[index:index + size]}"
                for index in range(max(len(normalized) - size + 1, 0))
            )
        features.extend(f"1.2:{word}" for word in re.findall(r"[a-z0-9]+", text.lower()))

        for feature in features:
            weight_text, value = feature.split(":", 1)
            digest = hashlib.blake2b(value.encode("utf-8"), digest_size=8).digest()
            bucket = int.from_bytes(digest[:4], "big") % self.dimensions
            sign = 1.0 if digest[4] & 1 else -1.0
            vector[bucket] += float(weight_text) * sign

        norm = math.sqrt(sum(value * value for value in vector))
        return [value / norm for value in vector] if norm else vector
