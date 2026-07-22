# Agent Service 开发日志

## 2026-07-22

### 本次完成
- 完成 P0–P4 交付审计：同步响应增加 `status`，SSE 保留图节点、工具、引用、确认、中断和最终状态事件。
- 用户图改为显式节点并使用 LangGraph 原生 `interrupt()`；SQLite checkpointer 和确认库支持进程重启后恢复，应用退出时关闭连接。
- 用户加购、购物车修改/删除/清空和领券接入批准、编辑、拒绝、过期、actor/session 绑定及一次性确认；加购绑定预览价格，Java 再校验在售状态和实时价格。
- 独立管理图接入订单、商品、套餐、优惠券、门店、评价、经营统计与运营知识查询，结果统一输出来源、单店范围和生成时间。
- 开放门店状态、订单流转、优惠券启停三个受控管理 Tool：默认关闭灰度开关，Java 二次鉴权、字段白名单、预期状态乐观校验、Redis/SQL 原子变更、24 小时幂等状态和结构化审计日志齐全。
- 补齐管理 Agent 新增菜品执行端点，并新增单菜品乐观修改和新增优惠券；三者均使用确认卡、灰度、幂等、Java 业务二次校验与结构化审计。管理端确认卡展示旧值/新值详情。
- Spring 用户端与管理端适配确认卡、恢复接口和状态字段；用户端支持修改数量，两个前端均支持批准/取消。
- Spring Internal API 客户端对只读请求增加一次传输层失败重试；写请求保持单次尝试，避免重复业务副作用。
- 新增读请求可重试、写请求不重试的回归测试。
- Agent Service 测试、静态检查与字节码编译全部通过。

### 验证
- `pytest -q`：49 项通过；`ruff check app tests scripts` 与 `compileall` 通过。
- `mvn.cmd -q clean test`：23 项通过，0 失败、0 错误。
- 管理端 `vue-tsc` 与 Vite production build 通过；用户端聊天 SFC 解析和脚本编译通过。

### P0–P4 剩余边界
- P5 已补充 Prometheus 指标、结构化请求 trace、按客户端限流、Spring Internal API 熔断、PostgreSQL 共享 checkpointer、生产 Compose 和回滚手册；模型 token 成本继续通过模型供应方 usage/账单汇总。
- 管理写仅开放已评审的单资源动作；删除、批量修改、员工权限、套餐和敏感词写入仍不注册。

本文件按日期记录 Agent 微服务迁移的实际开发内容、验证结果、未完成项和风险。新记录追加在顶部，不覆盖历史记录。当前目标方案以 `LANGCHAIN_RAG_AGENT_MICROSERVICE_PLAN.md` 为准，历史记录中的阶段名称、工具数量和测试数量不自动视为当前验收口径。

## 2026-07-13

### 已完成

- 建立 FastAPI 分层结构：API、Graph、Tools、Spring Internal API Client、Pydantic schema、prompt 和依赖注入。
- 实现用户侧 `POST /v1/user/chat`，完成营业状态与菜单搜索的首批只读编排。
- 实现 `get_shop_status`、`menu_search` 两个 LangChain 兼容 Tool；Python 不直连业务数据库。
- Spring Boot 用户对话入口改为直接调用 Agent API，移除 `UserAiChatServiceImpl` 对手写 `AiToolCallingClient`、Registry、Executor 和 Java RAG 编排的依赖。
- Spring Boot 管理端聊天、SSE 与健康检查改为调用 Agent API，移除 `AdminAiChatServiceImpl` 对手写 Tool Calling 和直连模型健康检查的依赖。
- 新增 `POST /v1/admin/chat`，强制 admin actor；管理端工具未迁移时返回明确能力边界。
- 保持原 `/user/ai/chat` 与 `/user/ai/chat/stream` 前端协议；SSE 当前由 Java 异步调用 Agent API 后转为兼容事件。
- 新增 Spring Boot `/internal/agent/shop/status` 与 `/internal/agent/menu/search`，返回统一 envelope，并限制菜单结果最多 10 项。
- Java 到 Python、Python 到 Java 均强制 `X-Agent-Service-Token` 服务认证；actor 上下文仅包含类型、ID、角色和短期过期时间，不转发用户 JWT。
- 更新 ADR：用户侧直接使用 Agent API，不维护 `legacy|python` 运行时双路由；故障采用友好降级与部署版本回滚。
- 接入 OpenAI-compatible `ornith` 模型，模型 URL、Key、模型名、温度、超时和最大 token 均由环境变量配置，API Key 未写入仓库。
- 将关键词规则路由替换为真实 LangGraph `model → tools → model` 循环，由模型选择 `get_shop_status` 或 `menu_search`。
- 使用 LangGraph `MemorySaver` 保存本进程内的多轮消息，并以 `actor_type + actor_id + session_id` 隔离会话。
- 新增用户端和管理端独立系统提示词、模型异常安全降级和脱敏结构化错误日志。
- 新增 Agent Service 流式接口；Java SSE 代理逐 token 转发 `delta`、`tool_status` 和 `done`，同时保留旧前端字段别名。

### 第二阶段：用户只读工具

- 新增最近订单、订单详情、购物车、脱敏地址、可用优惠券、敏感词检查 6 组 Spring Internal API 与 LangChain Tools。
- Python 侧使用严格 Pydantic 输入/输出模型，所有实时和用户私有数据只通过 Spring Internal API 获取，不直连业务数据库。
- Spring 鉴权拦截器在请求期间写入 `BaseContext`，并在请求结束后清理，复用现有订单归属和当前用户查询规则。
- 用户私有工具仅向 user actor 注册；admin actor 只能使用公共只读工具和敏感词检查。
- Internal API 返回最小必要字段：订单不包含手机号、完整地址和用户 ID；地址仅返回姓名、电话及门牌掩码；敏感词检查不返回词库命中列表。
- 新增 Internal API 专用异常映射，统一返回 `FORBIDDEN`、`NOT_FOUND`、`VALIDATION_ERROR` 或 `INTERNAL_ERROR` envelope。

