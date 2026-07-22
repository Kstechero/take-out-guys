# Java 与 Python 内部 API 契约

> 本文件是实现前的协议基线。正式路由、字段与错误码确认后，应同步生成 OpenAPI 文件；未同步契约不得修改跨服务接口。

## 通用约定

- Base URL：由 `SPRING_INTERNAL_BASE_URL` 配置；仅允许内网访问。
- 请求头：`X-Request-Id`、`X-Agent-Service-Token`、`X-Actor-Type`、`X-Actor-Id`、`X-Actor-Roles`、`X-Actor-Expires-At`。
- 所有响应使用 UTF-8 JSON，并包含 `request_id`。
- 读取请求可在网络失败时重试一次；写请求必须携带 `Idempotency-Key`，且不得自动重试。

```json
{
  "ok": true,
  "data": {},
  "error_code": null,
  "message": "",
  "request_id": "6c08e50a-8e6a-4cf1-b2d6-4db0e1ad8cb8"
}
```

失败时 `ok=false`，`error_code` 仅可使用：`UNAUTHENTICATED`、`FORBIDDEN`、`NOT_FOUND`、`VALIDATION_ERROR`、`CONFLICT`、`RATE_LIMITED`、`UPSTREAM_ERROR`、`INTERNAL_ERROR`。

## Java 调用 Agent Service

- Base URL：由 Java `AGENT_SERVICE_BASE_URL` 配置。
- 请求头：`Content-Type: application/json`、`X-Agent-Service-Token`；`AGENT_SERVICE_AUTH_TOKEN` 未配置时 Agent 对话接口返回 `503`。
- Java 只传递最小 actor 上下文，不转发用户 JWT。

### `POST /v1/user/chat`

```json
{
  "request_id": "uuid",
  "actor": {"type": "user", "id": 1001, "roles": ["USER"], "expires_at": "2026-07-10T13:00:00Z"},
  "session_id": "optional-id",
  "message": "帮我找一份不辣的午餐",
  "confirmed_action_token": null
}
```

响应字段：`answer`、`status`、`session_id`、`citations`、`suggested_actions`、`confirmation`、`trace_id`。`status` 取 `completed`、`waiting_user`、`failed`、`unavailable`；`confirmation` 不为空时必须为 `waiting_user`，且 `answer` 只说明待确认操作，不能声称已经完成。

### `POST /v1/admin/chat`

请求与响应 envelope 和用户接口一致，但 `actor.type` 必须为 `admin`，角色至少包含一个管理端角色。管理端工具只注册已完成权限评审的能力；未迁移能力必须返回明确边界，不得回到 Java 手写 Tool Calling。

### `POST /v1/user/chat/stream` 与 `POST /v1/admin/chat/stream`

请求体与对应同步聊天接口一致，响应为 `text/event-stream`。Spring Boot 逐事件代理该响应；对旧前端的 `delta.content` 和 `done.sessionId` 字段继续提供兼容别名。

### SSE 事件

| 事件 | data 内容 | 说明 |
| --- | --- | --- |
| `run_started` | `{ "thread_id": "...", "trace_id": "..." }` | 一次图运行开始 |
| `node_started` | `{ "node": "classify_intent" }` | 图节点开始，不暴露内部 prompt |
| `tool_started` | `{ "tool": "menu_search" }` | 工具开始，不暴露敏感参数或内部地址 |
| `tool_finished` | `{ "tool": "menu_search", "status": "ok" }` | 工具结束 |
| `delta` | `{ "text": "..." }` | 回答文本增量 |
| `citation` | `{ "title": "优惠券规则", "source": "...", "updated_at": "..." }` | RAG 来源 |
| `confirmation` | `{ "token": "...", "summary": "确认加入 1 份…", "expires_at": "..." }` | 写操作确认卡片 |
| `interrupt` | `{ "kind": "confirmation", "request": {} }` | 图暂停等待用户输入 |
| `done` | `{ "session_id": "...", "trace_id": "...", "status": "completed" }` | 正常结束或暂停等待用户；Spring 代理保留该状态 |
| `error` | `{ "code": "UPSTREAM_ERROR", "message": "..." }` | 可展示的失败信息 |

### `POST /v1/threads/{thread_id}/resume`

恢复请求必须携带 `agent_name`、最小 actor、`request_id` 和批准/编辑/拒绝数据。服务端校验 thread、Agent 和 actor 一致后才能恢复；恢复不得重放已经确认的写操作。

### 结构化推荐与评价草稿

| Agent API | 用途 | 约束 |
| --- | --- | --- |
| `POST /v1/user/recommendations` | 根据实时菜单、预算、人数和饮食偏好生成结构化推荐 | 候选必须来自 `/internal/agent/menu/search` |
| `POST /v1/user/reviews/draft` | 生成尚未发布的评价草稿 | 必须先调用 `/internal/agent/reviews/draft/check` 校验本人已完成订单、菜品归属和敏感内容 |

`POST /internal/agent/reviews/draft/check` 请求包含 `order_id`、`dish_id`、`rating`、`highlights`，只返回生成草稿所需的最小事实，不提交评价。

