# Takeout Guys Agent Service

This repository is now the starting point for the standalone Python Agent
microservice planned in `docs/agent-service/`.

The legacy Spring Boot implementation, admin web, user app, old Markdown notes,
and Apifox JSON exports are archived on the `legacy/java-agent-backend` branch.

## Current Scope

- Build a FastAPI + LangChain + LangGraph Agent Service.
- Keep Spring Boot as the business data and write-operation boundary.
- Do not connect this service directly to the takeout MySQL or Redis business
  stores.
- Treat the current code as a scaffold. RAG, tools, and Java integration still
  need implementation.

## Project Structure

```text
sky-takeout-agent/
├─ agent-service/           # Python microservice scaffold
│  ├─ app/
│  │  ├─ api/               # FastAPI routes
│  │  ├─ clients/           # LLM, Spring Internal API, vector store clients
│  │  ├─ core/              # settings and shared app setup
│  │  ├─ graphs/            # LangGraph workflows
│  │  ├─ rag/               # ingestion, retrieval, citation formatting
│  │  ├─ schemas/           # Pydantic request/response/tool models
│  │  ├─ security/          # service auth, actor validation, redaction
│  │  └─ tools/             # LangChain tools wrapping Spring Internal API
│  ├─ tests/
│  ├─ evals/
│  ├─ scripts/
│  ├─ pyproject.toml
│  └─ Dockerfile
└─ docs/agent-service/      # product, architecture, contracts, tests, ADRs
```

## Local Development

```powershell
cd agent-service
python -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -e ".[dev]"
uvicorn app.main:app --reload
```

Health check:

```powershell
Invoke-RestMethod http://127.0.0.1:8000/health
```

Tests:

```powershell
cd agent-service
pytest
```

## Required Reading

Start with:

- `docs/agent-service/LANGCHAIN_RAG_AGENT_MICROSERVICE_PLAN.md`
- `docs/agent-service/AGENTS.md`
- `docs/agent-service/02-internal-api-contract.md`
- `docs/agent-service/03-tool-catalog.md`
- `docs/agent-service/06-test-cases.md`
