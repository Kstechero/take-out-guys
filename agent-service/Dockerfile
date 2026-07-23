FROM python:3.11-slim-bookworm

ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1

WORKDIR /app

COPY agent-service/pyproject.toml ./
COPY agent-service/app ./app
COPY agent-service/scripts ./scripts
COPY docs/agent-service/knowledge /knowledge

RUN python -m pip install --no-cache-dir --no-build-isolation .

EXPOSE 8000

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
