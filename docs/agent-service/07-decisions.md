# Agent 服务架构决策记录（ADR）

## ADR-001：使用独立 Python Agent Service

- 状态：已决定（实施中）。
- 背景：项目现有 AI 是 Java 内嵌手写 Tool Calling 循环；希望以 LangChain 与 RAG 形成更清晰的 Agent 开发能力。
- 决策：使用 FastAPI 承载 LangChain/LangGraph，作为独立服务接入 Spring Boot。
- 后果：Python 生态下工作流、RAG 与评测更易扩展；新增跨服务契约、部署和可观测性成本。

## ADR-002：业务数据写入保留在 Java

- 状态：已决定（实施中）。
- 决策：Python 禁止直接写 MySQL；所有业务读写都经过 Spring Internal Agent API。
- 理由：复用现有权限、事务、缓存和 Service 规则，避免 Agent 越权或产生两套业务逻辑。
- 后果：需要维护 API 契约，但接口边界更清晰、安全性更高。

## ADR-003：RAG 使用“文档知识 + 实时 Tool”组合

- 状态：已决定（实施中）。
- 决策：规则、SOP、菜品说明进入向量库；订单、营业状态、优惠资格等实时或个人数据由 Tool 查询。
- 后果：避免知识库过期和隐私泄露；需要维护知识入库流程与引用展示。

## ADR-004：本地轻量索引，生产可替换向量后端

- 状态：待基础设施确认。
- 决策：当前开发阶段使用无需外部服务的本地哈希向量索引；生产根据规模和基础设施评估语义 embedding 与 pgvector 等向量后端，保持 Loader、Store、Retriever 接口不变。
- 待确认：生产 embedding/向量数据库选型、备份责任、容量和检索延迟目标。

## ADR-005：所有写 Tool 两阶段确认

- 状态：已决定（实施中）。
- 决策：Agent 首轮只能生成确认请求；确认 token 绑定用户、会话、操作摘要和过期时间；Java 执行前再次校验并幂等。
- 后果：对话多一轮，但可显著降低模型误执行风险并形成审计依据。

## ADR-006：用户侧直接使用 Agent API

- 状态：已决定（2026-07-13，替代原“保留 legacy 运行时回退”决策）。
- 决策：Spring 外部兼容路由直接调用对应 Agent API；用户进入 `user_support_agent`，管理端进入 `admin_operations_agent`，不实现 `agent.provider=legacy|python` 双路由。
- 后果：用户侧和管理侧都不再维护两套 Agent 编排逻辑；服务故障通过友好降级和部署版本回滚处理。尚未迁移的管理工具返回明确能力边界。