## Python 调用 Spring Internal API

| Tool | 方法与草案路由 | actor | 风险 | 关键约束 |
| --- | --- | --- | --- | --- |
| `shop_status` | `GET /internal/agent/shop/status` | user/admin | read | 返回当前状态和更新时间 |
| `menu_search` | `GET /internal/agent/menu/search` | user/admin | read | query、预算、口味为已校验参数 |
| `recent_orders` | `GET /internal/agent/orders/recent` | user | read | Java 固定使用当前 actor 查询 |
| `get_order` | `GET /internal/agent/orders/{order_id}` | user/admin | read | Java 校验订单归属或员工角色 |
| `get_cart` | `GET /internal/agent/cart` | user | read | 返回脱敏且最小化的购物车数据 |
| `list_addresses` | `GET /internal/agent/addresses` | user | read | 默认隐藏手机号与详细门牌号 |
| `list_available_coupons` | `GET /internal/agent/coupons/available` | user | read | 由 Java 按当前用户和订单条件计算 |
| `check_sensitive_words` | `POST /internal/agent/sensitive-words/check` | user/admin | read | 只用于内容检查，不泄露词库全文 |
| `add_to_cart` / `update_cart_item` / `remove_from_cart` / `clear_cart` | `POST /internal/agent/cart/changes` | user | write | 确认 token 与幂等键必填；加购绑定 `expected_unit_amount`，价格或上下架状态变化时拒绝 |
| `claim_coupon` | `POST /internal/agent/coupons/{coupon_id}/claim` | user | write | 确认 token 与幂等键必填 |
| `query_business_overview` | `GET /internal/agent/admin/business/overview` | admin | read | 显式日期范围与数据口径 |
| `admin_order_search` / `admin_order_detail` | `GET /internal/agent/admin/orders[/{order_id}]` | admin | read | 列表必须有订单号、状态或时间范围；返回脱敏摘要 |
| `admin_menu_search` / `admin_set_meal_search` | `GET /internal/agent/admin/menu`、`GET /internal/agent/admin/setmeals` | admin | read | 单店范围、最多 20 条 |
| `admin_coupon_search` | `GET /internal/agent/admin/coupons` | admin | read | 返回配置、余量和有效期，不返回领取用户 |
| `admin_review_search` | `GET /internal/agent/admin/reviews` | admin | read | 用户名和内容脱敏 |
| `set_shop_status` | `POST /internal/agent/admin/shop/status` | admin | write | 灰度开关、确认、幂等、Redis 乐观锁和审计理由必填 |
| `update_order` | `POST /internal/agent/admin/orders/{order_id}/actions` | admin | write | 仅允许 2→3、3→4、4→5，`expected_status` 原子校验 |
| `manage_coupon` | `POST /internal/agent/admin/coupons/{coupon_id}/actions` | admin | write | 仅允许启停，`expected_status` 原子校验 |
| `create_admin_dish` | `POST /internal/agent/admin/menu/items` | admin | write | 单菜品新增；字段白名单、确认、幂等、灰度和审计理由必填 |
| `update_admin_dish` | `POST /internal/agent/admin/menu/items/{dish_id}/actions` | admin | write | 单菜品部分字段修改；绑定 `expected_updated_at` 乐观版本，不开放口味替换 |
| `create_admin_coupon` | `POST /internal/agent/admin/coupons` | admin | write | 单优惠券新增；金额、发行量、限领数和有效期由 Java 再校验 |

管理查询响应统一包含 `source=spring_internal_api`、`scope` 和 `generated_at`。当前业务系统是单门店部署，范围显式标记为 `single_store`。管理写接口默认由 `AGENT_INTERNAL_WRITES_ENABLED=false` 关闭，验收或灰度环境必须显式开启。

管理写操作只修改单个 Redis 值或单个业务聚合：Redis 使用 `WATCH/MULTI/EXEC`，订单和优惠券状态使用带 `expected_status` 的原子 SQL，菜品修改使用 `expected_updated_at` 乐观 SQL；前置条件冲突时不产生变更，异常时事务回滚。幂等状态若进入 `FAILED_UNKNOWN`，禁止自动重放，由管理员依据结构化审计日志核对资源现值后重新发起新的确认，作为安全补偿流程。

## 写工具确认协议

1. Python 图节点根据 Tool 参数生成 canonical action：工具名、actor、资源、参数摘要、过期时间。
2. 服务端保存确认记录并返回不可猜测的 `confirmation_token`，有效期建议 5 分钟。
3. 用户通过原对话会话提交 token；Python 验证 token 与会话、actor、摘要未变。
4. Python 调用 Java 写接口；Java 再校验服务身份、actor、确认 token、资源归属和 `Idempotency-Key`。
5. Java 返回最终事实；模型只能据此回答“已完成”或“未完成”。

恢复接口的 `decision` 支持 `approve`、`edit`、`reject`。`edit` 必须携带结构化 `edited_arguments`；服务端作废旧 token、按对应 Tool schema 重新校验并生成新 token，旧 token 不得再次执行。
