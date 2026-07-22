# Takeout Guys Agent Service

This repository contains the P0–P4 implementation of the standalone Python
Agent service and its Spring Boot, admin web, and user app adapters.

## Current Scope

- Run a FastAPI + LangChain + LangGraph Agent Service with persistent checkpoints.
- Keep Spring Boot as the business data and write-operation boundary.
- Do not connect this service directly to the takeout MySQL or Redis business
  stores.
- Provide user read/RAG tools, confirmed cart and coupon writes, admin read tools,
  and gray-controlled confirmed management writes with audit and idempotency.

## Project Structure

```text
takeout-guys-agent/
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
├─ backend/                # 权威 Spring Boot 业务边界与 Agent 适配器
├─ backend-legacy/         # 只读历史迁移基线，不再作为运行或新增业务目录
├─ admin-web/              # admin Agent confirmation UI
├─ user-app/               # user Agent confirmation UI
└─ docs/agent-service/     # product, architecture, contracts, tests, ADRs
```

## Local Development

Use the system Python directly. Python 3.11 or newer is required.

```powershell
cd agent-service
.\start.ps1
```

On Windows, you can also use:

```powershell
cd agent-service
.\start.bat
```

The launcher starts `agent-service` at `http://127.0.0.1:8000` by default.
Override the address only when needed:

```powershell
cd agent-service
.\start.ps1 -HostName 0.0.0.0 -Port 8001
```

Install dependencies before the first run, or after dependencies change:

```powershell
cd agent-service
python -m pip install -e ".[dev]"
```

Health check:

```powershell
Invoke-RestMethod http://127.0.0.1:8000/health
```

Tests:

```powershell
cd agent-service
python -m pytest
```

## Required Reading

Start with:

- `docs/agent-service/LANGCHAIN_RAG_AGENT_MICROSERVICE_PLAN.md`
- `docs/agent-service/AGENTS.md`
- `docs/agent-service/02-internal-api-contract.md`
- `docs/agent-service/03-tool-catalog.md`
- `docs/agent-service/06-test-cases.md`
