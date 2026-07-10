# Agent 微服务开发约束

本文件约束 `agent-service/` 及其全部子目录。

## 项目与分支

- 权威项目仓库：`https://github.com/Kstechero/take-out-guys`。
- Agent 微服务必须在独立功能分支开发，分支统一以 `codex/` 开头；禁止直接在 `main` 实现或合并未验证的 Agent 改动。
- 当前 Java AI 实现是迁移基线，不是废弃代码。保留 `legacy` 路径，直到 Python 路径完成灰度、评测和回退验证。
- 当前工作区若存在未提交改动，不得将它们混入 Agent 功能提交。应在干净 worktree 中新建 Agent 分支，或先将无关改动拆分提交后再开始。
- 推荐分支职责：
  - `codex/agent-contracts`：仅契约、工具目录、评测与 ADR；
  - `codex/java-internal-agent-api`：仅 `backend/` 内部 Agent API、服务认证、兼容适配器；
  - `codex/python-agent-service`：仅 `agent-service/` FastAPI、LangChain、LangGraph、RAG；
  - `codex/agent-integration`：Compose、灰度开关、端到端回归；
- 每个分支只解决一个可验收切片。跨服务接口先在 `docs/agent-service/02-internal-api-contract.md` 冻结，再并行实现。

## 现有 Java AI 基线

迁移或新增能力前，必须阅读并理解以下代码，不得无理由删除或绕过：

- `backend/sky-server/src/main/java/com/sky/service/ai/AiToolCallingClient.java`：现有 OpenAI 兼容 Tool Calling 循环；
- `backend/sky-server/src/main/java/com/sky/service/ai/user/UserAiToolRegistry.java` 与 `UserAiToolExecutor.java`：用户侧工具定义和业务分发；
- `backend/sky-server/src/main/java/com/sky/service/ai/admin/AdminAiToolRegistry.java` 与 `AdminAiToolExecutor.java`：管理侧工具定义和业务分发；
- `backend/sky-server/src/main/java/com/sky/service/ai/knowledge/OperationsKnowledgeService.java`：现有轻量关键词知识检索；
- `backend/sky-server/src/main/java/com/sky/service/impl/UserAiChatServiceImpl.java` 与 `AdminAiChatServiceImpl.java`：现有 AI 入口与 SSE 行为。

Python 服务的职责是替换“模型编排、RAG、工具选择”能力，不是复制业务逻辑。原 Executor 中对订单、购物车、地址、优惠券、评价和敏感词的真实业务调用，应逐步收敛为 Spring Boot Internal Agent API。

## 架构边界

- 本服务使用 Python、FastAPI、LangChain 和 LangGraph，保持无状态 HTTP 部署。
- 禁止直连外卖业务 MySQL、Redis、Mapper 或绕过 Spring Service；业务能力只能调用 Spring Boot Internal Agent API。
- 禁止将用户 JWT 原样交给模型或写入日志；仅使用短期、最小化 actor 上下文与服务认证。
- 所有写业务 Tool 必须执行“确认 token → Java 二次校验 → 幂等执行”；写请求不得自动重试。
- 实时业务事实必须通过 Tool 获取；RAG 只保存经审核的规则、SOP、菜品知识，禁止写入个人或交易数据。
- Agent Service 失败、模型不可用或向量库故障时，必须返回可理解的降级结果；Java 可按 feature flag 回退 `legacy`。

## 代码约定

- Python 版本、依赖与运行命令以 `pyproject.toml` 为唯一事实来源，依赖必须固定兼容版本。
- 路由放在 `app/api/`，工作流放在 `app/graphs/`，Tools 放在 `app/tools/`，外部客户端放在 `app/clients/`，Pydantic 模型放在 `app/schemas/`。
- Tool 输入和输出使用 Pydantic 严格模型；禁止将 `dict[str, Any]` 作为跨模块业务边界。
- 所有外部请求必须设置连接/读取超时并透传 `request_id`；只读请求最多重试一次，写请求不重试。
- 日志采用结构化字段与脱敏；禁止记录密码、token、完整手机号、详细地址、模型密钥或完整 prompt。
- 任何模型输出、Tool 参数、RAG 文档内容均不可信；权限、资源归属和写入前置条件必须通过代码校验。

## 文档与测试

- 实现前阅读 `docs/LANGCHAIN_RAG_AGENT_MICROSERVICE_PLAN.md` 和 `docs/agent-service/` 下的契约、工具、RAG、提示词、测试、ADR 文档。
- 修改跨服务接口时，先同步契约文档和测试，再改实现；不得靠口头约定字段。
- 每个 Tool 至少覆盖成功、空结果、权限不足、参数错误和超时。
- 新增写 Tool 必须覆盖：未确认、确认过期、换用户确认、参数变化、幂等重放和 Java 最终拒绝。
- RAG 修改必须运行命中、拒答和可见性评测；回答必须能返回来源。
- 交付前运行当前切片的最小测试，并汇报未运行的集成测试及原因。

## Git 与交付

- 不提交 `.env`、密钥、向量库数据、模型缓存、个人测试数据或构建产物。
- 不修改 `backend/`、`admin-web/`、`user-app/`，除非当前任务明确授权且接口契约已经确认。
- 不删除 `AiToolCallingClient`、Registry、Executor 或现有 `/user/ai/**`、`/admin/ai/**` 路由，除非迁移评测和回退方案已通过评审。
- 结束时汇报：修改文件、测试命令及结果、未验证项、风险、是否影响契约，以及对应分支名。