### 第三阶段：RAG 知识库

- 建立 `docs/agent-service/knowledge/` 审核知识目录和文档清单。
- 从 legacy 后端实际业务校验中整理下单配送、取消退款、优惠券、评价客服及管理端订单 SOP 共 5 份可索引文档。
- 所有正文增加统一 YAML 元数据、代码事实来源、可见性和实时数据边界，未写入用户数据、交易明细或密钥。
- 明确配送范围、退款到账时间、赔付和活动叠加等暂无系统依据的场景必须拒绝猜测或转人工。
- 实现 approved Markdown front matter 校验、标题语义切分、内容 hash 和可重复增量入库；文档删除或可见性变更会清理旧向量。
- 实现 2048 维本地哈希向量索引、余弦检索、业务域预过滤、二元组轻量重排和 `0.15` 置信度阈值。
- 实现 user/public/admin 检索前可见性过滤，user 无法召回管理端 SOP，admin 不会召回用户知识。
- 注册 `search_service_knowledge` 与 `search_operational_knowledge` LangChain Tools，返回 snippets、score 和 citation。
- 非流式响应返回结构化 citations；SSE 新增 `citation` 事件；知识无依据时 Graph 强制返回统一拒答，阻止模型补全。
- 新增 20 条应命中与 10 条应拒答的离线 RAG 评测集，覆盖全部首批知识域和跨角色越权。

### 验证

- `pytest -q`：26 条测试通过，包含入库、增量复用、删除清理、可见性、30 条 RAG 评测、强制拒答和 SSE 引用事件。
- `ruff check app tests scripts`：通过。
- `python -m compileall -q app scripts`：通过。
- `python -m scripts.ingest_knowledge`：5 份文档生成 14 个 chunk；第二次执行复用全部 14 个向量。
- `mvn -pl sky-server -am test`：Reactor 构建通过，9 条 Java 测试通过。
- 真实模型基础调用：通过；营业状态和菜单问题均返回标准 OpenAI Tool Call。
- 真实模型 + 模拟 Spring Internal API 完整闭环：营业回答、菜单回答和 citation 均通过。
- 真实模型流式闭环：`delta`、`tool_status`、`done` 均通过。

### 历史待推进（现已完成）

- 确认 token、幂等和写工具两阶段确认已在 2026-07-22 P2/P4 完成。
- 管理端只读 Internal API 与 LangChain Tools 已在 2026-07-22 P3 完成。

### 已知限制

- `/user/ai/recommend` 已转发结构化 Agent `/v1/user/recommendations`，候选来自实时菜单 Tool，模型只生成推荐总结。
- `/user/ai/review/write` 已转发结构化 Agent `/v1/user/reviews/draft`；Spring 先校验本人已完成订单、菜品归属和敏感内容，Agent 生成未发布草稿。
- Internal API 需要设置 `AGENT_INTERNAL_AUTH_TOKEN`，并与 Agent Service 的 `SPRING_INTERNAL_AUTH_TOKEN` 保持一致。
- Agent API 两端需要使用相同的 `AGENT_SERVICE_AUTH_TOKEN`；未配置时对话接口会拒绝服务。
- 本地默认使用 SQLite；生产 Compose 使用 PostgreSQL `AsyncPostgresSaver` 共享图状态，确认存储使用共享持久卷并要求会话粘性路由。
- 当前默认 embedding 为离线字符 n-gram 哈希向量，适合本项目小型中文规则库；知识规模扩大前应评测并切换生产语义 embedding 与向量数据库。
# 2026-07-22 补充：管理端确认与菜品分类安全修复

### 本次完成
- 修复流式 Agent 请求携带 `confirmed_action_token` 时未进入确认执行链路的问题；流式确认现在会直接调用确认执行逻辑并返回 `done` 状态，避免把“确认”当作普通消息再次送入 `admin_agent` 节点。
- 新增管理端分类查询能力：Spring Internal API 增加 `/internal/agent/admin/categories`，Python client/schema/tool 增加 `admin_category_search`。
- 新增菜品前要求 Agent 查询真实启用菜品分类，并在 Python `create_admin_dish` 工具生成确认卡片前校验 `category_id`。
- Spring 写入侧新增二次校验：菜品新增时 `category_id` 必须引用已启用的菜品分类，防止模型或调用方传入不存在的分类 ID。
- 恢复管理端系统提示词为可读中文，并明确新增菜品前必须查询分类、不能编造分类名称或 `category_id`。
- README 和 Agent Service 开发命令改为直接使用本机 Python：`python -m pip install -e ".[dev]"`、`python -m uvicorn ...`、`python -m pytest`。

### 验证
- `python -m pytest agent-service\tests\test_admin_tools.py agent-service\tests\test_spring_internal_client.py agent-service\tests\test_admin_operations_graph.py`：17 项通过。
- `mvn -pl sky-server -Dtest=InternalAgentAdminControllerTest test`：未完成；首次因沙箱网络无法下载 Maven parent POM 失败，随后网络授权请求被取消。

### 风险与后续
- 需要在具备 Maven 依赖缓存或允许网络下载的环境中补跑 `InternalAgentAdminControllerTest`。
- 若前端仍通过自然语言“确认”发送普通聊天消息而不是调用 `/admin/ai/chat/resume`，业务确认仍无法执行；当前 admin web 已使用 resume 接口。
