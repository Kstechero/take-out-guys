# Legacy Java Agent Baseline

This branch keeps the pre-microservice implementation where AI orchestration
is handled inside the Spring Boot backend.

## Contents

- `api/ADMIN_API_APIFOX.json`: Admin-side Apifox API export.
- `api/USER_API_APIFOX.json`: User-side Apifox API export.
- `SKY_TAKE_OUT_FULL_PROJECT_README.md`: Full project notes for the Java backend,
  admin web, and user app version.
- `PROJECT_DEVELOPMENT_LOG.md`: Development log for the existing application.
- `JAVA_AGENT_NOTES.md`: Notes for the current Java-side Agent behavior.

## Java Agent Entry Points

The existing AI baseline lives under `backend/sky-server/src/main/java/com/sky`:

- `service/ai/AiToolCallingClient.java`
- `service/ai/user/UserAiToolRegistry.java`
- `service/ai/user/UserAiToolExecutor.java`
- `service/ai/admin/AdminAiToolRegistry.java`
- `service/ai/admin/AdminAiToolExecutor.java`
- `service/impl/UserAiChatServiceImpl.java`
- `service/impl/AdminAiChatServiceImpl.java`

Use this branch as the rollback and comparison point while the new
`agent-service/` FastAPI + LangChain project is developed on `main`.
