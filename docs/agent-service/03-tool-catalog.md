# Agent 工具目录

## 工具设计规则

- 一个工具只完成一个稳定业务动作，工具名使用英文 `snake_case`。
- 所有参数由 Pydantic 严格校验；禁止接收自由形态 SQL、URL、任意 JSON 或用户身份字段。
- 工具描述面向模型，但权限、归属和参数校验必须由 Java 与 Python 代码执行。
- `write` 工具默认不可在第一轮执行，必须进入确认状态；高风险管理操作暂不注册。
- 工具结果返回面向回答的 DTO，不返回密码、令牌、支付信息、完整手机号或数据库实体。

## 用户端 P0 工具

| Tool | 类型 | 来源映射 | 输入 | 输出 | 权限与限制 |
| --- | --- | --- | --- | --- | --- |
| `get_shop_status` | read | legacy `get_shop_status` | 无 | 状态、更新时间 | user/admin |
| `menu_search` | read | legacy `search_menu` | query、dietary_preferences、budget_max、limit | 菜品/套餐候选 | user/admin，limit 最大 10 |
| `list_recent_orders` | read | legacy `list_orders` | status、limit | 当前用户订单摘要 | user，仅查询自己 |
| `get_order_detail` | read | legacy `list_orders` | order_id | 订单详情、状态 | Java 校验归属 |
| `get_cart` | read | legacy `get_cart` | 无 | 购物车摘要 | user，仅当前用户 |
| `list_addresses` | read | legacy `list_addresses` | default_only | 脱敏地址摘要 | user，仅当前用户 |
| `list_available_coupons` | read | legacy `list_coupons` | order_amount | 可用优惠券摘要 | user，仅当前用户 |
| `search_service_knowledge` | read | legacy `search_service_knowledge` | query、domain | 文档片段和来源 | 按 visibility 过滤 |
| `check_sensitive_words` | read | 现有敏感词能力 | text | safe、masked_text | 不返回敏感词词库 |

## 用户端 P1 写工具

| Tool | 输入 | 确认摘要示例 | 幂等键 |
| --- | --- | --- | --- |
| `change_cart` | action、dish_id/setmeal_id、quantity | “确认将宫保鸡丁数量改为 2 吗？” | `actor + session + confirmation_token` |
| `claim_coupon` | coupon_id | “确认领取满 30 减 5 优惠券吗？” | `actor + coupon_id + confirmation_token` |
| `manage_address` | action、address payload | “确认将默认地址改为…吗？” | `actor + confirmation_token` |

AI 评价功能只生成草稿：订单完成状态和敏感词检查是前置条件，最终提交仍由现有用户端评价接口完成。

## 管理端工具分级

| 分级 | 允许工具 | 说明 |
| --- | --- | --- |
| A：只读 MVP | `get_shop_status`、`query_orders`、`get_order_statistics`、`query_coupons`、`query_dishes`、`query_setmeals`、`get_business_overview`、`get_business_trend`、`search_operational_knowledge` | 首批开放，回答标注口径和时间 |
| B：需确认 | `set_shop_status`、`update_order`、`manage_coupon` | 逐工具评审、二次确认、审计理由 |
| C：暂缓 | `manage_employee`、`change_my_password`、`manage_category`、`manage_dish`、`manage_setmeal`、`manage_sensitive_word` | 涉及高风险权限或数据变更，MVP 不注册 |

## 工具错误映射

| Java 错误 | Tool 返回 | 对用户的回答原则 |
| --- | --- | --- |
| `FORBIDDEN` | `{ "ok": false, "code": "FORBIDDEN" }` | 不泄露资源存在性，说明无权访问 |
| `NOT_FOUND` | `{ "ok": false, "code": "NOT_FOUND" }` | 说明未找到并建议检查信息 |
| `VALIDATION_ERROR` | `{ "ok": false, "code": "VALIDATION_ERROR", "fields": [...] }` | 请求用户补充或修正参数 |
| `UPSTREAM_ERROR` / timeout | `{ "ok": false, "code": "TEMPORARY_UNAVAILABLE" }` | 说明暂不可用，不虚构结果 |
