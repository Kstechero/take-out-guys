from pydantic import BaseModel, Field


class KnowledgeSearchInput(BaseModel):
    query: str = Field(min_length=2, max_length=500)
    domain: str | None = Field(default=None, min_length=2, max_length=40)


class KnowledgeSnippet(BaseModel):
    content: str
    score: float = Field(ge=0, le=1)
    domain: str


class KnowledgeCitation(BaseModel):
    title: str
    source: str
    updated_at: str


class KnowledgeSearchResult(BaseModel):
    available: bool = True
    found: bool
    snippets: list[KnowledgeSnippet] = Field(default_factory=list)
    citations: list[KnowledgeCitation] = Field(default_factory=list)
    answer_guidance: str
