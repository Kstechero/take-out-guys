# 使用 Codex 开发 Agent 微服务手册

## 开工前的最小上下文

每个 Codex task 开始时，都要求先阅读：

1. `docs/LANGCHAIN_RAG_AGENT_MICROSERVICE_PLAN.md`
2. 本目录中与任务相关的范围、架构、契约、工具、RAG、提示词与测试文档
3. `agent-service/AGENTS.md`
4. 如涉及 Java 内部 API，再阅读对应 Controller、Service、DTO 和现有 Tool Executor

任务提示必须包含：目标、允许修改目录、禁止修改目录、依赖契约、验收标准、测试命令和明确的“不做什么”。不要只输入“做一个 Agent”或“帮我接 RAG”。

## 推荐任务切片

按下列小切片依次开发，每一项单独提交和验证：

1. Python 工程骨架、配置和 `/health`；
2. Java Internal API 服务认证与一个只读 `menu_search`；
3. Python `SpringInternalClient` 与 `menu_search` Tool；
4. 文档入库、Chroma、检索和引用输出；
5. 用户侧只读 LangGraph 与 SSE；
6. 确认状态机与一个 `change_cart` 写工具；
7. 管理端只读运营图；
8. Eval、Docker Compose、灰度与回退。

## 多 Agent 协作

一个任务一个分支/worktree，例如 `codex/java-internal-api`、`codex/python-menu-tool`。在契约冻结前，不并行修改同一份接口文档；冻结后可按目录并行。

| Agent | 可修改范围 | 交付检查 |
| --- | --- | --- |
| 架构 Agent | `docs/agent-service/` | 契约、ADR、工具目录一致 |
| Java Agent | `backend/` | 鉴权、业务校验、MVC/Service 测试 |
| Python Agent | `agent-service/app/` | schema、Tool、Graph、pytest |
| RAG Agent | `agent-service/rag/`、`knowledge/` | 入库、检索、来源、eval |
| 测试 Agent | `agent-service/tests/`、`evals/` | Mock/集成/安全测试报告 |
| 集成 Agent | 少量跨模块文件 | Compose、回归、冲突处理 |

每个 Agent 完成后必须汇报：修改文件、已执行命令及结果、未执行的验证、风险、是否改变契约。集成 Agent 只在前置分支合并后处理跨服务问题，不重写其他 Agent 已验收的模块。

## 可复用提示词模板

```text
你在 F:\\sky-takeout-agent 工作。
先阅读 docs/LANGCHAIN_RAG_AGENT_MICROSERVICE_PLAN.md、
docs/agent-service/02-internal-api-contract.md、
docs/agent-service/03-tool-catalog.md、agent-service/AGENTS.md。

任务：在 agent-service 中实现 [一个明确能力]。
允许修改：[目录]。禁止修改：[目录]。
契约：[路由、请求/响应、错误码]。
安全约束：[actor、服务认证、禁止直连 DB、确认语义]。
验收： [可观察的行为与测试用例]。
先给出精简实现计划；实现后执行 [测试命令]。
结束时列出修改文件、测试结果、未解决风险；不要提交代码。
```

## 审查清单

- 是否越过 Java 直接访问业务数据库？
- 是否把 actor、订单 id、确认 token 当作可信输入？
- 是否让模型在没有确认的情况下完成了写操作？
- 是否 RAG 回答带来源且按 visibility 过滤？
- 是否遇到超时、模型错误时会虚构答案？
- 是否新增密钥、个人数据或构建产物进入 Git？
- 是否同步更新契约、工具目录和测试？
