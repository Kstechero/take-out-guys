from functools import lru_cache

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    app_name: str = "takeout-agent-service"
    app_env: str = "local"
    log_level: str = "INFO"
    spring_internal_base_url: str = "http://127.0.0.1:8080"
    spring_internal_auth_token: str = Field(default="", repr=False)
    spring_internal_timeout_seconds: float = 3.0
    agent_service_auth_token: str = Field(default="", repr=False)
    llm_base_url: str = "http://ai.tunayoshi.top:8000/v1"
    llm_api_key: str = Field(default="", repr=False)
    llm_model: str = "qwen36"
    llm_temperature: float = Field(default=0.2, ge=0, le=2)
    llm_timeout_seconds: float = Field(default=120.0, gt=0)
    llm_max_tokens: int = Field(default=2048, ge=128, le=8192)
    embedding_model: str = "text-embedding-3-small"
    rag_enabled: bool = True
    rag_auto_index: bool = True
    rag_knowledge_dir: str = "../docs/agent-service/knowledge"
    rag_index_path: str = "data/rag-index.json"
    rag_embedding_dimensions: int = Field(default=2048, ge=64, le=4096)
    rag_top_k: int = Field(default=4, ge=1, le=10)
    rag_min_score: float = Field(default=0.15, ge=0, le=1)
    agent_default_session_prefix: str = "user"
    confirmation_store_path: str = "data/confirmations.sqlite3"
    graph_checkpoint_path: str = "data/graph-checkpoints.sqlite3"
    graph_checkpoint_postgres_dsn: str = Field(default="", repr=False)
    confirmation_ttl_seconds: int = Field(default=300, ge=30, le=1800)
    rate_limit_requests_per_minute: int = Field(default=120, ge=10, le=10000)
    circuit_breaker_failure_threshold: int = Field(default=5, ge=1, le=50)
    circuit_breaker_reset_seconds: float = Field(default=30.0, ge=1, le=600)

    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")


@lru_cache
def get_settings() -> Settings:
    return Settings()


settings = get_settings()
