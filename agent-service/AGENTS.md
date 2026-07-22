# Agent Service Development Rules

This file applies to `agent-service/` and all of its subdirectories.

## Boundaries

- This service uses Python, FastAPI, LangChain, and LangGraph.
- Do not connect directly to the takeout business MySQL, Redis, Mapper, or Java
  service internals.
- Business facts and write operations must go through Spring Boot Internal Agent
  APIs.
- All write tools must use confirmation semantics before any Java-side execution.
- Never commit `.env`, secrets, vector database files, model caches, personal
  data, or generated runtime artifacts.

## Layout

- Routes: `app/api/`
- Workflows: `app/graphs/`
- Tools: `app/tools/`
- External clients: `app/clients/`
- Pydantic models: `app/schemas/`
- Prompts: `app/prompts/`
- Security helpers: `app/security/`
- RAG: `app/rag/`
- Observability: `app/observability/`

## Commands

```powershell
python -m pip install -e ".[dev]"
python -m uvicorn app.main:app --reload
python -m pytest
```

## Before Implementing

Read the relevant files in `../docs/agent-service/`, especially:

- `LANGCHAIN_RAG_AGENT_MICROSERVICE_PLAN.md`
- `02-internal-api-contract.md`
- `03-tool-catalog.md`
- `04-rag-knowledge-sources.md`
- `05-prompt-policy.md`
- `06-test-cases.md`
