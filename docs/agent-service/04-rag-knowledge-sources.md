# RAG 知识源与入库规则

## 可入库知识

首批白名单：

- `docs/agent.md` 中已确认的业务能力和维护事实；
- `docs/SKY_TAKE_OUT_FULL_PROJECT_README.md` 的项目说明；
- 人工维护的营业、配送、退款、优惠券、客服 SOP、菜品与套餐规则文档；
- 经管理员审核后放入 `docs/agent-service/knowledge/` 的 Markdown、HTML 或 PDF。

知识文档必须标明标题、维护人、更新时间、适用范围和可见级别。业务实时数据（订单状态、库存、店铺状态、优惠券资格）必须通过 Tool 查询，不能依赖向量库。

## 禁止入库内容

- 用户订单明细、地址、电话、支付信息、会话原文；
- 数据库备份、日志、密钥、token、`application-dev.yml`；
- `docs/private/`、个人简历、未审核的聊天记录；
- 可直接执行的管理操作口令或敏感词库全文。

## 文档元数据

每个 chunk 至少包含：

```json
{
  "source": "docs/agent-service/knowledge/coupon-policy.md",
  "title": "优惠券使用规则",
  "domain": "coupon",
  "visibility": "user",
  "updated_at": "2026-07-10",
  "content_hash": "sha256",
  "chunk_index": 3
}
```

`visibility` 仅使用 `public`、`user`、`admin`。检索时必须用 actor 类型和角色做元数据过滤，不能只在回答生成后再过滤。

## 切分、索引与检索

- 优先按 Markdown 标题和段落切分，保留标题层级；单 chunk 目标约 400–800 中文字符，重叠约 80–120 字符。
- 全量入库命令生成清单与 hash；增量入库只处理内容 hash 变化的源文件。
- 源文件被删除、降级可见范围或更新时，同步删除或更新对应向量。
- 检索顺序：可见性/域过滤 → 向量 Top-K → 可选重排 → 置信度阈值判断。
- 回答引用展示标题、路径和更新时间；置信度不足时说明“未在当前知识库找到确认依据”。

## 评测集要求

- 至少 20 条应命中文档的问题，覆盖营业、配送、优惠券、售后、菜品规则；
- 至少 10 条拒答样例，覆盖个人信息、越权管理规则、与业务无关问题；
- 每条记录包括 query、actor、预期 source、允许/禁止答案要点和最低相似度阈值；
- 文档更新后必须重新执行检索评测，确认旧内容不会被继续引用。
