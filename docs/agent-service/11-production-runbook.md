# Agent Service 生产运行与回滚手册

## 发布门槛

发布前必须执行 Python、`backend/`、管理端构建、uniapp SFC 编译和离线评测。任何越权、未确认写入、确认换人、幂等重放失败均阻断发布。生产图状态使用 PostgreSQL `AsyncPostgresSaver`，支持多个 Agent 副本共享 thread checkpoint；本地开发仍可使用 SQLite。确认 token 数据库当前放在共享持久卷，扩容时同一会话必须保持粘性路由。

## 启动与观测

使用 `docker compose -f docker-compose.agent.yml up -d --build`。检查 `/health`、`/metrics`、Spring `/doc.html` 和 Redis `PING`。Prometheus 默认位于 9090，告警至少覆盖 5xx 比例、429 比例、请求延迟、Agent 不可用响应和 Spring 熔断开启。

## 灰度与熔断

先只开放内部测试账号，再逐步开放用户和管理角色。Agent Service 对客户端按分钟限流；Spring Internal API 连续失败达到阈值后短时熔断。只读请求最多重试一次，写请求不重试。模型或 RAG 故障返回可理解降级，不得切回旧 Java Tool Calling。

## 回滚

1. 保留发布前镜像 tag、数据库版本和 `agent-state` 卷快照。
2. 停止新版本 Agent 流量，等待正在执行的确认写结束。
3. 将 compose 镜像 tag 改回上一个通过版本并重新启动；不要删除 Redis 幂等键。
4. 验证健康检查、只读查询、确认拒绝和一条脱敏端到端用例。
5. 若新版本写入过业务数据，按审计 `request_id` 使用对应业务补偿流程，禁止直接修改数据库。
