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

响应字段：`answer`、`session_id`、`citations`、`suggested_actions`、`confirmation`、`trace_id`。`confirmation` 不为空时，`answer` 只说明待确认操作，不能声称已经完成。

### SSE 事件

| 事件 | data 内容 | 说明 |
| --- | --- | --- |
| `delta` | `{ "text": "..." }` | 回答文本增量 |
| `tool_status` | `{ "tool": "menu_search", "status": "running" }` | 不暴露敏感参数或内部地址 |
| `citation` | `{ "title": "优惠券规则", "source": "...", "updated_at": "..." }` | RAG 来源 |
| `confirmation` | `{ "token": "...", "summary": "确认加入 1 份…", "expires_at": "..." }` | 写操作确认卡片 |
| `done` | `{ "session_id": "...", "trace_id": "..." }` | 正常结束 |
| `error` | `{ "code": "UPSTREAM_ERROR", "message": "..." }` | 可展示的失败信息 |

## Python 调用 Spring Internal API

| Tool | 方法与草案路由 | actor | 风险 | 关键约束 |
| --- | --- | --- | --- | --- |
| `get_shop_status` | `GET /internal/agent/shop/status` | user/admin | read | 返回当前状态和更新时间 |
| `menu_search` | `GET /internal/agent/menu/search` | user/admin | read | query、预算、口味为已校验参数 |
| `list_recent_orders` | `GET /internal/agent/orders/recent` | user | read | Java 固定使用当前 actor 查询 |
| `get_order_detail` | `GET /internal/agent/orders/{order_id}` | user/admin | read | Java 校验订单归属或员工角色 |
| `get_cart` | `GET /internal/agent/cart` | user | read | 返回脱敏且最小化的购物车数据 |
| `list_addresses` | `GET /internal/agent/addresses` | user | read | 默认隐藏手机号与详细门牌号 |
| `list_available_coupons` | `GET /internal/agent/coupons/available` | user | read | 由 Java 按当前用户和订单条件计算 |
| `check_sensitive_words` | `POST /internal/agent/sensitive-words/check` | user/admin | read | 只用于内容检查，不泄露词库全文 |
| `change_cart` | `POST /internal/agent/cart/changes` | user | write | 确认 token 与幂等键必填 |
| `claim_coupon` | `POST /internal/agent/coupons/{coupon_id}/claim` | user | write | 确认 token 与幂等键必填 |
| `query_business_overview` | `GET /internal/agent/admin/business/overview` | admin | read | 显式日期范围与数据口径 |

## 写工具确认协议

1. Python 图节点根据 Tool 参数生成 canonical action：工具名、actor、资源、参数摘要、过期时间。
2. 服务端保存确认记录并返回不可猜测的 `confirmation_token`，有效期建议 5 分钟。
3. 用户通过原对话会话提交 token；Python 验证 token 与会话、actor、摘要未变。
4. Python 调用 Java 写接口；Java 再校验服务身份、actor、确认 token、资源归属和 `Idempotency-Key`。
5. Java 返回最终事实；模型只能据此回答“已完成”或“未完成”。
